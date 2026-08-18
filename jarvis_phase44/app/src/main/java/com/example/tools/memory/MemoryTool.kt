package com.example.tools.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MemoryEntry(
    val id: String,
    val key: String,
    val value: String,
    val updatedAt: Long
)

/**
 * Small local-first memory store for agent preferences/facts.
 *
 * Data is kept in the app-private filesystem. No network/API is involved.
 */
class MemoryTool(context: Context) {
    private val file = File(context.filesDir, "agent_memory.json")
    private val mutex = Mutex()

    suspend fun put(key: String, value: String): Result<Unit> = mutex.withLock {
        if (key.isBlank()) return@withLock Result.failure(
            IllegalArgumentException("Memory key is empty.")
        )
        if (value.length > 10000) return@withLock Result.failure(
            IllegalArgumentException("Memory value is too large.")
        )

        return@withLock try {
            val all = readInternal().toMutableMap()
            val id = key.trim()
            all[id] = MemoryEntry(id, id, value, System.currentTimeMillis())
            writeInternal(all)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun get(key: String): Result<MemoryEntry?> = mutex.withLock {
        Result.success(readInternal()[key.trim()])
    }

    suspend fun search(query: String, limit: Int = 20): Result<List<MemoryEntry>> =
        mutex.withLock {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return@withLock Result.success(emptyList())
            val max = limit.coerceIn(1, 100)
            Result.success(
                readInternal().values
                    .filter {
                        it.key.lowercase().contains(q) ||
                        it.value.lowercase().contains(q)
                    }
                    .sortedByDescending { it.updatedAt }
                    .take(max)
            )
        }

    suspend fun remove(key: String): Result<Unit> = mutex.withLock {
        return@withLock try {
            val all = readInternal().toMutableMap()
            all.remove(key.trim())
            writeInternal(all)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun clear(): Result<Unit> = mutex.withLock {
        return@withLock try {
            if (file.exists()) file.delete()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun readInternal(): Map<String, MemoryEntry> {
        if (!file.exists()) return emptyMap()
        val root = JSONObject(file.readText())
        val arr = root.optJSONArray("entries") ?: JSONArray()
        val result = LinkedHashMap<String, MemoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id")
            if (id.isNotBlank()) {
                result[id] = MemoryEntry(
                    id,
                    o.optString("key"),
                    o.optString("value"),
                    o.optLong("updatedAt")
                )
            }
        }
        return result
    }

    private fun writeInternal(entries: Map<String, MemoryEntry>) {
        val arr = JSONArray()
        entries.values.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("key", it.key)
                put("value", it.value)
                put("updatedAt", it.updatedAt)
            })
        }
        file.writeText(JSONObject().put("entries", arr).toString())
    }
}
