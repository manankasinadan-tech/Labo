package com.example.labo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.labo.engine.CoreEngine
import com.example.labo.labx.LabxInstaller
import com.example.ui.theme.LaboPrimaryAmber
import com.example.ui.theme.LaboPrimaryCyan
import com.example.ui.theme.LaboPrimaryEmerald
import com.example.ui.theme.LaboPrimaryRose
import com.example.ui.theme.LaboPrimarySlate
import com.example.ui.theme.LaboStatusSuccess
import com.example.ui.theme.ThemeMode
import java.io.File

@Composable
fun SettingsScreen(
    coreEngine: CoreEngine,
    labxInstaller: LabxInstaller,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currentAccentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRoot by coreEngine.isRootAvailable.collectAsState()
    var showLabxFilePicker by remember { mutableStateOf(false) }
    var labxInstallResult by remember { mutableStateOf<String?>(null) }
    var showDocumentationModal by remember { mutableStateOf(false) }
    var showChangelogModal by remember { mutableStateOf(false) }

    if (showLabxFilePicker) {
        FilePickerModal(
            title = "Sélectionner un paquet d'extension (.labx ou .zip)",
            fileFilterExtensions = listOf("labx", "zip"),
            onDismissRequest = { showLabxFilePicker = false },
            onFileSelected = { file ->
                showLabxFilePicker = false
                try {
                    val ext = labxInstaller.installExtensionPackage(file)
                    labxInstallResult = "Extension ${ext.manifest.name} v${ext.manifest.version} installée avec succès !"
                } catch (e: Exception) {
                    labxInstallResult = "Erreur installation : ${e.message}"
                }
            }
        )
    }

    // Interactive Bible LABX Documentation Modal
    if (showDocumentationModal) {
        AlertDialog(
            onDismissRequest = { showDocumentationModal = false },
            title = {
                Text(
                    text = "Bible Technique LABO & Création LABX",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(14.dp)) {
                        item {
                            Text(
                                text = "GUIDE DU DÉVELOPPEUR D'EXTENSIONS LABX",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Une extension LABX est un module autonome structuré capable d'intégrer ses propres moteurs, de communiquer avec le CoreEngine de LABO et de manipuler les partitions GSI déballées.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "1. Structure obligatoire du paquet (.labx) :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "mon_extension.labx/\n├── manifest.toml (Déclaration)\n├── config/ (Règles par défaut)\n├── bin/ (Outils/scripts)\n└── docs/ (Documentation Markdown)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "2. Spécification de manifest.toml :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = """
[extension]
id = "org.labo.labx.monoutil"
name = "Mon Outil GSI"
version = "1.0.0"
author = "Dev"
description = "Description de mon outil."
min_labo_api = 1

[engines]
provided = ["MonMoteur"]
dependencies = ["CoreEngine", "ForgeImgEngine"]

[permissions]
read_projects = true
modify_projects = true

[ui]
category = "Ingénierie"
accent_color = "#0EA5E9"
                                """.trimIndent(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "3. Rétrocompatibilité et Boot GSI :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• Permissions POSIX : Veillez à déclarer 0755 pour les exécutables dans /system/bin et 0644 pour les APKs.\n• Contextes SELinux : Tout fichier ajouté doit être labellisé avec u:object_r:system_file:s0 dans file_contexts.\n• AVB : En cas de re-signature, vérifiez le hashtree et vbmeta.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocumentationModal = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Changelog Modal
    if (showChangelogModal) {
        AlertDialog(
            onDismissRequest = { showChangelogModal = false },
            title = { Text("Changelog & Version LABO", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("LABO Workbench v1.0.0 (Release Initial)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Architecture en 11 moteurs autonomes avec bus réactif CoreEngine.")
                    Text("• Moteur FORGE : Unpack et Repack intelligent avec capture des configurations fs_config et file_contexts.")
                    Text("• Suite SIGNALAB : Forensique V1 à V4, Keymaker CloneSign, SignAPP, ROMsignchain et ROM Signer.")
                    Text("• Gestionnaire de fichiers avec cartographie de projet et marqueurs de diff visuels.")
                    Text("• Console Terminal Universelle interactive.")
                    Text("• Écosystème d'extensions LABX avec extension Deodexer & APK Optimizer incluse.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogModal = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "PARAMÈTRES",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Thèmes, Privilèges, Extensions LABX & Documentation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thème de l'interface", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        listOf(
                            ThemeMode.SYSTEM to "Suivant le système Android",
                            ThemeMode.DARK to "Sombre (Minimaliste ardoise)",
                            ThemeMode.LIGHT to "Clair (Épuré)"
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onThemeModeChange(mode) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentThemeMode == mode,
                                    onClick = { onThemeModeChange(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Accent Color Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Couleur d'accentuation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                LaboPrimarySlate to "Ardoise",
                                LaboPrimaryCyan to "Cyan",
                                LaboPrimaryEmerald to "Émeraude",
                                LaboPrimaryAmber to "Ambre",
                                LaboPrimaryRose to "Rose"
                            ).forEach { (color, name) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onAccentColorChange(color) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color, CircleShape)
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(name, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Privileges & Permissions Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accès & Privilèges Système", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Privilèges Superutilisateur (Root)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isRoot) "Root détecté et actif" else "Exécution standard non-root",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isRoot) LaboStatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { coreEngine.checkRootAccess() }
                            ) {
                                Text("Vérifier Root")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Espace de stockage externe", fontWeight = FontWeight.SemiBold)
                                Text("Accès autorisé : /sdcard/LABO", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = LaboStatusSuccess)
                        }
                    }
                }
            }

            // LABX Extension Installer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajouter une extension LABX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Installez un paquet .labx contenant de nouveaux moteurs, fonctionnalités ou interfaces.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showLabxFilePicker = true },
                            modifier = Modifier.fillMaxWidth().testTag("settings_install_labx_button")
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Parcourir et Installer une extension (.labx)")
                        }

                        labxInstallResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = res,
                                style = MaterialTheme.typography.bodySmall,
                                color = LaboStatusSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Documentation & Bible LABX
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Documentation & Base de Connaissances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showDocumentationModal = true },
                            modifier = Modifier.fillMaxWidth().testTag("settings_open_bible_button")
                        ) {
                            Text("Bible Complète : Comment créer les LABX")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showChangelogModal = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Documentation du Projet & Changelog")
                        }
                    }
                }
            }
        }
    }
}
