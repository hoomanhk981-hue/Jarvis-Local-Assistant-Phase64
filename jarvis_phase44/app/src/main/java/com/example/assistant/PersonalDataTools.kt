package com.example.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.local.entities.MemoryCategory
import com.example.data.repository.AssistantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Real, local-only personal-data tools. No network calls are made here. */
class ContactsTool(private val context: Context) : JarvisTool {
    override val name = "search_contacts"
    override val description = "Search the device's real contacts by name or phone number and return up to 3 closest matches."

    override suspend fun execute(arguments: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolResult.Failure("READ_CONTACTS permission is not granted")
        }
        val query = arguments["query"]?.trim().orEmpty()
        if (query.isBlank()) return@withContext ToolResult.Failure("query is required")

        val contacts = AssistantRepository(context).getPhoneContacts()
        if (contacts.isEmpty()) return@withContext ToolResult.Failure("No readable contacts were found")

        val normalizedQuery = normalize(query)
        val ranked = contacts
            .map { contact ->
                val name = normalize(contact.displayName)
                val phone = normalizePhone(contact.phoneNumber)
                val score = maxOf(
                    similarity(normalizedQuery, name),
                    similarity(normalizedQuery, phone),
                    if (name.contains(normalizedQuery) || phone.contains(normalizedQuery)) 0.92 else 0.0
                )
                contact to score
            }
            .filter { it.second >= 0.25 }
            .sortedByDescending { it.second }
            .take(3)

        if (ranked.isEmpty()) return@withContext ToolResult.Failure("No close contact matches for '$query'")
        val text = ranked.mapIndexed { i, (c, score) ->
            "${i + 1}. ${c.displayName} — ${c.phoneNumber} (match=${"%.2f".format(score)})"
        }.joinToString("\n")
        ToolResult.Success("Found ${ranked.size} close contact matches", mapOf("matches" to text))
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace('ي', 'ی').replace('ك', 'ک')
        .replace(Regex("[\\s_\\-()]+"), "")

    private fun normalizePhone(value: String): String = value.filter(Char::isDigit).let {
        when {
            it.startsWith("98") -> "0" + it.drop(2)
            it.startsWith("0098") -> "0" + it.drop(4)
            else -> it
        }
    }

    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a.length > b.length) return similarity(b, a)
        val distance = levenshtein(a, b)
        return 1.0 - distance.toDouble() / b.length.coerceAtLeast(1)
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val cur = IntArray(b.length + 1)
            cur[0] = i + 1
            for (j in b.indices) {
                cur[j + 1] = minOf(
                    cur[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            prev = cur
        }
        return prev[b.length]
    }
}

class SmsSearchTool(private val context: Context) : JarvisTool {
    override val name = "search_sms"
    override val description = "Search real device SMS messages locally by text, sender, category, or OTP-related content."

    override suspend fun execute(arguments: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ToolResult.Failure("READ_SMS permission is not granted")
        }
        val query = arguments["query"]?.trim().orEmpty()
        val sender = arguments["sender"]?.trim().orEmpty()
        val category = arguments["category"]?.trim()?.uppercase().orEmpty()
        val limit = arguments["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 30
        if (query.isBlank() && sender.isBlank() && category.isBlank()) {
            return@withContext ToolResult.Failure("At least one of query, sender, or category is required")
        }

        val messages = AssistantRepository(context).getRecentSmsMessages(200)
        val q = normalize(query)
        val s = normalize(sender)
        val results = messages.filter { sms ->
            val body = normalize(sms.body)
            val address = normalize(sms.sender)
            (q.isBlank() || body.contains(q) || address.contains(q) || fuzzyTokenMatch(q, body)) &&
                (s.isBlank() || address.contains(s) || body.contains(s)) &&
                (category.isBlank() || sms.category.uppercase() == category)
        }.sortedByDescending { it.timestamp }.take(limit)

        if (results.isEmpty()) return@withContext ToolResult.Failure("No matching SMS messages found")
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val text = results.mapIndexed { i, sms ->
            "${i + 1}. [${sms.category}] ${sms.sender} | ${formatter.format(Date(sms.timestamp))}\n${sms.body.take(500)}"
        }.joinToString("\n---\n")
        ToolResult.Success("Found ${results.size} SMS messages", mapOf("messages" to text))
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace('ي', 'ی').replace('ك', 'ک')
        .replace(Regex("\\s+"), " ").trim()

    private fun fuzzyTokenMatch(query: String, body: String): Boolean {
        if (query.length < 3) return false
        return body.split(Regex("\\s+"), RegexOption.IGNORE_CASE).any { token ->
            levenshtein(query, token) <= maxOf(1, query.length / 4)
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val cur = IntArray(b.length + 1)
            cur[0] = i + 1
            for (j in b.indices) cur[j + 1] = minOf(cur[j] + 1, prev[j + 1] + 1, prev[j] + if (a[i] == b[j]) 0 else 1)
            prev = cur
        }
        return prev[b.length]
    }
}

class MemorySearchTool(context: Context) : JarvisTool {
    override val name = "search_memory"
    override val description = "Search Jarvis's private local memory database for facts, preferences, skills, or saved app knowledge."
    private val repository = AssistantRepository(context)

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]?.trim().orEmpty()
        if (query.isBlank()) return ToolResult.Failure("query is required")
        val memories = repository.searchMemories(query).take(20)
        if (memories.isEmpty()) return ToolResult.Failure("No memory matches for '$query'")
        val text = memories.joinToString("\n") { "[${it.category}] ${it.key}: ${it.value}" }
        return ToolResult.Success("Found ${memories.size} memories", mapOf("memories" to text))
    }
}

class RememberTool(context: Context) : JarvisTool {
    override val name = "remember"
    override val description = "Save a user-provided fact to local memory. Requires explicit confirmed=true; the model must never invent facts."
    private val repository = AssistantRepository(context)

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val key = arguments["key"]?.trim().orEmpty()
        val value = arguments["value"]?.trim().orEmpty()
        if (key.isBlank() || value.isBlank()) return ToolResult.Failure("key and value are required")
        if (!arguments["confirmed"].equals("true", ignoreCase = true)) {
            return ToolResult.NeedsConfirmation("remember", "ذخیره این مورد در حافظه محلی: $key = $value")
        }
        val category = runCatching { MemoryCategory.valueOf(arguments["category"].orEmpty().uppercase()) }
            .getOrDefault(MemoryCategory.USER_PREFERENCE)
        repository.saveMemory(key, value, category)
        return ToolResult.Success("Memory saved locally", mapOf("key" to key, "category" to category.name))
    }
}
