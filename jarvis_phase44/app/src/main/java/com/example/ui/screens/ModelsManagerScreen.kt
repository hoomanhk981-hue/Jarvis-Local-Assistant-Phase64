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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.ModelType
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderVibrant
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ModelsManagerScreen(
    models: List<DownloadedModelEntity>,
    onStartDownload: (DownloadedModelEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onDeleteModel: (DownloadedModelEntity) -> Unit,
    onLoadModel: (DownloadedModelEntity) -> Unit,
    onUnloadModel: (DownloadedModelEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            isElevated = true,
            borderColor = GlassBorderVibrant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ElectricViolet, Color(0x30130E26))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Memory",
                        modifier = Modifier.size(24.dp),
                        tint = NeonPurpleLight
                    )
                }
                Column {
                    Text(
                        text = "مدیریت مدل‌های هوش مصنوعی لوکال (GGUF Models)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "دانلود واقعی با قابلیت Pause/Resume • لود مستقیم در RAM بدون اینترنت",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Storage",
                    tint = NeonPurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "مدل‌های ثبت شده (متنی و ویژن):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = "${models.count { it.isDownloaded }} مدل دانلود شده",
                fontSize = 11.sp,
                color = GlowingCyan,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(models) { model ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("model_card_${model.id}"),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = if (model.isLoaded) GlowingCyan else if (model.isDownloaded) GlassBorderVibrant else GlassBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = model.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    GlassPill(
                                        isSelected = model.modelType == ModelType.VISION
                                    ) {
                                        Text(
                                            text = if (model.modelType == ModelType.VISION) "بینایی (Vision)" else "متنی (Text)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (model.modelType == ModelType.VISION) GlowingCyan else NeonPurpleLight
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = model.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("حداقل RAM: ${model.ramRequiredMb} MB", fontSize = 11.sp, color = TextMuted)
                                    Text("لایسنس: ${model.license}", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                            Text(
                                text = model.sizeFormatted,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = NeonPurpleLight
                            )
                        }

                        // Progress Bar during download
                        if (model.isDownloading || (!model.isDownloaded && model.downloadProgressPercentage > 0)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { model.downloadProgressPercentage / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonPurplePrimary,
                                    trackColor = GlassBorder
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "پیشرفت: ${model.downloadProgressPercentage}%",
                                        fontSize = 11.sp,
                                        color = GlowingCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = model.downloadSpeedText,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (model.isDownloaded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (model.isLoaded) {
                                        Button(
                                            onClick = { onUnloadModel(model) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332040)),
                                            modifier = Modifier.testTag("unload_button_${model.id}")
                                        ) {
                                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Unload", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("خروج از RAM", fontSize = 11.sp, color = Color(0xFFFF8A80))
                                        }
                                    } else {
                                        Button(
                                            onClick = { onLoadModel(model) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GlowingCyan.copy(alpha = 0.2f)),
                                            modifier = Modifier.testTag("load_button_${model.id}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Load", tint = GlowingCyan, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("بارگذاری در RAM", fontSize = 11.sp, color = GlowingCyan, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteModel(model) },
                                        modifier = Modifier.testTag("delete_model_button_${model.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (model.isDownloading) {
                                        OutlinedButton(
                                            onClick = { onPauseDownload(model.id) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.PauseCircle, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("توقف دانلود (Pause)", fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onStartDownload(model) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                                            modifier = Modifier.testTag("download_model_button_${model.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = "Download",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(
                                                if (model.downloadProgressPercentage > 0) "ادامه دانلود (Resume)" else "دانلود مدل واقعی GGUF",
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
