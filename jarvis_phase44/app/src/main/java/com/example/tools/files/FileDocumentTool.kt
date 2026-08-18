package com.example.tools.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap

data class OpenableFile(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String
)

class FileDocumentTool(private val context: Context) {

    fun open(uri: Uri): Result<Unit> {
        val mime = context.contentResolver.getType(uri)
            ?: guessMime(uri)
            ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) == null) {
                Result.failure(
                    IllegalArgumentException("No compatible application can open this file.")
                )
            } else {
                context.startActivity(intent)
                Result.success(Unit)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun createOpenIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    fun createOpenDocumentIntent(mimeTypes: Array<String>): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    fun persistReadPermission(uri: Uri): Result<Unit> =
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }

    private fun guessMime(uri: Uri): String? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            .lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }
}
