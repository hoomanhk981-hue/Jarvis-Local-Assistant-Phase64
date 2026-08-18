package com.example.agent

enum class ToolId {
    LOCAL_LLM,
    VISION,
    SPEECH_TO_TEXT,
    TTS,
    APP_LAUNCHER,
    SMS,
    CONTACTS,
    MEMORY,
    WEB_SEARCH,
    DEVICE_ACTIONS,
    FILE_DOCUMENT,
    PERMISSIONS,
    BANKING
}

data class ToolRequest(
    val userText: String,
    val hasImage: Boolean = false,
    val requiresInternet: Boolean = false
)

data class ToolPlan(
    val tools: List<ToolId>,
    val needsConfirmation: Boolean = false,
    val reason: String = ""
)

/**
 * Deterministic first-pass router.
 *
 * The local LLM can refine/confirm this plan, but safety-sensitive tools are
 * never implicitly authorized by classification alone.
 */
class AgentToolRouter {

    fun plan(request: ToolRequest): ToolPlan {
        val text = request.userText.trim().lowercase()

        if (request.hasImage) {
            return ToolPlan(listOf(ToolId.VISION), reason = "Image input detected.")
        }

        if (containsAny(text, "پیامک", "sms", "اس ام اس")) {
            return ToolPlan(listOf(ToolId.SMS), reason = "SMS intent detected.")
        }

        if (containsAny(text, "مخاطب", "کانتکت", "تماس")) {
            return ToolPlan(listOf(ToolId.CONTACTS), reason = "Contact intent detected.")
        }

        if (containsAny(text, "باز کن", "بازکردن", "open") &&
            containsAny(text, "برنامه", "اپ", "app", "application")) {
            return ToolPlan(listOf(ToolId.APP_LAUNCHER), reason = "App-launch intent detected.")
        }

        if (containsAny(text, "فایل", "سند", "pdf", "عکس", "document")) {
            return ToolPlan(listOf(ToolId.FILE_DOCUMENT), reason = "File/document intent detected.")
        }

        if (containsAny(text, "یادم", "به خاطر بسپار", "ذخیره کن", "حافظه")) {
            return ToolPlan(listOf(ToolId.MEMORY), reason = "Memory intent detected.")
        }

        if (containsAny(text, "بانک", "کارت به کارت", "واریز", "انتقال وجه")) {
            return ToolPlan(
                listOf(ToolId.BANKING),
                needsConfirmation = true,
                reason = "Financial intent detected; explicit confirmation required."
            )
        }

        if (request.requiresInternet) {
            return ToolPlan(listOf(ToolId.WEB_SEARCH), reason = "Internet explicitly requested.")
        }

        return ToolPlan(listOf(ToolId.LOCAL_LLM), reason = "General local assistant request.")
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }
}
