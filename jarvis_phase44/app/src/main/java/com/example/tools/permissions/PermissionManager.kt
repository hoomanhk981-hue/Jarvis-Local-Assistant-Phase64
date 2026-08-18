package com.example.tools.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class AppPermission {
    MICROPHONE,
    READ_SMS,
    SEND_SMS,
    READ_CONTACTS
}

class PermissionManager(private val context: Context) {

    fun androidPermission(permission: AppPermission): String = when (permission) {
        AppPermission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        AppPermission.READ_SMS -> Manifest.permission.READ_SMS
        AppPermission.SEND_SMS -> Manifest.permission.SEND_SMS
        AppPermission.READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    }

    fun isGranted(permission: AppPermission): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            androidPermission(permission)
        ) == PackageManager.PERMISSION_GRANTED

    fun missing(permissions: Collection<AppPermission>): List<AppPermission> =
        permissions.filterNot(::isGranted)

    fun request(activity: Activity, permissions: Collection<AppPermission>, requestCode: Int) {
        val missingPermissions = missing(permissions)
        if (missingPermissions.isEmpty()) return

        ActivityCompat.requestPermissions(
            activity,
            missingPermissions.map(::androidPermission).toTypedArray(),
            requestCode
        )
    }

    fun shouldExplain(activity: Activity, permission: AppPermission): Boolean =
        !isGranted(permission) &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                androidPermission(permission)
            )
}
