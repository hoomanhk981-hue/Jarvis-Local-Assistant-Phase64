package com.example.data.models

import com.example.data.local.entities.DownloadedModelEntity
import java.io.File

/**
 * Runtime boundary for a local vision model.
 *
 * The native/backend implementation is supplied by the vision-capable
 * runtime configured by the app. No cloud endpoint is assumed here.
 */
interface VisionRuntimeManager {
    fun load(model: DownloadedModelEntity, speedMode: String)
    fun unload()
    fun isLoaded(): Boolean
    fun describe(imageFile: File, prompt: String): String
}
