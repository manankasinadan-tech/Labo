package com.example.labo.engine

import com.example.labo.model.LogLevel
import com.example.labo.model.PartitionType
import java.io.File

data class PartitionEntry(
    val name: String,
    val type: PartitionType,
    val sizeBytes: Long,
    val offset: Long,
    val flags: Long
)

class PartitionEngine(
    private val coreEngine: CoreEngine
) : BaseEngine {
    override val name: String = "PartitionEngine"
    override val description: String = "Moteur d'analyse et gestion des structures de partitions et tables de partitionnement"
    override val isReady: Boolean = true

    init {
        coreEngine.registerEngine(this)
    }

    fun inspectPartitions(imageOrDevice: File): List<PartitionEntry> {
        coreEngine.log(LogLevel.INFO, name, "Inspection des partitions de ${imageOrDevice.name}...")
        // Detect standard sub-partitions
        val partitions = mutableListOf<PartitionEntry>()
        val nameLower = imageOrDevice.name.lowercase()

        val type = when {
            nameLower.contains("system") -> PartitionType.SYSTEM
            nameLower.contains("product") -> PartitionType.PRODUCT
            nameLower.contains("vendor") -> PartitionType.VENDOR
            nameLower.contains("system_ext") -> PartitionType.SYSTEM_EXT
            nameLower.contains("odm") -> PartitionType.ODM
            nameLower.contains("boot") -> PartitionType.BOOT
            else -> PartitionType.SYSTEM
        }

        partitions.add(
            PartitionEntry(
                name = type.name.lowercase(),
                type = type,
                sizeBytes = imageOrDevice.length(),
                offset = 0L,
                flags = 0L
            )
        )

        coreEngine.log(LogLevel.SUCCESS, name, "Partition principale identifiée : ${type.name} (${imageOrDevice.length() / (1024 * 1024)} Mo)")
        return partitions
    }
}
