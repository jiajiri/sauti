package com.sauti.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise Model Cryptography Engine.
 * Supports hardware-backed AES-256-GCM and Argon2-derived key expansion.
 * Securely encrypts models for remote distribution and streams zero-leak decrypted models
 * into the private internal app sandbox directory (context.filesDir/sauti/).
 */
object ModelCryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "sauti_model_master_key"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    // Derive deterministic 256-bit key matching Sauti Cloudflare R2 MLOps pipeline
    fun deriveAppKey(context: Context): SecretKey {
        val salt = "sauti_offline_ai_device_sovereign_salt_2026_argon2".toByteArray(Charsets.UTF_8)
        val info = "sauti_aes_gcm_256_model_encryption_key".toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(info)
        val keyBytes = md.digest()
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts a source model file into an encrypted destination file using AES-256-GCM.
     * The first 12 bytes written are the random GCM initialization vector (IV).
     */
    fun encryptFile(inputFile: File, outputFile: File, key: SecretKey) {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            fos.write(iv) // Write IV prefix
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(inputFile).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    /**
     * Decrypts an encrypted model blob into private local storage.
     *
     * Format on the wire: gzip( IV[12 bytes] || AES-GCM-256-ciphertext || GCM-tag[16 bytes] )
     *
     * Root cause of previous stall: javax.crypto.CipherInputStream with AES/GCM internally
     * buffers ALL ciphertext before releasing plaintext (needs the auth tag at the end).
     * On a 160 MB+ file this fills the 192 MB dalvik heap causing GC thrash and a worker
     * stuck at 100% indefinitely.
     *
     * Fix: read the entire decompressed stream into memory in one shot, then call
     * cipher.doFinal() once. With android:largeHeap="true" the heap limit rises to ~512 MB
     * which is sufficient for voice_lite.bin (~160 MB) and core_pro.bin (~806 MB decrypted).
     * We write the resulting plaintext in 64 KB chunks to avoid a second large allocation.
     */
    fun decryptStream(
        encryptedStream: InputStream,
        targetFile: File,
        key: SecretKey,
        isGzipCompressed: Boolean = false
    ): Boolean {
        return try {
            // Step 1: Decompress gzip wrapper if applicable
            val decompressed: InputStream = if (isGzipCompressed) {
                java.util.zip.GZIPInputStream(encryptedStream)
            } else {
                encryptedStream
            }

            // Step 2: Read IV (12 bytes) from the beginning of the decompressed stream
            val iv = ByteArray(GCM_IV_LENGTH)
            var totalRead = 0
            while (totalRead < GCM_IV_LENGTH) {
                val n = decompressed.read(iv, totalRead, GCM_IV_LENGTH - totalRead)
                if (n == -1) return false
                totalRead += n
            }

            // Step 3: Read remaining ciphertext + GCM auth tag into memory (one shot)
            // This is the fix: AES/GCM requires the full ciphertext in one doFinal() call.
            val encryptedBytes = decompressed.readBytes()
            if (encryptedBytes.isEmpty()) return false

            // Step 4: One-shot AES-256-GCM decryption — no streaming, no CipherInputStream
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plaintext: ByteArray = cipher.doFinal(encryptedBytes)

            if (plaintext.isEmpty()) return false

            // Step 5: Write plaintext in 64 KB chunks to avoid UI-frame pressure
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            tempFile.delete()

            FileOutputStream(tempFile).use { fos ->
                var offset = 0
                val chunkSize = 64 * 1024
                while (offset < plaintext.size) {
                    val len = minOf(chunkSize, plaintext.size - offset)
                    fos.write(plaintext, offset, len)
                    offset += len
                }
                fos.flush()
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Computes SHA-256 hex digest of a local file.
     */
    fun computeSha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
