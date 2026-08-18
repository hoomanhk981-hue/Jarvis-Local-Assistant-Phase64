package com.example.bank

import com.example.data.models.SmsMessageItem

sealed class BankTransferStatus {
    object Idle : BankTransferStatus()
    data class AwaitingCardConfirmation(val destCard: String, val amountRials: Long, val formattedAmount: String) : BankTransferStatus()
    data class AwaitingOtp(val destCard: String, val amountRials: Long, val bankName: String, val prompt: String) : BankTransferStatus()
    data class TransferExecuting(val progressMessage: String) : BankTransferStatus()
    data class TransferSuccess(val trackingCode: String, val message: String) : BankTransferStatus()
    data class TransferFailed(val errorCode: String, val errorMessage: String) : BankTransferStatus()
    data class TransferUnknown(val reason: String) : BankTransferStatus()
}

interface BankAdapter {
    val bankName: String
    val bankCode: String
    val packageName: String
    val isTestMode: Boolean

    fun validateCardNumber(cardNumber: String): Boolean {
        val clean = cardNumber.replace(Regex("[^0-9]"), "")
        if (clean.length != 16) return false
        // Luhn Algorithm validation
        var sum = 0
        var alternate = false
        for (i in clean.length - 1 downTo 0) {
            var n = clean[i].toString().toInt()
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }

    fun formatCardNumber(card: String): String {
        val clean = card.replace(Regex("[^0-9]"), "")
        return clean.chunked(4).joinToString(" - ")
    }

    fun formatAmountToman(amountRials: Long): String {
        val toman = amountRials / 10
        return String.format("%,d تومان (%,d ریال)", toman, amountRials)
    }

    fun extractOtpFromSms(smsList: List<SmsMessageItem>, maxAgeMillis: Long = 300_000): String? {
        val now = System.currentTimeMillis()
        val recentBankSms = smsList.filter { sms ->
            (now - sms.timestamp) <= maxAgeMillis &&
            (sms.body.contains("رمز پویا") || sms.body.contains("کد تایید") || sms.body.contains("رمز یکبار مصرف") || sms.body.contains("OTP"))
        }

        for (sms in recentBankSms) {
            // Regex for 5-8 digit OTP
            val regex = Regex("\\b\\d{5,8}\\b")
            val match = regex.find(sms.body)
            if (match != null) {
                return match.value
            }
        }
        return null
    }

    fun canHandleBank(cardPrefix: String): Boolean
}
