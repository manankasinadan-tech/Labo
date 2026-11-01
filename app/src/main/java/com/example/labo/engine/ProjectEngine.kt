package com.example.labo.engine

import android.content.Context
import com.example.labo.data.FileRecordEntity
import com.example.labo.data.LaboDatabase
import com.example.labo.data.ProjectEntity
import com.example.labo.model.DiffState
import com.example.labo.model.FileNode
import com.example.labo.model.FilesystemType
import com.example.labo.model.LogLevel
import com.example.labo.model.PartitionType
import com.example.labo.model.ProjectModel
import com.example.labo.model.ProjectStatus
import com.example.labo.model.SparseInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectEngine(
    private val context: Context,
    private val coreEngine: CoreEngine,
    private val fileEngine: FileEngine,
    private val database: LaboDatabase
) : BaseEngine {
    override val name: String = "ProjectEngine"
    override val description: String = "Gestionnaire du cycle de vie des projets et des espaces de travail isolés"
    override val isReady: Boolean = true

    val workspaceRoot: File by lazy {
        File(context.filesDir, "workspace").apply { mkdirs() }
    }

    val projectsDir: File by lazy {
        File(workspaceRoot, "projects").apply { mkdirs() }
    }

    val externalRepackDir: File
        get() = com.example.labo.util.StorageUtils.getLaboDirectory(context, "REPACKED")

    val externalRomSignDir: File
        get() = com.example.labo.util.StorageUtils.getLaboDirectory(context, "ROMSIGN")

    init {
        coreEngine.registerEngine(this)
        coreEngine.log(LogLevel.DEBUG, name, "Workspace initialisé dans : ${workspaceRoot.absolutePath}")
    }

    fun getAllProjectsFlow(): Flow<List<ProjectModel>> {
        return database.projectDao().getAllProjects().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getProjectById(projectId: String): ProjectModel? {
        return database.projectDao().getProjectById(projectId)?.toModel()
    }

    fun getProjectDirectory(projectId: String): File {
        return File(projectsDir, projectId)
    }

    fun getExtractedDirectory(projectId: String): File {
        return File(getProjectDirectory(projectId), "extracted")
    }

    fun getConfigDirectory(projectId: String): File {
        return File(getProjectDirectory(projectId), "config")
    }

    fun getDatabaseDirectory(projectId: String): File {
        return File(getProjectDirectory(projectId), "database")
    }

    suspend fun createProject(
        name: String,
        sourceImagePath: String,
        partition: PartitionType,
        filesystem: FilesystemType,
        sparseInfo: SparseInfo = SparseInfo()
    ): ProjectModel {
        val projectId = UUID.randomUUID().toString()
        val projDir = getProjectDirectory(projectId).apply { mkdirs() }
        val extDir = getExtractedDirectory(projectId).apply { mkdirs() }
        val cfgDir = getConfigDirectory(projectId).apply { mkdirs() }
        val dbDir = getDatabaseDirectory(projectId).apply { mkdirs() }

        val model = ProjectModel(
            id = projectId,
            name = name,
            sourcePath = sourceImagePath,
            targetPartition = partition,
            filesystem = filesystem,
            unpackDate = System.currentTimeMillis(),
            lastModifiedDate = System.currentTimeMillis(),
            totalFiles = 0,
            totalSizeBytes = 0L,
            status = ProjectStatus.EXTRACTED,
            sparseInfo = sparseInfo,
            hasFsConfig = true,
            hasFileContexts = true
        )

        // Save sparse_info.json in config
        val sparseJson = JSONObject().apply {
            put("is_sparse", sparseInfo.isSparse)
            put("block_size", sparseInfo.blockSize)
            put("total_blocks", sparseInfo.totalBlocks)
            put("total_chunks", sparseInfo.totalChunks)
        }
        File(cfgDir, "sparse_info.json").writeText(sparseJson.toString(2))

        // Save metadata.json in config
        val metaJson = JSONObject().apply {
            put("project_id", projectId)
            put("name", name)
            put("source_path", sourceImagePath)
            put("partition", partition.name)
            put("filesystem", filesystem.name)
            put("created_at", System.currentTimeMillis())
        }
        File(cfgDir, "metadata.json").writeText(metaJson.toString(2))

        database.projectDao().insertProject(ProjectEntity.fromModel(model))
        coreEngine.log(LogLevel.SUCCESS, name, "Projet créé : [$name] (ID: $projectId)")
        return model
    }

    suspend fun updateProject(project: ProjectModel) {
        database.projectDao().updateProject(ProjectEntity.fromModel(project))
    }

    suspend fun deleteProject(projectId: String) {
        val dir = getProjectDirectory(projectId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        database.fileRecordDao().deleteRecordsByProject(projectId)
        database.projectDao().deleteProjectById(projectId)
        coreEngine.log(LogLevel.INFO, name, "Projet $projectId supprimé.")
    }

    suspend fun saveInitialFileRecords(projectId: String, files: List<FileRecordEntity>) {
        database.fileRecordDao().insertRecords(files)
        val proj = getProjectById(projectId)
        if (proj != null) {
            val totalSize = files.sumOf { it.sizeBytes }
            updateProject(proj.copy(totalFiles = files.size, totalSizeBytes = totalSize))
        }
    }

    suspend fun scanProjectDiff(projectId: String): List<FileNode> {
        val extDir = getExtractedDirectory(projectId)
        if (!extDir.exists()) return emptyList()

        val savedRecords = database.fileRecordDao().getRecordsByProject(projectId).associateBy { it.relativePath }
        val visitedRelativePaths = mutableSetOf<String>()

        fun buildTree(currentFile: File, relative: String): FileNode {
            val isDir = currentFile.isDirectory
            val relPath = if (relative.isEmpty()) currentFile.name else "$relative/${currentFile.name}"
            visitedRelativePaths.add(relPath)

            val saved = savedRecords[relPath]
            val currentSha = if (!isDir) fileEngine.calculateSha256(currentFile) else ""

            val diffState = when {
                saved == null -> DiffState.ADDED
                !isDir && saved.originalSha256.isNotEmpty() && saved.originalSha256 != currentSha -> DiffState.MODIFIED
                else -> DiffState.UNMODIFIED
            }

            val children = if (isDir) {
                val list = currentFile.listFiles()?.toList() ?: emptyList()
                val sortedChildren = list.sortedWith(
                    compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
                )
                sortedChildren.map { child -> buildTree(child, relPath) }
            } else emptyList()

            return FileNode(
                path = currentFile.absolutePath,
                relativePath = relPath,
                name = currentFile.name,
                sizeBytes = if (isDir) 0L else currentFile.length(),
                isDirectory = isDir,
                modifiedTimestamp = currentFile.lastModified(),
                permissions = saved?.permissions ?: if (isDir) "0755" else "0644",
                uid = saved?.uid ?: 0,
                gid = saved?.gid ?: 0,
                selinuxContext = saved?.selinuxContext ?: "u:object_r:system_file:s0",
                diffState = diffState,
                sha256 = currentSha,
                children = children
            )
        }

        val fileList = extDir.listFiles()?.toList() ?: emptyList()
        val sortedList = fileList.sortedWith(
            compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
        )
        val activeNodes = sortedList.map { file ->
            buildTree(file, "")
        }.toMutableList()

        // Detect deleted files
        val deletedRecords = savedRecords.filterKeys { !visitedRelativePaths.contains(it) }
        deletedRecords.values.forEach { del ->
            activeNodes.add(
                FileNode(
                    path = File(extDir, del.relativePath).absolutePath,
                    relativePath = del.relativePath,
                    name = del.relativePath.substringAfterLast('/'),
                    sizeBytes = del.sizeBytes,
                    isDirectory = del.isDirectory,
                    modifiedTimestamp = System.currentTimeMillis(),
                    permissions = del.permissions,
                    uid = del.uid,
                    gid = del.gid,
                    selinuxContext = del.selinuxContext,
                    diffState = DiffState.DELETED,
                    sha256 = del.originalSha256
                )
            )
        }

        // Directories ALWAYS sorted first on top, then alphabetical
        return activeNodes.sortedWith(
            compareByDescending<FileNode> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Scans and returns all nodes strictly inside a given relative subfolder of the extracted directory,
     * guaranteeing that directories are placed at the top before files.
     */
    suspend fun getProjectFolderContents(projectId: String, relativeFolder: String = ""): List<FileNode> {
        val extDir = getExtractedDirectory(projectId)
        val targetDir = if (relativeFolder.isEmpty()) extDir else File(extDir, relativeFolder)
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        val savedRecords = database.fileRecordDao().getRecordsByProject(projectId).associateBy { it.relativePath }
        val files = targetDir.listFiles()?.toList() ?: emptyList()

        val nodes = files.map { file ->
            val relPath = if (relativeFolder.isEmpty()) file.name else "$relativeFolder/${file.name}"
            val isDir = file.isDirectory
            val saved = savedRecords[relPath]
            val currentSha = if (!isDir) fileEngine.calculateSha256(file) else ""
            val diffState = when {
                saved == null -> DiffState.ADDED
                !isDir && saved.originalSha256.isNotEmpty() && saved.originalSha256 != currentSha -> DiffState.MODIFIED
                else -> DiffState.UNMODIFIED
            }

            FileNode(
                path = file.absolutePath,
                relativePath = relPath,
                name = file.name,
                sizeBytes = if (isDir) 0L else file.length(),
                isDirectory = isDir,
                modifiedTimestamp = file.lastModified(),
                permissions = saved?.permissions ?: if (isDir) "0755" else "0644",
                uid = saved?.uid ?: 0,
                gid = saved?.gid ?: 0,
                selinuxContext = saved?.selinuxContext ?: "u:object_r:system_file:s0",
                diffState = diffState,
                sha256 = currentSha
            )
        }

        return nodes.sortedWith(
            compareByDescending<FileNode> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }
}
