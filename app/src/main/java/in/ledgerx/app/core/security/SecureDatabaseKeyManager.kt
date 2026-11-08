package com.ledgerx.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureDatabaseKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val preferences by lazy {
        context.getSharedPreferences("ledgerx_secure_material", Context.MODE_PRIVATE)
    }

    fun getOrCreatePassphrase(): ByteArray {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null)
        val iv = preferences.getString(KEY_IV, null)
        if (ciphertext != null && iv != null) {
            return decrypt(Base64.decode(ciphertext, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP))
        }

        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        }
        val encrypted = cipher.doFinal(passphrase)
        val persisted = preferences.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()
        check(persisted) { "Unable to persist wrapped database key material" }
        return passphrase
    }

    fun destroyKeyMaterial() {
        check(preferences.edit().clear().commit()) { "Unable to remove wrapped database key material" }
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray =
        Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, getOrCreateWrappingKey(), GCMParameterSpec(128, iv))
            doFinal(ciphertext)
        }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "ledgerx_database_wrapping_key_v1"
        const val KEY_CIPHERTEXT = "database_passphrase_ciphertext"
        const val KEY_IV = "database_passphrase_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
