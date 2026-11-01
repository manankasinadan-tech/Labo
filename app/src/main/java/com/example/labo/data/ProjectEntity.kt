package com.example.labo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.labo.model.FilesystemType
import com.example.labo.model.PartitionType
import com.example.labo.model.ProjectModel
import com.example.labo.model.ProjectStatus
import com.example.labo.model.SparseInfo

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourcePath: String,
    val targetPartition: String,
    val filesystem: String,
    val unpackDate: Long,
    val lastModifiedDate: Long,
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val status: String,
    val isSparse: Boolean,
    val hasFsConfig: Boolean,
    val hasFileContexts: Boolean
) {
    fun toModel(): ProjectModel {
        return ProjectModel(
            id = id,
            name = name,
            sourcePath = sourcePath,
            targetPartition = runCatching { PartitionType.valueOf(targetPartition) }.getOrDefault(PartitionType.SYSTEM),
            filesystem = runCatching { FilesystemType.valueOf(filesystem) }.getOrDefault(FilesystemType.EXT4),
            unpackDate = unpackDate,
            lastModifiedDate = lastModifiedDate,
            totalFiles = totalFiles,
            totalSizeBytes = totalSizeBytes,
            status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.EXTRACTED),
            sparseInfo = SparseInfo(isSparse = isSparse),
            hasFsConfig = hasFsConfig,
            hasFileContexts = hasFileContexts
        )
    }

    companion object {
        fun fromModel(model: ProjectModel): ProjectEntity {
            return ProjectEntity(
                id = model.id,
                name = model.name,
                sourcePath = model.sourcePath,
                targetPartition = model.targetPartition.name,
                filesystem = model.filesystem.name,
                unpackDate = model.unpackDate,
                lastModifiedDate = model.lastModifiedDate,
                totalFiles = model.totalFiles,
                totalSizeBytes = model.totalSizeBytes,
                status = model.status.name,
                isSparse = model.sparseInfo.isSparse,
                hasFsConfig = model.hasFsConfig,
                hasFileContexts = model.hasFileContexts
            )
        }
    }
}

@Entity(tableName = "file_records")
data class FileRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val projectId: String,
    val path: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val permissions: String,
    val uid: Int,
    val gid: Int,
    val selinuxContext: String,
    val diffState: String,
    val originalSha256: String,
    val currentSha256: String
)
