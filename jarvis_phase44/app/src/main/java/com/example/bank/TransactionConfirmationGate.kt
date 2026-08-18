package com.example.bank

import java.security.MessageDigest
import java.util.UUID

/**
 * Binds a human confirmation to one exact transfer request.
 * No bank submission is performed here.
 */
class TransactionConfirmationGate(private val ttlMillis: Long = 120_000L) {
    data class PendingConfirmation(
        val id: String = UUID.randomUUID().toString(),
        val requestFingerprint: String,
        val createdAtMillis: Long = System.currentTimeMillis(),
        var confirmed: Boolean = false
    )

    fun create(request: TransferRequest): PendingConfirmation =
        PendingConfirmation(requestFingerprint = fingerprint(request))

    fun confirm(pending: PendingConfirmation, request: TransferRequest, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (pending.confirmed) return false
        if (nowMillis - pending.createdAtMillis > ttlMillis) return false
        if (pending.requestFingerprint != fingerprint(request)) return false
        pending.confirmed = true
        return true
    }

    fun fingerprint(request: TransferRequest): String {
        val canonical = listOf(
            request.destinationCard.filter(Char::isDigit),
            request.amountRials.toString(),
            request.sourceCard?.filter(Char::isDigit).orEmpty(),
            request.expiryMonth?.toString().orEmpty(),
            request.expiryYear?.toString().orEmpty(),
            request.cvv2?.filter(Char::isDigit).orEmpty(),
            request.recipientName.orEmpty().trim()
        ).joinToString("|")
        return sha256(canonical)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
