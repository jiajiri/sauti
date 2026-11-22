package com.sauti.app.sync

import com.sauti.app.security.ModelCryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.spec.SecretKeySpec

class ModelBundleExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDecryptAndExtractZipStream_extractsBothVoiceAndCoreModels() {
        val testDir = tempFolder.newFolder("sauti_test")
        val key = SecretKeySpec(ByteArray(32) { 0x42.toByte() }, "AES")

        // 1. Create mock GGML Whisper header (0x6c 0x6d 0x67 0x67 = "lmgg")
        val ggmlHeader = byteArrayOf(0x6c.toByte(), 0x6d.toByte(), 0x67.toByte(), 0x67.toByte())
        val mockVoiceContent = ByteArray(1024 * 1024 + 64) { 0x01.toByte() }
        System.arraycopy(ggmlHeader, 0, mockVoiceContent, 0, 4)

        // 2. Create mock GGUF Core header ("GGUF" = 0x47 0x47 0x55 0x46)
        val ggufHeader = byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte())
        val mockCoreContent = ByteArray(1024 * 1024 + 64) { 0x02.toByte() }
        System.arraycopy(ggufHeader, 0, mockCoreContent, 0, 4)

        // 3. Package both into an unencrypted in-memory zip
        val zipBaos = ByteArrayOutputStream()
        ZipOutputStream(zipBaos).use { zos ->
            zos.putNextEntry(ZipEntry("voice_lite.bin"))
            zos.write(mockVoiceContent)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("core_pro.bin"))
            zos.write(mockCoreContent)
            zos.closeEntry()
        }
        val rawZipBytes = zipBaos.toByteArray()

        // 4. Encrypt zip bytes using AES-GCM-256
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(rawZipBytes)

        val encryptedPayload = ByteArrayOutputStream()
        encryptedPayload.write(iv)
        encryptedPayload.write(ciphertext)
        val encryptedBytes = encryptedPayload.toByteArray()

        // 5. Test decryptAndExtractZipStream
        val extractedMap = ModelCryptoManager.decryptAndExtractZipStream(
            encryptedStream = ByteArrayInputStream(encryptedBytes),
            targetDir = testDir,
            key = key,
            isGzipCompressed = false
        )

        // 6. Assertions
        assertEquals(2, extractedMap.size)
        val voiceFile = extractedMap["voice_lite.bin"]
        val coreFile = extractedMap["core_pro.bin"]

        assertNotNull(voiceFile)
        assertNotNull(coreFile)
        assertTrue(voiceFile!!.exists())
        assertTrue(coreFile!!.exists())

        assertTrue("Voice file must satisfy isValidGgml", ModelDownloadWorker.isValidGgml(voiceFile))
        assertTrue("Core file must satisfy isValidGguf", ModelDownloadWorker.isValidGguf(coreFile))
    }
}

