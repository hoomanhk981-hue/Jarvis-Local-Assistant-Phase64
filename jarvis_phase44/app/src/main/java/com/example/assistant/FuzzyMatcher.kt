package com.example.assistant

import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import kotlin.math.min

object FuzzyMatcher {

    /**
     * Normalizes Persian/Arabic strings by standardizing characters, removing diacritics and extra spaces.
     */
    fun normalizePersianText(input: String): String {
        return input.trim().lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('آ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('\u200c', ' ') // Zero-width non-joiner to space
            .replace(Regex("[\\p{Mn}]"), "") // Remove diacritics
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Finds top contact matches based on input name or number.
     */
    fun findTop3Contacts(query: String, allContacts: List<ContactMatch>): List<ContactMatch> {
        val normQuery = normalizePersianText(query)
        if (normQuery.isEmpty() || allContacts.isEmpty()) return emptyList()

        val scored = allContacts.map { contact ->
            val normName = normalizePersianText(contact.displayName)
            val cleanPhone = contact.phoneNumber.replace(Regex("[^0-9+]"), "")

            var score = 0.0f
            // Exact match
            if (normName == normQuery) {
                score = 1.0f
            } else if (normName.startsWith(normQuery) || normName.endsWith(normQuery)) {
                score = 0.95f
            } else if (normName.contains(normQuery) || normQuery.contains(normName)) {
                score = 0.88f
            } else if (cleanPhone.contains(normQuery.replace(Regex("[^0-9+]"), ""))) {
                score = 0.9f
            } else {
                // Token-based matching (e.g. searching for "علی" matches "علی محمدی")
                val queryTokens = normQuery.split(" ").filter { it.isNotBlank() }
                val nameTokens = normName.split(" ").filter { it.isNotBlank() }
                val tokenMatches = queryTokens.count { qTok -> nameTokens.any { nTok -> nTok.contains(qTok) || calculateSimilarity(qTok, nTok) > 0.8f } }
                if (tokenMatches > 0) {
                    score = 0.85f * (tokenMatches.toFloat() / queryTokens.size)
                } else {
                    score = calculateSimilarity(normQuery, normName)
                }
            }

            contact.copy(matchScore = score)
        }

        return scored
            .filter { it.matchScore >= 0.45f }
            .sortedByDescending { it.matchScore }
            .take(3)
    }

    /**
     * Map of Persian/English aliases to standard keywords and package names
     */
    private val appAliases = mapOf(
        "واتساپ" to listOf("whatsapp", "com.whatsapp"),
        "واتس اپ" to listOf("whatsapp", "com.whatsapp"),
        "واتس‌اپ" to listOf("whatsapp", "com.whatsapp"),
        "تلگرام" to listOf("telegram", "org.telegram.messenger", "org.telegram.plus"),
        "اینستا" to listOf("instagram", "com.instagram.android"),
        "اینستاگرام" to listOf("instagram", "com.instagram.android"),
        "روبیکا" to listOf("rubika", "ir.resaneh1.iptv", "ir.rubika"),
        "ایتا" to listOf("eitaa", "ir.eitaa.messenger"),
        "بله" to listOf("bale", "ir.ble.messenger"),
        "سروش" to listOf("soroush", "mobi.mmdt.ott"),
        "دیجیکالا" to listOf("digikala", "com.digikala.mobile"),
        "دیجی کالا" to listOf("digikala", "com.digikala.mobile"),
        "اسنپ" to listOf("snapp", "cab.snapp.passenger"),
        "تپسی" to listOf("tapsi", "tap30", "biz.tap30.passenger"),
        "نشان" to listOf("neshan", "org.neshan.maps"),
        "بلد" to listOf("balad", "ir.balad.maps"),
        "همراه کارت" to listOf("hamrah card", "hamrahcard", "ir.melli.hamrahcard", "ir.sadad.hamrahcard"),
        "همراهکارت" to listOf("hamrah card", "hamrahcard", "ir.melli.hamrahcard"),
        "بلو" to listOf("blubank", "blu", "com.samanpr.blu"),
        "بلو بانک" to listOf("blubank", "blu", "com.samanpr.blu"),
        "آپ" to listOf("asan pardakht", "733", "com.pec.asann"),
        "آسان پرداخت" to listOf("asan pardakht", "733", "com.pec.asann"),
        "ترموکس" to listOf("termux", "com.termux"),
        "پیامک" to listOf("messages", "messaging", "sms", "com.google.android.apps.messaging", "com.android.mms"),
        "پیامها" to listOf("messages", "messaging", "sms", "com.google.android.apps.messaging"),
        "تلفن" to listOf("phone", "dialer", "call", "com.google.android.dialer", "com.android.dialer"),
        "تماس" to listOf("phone", "dialer", "call", "com.google.android.dialer"),
        "دوربین" to listOf("camera", "com.android.camera", "com.google.android.GoogleCamera"),
        "گالری" to listOf("gallery", "photos", "com.google.android.apps.photos", "com.android.gallery3d"),
        "عکسها" to listOf("gallery", "photos", "com.google.android.apps.photos"),
        "تنظیمات" to listOf("settings", "com.android.settings"),
        "ماشین حساب" to listOf("calculator", "com.google.android.calculator", "com.android.calculator2"),
        "ساعت" to listOf("clock", "alarm", "com.google.android.deskclock"),
        "تقویم" to listOf("calendar", "com.google.android.calendar"),
        "کروم" to listOf("chrome", "com.android.chrome"),
        "گوگل" to listOf("google", "chrome", "com.google.android.googlequicksearchbox"),
        "یوتیوب" to listOf("youtube", "com.google.android.youtube"),
        "جیمیل" to listOf("gmail", "email", "mail", "com.google.android.gm"),
        "ایمیل" to listOf("gmail", "email", "mail", "com.google.android.gm"),
        "فایلها" to listOf("files", "file manager", "com.google.android.documentsui"),
        "مدیریت فایل" to listOf("files", "file manager", "com.google.android.documentsui"),
        "بازار" to listOf("bazaar", "cafe bazaar", "com.farsitel.bazaar"),
        "کافه بازار" to listOf("bazaar", "cafe bazaar", "com.farsitel.bazaar"),
        "مایکت" to listOf("myket", "ir.mservices.market"),
        "کالاف" to listOf("call of duty", "cod", "com.activision.callofduty.shooter"),
        "پابجی" to listOf("pubg", "com.tencent.ig")
    )

    /**
     * Finds best matching installed application for voice or text launch request.
     */
    fun findBestMatchingApp(query: String, installedApps: List<InstalledAppInfo>): InstalledAppInfo? {
        val normQuery = normalizePersianText(query)
        if (normQuery.isEmpty() || installedApps.isEmpty()) return null

        val targetKeywords = appAliases[normQuery] ?: listOf(normQuery)

        // 1. Check exact package or name match with aliases
        for (target in targetKeywords) {
            val exactMatch = installedApps.firstOrNull { app ->
                val appNormName = normalizePersianText(app.appName)
                val pkg = app.packageName.lowercase()
                appNormName == target || pkg == target || pkg.contains(target) || appNormName.contains(target)
            }
            if (exactMatch != null) return exactMatch
        }

        // 2. Fuzzy search over installed apps
        var bestApp: InstalledAppInfo? = null
        var highestScore = 0.0f

        for (app in installedApps) {
            val appNormName = normalizePersianText(app.appName)
            val pkg = app.packageName.lowercase()

            var score = calculateSimilarity(normQuery, appNormName)
            if (pkg.contains(normQuery)) {
                score = maxOf(score, 0.9f)
            }

            for (kw in targetKeywords) {
                val kwScore = calculateSimilarity(kw, appNormName)
                if (kwScore > score) score = kwScore
            }

            // Must exceed minimum threshold (0.6f) to avoid picking unrelated apps!
            if (score > highestScore && score >= 0.6f) {
                highestScore = score
                bestApp = app
            }
        }

        return bestApp
    }

    /**
     * Calculates similarity score between 0.0 and 1.0 using Levenshtein distance.
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        val n1 = normalizePersianText(s1)
        val n2 = normalizePersianText(s2)

        if (n1 == n2) return 1.0f
        if (n1.isEmpty() || n2.isEmpty()) return 0.0f
        if (n1.contains(n2) || n2.contains(n1)) return 0.88f

        val dist = levenshteinDistance(n1, n2)
        val maxLen = maxOf(n1.length, n2.length)
        if (maxLen == 0) return 1.0f

        return (1.0f - (dist.toFloat() / maxLen.toFloat())).coerceIn(0.0f, 1.0f)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[a.length][b.length]
    }
}
