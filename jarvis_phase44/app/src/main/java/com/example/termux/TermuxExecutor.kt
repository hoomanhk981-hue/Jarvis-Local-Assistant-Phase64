package com.example.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Executes commands through Termux RUN_COMMAND and, unlike the old implementation,
 * waits for the real Termux result (stdout/stderr/exit code).
 *
 * Requires Termux >= 0.109, com.termux.permission.RUN_COMMAND and
 * allow-external-apps=true in Termux settings.
 */
class TermuxExecutor(private val context: Context) {
    private val nextExecutionId = AtomicInteger(10_000)

    fun isTermuxInstalled(): Boolean = try {
        context.packageManager.getPackageInfo("com.termux", 0)
        true
    } catch (_: Exception) { false }

    suspend fun executeTermuxCommand(
        command: String,
        workdir: String = "/data/data/com.termux/files/home",
        timeoutMs: Long = 30_000L,
        stdin: String? = null
    ): TermuxCommandResult {
        if (!isTermuxInstalled()) {
            return TermuxCommandResult(-1, "", "Termux is not installed", 127)
        }
        if (command.length > 100_000) {
            return TermuxCommandResult(-1, "", "Command is too large for an Android intent", 126)
        }

        val executionId = nextExecutionId.incrementAndGet()
        val deferred = TermuxResultBroker.register(executionId)
        try {
            val callback = Intent(context, TermuxResultService::class.java).apply {
                putExtra(TermuxResultService.EXTRA_EXECUTION_ID, executionId)
            }
            val flags = PendingIntent.FLAG_ONE_SHOT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getService(context, executionId, callback, flags)

            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workdir)
                if (stdin != null) putExtra("com.termux.RUN_COMMAND_STDIN", stdin)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
                putExtra("com.termux.RUN_COMMAND_LABEL", "Jarvis command")
                putExtra("com.termux.RUN_COMMAND_DESCRIPTION", "Command requested by Jarvis")
            }
            context.startService(intent)

            return withTimeoutOrNull(timeoutMs) { deferred.await() }
                ?: TermuxCommandResult(executionId, "", "Timed out waiting for Termux result", 124)
        } catch (e: Exception) {
            return TermuxCommandResult(executionId, "", e.message ?: "Failed to start Termux command", 126)
        } finally {
            TermuxResultBroker.cancel(executionId)
        }
    }

    suspend fun executeCommandText(command: String): String {
        val result = executeTermuxCommand(command)
        return buildString {
            append(if (result.success) "Command completed successfully" else "Command failed")
            append("\nexit_code=${result.exitCode}")
            if (result.errorCode != null) append("\ntermux_error=${result.errorCode}")
            if (!result.errorMessage.isNullOrBlank()) append("\nerror=${result.errorMessage}")
            if (result.stdout.isNotBlank()) append("\nstdout:\n${result.stdout}")
            if (result.stderr.isNotBlank()) append("\nstderr:\n${result.stderr}")
            if (result.stdoutTruncated || result.stderrTruncated) append("\n(output truncated by Termux/Android result limits)")
        }
    }

    fun isDangerousCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("rm -rf") || lower.contains("mkfs") || lower.contains("dd if=") ||
            lower.contains(":(){ :|:& };") || lower.contains("format ") ||
            lower.contains("shutdown") || lower.contains("reboot")
    }

    suspend fun executeScript(filename: String, code: String, language: String): String {
        val escaped = filename.replace("'", "'\\''")
        return when (language.lowercase()) {
            "python", "py" -> executeCommandText("python '$escaped'")
            "cpp", "c++", "c" -> executeCommandText("clang++ '$escaped' -o '${escaped.substringBeforeLast(".")}_out' && './${escaped.substringBeforeLast(".")}_out'")
            "bash", "sh" -> executeCommandText("bash '$escaped'")
            else -> "فایل $filename ذخیره شد؛ اجرای خودکار برای زبان $language فعال نیست."
        }
    }
}
