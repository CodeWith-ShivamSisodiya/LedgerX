package com.ledgerx.app.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PortableBackupCrypto {
    private val magic = byteArrayOf(0x4c, 0x58, 0x42, 0x4b)
    private const val version = 1
    private const val iterations = 210_000

    fun encrypt(plain: ByteArray, password: CharArray, random: SecureRandom = SecureRandom()): ByteArray {
        require(password.size >= 12) { "Backup password must contain at least 12 characters" }
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output -> output.write(magic); output.writeInt(version); output.write(salt); output.write(iv); output.write(cipher.doFinal(plain)) }
            bytes.toByteArray()
        }
    }

    fun decrypt(encrypted: ByteArray, password: CharArray): ByteArray = try {
        require(password.size >= 12) { "Backup password must contain at least 12 characters" }
        DataInputStream(ByteArrayInputStream(encrypted)).use { input ->
            val header = ByteArray(magic.size).also(input::readFully)
            require(header.contentEquals(magic)) { "Not a LedgerX backup" }
            require(input.readInt() == version) { "Unsupported LedgerX backup version" }
            val salt = ByteArray(16).also(input::readFully)
            val iv = ByteArray(12).also(input::readFully)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
            cipher.doFinal(input.readBytes())
        }
    } catch (error: Exception) {
        throw IllegalArgumentException("Backup password is incorrect or the file is damaged", error)
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }
}
