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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.LightBorderVibrant
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AboutAppScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_app_hero_card"),
            shape = RoundedCornerShape(24.dp),
            isElevated = true,
            borderColor = LightBorderVibrant
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NeonPurplePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assistant,
                        contentDescription = "Jarvis Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "دستیار صوتی و هوش مصنوعی Jarvis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )

                Text(
                    text = "نسخه پیشرفته - اجرای ۱۰۰٪ محلی (Local) و آفلاین روی دستگاه",
                    fontSize = 13.sp,
                    color = NeonPurplePrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "برنامه Jarvis یک دستیار هوشمند، پرقدرت و مستقل است که بدون نیاز به سرورهای ابری یا کلید API خارج از دستگاه کار می‌کند. تمام پردازش‌ها، تشخیص گفتار، استدلال و مدیریت دیتابیس به‌صورت امن و کاملاً محلی انجام می‌پذیرد.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Text(
            text = "قابلیت‌ها و ویژگی‌های کلیدی برنامه:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Features List
        FeatureItem(
            icon = Icons.Default.Assistant,
            title = "تنظیم به عنوان دستیار پیش‌فرض سیستم (Default Android Assistant)",
            description = "امکان انتخاب Jarvis به عنوان دستیار صوتی اصلی گوشی در تنظیمات اندروید؛ تا بتوانید در تمام محیط سیستم‌عامل با نگه داشتن دکمه هوم، دستیار شخصی خود را احضار کنید."
        )

        FeatureItem(
            icon = Icons.Default.Mic,
            title = "مکالمه صوتی و تبدیل ویس به متن (Voice Speech & TTS)",
            description = "امکان گفتگو و صحبت کردن صوتی با دستیار؛ صدای شما به‌صورت آفلاین و لوکال تبدیل به متن شده و پاسخ‌ها نیز با موتور خوانش صوتی (TTS) برای شما پخش می‌شوند."
        )

        FeatureItem(
            icon = Icons.Default.Speed,
            title = "تنظیم سطح تفکر و سرعت (Speed / Thinking Level)",
            description = "با دکمه (^) در کنار کادر پیام، می‌توانید سطح تفکر مدل را بین Low (تند)، Medium (متوازن) و High (دقیق) تغییر دهید."
        )

        FeatureItem(
            icon = Icons.Default.AttachFile,
            title = "پشتیبانی از آپلود فایل و تصویر",
            description = "قابلیت ضمیمه کردن انواع فایل‌ها اعم از تصاویر، فایل‌های ZIP، اسناد و مدارک جهت پردازش و تحلیل توسط هوش مصنوعی."
        )

        FeatureItem(
            icon = Icons.Default.Psychology,
            title = "مدیریت مدل‌های لوکال (Local LLMs)",
            description = "دانلود و بارگذاری مدل‌های سبک و قدرتمند روی حافظه داخلی گوشی جهت پاسخ‌دهی هوشمند و کاملاً آفلاین."
        )

        FeatureItem(
            icon = Icons.Default.ImageSearch,
            title = "هوش بینایی و تحلیل تصویر (Vision AI)",
            description = "تشخیص محتوای تصاویر، استخراج متن از عکس‌ها و پاسخ به سوالات بر اساس عکس‌های آپلود شده."
        )

        FeatureItem(
            icon = Icons.Default.Code,
            title = "محیط کدنویسی و ترموکس (Code & Termux)",
            description = "ویرایش و اجرای لوکال کدهای Python، C++ و Bash به همراه قابلیت ساخت فایل خروجی ZIP از پروژه‌ها."
        )

        FeatureItem(
            icon = Icons.Default.Stars,
            title = "موتور دستورات سیستمی (Skills Engine)",
            description = "باز کردن برنامه‌های نصب‌شده، تماس تلفنی صوتی، جستجوی هوشمند در SMSها و تأیید کارت به کارت با امنیت بالا."
        )

        FeatureItem(
            icon = Icons.Default.Folder,
            title = "حافظه و دیتابیس شخصی امن",
            description = "ذخیره لوکال دانش کاربر، یادداشت‌ها و مدیریت پسوردها در دیتابیس داخلی با امکان خروجی گرفتن JSON."
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = LightBorderVibrant
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NeonPurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
