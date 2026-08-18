package com.example.assistant

import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import org.junit.Assert.*
import org.junit.Test

class FuzzyMatcherTest {
    @Test fun normalizesPersianVariants() {
        assertEquals("علی کاظمی", FuzzyMatcher.normalizePersianText("  عَلِي كاظمی "))
    }
    @Test fun returnsAtMostThreeContactCandidates() {
        val contacts=(1..6).map { ContactMatch("$it", "علی $it", "0912$it") }
        assertTrue(FuzzyMatcher.findTop3Contacts("علی", contacts).size <= 3)
    }
    @Test fun exactAppMatchWins() {
        val apps=listOf(InstalledAppInfo("Telegram","org.telegram.messenger"),InstalledAppInfo("Chrome","com.android.chrome"))
        assertEquals("org.telegram.messenger", FuzzyMatcher.findBestMatchingApp("تلگرام", apps)?.packageName)
    }
}
