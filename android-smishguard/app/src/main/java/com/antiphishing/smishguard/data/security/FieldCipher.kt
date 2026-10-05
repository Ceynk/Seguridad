package com.antiphishing.smishguard.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifrado de campos sensibles (cuerpo original de los SMS en cuarentena) con AES-256-GCM.
 * La clave vive en el Android Keystore (respaldado por hardware cuando está disponible),
 * nunca sale del dispositivo y no se incluye en copias de seguridad.
 *
 * Si el Keystore no está disponible, se degrada a texto plano (la BD ya es privada de la app)
 * para no perder mensajes. Los valores antiguos sin prefijo se leen tal cual (compatibilidad).
 */
object FieldCipher {

    private const val TAG = "FieldCipher"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "smishguard_field_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc1:"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128

    private val secretKey: SecretKey? by lazy {
        try {
            loadOrCreateKey()
        } catch (e: Exception) {
            Log.w(TAG, "Keystore no disponible; se almacenará sin cifrado adicional")
            null
        }
    }

    private fun loadOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        val key = secretKey ?: return plain
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(iv + ct, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al cifrar campo")
            plain
        }
    }

    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        val key = secretKey ?: return ""
        return try {
            val data = Base64.decode(stored.substring(PREFIX.length), Base64.NO_WRAP)
            val iv = data.copyOfRange(0, IV_SIZE)
            val ct = data.copyOfRange(IV_SIZE, data.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al descifrar campo")
            ""
        }
    }
}
