package com.example.bank

class SaderatTransferPlanner(private val adapter: BankAdapter) {
    data class Step(val label: String, val aliases: List<String>, val sensitive: Boolean)

    fun buildPlan(request: TransferRequest): Result<List<Step>> {
        val validation = BankTransferWorkflow(adapter).validate(request)
        if (!validation.ok) return Result.failure(IllegalArgumentException(validation.errors.joinToString("\n")))
        return Result.success(listOf(
            Step(SaderatTransferUiContract.TRANSFER_MENU, SaderatTransferUiContract.transferMenuAliases, false),
            Step(SaderatTransferUiContract.SOURCE_FIELD, SaderatTransferUiContract.sourceAliases, true),
            Step(SaderatTransferUiContract.DESTINATION_FIELD, SaderatTransferUiContract.destinationAliases, true),
            Step(SaderatTransferUiContract.AMOUNT_FIELD, SaderatTransferUiContract.amountAliases, true),
            Step(SaderatTransferUiContract.PAYMENT_ACTION, SaderatTransferUiContract.paymentAliases, true),
            Step(SaderatTransferUiContract.OTP_FIELD, SaderatTransferUiContract.otpAliases, true)
        ))
    }

    fun requiresExplicitConfirmation(step: Step): Boolean = step.sensitive
}
