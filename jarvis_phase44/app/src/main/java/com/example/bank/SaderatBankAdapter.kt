package com.example.bank

import android.content.Context
import android.content.Intent

/**
 * Bank Saderat execution bridge.
 *
 * SAPP is the official card-payment application used for Saderat-origin
 * card-to-card transfers. This bridge launches the installed app and hands
 * the validated transfer workflow to the user-facing SAPP flow.
 *
 * It intentionally does not store credentials/OTP or silently submit the
 * final payment.
 */
class SaderatBankAdapter(private val context: Context) : BankAdapter {
    override val bankName: String = "بانک صادرات ایران — صاپ"
    override val bankCode: String = "603769"

    // SAPP Android package.
    override val packageName: String = "ir.stsepehr.hamrahcard"
    override val isTestMode: Boolean = false

    override fun canHandleBank(cardPrefix: String): Boolean {
        val clean = cardPrefix.filter(Char::isDigit)
        return clean.startsWith("603769")
    }

    fun isAppInstalled(): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }

    fun createLaunchIntent(): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    fun launchSapp(): Boolean {
        val intent = createLaunchIntent() ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
