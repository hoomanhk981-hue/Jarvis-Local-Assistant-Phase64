package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.SpeedRating
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightBorderVibrant
import com.example.ui.theme.LightContainer
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhiteBackground
import com.example.ui.viewmodel.AssistantUiState

@Composable
fun AssistantHomeScreen(
    state: AssistantUiState,
    onSendMessage: (String, Uri?, Uri?, String?) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onOpenLiveVoice: () -> Unit,
    onSpeakText: (String) -> Unit,
    onSetSpeedMode: (SpeedRating) -> Unit,
    onClearSpeechText: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    // File picker launcher (*/* for all files including images, zip, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            var name = "فایل ضمیمه"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Fallback name
            }
            selectedFileName = name
        }
    }

    // Auto-fill speech recognized text into the input text field
    LaunchedEffect(state.speechRecognizedText) {
        if (state.speechRecognizedText.isNotEmpty()) {
            inputText = if (inputText.isBlank()) {
                state.speechRecognizedText
            } else {
                "$inputText ${state.speechRecognizedText}"
            }
            onClearSpeechText()
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Mic wave pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .testTag("chat_bubble_${msg.id}"),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        borderColor = if (isUser) LightBorderVibrant else LightBorder,
                        isElevated = isUser
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isUser) NeonPurplePrimary else NeonPurpleLight)
                                    )
                                    Text(
                                        text = if (isUser) "شما" else "دستیار هوشمند",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NeonPurplePrimary
                                    )
                                }
                                if (!isUser) {
                                    IconButton(
                                        onClick = { onSpeakText(msg.text) },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("speak_bubble_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Speak",
                                            tint = NeonPurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (msg.fileName != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEDE9FE))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Attachment",
                                        tint = NeonPurplePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = msg.fileName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.text,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected File Attachment Chip Indicator
        AnimatedVisibility(visible = selectedFileName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3E8FF))
                    .border(1.dp, LightBorderVibrant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "File Attached",
                        tint = NeonPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = selectedFileName ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(
                    onClick = {
                        selectedFileUri = null
                        selectedFileName = null
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove File",
                        tint = TextMuted
                    )
                }
            }
        }

        // Input Bar & Action Buttons (ChatGPT / Claude / Gemini Style)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = if (state.isListeningVoice) "در حال شنیدن گفتار شما..." else "پیام خود را بنویسید یا صحبت کنید...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // File Upload Attachment (+) Button
                        IconButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("upload_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Attachment",
                                tint = NeonPurplePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Speed / Thinking Level Selector Button (^)
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEDE9FE))
                                    .clickable { showSpeedMenu = !showSpeedMenu }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("speed_mode_popup_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Thinking Mode Menu",
                                    tint = NeonPurplePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (state.speedMode) {
                                        SpeedRating.LOW -> "Low"
                                        SpeedRating.MEDIUM -> "Med"
                                        SpeedRating.HIGH -> "High"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPurplePrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Low (تند - سریع ترین پاسخ)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                                    onClick = {
                                        onSetSpeedMode(SpeedRating.LOW)
                                        showSpeedMenu = false
                                    },
                                    modifier = Modifier.testTag("speed_option_low")
                                )
                                DropdownMenuItem(
                                    text = { Text("Medium (متوازن - استاندارد)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                                    onClick = {
                                        onSetSpeedMode(SpeedRating.MEDIUM)
                                        showSpeedMenu = false
                                    },
                                    modifier = Modifier.testTag("speed_option_medium")
                                )
                                DropdownMenuItem(
                                    text = { Text("High (دقیق - تفکر عميق)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                                    onClick = {
                                        onSetSpeedMode(SpeedRating.HIGH)
                                        showSpeedMenu = false
                                    },
                                    modifier = Modifier.testTag("speed_option_high")
                                )
                            }
                        }
                    }
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Blue Equalizer Button (Gemini Live Mode)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(GlowingCyan, Color(0xFF3B82F6))
                                    )
                                )
                                .clickable { onOpenLiveVoice() }
                                .testTag("blue_equalizer_live_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Live Voice Equalizer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Speech-to-text Mic Button (speech transcribes to text box locally)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .scale(if (state.isListeningVoice) pulseScale else 1.0f)
                                .clip(CircleShape)
                                .background(
                                    if (state.isListeningVoice) Color(0xFFEF4444) else Color(0xFFEDE9FE)
                                )
                                .clickable {
                                    if (state.isListeningVoice) onStopVoice() else onStartVoice()
                                }
                                .testTag("voice_mic_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (state.isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Mic",
                                tint = if (state.isListeningVoice) Color.White else NeonPurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || selectedFileUri != null) {
                                    onSendMessage(inputText, null, selectedFileUri, selectedFileName)
                                    inputText = ""
                                    selectedFileUri = null
                                    selectedFileName = null
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(NeonPurplePrimary, CircleShape)
                                .testTag("send_chat_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_input_text_field"),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LightContainer,
                    unfocusedContainerColor = LightContainer,
                    focusedBorderColor = LightBorderVibrant,
                    unfocusedBorderColor = LightBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4
            )
        }
    }
}
