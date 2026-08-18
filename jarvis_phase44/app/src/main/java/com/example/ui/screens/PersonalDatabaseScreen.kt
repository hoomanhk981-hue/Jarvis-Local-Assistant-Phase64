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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SaveAlt
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
import com.example.data.local.entities.SavedPasswordEntity
import com.example.data.local.entities.UserMemoryEntity
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
fun PersonalDatabaseScreen(
    customFolderPath: String,
    memories: List<UserMemoryEntity>,
    passwords: List<SavedPasswordEntity>,
    onExportJson: () -> Unit,
    onAddPassword: (String, String, String, String) -> Unit
) {
    var newAppName by remember { mutableStateOf("") }
    var newAccount by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Folder & Storage Database Card
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("database_folder_card"),
            shape = RoundedCornerShape(20.dp),
            isElevated = true,
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = "Folder", tint = GlowingCyan, modifier = Modifier.size(22.dp))
                    Text(text = "پوشه دیتابیس شخصی کاربر (JSON/TXT)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                }
                Text(text = "مسیر ذخیره فایل‌ها: $customFolderPath", fontSize = 12.sp, color = TextSecondary)
                Button(
                    onClick = onExportJson,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                    modifier = Modifier.fillMaxWidth().testTag("export_json_button")
                ) {
                    Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Export", tint = Color.White)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("خروجی به فایل JSON و همگام‌سازی", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add App Password Form
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("add_password_card"),
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorderVibrant
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = "Password", tint = NeonPurpleLight, modifier = Modifier.size(22.dp))
                    Text(text = "ثبت پسورد برنامه‌ها (جهت لاگین خودکار دستیار)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text("نام برنامه (مثلا همراه بانک / تلگرام)", color = NeonPurpleLight) },
                    modifier = Modifier.fillMaxWidth().testTag("pass_app_name_input"),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newAccount,
                        onValueChange = { newAccount = it },
                        label = { Text("نام کاربری", color = NeonPurpleLight) },
                        modifier = Modifier.weight(1f).testTag("pass_account_input"),
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
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("رمز عبور", color = NeonPurpleLight) },
                        modifier = Modifier.weight(1f).testTag("pass_secret_input"),
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
                }
                Button(
                    onClick = {
                        if (newAppName.isNotBlank() && newPass.isNotBlank()) {
                            onAddPassword(newAppName, newAccount, newPass, "ذخیره‌شده توسط کاربر")
                            newAppName = ""
                            newAccount = ""
                            newPass = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_password_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("ذخیره امن رمز عبور", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Learned Memories & Knowledge Base List
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = "Memories", tint = GlowingCyan, modifier = Modifier.size(20.dp))
            Text(text = "دانش و حافظه اختصاصی مدل هوش مصنوعی:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(memories) { mem ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("memory_item_${mem.id}"),
                    shape = RoundedCornerShape(14.dp),
                    borderColor = GlassBorder
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = mem.key, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonPurpleLight)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = mem.value, fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

