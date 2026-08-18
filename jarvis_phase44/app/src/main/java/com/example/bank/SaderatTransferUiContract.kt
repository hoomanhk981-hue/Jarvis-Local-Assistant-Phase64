package com.example.bank

object SaderatTransferUiContract {
    const val TRANSFER_MENU = "انتقال وجه"
    const val SOURCE_FIELD = "انتقال از"
    const val DESTINATION_FIELD = "انتقال به"
    const val AMOUNT_FIELD = "مبلغ"
    const val PAYMENT_ACTION = "پرداخت"
    const val OTP_FIELD = "رمز دوم پویا"

    val transferMenuAliases = listOf("انتقال وجه", "انتقال", "Transfer")
    val sourceAliases = listOf("انتقال از", "مبدأ", "کارت مبدأ", "شماره کارت مبدأ")
    val destinationAliases = listOf("انتقال به", "مقصد", "کارت مقصد", "شماره کارت مقصد")
    val amountAliases = listOf("مبلغ", "مبلغ انتقال", "Amount")
    val paymentAliases = listOf("پرداخت", "تأیید", "تایید", "Pay", "Confirm")
    val otpAliases = listOf("رمز دوم پویا", "رمز پویا", "رمز یکبار مصرف", "OTP")

    val cardToCardCheckpoints = listOf(
        TRANSFER_MENU, SOURCE_FIELD, DESTINATION_FIELD,
        AMOUNT_FIELD, PAYMENT_ACTION, OTP_FIELD
    )
}
