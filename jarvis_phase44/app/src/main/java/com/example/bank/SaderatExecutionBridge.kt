package com.example.bank

import android.content.Context

/**
 * Connects the already validated Jarvis banking workflow to SAPP.
 *
 * The bridge opens SAPP after the Jarvis review/approval gate. It never
 * bypasses SAPP authentication, OTP, or the final payment confirmation.
 */
class SaderatExecutionBridge(private val context: Context) {
    private val adapter = SaderatBankAdapter(context)

    data class LaunchResult(
        val ok: Boolean,
        val message: String
    )

    fun startApprovedFlow(): LaunchResult {
        if (!adapter.isAppInstalled()) {
            return LaunchResult(
                false,
                "صاپ روی دستگاه نصب نیست."
            )
        }

        if (!adapter.launchSapp()) {
            return LaunchResult(
                false,
                "امکان باز کردن صاپ وجود ندارد."
            )
        }

        return LaunchResult(
            true,
            "صاپ باز شد؛ ادامه تراکنش باید داخل محیط رسمی صاپ و با تأیید کاربر انجام شود."
        )
    }
}
