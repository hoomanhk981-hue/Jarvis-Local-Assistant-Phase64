package com.example.tools.contacts;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import androidx.core.content.ContextCompat;

data class ContactMatch(
    val contactId: Long,
    val displayName: String,
    val phones: List<String>
)

class ContactsTool(private val context: Context) {
    fun canRead(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    fun search(query: String, limit: Int = 20): Result<List<ContactMatch>> {
        if (!canRead()) return Result.failure(
            SecurityException("READ_CONTACTS permission is required.")
        )

        val q = query.trim()
        if (q.isEmpty()) return Result.failure(
            IllegalArgumentException("Contact search query is empty.")
        )

        val safeLimit = limit.coerceIn(1, 100)
        val contacts = LinkedHashMap<Long, ContactMatch>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection =
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR " +
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val like = "%$q%"

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, selection, arrayOf(like, like),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val name = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val number = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext() && contacts.size < safeLimit) {
                val contactId = cursor.getLong(id)
                val displayName = cursor.getString(name) ?: ""
                val phone = cursor.getString(number) ?: ""
                val old = contacts[contactId]
                contacts[contactId] = if (old == null)
                    ContactMatch(contactId, displayName, listOf(phone))
                else old.copy(phones = (old.phones + phone).distinct())
            }
        }
        return Result.success(contacts.values.toList())
    }
}
