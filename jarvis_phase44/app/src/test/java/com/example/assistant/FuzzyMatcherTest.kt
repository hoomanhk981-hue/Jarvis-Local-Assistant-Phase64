package com.example.assistant

import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import org.junit.Assert.*
import org.junit.Test

class FuzzyMatcherTest {
    @Test
    fun normalizesPersianVariants() {
        assertEquals("علی کاظمی", FuzzyMatcher.normalizePersianText("  عَلِي كاظمی "))
        assertEquals("بهترین پدر", FuzzyMatcher.normalizePersianText("بهترین پدر ❤️"))
        assertEquals("09121234567", FuzzyMatcher.normalizePhoneNumber("+989121234567"))
        assertEquals("09121234567", FuzzyMatcher.normalizePhoneNumber("۰۹۱۲۱۲۳۴۵۶۷"))
    }

    @Test
    fun returnsAtMostThreeContactCandidates() {
        val contacts = listOf(
            ContactMatch("1", "بهترین پدر ❤️", "09121111111"),
            ContactMatch("2", "پدر دوستم", "09122222222"),
            ContactMatch("3", "پدر عشق", "09123333333"),
            ContactMatch("4", "محمد رضایی", "09124444444")
        )
        val top3 = FuzzyMatcher.findTop3Contacts("بهترین پدر", contacts)
        assertTrue(top3.size in 1..3)
        assertEquals("1", top3[0].contactId)
        assertEquals("بهترین پدر ❤️", top3[0].displayName)
    }

    @Test
    fun matchesContactByPhoneNumber() {
        val contacts = listOf(
            ContactMatch("1", "رضا احمدی", "09123456789"),
            ContactMatch("2", "علی حسینی", "09351234567")
        )
        val result = FuzzyMatcher.findTop3Contacts("09123456789", contacts)
        assertEquals(1, result.size)
        assertEquals("رضا احمدی", result[0].displayName)
    }

    @Test
    fun exactAppMatchWins() {
        val apps = listOf(
            InstalledAppInfo("WhatsApp", "com.whatsapp"),
            InstalledAppInfo("Telegram", "org.telegram.messenger"),
            InstalledAppInfo("Chrome", "com.android.chrome"),
            InstalledAppInfo("Termux", "com.termux")
        )
        assertEquals("com.whatsapp", FuzzyMatcher.findBestMatchingApp("واتساپ", apps)?.packageName)
        assertEquals("com.whatsapp", FuzzyMatcher.findBestMatchingApp("واتس اپ", apps)?.packageName)
        assertEquals("org.telegram.messenger", FuzzyMatcher.findBestMatchingApp("تلگرام", apps)?.packageName)
        assertEquals("com.termux", FuzzyMatcher.findBestMatchingApp("ترموکس", apps)?.packageName)
    }
}
