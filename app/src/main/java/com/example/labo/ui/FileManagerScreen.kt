package com.example.labo.ui

import android.os.Environment
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.ui.theme.LaboDarkBorder
import com.example.ui.theme.LaboStatusError
import com.example.ui.theme.LaboStatusSuccess
import com.example.ui.theme.LaboStatusWarning
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FileManagerScreen(
    projectEngine: ProjectEngine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableIntStateOf(0) } // 0: PROJET, 1: STOCKAGE
    val projects by projectEngine.getAllProjectsFlow().collectAsState(initial = emptyList())
    var currentOpenProject by remember { mutableStateOf<ProjectModel?>(null) }
    var projectFiles by remember { mutableStateOf<List<FileNode>>(emptyList()) }

    val context = LocalContext.current
    var currentStorageDir by remember {
        mutableStateOf(StorageUtils.getBrowsableRootDirectory(context))
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentOpenProject?.id) {
        currentOpenProject?.let { proj ->
            projectFiles = projectEngine.scanProjectDiff(proj.id)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentOpenProject != null) {
                        currentOpenProject = null
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
                    text = { Text("PROJET (Arborescence & Diff)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("fm_tab_project")
                )
                Tab(
                    selected = selectedOption == 1,
                    onClick = { selectedOption = 1 },
                    text = { Text("STOCKAGE (/sdcard)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("fm_tab_storage")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selectedOption == 0) {
            // PROJET TAB
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
                                    .clickable { currentOpenProject = proj },
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
                // INSIDE A PROJECT: Cartography & Diff Tree
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arborescence extraite (Cartographie)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Quick Test: Add a test modification file to see diff markers
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    currentOpenProject?.let { proj ->
                                        val extDir = projectEngine.getExtractedDirectory(proj.id)
                                        val testModified = File(extDir, "etc/custom_gsi_patch.conf")
                                        testModified.parentFile?.mkdirs()
                                        testModified.writeText("labo.patch.enabled=true\n")
                                        projectFiles = projectEngine.scanProjectDiff(proj.id)
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter fichier test (Diff)")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            items(projectFiles) { node ->
                                ProjectFileNodeRow(node)
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        } else {
            // STOCKAGE TAB (/sdcard)
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStorageDir.parentFile != null && currentStorageDir.path != "/sdcard") {
                        IconButton(
                            onClick = {
                                currentStorageDir.parentFile?.let { currentStorageDir = it }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Dossier parent")
                        }
                    }
                    Text(
                        text = currentStorageDir.absolutePath,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val storageFiles = remember(currentStorageDir) {
                    val files = currentStorageDir.listFiles()?.toList() ?: emptyList()
                    val dirs = files.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
                    val regFiles = files.filter { !it.isDirectory }.sortedBy { it.name.lowercase() }
                    dirs + regFiles
                }

                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(storageFiles) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (file.isDirectory) currentStorageDir = file
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (!file.isDirectory) {
                                        Text(
                                            text = "${file.length() / 1024} Ko",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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

@Composable
fun ProjectFileNodeRow(node: FileNode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.relativePath,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
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
                    text = "${node.permissions} • UID:${node.uid} • ${node.selinuxContext}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
