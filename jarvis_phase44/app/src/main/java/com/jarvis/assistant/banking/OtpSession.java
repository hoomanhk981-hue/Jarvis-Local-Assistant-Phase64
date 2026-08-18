package com.jarvis.assistant.banking;

/**
 * Phase 48: one-shot OTP handling policy.
 *
 * OTP values are deliberately kept only in memory and are invalidated after use
 * or when the transaction changes. This class does not read SMS itself.
 */
public final class OtpSession {
    private final String transactionId;
    private String otp;
    private boolean consumed;

    public OtpSession(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("transactionId required");
        }
        this.transactionId = transactionId;
    }

    public String getTransactionId() { return transactionId; }

    public void setOtp(String value) {
        if (consumed) throw new IllegalStateException("OTP session consumed");
        if (value == null || !value.matches("\\d{4,8}")) {
            throw new IllegalArgumentException("invalid OTP format");
        }
        otp = value;
    }

    public String consume() {
        if (consumed || otp == null) {
            throw new IllegalStateException("OTP unavailable");
        }
        String value = otp;
        otp = null;
        consumed = true;
        return value;
    }

    public void invalidate() {
        otp = null;
        consumed = true;
    }

    public boolean isUsable() {
        return !consumed && otp != null;
    }
}
