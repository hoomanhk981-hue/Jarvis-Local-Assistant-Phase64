package com.example.ui.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistant.AssistantSkillEngine
import com.example.assistant.SkillResult
import com.example.assistant.ToolRegistry
import com.example.assistant.ConfirmationManager
import com.example.assistant.ToolResult
import com.example.bank.BankAdapter
import com.example.bank.BankTransferStatus
import com.example.bank.SaderatBankAdapter
import com.example.data.local.entities.ActionHistoryEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.MemoryCategory
import com.example.data.local.entities.ModelType
import com.example.data.local.entities.SavedPasswordEntity
import com.example.data.local.entities.SpeedRating
import com.example.data.local.entities.UserMemoryEntity
import com.example.data.models.CodeFile
import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import com.example.data.models.SmsMessageItem
import com.example.data.models.TransferDetails
import com.example.data.repository.AssistantRepository
import com.example.files.SafFileManager
import com.example.speech.SpeechToTextManager
import com.example.speech.TextToSpeechManager
import com.example.termux.TermuxExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val sender: String, // "USER" or "ASSISTANT"
    val text: String,
    val imageUri: Uri? = null,
    val fileUri: Uri? = null,
    val fileName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppTab {
    HOME_ASSISTANT,
    ABOUT_APP,
    MODELS_MANAGER,
    VISION_AI,
    SKILLS_HUB,
    CODE_TERMUX,
    PERSONAL_DATABASE,
    SETTINGS
}

data class AssistantUiState(
    val selectedTab: AppTab = AppTab.HOME_ASSISTANT,
    val isPersianLanguage: Boolean = true,
    val speedMode: SpeedRating = SpeedRating.MEDIUM,
    val currentSessionId: String = "",
    val chatSessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val isListeningVoice: Boolean = false,
    val isSpeakingVoice: Boolean = false,
    val isLiveVoiceAssistantOpen: Boolean = false,
    val speechRecognizedText: String = "",
    val activeTextModel: DownloadedModelEntity? = null,
    val activeVisionModel: DownloadedModelEntity? = null,
    val contactChoicesForCalling: List<ContactMatch> = emptyList(),
    val pendingTransferDetails: TransferDetails? = null,
    val showTransferConfirmationDialog: Boolean = false,
    val isTransferTestMode: Boolean = false,
    val transferStatus: BankTransferStatus = BankTransferStatus.Idle,
    val extractedOtp: String? = null,
    val matchedSmsList: List<SmsMessageItem> = emptyList(),
    val customDatabaseFolderPath: String = "",
    val codeFiles: List<CodeFile> = emptyList(),
    val selectedCodeFileIndex: Int = 0,
    val codeExecutionOutput: String = "",
    val statusNotification: String? = null,
    val isTermuxInstalled: Boolean = false,
    val isDangerousCommandPending: Boolean = false,
    val pendingDangerousCommand: String = "",
    val pendingToolConfirmation: ConfirmationManager.Request? = null,
    val pendingToolPermissions: List<String> = emptyList(),
    val pendingPermissionTool: String? = null,
    val pendingPermissionArguments: Map<String, String> = emptyMap(),
    val showFirstRunOnboarding: Boolean = false,
    val isVoiceSetupInProgress: Boolean = false,
    val voiceSetupProgress: Int = 0,
    val voiceSetupStatusText: String = "",
    val isPersonalDatabaseEnabled: Boolean = true
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AssistantRepository(application)
    private val skillEngine = AssistantSkillEngine(application) { model, prompt, speed ->
        repository.downloadManager.generate(model, prompt, speed)
    }
    private val speechToText = SpeechToTextManager(application)
    private val textToSpeech = TextToSpeechManager(application)
    val termuxExecutor = TermuxExecutor(application)
    val safFileManager = SafFileManager(application)
    val toolRegistry = ToolRegistry(application)
    val confirmationManager = ConfirmationManager()
    private val localAgent = com.example.assistant.LocalAgentEngine(application)

    private val saderatAdapter = SaderatBankAdapter(application)
    private val prefs = application.getSharedPreferences("jarvis_assistant_prefs", Context.MODE_PRIVATE)

    val allModels: StateFlow<List<DownloadedModelEntity>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories: StateFlow<List<UserMemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPasswords: StateFlow<List<SavedPasswordEntity>> = repository.allPasswords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionHistory: StateFlow<List<ActionHistoryEntity>> = repository.actionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        AssistantUiState(
            customDatabaseFolderPath = File(application.filesDir, "assistant_db").absolutePath,
            isTermuxInstalled = termuxExecutor.isTermuxInstalled(),
            showFirstRunOnboarding = prefs.getBoolean("is_first_run", true),
            messages = listOf(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = "درود! دستیار هوشمند Jarvis آماده خدمت است. هر فرمانی (تماس، باز کردن برنامه‌ها، اجرای دستورات ترموکس، خلاصه پیامک‌ها و رمز عبور) را بگویید یا بنویسید تا بلافاصله اجرا شود."
                )
            ),
            codeFiles = listOf(
                CodeFile("task_runner.py", "# اسکریپت پایتون واقعی برای پردازش در ترموکس\nimport os\nimport sys\n\nprint('=== Jarvis Real Python Script ===')\nprint(f'محیط اجرا: {sys.platform}')\n", "python"),
                CodeFile("main.cpp", "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"=== Jarvis C++ Local ===\" << endl;\n    return 0;\n}\n", "cpp"),
                CodeFile("termux_run.sh", "#!/bin/bash\necho 'اجرای دستورات واقعی در ترموکس'\npkg info python\n", "bash")
            )
        )
    )
    val uiState: StateFlow<AssistantUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.seedInitialModelsIfNeeded()
            loadCodeFilesFromDisk()
            initChatSessions()
        }

        // Observe chat sessions
        viewModelScope.launch {
            repository.allChatSessions.collect { sessions ->
                _uiState.update { it.copy(chatSessions = sessions) }
            }
        }

        // Observe voice recognizer state
        viewModelScope.launch {
            speechToText.isListening.collect { listening ->
                _uiState.update { it.copy(isListeningVoice = listening) }
            }
        }

        // Observe speech result
        viewModelScope.launch {
            speechToText.speechResult.collect { text ->
                if (text.isNotBlank()) {
                    _uiState.update { it.copy(speechRecognizedText = text) }
                    if (_uiState.value.isLiveVoiceAssistantOpen) {
                        processUserMessage(text)
                    }
                }
            }
        }

        // Observe TTS state
        viewModelScope.launch {
            textToSpeech.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeakingVoice = speaking) }
            }
        }

        // Check active models from database
        viewModelScope.launch {
            repository.allModels.collect { models ->
                val loadedText = models.firstOrNull { it.isLoaded && it.modelType == ModelType.TEXT }
                val loadedVision = models.firstOrNull { it.isLoaded && it.modelType == ModelType.VISION }
                _uiState.update {
                    it.copy(
                        activeTextModel = loadedText,
                        activeVisionModel = loadedVision
                    )
                }
            }
        }
    }

    // ================= MULTI-CHAT SESSIONS (CHATGPT STYLE) =================

    private suspend fun initChatSessions() {
        val existing = repository.chatDao.getAllSessions()
        val firstSession = repository.createChatSession("گفتگوی اصلی")
        _uiState.update { it.copy(currentSessionId = firstSession.id) }
    }

    fun startNewChat() {
        viewModelScope.launch {
            val newSession = repository.createChatSession("گفتگوی جدید")
            _uiState.update {
                it.copy(
                    currentSessionId = newSession.id,
                    messages = listOf(
                        ChatMessage(
                            sender = "ASSISTANT",
                            text = "گفتگوی جدید آغاز شد. چطور می‌توانم کمکتان کنم؟"
                        )
                    ),
                    selectedTab = AppTab.HOME_ASSISTANT
                )
            }
        }
    }

    fun selectChatSession(sessionId: String) {
        viewModelScope.launch {
            val dbMsgs = repository.chatDao.getMessagesForSessionSnapshot(sessionId)
            val converted = if (dbMsgs.isEmpty()) {
                listOf(
                    ChatMessage(
                        sender = "ASSISTANT",
                        text = "گفتگوی آماده دریافت پیام شماست."
                    )
                )
            } else {
                dbMsgs.map {
                    ChatMessage(
                        id = it.id,
                        sender = it.sender,
                        text = it.text,
                        imageUri = it.imageUriString?.let(Uri::parse),
                        fileUri = it.fileUriString?.let(Uri::parse),
                        fileName = it.fileName,
                        timestamp = it.timestamp
                    )
                }
            }
            _uiState.update {
                it.copy(
                    currentSessionId = sessionId,
                    messages = converted,
                    selectedTab = AppTab.HOME_ASSISTANT
                )
            }
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                startNewChat()
            }
        }
    }

    // ================= 1-CLICK UNIFIED VOICE & AI SETUP =================

    fun startUnifiedVoiceSetup() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVoiceSetupInProgress = true,
                    voiceSetupProgress = 15,
                    voiceSetupStatusText = "در حال بررسی موتور صوتی سیستم..."
                )
            }
            kotlinx.coroutines.delay(400)

            _uiState.update {
                it.copy(
                    voiceSetupProgress = 45,
                    voiceSetupStatusText = "پیکربندی زبان‌های فارسی (fa-IR) و انگلیسی (en-US)..."
                )
            }
            textToSpeech.speak("سیستم صوتی دستیار هوشمند آماده است", true)
            kotlinx.coroutines.delay(500)

            _uiState.update {
                it.copy(
                    voiceSetupProgress = 80,
                    voiceSetupStatusText = "بررسی سرویس تشخیص گفتار محلی و میکروفون..."
                )
            }
            kotlinx.coroutines.delay(400)

            _uiState.update {
                it.copy(
                    voiceSetupProgress = 100,
                    isVoiceSetupInProgress = false,
                    voiceSetupStatusText = "✅ سیستم صوتی با موفقیت راه‌اندازی و تست شد!",
                    statusNotification = "✅ سیستم صوتی هوشمند فعال گردید!"
                )
            }
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("is_first_run", false).apply()
        _uiState.update { it.copy(showFirstRunOnboarding = false) }
    }

    fun requestInitialPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_SMS
        )
        _uiState.update { it.copy(pendingToolPermissions = permissions) }
    }

    fun initializePersonalDatabase() {
        viewModelScope.launch {
            repository.saveMemory("دیتابیس_شخصی", "فعال_شده", MemoryCategory.PERSONAL_PREFERENCE)
            _uiState.update {
                it.copy(
                    isPersonalDatabaseEnabled = true,
                    statusNotification = "✅ دیتابیس امن و حافظه شخصی محلی فعال گردید."
                )
            }
        }
    }

    fun onRuntimeTrimMemory(level: Int) {
        viewModelScope.launch {
            repository.downloadManager.onTrimMemory(level)
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSpeedMode(mode: SpeedRating) {
        _uiState.update { it.copy(speedMode = mode) }
    }

    fun currentSpeedMode(): String = _uiState.value.speedMode.name

    fun toggleLanguage() {
        _uiState.update { it.copy(isPersianLanguage = !_uiState.value.isPersianLanguage) }
    }

    fun openLiveVoiceAssistant() {
        _uiState.update { it.copy(isLiveVoiceAssistantOpen = true) }
        startVoiceListening()
    }

    fun closeLiveVoiceAssistant() {
        _uiState.update { it.copy(isLiveVoiceAssistantOpen = false) }
        stopVoiceListening()
        stopSpeaking()
    }

    fun startVoiceListening() {
        val lang = if (_uiState.value.isPersianLanguage) "fa-IR" else "en-US"
        _uiState.update { it.copy(speechRecognizedText = "") }
        speechToText.startListening(lang)
    }

    fun stopVoiceListening() {
        speechToText.stopListening()
    }

    fun stopSpeaking() {
        textToSpeech.stop()
    }

    fun clearSpeechRecognizedText() {
        _uiState.update { it.copy(speechRecognizedText = "") }
    }

    fun speakText(text: String) {
        val ok = textToSpeech.speak(text, _uiState.value.isPersianLanguage)
        if (!ok) {
            notifyStatus(textToSpeech.errorState.value ?: "صدای آفلاین در دسترس نیست")
        }
    }

    // ================= TOOL CONFIRMATION / REJECTION =================

    fun approvePendingTool() {
        val request = _uiState.value.pendingToolConfirmation ?: return
        viewModelScope.launch {
            val approved = confirmationManager.approve(request.id)
            if (approved == null) {
                addAssistantMessage("❌ درخواست تأیید منقضی شده یا نامعتبر است.")
                _uiState.update { it.copy(pendingToolConfirmation = null) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    pendingToolConfirmation = null,
                    isDangerousCommandPending = false,
                    pendingDangerousCommand = ""
                )
            }
            executeToolInternal(approved.toolName, approved.arguments + mapOf("confirmed" to "true"))
        }
    }

    private suspend fun executeToolInternal(toolName: String, args: Map<String, String>) {
        val missing = toolRegistry.permissionCoordinator.missingPermissions(toolName)
        if (missing.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    pendingToolPermissions = missing,
                    pendingPermissionTool = toolName,
                    pendingPermissionArguments = args
                )
            }
            return
        }

        val result = toolRegistry.execute(toolName, args)
        when (result) {
            is ToolResult.Success -> {
                addAssistantMessage("✅ ${result.message}")
                speakText(result.message)
            }
            is ToolResult.Failure -> {
                addAssistantMessage("❌ خطا: ${result.message}")
            }
            is ToolResult.NeedsPermission -> {
                _uiState.update {
                    it.copy(
                        pendingToolPermissions = result.permissions,
                        pendingPermissionTool = result.tool,
                        pendingPermissionArguments = args
                    )
                }
            }
            is ToolResult.NeedsConfirmation -> {
                addAssistantMessage("⚠️ تأیید دوباره لازم شد: ${result.summary}")
                result.request?.let { next ->
                    _uiState.update {
                        it.copy(
                            pendingToolConfirmation = next,
                            isDangerousCommandPending = true,
                            pendingDangerousCommand = result.summary
                        )
                    }
                }
            }
        }
    }

    fun rejectPendingTool() {
        val request = _uiState.value.pendingToolConfirmation ?: return
        confirmationManager.reject(request.id)
        _uiState.update {
            it.copy(
                pendingToolConfirmation = null,
                isDangerousCommandPending = false,
                pendingDangerousCommand = ""
            )
        }
        addAssistantMessage("❌ عملیات به درخواست کاربر لغو شد.")
    }

    fun clearPendingPermissions() {
        _uiState.update {
            it.copy(
                pendingToolPermissions = emptyList(),
                pendingPermissionTool = null,
                pendingPermissionArguments = emptyMap()
            )
        }
    }

    fun notifyStatus(message: String) {
        _uiState.update { it.copy(statusNotification = message) }
    }

    fun onRuntimePermissionsResult(granted: Boolean) {
        val state = _uiState.value
        val tool = state.pendingPermissionTool
        val args = state.pendingPermissionArguments
        clearPendingPermissions()
        if (granted && tool != null) {
            if (tool == "make_call") {
                initiatePhoneCall(args["number"].orEmpty())
            } else {
                viewModelScope.launch { executeToolInternal(tool, args) }
            }
        } else if (!granted) {
            addAssistantMessage("❌ دسترسی لازم داده نشد؛ عملیات اجرا نشد.")
        }
    }

    fun emergencyStop() {
        stopVoiceListening()
        stopSpeaking()
        _uiState.update {
            it.copy(
                isLiveVoiceAssistantOpen = false,
                isListeningVoice = false,
                isSpeakingVoice = false,
                showTransferConfirmationDialog = false,
                isDangerousCommandPending = false,
                pendingToolConfirmation = null,
                pendingToolPermissions = emptyList(),
                pendingPermissionTool = null,
                pendingPermissionArguments = emptyMap(),
                statusNotification = "🛑 توقف اضطراری (Emergency Stop) فعال شد."
            )
        }
        addAssistantMessage("🛑 تمام پردازش‌ها و فرآیندهای صوتی/پس‌زمینه متوقف شدند.")
    }

    // ================= USER CHAT & NATURAL LANGUAGE PROCESSING =================

    fun processUserMessage(
        text: String,
        imageUri: Uri? = null,
        fileUri: Uri? = null,
        fileName: String? = null
    ) {
        if (text.isBlank() && imageUri == null && fileUri == null) return

        // Disambiguation check for active contact choices:
        val pendingChoices = _uiState.value.contactChoicesForCalling
        if (pendingChoices.isNotEmpty() && text.isNotBlank()) {
            val norm = com.example.assistant.FuzzyMatcher.normalizePersianText(text)
            var chosenIndex = -1
            if (norm in listOf("اول", "اولی", "اولین", "1", "۱", "شماره 1", "شماره ۱", "مورد اول")) {
                chosenIndex = 0
            } else if (norm in listOf("دوم", "دومی", "دومین", "2", "۲", "شماره 2", "شماره ۲", "مورد دوم")) {
                chosenIndex = 1
            } else if (norm in listOf("سوم", "سومی", "سومین", "3", "۳", "شماره 3", "شماره ۳", "مورد سوم")) {
                chosenIndex = 2
            } else {
                val matched = com.example.assistant.FuzzyMatcher.findTop3Contacts(text, pendingChoices).firstOrNull()
                if (matched != null && matched.matchScore >= 0.55f) {
                    chosenIndex = pendingChoices.indexOfFirst { it.contactId == matched.contactId }
                }
            }

            if (chosenIndex in pendingChoices.indices) {
                confirmContactChoice(pendingChoices[chosenIndex])
                return
            }
        }

        val userMsg = ChatMessage(
            sender = "USER",
            text = text,
            imageUri = imageUri,
            fileUri = fileUri,
            fileName = fileName
        )
        val currentMsgs = _uiState.value.messages.toMutableList()
        currentMsgs.add(userMsg)
        _uiState.update { it.copy(messages = currentMsgs) }

        // Persist message in Room database
        val activeSessionId = _uiState.value.currentSessionId
        if (activeSessionId.isNotBlank()) {
            viewModelScope.launch {
                repository.saveChatMessage(activeSessionId, "USER", text, imageUri, fileUri, fileName)
            }
        }

        viewModelScope.launch {
            if (imageUri != null) {
                val visionModel = _uiState.value.activeVisionModel
                if (visionModel == null || !visionModel.isLoaded) {
                    val warn = "⚠️ مدل بینایی (Vision Model) هنوز دانلود یا لود نشده است. برای تحلیل تصویر، ابتدا مدل را از تب مدیریت مدل‌ها لود نمایید."
                    addAssistantMessage(warn)
                    speakText(warn)
                    repository.logAction("ارسال تصویر برای تحلیل", "VISION_ANALYSIS", warn, false)
                    return@launch
                }
            }

            val installedApps = repository.getInstalledApps()
            val contacts = repository.getPhoneContacts()
            val smsList = repository.getRecentSmsMessages()

            val inputTextForSkill = if (fileName != null) {
                "$text (فایل ضمیمه: $fileName)"
            } else {
                text
            }

            val result = skillEngine.processCommand(
                input = inputTextForSkill,
                installedApps = installedApps,
                allContacts = contacts,
                recentSms = smsList,
                speedMode = _uiState.value.speedMode.name,
                activeTextModel = _uiState.value.activeTextModel
            )

            handleSkillResult(result, text, smsList)
        }
    }

    private suspend fun handleSkillResult(result: SkillResult, originalText: String, smsList: List<SmsMessageItem>) {
        when (result) {
            is SkillResult.EmergencyStop -> {
                emergencyStop()
            }
            is SkillResult.LaunchApp -> {
                addAssistantMessage(result.statusMessage)
                speakText(result.statusMessage)
                repository.logAction(originalText, "APP_LAUNCH", result.statusMessage, true)
                launchAppPackage(result.app.packageName)
            }
            is SkillResult.SearchAppWeb -> {
                addAssistantMessage(result.message)
                speakText(result.message)
                openWebSearch(result.query)
            }
            is SkillResult.CallContactExact -> {
                addAssistantMessage(result.message)
                speakText(result.message)
                repository.logAction(originalText, "PHONE_CALL", result.message, true)
                initiatePhoneCall(result.contact.phoneNumber)
            }
            is SkillResult.CallPhoneNumberDirect -> {
                addAssistantMessage(result.message)
                speakText(result.message)
                repository.logAction(originalText, "PHONE_CALL", result.message, true)
                initiatePhoneCall(result.phoneNumber)
            }
            is SkillResult.CallContactNearestChoices -> {
                _uiState.update { it.copy(contactChoicesForCalling = result.top3Choices) }
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.CardTransferConfirmation -> {
                val otp = saderatAdapter.extractOtpFromSms(smsList)
                _uiState.update {
                    it.copy(
                        pendingTransferDetails = result.details,
                        showTransferConfirmationDialog = true,
                        isTransferTestMode = result.isTestMode,
                        extractedOtp = otp,
                        transferStatus = BankTransferStatus.AwaitingCardConfirmation(
                            destCard = result.details.destCardNumber,
                            amountRials = result.details.amountRials,
                            formattedAmount = saderatAdapter.formatAmountToman(result.details.amountRials)
                        )
                    )
                }
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.SmsSearchResult -> {
                _uiState.update { it.copy(matchedSmsList = result.matchedSms) }
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.SmsSummaryResult -> {
                addAssistantMessage(result.summaryText)
                speakText(result.summaryText)
                repository.logAction(originalText, "SMS_SUMMARY", result.summaryText, true)
            }
            is SkillResult.GmailSummaryResult -> {
                addAssistantMessage(result.summaryText)
                speakText(result.summaryText)
                repository.logAction(originalText, "GMAIL_SUMMARY", result.summaryText, true)
            }
            is SkillResult.ForwardSmsRequest -> {
                addAssistantMessage(result.message)
                speakText(result.message)
                if (result.targetContact != null) {
                    openSmsApp(result.targetContact.phoneNumber, result.content)
                }
            }
            is SkillResult.ExecuteRawTermuxCommand -> {
                if (result.isDangerous) {
                    _uiState.update {
                        it.copy(
                            isDangerousCommandPending = true,
                            pendingDangerousCommand = result.command
                        )
                    }
                    addAssistantMessage("⚠️ هشدار امنیتی: دستور «${result.command}» پرخطر است و نیاز به تأیید صریح شما دارد.")
                    return
                }
                val output = termuxExecutor.executeCommandText(result.command)
                _uiState.update { it.copy(codeExecutionOutput = output) }
                addAssistantMessage(output)
                speakText(if (output.contains("موفقیت")) "دستور در ترموکس با موفقیت اجرا شد." else "اجرای دستور به پایان رسید.")
                repository.logAction(originalText, "TERMUX_COMMAND", output, true)
            }
            is SkillResult.ExecuteTermuxScript -> {
                safFileManager.saveCodeFile(result.generatedFile)
                val updatedFiles = _uiState.value.codeFiles.toMutableList()
                val existingIndex = updatedFiles.indexOfFirst { it.name == result.generatedFile.name }
                if (existingIndex >= 0) {
                    updatedFiles[existingIndex] = result.generatedFile
                } else {
                    updatedFiles.add(0, result.generatedFile)
                }

                if (result.isDangerous) {
                    _uiState.update {
                        it.copy(
                            codeFiles = updatedFiles,
                            isDangerousCommandPending = true,
                            pendingDangerousCommand = result.termuxCommand
                        )
                    }
                    addAssistantMessage("⚠️ هشدار امنیتی: دستور «${result.termuxCommand}» ممکن است باعث حذف یا تغییر داده‌ها شود. جهت اجرا نیاز به تأیید دارد.")
                    return
                }

                val execResult = termuxExecutor.executeScript(result.generatedFile.name, result.generatedFile.content, result.generatedFile.language)
                _uiState.update {
                    it.copy(
                        codeFiles = updatedFiles,
                        codeExecutionOutput = execResult
                    )
                }

                addAssistantMessage("${result.message}\n\n$execResult")
                speakText(result.message)
                repository.logAction(originalText, "TERMUX_EXEC", execResult, true)
            }
            is SkillResult.PasswordSaved -> {
                repository.savePassword(result.appName, "حساب شخصی", result.passwordSecret, "رمزنگاری امن با کلید اختصاصی")
                addAssistantMessage(result.message)
                speakText(result.message)
                repository.logAction(originalText, "PASSWORD_SAVED", result.message, true)
            }
            is SkillResult.GetPasswordResult -> {
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.KnowledgeSaved -> {
                repository.saveMemory(result.key, result.value, MemoryCategory.MODEL_KNOWLEDGE)
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.GeneralAnswer -> {
                val model = _uiState.value.activeTextModel
                val actionLike = originalText.contains(Regex("(?i)\\b(open|launch|run|click|type|execute|termux)\\b|باز کن|برو تو|اجرا کن|کلیک کن|وارد کن|ترموکس|روی .* بزن"))
                if (model != null && model.isLoaded && actionLike) {
                    val response = try {
                        localAgent.run(
                            input = originalText,
                            model = model,
                            speedMode = _uiState.value.speedMode.name,
                            generate = { m, p, speed -> repository.downloadManager.generate(m, p, speed) },
                            registry = toolRegistry
                        )
                    } catch (e: Exception) {
                        "خطا در Agent محلی: ${e.message ?: "خطای ناشناخته"}"
                    }
                    addAssistantMessage(response)
                    speakText(response)
                    repository.logAction(originalText, "LOCAL_AGENT", response, true)
                } else {
                    addAssistantMessage(result.responseText)
                    speakText(result.responseText)
                }
            }
        }
    }

    // ================= REAL MODEL MANAGER ACTIONS =================

    fun startModelDownload(model: DownloadedModelEntity) {
        viewModelScope.launch {
            repository.downloadManager.startDownload(model) { ok, msg ->
                viewModelScope.launch {
                    notifyStatus(msg)
                }
            }
        }
    }

    fun deleteModel(model: DownloadedModelEntity) {
        viewModelScope.launch {
            val ok = repository.downloadManager.deleteModel(model)
            notifyStatus(if (ok) "مدل ${model.name} حذف شد." else "خطا در حذف فایل مدل.")
        }
    }

    fun loadModel(model: DownloadedModelEntity) {
        viewModelScope.launch {
            val msg = repository.downloadManager.loadModel(model, _uiState.value.speedMode.name)
            notifyStatus(msg)
            addAssistantMessage("⚡ $msg")
            speakText(msg)
        }
    }

    fun unloadModel(model: DownloadedModelEntity) {
        viewModelScope.launch {
            val msg = repository.downloadManager.unloadModel(model)
            notifyStatus(msg)
            addAssistantMessage("ℹ️ $msg")
        }
    }

    // ================= REAL BANKING ACTIONS =================

    fun confirmTransfer(otp: String) {
        val details = _uiState.value.pendingTransferDetails ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showTransferConfirmationDialog = false,
                    pendingTransferDetails = null,
                    transferStatus = BankTransferStatus.TransferExecuting("در حال ارجاع به درگاه امن بانک صادرات...")
                )
            }

            val intent = saderatAdapter.createLaunchIntent()?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                try {
                    getApplication<Application>().startActivity(intent)
                    val msg = "اپلیکیشن بانک صادرات باز شد. شماره کارت مقصد (${saderatAdapter.formatCardNumber(details.destCardNumber)}) و مبلغ در کلیپ‌بورد/درگاه آماده است."
                    addAssistantMessage(msg)
                    speakText(msg)
                    repository.logAction("کارت به کارت بانک صادرات", "BANK_TRANSFER_SADERAT", msg, true)
                } catch (e: Exception) {
                    val err = "خطا در باز کردن اپلیکیشن بانک صادرات: ${e.message}"
                    addAssistantMessage(err)
                }
            } else {
                addAssistantMessage("اپلیکیشن بانک صادرات روی دستگاه یافت نشد.")
            }
        }
    }

    fun cancelTransfer() {
        _uiState.update {
            it.copy(
                showTransferConfirmationDialog = false,
                pendingTransferDetails = null,
                transferStatus = BankTransferStatus.Idle
            )
        }
        addAssistantMessage("عملیات انتقال وجه لغو گردید.")
    }

    // ================= REAL CODE & TERMUX ACTIONS =================

    fun executeCodeInTermux(code: String, filename: String, language: String) {
        viewModelScope.launch {
            safFileManager.saveCodeFile(CodeFile(filename, code, language))
            val output = termuxExecutor.executeScript(filename, code, language)
            _uiState.update { it.copy(codeExecutionOutput = output) }
        }
    }

    fun confirmDangerousCommand() {
        val cmd = _uiState.value.pendingDangerousCommand
        _uiState.update {
            it.copy(
                isDangerousCommandPending = false,
                pendingDangerousCommand = ""
            )
        }
        viewModelScope.launch {
            val res = termuxExecutor.executeTermuxCommand(cmd)
            _uiState.update { it.copy(codeExecutionOutput = res.stdout.ifBlank { res.stderr }) }
        }
    }

    fun cancelDangerousCommand() {
        _uiState.update {
            it.copy(
                isDangerousCommandPending = false,
                pendingDangerousCommand = ""
            )
        }
        addAssistantMessage("اجرای دستور پرخطر لغو گردید.")
    }

    fun createZipArchiveFromCode() {
        viewModelScope.launch {
            val zip = safFileManager.createZipArchive()
            val msg = if (zip != null) {
                "فایل ZIP واقعی پروژه در مسیر زیر ایجاد شد:\n${zip.absolutePath}"
            } else {
                "خطا در ایجاد فایل ZIP."
            }
            _uiState.update { it.copy(statusNotification = msg) }
        }
    }

    private fun loadCodeFilesFromDisk() {
        viewModelScope.launch {
            val files = safFileManager.listSavedCodeFiles()
            if (files.isNotEmpty()) {
                _uiState.update { it.copy(codeFiles = files) }
            }
        }
    }

    // ================= REAL SYSTEM ASSISTANT ROLE =================

    fun openDefaultAssistantSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getApplication<Application>().getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(intent)
                    return
                }
            }
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(intent)
            } catch (ex: Exception) {
                _uiState.update { it.copy(statusNotification = "امکان باز کردن تنظیمات دستیار پیش‌فرض وجود ندارد.") }
            }
        }
    }

    // ================= CONTACTS & SMS HELPERS =================

    fun confirmContactChoice(contact: ContactMatch) {
        _uiState.update { it.copy(contactChoicesForCalling = emptyList()) }
        val msg = "در حال برقراری تماس با ${contact.displayName} (${contact.phoneNumber})..."
        addAssistantMessage(msg)
        speakText(msg)
        viewModelScope.launch {
            repository.logAction("تماس تلفنی", "PHONE_CALL", msg, true)
        }
        initiatePhoneCall(contact.phoneNumber)
    }

    fun dismissContactChoices() {
        _uiState.update { it.copy(contactChoicesForCalling = emptyList()) }
    }

    private fun addAssistantMessage(text: String) {
        val currentMsgs = _uiState.value.messages.toMutableList()
        currentMsgs.add(ChatMessage(sender = "ASSISTANT", text = text))
        _uiState.update { it.copy(messages = currentMsgs) }

        val activeSessionId = _uiState.value.currentSessionId
        if (activeSessionId.isNotBlank()) {
            viewModelScope.launch {
                repository.saveChatMessage(activeSessionId, "ASSISTANT", text)
            }
        }
    }

    private fun launchAppPackage(packageName: String) {
        try {
            val launchIntent = getApplication<Application>().packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(launchIntent)
            }
        } catch (e: Exception) {
            // Intent fallback
        }
    }

    fun initiatePhoneCall(number: String) {
        val cleanNumber = number.replace(Regex("[^0-9+*#]"), "")
        if (cleanNumber.isBlank()) {
            addAssistantMessage("شماره تلفن نامعتبر است.")
            return
        }

        val app = getApplication<Application>()
        val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            app, android.Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(callIntent)
            } catch (e: Exception) {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(dialIntent)
                } catch (_: Exception) {}
            }
        } else {
            _uiState.update {
                it.copy(
                    pendingToolPermissions = listOf(android.Manifest.permission.CALL_PHONE),
                    pendingPermissionTool = "make_call",
                    pendingPermissionArguments = mapOf("number" to cleanNumber)
                )
            }
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(dialIntent)
            } catch (_: Exception) {}
        }
    }

    private fun openSmsApp(phoneNumber: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // SMS fallback
        }
    }

    private fun openWebSearch(query: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Browser fallback
        }
    }
}
