package com.ledgerx.app.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class PortableBackupCryptoTest {
    @Test fun encryptedBackupRoundTrips() {
        val clear = "private financial data ₹".toByteArray()
        val encrypted = PortableBackupCrypto.encrypt(clear, "correct horse battery staple".toCharArray())
        assertFalse(clear.contentEquals(encrypted))
        assertArrayEquals(clear, PortableBackupCrypto.decrypt(encrypted, "correct horse battery staple".toCharArray()))
    }

    @Test fun wrongPasswordAndTamperingFailAuthentication() {
        val encrypted = PortableBackupCrypto.encrypt("records".toByteArray(), "correct horse battery staple".toCharArray())
        assertThrows(IllegalArgumentException::class.java) { PortableBackupCrypto.decrypt(encrypted, "incorrect password value".toCharArray()) }
        encrypted[encrypted.lastIndex] = (encrypted.last() + 1).toByte()
        assertThrows(IllegalArgumentException::class.java) { PortableBackupCrypto.decrypt(encrypted, "correct horse battery staple".toCharArray()) }
    }
}
