package com.example.assistant

import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

object FuzzyMatcher {

    /**
     * Comprehensive Persian/Arabic & English Unicode normalization:
     * - Strips all emojis and pictographic symbols
     * - Standardizes Persian/Arabic characters (ی, ک, ه, ا, و)
     * - Converts Persian & Arabic digits to standard Latin digits (0-9)
     * - Removes zero-width characters (ZWNJ, ZWJ, LTR, RTL)
     * - Removes diacritics / Tanwin / Harakat
     * - Standardizes whitespace and punctuation
     */
    fun normalizePersianText(input: String): String {
        if (input.isBlank()) return ""

        var s = input.trim()

        // 1. Remove Emojis and Symbols
        s = s.replace(Regex("[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Cs}\uD83C-\uDBFF\uDC00-\uDFFF]"), " ")

        // 2. Normalize Persian & Arabic digits to English digits (0-9)
        s = s.replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')

        // 3. Normalize Arabic / Persian letter variants
        s = s.replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ئ', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('آ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('ؤ', 'و')
            .replace('\u0640', ' ') // Tatweel
            .replace('\u200c', ' ') // ZWNJ to space
            .replace('\u200d', ' ') // ZWJ
            .replace('\u200e', ' ') // LTR
            .replace('\u200f', ' ') // RTL

        // 4. Remove diacritics / Harakat
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
        s = s.replace(Regex("[\\p{Mn}\u064B-\u065F\u0670]"), "")

        // 5. Replace punctuation with spaces and collapse whitespace
        s = s.replace(Regex("[\\p{Punct}\\p{IsPunctuation}،؛؟!«»]+"), " ")
        s = s.replace(Regex("\\s+"), " ").trim().lowercase()

        return s
    }

    /**
     * Standardizes phone numbers:
     * - Converts Persian/Arabic digits to English
     * - Removes spaces, dashes, parentheses, dots
     * - Normalizes Iranian prefix: +98 or 0098 or 912... -> 0912...
     */
    fun normalizePhoneNumber(raw: String): String {
        if (raw.isBlank()) return ""

        val digits = normalizePersianText(raw).replace(Regex("[^0-9+]"), "")
        return when {
            digits.startsWith("+98") -> "0" + digits.substring(3)
            digits.startsWith("0098") -> "0" + digits.substring(4)
            digits.length == 10 && digits.startsWith("9") -> "0$digits"
            else -> digits
        }
    }

    /**
     * Finds top contact matches based on input name, partial words, or phone number.
     * Guaranteed to return at most 3 strongest candidates.
     */
    fun findTop3Contacts(query: String, allContacts: List<ContactMatch>): List<ContactMatch> {
        val normQuery = normalizePersianText(query)
        if (normQuery.isEmpty() || allContacts.isEmpty()) return emptyList()

        val queryPhone = normalizePhoneNumber(query)
        val isQueryNumeric = queryPhone.length >= 4 && normQuery.all { it.isDigit() || it == '+' || it == ' ' }

        val scored = allContacts.map { contact ->
            val normName = normalizePersianText(contact.displayName)
            val cleanPhone = normalizePhoneNumber(contact.phoneNumber)

            var score = 0.0f

            if (isQueryNumeric && queryPhone.isNotEmpty()) {
                // Phone number matching
                if (cleanPhone == queryPhone) {
                    score = 1.0f
                } else if (cleanPhone.endsWith(queryPhone) || queryPhone.endsWith(cleanPhone)) {
                    score = 0.98f
                } else if (cleanPhone.contains(queryPhone)) {
                    score = 0.92f
                }
            }

            if (normName.isNotEmpty()) {
                val nameScore = computeNameSimilarity(normQuery, normName)
                if (nameScore > score) {
                    score = nameScore
                }
            }

            contact.copy(matchScore = score)
        }

        return scored
            .filter { it.matchScore >= 0.40f }
            .sortedByDescending { it.matchScore }
            .take(3)
    }

    private fun computeNameSimilarity(query: String, target: String): Float {
        if (query == target) return 1.0f
        if (query.isEmpty() || target.isEmpty()) return 0.0f

        // Prefix / Suffix / Substring checks
        if (target.startsWith(query) || target.endsWith(query)) return 0.95f
        if (target.contains(query) || query.contains(target)) return 0.90f

        val qTokens = query.split(" ").filter { it.isNotBlank() }
        val tTokens = target.split(" ").filter { it.isNotBlank() }

        if (qTokens.isEmpty() || tTokens.isEmpty()) return 0.0f

        // Token overlap & partial token matching
        var matchedTokens = 0
        var totalTokenScore = 0.0f

        for (qTok in qTokens) {
            var bestTokScore = 0.0f
            for (tTok in tTokens) {
                if (qTok == tTok) {
                    bestTokScore = maxOf(bestTokScore, 1.0f)
                } else if (tTok.startsWith(qTok) || tTok.contains(qTok)) {
                    bestTokScore = maxOf(bestTokScore, 0.90f)
                } else {
                    val sim = calculateSimilarity(qTok, tTok)
                    if (sim > 0.70f) {
                        bestTokScore = maxOf(bestTokScore, sim)
                    }
                }
            }
            if (bestTokScore > 0.0f) {
                matchedTokens++
                totalTokenScore += bestTokScore
            }
        }

        val tokenOverlapScore = if (qTokens.isNotEmpty()) totalTokenScore / qTokens.size else 0.0f
        val fullLevenshtein = calculateSimilarity(query, target)

        return maxOf(tokenOverlapScore, fullLevenshtein)
    }

    /**
     * Map of Persian/English aliases to standard keywords and package names
     */
    val appAliases = mapOf(
        "واتساپ" to listOf("whatsapp", "com.whatsapp", "whatsapp business", "com.whatsapp.w4b"),
        "واتس اپ" to listOf("whatsapp", "com.whatsapp"),
        "واتس‌اپ" to listOf("whatsapp", "com.whatsapp"),
        "واتسپ" to listOf("whatsapp", "com.whatsapp"),
        "whatsapp" to listOf("whatsapp", "com.whatsapp"),
        "whatsap" to listOf("whatsapp", "com.whatsapp"),
        "تلگرام" to listOf("telegram", "org.telegram.messenger", "org.telegram.plus", "ir.nasim"),
        "تلگ رام" to listOf("telegram", "org.telegram.messenger"),
        "telegram" to listOf("telegram", "org.telegram.messenger"),
        "اینستا" to listOf("instagram", "com.instagram.android"),
        "اینستاگرام" to listOf("instagram", "com.instagram.android"),
        "instagram" to listOf("instagram", "com.instagram.android"),
        "روبیکا" to listOf("rubika", "ir.resaneh1.iptv", "ir.rubika"),
        "rubika" to listOf("rubika", "ir.resaneh1.iptv", "ir.rubika"),
        "ایتا" to listOf("eitaa", "ir.eitaa.messenger"),
        "eitaa" to listOf("eitaa", "ir.eitaa.messenger"),
        "بله" to listOf("bale", "ir.ble.messenger"),
        "bale" to listOf("bale", "ir.ble.messenger"),
        "سروش" to listOf("soroush", "mobi.mmdt.ott", "mobi.mmdt.ottplus"),
        "soroush" to listOf("soroush", "mobi.mmdt.ott"),
        "دیجیکالا" to listOf("digikala", "com.digikala.mobile"),
        "دیجی کالا" to listOf("digikala", "com.digikala.mobile"),
        "digikala" to listOf("digikala", "com.digikala.mobile"),
        "اسنپ" to listOf("snapp", "cab.snapp.passenger"),
        "snapp" to listOf("snapp", "cab.snapp.passenger"),
        "تپسی" to listOf("tapsi", "tap30", "biz.tap30.passenger"),
        "tapsi" to listOf("tapsi", "tap30", "biz.tap30.passenger"),
        "نشان" to listOf("neshan", "org.neshan.maps"),
        "neshan" to listOf("neshan", "org.neshan.maps"),
        "بلد" to listOf("balad", "ir.balad.maps"),
        "balad" to listOf("balad", "ir.balad.maps"),
        "دیوار" to listOf("divar", "ir.divar"),
        "divar" to listOf("divar", "ir.divar"),
        "شیپور" to listOf("sheypoor", "com.sheypoor.mobile"),
        "sheypoor" to listOf("sheypoor", "com.sheypoor.mobile"),
        "همراه کارت" to listOf("hamrah card", "hamrahcard", "ir.melli.hamrahcard", "ir.sadad.hamrahcard"),
        "همراهکارت" to listOf("hamrah card", "hamrahcard", "ir.melli.hamrahcard"),
        "بلو" to listOf("blubank", "blu", "com.samanpr.blu"),
        "بلو بانک" to listOf("blubank", "blu", "com.samanpr.blu"),
        "blubank" to listOf("blubank", "blu", "com.samanpr.blu"),
        "آپ" to listOf("asan pardakht", "733", "com.pec.asann"),
        "آسان پرداخت" to listOf("asan pardakht", "733", "com.pec.asann"),
        "ترموکس" to listOf("termux", "com.termux"),
        "termux" to listOf("termux", "com.termux"),
        "پیامک" to listOf("messages", "messaging", "sms", "com.google.android.apps.messaging", "com.android.mms"),
        "پیامها" to listOf("messages", "messaging", "sms", "com.google.android.apps.messaging"),
        "تلفن" to listOf("phone", "dialer", "call", "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer"),
        "تماس" to listOf("phone", "dialer", "call", "com.google.android.dialer"),
        "دوربین" to listOf("camera", "com.android.camera", "com.google.android.GoogleCamera", "com.sec.android.app.camera"),
        "camera" to listOf("camera", "com.android.camera", "com.google.android.GoogleCamera"),
        "گالری" to listOf("gallery", "photos", "com.google.android.apps.photos", "com.android.gallery3d", "com.sec.android.gallery3d"),
        "gallery" to listOf("gallery", "photos", "com.google.android.apps.photos"),
        "عکسها" to listOf("gallery", "photos", "com.google.android.apps.photos"),
        "تنظیمات" to listOf("settings", "com.android.settings"),
        "settings" to listOf("settings", "com.android.settings"),
        "ماشین حساب" to listOf("calculator", "com.google.android.calculator", "com.android.calculator2", "com.sec.android.app.popupcalculator"),
        "calculator" to listOf("calculator", "com.google.android.calculator"),
        "ساعت" to listOf("clock", "alarm", "com.google.android.deskclock", "com.sec.android.app.clockpackage"),
        "clock" to listOf("clock", "alarm", "com.google.android.deskclock"),
        "تقویم" to listOf("calendar", "com.google.android.calendar", "com.sec.android.app.calendar"),
        "calendar" to listOf("calendar", "com.google.android.calendar"),
        "کروم" to listOf("chrome", "com.android.chrome"),
        "chrome" to listOf("chrome", "com.android.chrome"),
        "گوگل" to listOf("google", "chrome", "com.google.android.googlequicksearchbox"),
        "google" to listOf("google", "com.google.android.googlequicksearchbox"),
        "یوتیوب" to listOf("youtube", "com.google.android.youtube"),
        "youtube" to listOf("youtube", "com.google.android.youtube"),
        "جیمیل" to listOf("gmail", "email", "mail", "com.google.android.gm"),
        "ایمیل" to listOf("gmail", "email", "mail", "com.google.android.gm"),
        "gmail" to listOf("gmail", "email", "mail", "com.google.android.gm"),
        "فایلها" to listOf("files", "file manager", "com.google.android.documentsui", "com.sec.android.app.myfiles"),
        "مدیریت فایل" to listOf("files", "file manager", "com.google.android.documentsui"),
        "files" to listOf("files", "file manager", "com.google.android.documentsui"),
        "بازار" to listOf("bazaar", "cafe bazaar", "com.farsitel.bazaar"),
        "کافه بازار" to listOf("bazaar", "cafe bazaar", "com.farsitel.bazaar"),
        "bazaar" to listOf("bazaar", "cafe bazaar", "com.farsitel.bazaar"),
        "مایکت" to listOf("myket", "ir.mservices.market"),
        "myket" to listOf("myket", "ir.mservices.market")
    )

    /**
     * Finds best matching installed application for voice or text launch request.
     */
    fun findBestMatchingApp(query: String, installedApps: List<InstalledAppInfo>): InstalledAppInfo? {
        val normQuery = normalizePersianText(query)
        if (normQuery.isEmpty() || installedApps.isEmpty()) return null

        val targetKeywords = (appAliases[normQuery] ?: emptyList()) + listOf(normQuery)

        // 1. Check exact match with app name, package name, or known aliases
        for (target in targetKeywords) {
            val normTarget = normalizePersianText(target)
            val exact = installedApps.firstOrNull { app ->
                val appNorm = normalizePersianText(app.appName)
                val pkg = app.packageName.lowercase()
                appNorm == normTarget || pkg == target.lowercase() || pkg == normTarget
            }
            if (exact != null) return exact
        }

        // 2. Check contains or prefix match with aliases
        for (target in targetKeywords) {
            val normTarget = normalizePersianText(target)
            val containsMatch = installedApps.firstOrNull { app ->
                val appNorm = normalizePersianText(app.appName)
                val pkg = app.packageName.lowercase()
                appNorm.contains(normTarget) || pkg.contains(target.lowercase()) || normTarget.contains(appNorm)
            }
            if (containsMatch != null) return containsMatch
        }

        // 3. Fuzzy search over installed apps
        var bestApp: InstalledAppInfo? = null
        var highestScore = 0.0f

        for (app in installedApps) {
            val appNorm = normalizePersianText(app.appName)
            val pkg = app.packageName.lowercase()

            var score = calculateSimilarity(normQuery, appNorm)
            if (pkg.contains(normQuery)) {
                score = maxOf(score, 0.85f)
            }

            for (kw in targetKeywords) {
                val kwScore = calculateSimilarity(normalizePersianText(kw), appNorm)
                if (kwScore > score) score = kwScore
            }

            if (score > highestScore && score >= 0.55f) {
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
