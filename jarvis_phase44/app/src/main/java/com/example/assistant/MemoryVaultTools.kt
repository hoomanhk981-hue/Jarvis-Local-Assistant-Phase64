package com.example.assistant

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entities.SavedPasswordEntity
import com.example.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Password vault tools. Password plaintext is only returned when explicitly requested by the user flow. */
class SavePasswordTool(private val context: Context) : JarvisTool {
    override val name = "save_password"
    override val description = "Save an app/service password in the local encrypted vault. Explicit confirmation is required."
    private val dao = AppDatabase.getDatabase(context).savedPasswordDao()
    private val security = SecurityManager(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val app = arguments["app"]?.trim().orEmpty(); val account = arguments["account"]?.trim().orEmpty(); val password = arguments["password"] ?: ""
        if (app.isBlank() || password.isBlank()) return@withContext ToolResult.Failure("app and password are required")
        if (!arguments["confirmed"].equals("true", true)) return@withContext ToolResult.NeedsConfirmation(name, "ذخیره رمز برنامه $app در Vault محلی")
        dao.insertPassword(SavedPasswordEntity(appName = app, accountName = account, passwordEncrypted = security.encrypt(password), notes = arguments["notes"].orEmpty()))
        ToolResult.Success("Password saved in encrypted local vault", mapOf("app" to app, "account" to account))
    }
}

class GetPasswordTool(private val context: Context) : JarvisTool {
    override val name = "get_password"
    override val description = "Retrieve a password from the encrypted local vault; requires explicit confirmation."
    private val dao = AppDatabase.getDatabase(context).savedPasswordDao()
    private val security = SecurityManager(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val app = arguments["app"]?.trim().orEmpty()
        if (app.isBlank()) return@withContext ToolResult.Failure("app is required")
        if (!arguments["confirmed"].equals("true", true)) return@withContext ToolResult.NeedsConfirmation(name, "نمایش رمز ذخیره‌شده برای $app")
        val row = dao.getPasswordForApp(app) ?: return@withContext ToolResult.Failure("No saved password for $app")
        val passwordResult = runCatching { security.decrypt(row.passwordEncrypted) }
        if (passwordResult.isFailure) return@withContext ToolResult.Failure("Vault entry cannot be decrypted on this device")
        val password = passwordResult.getOrThrow()
        ToolResult.Success("Password retrieved", mapOf("app" to row.appName, "account" to row.accountName, "password" to password))
    }
}

/** Exports non-secret personal memory to a user-selected JSON document. Passwords are never exported plaintext. */
class ExportMemoryJsonTool(private val context: Context) : JarvisTool {
    override val name = "export_memory_json"
    override val description = "Export personal memory and non-secret metadata to JSON in the selected workspace."
    private val db = AppDatabase.getDatabase(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        if (!arguments["confirmed"].equals("true", true)) return@withContext ToolResult.NeedsConfirmation(name, "خروجی گرفتن از حافظه شخصی به JSON")
        val memories = db.userMemoryDao().getAllMemoriesSnapshot()
        val root = JSONObject().apply {
            put("schema", 1)
            put("generatedAt", System.currentTimeMillis())
            put("memories", JSONArray().apply { memories.forEach { put(JSONObject().apply { put("key", it.key); put("value", it.value); put("category", it.category.name); put("timestamp", it.timestamp) }) } })
            put("note", "Secrets/passwords are intentionally excluded from this export.")
        }
        val workspace = com.example.files.SafWorkspaceManager(context)
        val name = arguments["name"]?.trim().takeUnless { it.isNullOrBlank() } ?: "jarvis-memory.json"
        val ok = workspace.writeFile(name, root.toString(2))
        if (ok) ToolResult.Success("Memory JSON exported", mapOf("name" to name, "count" to memories.size.toString())) else ToolResult.Failure("Could not write JSON to selected workspace")
    }
}

private suspend fun com.example.data.local.dao.UserMemoryDao.getAllMemoriesSnapshot() = getAllMemories().first()
