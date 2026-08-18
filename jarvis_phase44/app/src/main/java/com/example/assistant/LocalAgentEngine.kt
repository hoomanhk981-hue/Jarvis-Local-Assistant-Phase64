package com.example.assistant

import android.content.Context
import com.example.data.local.entities.DownloadedModelEntity
import org.json.JSONObject

/**
 * Small, deterministic agent loop around the local LLM.
 * The model emits exactly one JSON action; Android executes it through ToolRegistry;
 * the tool result is fed back to the model for the final user-facing response.
 */
class LocalAgentEngine(private val context: Context) {
    suspend fun run(
        input: String,
        model: DownloadedModelEntity,
        speedMode: String,
        generate: suspend (DownloadedModelEntity, String, String) -> String,
        registry: ToolRegistry,
        maxSteps: Int = 3
    ): String {
        var transcript = "User: $input"
        repeat(maxSteps) { step ->
            val prompt = buildPrompt(transcript, registry.descriptions(), step)
            val raw = generate(model, prompt, speedMode)
            val call = parseCall(raw) ?: return raw.trim()
            if (call.name == "none") return call.finalText.ifBlank { raw.trim() }

            val result = registry.execute(call.name, call.args)
            when (result) {
                is ToolResult.NeedsConfirmation -> {
                    val id = result.confirmationId.orEmpty()
                    return "⚠️ برای انجام این عملیات تأیید کاربر لازم است.\n${result.summary}\nشناسه: $id\nلطفاً از رابط تأیید Jarvis استفاده کنید."
                }
                is ToolResult.NeedsPermission -> {
                    return "🔐 برای ${result.tool} این دسترسی‌های اندروید لازم است: ${result.permissions.joinToString() }"
                }
                is ToolResult.Failure -> transcript += "\nTool ${call.name} failed: ${result.message}"
                is ToolResult.Success -> transcript += "\nTool ${call.name} succeeded: ${result.message} ${result.data}"
            }

            if (result is ToolResult.Failure) return@repeat
            val finalPrompt = """
You are Jarvis, a truthful private Android assistant.
Respond to the user in the same language as the user.
A real Android tool just returned this result:
${when(result) {
    is ToolResult.Success -> result.message + " " + result.data
    is ToolResult.Failure -> result.message
    is ToolResult.NeedsConfirmation -> result.summary
    is ToolResult.NeedsPermission -> "Permissions needed: " + result.permissions.joinToString()
}}
User request: $input
Give a concise final response. Never claim an action succeeded unless the tool result says it succeeded.
""".trimIndent()
            return generate(model, finalPrompt, speedMode).trim()
        }
        return "نتوانستم عملیات را در محدوده امن و مشخص اجرا کنم."
    }

    private fun buildPrompt(transcript: String, tools: List<String>, step: Int): String = """
You are Jarvis, a local Android agent. This is tool-planning step $step.
Available tools:
${tools.joinToString("\n")}

$transcript

If an Android action is needed, output ONLY one JSON object:
{"tool":"tool_name","args":{"key":"value"}}
If no tool is needed, output ONLY:
{"tool":"none","final":"your answer"}
Do not invent tool names or arguments. Do not claim an action happened before a tool reports success.
Never emit confirmed=true yourself unless the Android confirmation system has already returned it.
For actions that modify device state, communicate externally, expose secrets, or execute code, let the tool/confirmation gate decide.
For accessibility actions, inspect first when you do not know the current UI.
Use at most one tool call per planning step. If a tool fails, use its real error to choose the next step; never fabricate recovery.
""".trimIndent()

    private data class Call(val name: String, val args: Map<String, String>, val finalText: String = "")

    private fun parseCall(raw: String): Call? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val o = JSONObject(raw.substring(start, end + 1))
            val name = o.optString("tool", "none")
            val argsJson = o.optJSONObject("args")
            val args = mutableMapOf<String, String>()
            if (argsJson != null) {
                argsJson.keys().forEach { key -> args[key] = argsJson.optString(key) }
            }
            Call(name, args, o.optString("final", ""))
        } catch (_: Exception) { null }
    }
}
