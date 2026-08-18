package com.example.bank

/**
 * Local verifier for the final result observed by the user/accessibility layer.
 * It intentionally does not infer success from merely reaching a button or
 * screen. A positive result needs an explicit success indicator/reference.
 */
object BankTransferResultVerifier {
    enum class Result { SUCCESS, FAILURE, UNKNOWN }

    data class Observation(
        val screenText: String,
        val reference: String? = null,
        val explicitSuccess: Boolean = false,
        val explicitFailure: Boolean = false
    )

    fun verify(observation: Observation): Result {
        if (observation.explicitFailure) return Result.FAILURE
        if (observation.explicitSuccess && !observation.reference.isNullOrBlank()) return Result.SUCCESS
        val text = observation.screenText.lowercase()
        val failureWords = listOf("خطا", "ناموفق", "لغو شد", "failed", "error", "declined")
        if (failureWords.any(text::contains)) return Result.FAILURE
        return Result.UNKNOWN
    }
}
