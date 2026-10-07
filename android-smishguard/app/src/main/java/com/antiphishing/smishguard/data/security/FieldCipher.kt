package com.antiphishing.smishguard.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object FieldCipher {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "smishguard_field_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc1:"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128

    private var cachedSecretKey: SecretKey? = null

    private fun getSecretKey(): SecretKey? {
        val existing = cachedSecretKey
        if (existing != null) {
            return existing
        }
        val loaded = try {
            loadOrCreateKey()
        } catch (e: Exception) {
            null
        }
        cachedSecretKey = loaded
        return loaded
    }

    private fun loadOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE)
        ks.load(null)
        val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (entry != null) {
            return entry.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) {
            return plain
        }
        val key = getSecretKey()
        if (key == null) {
            return plain
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = iv + ct
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            PREFIX + encoded
        } catch (e: Exception) {
            plain
        }
    }

    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) {
            return stored
        }
        val key = getSecretKey()
        if (key == null) {
            return ""
        }
        return try {
            val base64Text = stored.substring(PREFIX.length)
            val data = Base64.decode(base64Text, Base64.NO_WRAP)
            val iv = data.copyOfRange(0, IV_SIZE)
            val ct = data.copyOfRange(IV_SIZE, data.size)
            val spec = GCMParameterSpec(TAG_BITS, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(ct)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
