package com.jarvis.assistant.banking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 47: immutable review snapshot for a transaction.
 * Sensitive values should be redacted by the caller before constructing the snapshot.
 */
public final class TransactionReviewSnapshot {
    private final String transactionId;
    private final long createdAtMillis;
    private final Map<String, String> fields;

    public TransactionReviewSnapshot(String transactionId,
                                     long createdAtMillis,
                                     Map<String, String> fields) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("transactionId required");
        }
        if (fields == null) {
            throw new IllegalArgumentException("fields required");
        }
        this.transactionId = transactionId;
        this.createdAtMillis = createdAtMillis;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public String getTransactionId() { return transactionId; }
    public long getCreatedAtMillis() { return createdAtMillis; }
    public Map<String, String> getFields() { return fields; }

    /** Review must be explicit after an interrupted/unknown state. */
    public boolean isReviewable(long nowMillis, long maxAgeMillis) {
        return nowMillis >= createdAtMillis
                && nowMillis - createdAtMillis <= maxAgeMillis;
    }
}
