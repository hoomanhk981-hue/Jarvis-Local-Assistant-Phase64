package com.example.bank

/**
 * Produces deterministic UI actions for the currently installed Saderat app.
 * It never presses the final payment action itself; that action remains behind
 * the application's central user-confirmation gate.
 */
class SaderatTransferAccessibilityPlan {
    enum class Step { OPEN_TRANSFER, SOURCE, DESTINATION, AMOUNT, REVIEW, PAYMENT, OTP, RESULT }

    data class Action(
        val step: Step,
        val aliases: List<String>,
        val sensitive: Boolean,
        val requiresUserConfirmation: Boolean
    )

    fun actionFor(step: Step): Action = when (step) {
        Step.OPEN_TRANSFER -> Action(step, SaderatTransferUiContract.transferMenuAliases, false, false)
        Step.SOURCE -> Action(step, SaderatTransferUiContract.sourceAliases, true, false)
        Step.DESTINATION -> Action(step, SaderatTransferUiContract.destinationAliases, true, false)
        Step.AMOUNT -> Action(step, SaderatTransferUiContract.amountAliases, true, false)
        Step.REVIEW -> Action(step, listOf("بررسی", "بازبینی", "Review", "خلاصه"), true, true)
        Step.PAYMENT -> Action(step, SaderatTransferUiContract.paymentAliases, true, true)
        Step.OTP -> Action(step, SaderatTransferUiContract.otpAliases, true, true)
        Step.RESULT -> Action(step, listOf("موفق", "انجام شد", "تراکنش", "رسید", "نتیجه", "Success", "Receipt"), false, false)
    }

    fun next(step: Step): Step? = when (step) {
        Step.OPEN_TRANSFER -> Step.SOURCE
        Step.SOURCE -> Step.DESTINATION
        Step.DESTINATION -> Step.AMOUNT
        Step.AMOUNT -> Step.REVIEW
        Step.REVIEW -> Step.PAYMENT
        Step.PAYMENT -> Step.OTP
        Step.OTP -> Step.RESULT
        Step.RESULT -> null
    }
}
