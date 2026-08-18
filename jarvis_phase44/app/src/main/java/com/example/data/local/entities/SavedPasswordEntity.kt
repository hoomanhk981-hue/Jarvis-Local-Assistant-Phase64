package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_passwords")
data class SavedPasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val accountName: String,
    val passwordEncrypted: String,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
