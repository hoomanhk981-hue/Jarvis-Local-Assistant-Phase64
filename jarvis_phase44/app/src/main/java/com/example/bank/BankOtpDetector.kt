package com.example.bank

/**
 * Local-only detector for bank OTP SMS messages.
 * It extracts a short numeric OTP without sending SMS contents anywhere.
 * The detector never chooses an OTP solely by age: callers must provide the
 * SMS timestamp and may enforce their own freshness window.
 */
object BankOtpDetector {
    private val codePatterns = listOf(
        Regex("(?i)(?:رمز|کد|otp|one[- ]?time|verification)[^0-9]{0,24}([0-9]{4,8})"),
        Regex("(?<![0-9])([0-9]{5,8})(?![0-9])")
    )

    data class Candidate(val code: String, val score: Int, val reason: String)

    fun detect(message: String, sender: String? = null): List<Candidate> {
        val normalized = message.replace('۰','0').replace('۱','1').replace('۲','2')
            .replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6')
            .replace('۷','7').replace('۸','8').replace('۹','9')
        val senderBoost = if (sender.orEmpty().contains("bank", true) || sender.orEmpty().contains("بانک")) 10 else 0
        val candidates = linkedMapOf<String, Candidate>()
        codePatterns.forEachIndexed { index, regex ->
            regex.findAll(normalized).forEach { match ->
                val code = match.groupValues[1]
                val score = (if (index == 0) 80 else 45) + senderBoost
                candidates[code] = Candidate(code, score, if (index == 0) "otp-keyword" else "numeric-candidate")
            }
        }
        return candidates.values.sortedByDescending { it.score }
    }
}
