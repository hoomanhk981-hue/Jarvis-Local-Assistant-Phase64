package com.example.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Maps tools to Android runtime permissions without ever attempting to bypass the OS gate. */
class PermissionCoordinator(private val context: Context) {
    fun missingPermissions(toolName: String): List<String> {
        val required = when (toolName) {
            "search_contacts" -> listOf(Manifest.permission.READ_CONTACTS)
            "make_call" -> listOf(Manifest.permission.CALL_PHONE)
            "search_sms" -> listOf(Manifest.permission.READ_SMS)
            "send_sms" -> listOf(Manifest.permission.SEND_SMS)
            "call_phone" -> listOf(Manifest.permission.CALL_PHONE)
            else -> emptyList()
        }
        return required.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    }
}
