package com.example.assistant

sealed class ToolResult {
    data class Success(val message: String, val data: Map<String, String> = emptyMap()) : ToolResult()
    data class NeedsConfirmation(
        val action: String,
        val summary: String,
        val confirmationId: String? = null,
        val risk: ConfirmationManager.Risk = ConfirmationManager.Risk.MEDIUM,
        val request: ConfirmationManager.Request? = null
    ) : ToolResult()
    data class NeedsPermission(val tool: String, val permissions: List<String>) : ToolResult()
    data class Failure(val message: String, val data: Map<String, String> = emptyMap()) : ToolResult()
}

interface JarvisTool {
    val name: String
    val description: String
    suspend fun execute(arguments: Map<String, String>): ToolResult
}
