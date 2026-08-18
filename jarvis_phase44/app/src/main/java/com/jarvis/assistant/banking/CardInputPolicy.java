package com.jarvis.assistant.banking;

/**
 * Phase 62: safe card-input policy.
 *
 * The UI can validate and mask a card number while avoiding persistence of
 * raw card data. CVV2 is intentionally never represented by this class.
 */
public final class CardInputPolicy {
    private CardInputPolicy() {}

    public static String sanitizeCard(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9]", "");
    }

    public static String displayCard(String raw) {
        String digits = sanitizeCard(raw);
        if (digits.length() != 16) return "****-****-****-****";
        return digits.substring(0, 4) + "-****-****-" + digits.substring(12);
    }

    public static boolean readyForValidation(String raw) {
        return sanitizeCard(raw).length() == 16;
    }
}
