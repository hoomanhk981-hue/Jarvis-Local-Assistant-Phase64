package com.example.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Executes commands through Termux RUN_COMMAND service and waits for real execution results.
 *
 * Requirements on Termux:
 * 1. Termux >= 0.109 installed (com.termux).
 * 2. com.termux.permission.RUN_COMMAND declared.
 * 3. allow-external-apps=true enabled in ~/.termux/termux.properties.
 */
class TermuxExecutor(private val context: Context) {
    private val nextExecutionId = AtomicInteger(10_000)

    fun isTermuxInstalled(): Boolean = try {
        context.packageManager.getPackageInfo("com.termux", 0)
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Directly launches the Termux application in the foreground.
     */
    fun launchTermuxApp(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Dispatches a command to Termux's RunCommandService and waits for stdout/stderr/exitCode.
     */
    suspend fun executeTermuxCommand(
        command: String,
        workdir: String = "/data/data/com.termux/files/home",
        timeoutMs: Long = 30_000L,
        stdin: String? = null
    ): TermuxCommandResult {
        if (!isTermuxInstalled()) {
            return TermuxCommandResult(
                executionId = -1,
                stdout = "",
                stderr = "اپلیکیشن Termux روی دستگاه نصب نیست.",
                exitCode = 127,
                errorMessage = "Termux is not installed on this device"
            )
        }

        val trimmedCmd = command.trim()
        if (trimmedCmd.isEmpty()) {
            return TermuxCommandResult(
                executionId = -1,
                stdout = "",
                stderr = "دستور ارسال‌شده خالی است.",
                exitCode = 1,
                errorMessage = "Command is empty"
            )
        }

        if (trimmedCmd.length > 100_000) {
            return TermuxCommandResult(
                executionId = -1,
                stdout = "",
                stderr = "طول دستور بیش از حد مجاز است.",
                exitCode = 126,
                errorMessage = "Command exceeds Android intent size limits"
            )
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
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", trimmedCmd))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workdir)
                if (stdin != null) putExtra("com.termux.RUN_COMMAND_STDIN", stdin)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
                putExtra("com.termux.RUN_COMMAND_LABEL", "Jarvis Assistant Command")
                putExtra("com.termux.RUN_COMMAND_DESCRIPTION", "Command execution requested by Jarvis")
            }

            context.startService(intent)

            val received = withTimeoutOrNull(timeoutMs) { deferred.await() }
            if (received != null) {
                return received
            } else {
                return TermuxCommandResult(
                    executionId = executionId,
                    stdout = "",
                    stderr = "زمان اجرای دستور در ترموکس به پایان رسید (Timeout پس از ${timeoutMs / 1000} ثانیه).",
                    exitCode = 124,
                    timedOut = true,
                    errorMessage = "Timed out waiting for Termux response"
                )
            }
        } catch (e: SecurityException) {
            return TermuxCommandResult(
                executionId = executionId,
                stdout = "",
                stderr = "خطای مجوز Termux: لطفاً دسترسی allow-external-apps=true را در فایل ~/.termux/termux.properties فعال کنید.",
                exitCode = 126,
                errorMessage = e.message
            )
        } catch (e: Exception) {
            return TermuxCommandResult(
                executionId = executionId,
                stdout = "",
                stderr = "خطا در ارسال دستور به ترموکس: ${e.message ?: "خطای ناشناخته"}",
                exitCode = 126,
                errorMessage = e.message
            )
        } finally {
            TermuxResultBroker.cancel(executionId)
        }
    }

    /**
     * Executes a command and formats a clear human-readable Persian/English summary.
     */
    suspend fun executeCommandText(command: String): String {
        val result = executeTermuxCommand(command)
        return buildString {
            if (result.success) {
                append("✅ دستور با موفقیت در ترموکس اجرا شد (Exit Code: 0)")
            } else if (result.timedOut) {
                append("⏱️ اجرای دستور با وقفه زمانی (Timeout) مواجه شد.")
            } else {
                append("❌ اجرای دستور با خطا متوقف شد (Exit Code: ${result.exitCode})")
            }

            if (!result.errorMessage.isNullOrBlank() && !result.success) {
                append("\nعلت خطا: ${result.errorMessage}")
            }
            if (result.stdout.isNotBlank()) {
                append("\n\n📄 خروجی (stdout):\n${result.stdout.trim()}")
            }
            if (result.stderr.isNotBlank()) {
                append("\n\n⚠️ پیام خطا (stderr):\n${result.stderr.trim()}")
            }
            if (result.stdoutTruncated || result.stderrTruncated) {
                append("\n(بخشی از خروجی به دلیل محدودیت طول Intent فشرده شده است)")
            }
        }
    }

    fun isDangerousCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("rm -rf") || lower.contains("rm -r /") ||
            lower.contains("mkfs") || lower.contains("dd if=") ||
            lower.contains(":(){ :|:& };") || lower.contains("format ") ||
            lower.contains("shutdown") || lower.contains("reboot") ||
            lower.contains("chmod -r 777") || lower.contains("wipe")
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
