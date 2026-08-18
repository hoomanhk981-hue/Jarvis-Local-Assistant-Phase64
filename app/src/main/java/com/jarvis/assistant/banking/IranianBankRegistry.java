package com.jarvis.assistant.banking;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Phase 58: local Iranian-bank profile registry.
 * Selection metadata only; no credentials or payment execution.
 */
public final class IranianBankRegistry {
    public static final class BankProfile {
        private final String id;
        private final String displayName;

        public BankProfile(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
    }

    private static final List<BankProfile> BANKS =
            Collections.unmodifiableList(Arrays.asList(
                    new BankProfile("melli", "بانک ملی ایران"),
                    new BankProfile("mellat", "بانک ملت"),
                    new BankProfile("saderat", "بانک صادرات ایران"),
                    new BankProfile("tejarat", "بانک تجارت"),
                    new BankProfile("refah", "بانک رفاه کارگران"),
                    new BankProfile("saman", "بانک سامان"),
                    new BankProfile("pasargad", "بانک پاسارگاد"),
                    new BankProfile("parsian", "بانک پارسیان"),
                    new BankProfile("eghtesad_novin", "بانک اقتصاد نوین"),
                    new BankProfile("shahr", "بانک شهر")
            ));

    private IranianBankRegistry() {}

    public static List<BankProfile> all() { return BANKS; }

    public static BankProfile findById(String id) {
        if (id == null) return null;
        for (BankProfile bank : BANKS)
            if (bank.id.equals(id)) return bank;
        return null;
    }
}
