package com.jarvis.assistant.banking;

/** Phase 63: local transfer-party validation. */
public final class TransferPartiesValidator {
    private TransferPartiesValidator() {}

    public static boolean distinctCards(String source, String destination) {
        String a = digits(source), b = digits(destination);
        return a.length() == 16 && b.length() == 16 && !a.equals(b);
    }

    public static boolean validDestinationIban(String iban) {
        return IranianIbanValidator.isValid(iban);
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
