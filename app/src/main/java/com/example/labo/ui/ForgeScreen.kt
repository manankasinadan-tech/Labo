package com.example.labo.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.labo.engine.ForgeImgEngine
import com.example.labo.engine.ProjectEngine
import com.example.labo.model.ProjectModel
import com.example.labo.util.StorageUtils
import com.example.ui.theme.LaboStatusSuccess
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ForgeScreen(
    forgeEngine: ForgeImgEngine,
    projectEngine: ProjectEngine,
    onBack: () -> Unit,
    onGoToAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: UNPACK, 1: REPACK
    var selectedImagePath by remember { mutableStateOf("") }
    var showFilePicker by remember { mutableStateOf(false) }

    val projects by projectEngine.getAllProjectsFlow().collectAsState(initial = emptyList())
    var selectedProjectForRepack by remember { mutableStateOf<ProjectModel?>(null) }
    var repackDropdownExpanded by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0f) }
    var completedResultPath by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    if (showFilePicker) {
        FilePickerModal(
            title = "Sélectionner une image GSI / partition",
            fileFilterExtensions = listOf("img", "raw", "sparse", "bin"),
            onDismissRequest = { showFilePicker = false },
            onFileSelected = { file ->
                selectedImagePath = file.absolutePath
                showFilePicker = false
            }
        )
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
                onClick = onBack,
                modifier = Modifier.testTag("forge_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "FORGE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Unpack & Repack intelligent d'images GSI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: Unpack vs Repack
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("UNPACK (Déballage)", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("forge_tab_unpack")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("REPACK (Reconstitution)", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("forge_tab_repack")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // UNPACK SECTION
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "1. Sélectionner l'image source",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = selectedImagePath,
                                onValueChange = { selectedImagePath = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forge_image_path_input"),
                                label = { Text("Chemin de l'image GSI (.img)") },
                                placeholder = { Text("/sdcard/LABO/system.img") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showFilePicker = true },
                                        modifier = Modifier.testTag("forge_open_picker_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Parcourir"
                                        )
                                    }
                                },
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Helper button to generate a ready-to-test sample image
                            OutlinedButton(
                                onClick = {
                                    val sampleDir = StorageUtils.getLaboDirectory(context)
                                    val sampleFile = File(sampleDir, "gsi_system_sample.img")
                                    if (!sampleFile.exists()) {
                                        StorageUtils.safeWriteBytes(sampleFile, ByteArray(1024 * 512) { 0x00 })
                                    }
                                    selectedImagePath = sampleFile.absolutePath
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Générer une image de test GSI (Demo)")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (selectedImagePath.isNotBlank()) {
                                        isProcessing = true
                                        completedResultPath = null
                                        coroutineScope.launch {
                                            try {
                                                val proj = forgeEngine.unpackImage(File(selectedImagePath)) { prog, msg ->
                                                    progressValue = prog
                                                    progressStatus = msg
                                                }
                                                completedResultPath = "Projet créé : ${proj.name} (${proj.totalFiles} fichiers extraits)"
                                            } catch (e: Exception) {
                                                progressStatus = "Erreur : ${e.message}"
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    }
                                },
                                enabled = selectedImagePath.isNotBlank() && !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forge_launch_unpack_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LANCER Unpack (Créer un Projet)")
                            }
                        }
                    }
                }
            } else {
                // REPACK SECTION
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "1. Sélectionner le projet à compiler",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (projects.isEmpty()) {
                                Text(
                                    text = "Aucun projet déballé disponible. Effectuez d'abord un Unpack.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    projects.forEach { proj ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedProjectForRepack = proj }
                                                .testTag("forge_repack_select_${proj.id}"),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selectedProjectForRepack?.id == proj.id) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selectedProjectForRepack?.id == proj.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selectedProjectForRepack?.id == proj.id,
                                                    onClick = { selectedProjectForRepack = proj }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = proj.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "${proj.targetPartition.name} • ${proj.filesystem.name} • ${proj.totalFiles} fichiers",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val proj = selectedProjectForRepack ?: return@Button
                                        isProcessing = true
                                        completedResultPath = null
                                        coroutineScope.launch {
                                            try {
                                                val out = forgeEngine.repackProject(proj.id) { prog, msg ->
                                                    progressValue = prog
                                                    progressStatus = msg
                                                }
                                                completedResultPath = "Image exportée : ${out.absolutePath}"
                                            } catch (e: Exception) {
                                                progressStatus = "Erreur : ${e.message}"
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    enabled = selectedProjectForRepack != null && !isProcessing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("forge_launch_repack_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("REPACK Projet (Reconstruire GSI)")
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Progress Card
            if (isProcessing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = progressStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(progressValue * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressValue },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Completed Result Banner
            completedResultPath?.let { result ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LaboStatusSuccess.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaboStatusSuccess)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LaboStatusSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
