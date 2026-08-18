package com.example.bank

import java.util.UUID

/**
 * Explicit state machine for high-risk bank transfers.
 * This class deliberately does NOT submit money to a real bank.
 * It coordinates user confirmation, optional test execution, and terminal state.
 */
class BankTransactionStateMachine {
    enum class State { DRAFT, VALIDATED, AWAITING_CONFIRMATION, CONFIRMED, TEST_EXECUTED, FAILED, CANCELLED }

    data class Session(
        val id: String = UUID.randomUUID().toString(),
        val request: TransferRequest,
        var state: State = State.DRAFT,
        var failure: String? = null,
        var testReference: String? = null
    )

    fun validate(session: Session, adapter: BankAdapter): Session {
        val result = BankTransferWorkflow(adapter).validate(session.request)
        session.state = if (result.ok) State.VALIDATED else State.FAILED
        session.failure = result.errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
        if (result.ok) session.state = State.AWAITING_CONFIRMATION
        return session
    }

    fun confirm(session: Session): Session {
        require(session.state == State.AWAITING_CONFIRMATION) { "Transfer is not awaiting confirmation" }
        session.state = State.CONFIRMED
        return session
    }

    fun cancel(session: Session): Session {
        if (session.state != State.TEST_EXECUTED) session.state = State.CANCELLED
        return session
    }
}
