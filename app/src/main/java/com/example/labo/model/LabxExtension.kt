package com.example.labo.model

data class LabxPermissions(
    val readProjects: Boolean = true,
    val modifyProjects: Boolean = false,
    val runTerminalCommands: Boolean = false,
    val requireRoot: Boolean = false
)

data class LabxManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val minLaboApi: Int = 1,
    val providedEngines: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val permissions: LabxPermissions = LabxPermissions(),
    val category: String = "Utility",
    val accentColorHex: String = "#0EA5E9",
    val entryPoint: String = "main"
)

data class LabxExtension(
    val manifest: LabxManifest,
    val installDirectory: String,
    val isInstalled: Boolean = true,
    val isEnabled: Boolean = true,
    val isBuiltin: Boolean = false
)
