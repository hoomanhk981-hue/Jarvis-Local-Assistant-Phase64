package com.example.tools.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class SmsMessage(
    val id: Long,
    val address: String?,
    val body: String,
    val timestamp: Long,
    val type: Int
)

class SmsTool(private val context: Context) {

    fun canRead(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

    fun canSend(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

    fun readLatest(limit: Int = 50): Result<List<SmsMessage>> {
        if (!canRead()) {
            return Result.failure(SecurityException("READ_SMS permission is required."))
        }

        val safeLimit = limit.coerceIn(1, 200)
        val result = mutableListOf<SmsMessage>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $safeLimit"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val address = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (cursor.moveToNext()) {
                result += SmsMessage(
                    cursor.getLong(id),
                    if (address >= 0) cursor.getString(address) else null,
                    cursor.getString(body) ?: "",
                    cursor.getLong(date),
                    cursor.getInt(type)
                )
            }
        }

        return Result.success(result)
    }

    fun send(phone: String, message: String): Result<Unit> {
        if (!canSend()) {
            return Result.failure(SecurityException("SEND_SMS permission is required."))
        }
        if (phone.isBlank() || message.isBlank()) {
            return Result.failure(IllegalArgumentException("Phone and message are required."))
        }

        return try {
            val manager = android.telephony.SmsManager.getDefault()
            manager.sendTextMessage(phone, null, message, null, null)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
