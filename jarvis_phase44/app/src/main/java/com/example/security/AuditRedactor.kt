package com.example.security

/** Redacts high-risk values before they can enter local action-history/audit text. */
object AuditRedactor {
    private val card = Regex("(?<!\\d)(?:\\d[ -]?){15}\\d(?!\\d)")
    private val otp = Regex("(?i)(?:رمز(?:\\s+پویا)?|otp|one[- ]?time|verification)\\s*[:：-]?\\s*\\d{4,8}")
    private val cvv = Regex("(?i)(?:cvv2|cvv|cvc)\\s*[:：-]?\\s*\\d{3,4}")
    private val password = Regex("(?i)(?:password|passcode|رمز(?: عبور)?)\\s*[:：-]?\\S+")

    fun redact(input: String): String = input
        .replace(card, "[CARD_PROTECTED]")
        .replace(otp, "[OTP_PROTECTED]")
        .replace(cvv, "[CVV_PROTECTED]")
        .replace(password, "[PASSWORD_PROTECTED]")
}
