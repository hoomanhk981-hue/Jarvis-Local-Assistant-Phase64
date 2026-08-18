package com.jarvis.assistant.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 57: offline fuzzy app-name matching.
 *
 * Returns ranked candidates from the locally installed-app list. It never
 * invents an application name; callers should require a confidence threshold
 * and user confirmation when the top candidates are close.
 */
public final class AppNameMatcher {
    public static final class Candidate {
        private final String name;
        private final String packageName;
        private final int distance;

        public Candidate(String name, String packageName, int distance) {
            this.name = name;
            this.packageName = packageName;
            this.distance = distance;
        }

        public String getName() { return name; }
        public String getPackageName() { return packageName; }
        public int getDistance() { return distance; }
    }

    private AppNameMatcher() {}

    public static List<Candidate> rank(String query, List<Candidate> installedApps) {
        String q = AppNameNormalizer.normalize(query);
        List<Candidate> result = new ArrayList<>();

        for (Candidate app : installedApps) {
            if (app == null) continue;
            String n = AppNameNormalizer.normalize(app.getName());
            int d = levenshtein(q, n);
            result.add(new Candidate(app.getName(), app.getPackageName(), d));
        }

        result.sort(Comparator.comparingInt(Candidate::getDistance));
        return result;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(
                        Math.min(cur[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }
}
