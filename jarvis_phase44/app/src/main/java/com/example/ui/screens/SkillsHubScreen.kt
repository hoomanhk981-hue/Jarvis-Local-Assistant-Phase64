package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.SmsMessageItem
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderVibrant
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SkillsHubScreen(
    matchedSmsList: List<SmsMessageItem>,
    onTriggerCommand: (String) -> Unit
) {
    var appNameInput by remember { mutableStateOf("Call of Duty") }
    var contactInput by remember { mutableStateOf("علی") }
    var transferInput by remember { mutableStateOf("کارت به کارت ۵۰۰۰۰۰۰ ریال به ۶۰۳۷۹۹۷۵۱۲۳۴۵۶۷۸") }
    var smsQueryInput by remember { mutableStateOf("بلیط آبسار") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skill 1: App Launcher
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("skill_card_app_launcher"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Apps, contentDescription = "App Launcher", tint = NeonPurplePrimary, modifier = Modifier.size(22.dp))
                    Text(text = "قابلیت ۱: اجرا و باز کردن برنامه‌ها (با تشخیص غلط املایی)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                OutlinedTextField(
                    value = appNameInput,
                    onValueChange = { appNameInput = it },
                    label = { Text("نام برنامه (فارسی یا انگلیسی)", color = NeonPurpleLight) },
                    modifier = Modifier.fillMaxWidth().testTag("skill_app_launcher_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface,
                        focusedBorderColor = GlassBorderVibrant,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Button(
                    onClick = { onTriggerCommand("Open $appNameInput") },
                    modifier = Modifier.fillMaxWidth().testTag("skill_app_launcher_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("باز کردن برنامه", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Skill 2: Phone Dialing with Fuzzy Matches
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("skill_card_phone_dialer"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone Call", tint = GlowingCyan, modifier = Modifier.size(22.dp))
                    Text(text = "قابلیت ۲: تماس با مخاطب (۳ شماره نزدیک در صورت ابهام)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                OutlinedTextField(
                    value = contactInput,
                    onValueChange = { contactInput = it },
                    label = { Text("نام مخاطب یا شماره تلفن", color = NeonPurpleLight) },
                    modifier = Modifier.fillMaxWidth().testTag("skill_phone_dialer_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface,
                        focusedBorderColor = GlassBorderVibrant,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Button(
                    onClick = { onTriggerCommand("زنگ بزن به $contactInput") },
                    modifier = Modifier.fillMaxWidth().testTag("skill_phone_dialer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("برقراری تماس هوشمند", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Skill 3: Banking & Card Transfer
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("skill_card_banking"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = "Banking", tint = NeonPurpleLight, modifier = Modifier.size(22.dp))
                    Text(text = "قابلیت ۳: دستیار انتقال وجه و کارت به کارت با تأیید امنیتی", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                OutlinedTextField(
                    value = transferInput,
                    onValueChange = { transferInput = it },
                    label = { Text("دستور کارت به کارت و مبلغ", color = NeonPurpleLight) },
                    modifier = Modifier.fillMaxWidth().testTag("skill_banking_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface,
                        focusedBorderColor = GlassBorderVibrant,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Button(
                    onClick = { onTriggerCommand(transferInput) },
                    modifier = Modifier.fillMaxWidth().testTag("skill_banking_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("بررسی و نمایش فرم تأیید کارت به کارت", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Skill 4: SMS Ticket & Bank Reader
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("skill_card_sms_reader"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS Reader", tint = GlowingCyan, modifier = Modifier.size(22.dp))
                    Text(text = "قابلیت ۴: جستجو در SMSها (بلیط آبسار، بانک و رمز پویا)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                OutlinedTextField(
                    value = smsQueryInput,
                    onValueChange = { smsQueryInput = it },
                    label = { Text("عنوان یا کد مورد نظر جهت جستجو", color = NeonPurpleLight) },
                    modifier = Modifier.fillMaxWidth().testTag("skill_sms_reader_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface,
                        focusedBorderColor = GlassBorderVibrant,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Button(
                    onClick = { onTriggerCommand("جستجوی $smsQueryInput در پیامک") },
                    modifier = Modifier.fillMaxWidth().testTag("skill_sms_reader_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("جستجو در صندوق پیامک‌ها", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (matchedSmsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "نتایج یافت شده:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GlowingCyan)
                    matchedSmsList.forEach { sms ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = GlassBorder
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "${sms.sender} • [${sms.category}]", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonPurpleLight)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = sms.body, fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

