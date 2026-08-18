package com.example.bank

/** Small registry-independent service used by the Agent layer. */
class BankTool(private val adapter: BankAdapter) {
    fun prepare(request: TransferRequest): Result<String> {
        val validation = BankTransferWorkflow(adapter).validate(request)
        if (!validation.ok) return Result.failure(IllegalArgumentException(validation.errors.joinToString(" ")))
        return Result.success(BankTransferWorkflow(adapter).confirmationText(request))
    }
}
