package com.example.bank

class SaderatTestAdapter : BankAdapter {
    override val bankName: String = "[TEST MODE] بانک صادرات (محیط تست دولوپر)"
    override val bankCode: String = "603769"
    override val packageName: String = "com.example.test.saderat"
    override val isTestMode: Boolean = true

    override fun canHandleBank(cardPrefix: String): Boolean {
        val clean = cardPrefix.replace(Regex("[^0-9]"), "")
        return clean.startsWith("6037")
    }

    /**
     * Executes simulated verification solely for developer testing environments.
     * All output is explicitly tagged with [TEST MODE].
     */
    fun executeTestTransfer(destCard: String, amountRials: Long, otp: String): BankTransferStatus {
        if (!validateCardNumber(destCard)) {
            return BankTransferStatus.TransferFailed("ERR_INVALID_CARD", "[TEST MODE] شماره کارت مقصد ۱۶ رقمی معتبر نیست.")
        }
        if (amountRials <= 0) {
            return BankTransferStatus.TransferFailed("ERR_INVALID_AMOUNT", "[TEST MODE] مبلغ وارد شده باید بیشتر از صفر باشد.")
        }
        if (otp.length < 5) {
            return BankTransferStatus.TransferFailed("ERR_INVALID_OTP", "[TEST MODE] رمز پویا وارد نشده یا منقضی شده است.")
        }

        val refCode = "TEST-REF-" + (100000..999999).random()
        return BankTransferStatus.TransferSuccess(
            trackingCode = refCode,
            message = "[TEST MODE - تراکنش آزمایشی]: انتقال مبلغ ${formatAmountToman(amountRials)} به کارت ${formatCardNumber(destCard)} در محیط تستی با کد پیگیری $refCode شبیه‌سازی شد."
        )
    }
}
