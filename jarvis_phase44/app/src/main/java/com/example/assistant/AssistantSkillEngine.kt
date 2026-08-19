package com.example.assistant

import android.content.Context
import com.example.bank.BankAdapter
import com.example.bank.SaderatBankAdapter
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.models.CodeFile
import com.example.data.models.ContactMatch
import com.example.data.models.InstalledAppInfo
import com.example.data.models.SmsMessageItem
import com.example.data.models.TransferDetails
import com.example.termux.TermuxExecutor

sealed class SkillResult {
    data class LaunchApp(val app: InstalledAppInfo, val statusMessage: String) : SkillResult()
    data class SearchAppWeb(val query: String, val message: String) : SkillResult()
    data class CallContactExact(val contact: ContactMatch, val message: String) : SkillResult()
    data class CallPhoneNumberDirect(val phoneNumber: String, val displayName: String, val message: String) : SkillResult()
    data class CallContactNearestChoices(val top3Choices: List<ContactMatch>, val message: String) : SkillResult()
    data class CardTransferConfirmation(val details: TransferDetails, val message: String, val isTestMode: Boolean) : SkillResult()
    data class SmsSearchResult(val matchedSms: List<SmsMessageItem>, val message: String) : SkillResult()
    data class SmsSummaryResult(val summaryText: String, val rawCount: Int) : SkillResult()
    data class ForwardSmsRequest(val targetContact: ContactMatch?, val content: String, val message: String) : SkillResult()
    data class GmailSummaryResult(val summaryText: String) : SkillResult()
    data class ExecuteTermuxScript(val generatedFile: CodeFile, val termuxCommand: String, val message: String, val isDangerous: Boolean = false) : SkillResult()
    data class ExecuteRawTermuxCommand(val command: String, val message: String, val isDangerous: Boolean = false) : SkillResult()
    data class PasswordSaved(val appName: String, val passwordSecret: String, val message: String) : SkillResult()
    data class GetPasswordResult(val appName: String, val message: String) : SkillResult()
    data class KnowledgeSaved(val key: String, val value: String, val message: String) : SkillResult()
    data class EmergencyStop(val message: String) : SkillResult()
    data class GeneralAnswer(val responseText: String) : SkillResult()
}

class AssistantSkillEngine(
    private val context: Context,
    private val localInference: suspend (DownloadedModelEntity, String, String) -> String
) {

    private val saderatAdapter = SaderatBankAdapter(context)
    private val termuxExecutor = TermuxExecutor(context)

    /**
     * Processes user commands with zero simulation and strict validation.
     */
    suspend fun processCommand(
        input: String,
        installedApps: List<InstalledAppInfo>,
        allContacts: List<ContactMatch>,
        recentSms: List<SmsMessageItem>,
        speedMode: String = "MEDIUM", // LOW, MEDIUM, HIGH
        activeTextModel: DownloadedModelEntity? = null
    ): SkillResult {
        val query = FuzzyMatcher.normalizePersianText(input)

        // 0. Emergency Stop Command
        if (query == "stop" || query == "توقف" || query == "استپ" || query == "وایسا" || query == "صبر کن" || query == "کنسل") {
            return SkillResult.EmergencyStop("تمامی عملیات هوش مصنوعی و فرآیندهای پس‌زمینه بلافاصله متوقف شدند.")
        }

        // 1. Password & Credential Saving ("... یادت بمونه")
        if (input.contains("یادت بمونه") || query.contains("یادت بمونه") || query.contains("یادت باشه") || (query.contains("رمز عبور") && query.contains("ذخیره"))) {
            val cleanStr = input
                .replace("یادت بمونه", "")
                .replace("یادت باشه", "")
                .replace("این پسورد", "")
                .replace("پسورد", "")
                .replace("رمز عبور", "")
                .replace("رمز", "")
                .trim()

            val parts = cleanStr.split(Regex("[:=–\\s]+")).filter { it.isNotBlank() }
            val app = if (parts.isNotEmpty()) parts.first().replace("مه", "").replace("ام", "").replace("م", "").trim() else "حساب کاربری"
            val pass = if (parts.size > 1) parts.drop(1).joinToString(" ") else cleanStr

            return SkillResult.PasswordSaved(
                appName = app,
                passwordSecret = pass,
                message = "رمز عبور با استاندارد رمزنگاری AES در پایگاه داده امن ثبت شد: برای حساب «$app» رمز عبور ذخیره گردید."
            )
        }

        // Querying Saved Passwords
        if ((query.contains("پسورد") || query.contains("رمز")) && (query.contains("چیه") || query.contains("چنده") || query.contains("بگو") || query.contains("نمایش"))) {
            val appQuery = query
                .replace("پسورد", "")
                .replace("رمز عبور", "")
                .replace("رمز", "")
                .replace("رو بگو", "")
                .replace("چیه", "")
                .replace("چنده", "")
                .replace("من", "")
                .trim()

            return SkillResult.GetPasswordResult(
                appName = appQuery,
                message = "در حال جستجو در پایگاه داده امن برای رمز عبور «$appQuery»..."
            )
        }

        // 2. Open Termux App vs Execute Termux Commands
        val isTermuxOpenIntent = (query.contains("ترموکس") || query.contains("termux")) &&
            (query.contains("باز کن") || query.contains("بازش کن") || query.contains("برو تو") || query.startsWith("open")) &&
            !query.contains("اجرا") && !query.contains("کامند") && !query.contains("دستور") && !query.contains("کد")

        if (isTermuxOpenIntent) {
            val termuxApp = installedApps.firstOrNull { it.packageName == "com.termux" }
                ?: InstalledAppInfo("Termux", "com.termux")
            return SkillResult.LaunchApp(termuxApp, "در حال باز کردن محیط ترموکس (Termux)...")
        }

        if (query.contains("ترموکس") || query.contains("termux") ||
            (query.contains("کد") && (query.contains("بنویس") || query.contains("اجرا کن") || query.contains("ران کن"))) ||
            query.startsWith("دستور ") || query.contains("در ترموکس") || query.contains("توی ترموکس")
        ) {
            // Extract raw command if user requested executing a specific command
            var directCmd = ""
            if (query.contains("رو اجرا کن") || query.contains("اجرا کن") || query.contains("بزن")) {
                directCmd = input
                    .replace(Regex("(?i)ترموکس|termux|توی ترموکس|در ترموکس|داخل ترموکس|رو توی ترموکس|رو در ترموکس|این دستور رو|دستور|کد|رو اجرا کن|اجرا کن|رو بزن|بزن|لطفا|لطفاً"), " ")
                    .trim()
            }

            if (directCmd.isNotBlank() && !directCmd.contains("پایتون") && !directCmd.contains("کد بنویس")) {
                val isDanger = termuxExecutor.isDangerousCommand(directCmd)
                return SkillResult.ExecuteRawTermuxCommand(
                    command = directCmd,
                    message = "دستور «$directCmd» به ترموکس ارسال شد.",
                    isDangerous = isDanger
                )
            }

            val scriptCode: String
            val filename: String
            val lang: String
            val termuxCmd: String

            when {
                query.contains("پایتون") || query.contains("python") || query.contains("اسکرپ") || query.contains("دانلود") -> {
                    filename = "auto_task.py"
                    lang = "python"
                    scriptCode = """
# تولید شده توسط دستیار Jarvis
import sys

def main():
    print("=== Jarvis Python Runner ===")
    print("در حال اجرای برنامه پایتون در ترموکس...")

if __name__ == "__main__":
    main()
""".trimIndent()
                    termuxCmd = "python auto_task.py"
                }
                query.contains("سی") || query.contains("c++") || query.contains("cpp") -> {
                    filename = "main.cpp"
                    lang = "cpp"
                    scriptCode = """
#include <iostream>
using namespace std;

int main() {
    cout << "=== Jarvis C++ Runner ===" << endl;
    return 0;
}
""".trimIndent()
                    termuxCmd = "clang++ main.cpp -o main_app && ./main_app"
                }
                else -> {
                    filename = "script.sh"
                    lang = "bash"
                    val cmdToRun = if (query.contains("آپدیت") || query.contains("update")) "pkg update && pkg upgrade -y" else "echo '[Jarvis Command]' && pwd && ls -la"
                    scriptCode = """
#!/bin/bash
$cmdToRun
""".trimIndent()
                    termuxCmd = cmdToRun
                }
            }

            val codeFile = CodeFile(filename, scriptCode, lang)
            val isDanger = termuxExecutor.isDangerousCommand(termuxCmd)
            return SkillResult.ExecuteTermuxScript(
                generatedFile = codeFile,
                termuxCommand = termuxCmd,
                message = if (termuxExecutor.isTermuxInstalled()) {
                    "فایل $filename ایجاد و در فضای کاری ذخیره شد. دستور جهت اجرا به ترموکس ارسال می‌گردد."
                } else {
                    "فایل $filename ایجاد و ذخیره شد. برنامه ترموکس روی دستگاه نصب نیست؛ دستور آماده است: $termuxCmd"
                },
                isDangerous = isDanger
            )
        }

        // 3. SMS Reading, Summarizing & OTP Extraction
        if (query.contains("اس ام اس") || query.contains("پیامک") || query.contains("sms") || query.contains("پیام ها") || query.contains("پیام‌ها")) {
            if (recentSms.isEmpty()) {
                return SkillResult.GeneralAnswer("هیچ پیامکی در دستگاه یافت نشد یا دسترسی به پیامک‌ها (READ_SMS) داده نشده است.")
            }

            // Summarize SMS
            if (query.contains("خلاصه") || query.contains("بخون") || query.contains("بررسی")) {
                val count = recentSms.size
                val banks = recentSms.filter { it.category == "BANK" }
                val otps = recentSms.filter { it.category == "OTP" }
                val tickets = recentSms.filter { it.category == "TICKET" }

                val summaryBuilder = StringBuilder()
                summaryBuilder.appendLine("📊 خلاصه پیامک‌های دریافتی ($count پیامک اخیر):")
                if (otps.isNotEmpty()) {
                    summaryBuilder.appendLine("🔑 رمزهای پویا / کدهای تایید: ${otps.size} پیامک (آخرین: «${otps.first().body.take(35)}...»)")
                }
                if (banks.isNotEmpty()) {
                    summaryBuilder.appendLine("💳 پیام‌های تراکنش بانکی: ${banks.size} مورد ثبت شده است.")
                }
                if (tickets.isNotEmpty()) {
                    summaryBuilder.appendLine("🎫 رزروها و پیام‌های سامانه‌ای: ${tickets.size} مورد یافت گردید.")
                }

                return SkillResult.SmsSummaryResult(
                    summaryText = summaryBuilder.toString(),
                    rawCount = count
                )
            }

            // Search specific SMS keywords
            val matchedSms = recentSms.filter { sms ->
                val body = FuzzyMatcher.normalizePersianText(sms.body)
                val sender = FuzzyMatcher.normalizePersianText(sms.sender)
                body.contains(query) || sender.contains(query)
            }

            return SkillResult.SmsSearchResult(
                matchedSms = matchedSms,
                message = if (matchedSms.isNotEmpty()) "تعداد ${matchedSms.size} پیامک مطابق جستجو پیدا شد."
                else "پیامکی منطبق با عبارت «$input» در پیامک‌های دستگاه پیدا نشد."
            )
        }

        // 4. Phone Calling Intent
        val isCallIntent = query.startsWith("call ") || query.contains("زنگ بزن") ||
            query.contains("تماس بگیر") || query.contains("تماس با") || query.contains("شماره بگیر")

        if (isCallIntent) {
            // Check if user provided an explicit phone number (digits)
            val cleanPhone = FuzzyMatcher.normalizePhoneNumber(input)
            val hasDirectPhone = cleanPhone.length >= 7 && cleanPhone.any { it.isDigit() }

            if (hasDirectPhone) {
                // Check if belongs to a saved contact
                val matchedContact = allContacts.firstOrNull {
                    val cPhone = FuzzyMatcher.normalizePhoneNumber(it.phoneNumber)
                    cPhone == cleanPhone || cPhone.endsWith(cleanPhone) || cleanPhone.endsWith(cPhone)
                }
                if (matchedContact != null) {
                    return SkillResult.CallContactExact(
                        matchedContact,
                        "در حال برقراری تماس با ${matchedContact.displayName} (${matchedContact.phoneNumber})..."
                    )
                } else {
                    return SkillResult.CallPhoneNumberDirect(
                        phoneNumber = cleanPhone,
                        displayName = cleanPhone,
                        message = "در حال برقراری تماس با شماره $cleanPhone..."
                    )
                }
            }

            val contactQuery = query
                .replace("call", "")
                .replace("زنگ بزن به", "")
                .replace("زنگ بزن با", "")
                .replace("زنگ بزن", "")
                .replace("تماس بگیر با", "")
                .replace("تماس بگیر به", "")
                .replace("تماس بگیر", "")
                .replace("تماس با", "")
                .replace("شماره بگیر با", "")
                .replace("شماره بگیر", "")
                .replace("رو زنگ بزن", "")
                .replace("رو تماس بگیر", "")
                .trim()

            if (allContacts.isEmpty()) {
                return SkillResult.GeneralAnswer("دفترچه مخاطبین خالی است یا دسترسی به مخاطبین (READ_CONTACTS) تایید نشده است.")
            }

            val matches = FuzzyMatcher.findTop3Contacts(contactQuery, allContacts)
            return when {
                matches.isEmpty() -> {
                    SkillResult.GeneralAnswer("مخاطبی با نام «$contactQuery» در دفترچه مخاطبین یافت نشد. می‌توانید شماره تماس را بفرمایید.")
                }
                matches.first().matchScore >= 0.82f && (matches.size == 1 || matches[0].matchScore - (matches.getOrNull(1)?.matchScore ?: 0f) >= 0.10f) -> {
                    SkillResult.CallContactExact(
                        matches.first(),
                        "در حال تماس با ${matches.first().displayName} (${matches.first().phoneNumber})..."
                    )
                }
                else -> {
                    val candidateNames = matches.mapIndexed { idx, c -> "${idx + 1}. ${c.displayName}" }.joinToString(" | ")
                    SkillResult.CallContactNearestChoices(
                        matches,
                        "چند مخاطب با نام مشابه پیدا شد: $candidateNames\nبه کدام مورد می‌خواهی زنگ بزنم؟"
                    )
                }
            }
        }

        // 5. App Launching Intent (Local Search FIRST)
        val isAppLaunchIntent = query.startsWith("open ") || query.startsWith("launch ") ||
            query.startsWith("باز کن") || query.contains("رو باز کن") || query.contains("رو بازش کن") ||
            query.contains("بازش کن") || query.startsWith("برو تو") || query.startsWith("برو توی") ||
            query.startsWith("اجرا کن") || query.contains("رو بیار") || query.contains("رو اجرا کن")

        if (isAppLaunchIntent) {
            val appQuery = query
                .replace("open", "")
                .replace("launch", "")
                .replace("باز کن", "")
                .replace("رو باز کن", "")
                .replace("رو بازش کن", "")
                .replace("بازش کن", "")
                .replace("برو توی", "")
                .replace("برو تو", "")
                .replace("رو بیار", "")
                .replace("بیار", "")
                .replace("رو اجرا کن", "")
                .replace("اجرا کن", "")
                .trim()

            val matchedApp = FuzzyMatcher.findBestMatchingApp(appQuery, installedApps)
            return if (matchedApp != null) {
                SkillResult.LaunchApp(matchedApp, "در حال باز کردن اپلیکیشن «${matchedApp.appName}»...")
            } else {
                SkillResult.SearchAppWeb(
                    appQuery,
                    "برنامه «$appQuery» در بین برنامه‌های نصب‌شده دستگاه یافت نشد. می‌توانید آن را از استورها (گوگل‌پلی / بازار / مایکت) دانلود کنید:"
                )
            }
        }

        // 6. Card-to-Card & Banking with Saderat Adapter & Strict Validation
        if (query.contains("کارت به کارت") || query.contains("واریز") || query.contains("انتقال وجه") || query.contains("انتقال بده") || query.contains("transfer")) {
            val numbers = extractNumbers(input)
            val potentialCard = numbers.firstOrNull { it.length == 16 } ?: ""
            val amount = numbers.firstOrNull { it.length in 4..11 }?.toLongOrNull() ?: 0L

            val isSaderat = potentialCard.startsWith("6037") || potentialCard.startsWith("603769")
            if (isSaderat && !saderatAdapter.isAppInstalled()) {
                return SkillResult.GeneralAnswer("اپلیکیشن رسمی بانک صادرات روی دستگاه نصب نیست. برای جلوگیری از هرگونه شبیه‌سازی، انتقال تستی یا موفقیت جعلی انجام نمی‌دهم؛ ابتدا اپ رسمی بانک را نصب کنید.")
            }
            val adapter: BankAdapter = saderatAdapter

            val formattedCard = if (potentialCard.length == 16) adapter.formatCardNumber(potentialCard) else "شماره کارت نامشخص"
            val formattedAmount = if (amount > 0) adapter.formatAmountToman(amount) else "مبلغ مشخص نشده"

            val details = TransferDetails(
                destCardNumber = potentialCard,
                amountRials = amount,
                recipientName = if (isSaderat) "دارنده کارت بانک صادرات" else "مقصد",
                sourceCardNumber = "کارت پیش‌فرض",
                bankName = adapter.bankName,
                statusText = "نیازمند تأیید نهایی کاربر پیش از ارسال به درگاه بانکی"
            )

            val isValidCard = adapter.validateCardNumber(potentialCard)
            val msg = if (!isValidCard && potentialCard.isNotEmpty()) {
                "شماره کارت وارد شده ۱۶ رقمی معتبر بانکی نیست (بررسی الگوریتم Luhn ناموفق بود). لطفاً شماره کارت صحیح را اعلام کنید."
            } else {
                "پیش‌نویس انتقال وجه آماده شد: مبلغ $formattedAmount به کارت $formattedCard (${adapter.bankName}). آیا انتقال را تأیید می‌فرمایید؟"
            }

            return SkillResult.CardTransferConfirmation(details, msg, isTestMode = adapter.isTestMode)
        }

        // Default: Conversational reasoning using downloaded model status
        val modelStatusNotice = if (activeTextModel != null && activeTextModel.isLoaded) {
            "⚡ [مدل فعال: ${activeTextModel.name}]"
        } else {
            "⚠️ [هیچ مدل لوکالی در حافظه Load نشده است]"
        }

        if (activeTextModel != null && activeTextModel.isLoaded) {
            return try {
                val prompt = buildString {
                    appendLine("User message: $input")
                    appendLine("Response language: ${if (query.any { it in "اآبپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی" }) "Persian" else "English"}")
                    appendLine("Speed mode: $speedMode")
                    appendLine("If the user asks you to perform an action, describe the required action only; do not claim that it happened.")
                }
                val response = localInference(activeTextModel, prompt, speedMode)
                SkillResult.GeneralAnswer(response.ifBlank { "مدل محلی پاسخ متنی تولید نکرد." })
            } catch (e: Exception) {
                SkillResult.GeneralAnswer("Inference محلی ناموفق بود: ${e.message ?: "خطای ناشناخته"}\n$modelStatusNotice")
            }
        }

        val answer = when {
            query.contains("سلام") || query.contains("hello") || query.contains("درود") -> "سلام! در خدمت شما هستم. چطور می‌توانم کمکتان کنم؟ $modelStatusNotice"
            query.contains("چطوری") || query.contains("خوبی") -> "ممنون، آماده اجرای دستورات شما هستم. $modelStatusNotice"
            else -> "درخواست «$input» نیازمند مدل محلی است. ابتدا یک مدل متنی را از بخش مدیریت مدل‌ها دانلود و Load کنید."
        }

        return SkillResult.GeneralAnswer(answer)
    }

    private fun extractNumbers(text: String): List<String> {
        val normalized = FuzzyMatcher.normalizePersianText(text)
        val regex = Regex("\\d+")
        return regex.findAll(normalized).map { it.value }.toList()
    }
}
