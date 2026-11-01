package com.example.labo.engine

import com.example.labo.model.FilesystemType
import com.example.labo.model.LogLevel
import com.example.labo.model.PartitionType
import com.example.labo.model.SparseInfo
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ImageAnalysisResult(
    val file: File,
    val filesystem: FilesystemType,
    val partitionHint: PartitionType,
    val sparseInfo: SparseInfo,
    val estimatedSize: Long,
    val rawSize: Long,
    val isBootableGsi: Boolean,
    val details: Map<String, String>
)

class AnalystEngine(
    private val coreEngine: CoreEngine,
    private val fileEngine: FileEngine
) : BaseEngine {
    override val name: String = "AnalystEngine"
    override val description: String = "Moteur central d'analyse technique, détection des formats et inspection des images"
    override val isReady: Boolean = true

    init {
        coreEngine.registerEngine(this)
    }

    fun analyzeImage(imageFile: File): ImageAnalysisResult {
        coreEngine.log(LogLevel.INFO, name, "Analyse technique de : ${imageFile.name} (${imageFile.length()} octets)")

        if (!imageFile.exists() || imageFile.length() < 1024) {
            coreEngine.log(LogLevel.WARN, name, "Fichier trop petit ou inexistant.")
            return ImageAnalysisResult(
                file = imageFile,
                filesystem = FilesystemType.UNKNOWN,
                partitionHint = PartitionType.UNKNOWN,
                sparseInfo = SparseInfo(),
                estimatedSize = imageFile.length(),
                rawSize = imageFile.length(),
                isBootableGsi = false,
                details = mapOf("Erreur" to "Fichier invalide")
            )
        }

        var detectedFs = FilesystemType.UNKNOWN
        var sparse = SparseInfo()
        val details = mutableMapOf<String, String>()

        runCatching {
            RandomAccessFile(imageFile, "r").use { raf ->
                // Check sparse magic: 0xED26FF3A
                val magicBuffer = ByteArray(4)
                raf.readFully(magicBuffer)
                val magicInt = ByteBuffer.wrap(magicBuffer).order(ByteOrder.LITTLE_ENDIAN).int

                if (magicInt == 0xED26FF3A.toInt()) {
                    // It is an Android Sparse Image
                    val majorVersion = raf.readUnsignedShort()
                    val minorVersion = raf.readUnsignedShort()
                    val fileHeaderSize = raf.readUnsignedShort()
                    val chunkHeaderSize = raf.readUnsignedShort()
                    val blockSize = raf.readInt()
                    val totalBlocks = raf.readInt().toLong() and 0xFFFFFFFFL
                    val totalChunks = raf.readInt()

                    sparse = SparseInfo(
                        isSparse = true,
                        blockSize = blockSize,
                        totalBlocks = totalBlocks,
                        totalChunks = totalChunks
                    )
                    detectedFs = FilesystemType.SPARSE
                    details["Format"] = "Android Sparse Image v$majorVersion.$minorVersion"
                    details["BlockSize"] = "$blockSize octets"
                    details["TotalBlocks"] = totalBlocks.toString()
                    details["TotalChunks"] = totalChunks.toString()
                    details["RawUncompressedSize"] = "${(totalBlocks * blockSize) / (1024 * 1024)} Mo"
                    coreEngine.log(LogLevel.SUCCESS, name, "Image Sparse Android détectée (${sparse.totalBlocks} blocs)")
                } else {
                    // Check for ext4 superblock at offset 1024
                    raf.seek(1024)
                    val sbBuffer = ByteArray(1024)
                    raf.readFully(sbBuffer)
                    val sbWrap = ByteBuffer.wrap(sbBuffer).order(ByteOrder.LITTLE_ENDIAN)

                    // ext4 magic is at offset 0x38 (56) in superblock: 0xEF53
                    val ext4Magic = sbWrap.getShort(56).toInt() and 0xFFFF
                    if (ext4Magic == 0xEF53) {
                        detectedFs = FilesystemType.EXT4
                        val sInodesCount = sbWrap.getInt(0)
                        val sBlocksCount = sbWrap.getInt(4)
                        details["Format"] = "Système de fichiers Linux ext4"
                        details["InodesCount"] = sInodesCount.toString()
                        details["BlocksCount"] = sBlocksCount.toString()
                        details["Magic"] = "0xEF53"
                        coreEngine.log(LogLevel.SUCCESS, name, "Système de fichiers ext4 valide détecté (0xEF53)")
                    } else {
                        // Check for EROFS superblock at offset 1024: 0xE0F5E1E2
                        val erofsMagic = sbWrap.getInt(0)
                        if (erofsMagic == 0xE0F5E1E2.toInt()) {
                            detectedFs = FilesystemType.EROFS
                            details["Format"] = "Système de fichiers EROFS (Enhanced Read-Only FS)"
                            details["Magic"] = "0xE0F5E1E2"
                            coreEngine.log(LogLevel.SUCCESS, name, "Système de fichiers EROFS détecté (0xE0F5E1E2)")
                        } else {
                            detectedFs = FilesystemType.RAW
                            details["Format"] = "Image Raw brute ou non-sparse"
                        }
                    }
                }
            }
        }.onFailure {
            coreEngine.log(LogLevel.ERROR, name, "Erreur lecture entête : ${it.message}")
        }

        // Guess partition from filename
        val lowerName = imageFile.name.lowercase()
        val partitionHint = when {
            lowerName.contains("system") -> PartitionType.SYSTEM
            lowerName.contains("product") -> PartitionType.PRODUCT
            lowerName.contains("vendor") -> PartitionType.VENDOR
            lowerName.contains("system_ext") -> PartitionType.SYSTEM_EXT
            lowerName.contains("odm") -> PartitionType.ODM
            lowerName.contains("boot") -> PartitionType.BOOT
            else -> PartitionType.SYSTEM
        }

        details["PartitionCible"] = partitionHint.name
        details["TailleFichier"] = "${imageFile.length() / (1024 * 1024)} Mo"

        return ImageAnalysisResult(
            file = imageFile,
            filesystem = detectedFs,
            partitionHint = partitionHint,
            sparseInfo = sparse,
            estimatedSize = if (sparse.isSparse) sparse.totalBlocks * sparse.blockSize else imageFile.length(),
            rawSize = imageFile.length(),
            isBootableGsi = partitionHint == PartitionType.SYSTEM || partitionHint == PartitionType.PRODUCT,
            details = details
        )
    }

    fun parseBuildProp(buildPropFile: File): Map<String, String> {
        if (!buildPropFile.exists()) return emptyMap()
        val map = mutableMapOf<String, String>()
        runCatching {
            buildPropFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val key = trimmed.substringBefore("=").trim()
                    val value = trimmed.substringAfter("=").trim()
                    map[key] = value
                }
            }
        }
        return map
    }
}
