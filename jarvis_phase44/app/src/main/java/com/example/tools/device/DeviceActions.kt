package com.example.tools.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Safe Android device/UI action boundary.
 *
 * Actions that affect another app are exposed as explicit Android intents.
 * Arbitrary shell execution and security-control bypasses are intentionally
 * outside this tool.
 */
class DeviceActions(private val context: Context) {

    fun openApp(packageName: String): Result<Unit> {
        if (packageName.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name is empty."))
        }

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return Result.failure(
                IllegalArgumentException("Application is not installed.")
            )

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun openFile(uri: Uri): Result<Unit> {
        val mime = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) == null) {
                Result.failure(IllegalArgumentException("No compatible application found."))
            } else {
                context.startActivity(intent)
                Result.success(Unit)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun openSystemSettings(): Result<Unit> =
        start(Intent(Settings.ACTION_SETTINGS))

    fun openAppSettings(packageName: String): Result<Unit> {
        if (packageName.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name is empty."))
        }

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        return start(intent)
    }

    private fun start(intent: Intent): Result<Unit> {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
