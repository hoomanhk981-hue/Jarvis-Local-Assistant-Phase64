package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CodeFile
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.DarkBackground
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
fun CodeTermuxScreen(
    codeFiles: List<CodeFile>,
    executionOutput: String,
    onExecuteCode: (String, String, String) -> Unit,
    onAiGenerateCodeAndRunInTermux: (String) -> Unit,
    onCreateZipExport: () -> Unit,
    onChooseWorkspace: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Editor, 1: Termux Commands
    var activeFileIndex by remember { mutableStateOf(0) }
    var currentCode by remember { mutableStateOf(codeFiles.firstOrNull()?.content ?: "") }
    var termuxCommandInput by remember { mutableStateOf("cd /sdcard && ls -la && python script.py") }
    var aiPromptInput by remember { mutableStateOf("یک اسکریپت پایتون بنویس و در ترموکس اجرا کن") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Button(
            onClick = onChooseWorkspace,
            modifier = Modifier.fillMaxWidth().testTag("choose_workspace_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface)
        ) {
            Icon(imageVector = Icons.Default.FolderZip, contentDescription = "Workspace", tint = GlowingCyan)
            Spacer(modifier = Modifier.size(6.dp))
            Text("انتخاب پوشه کاری واقعی (Storage Access Framework)", color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        // AI Autonomous Code Generator & Termux Runner Card
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("ai_code_generator_card"),
            shape = RoundedCornerShape(18.dp),
            isElevated = true,
            borderColor = GlowingCyan
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Auto Code", tint = GlowingCyan, modifier = Modifier.size(24.dp))
                    Text(text = "تولید خودکار کد توسط هوش مصنوعی و اجرای مستقیم در ترموکس", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }
                Text(
                    text = "فقط به هوش مصنوعی بگویید چه برنامه‌ای می‌خواهید؛ هوش مصنوعی کد را می‌نویسد، ذخیره می‌کند و دستور آن را مستقیماً به ترموکس ارسال و اجرا می‌نماید.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Quick Prompt Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassPill(
                        isSelected = false,
                        onClick = { aiPromptInput = "یک اسکریپت پایتون برای پردازش داده بنویس و در ترموکس اجرا کن" }
                    ) {
                        Text("🐍 پایتون", fontSize = 11.sp, color = GlowingCyan)
                    }
                    GlassPill(
                        isSelected = false,
                        onClick = { aiPromptInput = "دستور آپدیت و نصب پکیج‌های پایتون و clang در ترموکس را بزن" }
                    ) {
                        Text("⚡ آپدیت ترموکس", fontSize = 11.sp, color = GlowingCyan)
                    }
                    GlassPill(
                        isSelected = false,
                        onClick = { aiPromptInput = "یک برنامه C++ بنویس و با clang در ترموکس کامپایل کن" }
                    ) {
                        Text("⚙️ برنامه C++", fontSize = 11.sp, color = GlowingCyan)
                    }
                }

                OutlinedTextField(
                    value = aiPromptInput,
                    onValueChange = { aiPromptInput = it },
                    label = { Text("درخواست خود را به زبان فارسی یا انگلیسی بنویسید", color = NeonPurpleLight) },
                    modifier = Modifier.fillMaxWidth().testTag("ai_code_prompt_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        if (aiPromptInput.isNotBlank()) {
                            onAiGenerateCodeAndRunInTermux(aiPromptInput)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("ai_generate_and_run_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("تولید کد و ارسال مستقیم به ترموکس (Termux)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        // Tab Row for Editor vs Termux
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = GlassSurface,
            contentColor = NeonPurpleLight,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = NeonPurplePrimary,
                    height = 3.dp
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("code_termux_subtabs")
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "Editor", modifier = Modifier.size(18.dp), tint = if (selectedSubTab == 0) NeonPurplePrimary else TextSecondary)
                        Text("ویرایشگر کد (Code Editor)", color = if (selectedSubTab == 0) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_code_editor")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = "Termux", modifier = Modifier.size(18.dp), tint = if (selectedSubTab == 1) GlowingCyan else TextSecondary)
                        Text("دستورات ترموکس (Termux)", color = if (selectedSubTab == 1) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_termux_runner")
            )
        }

        if (selectedSubTab == 0) {
            // Code File selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                codeFiles.forEachIndexed { idx, file ->
                    GlassPill(
                        isSelected = activeFileIndex == idx,
                        onClick = {
                            activeFileIndex = idx
                            currentCode = file.content
                        },
                        modifier = Modifier.testTag("file_tab_$idx")
                    ) {
                        Text(file.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (activeFileIndex == idx) Color.White else TextSecondary)
                    }
                }
            }

            // Code Editor Box
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                borderColor = GlassBorderVibrant
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "// File: ${codeFiles.getOrNull(activeFileIndex)?.name ?: "script.py"}",
                        color = NeonPurpleLight,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = currentCode,
                        onValueChange = { currentCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .testTag("code_editor_text_area"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFC7D2FE),
                            unfocusedTextColor = Color(0xFFC7D2FE),
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = GlassBorderVibrant,
                            unfocusedBorderColor = GlassBorder
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Code Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val active = codeFiles.getOrNull(activeFileIndex)
                        onExecuteCode(
                            currentCode,
                            active?.name ?: "script.py",
                            active?.language ?: "python"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                    modifier = Modifier.weight(1f).testTag("run_code_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("اجرا و ارسال", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCreateZipExport,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                    modifier = Modifier.testTag("create_zip_export_button")
                ) {
                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = "ZIP", tint = GlowingCyan)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("ساخت ZIP پروژه", color = GlowingCyan, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Termux Command Interface
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                borderColor = GlassBorderVibrant
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "ارسال مستقیم کامند به محیط Termux:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    OutlinedTextField(
                        value = termuxCommandInput,
                        onValueChange = { termuxCommandInput = it },
                        label = { Text("دستورات شل / ترموکس", color = NeonPurpleLight) },
                        modifier = Modifier.fillMaxWidth().testTag("termux_command_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = GlassBorderVibrant,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = GlowingCyan,
                            unfocusedTextColor = GlowingCyan
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                    )
                    Button(
                        onClick = {
                            onExecuteCode(termuxCommandInput, "termux_cmd.sh", "bash")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("run_termux_command_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = "Run Command", tint = Color.White)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("اجرا در ترموکس (Run in Termux)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Terminal Output Screen
        if (executionOutput.isNotEmpty()) {
            Text(text = "خروجی کنسول و نتیجه اجرای کد:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GlowingCyan)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                borderColor = GlassBorderVibrant
            ) {
                Text(
                    text = executionOutput,
                    color = Color(0xFF00FF88),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(14.dp)
                        .testTag("terminal_output_console")
                )
            }
        }
    }
}

