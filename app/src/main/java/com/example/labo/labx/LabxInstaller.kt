package com.example.labo.labx

import com.example.labo.engine.ArchiveEngine
import com.example.labo.engine.CoreEngine
import com.example.labo.model.LabxExtension
import com.example.labo.model.LabxManifest
import com.example.labo.model.LabxPermissions
import com.example.labo.model.LogLevel
import java.io.File

class LabxInstaller(
    private val coreEngine: CoreEngine,
    private val archiveEngine: ArchiveEngine,
    private val labxFramework: LabxFramework
) {
    fun installExtensionPackage(packageFile: File): LabxExtension {
        coreEngine.log(LogLevel.INFO, "LABXInstaller", "Installation du paquet d'extension : ${packageFile.name}...")

        if (!packageFile.exists()) {
            throw IllegalArgumentException("Fichier d'extension introuvable : ${packageFile.absolutePath}")
        }

        val tempExtractDir = File(labxFramework.extensionsDir, "temp_${System.currentTimeMillis()}").apply { mkdirs() }

        try {
            archiveEngine.extractZip(packageFile, tempExtractDir)

            val manifestFile = File(tempExtractDir, "manifest.toml")
            val manifest = if (manifestFile.exists()) {
                parseManifestToml(manifestFile)
            } else {
                // Check if wrapped in subfolder
                val nestedManifest = tempExtractDir.walkTopDown().firstOrNull { it.name == "manifest.toml" }
                if (nestedManifest != null) parseManifestToml(nestedManifest)
                else {
                    // Fallback manifest if simple script archive
                    LabxManifest(
                        id = "org.labo.labx.${packageFile.nameWithoutExtension.lowercase().replace("[^a-z0-9]".toRegex(), "")}",
                        name = packageFile.nameWithoutExtension,
                        version = "1.0.0",
                        author = "User Installed",
                        description = "Extension LABX installée manuellement."
                    )
                }
            }

            // Move to final destination: extensionsDir/{id}
            val finalDir = File(labxFramework.extensionsDir, manifest.id)
            if (finalDir.exists()) finalDir.deleteRecursively()
            tempExtractDir.renameTo(finalDir)

            val extension = LabxExtension(
                manifest = manifest,
                installDirectory = finalDir.absolutePath,
                isInstalled = true,
                isEnabled = true,
                isBuiltin = false
            )

            labxFramework.registerExtension(extension)
            coreEngine.log(LogLevel.SUCCESS, "LABXInstaller", "Installation réussie de [${manifest.name}] v${manifest.version}")
            return extension
        } catch (e: Exception) {
            coreEngine.log(LogLevel.ERROR, "LABXInstaller", "Erreur lors de l'installation de l'extension : ${e.message}")
            tempExtractDir.deleteRecursively()
            throw e
        }
    }

    private fun parseManifestToml(file: File): LabxManifest {
        var id = "unknown"
        var name = file.parentFile?.name ?: "Unknown Extension"
        var version = "1.0.0"
        var author = "Community"
        var desc = "LABX Extension"
        var minApi = 1
        var category = "Tools"
        var accentColor = "#0EA5E9"
        var entryPoint = "main"

        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val key = trimmed.substringBefore("=").trim().lowercase()
                val value = trimmed.substringAfter("=").trim().replace("\"", "").replace("'", "")
                when (key) {
                    "id" -> id = value
                    "name" -> name = value
                    "version" -> version = value
                    "author" -> author = value
                    "description" -> desc = value
                    "min_labo_api" -> minApi = value.toIntOrNull() ?: 1
                    "category" -> category = value
                    "accent_color" -> accentColor = value
                    "entry_point" -> entryPoint = value
                }
            }
        }

        return LabxManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = desc,
            minLaboApi = minApi,
            category = category,
            accentColorHex = accentColor,
            entryPoint = entryPoint
        )
    }
}
