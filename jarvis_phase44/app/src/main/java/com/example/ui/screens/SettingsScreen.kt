package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.example.data.local.entities.SpeedRating
import com.example.ui.theme.LightBorder
import com.example.ui.theme.LightContainer
import com.example.ui.theme.LightContainerElevated
import com.example.ui.theme.NeonPurpleDark
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhiteBackground
import com.example.ui.viewmodel.AssistantUiState

@Composable
fun SettingsScreen(
    state: AssistantUiState,
    actionHistory: List<ActionHistoryEntity> = emptyList(),
    onToggleLanguage: () -> Unit,
    onOpenDefaultAssistantSettings: () -> Unit = {},
    onStartVoiceSetup: () -> Unit = {},
    onEnableDatabase: () -> Unit = {},
    onExportDatabase: () -> Unit = {},
    onSetSpeedMode: (SpeedRating) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonPurplePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "تنظیمات دستیار هوشمند Jarvis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                Text(
                    text = "پیکربندی قابلیت‌های صوتی، امنیتی و هوش مصنوعی",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        // Section 1: 1-Click Voice & AI Setup
        SettingsCard(
            title = "🚀 راه‌اندازی ۱-کلیک صوت و هوش مصنوعی (Voice Setup)",
            description = "تست، دانلود و پیکربندی خودکار موتور تبدیل متن به گفتار (TTS فارسی و انگلیسی) و تشخیص گفتار"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartVoiceSetup,
                    enabled = !state.isVoiceSetupInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings_start_voice_setup_button")
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (state.voiceSetupProgress == 100) "✅ سیستم صوتی آماده است (تست مجدد)" else "⚡ راه‌اندازی و فعال‌سازی خودکار صوت",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.isVoiceSetupInProgress || state.voiceSetupProgress > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { state.voiceSetupProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonPurplePrimary,
                            trackColor = Color(0xFFE5E7EB)
                        )
                        Text(
                            text = state.voiceSetupStatusText,
                            fontSize = 11.5.sp,
                            color = if (state.voiceSetupProgress == 100) Color(0xFF16A34A) else NeonPurplePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section 2: Default Android AI Assistant (Power Button trigger)
        SettingsCard(
            title = "🤖 دستیار پیش‌فرض گوشی و دکمه پاور (Power Button)",
            description = "با انتخاب Jarvis به عنوان دستیار پیش‌فرض، با نگه داشتن دکمه پاور یا هوم گوشی، دستیار بلافاصله فعال می‌شود."
        ) {
            Button(
                onClick = onOpenDefaultAssistantSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("settings_open_assistant_settings_button")
            ) {
                Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("تنظیم به عنوان دستیار پیش‌فرض اندروید", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Section 3: Personal Database & Memory Vault
        SettingsCard(
            title = "💾 پایگاه داده امن و حافظه شخصی (Database & Vault)",
            description = "ذخیره محلی و رمزنگاری‌شده رمزها و اطلاعات شخصی با استاندارد نظامی AES"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضعیت دیتابیس محلی:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.isPersonalDatabaseEnabled) "✅ فعال و امن" else "⚠️ غیرفعال",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isPersonalDatabaseEnabled) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEnableDatabase,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("فعال‌سازی دیتابیس", fontSize = 11.sp, color = NeonPurplePrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExportDatabase,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("خروجی JSON", fontSize = 11.sp, color = NeonPurplePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 4: Termux Environment
        SettingsCard(
            title = "⚡ محیط ترموکس و اجرای کد (Termux Integration)",
            description = "اجرای دستورات پایتون، C++ و لینوکس از طریق سرویس RunCommand"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (state.isTermuxInstalled) Color(0xFF16A34A) else Color(0xFFEAB308),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (state.isTermuxInstalled) "ترموکس روی دستگاه نصب است" else "ترموکس یافت نشد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Section 5: Speed Mode & Language
        SettingsCard(
            title = "⚙️ حالت سرعت پردازش و زبان",
            description = "تنظیم میزان دقت و سرعت استنتاج مدل‌های محلی و تغییر زبان پیش‌فرض"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "زبان پاسخگویی دستیار (فارسی / English):", fontSize = 12.sp, color = TextPrimary)
                    Switch(
                        checked = state.isPersianLanguage,
                        onCheckedChange = { onToggleLanguage() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurplePrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeedRating.entries.forEach { mode ->
                        val isSelected = state.speedMode == mode
                        Button(
                            onClick = { onSetSpeedMode(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonPurplePrimary else LightContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (mode) {
                                    SpeedRating.LOW -> "سریع"
                                    SpeedRating.MEDIUM -> "متعادل"
                                    SpeedRating.HIGH -> "دقیق"
                                },
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFC)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }

            content()
        }
    }
}
