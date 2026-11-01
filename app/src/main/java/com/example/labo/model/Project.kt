package com.example.labo.model

enum class PartitionType {
    SYSTEM, SYSTEM_EXT, PRODUCT, VENDOR, ODM, BOOT, INIT_BOOT, VENDOR_BOOT, UNKNOWN
}

enum class FilesystemType {
    EXT4, EROFS, RAW, SPARSE, UNKNOWN
}

enum class ProjectStatus {
    EXTRACTED,
    MODIFIED,
    REPACKED,
    SIGN_AUDITED
}

data class SparseInfo(
    val isSparse: Boolean = false,
    val blockSize: Int = 4096,
    val totalBlocks: Long = 0,
    val totalChunks: Int = 0
)

data class ProjectModel(
    val id: String,
    val name: String,
    val sourcePath: String,
    val targetPartition: PartitionType = PartitionType.SYSTEM,
    val filesystem: FilesystemType = FilesystemType.EXT4,
    val unpackDate: Long = System.currentTimeMillis(),
    val lastModifiedDate: Long = System.currentTimeMillis(),
    val totalFiles: Int = 0,
    val totalSizeBytes: Long = 0L,
    val status: ProjectStatus = ProjectStatus.EXTRACTED,
    val sparseInfo: SparseInfo = SparseInfo(),
    val buildPropSummary: Map<String, String> = emptyMap(),
    val hasFsConfig: Boolean = true,
    val hasFileContexts: Boolean = true
)
