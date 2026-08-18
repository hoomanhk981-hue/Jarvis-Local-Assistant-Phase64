package com.jarvis.assistant.core;

import java.util.Locale;

/**
 * Phase 56: deterministic offline app-name normalization.
 *
 * Keeps common Persian/Arabic variants and whitespace/punctuation differences
 * from causing the launcher to choose an unrelated application.
 */
public final class AppNameNormalizer {
    private AppNameNormalizer() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.trim()
                .replace('\u064A', '\u06CC') // Arabic Yeh -> Persian Yeh
                .replace('\u0649', '\u06CC')
                .replace('\u0643', '\u06A9') // Arabic Kaf -> Persian Kaf
                .replace('\u0640', ' ');      // Tatweel

        s = s.replaceAll("[\\p{Punct}\\p{IsPunctuation}]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s.toLowerCase(Locale.ROOT);
    }

    public static boolean equivalent(String a, String b) {
        return normalize(a).equals(normalize(b));
    }
}
