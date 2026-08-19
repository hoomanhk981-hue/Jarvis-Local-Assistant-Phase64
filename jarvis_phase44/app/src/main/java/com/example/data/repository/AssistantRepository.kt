package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.Telephony
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ActionHistoryEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.MemoryCategory
import com.example.data.local.entities.ModelType
import com.example.data.local.entities.SavedPasswordEntity
import com.example.data.local.entities.SpeedRating
import com.example.data.local.entities.UserMemoryEntity
import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import com.example.data.models.RealModelDownloadManager
import com.example.data.models.SmsMessageItem
import com.example.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AssistantRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    val modelDao = db.modelDao()
    val chatDao = db.chatDao()
    private val memoryDao = db.userMemoryDao()
    private val passwordDao = db.savedPasswordDao()
    private val actionDao = db.actionHistoryDao()

    val securityManager = SecurityManager(context)
    val downloadManager = RealModelDownloadManager(context, modelDao)

    val allModels: Flow<List<DownloadedModelEntity>> = modelDao.getAllModels()
    val allMemories: Flow<List<UserMemoryEntity>> = memoryDao.getAllMemories()
    val allPasswords: Flow<List<SavedPasswordEntity>> = passwordDao.getAllPasswords()
    val actionHistory: Flow<List<ActionHistoryEntity>> = actionDao.getRecentHistory()
    val allChatSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun createChatSession(title: String = "گفتگوی جدید"): ChatSessionEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val session = ChatSessionEntity(id = id, title = title)
        chatDao.insertOrUpdateSession(session)
        session
    }

    suspend fun saveChatMessage(
        sessionId: String,
        sender: String,
        text: String,
        imageUri: Uri? = null,
        fileUri: Uri? = null,
        fileName: String? = null
    ): ChatMessageEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val msg = ChatMessageEntity(
            id = id,
            sessionId = sessionId,
            sender = sender,
            text = text,
            imageUriString = imageUri?.toString(),
            fileUriString = fileUri?.toString(),
            fileName = fileName,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(msg)
        val existingSession = chatDao.getSessionById(sessionId)
        if (existingSession != null && (existingSession.title == "گفتگوی جدید" || existingSession.title == "New Chat") && sender == "USER" && text.isNotBlank()) {
            chatDao.updateSessionTitle(sessionId, text.take(35))
        }
        msg
    }

    suspend fun deleteChatSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun updateChatTitle(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    /**
     * Seeds initial real open-source GGUF model registry.
     */
    suspend fun seedInitialModelsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = modelDao.getAllModels().first()
        if (existing.isEmpty()) {
            val presets = listOf(
                DownloadedModelEntity(
                    id = "qwen2.5_0.5b",
                    name = "Qwen 2.5 0.5B Instruct GGUF",
                    description = "مدل فوق‌العاده سبک و پرسرعت با پشتیبانی قوی از زبان فارسی و انگلیسی برای موبایل",
                    sizeBytes = 398000000L,
                    sizeFormatted = "398 MB",
                    ramRequiredMb = 800,
                    requiredAbi = "arm64-v8a",
                    license = "Apache 2.0 (Open Source)",
                    modelType = ModelType.TEXT,
                    speedRating = SpeedRating.LOW,
                    isDownloaded = false,
                    downloadProgressPercentage = 0,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
                ),
                DownloadedModelEntity(
                    id = "smollm2_360m",
                    name = "SmolLM2 360M UltraFast",
                    description = "مدل فوق‌العاده کم‌حجم هاگینگ‌فیس با پاسخگویی آنی و مصرف رم بهینه",
                    sizeBytes = 270590880L,
                    sizeFormatted = "258 MB",
                    ramRequiredMb = 512,
                    requiredAbi = "arm64-v8a / armeabi-v7a",
                    license = "Apache 2.0",
                    modelType = ModelType.TEXT,
                    speedRating = SpeedRating.LOW,
                    isDownloaded = false,
                    downloadProgressPercentage = 0,
                    downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf"
                ),
                DownloadedModelEntity(
                    id = "llama3.2_1b",
                    name = "Llama 3.2 1B Mobile Instruct",
                    description = "مدل قدرتمند متنی متا مخصوص اجرای روی دیوایس‌های اندرویدی",
                    sizeBytes = 850000000L,
                    sizeFormatted = "850 MB",
                    ramRequiredMb = 1600,
                    requiredAbi = "arm64-v8a",
                    license = "Llama 3.2 Community License",
                    modelType = ModelType.TEXT,
                    speedRating = SpeedRating.MEDIUM,
                    isDownloaded = false,
                    downloadProgressPercentage = 0,
                    downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
                ),
                DownloadedModelEntity(
                    id = "qwen2.5_1.5b",
                    name = "Qwen 2.5 1.5B High Reasoning",
                    description = "مدل سطح بالا برای حل مسائل پیچیده، استدلال و کدنویسی در حالت دقت حداکثری",
                    sizeBytes = 1150000000L,
                    sizeFormatted = "1.15 GB",
                    ramRequiredMb = 2400,
                    requiredAbi = "arm64-v8a",
                    license = "Apache 2.0",
                    modelType = ModelType.TEXT,
                    speedRating = SpeedRating.HIGH,
                    isDownloaded = false,
                    downloadProgressPercentage = 0,
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                    checksumSha256 = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
                ),
                DownloadedModelEntity(
                    id = "qwen2_vl_2b",
                    name = "Qwen 2-VL 2B Vision AI",
                    description = "مدل بینایی مستقل (Vision Model) برای تحلیل و توصیف تصاویر و OCR متون",
                    sizeBytes = 1850000000L,
                    sizeFormatted = "1.85 GB",
                    ramRequiredMb = 3200,
                    requiredAbi = "arm64-v8a",
                    license = "Apache 2.0",
                    modelType = ModelType.VISION,
                    speedRating = SpeedRating.HIGH,
                    isDownloaded = false,
                    downloadProgressPercentage = 0,
                    downloadUrl = "https://huggingface.co/ggml-org/Qwen2-VL-2B-Instruct-GGUF/resolve/main/Qwen2-VL-2B-Instruct-Q4_K_M.gguf",
                    auxiliaryDownloadUrl = "https://huggingface.co/ggml-org/Qwen2-VL-2B-Instruct-GGUF/resolve/main/mmproj-Qwen2-VL-2B-Instruct-f16.gguf",
                    auxiliaryFileName = "qwen2_vl_2b-mmproj-f16.gguf"
                )
            )
            modelDao.insertAll(presets)
        }
    }

    suspend fun searchMemories(query: String): List<UserMemoryEntity> = withContext(Dispatchers.IO) {
        memoryDao.searchMemories(query.trim()).take(20)
    }

    suspend fun saveMemory(key: String, value: String, category: MemoryCategory, customFolder: String = "") {
        memoryDao.insertMemory(
            UserMemoryEntity(
                key = key,
                value = value,
                category = category,
                sourceFolder = customFolder
            )
        )
    }

    suspend fun deleteMemory(memory: UserMemoryEntity) {
        memoryDao.deleteMemory(memory.id)
    }

    suspend fun savePassword(appName: String, accountName: String, plainPassword: String, notes: String) {
        val encrypted = securityManager.encrypt(plainPassword)
        passwordDao.insertPassword(
            SavedPasswordEntity(
                appName = appName,
                accountName = accountName,
                passwordEncrypted = encrypted,
                notes = notes
            )
        )
    }

    suspend fun deletePassword(passwordEntity: SavedPasswordEntity) {
        passwordDao.deletePassword(passwordEntity.id)
    }

    suspend fun logAction(command: String, skillExecuted: String, summary: String, isSuccess: Boolean) {
        val safeCommand = securityManager.sanitizeForAuditLog(command)
        val safeSummary = securityManager.sanitizeForAuditLog(summary)
        actionDao.insertAction(
            ActionHistoryEntity(
                commandText = safeCommand,
                skillExecuted = skillExecuted,
                resultSummary = safeSummary,
                isSuccess = isSuccess
            )
        )
    }

    /**
     * Reads genuine installed apps on the device using PackageManager.
     * Combines launcher activities with installed packages for complete resolution.
     */
    fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val appsMap = LinkedHashMap<String, InstalledAppInfo>()
        try {
            // 1. Query all launchable activities (standard launcher apps)
            val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launchIntent, 0)
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                val name = resolveInfo.loadLabel(pm)?.toString()?.trim() ?: pkgName
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                appsMap[pkgName] = InstalledAppInfo(appName = name, packageName = pkgName, isSystemApp = isSystem)
            }

            // 2. Also query all installed applications to capture background tools
            val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedPackages) {
                if (!appsMap.containsKey(appInfo.packageName)) {
                    val name = pm.getApplicationLabel(appInfo).toString().trim()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    appsMap[appInfo.packageName] = InstalledAppInfo(appName = name, packageName = appInfo.packageName, isSystemApp = isSystem)
                }
            }
        } catch (e: Exception) {
            // Return collected apps
        }
        return appsMap.values.toList()
    }

    /**
     * Reads real phone contacts via ContactsContract.
     * Never returns fake mock contacts.
     */
    fun getPhoneContacts(): List<ContactMatch> {
        val contacts = mutableListOf<ContactMatch>()
        val cr = context.contentResolver

        try {
            val cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else "مخاطب"
                    val number = if (numIdx >= 0) it.getString(numIdx) else ""
                    if (number.isNotBlank()) {
                        contacts.add(ContactMatch(contactId = id, displayName = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            // Truthful: return empty list on permission or query failure
        }

        return contacts
    }

    /**
     * Reads genuine SMS messages via Telephony.Sms.
     * Never returns fake mock SMS.
     */
    fun getRecentSmsMessages(limit: Int = 30): List<SmsMessageItem> {
        val messages = mutableListOf<SmsMessageItem>()
        val cr = context.contentResolver

        try {
            val cursor = cr.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null, null, "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else ""
                    val sender = if (addrIdx >= 0) it.getString(addrIdx) else "ناشناس"
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else System.currentTimeMillis()

                    val category = when {
                        body.contains("رمز پویا") || body.contains("کد تایید") || body.contains("OTP") -> "OTP"
                        body.contains("بلیط") || body.contains("رزرو") || body.contains("سامانه") -> "TICKET"
                        body.contains("بانک") || body.contains("واریز") || body.contains("برداشت") || body.contains("حساب") -> "BANK"
                        else -> "GENERAL"
                    }

                    messages.add(SmsMessageItem(id = id, sender = sender, body = body, timestamp = date, category = category))
                }
            }
        } catch (e: Exception) {
            // Truthful: return empty list on failure
        }

        return messages
    }

    /**
     * Exports database memories & passwords into a custom folder JSON file.
     */
    suspend fun exportToJsonFile(targetDir: File): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val memories = memoryDao.getAllMemories().first()
            val passwords = passwordDao.getAllPasswords().first()

            val rootJson = JSONObject()
            val memArray = JSONArray()
            memories.forEach { m ->
                val obj = JSONObject()
                obj.put("key", m.key)
                obj.put("value", m.value)
                obj.put("category", m.category.name)
                memArray.put(obj)
            }

            val passArray = JSONArray()
            passwords.forEach { p ->
                val obj = JSONObject()
                obj.put("appName", p.appName)
                obj.put("accountName", p.accountName)
                obj.put("passwordEncrypted", p.passwordEncrypted)
                passArray.put(obj)
            }

            rootJson.put("user_memories", memArray)
            rootJson.put("saved_passwords", passArray)

            if (!targetDir.exists()) targetDir.mkdirs()
            val outFile = File(targetDir, "personal_assistant_db.json")
            outFile.writeText(rootJson.toString(2))
            outFile
        } catch (e: Exception) {
            null
        }
    }
}
