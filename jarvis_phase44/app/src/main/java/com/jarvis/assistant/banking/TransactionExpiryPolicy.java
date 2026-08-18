package com.jarvis.assistant.banking;

/**
 * Phase 55: transaction timeout / expiry policy.
 *
 * An approval or execution window cannot remain valid indefinitely.
 * The policy is deterministic and contains no network dependency.
 */
public final class TransactionExpiryPolicy {
    private final long approvalLifetimeMillis;
    private final long executionLifetimeMillis;

    public TransactionExpiryPolicy(long approvalLifetimeMillis,
                                   long executionLifetimeMillis) {
        if (approvalLifetimeMillis <= 0 || executionLifetimeMillis <= 0) {
            throw new IllegalArgumentException("lifetimes must be positive");
        }
        this.approvalLifetimeMillis = approvalLifetimeMillis;
        this.executionLifetimeMillis = executionLifetimeMillis;
    }

    public boolean approvalValid(long approvedAtMillis, long nowMillis) {
        return nowMillis >= approvedAtMillis
                && nowMillis - approvedAtMillis <= approvalLifetimeMillis;
    }

    public boolean executionWindowValid(long startedAtMillis, long nowMillis) {
        return nowMillis >= startedAtMillis
                && nowMillis - startedAtMillis <= executionLifetimeMillis;
    }

    public long getApprovalLifetimeMillis() {
        return approvalLifetimeMillis;
    }

    public long getExecutionLifetimeMillis() {
        return executionLifetimeMillis;
    }
}
