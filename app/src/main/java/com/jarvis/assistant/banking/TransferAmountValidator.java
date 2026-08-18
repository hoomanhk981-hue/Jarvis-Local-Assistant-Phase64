package com.jarvis.assistant.banking;

/**
 * Phase 61: local transfer amount validation.
 *
 * Amounts are represented in the smallest supported currency unit to avoid
 * floating-point errors. This class only validates input; it never transfers.
 */
public final class TransferAmountValidator {
    private TransferAmountValidator() {}

    public static boolean isValid(long amount, long minimum, long maximum) {
        return amount > 0 && minimum > 0 && maximum >= minimum
                && amount >= minimum && amount <= maximum;
    }

    public static String normalizeDigits(String raw) {
        if (raw == null) return "";
        return raw
                .replace('\u0660','0').replace('\u0661','1')
                .replace('\u0662','2').replace('\u0663','3')
                .replace('\u0664','4').replace('\u0665','5')
                .replace('\u0666','6').replace('\u0667','7')
                .replace('\u0668','8').replace('\u0669','9')
                .replace('\u06F0','0').replace('\u06F1','1')
                .replace('\u06F2','2').replace('\u06F3','3')
                .replace('\u06F4','4').replace('\u06F5','5')
                .replace('\u06F6','6').replace('\u06F7','7')
                .replace('\u06F8','8').replace('\u06F9','9')
                .replaceAll("[,\\s٬]", "");
    }
}
