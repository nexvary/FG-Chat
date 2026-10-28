package com.fgchat.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Fail closed: a corrupt/unavailable vault is never replaced by a new identity. */
class IdentityVault(context: Context, private val namespace: String = "identity") {
    init { require(namespace.matches(Regex("[a-z-]{1,32}"))) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "$namespace.v1"))
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("fgchat.$namespace.v1", null) as? SecretKey)?.let { return it }
        check(!file.baseFile.exists()) { "Identity key missing; existing data preserved" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("fgchat.$namespace.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    fun load(): ByteArray? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        val bytes = file.readFully()
        check(bytes.size in 29..(4 * 1024 * 1024)) { "Invalid identity vault" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12)))
        cipher.updateAAD("FGCHAT-IDENTITY-1".toByteArray())
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size))
    }
    fun save(plaintext: ByteArray) {
        require(plaintext.size <= 4 * 1024 * 1024 - 28) { "Identity vault size limit exceeded" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD("FGCHAT-IDENTITY-1".toByteArray())
        val encrypted = cipher.iv + cipher.doFinal(plaintext)
        val stream = file.startWrite()
        try { stream.write(encrypted); file.finishWrite(stream) } catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
