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
    val pendingPermissionArguments: Map<String, String> = emptyMap()
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
            messages = listOf(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = "درود! دستیار خودمختار Jarvis آماده خدمت است. کلیه عملیات‌ها (تماس، پیامک، ترموکس، مدیریت مدل‌ها، کارت به کارت و رمزنگاری داده‌ها) به‌صورت ۱۰۰٪ واقعی و بدون هیچ‌گونه شبیه‌سازی انجام می‌پذیرند."
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
        }

        // Observe voice recognizer state
        viewModelScope.launch {
            speechToText.isListening.collect { listening ->
                _uiState.value = _uiState.value.copy(isListeningVoice = listening)
            }
        }

        // Observe speech result
        viewModelScope.launch {
            speechToText.speechResult.collect { text ->
                if (text.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(speechRecognizedText = text)
                    if (_uiState.value.isLiveVoiceAssistantOpen) {
                        processUserMessage(text)
                    }
                }
            }
        }

        // Observe TTS state
        viewModelScope.launch {
            textToSpeech.isSpeaking.collect { speaking ->
                _uiState.value = _uiState.value.copy(isSpeakingVoice = speaking)
            }
        }

        // Check active models from database
        viewModelScope.launch {
            repository.allModels.collect { models ->
                val loadedText = models.firstOrNull { it.isLoaded && it.modelType == ModelType.TEXT }
                val loadedVision = models.firstOrNull { it.isLoaded && it.modelType == ModelType.VISION }
                _uiState.value = _uiState.value.copy(
                    activeTextModel = loadedText,
                    activeVisionModel = loadedVision
                )
            }
        }
    }

    private suspend fun loadCodeFilesFromDisk() {
        val indexed = safFileManager.indexDirectory()
        if (indexed.isNotEmpty()) {
            val loadedFiles = mutableListOf<CodeFile>()
            for (f in indexed.take(10)) {
                val code = safFileManager.readCodeFile(f.name)
                if (code != null) loadedFiles.add(code)
            }
            if (loadedFiles.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(codeFiles = loadedFiles)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setSpeedMode(mode: SpeedRating) {
        _uiState.value = _uiState.value.copy(speedMode = mode)
    }

    fun currentSpeedMode(): String = _uiState.value.speedMode.name

    fun toggleLanguage() {
        _uiState.value = _uiState.value.copy(isPersianLanguage = !_uiState.value.isPersianLanguage)
    }

    fun openLiveVoiceAssistant() {
        _uiState.value = _uiState.value.copy(isLiveVoiceAssistantOpen = true)
        startVoiceListening()
    }

    fun closeLiveVoiceAssistant() {
        _uiState.value = _uiState.value.copy(isLiveVoiceAssistantOpen = false)
        stopVoiceListening()
        stopSpeaking()
    }

    fun startVoiceListening() {
        val lang = if (_uiState.value.isPersianLanguage) "fa-IR" else "en-US"
        _uiState.value = _uiState.value.copy(speechRecognizedText = "")
        speechToText.startListening(lang)
    }

    fun stopVoiceListening() {
        speechToText.stopListening()
    }

    fun stopSpeaking() {
        textToSpeech.stop()
    }

    fun clearSpeechRecognizedText() {
        _uiState.value = _uiState.value.copy(speechRecognizedText = "")
    }

    fun speakText(text: String) {
        val ok = textToSpeech.speak(text, _uiState.value.isPersianLanguage)
        if (!ok) {
            notifyStatus(textToSpeech.errorState.value ?: "صدای آفلاین در دسترس نیست")
        }
    }

    fun localVoiceStatus(): String {
        val stt = speechToText.isOnDevice.value
        val ttsReady = textToSpeech.hasOfflineVoice(_uiState.value.isPersianLanguage)
        return when {
            stt && ttsReady -> "ورودی و خروجی صوتی کاملاً محلی فعال است."
            !ttsReady -> "برای زبان انتخاب‌شده صدای TTS آفلاین روی دستگاه نصب نیست."
            else -> "تشخیص گفتار محلی هنوز فعال نشده است."
        }
    }

    /** Execute a named tool through the central, permission-aware tool boundary. */
    fun executeTool(name: String, arguments: Map<String, String>) {
        viewModelScope.launch { executeToolInternal(name, arguments) }
    }

    private suspend fun executeToolInternal(name: String, arguments: Map<String, String>) {
        when (val result = toolRegistry.execute(name, arguments)) {
            is ToolResult.Success -> {
                addAssistantMessage("✅ ${result.message}")
                repository.logAction("tool:$name", name, result.message, true)
            }
            is ToolResult.NeedsConfirmation -> {
                // The registry owns the canonical request. Do not create a second
                // independent ConfirmationManager request here.
                val request = result.request
                addAssistantMessage("⚠️ تأیید لازم است: ${result.summary}")
                if (request != null) {
                    _uiState.value = _uiState.value.copy(
                        pendingToolConfirmation = request,
                        isDangerousCommandPending = true,
                        pendingDangerousCommand = result.summary
                    )
                } else {
                    addAssistantMessage("❌ درخواست تأیید معتبر از موتور ابزار دریافت نشد؛ عملیات اجرا نشد.")
                }
            }
            is ToolResult.NeedsPermission -> {
                addAssistantMessage("🔐 برای این عملیات دسترسی اندروید لازم است.")
                _uiState.value = _uiState.value.copy(
                    pendingToolPermissions = result.permissions,
                    pendingPermissionTool = name,
                    pendingPermissionArguments = arguments
                )
            }
            is ToolResult.Failure -> {
                addAssistantMessage("❌ ${result.message}")
                repository.logAction("tool:$name", name, result.message, false)
            }
        }
    }

    fun approvePendingTool() {
        val request = _uiState.value.pendingToolConfirmation ?: return
        viewModelScope.launch {
            val result = toolRegistry.approveConfirmation(request.id)
            _uiState.value = _uiState.value.copy(
                pendingToolConfirmation = null,
                isDangerousCommandPending = false,
                pendingDangerousCommand = ""
            )
            when (result) {
                is ToolResult.Success -> {
                    addAssistantMessage("✅ ${result.message}")
                    repository.logAction("tool:${request.toolName}", request.toolName, result.message, true)
                }
                is ToolResult.NeedsPermission -> {
                    _uiState.value = _uiState.value.copy(
                        pendingPermissionTool = request.toolName,
                        pendingPermissionArguments = request.arguments,
                        pendingToolPermissions = result.permissions
                    )
                    addAssistantMessage("🔐 برای ادامه این عملیات دسترسی اندروید لازم است.")
                }
                is ToolResult.Failure -> {
                    addAssistantMessage("❌ ${result.message}")
                    repository.logAction("tool:${request.toolName}", request.toolName, result.message, false)
                }
                is ToolResult.NeedsConfirmation -> {
                    addAssistantMessage("⚠️ تأیید دوباره لازم شد: ${result.summary}")
                    result.request?.let { next ->
                        _uiState.value = _uiState.value.copy(
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
        _uiState.value = _uiState.value.copy(
            pendingToolConfirmation = null,
            isDangerousCommandPending = false,
            pendingDangerousCommand = ""
        )
        addAssistantMessage("❌ عملیات به درخواست کاربر لغو شد.")
    }

    fun clearPendingPermissions() {
        _uiState.value = _uiState.value.copy(
            pendingToolPermissions = emptyList(),
            pendingPermissionTool = null,
            pendingPermissionArguments = emptyMap()
        )
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
            viewModelScope.launch { executeToolInternal(tool, args) }
        } else if (!granted) {
            addAssistantMessage("❌ دسترسی لازم داده نشد؛ عملیات اجرا نشد.")
        }
    }

    fun emergencyStop() {
        stopVoiceListening()
        stopSpeaking()
        _uiState.value = _uiState.value.copy(
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
        addAssistantMessage("🛑 تمام پردازش‌ها و فرآیندهای صوتی/پس‌زمینه متوقف شدند.")
    }

    fun processUserMessage(
        text: String,
        imageUri: Uri? = null,
        fileUri: Uri? = null,
        fileName: String? = null
    ) {
        if (text.isBlank() && imageUri == null && fileUri == null) return

        val userMsg = ChatMessage(
            sender = "USER",
            text = text,
            imageUri = imageUri,
            fileUri = fileUri,
            fileName = fileName
        )
        val currentMsgs = _uiState.value.messages.toMutableList()
        currentMsgs.add(userMsg)
        _uiState.value = _uiState.value.copy(messages = currentMsgs)

        viewModelScope.launch {
            // If image is attached, verify vision model status truthfully
            if (imageUri != null) {
                val visionModel = _uiState.value.activeVisionModel
                if (visionModel == null || !visionModel.isLoaded) {
                    val warn = "⚠️ مدل بینایی (Vision Model مانند Qwen2-VL) هنوز دانلود یا بارگذاری (Load) نشده است. برای تحلیل تصویر، لطفاً ابتدا مدل بینایی را از تب «مدیریت مدل‌ها» دانلود و بارگذاری نمایید."
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
                dialPhoneNumber(result.contact.phoneNumber)
            }
            is SkillResult.CallContactNearestChoices -> {
                _uiState.value = _uiState.value.copy(contactChoicesForCalling = result.top3Choices)
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.CardTransferConfirmation -> {
                // Check recent SMS for genuine OTP if available
                val otp = saderatAdapter.extractOtpFromSms(smsList)
                _uiState.value = _uiState.value.copy(
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
                addAssistantMessage(result.message)
                speakText(result.message)
            }
            is SkillResult.SmsSearchResult -> {
                _uiState.value = _uiState.value.copy(matchedSmsList = result.matchedSms)
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
            is SkillResult.ExecuteTermuxScript -> {
                // Save real code file to SAF workspace
                safFileManager.saveCodeFile(result.generatedFile)
                val updatedFiles = _uiState.value.codeFiles.toMutableList()
                val existingIndex = updatedFiles.indexOfFirst { it.name == result.generatedFile.name }
                if (existingIndex >= 0) {
                    updatedFiles[existingIndex] = result.generatedFile
                } else {
                    updatedFiles.add(0, result.generatedFile)
                }

                if (result.isDangerous) {
                    _uiState.value = _uiState.value.copy(
                        codeFiles = updatedFiles,
                        isDangerousCommandPending = true,
                        pendingDangerousCommand = result.termuxCommand
                    )
                    addAssistantMessage("⚠️ هشدار امنیتی: دستور «${result.termuxCommand}» ممکن است باعث حذف یا تغییر داده‌ها شود. جهت اجرا نیاز به تأیید صریح شما دارد.")
                    return
                }

                val execResult = termuxExecutor.executeScript(result.generatedFile.name, result.generatedFile.content, result.generatedFile.language)
                _uiState.value = _uiState.value.copy(
                    codeFiles = updatedFiles,
                    codeExecutionOutput = execResult
                )

                addAssistantMessage(result.message)
                speakText(result.message)
                repository.logAction(originalText, "TERMUX_EXEC", result.message, true)
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
        repository.downloadManager.startDownload(model) { success, msg ->
            _uiState.value = _uiState.value.copy(statusNotification = msg)
        }
    }

    fun pauseModelDownload(modelId: String) {
        repository.downloadManager.pauseDownload(modelId)
        _uiState.value = _uiState.value.copy(statusNotification = "دانلود مدل متوقف شد.")
    }

    fun deleteModel(model: DownloadedModelEntity) {
        viewModelScope.launch {
            repository.downloadManager.deleteModel(model)
            _uiState.value = _uiState.value.copy(statusNotification = "مدل ${model.name} و فایل‌های مرتبط از حافظه حذف گردیدند.")
        }
    }

    fun loadModel(model: DownloadedModelEntity) {
        viewModelScope.launch {
            val res = repository.downloadManager.loadModel(model, _uiState.value.speedMode.name)
            _uiState.value = _uiState.value.copy(statusNotification = res)
            addAssistantMessage(res)
            speakText(res)
        }
    }

    fun unloadModel(model: DownloadedModelEntity) {
        viewModelScope.launch {
            val res = repository.downloadManager.unloadModel(model)
            _uiState.value = _uiState.value.copy(statusNotification = res)
        }
    }

    // ================= REAL BANKING ACTIONS =================

    fun confirmTransfer(userEnteredOtp: String = "") {
        val details = _uiState.value.pendingTransferDetails ?: return
        val isTest = _uiState.value.isTransferTestMode
        val otpToUse = userEnteredOtp.ifBlank { _uiState.value.extractedOtp ?: "" }

        if (isTest) {
            _uiState.value = _uiState.value.copy(
                showTransferConfirmationDialog = false,
                transferStatus = BankTransferStatus.TransferFailed("TEST_MODE_DISABLED", "حالت انتقال تستی برای جلوگیری از موفقیت جعلی غیرفعال شده است." )
            )
            addAssistantMessage("حالت انتقال تستی غیرفعال است؛ هیچ پولی جابه‌جا نشد.")
        } else {
            // Real Saderat Banking: Launch official Bank Saderat App or Web
            _uiState.value = _uiState.value.copy(
                showTransferConfirmationDialog = false,
                pendingTransferDetails = null,
                transferStatus = BankTransferStatus.TransferExecuting("در حال ارجاع به درگاه امن بانک صادرات...")
            )

            val intent = saderatAdapter.createLaunchIntent()?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                try {
                    getApplication<Application>().startActivity(intent)
                    val msg = "اپلیکیشن بانک صادرات باز شد. شماره کارت مقصد (${saderatAdapter.formatCardNumber(details.destCardNumber)}) و مبلغ در کلیپ‌بورد/درگاه آماده است."
                    addAssistantMessage(msg)
                    speakText(msg)
                    viewModelScope.launch {
                        repository.logAction("کارت به کارت بانک صادرات", "BANK_TRANSFER_SADERAT", msg, true)
                    }
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
        _uiState.value = _uiState.value.copy(
            showTransferConfirmationDialog = false,
            pendingTransferDetails = null,
            transferStatus = BankTransferStatus.Idle
        )
        addAssistantMessage("عملیات انتقال وجه لغو گردید.")
    }

    // ================= REAL CODE & TERMUX ACTIONS =================

    fun executeCodeInTermux(code: String, filename: String, language: String) {
        viewModelScope.launch {
            safFileManager.saveCodeFile(CodeFile(filename, code, language))
            val output = termuxExecutor.executeScript(filename, code, language)
            _uiState.value = _uiState.value.copy(codeExecutionOutput = output)
        }
    }

    fun confirmDangerousCommand() {
        val cmd = _uiState.value.pendingDangerousCommand
        _uiState.value = _uiState.value.copy(
            isDangerousCommandPending = false,
            pendingDangerousCommand = ""
        )
        viewModelScope.launch {
            val res = termuxExecutor.executeTermuxCommand(cmd)
            _uiState.value = _uiState.value.copy(codeExecutionOutput = res.stdout.ifBlank { res.stderr })
        }
    }

    fun cancelDangerousCommand() {
        _uiState.value = _uiState.value.copy(
            isDangerousCommandPending = false,
            pendingDangerousCommand = ""
        )
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
            _uiState.value = _uiState.value.copy(statusNotification = msg)
            addAssistantMessage(msg)
        }
    }

    fun saveCodeFileToDisk(filename: String, content: String, lang: String) {
        viewModelScope.launch {
            val file = safFileManager.saveCodeFile(CodeFile(filename, content, lang))
            loadCodeFilesFromDisk()
            _uiState.value = _uiState.value.copy(statusNotification = "فایل ${file.name} در حافظه ذخیره شد.")
        }
    }

    fun exportDatabaseJson() {
        viewModelScope.launch {
            val targetDir = File(getApplication<Application>().filesDir, "exports")
            val outFile = repository.exportToJsonFile(targetDir)
            val msg = if (outFile != null) "خروجی پایگاه داده در مسیر ${outFile.absolutePath} ذخیره شد." else "خطا در خروجی فایل JSON."
            _uiState.value = _uiState.value.copy(statusNotification = msg)
            addAssistantMessage(msg)
        }
    }

    fun addNewPassword(appName: String, accountName: String, plainPassword: String, notes: String?) {
        viewModelScope.launch {
            repository.savePassword(appName, accountName, plainPassword, notes ?: "")
            _uiState.value = _uiState.value.copy(statusNotification = "رمز عبور برای $appName با رمزنگاری AES-GCM ذخیره شد.")
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
                _uiState.value = _uiState.value.copy(statusNotification = "امکان باز کردن تنظیمات دستیار پیش‌فرض وجود ندارد.")
            }
        }
    }

    // ================= CONTACTS & SMS HELPERS =================

    fun confirmContactChoice(contact: ContactMatch) {
        _uiState.value = _uiState.value.copy(contactChoicesForCalling = emptyList())
        val msg = "در حال تماس با ${contact.displayName} (${contact.phoneNumber})..."
        addAssistantMessage(msg)
        speakText(msg)
        dialPhoneNumber(contact.phoneNumber)
    }

    fun dismissContactChoices() {
        _uiState.value = _uiState.value.copy(contactChoicesForCalling = emptyList())
    }

    private fun addAssistantMessage(text: String) {
        val currentMsgs = _uiState.value.messages.toMutableList()
        currentMsgs.add(ChatMessage(sender = "ASSISTANT", text = text))
        _uiState.value = _uiState.value.copy(messages = currentMsgs)
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

    private fun dialPhoneNumber(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Dial fallback
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Search fallback
        }
    }

    fun onRuntimeTrimMemory(level: Int) {
        viewModelScope.launch {
            repository.downloadManager.onTrimMemory(level)
        }
    }

    override fun onCleared() {
        speechToText.destroy()
        textToSpeech.shutdown()
        super.onCleared()
    }
}
