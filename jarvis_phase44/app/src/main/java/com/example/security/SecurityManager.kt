package com.example.security

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-local encryption backed by Android Keystore. Plaintext secrets never belong in logs. */
class SecurityManager(private val context: Context) {
    companion object { private const val KEYSTORE = "AndroidKeyStore"; private const val ALIAS = "jarvis.local.vault.v1" }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", KEYSTORE)
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val out = ByteArray(cipher.iv.size + encrypted.size)
        System.arraycopy(cipher.iv, 0, out, 0, cipher.iv.size)
        System.arraycopy(encrypted, 0, out, cipher.iv.size, encrypted.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String): String {
        val data = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        require(data.size > 12) { "Invalid encrypted value" }
        val iv = data.copyOfRange(0, 12)
        val payload = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(payload), StandardCharsets.UTF_8)
    }

    fun sanitizeForAuditLog(text: String): String = text
        .replace(Regex("(?<!\\d)(?:\\d[ -]?){15}\\d(?!\\d)"), "[CARD_PROTECTED]")
        .replace(Regex("(?i)(رمز پویا|otp|رمز عبور|password|cvv2)[:\\s]+[^\\s,;]+"), "$1: [PROTECTED]")
}
