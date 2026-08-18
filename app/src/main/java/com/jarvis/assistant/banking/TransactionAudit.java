package com.jarvis.assistant.banking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phase 51: local, non-sensitive transaction audit trail.
 *
 * Stores state/event names only; callers must not put card numbers, CVV2,
 * passwords, OTPs, or other secrets into event details.
 */
public final class TransactionAudit {
    public static final class Event {
        private final String transactionId;
        private final String type;
        private final long timestampMillis;

        public Event(String transactionId, String type, long timestampMillis) {
            this.transactionId = transactionId;
            this.type = type;
            this.timestampMillis = timestampMillis;
        }

        public String getTransactionId() { return transactionId; }
        public String getType() { return type; }
        public long getTimestampMillis() { return timestampMillis; }
    }

    private final List<Event> events = new ArrayList<>();

    public void record(String transactionId, String type, long timestampMillis) {
        if (transactionId == null || transactionId.trim().isEmpty()
                || type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("transactionId and type required");
        }
        events.add(new Event(transactionId, type, timestampMillis));
    }

    public List<Event> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
