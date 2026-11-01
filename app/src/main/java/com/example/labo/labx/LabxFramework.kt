package com.example.labo.labx

import android.content.Context
import com.example.labo.engine.CoreEngine
import com.example.labo.model.LabxExtension
import com.example.labo.model.LabxManifest
import com.example.labo.model.LabxPermissions
import com.example.labo.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class LabxFramework(
    private val context: Context,
    private val coreEngine: CoreEngine
) {
    val extensionsDir: File by lazy {
        File(context.filesDir, "extensions").apply { mkdirs() }
    }

    private val _installedExtensions = MutableStateFlow<List<LabxExtension>>(emptyList())
    val installedExtensions: StateFlow<List<LabxExtension>> = _installedExtensions.asStateFlow()

    init {
        coreEngine.log(LogLevel.INFO, "LABX", "Initialisation de LABX Framework v1.0.0...")
        registerBuiltinExtensions()
    }

    private fun registerBuiltinExtensions() {
        // Builtin pre-installed reference extension: Deodexer & APK Optimizer
        val deodexerManifest = LabxManifest(
            id = "org.labo.labx.deodexer",
            name = "Deodexer & APK Optimizer",
            version = "1.0.0",
            author = "LABO Core Team",
            description = "Analyse et fusionne les artefacts .vdex/.odex pour rendre les APKs de la GSI universellement portables.",
            minLaboApi = 1,
            providedEngines = listOf("DeodexEngine"),
            dependencies = listOf("CoreEngine", "ProjectEngine", "FileEngine", "ArtifactEngine"),
            permissions = LabxPermissions(readProjects = true, modifyProjects = true),
            category = "Optimization",
            accentColorHex = "#10B981",
            entryPoint = "deodex_dashboard"
        )

        val deodexerDir = File(extensionsDir, deodexerManifest.id).apply { mkdirs() }

        val builtin = LabxExtension(
            manifest = deodexerManifest,
            installDirectory = deodexerDir.absolutePath,
            isInstalled = true,
            isEnabled = true,
            isBuiltin = true
        )

        _installedExtensions.update { current ->
            if (current.any { it.manifest.id == builtin.manifest.id }) current else current + builtin
        }

        coreEngine.log(LogLevel.SUCCESS, "LABX", "Extension préinstallée active : [${deodexerManifest.name}]")
    }

    fun registerExtension(extension: LabxExtension) {
        _installedExtensions.update { current ->
            current.filter { it.manifest.id != extension.manifest.id } + extension
        }
        coreEngine.log(LogLevel.SUCCESS, "LABX", "Extension LABX enregistrée : [${extension.manifest.name}] v${extension.manifest.version}")
    }

    fun uninstallExtension(extensionId: String) {
        val ext = _installedExtensions.value.firstOrNull { it.manifest.id == extensionId } ?: return
        if (ext.isBuiltin) {
            coreEngine.log(LogLevel.WARN, "LABX", "Impossible de désinstaller une extension intégrée.")
            return
        }

        val dir = File(ext.installDirectory)
        if (dir.exists()) dir.deleteRecursively()

        _installedExtensions.update { current ->
            current.filter { it.manifest.id != extensionId }
        }
        coreEngine.log(LogLevel.INFO, "LABX", "Extension désinstallée : [${ext.manifest.name}]")
    }
}
