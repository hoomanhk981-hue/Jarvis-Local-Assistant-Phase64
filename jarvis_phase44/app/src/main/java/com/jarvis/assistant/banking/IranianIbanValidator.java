package com.jarvis.assistant.banking;

public final class IranianIbanValidator {
    private IranianIbanValidator() {}

    public static boolean isValid(String raw) {
        if (raw == null) return false;
        String iban = normalize(raw);
        if (!iban.matches("IR\\d{24}")) return false;

        String rearranged = iban.substring(4) + "1827" + iban.substring(2, 4);
        int remainder = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            char c = rearranged.charAt(i);
            if (c < '0' || c > '9') return false;
            remainder = (remainder * 10 + c - '0') % 97;
        }
        return remainder == 1;
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static String masked(String raw) {
        String iban = normalize(raw);
        if (!iban.matches("IR\\d{24}")) return "IR**********************";
        return iban.substring(0, 4) + "******************" + iban.substring(22);
    }
}
