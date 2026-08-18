package com.jarvis.assistant.banking;

/**
 * Phase 54: idempotency guard for payment actions.
 *
 * A single approved transaction/action key can be consumed once. Repeated
 * agent retries therefore cannot accidentally request the same action twice.
 */
public final class PaymentIdempotencyGuard {
    private String consumedKey;

    public boolean canExecute(String actionKey) {
        return actionKey != null
                && !actionKey.trim().isEmpty()
                && !actionKey.equals(consumedKey);
    }

    public void markExecuted(String actionKey) {
        if (!canExecute(actionKey)) {
            throw new IllegalStateException("action key already consumed or invalid");
        }
        consumedKey = actionKey;
    }

    public boolean wasExecuted(String actionKey) {
        return actionKey != null && actionKey.equals(consumedKey);
    }
}
