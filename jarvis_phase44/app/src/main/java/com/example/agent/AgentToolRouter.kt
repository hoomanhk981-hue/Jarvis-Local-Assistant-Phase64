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
    TERMUX,
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
 * Deterministic first-pass router for natural language user requests.
 */
class AgentToolRouter {

    fun plan(request: ToolRequest): ToolPlan {
        val text = request.userText.trim().lowercase()

        if (request.hasImage || containsAny(text, "عکس", "تصویر", "این چیه", "عکس رو ببین", "photo", "image")) {
            return ToolPlan(listOf(ToolId.VISION), reason = "Image/Vision intent detected.")
        }

        if (containsAny(text, "پیامک", "sms", "اس ام اس", "پیام ها", "پیام‌ها", "پیامم", "صندوق پیام")) {
            return ToolPlan(listOf(ToolId.SMS), reason = "SMS intent detected.")
        }

        if (containsAny(text, "زنگ بزن", "تماس بگیر", "تماس با", "شماره بگیر", "مخاطب", "کانتکت", "call", "dial")) {
            return ToolPlan(listOf(ToolId.CONTACTS), reason = "Contact/Calling intent detected.")
        }

        if (containsAny(text, "ترموکس", "termux", "دستور اجرا", "کامند", "ترمینال", "python --version", "pkg install")) {
            return ToolPlan(listOf(ToolId.TERMUX), reason = "Termux execution intent detected.")
        }

        if (containsAny(text, "باز کن", "بازش کن", "رو باز کن", "برو تو", "برو توی", "اجرا کن", "رو بیار", "open", "launch")) {
            return ToolPlan(listOf(ToolId.APP_LAUNCHER), reason = "App-launch intent detected.")
        }

        if (containsAny(text, "فایل", "سند", "pdf", "پوشه", "workspace", "document", "فایل متنی")) {
            return ToolPlan(listOf(ToolId.FILE_DOCUMENT), reason = "File/document intent detected.")
        }

        if (containsAny(text, "یادم", "به خاطر بسپار", "یادداشت کن", "رمز", "پسورد", "ذخیره کن", "چیه رمز", "رمز من")) {
            return ToolPlan(listOf(ToolId.MEMORY), reason = "Memory/Vault intent detected.")
        }

        if (containsAny(text, "بانک", "کارت به کارت", "واریز", "انتقال وجه", "transfer", "صادرات")) {
            return ToolPlan(
                listOf(ToolId.BANKING),
                needsConfirmation = true,
                reason = "Financial intent detected; explicit user confirmation required."
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
