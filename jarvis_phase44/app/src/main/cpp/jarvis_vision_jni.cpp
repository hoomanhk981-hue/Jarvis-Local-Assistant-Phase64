#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "JarvisVision", __VA_ARGS__)

namespace {
struct VisionRuntime {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    mtmd_context * vision = nullptr;
    llama_sampler * sampler = nullptr;
    int threads = 2;
    int context = 4096;
    bool initialized = false;
    std::mutex mutex;
};

VisionRuntime g;

struct TextRuntime {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    llama_sampler * sampler = nullptr;
    int threads = 2;
    int context = 4096;
    bool initialized = false;
    std::mutex mutex;
};

TextRuntime gt;

std::string jstr(JNIEnv * env, jstring value) {
    if (!value) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    std::string out = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return out;
}

void throwIllegal(JNIEnv * env, const std::string & message) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls) env->ThrowNew(cls, message.c_str());
}

void releaseLocked() {
    if (g.sampler) {
        llama_sampler_free(g.sampler);
        g.sampler = nullptr;
    }
    if (g.vision) {
        mtmd_free(g.vision);
        g.vision = nullptr;
    }
    if (g.ctx) {
        llama_free(g.ctx);
        g.ctx = nullptr;
    }
    if (g.model) {
        llama_model_free(g.model);
        g.model = nullptr;
    }
    g.initialized = false;
}

bool loadLocked(const std::string & modelPath, const std::string & mmprojPath, int threads, int context) {
    releaseLocked();
    llama_backend_init();

    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0; // predictable CPU-first baseline for Android
    g.model = llama_model_load_from_file(modelPath.c_str(), mp);
    if (!g.model) {
        LOGE("Failed to load vision model: %s", modelPath.c_str());
        return false;
    }

    auto cp = llama_context_default_params();
    cp.n_ctx = static_cast<uint32_t>(std::max(1024, context));
    cp.n_batch = 1024;
    cp.n_ubatch = 256;
    cp.n_threads = std::max(1, threads);
    cp.n_threads_batch = std::max(1, threads);
    cp.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_AUTO;
    g.ctx = llama_init_from_model(g.model, cp);
    if (!g.ctx) {
        LOGE("Failed to create llama context for vision");
        releaseLocked();
        return false;
    }

    auto vp = mtmd_context_params_default();
    vp.use_gpu = false;
    vp.print_timings = false;
    vp.n_threads = std::max(1, threads);
    vp.warmup = false;
    g.vision = mtmd_init_from_file(mmprojPath.c_str(), g.model, vp);
    if (!g.vision || !mtmd_support_vision(g.vision)) {
        LOGE("Failed to initialize multimodal projector: %s", mmprojPath.c_str());
        releaseLocked();
        return false;
    }

    auto sp = llama_sampler_chain_default_params();
    g.sampler = llama_sampler_chain_init(sp);
    llama_sampler_chain_add(g.sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(g.sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(g.sampler, llama_sampler_init_temp(0.2f));
    llama_sampler_chain_add(g.sampler, llama_sampler_init_greedy());

    g.threads = threads;
    g.context = context;
    g.initialized = true;
    return true;
}

std::string piece(const llama_vocab * vocab, llama_token token) {
    char buf[8192];
    const int n = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, false);
    return n > 0 ? std::string(buf, static_cast<size_t>(n)) : std::string();
}

std::string formattedPrompt(const llama_model * model, const std::string & userText) {
    const char * tmpl = llama_model_chat_template(model, nullptr);
    if (!tmpl) {
        return userText;
    }
    llama_chat_message msg{ "user", userText.c_str() };
    int32_t size = llama_chat_apply_template(tmpl, &msg, 1, true, nullptr, 0);
    if (size <= 0) return userText;
    std::string out(static_cast<size_t>(size), '\0');
    llama_chat_apply_template(tmpl, &msg, 1, true, out.data(), size);
    return out;
}

std::string analyzeLocked(const unsigned char * image, size_t imageLen, const std::string & question, int maxTokens) {
    const char * marker = mtmd_get_marker(g.vision);
    if (!marker) marker = mtmd_default_marker();

    std::string user = std::string(marker) + "\n" + question;
    std::string prompt = formattedPrompt(g.model, user);

    auto bmp = mtmd_helper_bitmap_init_from_buf(g.vision, image, imageLen, false);
    if (!bmp.bitmap) return "خطا: تصویر توسط Vision Runtime قابل خواندن نیست.";

    auto chunks = mtmd_input_chunks_init();
    mtmd_input_text text{};
    text.text = prompt.c_str();
    text.text_len = prompt.size();
    text.add_special = true;
    text.parse_special = true;

    const mtmd_bitmap * bitmapPtr = bmp.bitmap;
    const int32_t tok = mtmd_tokenize(g.vision, chunks, &text, &bitmapPtr, 1);
    if (tok != 0) {
        mtmd_bitmap_free(bmp.bitmap);
        mtmd_input_chunks_free(chunks);
        return "خطا: tokenize تصویر/پرامپت شکست خورد (کد " + std::to_string(tok) + ").";
    }

    llama_memory_clear(llama_get_memory(g.ctx), true);
    llama_sampler_reset(g.sampler);
    llama_pos nPast = 0;
    const size_t nChunks = mtmd_input_chunks_size(chunks);
    for (size_t i = 0; i < nChunks; ++i) {
        const mtmd_input_chunk * chunk = mtmd_input_chunks_get(chunks, i);
        llama_pos nextPast = nPast;
        const int32_t rc = mtmd_helper_eval_chunk_single(
            g.vision, g.ctx, chunk, nPast, 0, 1024, i + 1 == nChunks, &nextPast);
        if (rc != 0) {
            mtmd_bitmap_free(bmp.bitmap);
            mtmd_input_chunks_free(chunks);
            return "خطا: اجرای multimodal chunk شکست خورد (کد " + std::to_string(rc) + ").";
        }
        nPast = nextPast;
    }

    mtmd_bitmap_free(bmp.bitmap);
    mtmd_input_chunks_free(chunks);

    const llama_vocab * vocab = llama_model_get_vocab(g.model);
    std::string output;
    output.reserve(static_cast<size_t>(maxTokens) * 4);
    llama_batch batch = llama_batch_init(1, 0, 1);

    for (int i = 0; i < maxTokens; ++i) {
        const llama_token token = llama_sampler_sample(g.sampler, g.ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        output += piece(vocab, token);
        llama_sampler_accept(g.sampler, token);

        batch.n_tokens = 1;
        batch.token[0] = token;
        batch.pos[0] = nPast++;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        if (llama_decode(g.ctx, batch) != 0) {
            output += "\n[Vision generation error]";
            break;
        }
    }
    llama_batch_free(batch);
    return output.empty() ? "پاسخی از مدل Vision دریافت نشد." : output;
}

void releaseTextLocked() {
    if (gt.sampler) {
        llama_sampler_free(gt.sampler);
        gt.sampler = nullptr;
    }
    if (gt.ctx) {
        llama_free(gt.ctx);
        gt.ctx = nullptr;
    }
    if (gt.model) {
        llama_model_free(gt.model);
        gt.model = nullptr;
    }
    gt.initialized = false;
}

bool loadTextLocked(const std::string & modelPath, int threads, int context) {
    releaseTextLocked();
    llama_backend_init();

    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    gt.model = llama_model_load_from_file(modelPath.c_str(), mp);
    if (!gt.model) {
        LOGE("Failed to load text model: %s", modelPath.c_str());
        return false;
    }

    auto cp = llama_context_default_params();
    cp.n_ctx = static_cast<uint32_t>(std::max(512, context));
    cp.n_batch = 512;
    cp.n_ubatch = 256;
    cp.n_threads = std::max(1, threads);
    cp.n_threads_batch = std::max(1, threads);
    cp.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_AUTO;
    gt.ctx = llama_init_from_model(gt.model, cp);
    if (!gt.ctx) {
        LOGE("Failed to create llama context for text model");
        releaseTextLocked();
        return false;
    }

    auto sp = llama_sampler_chain_default_params();
    gt.sampler = llama_sampler_chain_init(sp);
    llama_sampler_chain_add(gt.sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(gt.sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(gt.sampler, llama_sampler_init_temp(0.3f));
    llama_sampler_chain_add(gt.sampler, llama_sampler_init_greedy());

    gt.threads = threads;
    gt.context = context;
    gt.initialized = true;
    return true;
}

std::string completeTextLocked(const std::string & prompt, const std::string & systemPrompt, int maxTokens) {
    if (!gt.initialized || !gt.model || !gt.ctx || !gt.sampler) {
        return "خطا: مدل محلی لود نشده است.";
    }

    const llama_vocab * vocab = llama_model_get_vocab(gt.model);
    std::string formatted = prompt;
    const char * tmpl = llama_model_chat_template(gt.model, nullptr);
    if (tmpl) {
        llama_chat_message msgs[2];
        int n_msgs = 0;
        if (!systemPrompt.empty()) {
            msgs[n_msgs++] = llama_chat_message{ "system", systemPrompt.c_str() };
        }
        msgs[n_msgs++] = llama_chat_message{ "user", prompt.c_str() };
        int32_t size = llama_chat_apply_template(tmpl, msgs, n_msgs, true, nullptr, 0);
        if (size > 0) {
            formatted.resize(static_cast<size_t>(size));
            llama_chat_apply_template(tmpl, msgs, n_msgs, true, formatted.data(), size);
        }
    }

    std::vector<llama_token> tokens(formatted.size() + 32);
    int nTokens = llama_tokenize(vocab, formatted.c_str(), static_cast<int32_t>(formatted.size()), tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
    if (nTokens < 0) {
        tokens.resize(static_cast<size_t>(-nTokens));
        nTokens = llama_tokenize(vocab, formatted.c_str(), static_cast<int32_t>(formatted.size()), tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
    }
    if (nTokens <= 0) return "خطا در پردازش توکن‌های ورودی.";
    tokens.resize(static_cast<size_t>(nTokens));

    llama_memory_clear(llama_get_memory(gt.ctx), true);
    llama_sampler_reset(gt.sampler);

    llama_batch batch = llama_batch_init(std::max(512, nTokens), 0, 1);
    for (int i = 0; i < nTokens; ++i) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == nTokens - 1) ? 1 : 0;
    }
    batch.n_tokens = nTokens;

    if (llama_decode(gt.ctx, batch) != 0) {
        llama_batch_free(batch);
        return "خطا در decode پرامپت متنی.";
    }
    llama_batch_free(batch);

    llama_pos nPast = nTokens;
    std::string output;
    output.reserve(static_cast<size_t>(maxTokens) * 4);
    batch = llama_batch_init(1, 0, 1);

    for (int i = 0; i < maxTokens; ++i) {
        const llama_token token = llama_sampler_sample(gt.sampler, gt.ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        output += piece(vocab, token);
        llama_sampler_accept(gt.sampler, token);

        batch.n_tokens = 1;
        batch.token[0] = token;
        batch.pos[0] = nPast++;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        if (llama_decode(gt.ctx, batch) != 0) {
            output += "\n[Text generation error]";
            break;
        }
    }
    llama_batch_free(batch);
    return output.empty() ? "پاسخی از مدل دریافت نشد." : output;
}
}

// Vision LLM JNI
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_vision_LocalVisionLlmEngine_nativeLoad(
    JNIEnv * env, jobject, jstring modelPath, jstring mmprojPath, jint threads, jint context) {
    std::lock_guard<std::mutex> lock(g.mutex);
    return loadLocked(jstr(env, modelPath), jstr(env, mmprojPath), threads, context) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_vision_LocalVisionLlmEngine_nativeAnalyze(
    JNIEnv * env, jobject, jbyteArray imageBytes, jstring question, jint maxTokens) {
    std::lock_guard<std::mutex> lock(g.mutex);
    if (!g.initialized) {
        throwIllegal(env, "Vision Runtime هنوز Load نشده است.");
        return nullptr;
    }
    const jsize len = env->GetArrayLength(imageBytes);
    if (len <= 0) {
        throwIllegal(env, "داده تصویر خالی است.");
        return nullptr;
    }
    std::vector<unsigned char> bytes(static_cast<size_t>(len));
    env->GetByteArrayRegion(imageBytes, 0, len, reinterpret_cast<jbyte *>(bytes.data()));
    try {
        const std::string result = analyzeLocked(bytes.data(), bytes.size(), jstr(env, question), std::max(16, static_cast<int>(maxTokens)));
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception & ex) {
        throwIllegal(env, ex.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_vision_LocalVisionLlmEngine_nativeRelease(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g.mutex);
    releaseLocked();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_vision_LocalVisionLlmEngine_nativeIsLoaded(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g.mutex);
    return g.initialized ? JNI_TRUE : JNI_FALSE;
}

// Text LLM JNI
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_data_models_LocalLlmEngine_nativeLoad(
    JNIEnv * env, jobject, jstring modelPath, jint threads, jint context) {
    std::lock_guard<std::mutex> lock(gt.mutex);
    return loadTextLocked(jstr(env, modelPath), threads, context) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_data_models_LocalLlmEngine_nativeComplete(
    JNIEnv * env, jobject, jstring prompt, jstring systemPrompt, jint maxTokens) {
    std::lock_guard<std::mutex> lock(gt.mutex);
    if (!gt.initialized) {
        throwIllegal(env, "Local LLM Runtime هنوز Load نشده است.");
        return nullptr;
    }
    try {
        const std::string result = completeTextLocked(jstr(env, prompt), jstr(env, systemPrompt), std::max(8, static_cast<int>(maxTokens)));
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception & ex) {
        throwIllegal(env, ex.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_data_models_LocalLlmEngine_nativeRelease(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(gt.mutex);
    releaseTextLocked();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_data_models_LocalLlmEngine_nativeIsLoaded(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(gt.mutex);
    return gt.initialized ? JNI_TRUE : JNI_FALSE;
}
