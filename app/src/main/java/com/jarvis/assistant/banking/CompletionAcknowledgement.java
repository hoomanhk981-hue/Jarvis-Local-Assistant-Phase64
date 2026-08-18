package com.jarvis.assistant.banking;

/**
 * Phase 50: explicit transaction completion acknowledgement.
 *
 * The user, not the model, declares whether the external banking UI showed
 * success or failure. The result is tied to the transaction ID and cannot
 * silently change an UNKNOWN transaction into SUCCESS.
 */
public final class CompletionAcknowledgement {
    public enum Result { USER_CONFIRMED_SUCCESS, USER_CONFIRMED_FAILURE }

    private final String transactionId;
    private final Result result;
    private final long acknowledgedAtMillis;

    public CompletionAcknowledgement(String transactionId, Result result, long acknowledgedAtMillis) {
        if (transactionId == null || transactionId.trim().isEmpty() || result == null) {
            throw new IllegalArgumentException("transactionId and result required");
        }
        this.transactionId = transactionId;
        this.result = result;
        this.acknowledgedAtMillis = acknowledgedAtMillis;
    }

    public String getTransactionId() { return transactionId; }
    public Result getResult() { return result; }
    public long getAcknowledgedAtMillis() { return acknowledgedAtMillis; }
}
