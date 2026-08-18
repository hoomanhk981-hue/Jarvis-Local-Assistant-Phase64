package com.jarvis.assistant.banking;

import java.util.Objects;

/**
 * Phase 49: final user-approval token bound to the exact transaction snapshot.
 *
 * A confirmation is not reusable after transaction data changes.
 * This class is only an authorization record; it does not execute a payment.
 */
public final class FinalApproval {
    private final String transactionId;
    private final String snapshotFingerprint;
    private final long approvedAtMillis;
    private boolean consumed;

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public FinalApproval(String transactionId, String snapshotFingerprint, long approvedAtMillis) {
        if (isBlank(transactionId) || isBlank(snapshotFingerprint)) {
            throw new IllegalArgumentException("transactionId and fingerprint required");
        }
        this.transactionId = transactionId;
        this.snapshotFingerprint = snapshotFingerprint;
        this.approvedAtMillis = approvedAtMillis;
    }

    public boolean matches(String transactionId, String currentFingerprint, long nowMillis,
                           long maxAgeMillis) {
        return !consumed
                && Objects.equals(this.transactionId, transactionId)
                && Objects.equals(this.snapshotFingerprint, currentFingerprint)
                && nowMillis >= approvedAtMillis
                && nowMillis - approvedAtMillis <= maxAgeMillis;
    }

    public void consume() {
        if (consumed) throw new IllegalStateException("approval already consumed");
        consumed = true;
    }

    public boolean isConsumed() { return consumed; }
    public String getTransactionId() { return transactionId; }
}
