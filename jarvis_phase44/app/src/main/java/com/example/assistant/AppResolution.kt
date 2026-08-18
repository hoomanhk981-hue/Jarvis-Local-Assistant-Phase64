package com.example.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.models.InstalledAppInfo

/** Deterministic app resolution: never launches an unrelated app just because it is the closest result. */
object AppResolution {
    data class Candidate(val app: InstalledAppInfo, val score: Float)

    fun rank(query: String, apps: List<InstalledAppInfo>): List<Candidate> {
        val q = FuzzyMatcher.normalizePersianText(query)
        if (q.isBlank()) return emptyList()
        return apps.mapNotNull { app ->
            val name = FuzzyMatcher.normalizePersianText(app.appName)
            val pkg = app.packageName.lowercase()
            var score = FuzzyMatcher.calculateSimilarity(q, name)
            if (pkg == q) score = 1f
            else if (pkg.contains(q.replace(" ", ""))) score = maxOf(score, .94f)
            Candidate(app, score)
        }.sortedByDescending { it.score }.take(3)
    }

    fun canUseInternet(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Builds a normal browser search URL. No search API key or paid API is required. */
    fun webSearchUrl(query: String): String =
        "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
}
