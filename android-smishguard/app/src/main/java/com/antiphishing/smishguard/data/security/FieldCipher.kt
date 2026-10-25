package com.antiphishing.smishguard.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.UnrecoverableKeyException
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

    @Volatile
    private var cachedSecretKey: SecretKey? = null

    sealed interface DecryptionResult {
        data class Decrypted(val value: String) : DecryptionResult
        data class LegacyPlaintext(val value: String) : DecryptionResult
        enum class FailureReason { INVALID_PREFIX, CORRUPT_DATA, KEY_UNAVAILABLE }
        data class Unreadable(val reason: FailureReason) : DecryptionResult
    }

    private fun getSecretKey(createIfMissing: Boolean): SecretKey {
        val existing = cachedSecretKey
        if (existing != null) {
            return existing
        }
        return synchronized(this) {
            val cached = cachedSecretKey
            if (cached != null) {
                cached
            } else {
                loadKey(createIfMissing).also { cachedSecretKey = it }
            }
        }
    }

    private fun loadKey(createIfMissing: Boolean): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE)
        ks.load(null)
        val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (entry != null) {
            return entry.secretKey
        }
        if (!createIfMissing) {
            throw KeyStoreException("La clave de cifrado no está disponible")
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
        val key = getSecretKey(createIfMissing = true)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = iv + ct
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            PREFIX + encoded
        } catch (e: Exception) {
            throw IllegalStateException("No se pudo cifrar el campo", e)
        }
    }

    fun decrypt(stored: String): DecryptionResult {
        if (!stored.startsWith(PREFIX)) {
            return DecryptionResult.LegacyPlaintext(stored)
        }
        return try {
            val key = getSecretKey(createIfMissing = false)
            val base64Text = stored.substring(PREFIX.length)
            val data = Base64.decode(base64Text, Base64.NO_WRAP)
            if (data.size <= IV_SIZE) {
                return DecryptionResult.Unreadable(DecryptionResult.FailureReason.CORRUPT_DATA)
            }
            val iv = data.copyOfRange(0, IV_SIZE)
            val ct = data.copyOfRange(IV_SIZE, data.size)
            val spec = GCMParameterSpec(TAG_BITS, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(ct)
            DecryptionResult.Decrypted(String(decryptedBytes, Charsets.UTF_8))
        } catch (_: KeyStoreException) {
            DecryptionResult.Unreadable(DecryptionResult.FailureReason.KEY_UNAVAILABLE)
        } catch (_: UnrecoverableKeyException) {
            DecryptionResult.Unreadable(DecryptionResult.FailureReason.KEY_UNAVAILABLE)
        } catch (e: Exception) {
            DecryptionResult.Unreadable(DecryptionResult.FailureReason.CORRUPT_DATA)
        }
    }
}
