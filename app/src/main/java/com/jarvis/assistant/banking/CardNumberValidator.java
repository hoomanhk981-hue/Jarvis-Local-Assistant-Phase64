package com.jarvis.assistant.banking;

/**
 * Phase 59: strict card-number validation.
 *
 * Validates formatting and checksum locally. It does not store, transmit,
 * or reveal the full card number.
 */
public final class CardNumberValidator {
    private CardNumberValidator() {}

    public static boolean isValid(String raw) {
        if (raw == null) return false;

        String digits = raw.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{16}")) return false;

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (doubleDigit) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    public static String masked(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{16}")) return "****";
        return "****-****-****-" + digits.substring(12);
    }
}
