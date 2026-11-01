package com.example.labo.engine

import com.example.labo.model.LogLevel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ArchiveEngine(
    private val coreEngine: CoreEngine
) : BaseEngine {
    override val name: String = "ArchiveEngine"
    override val description: String = "Moteur spécialisé dans la gestion, analyse et extraction d'archives (ZIP, TAR, LABX)"
    override val isReady: Boolean = true

    init {
        coreEngine.registerEngine(this)
    }

    fun extractZip(zipFile: File, targetDir: File, onProgress: (Float) -> Unit = {}) {
        coreEngine.log(LogLevel.INFO, name, "Extraction de l'archive ${zipFile.name} vers ${targetDir.name}...")
        targetDir.mkdirs()

        val totalBytes = zipFile.length().coerceAtLeast(1L)
        var readBytes = 0L

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } != -1) {
                            fos.write(buffer, 0, len)
                            readBytes += len
                            onProgress((readBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        coreEngine.log(LogLevel.SUCCESS, name, "Extraction terminée de ${zipFile.name}")
    }

    fun createZip(sourceDir: File, outputZip: File, onProgress: (Float) -> Unit = {}) {
        coreEngine.log(LogLevel.INFO, name, "Compression de ${sourceDir.name} vers ${outputZip.name}...")
        outputZip.parentFile?.mkdirs()

        val allFiles = mutableListOf<File>()
        fun collect(dir: File) {
            dir.listFiles()?.forEach { file ->
                allFiles.add(file)
                if (file.isDirectory) collect(file)
            }
        }
        collect(sourceDir)

        val totalCount = allFiles.size.coerceAtLeast(1)
        var processed = 0

        ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
            allFiles.forEach { file ->
                val relativePath = file.relativeTo(sourceDir).path.replace('\\', '/')
                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$relativePath/"))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(relativePath))
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (fis.read(buffer).also { len = it } != -1) {
                            zos.write(buffer, 0, len)
                        }
                    }
                    zos.closeEntry()
                }
                processed++
                onProgress(processed.toFloat() / totalCount)
            }
        }
        coreEngine.log(LogLevel.SUCCESS, name, "Archive créée avec succès : ${outputZip.name}")
    }
}
