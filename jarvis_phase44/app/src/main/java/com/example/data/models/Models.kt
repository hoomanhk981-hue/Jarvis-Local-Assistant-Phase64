package com.example.data.models

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false
)

data class ContactMatch(
    val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val matchScore: Float = 1.0f
)

data class SmsMessageItem(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val category: String = "GENERAL" // GENERAL, OTP, TICKET, BANK
)

data class TransferDetails(
    val destCardNumber: String = "",
    val amountRials: Long = 0,
    val recipientName: String = "",
    val sourceCardNumber: String = "",
    val bankName: String = "",
    val otpCode: String = "",
    val statusText: String = ""
)

data class CodeFile(
    val name: String,
    val content: String,
    val language: String, // "python", "cpp", "json", "bash"
    val filePath: String = ""
)
