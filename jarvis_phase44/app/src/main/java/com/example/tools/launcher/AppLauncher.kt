package com.example.tools.launcher

import android.content.Context
import android.content.Intent
import java.net.URLEncoder
import java.text.Normalizer
import kotlin.math.max

data class InstalledApp(
    val label: String,
    val packageName: String
)

data class AppMatch(
    val app: InstalledApp,
    val score: Int
)

enum class LaunchResolution {
    LOCAL_EXACT,
    LOCAL_FUZZY,
    NEEDS_ONLINE_RESOLUTION,
    NOT_FOUND
}

data class LaunchPlan(
    val resolution: LaunchResolution,
    val candidates: List<AppMatch>,
    val rawQuery: String
)

class AppLauncher(private val context: Context) {

    fun installedApps(): List<InstalledApp> =
        context.packageManager.getInstalledApplications(0)
            .mapNotNull { info ->
                val label = info.loadLabel(context.packageManager)?.toString()?.trim()
                if (label.isNullOrBlank()) null
                else InstalledApp(label, info.packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

    /**
     * Offline-first resolver. No network is touched here.
     */
    fun resolveLocal(query: String, limit: Int = 5): List<AppMatch> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()

        return installedApps()
            .map { app ->
                val label = normalize(app.label)
                val pkg = normalize(app.packageName)
                var score = 0

                if (label == q) score += 100
                if (label.startsWith(q)) score += 50
                if (label.contains(q)) score += 35
                if (pkg.contains(q)) score += 20

                val distance = levenshtein(q, label)
                if (distance <= max(2, q.length / 3)) {
                    score += max(0, 30 - distance * 8)
                }

                AppMatch(app, score)
            }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(limit.coerceIn(1, 10))
    }

    /**
     * Complete decision boundary:
     * 1) exact/local fuzzy match when confident;
     * 2) otherwise signal the Agent to perform the already-authorized
     *    online resolution step;
     * 3) if offline, the best local candidates remain available.
     */
    fun plan(query: String, internetAvailable: Boolean): LaunchPlan {
        val candidates = resolveLocal(query, 5)
        if (candidates.isEmpty()) {
            return LaunchPlan(
                if (internetAvailable) LaunchResolution.NEEDS_ONLINE_RESOLUTION
                else LaunchResolution.NOT_FOUND,
                candidates,
                query
            )
        }

        val top = candidates.first()
        val second = candidates.getOrNull(1)

        return when {
            top.score >= 100 -> LaunchPlan(
                LaunchResolution.LOCAL_EXACT, candidates, query
            )
            top.score >= 70 && (second == null || top.score - second.score >= 15) ->
                LaunchPlan(LaunchResolution.LOCAL_FUZZY, candidates, query)
            internetAvailable -> LaunchPlan(
                LaunchResolution.NEEDS_ONLINE_RESOLUTION, candidates, query
            )
            else -> LaunchPlan(LaunchResolution.LOCAL_FUZZY, candidates, query)
        }
    }

    fun open(match: AppMatch): Result<Unit> {
        val intent = context.packageManager
            .getLaunchIntentForPackage(match.app.packageName)
            ?: return Result.failure(
                IllegalArgumentException("Application is not launchable.")
            )

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Produces the exact user-entered query for the online resolver.
     * The HTTP/search execution stays in Web Search so this tool remains
     * usable offline and does not silently access the network.
     */
    fun onlineSearchUrl(rawQuery: String): String =
        "https://www.google.com/search?q=" +
            URLEncoder.encode(rawQuery.trim(), "UTF-8")

    private fun normalize(value: String): String =
        Normalizer.normalize(
            value.lowercase()
                .replace('ي', 'ی')
                .replace('ى', 'ی')
                .replace('ك', 'ک')
                .replace('\u200c', ' '),
            Normalizer.Form.NFKD
        )
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}\\s._-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
            }
            for (j in prev.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}
