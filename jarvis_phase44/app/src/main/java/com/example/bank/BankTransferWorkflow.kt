package com.example.bank

/**
 * Deterministic, field-by-field banking workflow. This class never performs a
 * financial transaction itself; it prepares and validates the exact payload
 * that must be shown to the user before UI automation can continue.
 */
data class TransferRequest(
    val destinationCard: String,
    val amountRials: Long,
    val sourceCard: String? = null,
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
    val cvv2: String? = null,
    val recipientName: String? = null
)

data class TransferValidation(
    val ok: Boolean,
    val errors: List<String>,
    val normalizedDestination: String,
    val normalizedSource: String?
)

class BankTransferWorkflow(private val adapter: BankAdapter) {
    fun validate(request: TransferRequest): TransferValidation {
        val errors = mutableListOf<String>()
        val destination = request.destinationCard.filter(Char::isDigit)
        val source = request.sourceCard?.filter(Char::isDigit)

        if (!adapter.validateCardNumber(destination)) {
            errors += "شماره کارت مقصد معتبر نیست."
        }
        if (request.amountRials <= 0L) {
            errors += "مبلغ باید بیشتر از صفر باشد."
        }
        if (request.amountRials > 500_000_000_000L) {
            errors += "مبلغ از سقف ایمن تعریف‌شده در Jarvis بیشتر است."
        }
        if (source != null && !adapter.validateCardNumber(source)) {
            errors += "شماره کارت مبدأ معتبر نیست."
        }
        if (request.expiryMonth != null && request.expiryMonth !in 1..12) {
            errors += "ماه انقضای کارت باید بین 1 تا 12 باشد."
        }
        if (request.expiryYear != null && request.expiryYear !in 0..99) {
            errors += "سال انقضای کارت باید دو رقمی باشد."
        }
        if (request.cvv2 != null && request.cvv2.filter(Char::isDigit).length !in 3..4) {
            errors += "CVV2 باید 3 یا 4 رقم باشد."
        }

        return TransferValidation(
            ok = errors.isEmpty(),
            errors = errors,
            normalizedDestination = destination,
            normalizedSource = source
        )
    }

    fun confirmationText(request: TransferRequest): String {
        val validation = validate(request)
        return buildString {
            append("بانک: ${adapter.bankName}\n")
            append("مقصد: ${adapter.formatCardNumber(validation.normalizedDestination)}\n")
            if (!request.recipientName.isNullOrBlank()) append("گیرنده: ${request.recipientName}\n")
            append("مبلغ: ${adapter.formatAmountToman(request.amountRials)}\n")
            append("این اطلاعات فقط برای تأیید کاربر نمایش داده می‌شوند. اجرای انتقال منوط به تأیید صریح کاربر است.")
        }
    }
}
