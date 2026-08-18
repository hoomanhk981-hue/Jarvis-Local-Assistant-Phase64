cmake_minimum_required(VERSION 3.22.1)
project(jarvis_native LANGUAGES C CXX)

include(FetchContent)

# llama.cpp is fetched at build time; no model weights are bundled in the APK/source ZIP.
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_MTMD ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(MTMD_VIDEO OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)

# llama.cpp is pinned to a specific commit (not "master") so builds stay
# reproducible and cannot break when upstream changes its API. This revision
# is the one the JNI bridge (jarvis_vision_jni.cpp) was validated against.
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG 9731ad3f29da96f588711a0d1eb08cf210721e16
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(llama_cpp)

add_library(jarvis_vision SHARED jarvis_vision_jni.cpp)

target_compile_features(jarvis_vision PRIVATE cxx_std_17)
target_include_directories(jarvis_vision PRIVATE
    ${llama_cpp_SOURCE_DIR}/include
    ${llama_cpp_SOURCE_DIR}/tools/mtmd
)
target_link_libraries(jarvis_vision PRIVATE mtmd llama log android)

set_target_properties(jarvis_vision PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
)
