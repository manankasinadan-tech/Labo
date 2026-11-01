package com.example.labo.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.labo.engine.ProjectEngine
import com.example.labo.model.DiffState
import com.example.labo.model.FileNode
import com.example.labo.model.ProjectModel
import com.example.labo.util.StorageUtils
import com.example.ui.theme.LaboStatusError
import com.example.ui.theme.LaboStatusSuccess
import com.example.ui.theme.LaboStatusWarning
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileManagerScreen(
    projectEngine: ProjectEngine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedOption by remember { mutableIntStateOf(0) } // 0: PROJET, 1: STOCKAGE
    val projects by projectEngine.getAllProjectsFlow().collectAsState(initial = emptyList())
    var currentOpenProject by remember { mutableStateOf<ProjectModel?>(null) }

    // Project view states
    var projectSubFolder by remember { mutableStateOf("") }
    var projectFolderNodes by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    var projectDiffNodes by remember { mutableStateOf<List<FileNode>>(emptyList()) }
    var viewMode by remember { mutableIntStateOf(0) } // 0: Navigation par dossiers, 1: Diff global

    // Storage view states
    var currentStorageDir by remember {
        mutableStateOf(StorageUtils.getLaboDirectory(context))
    }
    var storageRefreshTrigger by remember { mutableIntStateOf(0) }
    var selectedFileDetails by remember { mutableStateOf<File?>(null) }
    var allFilesAccessGranted by remember { mutableStateOf(StorageUtils.isAllFilesAccessGranted()) }

    // SAF Picker Launcher
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val copied = StorageUtils.copyUriToLocalCache(context, it)
                if (copied != null) {
                    currentStorageDir = copied.parentFile ?: currentStorageDir
                    storageRefreshTrigger++
                }
            }
        }
    }

    // Refresh project nodes when open project or subfolder changes
    LaunchedEffect(currentOpenProject?.id, projectSubFolder, viewMode) {
        currentOpenProject?.let { proj ->
            if (viewMode == 0) {
                projectFolderNodes = projectEngine.getProjectFolderContents(proj.id, projectSubFolder)
            } else {
                projectDiffNodes = projectEngine.scanProjectDiff(proj.id)
            }
        }
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
            IconButton(
                onClick = {
                    if (currentOpenProject != null) {
                        if (projectSubFolder.isNotEmpty()) {
                            // Go to parent subfolder
                            projectSubFolder = if (projectSubFolder.contains('/')) {
                                projectSubFolder.substringBeforeLast('/')
                            } else {
                                ""
                            }
                        } else {
                            currentOpenProject = null
                        }
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.testTag("file_manager_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "GESTIONNAIRE DE FICHIERS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (currentOpenProject != null) "Projet : ${currentOpenProject?.name}" else "Exploration Projets & Stockage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (currentOpenProject == null) {
            TabRow(
                selectedTabIndex = selectedOption,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedOption == 0,
                    onClick = { selectedOption = 0 },
                    text = { Text("PROJETS DÉBALLÉS", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("fm_tab_project")
                )
                Tab(
                    selected = selectedOption == 1,
                    onClick = {
                        selectedOption = 1
                        allFilesAccessGranted = StorageUtils.isAllFilesAccessGranted()
                    },
                    text = { Text("STOCKAGE PHYSIQUE", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("fm_tab_storage")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selectedOption == 0) {
            // ==========================================
            // PROJET TAB
            // ==========================================
            if (currentOpenProject == null) {
                // List of Unpacked Projects
                if (projects.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucun projet déballé.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Utilisez la fonction FORGE pour déballer une image GSI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(projects) { proj ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("project_item_${proj.id}")
                                    .clickable {
                                        currentOpenProject = proj
                                        projectSubFolder = ""
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = proj.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${proj.targetPartition.name} • ${proj.filesystem.name} • ${proj.totalFiles} fichiers",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // INSIDE A PROJECT: Interactive Folder Exploration & Cartography
                val currentProj = currentOpenProject!!
                Column(modifier = Modifier.fillMaxSize()) {
                    // Navigation Breadcrumb Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (projectSubFolder.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    projectSubFolder = if (projectSubFolder.contains('/')) {
                                        projectSubFolder.substringBeforeLast('/')
                                    } else {
                                        ""
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Dossier parent")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (projectSubFolder.isEmpty()) "extracted/ (Racine)" else "extracted/$projectSubFolder",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            Text(
                                text = "Dossiers prioritaires en haut • Fichiers en dessous",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // View mode toggle chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = viewMode == 0,
                                onClick = { viewMode = 0 },
                                label = { Text("Dossiers", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = viewMode == 1,
                                onClick = { viewMode = 1 },
                                label = { Text("Diff Global", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action: Add test modification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val extDir = projectEngine.getExtractedDirectory(currentProj.id)
                                    val testModified = File(extDir, "etc/custom_gsi_patch.conf")
                                    testModified.parentFile?.mkdirs()
                                    testModified.writeText("labo.patch.enabled=true\nro.custom.version=2026.1\n")
                                    if (viewMode == 0) {
                                        projectFolderNodes = projectEngine.getProjectFolderContents(currentProj.id, projectSubFolder)
                                    } else {
                                        projectDiffNodes = projectEngine.scanProjectDiff(currentProj.id)
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter fichier test (Diff)", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val activeDisplayNodes = if (viewMode == 0) projectFolderNodes else projectDiffNodes

                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (activeDisplayNodes.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ce dossier est vide.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                items(activeDisplayNodes) { node ->
                                    ProjectFileNodeRow(
                                        node = node,
                                        onFolderClick = { folderNode ->
                                            if (folderNode.isDirectory) {
                                                projectSubFolder = folderNode.relativePath
                                            }
                                        }
                                    )
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // STOCKAGE PHYSIQUE TAB (/sdcard, LABO, etc.)
            // ==========================================
            Column(modifier = Modifier.fillMaxSize()) {
                // Android 11+ Permission Notice Banner
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !allFilesAccessGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = LaboStatusWarning.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaboStatusWarning)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = LaboStatusWarning, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Accès aux fichiers restreint par Android",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = LaboStatusWarning
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "À la racine /sdcard, Android masque les fichiers non multimédias (.img, .bin) sans l'autorisation 'Accès à tous les fichiers'. De plus, Android ne stocke par défaut que des dossiers à la racine.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val intent = StorageUtils.getAllFilesAccessSettingsIntent(context)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LaboStatusWarning),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Autoriser dans les Paramètres", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }
                }

                // Storage Shortcuts Row
                val shortcuts = remember { StorageUtils.getStorageShortcuts(context) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shortcuts.forEach { shortcut ->
                        FilterChip(
                            selected = currentStorageDir.absolutePath == shortcut.dir.absolutePath,
                            onClick = {
                                currentStorageDir = shortcut.dir
                                storageRefreshTrigger++
                            },
                            label = { Text(shortcut.label, fontSize = 11.sp) },
                            colors = if (shortcut.isPrimary) FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current path breadcrumb & parent navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStorageDir.parentFile != null && currentStorageDir.path != "/sdcard") {
                        IconButton(
                            onClick = {
                                currentStorageDir.parentFile?.let { currentStorageDir = it }
                                storageRefreshTrigger++
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Dossier parent")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = currentStorageDir.absolutePath,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { storageRefreshTrigger++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualiser", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Storage Action Buttons (SAF System Picker + Generate Sample File)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            safPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sélecteur Système (SAF)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val laboDir = StorageUtils.createSampleLaboFiles(context)
                            currentStorageDir = laboDir
                            storageRefreshTrigger++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Créer fichier test LABO", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Storage File Listing with strictly DOSSIERS D'ABORD (Directories on top)
                val storageFiles = remember(currentStorageDir, storageRefreshTrigger) {
                    val files = currentStorageDir.listFiles()?.toList() ?: emptyList()
                    val dirs = files.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
                    val regFiles = files.filter { !it.isDirectory }.sortedBy { it.name.lowercase() }
                    dirs + regFiles
                }

                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (storageFiles.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun fichier ou dossier dans ce répertoire.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cliquez sur 'Créer fichier test LABO' ou utilisez les raccourcis au-dessus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(storageFiles) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (file.isDirectory) {
                                                currentStorageDir = file
                                                storageRefreshTrigger++
                                            } else {
                                                selectedFileDetails = file
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = if (file.isDirectory) "Dossier" else "${file.length() / 1024} Ko • ${file.extension.uppercase().ifEmpty { "FICHIER" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal when a storage file is clicked
    selectedFileDetails?.let { file ->
        val dateFormatted = remember(file) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
        }
        AlertDialog(
            onDismissRequest = { selectedFileDetails = null },
            title = {
                Text(text = file.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column {
                    Text(text = "Chemin absolu :", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(text = file.absolutePath, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Taille : ${file.length()} octets (${file.length() / 1024} Ko)", fontSize = 12.sp)
                    Text(text = "Dernière modification : $dateFormatted", fontSize = 12.sp)
                    Text(text = "Permissions : ${if (file.canRead()) "r" else "-"}${if (file.canWrite()) "w" else "-"}${if (file.canExecute()) "x" else "-"}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFileDetails = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun ProjectFileNodeRow(
    node: FileNode,
    onFolderClick: (FileNode) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDirectory) {
                    onFolderClick(node)
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with diff badge or Red Cross for deleted
        if (node.diffState == DiffState.DELETED) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fichier supprimé",
                tint = LaboStatusError,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.relativePath,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (node.isDirectory) FontWeight.Bold else FontWeight.Medium,
                    color = if (node.diffState == DiffState.DELETED) LaboStatusError else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Diff State Badges
                when (node.diffState) {
                    DiffState.MODIFIED -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LaboStatusWarning.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "MODIFIÉ",
                                style = MaterialTheme.typography.labelSmall,
                                color = LaboStatusWarning,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    DiffState.ADDED -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LaboStatusSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "AJOUTÉ",
                                style = MaterialTheme.typography.labelSmall,
                                color = LaboStatusSuccess,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    DiffState.DELETED -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LaboStatusError.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "SUPPRIMÉ",
                                style = MaterialTheme.typography.labelSmall,
                                color = LaboStatusError,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    DiffState.UNMODIFIED -> {}
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${node.permissions} • UID:${node.uid} • ${node.selinuxContext}${if (!node.isDirectory) " • ${node.sizeBytes / 1024} Ko" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
