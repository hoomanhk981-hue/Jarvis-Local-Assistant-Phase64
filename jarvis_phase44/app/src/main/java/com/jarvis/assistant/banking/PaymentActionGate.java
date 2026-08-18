package com.jarvis.assistant.banking;

/**
 * Phase 53: payment-action capability gate.
 *
 * The agent cannot directly execute a payment. A payment action can only be
 * represented as a request after an explicit, currently-valid user approval.
 */
public final class PaymentActionGate {
    public enum Decision { ALLOW, DENY }

    public Decision evaluate(boolean userApproved,
                             boolean approvalStillValid,
                             boolean transactionUnchanged,
                             boolean sensitiveDataPresent) {
        if (!userApproved) return Decision.DENY;
        if (!approvalStillValid) return Decision.DENY;
        if (!transactionUnchanged) return Decision.DENY;
        if (sensitiveDataPresent) return Decision.DENY;
        return Decision.ALLOW;
    }
}
