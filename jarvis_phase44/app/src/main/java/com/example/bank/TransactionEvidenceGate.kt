package com.example.bank

/**
 * Final evidence gate for a high-risk transfer.
 * A success screen/button alone is not treated as proof. The observed result
 * must be tied to the exact transfer request and contain an explicit success
 * signal plus a bank reference/tracking code.
 */
object TransactionEvidenceGate {
    data class Evidence(
        val destinationCard: String?,
        val amountRials: Long?,
        val trackingCode: String?,
        val explicitSuccess: Boolean,
        val explicitFailure: Boolean,
        val screenText: String = ""
    )

    enum class Decision { SUCCESS, FAILURE, UNKNOWN, MISMATCH }

    fun evaluate(request: TransferRequest, evidence: Evidence): Decision {
        if (evidence.explicitFailure) return Decision.FAILURE
        if (!sameCard(request.destinationCard, evidence.destinationCard)) return Decision.MISMATCH
        if (evidence.amountRials != request.amountRials) return Decision.MISMATCH
        if (evidence.explicitSuccess && !evidence.trackingCode.isNullOrBlank()) return Decision.SUCCESS

        val text = evidence.screenText.lowercase()
        if (listOf("خطا", "ناموفق", "لغو شد", "failed", "error", "declined").any(text::contains)) {
            return Decision.FAILURE
        }
        return Decision.UNKNOWN
    }

    private fun sameCard(a: String, b: String?): Boolean {
        if (b.isNullOrBlank()) return false
        return a.filter(Char::isDigit) == b.filter(Char::isDigit)
    }
}
