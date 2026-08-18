package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun LiveVoiceAssistantModal(
    isOpen: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    recognizedText: String,
    lastAssistantResponse: String,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val infiniteTransition = rememberInfiniteTransition(label = "live_equalizer")
    
    // Wave animation phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_orb"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0A16))
            .clickable(enabled = false) {}
            .testTag("live_voice_assistant_modal"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(GlowingCyan)
                    )
                    Text(
                        text = "دستیار صوتی زنده (Jarvis Live Voice)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x30FFFFFF))
                        .testTag("close_live_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Live",
                        tint = Color.White
                    )
                }
            }

            // Center Visualizer: Animated Glowing Blue Equalizer & Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Orb & Wave Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    // Outer radiant aura
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(if (isListening || isSpeaking) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        if (isSpeaking) GlowingCyan.copy(alpha = 0.45f) else Color(0xFF3B82F6).copy(alpha = 0.45f),
                                        NeonPurplePrimary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Core Glowing Ring
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF06B6D4),
                                        Color(0xFF3B82F6),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            )
                            .border(3.dp, GlowingCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    // 12-Bar Glowing Equalizer Waves
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = 18
                        val barWidth = 6.dp.toPx()
                        val spacing = (size.width - (barCount * barWidth)) / (barCount + 1)
                        val centerY = size.height * 0.82f

                        for (i in 0 until barCount) {
                            val x = spacing + i * (barWidth + spacing)
                            val factor = if (isListening || isSpeaking) {
                                (sin(phase + i * 0.55).toFloat() * 0.5f + 0.5f)
                            } else {
                                0.15f
                            }
                            val barHeight = 12.dp.toPx() + factor * 50.dp.toPx()
                            val top = centerY - barHeight / 2

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        GlowingCyan,
                                        Color(0xFF3B82F6),
                                        NeonPurplePrimary
                                    )
                                ),
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // Status Banner
                Text(
                    text = when {
                        isListening -> "🎙️ در حال گوش دادن به صدای شما..."
                        isSpeaking -> "🔊 در حال پاسخگویی صوتی..."
                        else -> "آماده دریافت دستور صوتی شما"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GlowingCyan,
                    textAlign = TextAlign.Center
                )

                // Real-time voice transcription or last response
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = GlowingCyan.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (recognizedText.isNotEmpty()) {
                            Text(
                                text = "گفتار شما: «$recognizedText»",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (lastAssistantResponse.isNotEmpty()) {
                            Text(
                                text = lastAssistantResponse,
                                color = Color(0xFFC7D2FE),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions & Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GlassPill(
                        isSelected = false,
                        onClick = { onQuickPrompt("کد پایتون بنویس و در ترموکس اجرا کن") },
                        modifier = Modifier.testTag("quick_termux_chip")
                    ) {
                        Text("⚡ کد در ترموکس", fontSize = 10.sp, color = GlowingCyan)
                    }
                    GlassPill(
                        isSelected = false,
                        onClick = { onQuickPrompt("اس ام اس های من رو خلاصه کن") },
                        modifier = Modifier.testTag("quick_sms_chip")
                    ) {
                        Text("📩 خلاصه پیامک‌ها", fontSize = 10.sp, color = GlowingCyan)
                    }
                    GlassPill(
                        isSelected = false,
                        onClick = { onQuickPrompt("واتساپ رو باز کن") },
                        modifier = Modifier.testTag("quick_open_app_chip")
                    ) {
                        Text("🚀 باز کردن برنامه", fontSize = 10.sp, color = GlowingCyan)
                    }
                }

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop speech TTS
                    IconButton(
                        onClick = onStopSpeaking,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .testTag("live_stop_tts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Stop TTS",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Mic Action Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .scale(if (isListening) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isListening) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                    else listOf(GlowingCyan, Color(0xFF3B82F6))
                                )
                            )
                            .clickable {
                                if (isListening) onStopVoice() else onStartVoice()
                            }
                            .testTag("live_mic_action_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic Action",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Done / Dismiss
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .testTag("live_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Exit",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
