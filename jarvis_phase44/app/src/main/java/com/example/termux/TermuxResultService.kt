package com.example.termux

import android.app.IntentService
import android.content.Intent
import android.os.Bundle

/** Receives asynchronous RUN_COMMAND results from Termux. */
class TermuxResultService : IntentService("JarvisTermuxResultService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val bundle = intent.getBundleExtra(EXTRA_RESULT_BUNDLE) ?: return
        val executionId = intent.getIntExtra(EXTRA_EXECUTION_ID, -1)
        if (executionId < 0) return

        val stdout = bundle.getString(EXTRA_STDOUT, "")
        val stderr = bundle.getString(EXTRA_STDERR, "")
        val stdoutOriginal = bundle.getString(EXTRA_STDOUT_ORIGINAL_LENGTH)?.toLongOrNull()
        val stderrOriginal = bundle.getString(EXTRA_STDERR_ORIGINAL_LENGTH)?.toLongOrNull()
        val exitCode = bundle.getInt(EXTRA_EXIT_CODE, -1)
        val errorCode = if (bundle.containsKey(EXTRA_ERR)) bundle.getInt(EXTRA_ERR) else null
        val errorMessage = bundle.getString(EXTRA_ERRMSG)

        TermuxResultBroker.complete(
            TermuxCommandResult(
                executionId = executionId,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                errorCode = errorCode?.takeUnless { it == -1 },
                errorMessage = errorMessage,
                stdoutTruncated = stdoutOriginal != null && stdoutOriginal > stdout.length,
                stderrTruncated = stderrOriginal != null && stderrOriginal > stderr.length
            )
        )
    }

    companion object {
        const val EXTRA_EXECUTION_ID = "jarvis_execution_id"
        const val EXTRA_RESULT_BUNDLE = "com.termux.plugin_result_bundle"
        const val EXTRA_STDOUT = "stdout"
        const val EXTRA_STDERR = "stderr"
        const val EXTRA_EXIT_CODE = "exitCode"
        const val EXTRA_ERR = "err"
        const val EXTRA_ERRMSG = "errmsg"
        const val EXTRA_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"
        const val EXTRA_STDERR_ORIGINAL_LENGTH = "stderr_original_length"
    }
}
