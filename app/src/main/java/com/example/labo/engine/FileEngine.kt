package com.example.labo.engine

import android.content.Context
import com.example.labo.model.LogLevel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class FileEngine(private val context: Context, private val coreEngine: CoreEngine) : BaseEngine {
    override val name: String = "FileEngine"
    override val description: String = "Gestionnaire de flux, hash cryptographiques et arborescences de fichiers"
    override val isReady: Boolean = true

    init {
        coreEngine.registerEngine(this)
    }

    fun calculateSha256(file: File): String {
        if (!file.exists() || file.isDirectory) return ""
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrElse {
            coreEngine.log(LogLevel.ERROR, name, "Erreur calcul SHA256 pour ${file.name}: ${it.message}")
            ""
        }
    }

    fun calculateSha1(file: File): String {
        if (!file.exists() || file.isDirectory) return ""
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-1")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }

    fun readMagicBytes(file: File, length: Int = 8): ByteArray {
        if (!file.exists() || file.length() < length) return ByteArray(0)
        return runCatching {
            FileInputStream(file).use { fis ->
                val bytes = ByteArray(length)
                val read = fis.read(bytes)
                if (read == length) bytes else bytes.copyOf(read.coerceAtLeast(0))
            }
        }.getOrDefault(ByteArray(0))
    }

    fun copyFileWithProgress(source: File, destination: File, onProgress: (Float) -> Unit = {}) {
        destination.parentFile?.mkdirs()
        val totalLength = source.length().coerceAtLeast(1L)
        var totalCopied = 0L

        FileInputStream(source).use { fis ->
            FileOutputStream(destination).use { fos ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                    totalCopied += bytesRead
                    onProgress((totalCopied.toFloat() / totalLength).coerceIn(0f, 1f))
                }
            }
        }
    }

    fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }

    fun countFiles(dir: File): Int {
        if (!dir.exists()) return 0
        if (dir.isFile) return 1
        var count = 0
        dir.listFiles()?.forEach { file ->
            count += if (file.isDirectory) countFiles(file) else 1
        }
        return count
    }

    fun createTempFile(prefix: String, suffix: String): File {
        val tmpDir = File(context.cacheDir, "labo_tmp").apply { mkdirs() }
        return File.createTempFile(prefix, suffix, tmpDir)
    }

    fun cleanTempFiles() {
        val tmpDir = File(context.cacheDir, "labo_tmp")
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
            tmpDir.mkdirs()
        }
    }
}
