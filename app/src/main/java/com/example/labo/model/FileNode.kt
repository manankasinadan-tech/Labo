package com.example.labo.model

enum class DiffState {
    UNMODIFIED,
    MODIFIED,
    ADDED,
    DELETED
}

data class FileNode(
    val path: String,
    val relativePath: String,
    val name: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val modifiedTimestamp: Long,
    val permissions: String = "0644",
    val uid: Int = 0,
    val gid: Int = 0,
    val selinuxContext: String = "u:object_r:system_file:s0",
    val diffState: DiffState = DiffState.UNMODIFIED,
    val sha256: String = "",
    val children: List<FileNode> = emptyList()
)
