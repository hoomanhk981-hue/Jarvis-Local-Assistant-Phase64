package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ActionHistoryEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderVibrant
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantUiState

@Composable
fun SettingsScreen(
    state: AssistantUiState,
    actionHistory: List<ActionHistoryEntity> = emptyList(),
    onToggleLanguage: () -> Unit,
    onOpenDefaultAssistantSettings: () -> Unit = {},
    onInstallPersianTts: () -> Unit = {},
    onInstallEnglishTts: () -> Unit = {},
    onInstallSpeechRecognition: () -> Unit = {},
    offlinePersianTts: Boolean = false,
    offlineEnglishTts: Boolean = false,
    offlineSpeechAvailable: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Assistant Default Setup Card
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("setting_assistant_card"),
            shape = RoundedCornerShape(20.dp),
            isElevated = true,
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.Assistant, contentDescription = "Assistant", modifier = Modifier.size(28.dp), tint = GlowingCyan)
                    Text(text = "تنظیم Jarvis به عنوان دستیار پیش‌فرض سیستم", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                }
                Text(
                    text = "با انتخاب Jarvis به‌عنوان دستیار پیش‌فرض، با فشردن طولانی دکمه پاور، هوم یا کلید صوتی در هر جای اندروید، دستیار بلافاصله باز می‌شود.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Button(
                    onClick = { onOpenDefaultAssistantSettings() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                    modifier = Modifier.fillMaxWidth().testTag("open_assistant_settings_button")
                ) {
                    Text("تنظیم در بخش Default Apps اندروید", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Offline Voice Packs
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("setting_voice_packs_card"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Offline Voice", tint = GlowingCyan)
                    Column {
                        Text("بسته‌های صوتی آفلاین", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("برای کارکرد Voice بدون API و بدون اینترنت", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Text(
                    text = if (offlineSpeechAvailable) "تشخیص گفتار on-device روی این دستگاه در دسترس است." else "تشخیص گفتار آفلاین در دسترس تشخیص داده نشد.",
                    fontSize = 11.sp, color = if (offlineSpeechAvailable) GlowingCyan else TextSecondary
                )
                Button(onClick = onInstallSpeechRecognition, modifier = Modifier.fillMaxWidth().testTag("install_offline_speech_button")) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("مدیریت / دانلود زبان‌های Speech-to-Text")
                }

                Text(
                    text = if (offlinePersianTts) "✓ صدای فارسی آفلاین نصب است" else "صدای فارسی آفلاین نصب نیست",
                    fontSize = 11.sp, color = if (offlinePersianTts) GlowingCyan else TextSecondary
                )
                Button(onClick = onInstallPersianTts, modifier = Modifier.fillMaxWidth().testTag("install_persian_tts_button")) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("دانلود / نصب TTS فارسی")
                }

                Text(
                    text = if (offlineEnglishTts) "✓ صدای انگلیسی آفلاین نصب است" else "صدای انگلیسی آفلاین نصب نیست",
                    fontSize = 11.sp, color = if (offlineEnglishTts) GlowingCyan else TextSecondary
                )
                Button(onClick = onInstallEnglishTts, modifier = Modifier.fillMaxWidth().testTag("install_english_tts_button")) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("دانلود / نصب TTS انگلیسی")
                }
            }
        }

        // Termux Integration Status Card
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("setting_termux_card"),
            shape = RoundedCornerShape(18.dp),
            borderColor = if (state.isTermuxInstalled) GlowingCyan else GlassBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Termux",
                        tint = if (state.isTermuxInstalled) GlowingCyan else Color(0xFFFFB74D)
                    )
                    Column {
                        Text(text = "وضعیت محیط ترموکس (Termux)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(
                            text = if (state.isTermuxInstalled) "✅ نصب شده و آماده اجرای دستورات" else "⚠️ نصب نیست (اجرای کدها به ذخیره فایل محدود می‌شود)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Language toggle
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("setting_language_card"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Language", tint = NeonPurpleLight)
                    Column {
                        Text(text = "زبان رابط و پردازش گفتار (Language)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(
                            text = if (state.isPersianLanguage) "فارسی (Persian - fa-IR)" else "English (en-US)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = state.isPersianLanguage,
                    onCheckedChange = { onToggleLanguage() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurplePrimary,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0x30FFFFFF)
                    ),
                    modifier = Modifier.testTag("toggle_language_switch")
                )
            }
        }

        // Action History Audit Log
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("setting_audit_log_card"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorder
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "Audit Log", tint = NeonPurpleLight)
                    Text(text = "تاریخچه دستورات اجرا شده (Action Audit Log)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                Text(
                    text = "اطلاعات حساس (رمزهای عبور، OTP و کارت‌ها) جهت امنیت قبل از ثبت به صورت خودکار ماسک می‌شوند.",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                if (actionHistory.isEmpty()) {
                    Text("هنوز دستوری ثبت نشده است.", fontSize = 12.sp, color = TextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        actionHistory.take(5).forEach { action ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (action.isSuccess) GlowingCyan else Color(0xFFFF5252))
                                    )
                                    Text(action.commandText, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                                }
                                Text(action.skillExecuted, fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
