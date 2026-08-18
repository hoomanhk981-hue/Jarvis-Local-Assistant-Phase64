package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ImageSearch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.ElectricViolet
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
fun VisionAiScreen(
    selectedImageUri: Uri?,
    analysisResult: String?,
    isAnalyzing: Boolean,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onAnalyzeImageQuestion: (String) -> Unit
) {
    var questionText by remember { mutableStateOf("توضیح بده چه چیزهایی در این تصویر وجود داره؟") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        imageVector = Icons.Default.ImageSearch,
                        contentDescription = "Vision AI",
                        modifier = Modifier.size(24.dp),
                        tint = GlowingCyan
                    )
                }
                Column {
                    Text(
                        text = "مدل هوش مصنوعی تصویر و بینایی لوکال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "اختصاصاً جهت تحلیل عکس‌ها و پاسخ به سوالات تصویری",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Image Preview Canvas
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(20.dp),
            borderColor = GlassBorderVibrant
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.assistant_hero_1786526937578),
                        contentDescription = "Vision preview placeholder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x300A0714), Color(0xDC0A0714))
                            )
                        )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                ) {
                    GlassPill(
                        isSelected = true,
                        onClick = onPickGallery,
                        modifier = Modifier.testTag("pick_gallery_image_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("انتخاب عکس از گالری", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    GlassPill(
                        isSelected = false,
                        onClick = onTakePhoto,
                        modifier = Modifier.testTag("take_photo_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text("عکاسی با دوربین", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            label = { Text("سوال درباره تصویر", color = NeonPurpleLight) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vision_question_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedBorderColor = GlassBorderVibrant,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            maxLines = 3
        )

        Button(
            onClick = { onAnalyzeImageQuestion(questionText) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("analyze_vision_image_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Analyze", tint = Color.White)
            Spacer(modifier = Modifier.size(8.dp))
            Text("تحلیل و پاسخ صوتی/متنی به تصویر", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        if (analysisResult != null) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vision_analysis_result_card"),
                shape = RoundedCornerShape(18.dp),
                isElevated = true,
                borderColor = GlassBorderVibrant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "نتیجه تحلیل محلی:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GlowingCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = analysisResult ?: "", fontSize = 13.sp, color = TextPrimary)
                }
            }
        }
    }
}

