package com.example.data.models

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Central lifecycle/safety boundary for the local text runtime.
 * It serializes load/inference/unload so native handles are never released while
 * another coroutine is using them. It also chooses a conservative runtime profile
 * from actual device memory/CPU capacity.
 */
class LlmRuntimeManager(private val context: Context) {
    data class DeviceCapability(
        val totalRamMb: Long,
        val availableRamMb: Long,
        val cpuCores: Int,
        val recommended: LocalLlmEngine.SpeedProfile,
        val lowRam: Boolean
    )

    private val mutex = Mutex()
    private val engine = LocalLlmEngine()
    @Volatile private var lastActivityMs = 0L

    fun capability(): DeviceCapability {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val total = info.totalMem / (1024L * 1024L)
        val avail = info.availMem / (1024L * 1024L)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val lowRam = info.lowMemory || (Build.VERSION.SDK_INT >= 19 && am.isLowRamDevice)
        val recommended = when {
            lowRam || total < 3000 -> LocalLlmEngine.SpeedProfile.LOW
            total < 5500 -> LocalLlmEngine.SpeedProfile.MEDIUM
            else -> LocalLlmEngine.SpeedProfile.HIGH
        }
        return DeviceCapability(total, avail, cores, recommended, lowRam)
    }

    suspend fun load(file: File, requested: String): LocalLlmEngine.SpeedProfile = mutex.withLock {
        val requestedProfile = LocalLlmEngine.SpeedProfile.valueOf(requested.uppercase())
        val safeProfile = clampToDevice(requestedProfile)
        engine.load(file, safeProfile.name)
        lastActivityMs = System.currentTimeMillis()
        safeProfile
    }

    suspend fun complete(prompt: String, requested: String): String = mutex.withLock {
        if (!engine.isLoaded()) error("مدل محلی Load نشده است")
        val safeProfile = clampToDevice(LocalLlmEngine.SpeedProfile.valueOf(requested.uppercase()))
        lastActivityMs = System.currentTimeMillis()
        try {
            // llama.complete is a native blocking call. We cannot safely interrupt
            // the native operation unless the binding exposes a cancellation API.
            // Coroutine cancellation is therefore checked before/after the call,
            // while the mutex prevents unsafe concurrent release/reload.
            val result = withContext(Dispatchers.Default) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                engine.complete(prompt, safeProfile.name)
            }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            lastActivityMs = System.currentTimeMillis()
            result
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun unload() = mutex.withLock {
        engine.unload()
        lastActivityMs = 0L
    }

    /** Called from Activity/Process trim callbacks. */
    suspend fun onTrimMemory(level: Int) {
        // COMPLETE/critical memory pressure: release native weights immediately.
        // UI hidden/background levels also release the large model to avoid OOM.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            unload()
        }
    }

    fun isLoaded(): Boolean = engine.isLoaded()
    fun lastActivityMillis(): Long = lastActivityMs

    private fun clampToDevice(requested: LocalLlmEngine.SpeedProfile): LocalLlmEngine.SpeedProfile {
        val cap = capability()
        return when {
            cap.lowRam -> LocalLlmEngine.SpeedProfile.LOW
            cap.totalRamMb < 4500 && requested == LocalLlmEngine.SpeedProfile.HIGH -> LocalLlmEngine.SpeedProfile.MEDIUM
            cap.cpuCores <= 2 && requested == LocalLlmEngine.SpeedProfile.HIGH -> LocalLlmEngine.SpeedProfile.MEDIUM
            else -> requested
        }
    }
}
