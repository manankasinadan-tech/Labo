package com.example.labo.engine

import com.example.labo.model.LogLevel
import java.io.File

data class DependencyLink(
    val sourceComponent: String,
    val targetDependency: String,
    val type: String // library, permission, shared_uid
)

data class ImpactAnalysisResult(
    val affectedComponents: List<String>,
    val bootRiskLevel: String, // LOW, MEDIUM, CRITICAL
    val recommendation: String
)

class MappingEngine(
    private val coreEngine: CoreEngine
) : BaseEngine {
    override val name: String = "MappingEngine"
    override val description: String = "Moteur de cartographie des composants, des dépendances et d'analyse d'impact"
    override val isReady: Boolean = true

    init {
        coreEngine.registerEngine(this)
    }

    fun buildDependencyMap(extractedDir: File): List<DependencyLink> {
        coreEngine.log(LogLevel.INFO, name, "Génération de la cartographie des dépendances système...")
        val links = mutableListOf<DependencyLink>()

        // System core mappings
        links.add(DependencyLink("SystemLaboCore.apk", "libc++.so", "native_library"))
        links.add(DependencyLink("SystemLaboCore.apk", "android.permission.INTERNET", "permission"))
        links.add(DependencyLink("framework.jar", "core-libart.jar", "bootclasspath"))

        coreEngine.log(LogLevel.SUCCESS, name, "${links.size} liaisons de dépendances cartographiées.")
        return links
    }

    fun evaluateImpact(modifiedOrDeletedPath: String): ImpactAnalysisResult {
        val lower = modifiedOrDeletedPath.lowercase()
        return when {
            lower.contains("build.prop") -> {
                ImpactAnalysisResult(
                    affectedComponents = listOf("init", "zygote", "SystemServer"),
                    bootRiskLevel = "CRITICAL",
                    recommendation = "Préserver impérativement les propriétés ro.build.flavor et ro.treble.enabled pour le boot GSI."
                )
            }
            lower.contains("framework") -> {
                ImpactAnalysisResult(
                    affectedComponents = listOf("Android Runtime", "Services système"),
                    bootRiskLevel = "CRITICAL",
                    recommendation = "Régénérer les artefacts ART et vérifier le bootclasspath."
                )
            }
            lower.contains("bin/") || lower.contains("xbin/") -> {
                ImpactAnalysisResult(
                    affectedComponents = listOf("Scripts init", "Démons de démarrage"),
                    bootRiskLevel = "MEDIUM",
                    recommendation = "Vérifier les permissions 0755 et le contexte SELinux u:object_r:system_file:s0."
                )
            }
            else -> {
                ImpactAnalysisResult(
                    affectedComponents = listOf("Application cible"),
                    bootRiskLevel = "LOW",
                    recommendation = "Signature et permissions adaptées automatiquement lors du Repack."
                )
            }
        }
    }
}
