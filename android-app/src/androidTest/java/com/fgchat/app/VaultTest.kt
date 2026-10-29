package com.fgchat.app

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyStore

class VaultTest {
    @Test fun keystoreRoundTripAndTamperFailurePreservesData() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.noBackupFilesDir,"test-identity.v1")
        val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        file.delete(); store.deleteEntry("fgchat.test-identity.v1")
        try {
            val vault=IdentityVault(context,"test-identity")
            assertNull(vault.load())
            val secret=ByteArray(512) { (it%251).toByte() }
            vault.save(secret);assertArrayEquals(secret,vault.load())
            val ciphertext=file.readBytes();assertFalse(ciphertext.contentEquals(secret))
            ciphertext[ciphertext.lastIndex]=(ciphertext.last().toInt() xor 1).toByte()
            file.writeBytes(ciphertext)
            var rejected=false
            try {vault.load()} catch (_: Exception) {rejected=true}
            assertTrue(rejected);assertArrayEquals(ciphertext,file.readBytes())
        } finally {file.delete();store.deleteEntry("fgchat.test-identity.v1")}
    }
}
