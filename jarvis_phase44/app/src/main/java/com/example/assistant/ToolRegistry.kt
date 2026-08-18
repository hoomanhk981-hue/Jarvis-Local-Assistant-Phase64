package com.example.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.models.InstalledAppInfo
import com.example.termux.TermuxExecutor
import com.example.files.SafWorkspaceManager
import com.example.bank.BankTool
import com.example.bank.SaderatBankAdapter
import com.example.bank.TransferRequest

/**
 * Central tool registry. The LLM never receives direct access to Android APIs;
 * it can only request named tools and receive structured results.
 */
class ToolRegistry(private val context: Context) {
    private val permissionCoordinator = PermissionCoordinator(context)
    private val confirmationManager = ConfirmationManager()
    private val tools = linkedMapOf<String, JarvisTool>()

    init {
        register(AppTool(context))
        register(TermuxTool(context))
        register(AccessibilityTool())
        register(BrowserOpenTool(context))
        register(BrowserInspectTool())
        register(BrowserClickTool())
        register(BrowserTypeTool())
        register(BrowserBackTool())
        register(ContactsTool(context))
        register(SmsSearchTool(context))
        register(MemorySearchTool(context))
        register(RememberTool(context))
        register(MakeCallTool(context))
        register(SendSmsTool(context))
        register(FileListTool(context))
        register(FileReadTool(context))
        register(FileWriteTool(context))
        register(FileZipTool(context))
        register(OpenFileTool(context))
        register(CodeExecuteTool(context))
        register(CodeInstallDependencyTool(context))
        register(SavePasswordTool(context))
        register(GetPasswordTool(context))
        register(ExportMemoryJsonTool(context))
        register(PrepareBankTransferTool(context))
    }

    fun register(tool: JarvisTool) { tools[tool.name] = tool }
    fun get(name: String): JarvisTool? = tools[name]

    suspend fun execute(name: String, arguments: Map<String, String>): ToolResult {
        val tool = tools[name] ?: return ToolResult.Failure("Unknown tool: $name")
        val missing = permissionCoordinator.missingPermissions(name)
        if (missing.isNotEmpty()) return ToolResult.NeedsPermission(name, missing)

        // One central confirmation boundary. Individual tools may also enforce a
        // safety check, but they receive confirmed=true only after this gate passes.
        if (requiresConfirmation(name, arguments) && !arguments["confirmed"].equals("true", ignoreCase = true)) {
            val risk = ConfirmationManager.riskForTool(name, arguments)
            val summary = confirmationSummary(name, arguments)
            val request = confirmationManager.create(name, arguments, summary, risk)
            return ToolResult.NeedsConfirmation(name, "${request.summary} (شناسه تأیید: ${request.id})", request.id, risk, request)
        }
        return tool.execute(arguments)
    }

    /** Approves a pending action exactly once and executes the original arguments. */
    suspend fun approveConfirmation(id: String): ToolResult {
        val request = confirmationManager.approve(id)
            ?: return ToolResult.Failure("Confirmation is missing, expired, or already used")
        val missing = permissionCoordinator.missingPermissions(request.toolName)
        if (missing.isNotEmpty()) {
            // Put the request back only if the OS permission is the remaining blocker.
            confirmationManager.create(request.toolName, request.arguments, request.summary, request.risk)
            return ToolResult.NeedsPermission(request.toolName, missing)
        }
        return get(request.toolName)?.execute(request.arguments + ("confirmed" to "true"))
            ?: ToolResult.Failure("Unknown tool: ${request.toolName}")
    }

    fun rejectConfirmation(id: String): Boolean = confirmationManager.reject(id) != null

    fun descriptions(): List<String> = tools.values.map {
        val confirmation = if (requiresConfirmation(it.name, emptyMap())) " | confirmation_required" else ""
        "${it.name}: ${it.description}$confirmation"
    }
    fun names(): Set<String> = tools.keys

    private fun requiresConfirmation(name: String, args: Map<String, String>): Boolean = when (name) {
        "make_call", "send_sms", "write_file", "zip_workspace", "code_execute",
        "install_code_dependency", "save_password", "get_password", "export_memory_json",
        "remember", "browser_click", "browser_type" -> true
        "run_termux" -> TermuxSafety.isDangerous(args["command"].orEmpty())
        "accessibility_action" -> args["action"]?.lowercase() in setOf("click", "set_text", "type", "long_click", "swipe", "back", "home")
        else -> false
    }

    private fun confirmationSummary(name: String, args: Map<String, String>): String = when (name) {
        "make_call" -> "تماس با ${args["number"] ?: args["name"] ?: "مخاطب انتخاب‌شده"}"
        "send_sms" -> "ارسال پیامک به ${args["number"] ?: "شماره انتخاب‌شده"}: ${args["message"].orEmpty().take(120)}"
        "write_file" -> "نوشتن/تغییر فایل ${args["path"] ?: "انتخاب‌شده"}"
        "zip_workspace" -> "ساخت ZIP از Workspace"
        "code_execute" -> "اجرای کد ${args["language"] ?: "نامشخص"}"
        "install_code_dependency" -> "نصب وابستگی کدنویسی: ${args["package"] ?: "نامشخص"}"
        "save_password" -> "ذخیره رمز برای ${args["appName"] ?: "برنامه"}"
        "get_password" -> "نمایش/استفاده از رمز ذخیره‌شده برای ${args["appName"] ?: "برنامه"}"
        "browser_click", "browser_type" -> "تعامل با صفحه وب"
        "accessibility_action" -> "انجام عملیات روی رابط کاربری برنامه فعال: ${args["action"] ?: "inspect"}"
        "run_termux" -> "اجرای دستور حساس در Termux: ${args["command"].orEmpty().take(160)}"
        else -> "اجرای عملیات ${name}"
    }

    private object TermuxSafety {
        fun isDangerous(command: String): Boolean {
            val normalized = command.lowercase()
            return listOf(
                "rm -rf", "rm -r /", "mkfs", "dd if=", "shutdown", "reboot",
                "factory-reset", "wipe", "chmod 777", "chown -r", "> /dev/"
            ).any(normalized::contains)
        }
    }
}

private class PrepareBankTransferTool(private val context: Context) : JarvisTool {
    override val name = "prepare_bank_transfer"
    override val description = "Validate and prepare a Bank Saderat card-to-card transfer for explicit user confirmation; does not execute the transfer."

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val destination = arguments["destination_card"].orEmpty()
        val amount = arguments["amount_rials"]?.toLongOrNull()
            ?: return ToolResult.Failure("amount_rials must be an integer")
        val request = TransferRequest(
            destinationCard = destination,
            amountRials = amount,
            sourceCard = arguments["source_card"],
            expiryMonth = arguments["expiry_month"]?.toIntOrNull(),
            expiryYear = arguments["expiry_year"]?.toIntOrNull(),
            cvv2 = arguments["cvv2"],
            recipientName = arguments["recipient_name"]
        )
        val result = BankTool(SaderatBankAdapter(context)).prepare(request)
        return result.fold(
            onSuccess = { ToolResult.Success("Transfer prepared; awaiting explicit confirmation", mapOf("confirmation_text" to it, "bank" to "Saderat", "execution" to "NOT_EXECUTED")) },
            onFailure = { ToolResult.Failure(it.message ?: "Transfer validation failed") }
        )
    }
}

private class AppTool(private val context: Context) : JarvisTool {
    override val name = "open_app"
    override val description = "Safely resolve and open an installed Android app by name/package. Uses local fuzzy matching first and never launches a low-confidence unrelated app."

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val requested = arguments["package"]?.trim().orEmpty().ifBlank { arguments["app"]?.trim().orEmpty() }
        if (requested.isBlank()) return ToolResult.Failure("package or app is required")
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { info ->
                com.example.data.models.InstalledAppInfo(
                    appName = pm.getApplicationLabel(info).toString(),
                    packageName = info.packageName,
                    isSystemApp = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }

        val ranked = AppResolution.rank(requested, apps)
        if (ranked.isEmpty()) return ToolResult.Failure("No launchable installed app matched: $requested")

        val exact = ranked.firstOrNull { it.score >= 0.96f }
        val best = exact ?: ranked.firstOrNull()
        // Ambiguous/weak matches must not silently open the wrong app.
        if (best == null || (best.score < 0.82f && ranked.size > 1)) {
            return ToolResult.Failure(
                "APP_SELECTION_REQUIRED: " + ranked.joinToString(" | ") { "${it.app.appName} (${String.format("%.0f", it.score * 100)}%)" } +
                    if (AppResolution.canUseInternet(context)) " ; internet_available=true; web_search=${AppResolution.webSearchUrl(requested)}" else " ; offline_mode=true"
            )
        }
        // Close scores are ambiguous even when both are reasonably good.
        if (ranked.size >= 2 && (best.score - ranked[1].score) < 0.08f && best.score < 0.96f) {
            return ToolResult.Failure(
                "APP_SELECTION_REQUIRED: " + ranked.joinToString(" | ") { "${it.app.appName} (${String.format("%.0f", it.score * 100)}%)" }
            )
        }

        val intent = pm.getLaunchIntentForPackage(best.app.packageName)
            ?: return ToolResult.Failure("No launch intent for ${best.app.packageName}")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult.Success(
            "Opened ${best.app.appName}",
            mapOf("package" to best.app.packageName, "score" to best.score.toString(), "offline_safe" to "true")
        )
    }
}

private class TermuxTool(private val context: Context) : JarvisTool {
    override val name = "run_termux"
    override val description = "Send a command to Termux; destructive commands require confirmation."
    private val executor = TermuxExecutor(context)

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val command = arguments["command"]?.trim().orEmpty()
        if (command.isBlank()) return ToolResult.Failure("command is required")
        if (executor.isDangerousCommand(command) && !arguments["confirmed"].equals("true", ignoreCase = true)) {
            return ToolResult.NeedsConfirmation("run_termux", command)
        }
        if (!executor.isTermuxInstalled()) return ToolResult.Failure("Termux is not installed")
        val result = executor.executeTermuxCommand(command, arguments["workdir"] ?: "/data/data/com.termux/files/home")
        return if (result.success) {
            ToolResult.Success(
                "Command completed in Termux",
                mapOf(
                    "command" to command,
                    "exit_code" to result.exitCode.toString(),
                    "stdout" to result.stdout.take(100_000),
                    "stderr" to result.stderr.take(25_000)
                )
            )
        } else {
            ToolResult.Failure(
                buildString {
                    append("Termux command failed (exit=${result.exitCode})")
                    if (!result.errorMessage.isNullOrBlank()) append(": ${result.errorMessage}")
                    if (result.stdout.isNotBlank()) append("\nstdout:\n${result.stdout.take(100_000)}")
                    if (result.stderr.isNotBlank()) append("\nstderr:\n${result.stderr.take(25_000)}")
                }
            )
        }
    }
}

private class AccessibilityTool : JarvisTool {
    override val name = "accessibility_action"
    override val description = "Inspect or act on the active app through the user-enabled Jarvis accessibility service."

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val action = arguments["action"] ?: "inspect"
        val service = JarvisAccessibilityService.instance
            ?: return ToolResult.Failure("Jarvis Accessibility Service is not enabled")
        return service.performToolAction(action, arguments)
    }
}

private class BrowserOpenTool(private val context: Context) : JarvisTool {
    override val name = "open_url"
    override val description = "Open a safe http/https URL in Jarvis's private browser."
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val url = arguments["url"]?.trim().orEmpty()
        if (url.isBlank()) return ToolResult.Failure("url is required")
        val intent = android.content.Intent(context, BrowserActivity::class.java).putExtra("url", url).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult.Success("Browser opened", mapOf("url" to url))
    }
}

private class BrowserInspectTool : JarvisTool {
    override val name = "browser_inspect"
    override val description = "Inspect the current page DOM and return visible interactive elements."
    override suspend fun execute(arguments: Map<String, String>): ToolResult = ToolResult.Success(BrowserController.inspect())
}

private class BrowserClickTool : JarvisTool {
    override val name = "browser_click"
    override val description = "Click a visible link/button by its text or accessible label."
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val target = arguments["target"]?.trim().orEmpty().ifBlank { return ToolResult.Failure("target is required") }
        return when (val r = BrowserController.click(target)) { "CLICKED" -> ToolResult.Success("Clicked '$target'"); "NOT_FOUND" -> ToolResult.Failure("Element not found: $target"); else -> ToolResult.Failure(r) }
    }
}

private class BrowserTypeTool : JarvisTool {
    override val name = "browser_type"
    override val description = "Type text into an input identified by name, id, placeholder, or aria-label."
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val target = arguments["target"]?.trim().orEmpty()
        val text = arguments["text"] ?: ""
        if (target.isBlank()) return ToolResult.Failure("target is required")
        return when (val r = BrowserController.type(target, text)) { "TYPED" -> ToolResult.Success("Text entered"); "NOT_FOUND" -> ToolResult.Failure("Input not found: $target"); else -> ToolResult.Failure(r) }
    }
}

private class BrowserBackTool : JarvisTool {
    override val name = "browser_back"
    override val description = "Navigate back in Jarvis's private browser."
    override suspend fun execute(arguments: Map<String, String>): ToolResult { BrowserController.back(); return ToolResult.Success("Browser back requested") }
}


private class MakeCallTool(private val context: Context) : JarvisTool {
    override val name = "make_call"
    override val description = "Call a phone number or a contact after explicit user confirmation."

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val rawNumber = arguments["phone"]?.trim().orEmpty()
        val contactName = arguments["contact"]?.trim().orEmpty()
        if (rawNumber.isBlank() && contactName.isBlank()) return ToolResult.Failure("phone or contact is required")

        val number = if (rawNumber.isNotBlank()) rawNumber else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return ToolResult.NeedsPermission(name, listOf(Manifest.permission.READ_CONTACTS))
            }
            val matches = com.example.data.repository.AssistantRepository(context).getPhoneContacts()
                .filter { it.displayName.contains(contactName, ignoreCase = true) }
                .take(3)
            if (matches.isEmpty()) return ToolResult.Failure("No contact found for '$contactName'")
            if (matches.size > 1 && arguments["selected_index"].isNullOrBlank()) {
                return ToolResult.Failure("Multiple contacts found. Choose one of: " + matches.mapIndexed { i, c -> "${i + 1}. ${c.displayName} ${c.phoneNumber}" }.joinToString("; "))
            }
            val index = (arguments["selected_index"]?.toIntOrNull() ?: 1) - 1
            matches.getOrNull(index)?.phoneNumber ?: return ToolResult.Failure("Invalid contact selection")
        }

        if (!arguments["confirmed"].equals("true", ignoreCase = true)) {
            return ToolResult.NeedsConfirmation(name, "تماس با ${if (contactName.isNotBlank()) contactName else number} به شماره $number")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return ToolResult.NeedsPermission(name, listOf(Manifest.permission.CALL_PHONE))
        }
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult.Success("Call started", mapOf("phone" to number, "contact" to contactName))
        } catch (e: Exception) {
            ToolResult.Failure("Unable to start call: ${e.message ?: "unknown error"}")
        }
    }
}

private class SendSmsTool(private val context: Context) : JarvisTool {
    override val name = "send_sms"
    override val description = "Send an SMS to a phone number after explicit user confirmation."

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val phone = arguments["phone"]?.trim().orEmpty()
        val text = arguments["text"]?.trim().orEmpty()
        if (phone.isBlank() || text.isBlank()) return ToolResult.Failure("phone and text are required")
        if (!arguments["confirmed"].equals("true", ignoreCase = true)) {
            return ToolResult.NeedsConfirmation(name, "ارسال پیامک به $phone:\n$text")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return ToolResult.NeedsPermission(name, listOf(Manifest.permission.SEND_SMS))
        }
        return try {
            val sms = SmsManager.getDefault()
            val parts = sms.divideMessage(text)
            if (parts.size == 1) {
                sms.sendTextMessage(phone, null, text, null, null)
            } else {
                sms.sendMultipartTextMessage(phone, null, ArrayList(parts), null, null)
            }
            ToolResult.Success("SMS submitted to the system for sending", mapOf("phone" to phone, "parts" to parts.size.toString()))
        } catch (e: Exception) {
            ToolResult.Failure("SMS send failed: ${e.message ?: "unknown error"}")
        }
    }
}


private class FileListTool(private val context: Context) : JarvisTool {
    override val name = "list_workspace"
    override val description = "List files in the user-selected workspace through Android Storage Access Framework."
    private val files = SafWorkspaceManager(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        if (files.currentTreeUri() == null) return ToolResult.Failure("No workspace folder has been selected. Ask the user to choose a workspace folder first.")
        val items = files.listFiles(arguments["max"]?.toIntOrNull()?.coerceIn(1, 500) ?: 200)
        return ToolResult.Success("Workspace indexed", mapOf("count" to items.size.toString(), "files" to items.joinToString("\n") { "${it.path} | ${it.sizeBytes} bytes" }))
    }
}

private class FileReadTool(private val context: Context) : JarvisTool {
    override val name = "read_file"
    override val description = "Read a UTF-8 text/code file from the user-selected workspace."
    private val files = SafWorkspaceManager(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"]?.trim().orEmpty(); if (path.isBlank()) return ToolResult.Failure("path is required")
        val content = files.readFile(path) ?: return ToolResult.Failure("File not found or unreadable: $path")
        return ToolResult.Success("File read", mapOf("path" to path, "content" to content.take(120000)))
    }
}

private class FileWriteTool(private val context: Context) : JarvisTool {
    override val name = "write_file"
    override val description = "Create or overwrite a text/code file in the user-selected workspace; requires confirmation."
    private val files = SafWorkspaceManager(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"]?.trim().orEmpty(); val content = arguments["content"] ?: ""
        if (path.isBlank()) return ToolResult.Failure("path is required")
        if (!arguments["confirmed"].equals("true", true)) return ToolResult.NeedsConfirmation(name, "نوشتن/جایگزینی فایل $path")
        val ok = files.writeFile(path, content)
        return if (ok) ToolResult.Success("File written", mapOf("path" to path)) else ToolResult.Failure("Could not write $path. Ensure the parent directory exists and the selected provider permits writing.")
    }
}

private class FileZipTool(private val context: Context) : JarvisTool {
    override val name = "zip_workspace"
    override val description = "Create a real ZIP archive of the user-selected workspace."
    private val files = SafWorkspaceManager(context)
    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        if (!arguments["confirmed"].equals("true", true)) return ToolResult.NeedsConfirmation(name, "ساخت ZIP از فایل‌های پوشه کاری انتخاب‌شده")
        val name = arguments["name"]?.trim().takeUnless { it.isNullOrBlank() } ?: "jarvis_project.zip"
        val uri = files.writeZip(name) ?: return ToolResult.Failure("Could not create ZIP. Select a writable workspace and make sure it contains files.")
        return ToolResult.Success("ZIP created", mapOf("name" to name, "uri" to uri.toString()))
    }
}


/** Executes a selected workspace source file inside a private Termux workspace.
 *  Files are transferred over stdin rather than assuming a SAF URI has a real POSIX path.
 */
private class CodeExecuteTool(private val context: Context) : JarvisTool {
    override val name = "execute_code"
    override val description = "Run a Python, C/C++ or shell source file from the selected workspace in Termux and return stdout, stderr and exit code."
    private val workspace = SafWorkspaceManager(context)
    private val termux = TermuxExecutor(context)

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"]?.trim().orEmpty()
        val language = arguments["language"]?.trim()?.lowercase().orEmpty()
        if (path.isBlank()) return ToolResult.Failure("path is required")
        if (language !in setOf("python", "py", "cpp", "c++", "c", "bash", "sh")) {
            return ToolResult.Failure("Unsupported language: $language")
        }
        val source = workspace.readFile(path) ?: return ToolResult.Failure("Cannot read workspace file: $path")
        if (source.toByteArray(Charsets.UTF_8).size > 350_000) {
            return ToolResult.Failure("Source file is too large for the safe local execution bridge")
        }
        if (!termux.isTermuxInstalled()) return ToolResult.Failure("Termux is not installed")

        val safeName = path.substringAfterLast('/').replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val ext = when (language) {
            "python", "py" -> ".py"
            "cpp", "c++" -> ".cpp"
            "c" -> ".c"
            else -> ".sh"
        }
        val file = ".jarvis_exec/$safeName$ext"
        val base64 = android.util.Base64.encodeToString(source.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val write = termux.executeTermuxCommand(
            "mkdir -p ~/.jarvis_exec && base64 -d > '$file'",
            stdin = base64
        )
        if (!write.success) return ToolResult.Failure("Could not transfer source to Termux: ${write.stderr.ifBlank { write.errorMessage ?: "unknown error" }}")

        val command = when (language) {
            "python", "py" -> "python '$file'"
            "cpp", "c++" -> "clang++ '$file' -std=c++17 -O2 -o '${file.removeSuffix(".cpp")}.out' && '${file.removeSuffix(".cpp")}.out'"
            "c" -> "clang '$file' -std=c17 -O2 -o '${file.removeSuffix(".c")}.out' && '${file.removeSuffix(".c")}.out'"
            else -> "bash '$file'"
        }
        val result = termux.executeTermuxCommand(command, timeoutMs = arguments["timeout_ms"]?.toLongOrNull()?.coerceIn(1000, 120_000) ?: 30_000)
        val data = mapOf(
            "path" to path,
            "language" to language,
            "exit_code" to result.exitCode.toString(),
            "stdout" to result.stdout.take(80_000),
            "stderr" to result.stderr.take(40_000)
        )
        return if (result.success) ToolResult.Success("Code executed successfully", data)
        else ToolResult.Failure("Code execution failed", data)
    }
}

private class CodeInstallDependencyTool(private val context: Context) : JarvisTool {
    override val name = "install_code_dependency"
    override val description = "Install a Python or Termux package for the current coding task; always requires explicit confirmation."
    private val termux = TermuxExecutor(context)

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val manager = arguments["manager"]?.trim()?.lowercase().orEmpty()
        val packageName = arguments["package"]?.trim().orEmpty()
        if (packageName.isBlank()) return ToolResult.Failure("package is required")
        if (!packageName.matches(Regex("[A-Za-z0-9_.:+@/-]{1,120}"))) return ToolResult.Failure("Invalid package name")
        if (!arguments["confirmed"].equals("true", ignoreCase = true)) {
            return ToolResult.NeedsConfirmation(name, "نصب وابستگی $packageName با $manager در Termux")
        }
        val command = when (manager) {
            "pip", "python" -> "python -m pip install $packageName"
            "pkg", "apt" -> "pkg install -y $packageName"
            else -> return ToolResult.Failure("Unsupported package manager: $manager")
        }
        val result = termux.executeTermuxCommand(command, timeoutMs = 120_000)
        return if (result.success) ToolResult.Success("Dependency installed", mapOf("package" to packageName, "stdout" to result.stdout.take(30_000)))
        else ToolResult.Failure("Dependency installation failed", mapOf("package" to packageName, "stderr" to result.stderr.take(30_000), "stdout" to result.stdout.take(20_000)))
    }
}
private class OpenFileTool(private val context: Context) : JarvisTool {
    override val name = "open_file"
    override val description = "Open a user-selected workspace file with a compatible Android app. Uses Android MIME resolution and never requires a server."

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"]?.trim().orEmpty()
        if (path.isBlank()) return ToolResult.Failure("path is required")

        val workspace = SafWorkspaceManager(context)
        val uri = workspace.resolveFileUri(path)
            ?: return ToolResult.Failure("File not found in the selected workspace: $path")
        val mime = workspace.mimeType(path) ?: "application/octet-stream"
        val pm = context.packageManager
        val preferredPackage = arguments["package"]?.trim().orEmpty()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (preferredPackage.isNotBlank()) {
            val targeted = Intent(intent).setPackage(preferredPackage)
            if (targeted.resolveActivity(pm) != null) {
                context.startActivity(targeted)
                return ToolResult.Success(
                    "Opened $path",
                    mapOf("mime" to mime, "package" to preferredPackage, "local_only" to "true")
                )
            }
            return ToolResult.Failure("The selected app does not support this file type: $preferredPackage")
        }

        val handlers = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (handlers.isEmpty()) {
            return ToolResult.Failure("No installed Android app can open $path ($mime)")
        }

        if (handlers.size == 1) {
            context.startActivity(intent)
            return ToolResult.Success(
                "Opened $path",
                mapOf("mime" to mime, "package" to handlers.first().activityInfo.packageName, "local_only" to "true")
            )
        }

        // Multiple compatible apps: let Android's own chooser make the final selection.
        context.startActivity(Intent.createChooser(intent, "Open with"))
        return ToolResult.Success(
            "Several compatible apps were found; Android chooser opened",
            mapOf("mime" to mime, "handler_count" to handlers.size.toString(), "local_only" to "true")
        )
    }
}

