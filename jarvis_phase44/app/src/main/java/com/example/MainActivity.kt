package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.ToolConfirmationDialog
import com.example.ui.components.ContactMatchDialog
import com.example.ui.components.LiveVoiceAssistantModal
import com.example.ui.screens.AboutAppScreen
import com.example.ui.screens.AssistantHomeScreen
import com.example.ui.screens.CodeTermuxScreen
import com.example.ui.screens.ModelsManagerScreen
import com.example.ui.screens.PersonalDatabaseScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SkillsHubScreen
import com.example.ui.screens.VisionAiScreen
import com.example.ui.theme.LightBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhiteBackground
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.AssistantViewModel
import com.example.vision.LocalVisionAnalyzer
import com.example.speech.VoicePackManager
import com.example.files.SafWorkspaceManager
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val visionAnalyzer by lazy { LocalVisionAnalyzer(this) }
    private val selectedVisionUriState = mutableStateOf<Uri?>(null)
    private val visionResultState = mutableStateOf<String?>(null)
    private val visionBusyState = mutableStateOf(false)
    private var pendingCameraUri: Uri? = null
    private var voicePackStatus by mutableStateOf(VoicePackManager(this).status())

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onRuntimePermissionsResult(result.values.all { it })
    }

    private val pickVisionImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedVisionUriState.value = uri
            visionResultState.value = null
            renderVisionState()
        }
    }

    private val takeVisionPhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingCameraUri != null) {
            selectedVisionUriState.value = pendingCameraUri
            visionResultState.value = null
            renderVisionState()
        }
    }

    private val viewModel: AssistantViewModel by viewModels()
    private val workspaceManager by lazy { SafWorkspaceManager(this) }
    private val voicePackManager by lazy { VoicePackManager(this) }

    private val pickWorkspaceFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            workspaceManager.saveTreeUri(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            lifecycleScope.launch { viewModel.notifyStatus("پوشه کاری با دسترسی دائمی انتخاب شد") }
        }
    }

    private fun renderVisionState() {
        // State holders above trigger Compose recomposition automatically.
    }

    private fun launchCameraCapture() {
        val file = File.createTempFile("jarvis_vision_", ".jpg", cacheDir)
        pendingCameraUri = androidx.core.content.FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
        takeVisionPhoto.launch(pendingCameraUri)
    }

    private fun analyzeVision(question: String) {
        val uri = selectedVisionUriState.value ?: return
        visionBusyState.value = true
        visionResultState.value = null
        lifecycleScope.launch {
            val result = visionAnalyzer.analyze(uri, question, viewModel.currentSpeedMode())
            visionBusyState.value = false
            visionResultState.value = result
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        viewModel.onRuntimeTrimMemory(level)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle external system assistant invocation
        if (intent?.action == Intent.ACTION_ASSIST || intent?.action == Intent.ACTION_VOICE_COMMAND) {
            viewModel.openLiveVoiceAssistant()
        }

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.pendingToolPermissions) {
                val permissions = state.pendingToolPermissions
                if (permissions.isNotEmpty()) {
                    runtimePermissionLauncher.launch(permissions.toTypedArray())
                }
            }
                val models by viewModel.allModels.collectAsStateWithLifecycle()
                val memories by viewModel.allMemories.collectAsStateWithLifecycle()
                val passwords by viewModel.allPasswords.collectAsStateWithLifecycle()
                val actionHistory by viewModel.actionHistory.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(state.statusNotification) {
                    state.statusNotification?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WhiteBackground)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = WhiteBackground,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(NeonPurplePrimary)
                                        )
                                        Text(
                                            text = when (state.selectedTab) {
                                                AppTab.HOME_ASSISTANT -> "دستیار صوتی و هوشمند Jarvis"
                                                AppTab.ABOUT_APP -> "درباره برنامه"
                                                AppTab.MODELS_MANAGER -> "مدیریت مدل‌های لوکال"
                                                AppTab.VISION_AI -> "مدل بینایی و تصویر"
                                                AppTab.SKILLS_HUB -> "قابلیت‌ها و دانلود هوشمند"
                                                AppTab.CODE_TERMUX -> "ویرایشگر کد و ترموکس"
                                                AppTab.PERSONAL_DATABASE -> "دیتابیس و حافظه شخصی"
                                                AppTab.SETTINGS -> "تنظیمات دستیار"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.testTag("app_top_bar_title")
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFFF3F4F6),
                                    titleContentColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                                    .border(1.dp, LightBorder, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color(0xFFF8FAFC),
                                contentColor = NeonPurpleLight,
                                modifier = Modifier
                                    .testTag("bottom_navigation_bar")
                                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                                    .border(1.dp, LightBorder, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                            ) {
                                val itemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = NeonPurplePrimary,
                                    indicatorColor = NeonPurplePrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                )

                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.HOME_ASSISTANT,
                                    onClick = { viewModel.selectTab(AppTab.HOME_ASSISTANT) },
                                    icon = { Icon(imageVector = Icons.Default.Assistant, contentDescription = "Home") },
                                    label = { Text("دستیار", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.HOME_ASSISTANT) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_assistant")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.ABOUT_APP,
                                    onClick = { viewModel.selectTab(AppTab.ABOUT_APP) },
                                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "About") },
                                    label = { Text("درباره", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.ABOUT_APP) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_about")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.MODELS_MANAGER,
                                    onClick = { viewModel.selectTab(AppTab.MODELS_MANAGER) },
                                    icon = { Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Models") },
                                    label = { Text("مدل‌ها", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.MODELS_MANAGER) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_models")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.VISION_AI,
                                    onClick = { viewModel.selectTab(AppTab.VISION_AI) },
                                    icon = { Icon(imageVector = Icons.Default.ImageSearch, contentDescription = "Vision") },
                                    label = { Text("بینایی", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.VISION_AI) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_vision")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.SKILLS_HUB,
                                    onClick = { viewModel.selectTab(AppTab.SKILLS_HUB) },
                                    icon = { Icon(imageVector = Icons.Default.Stars, contentDescription = "Skills") },
                                    label = { Text("قابلیت‌ها", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.SKILLS_HUB) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_skills")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.CODE_TERMUX,
                                    onClick = { viewModel.selectTab(AppTab.CODE_TERMUX) },
                                    icon = { Icon(imageVector = Icons.Default.Code, contentDescription = "Code") },
                                    label = { Text("کد/ترموکس", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.CODE_TERMUX) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_code")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.PERSONAL_DATABASE,
                                    onClick = { viewModel.selectTab(AppTab.PERSONAL_DATABASE) },
                                    icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Database") },
                                    label = { Text("دیتابیس", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.PERSONAL_DATABASE) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_db")
                                )
                                NavigationBarItem(
                                    selected = state.selectedTab == AppTab.SETTINGS,
                                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("تنظیمات", fontSize = 10.sp, fontWeight = if (state.selectedTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                                    colors = itemColors,
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (state.selectedTab) {
                                AppTab.HOME_ASSISTANT -> AssistantHomeScreen(
                                    state = state,
                                    onSendMessage = { text, imgUri, fileUri, fileName ->
                                        viewModel.processUserMessage(text, imgUri, fileUri, fileName)
                                    },
                                    onStartVoice = { viewModel.startVoiceListening() },
                                    onStopVoice = { viewModel.stopVoiceListening() },
                                    onOpenLiveVoice = { viewModel.openLiveVoiceAssistant() },
                                    onSpeakText = { text -> viewModel.speakText(text) },
                                    onSetSpeedMode = { mode -> viewModel.setSpeedMode(mode) },
                                    onClearSpeechText = { viewModel.clearSpeechRecognizedText() }
                                )
                                AppTab.ABOUT_APP -> AboutAppScreen()
                                AppTab.MODELS_MANAGER -> ModelsManagerScreen(
                                    models = models,
                                    onStartDownload = { model -> viewModel.startModelDownload(model) },
                                    onPauseDownload = { modelId -> viewModel.pauseModelDownload(modelId) },
                                    onDeleteModel = { model -> viewModel.deleteModel(model) },
                                    onLoadModel = { model -> viewModel.loadModel(model) },
                                    onUnloadModel = { model -> viewModel.unloadModel(model) }
                                )
                                AppTab.VISION_AI -> VisionAiScreen(
                                    selectedImageUri = selectedVisionUriState.value,
                                    analysisResult = visionResultState.value,
                                    isAnalyzing = visionBusyState.value,
                                    onPickGallery = { pickVisionImage.launch("image/*") },
                                    onTakePhoto = { launchCameraCapture() },
                                    onAnalyzeImageQuestion = { q -> analyzeVision(q) }
                                )
                                AppTab.SKILLS_HUB -> SkillsHubScreen(
                                    matchedSmsList = state.matchedSmsList,
                                    onTriggerCommand = { cmd -> viewModel.processUserMessage(cmd) }
                                )
                                AppTab.CODE_TERMUX -> CodeTermuxScreen(
                                    codeFiles = state.codeFiles,
                                    executionOutput = state.codeExecutionOutput,
                                    onExecuteCode = { code, filename, lang ->
                                        viewModel.executeCodeInTermux(code, filename, lang)
                                    },
                                    onAiGenerateCodeAndRunInTermux = { prompt ->
                                        viewModel.processUserMessage(prompt)
                                    },
                                    onCreateZipExport = { viewModel.createZipArchiveFromCode() },
                                    onChooseWorkspace = { pickWorkspaceFolder.launch(null) }
                                )
                                AppTab.PERSONAL_DATABASE -> PersonalDatabaseScreen(
                                    customFolderPath = state.customDatabaseFolderPath,
                                    memories = memories,
                                    passwords = passwords,
                                    onExportJson = { viewModel.exportDatabaseJson() },
                                    onAddPassword = { app, acc, pass, notes ->
                                        viewModel.addNewPassword(app, acc, pass, notes)
                                    }
                                )
                                AppTab.SETTINGS -> SettingsScreen(
                                    state = state,
                                    actionHistory = actionHistory,
                                    onToggleLanguage = { viewModel.toggleLanguage() },
                                    onOpenDefaultAssistantSettings = { viewModel.openDefaultAssistantSettings() },
                                    onInstallPersianTts = { voicePackManager.openTtsInstall("fa"); voicePackStatus = voicePackManager.status() },
                                    onInstallEnglishTts = { voicePackManager.openTtsInstall("en"); voicePackStatus = voicePackManager.status() },
                                    onInstallSpeechRecognition = { voicePackManager.openSpeechRecognitionSettings(); voicePackStatus = voicePackManager.status() },
                                    offlinePersianTts = voicePackStatus.offlinePersianTts,
                                    offlineEnglishTts = voicePackStatus.offlineEnglishTts,
                                    offlineSpeechAvailable = voicePackStatus.offlineSpeechLikelyAvailable
                                )
                            }

                            // Dialogs
                            if (state.contactChoicesForCalling.isNotEmpty()) {
                                ContactMatchDialog(
                                    top3Choices = state.contactChoicesForCalling,
                                    onSelectContact = { contact -> viewModel.confirmContactChoice(contact) },
                                    onDismiss = { viewModel.dismissContactChoices() }
                                )
                            }

                            if (state.pendingToolConfirmation != null) {
                                ToolConfirmationDialog(
                                    request = state.pendingToolConfirmation!!,
                                    onApprove = { viewModel.approvePendingTool() },
                                    onReject = { viewModel.rejectPendingTool() }
                                )
                            }

                            if (state.showTransferConfirmationDialog && state.pendingTransferDetails != null) {
                                ConfirmationDialog(
                                    details = state.pendingTransferDetails!!,
                                    isTestMode = state.isTransferTestMode,
                                    detectedOtp = state.extractedOtp,
                                    onConfirm = { otp -> viewModel.confirmTransfer(otp) },
                                    onCancel = { viewModel.cancelTransfer() }
                                )
                            }

                            // Fullscreen Live Voice Equalizer Assistant Modal (Gemini Live Mode)
                            if (state.isLiveVoiceAssistantOpen) {
                                LiveVoiceAssistantModal(
                                    isOpen = true,
                                    isListening = state.isListeningVoice,
                                    isSpeaking = state.isSpeakingVoice,
                                    recognizedText = state.speechRecognizedText,
                                    lastAssistantResponse = state.messages.lastOrNull { it.sender == "ASSISTANT" }?.text ?: "",
                                    onStartVoice = { viewModel.startVoiceListening() },
                                    onStopVoice = { viewModel.stopVoiceListening() },
                                    onStopSpeaking = { viewModel.stopSpeaking() },
                                    onQuickPrompt = { prompt -> viewModel.processUserMessage(prompt) },
                                    onDismiss = { viewModel.closeLiveVoiceAssistant() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_ASSIST || intent.action == Intent.ACTION_VOICE_COMMAND) {
            viewModel.openLiveVoiceAssistant()
        }
    }
}
