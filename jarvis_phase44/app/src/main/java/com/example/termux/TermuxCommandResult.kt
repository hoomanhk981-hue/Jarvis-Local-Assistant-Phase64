package com.example.termux

data class TermuxCommandResult(
    val executionId: Int,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val stdoutTruncated: Boolean = false,
    val stderrTruncated: Boolean = false
) {
    val success: Boolean get() = exitCode == 0 && errorCode == null
}
