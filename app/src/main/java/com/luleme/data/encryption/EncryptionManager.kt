package com.luleme.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyAlias = "luleme_master_key"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    init {
        createKeyIfNotExists()
    }

    private fun createKeyIfNotExists() {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            generateKey()
        }
    }

    fun encryptData(plainText: String): String {
        try {
            val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
            val key = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Return Base64(IV + Encrypted)
            // IV is typically 12 bytes for GCM
            val combined = iv + encrypted
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText // Fallback or handle error
        }
    }

    fun decryptData(encryptedText: String): String {
        try {
            val decoded = Base64.decode(encryptedText, Base64.DEFAULT)
            
            // Extract IV (first 12 bytes)
            val iv = decoded.copyOfRange(0, 12)
            val encrypted = decoded.copyOfRange(12, decoded.size)

            val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
            val key = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "" // Handle error
        }
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            androidKeyStore
        )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) 
                .build()
        )

        keyGenerator.generateKey()
    }
}
