package com.jarvis.assistant.banking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 46: deterministic recovery/rollback state for interrupted transactions.
 * No network or payment API is used here.
 */
public final class TransactionRecovery {
    public enum State {
        CREATED, CONFIRMED, EXECUTING, OTP_PENDING, VERIFYING,
        SUCCESS, UNKNOWN, FAILED, CANCELLED
    }

    private final String transactionId;
    private State state;
    private final List<State> history = new ArrayList<>();

    public TransactionRecovery(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("transactionId required");
        }
        this.transactionId = transactionId;
        transition(State.CREATED);
    }

    public String getTransactionId() { return transactionId; }
    public State getState() { return state; }
    public List<State> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void confirm() { require(State.CREATED, State.UNKNOWN); transition(State.CONFIRMED); }
    public void startExecution() { require(State.CONFIRMED); transition(State.EXECUTING); }
    public void requestOtp() { require(State.EXECUTING); transition(State.OTP_PENDING); }
    public void startVerification() { require(State.OTP_PENDING, State.EXECUTING); transition(State.VERIFYING); }
    public void markSuccess() { require(State.VERIFYING); transition(State.SUCCESS); }
    public void markUnknown() { require(State.EXECUTING, State.OTP_PENDING, State.VERIFYING); transition(State.UNKNOWN); }
    public void markFailed() { require(State.EXECUTING, State.OTP_PENDING, State.VERIFYING, State.UNKNOWN); transition(State.FAILED); }
    public void cancel() { require(State.CREATED, State.CONFIRMED, State.UNKNOWN); transition(State.CANCELLED); }

    /** After an interruption, UNKNOWN is the only safe resumable state. */
    public boolean requiresUserReview() {
        return state == State.UNKNOWN || state == State.FAILED;
    }

    private void require(State... allowed) {
        for (State s : allowed) if (state == s) return;
        throw new IllegalStateException("Invalid transition from " + state);
    }

    private void transition(State next) {
        state = next;
        history.add(next);
    }
}
