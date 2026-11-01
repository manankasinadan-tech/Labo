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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TextButton
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
import com.example.labo.engine.ProjectEngine
import com.example.labo.engine.SignerEngine
import com.example.labo.model.AppSignatureReport
import com.example.labo.model.KeyMakerConfig
import com.example.labo.model.KeyProfile
import com.example.labo.model.ProjectModel
import com.example.labo.model.RomSignChainReport
import com.example.ui.theme.LaboStatusSuccess
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.labo.util.StorageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignaLabScreen(
    signerEngine: SignerEngine,
    projectEngine: ProjectEngine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 vertical options:
    // 0: VerifyAPP, 1: Keymaker, 2: SIGNAPP, 3: ROMsignchain, 4: ROM SIGNER
    val context = LocalContext.current
    var currentOption by remember { mutableIntStateOf(0) }

    // File pickers
    var showApkPicker by remember { mutableStateOf(false) }
    var selectedApkPath by remember { mutableStateOf("") }
    var verificationReport by remember { mutableStateOf<AppSignatureReport?>(null) }

    // Keymaker state
    var keyAlias by remember { mutableStateOf("my_release_key") }
    var commonName by remember { mutableStateOf("Android Release") }
    var organization by remember { mutableStateOf("Custom Lab") }
    var showCloneChoiceDialog by remember { mutableStateOf(false) }
    var keyList by remember { mutableStateOf(signerEngine.getKeyProfiles()) }

    // SIGNAPP state
    var apkToSignPath by remember { mutableStateOf("") }
    var showSignApkPicker by remember { mutableStateOf(false) }
    var showKeyChoiceForSign by remember { mutableStateOf(false) }
    var signSuccessMessage by remember { mutableStateOf<String?>(null) }

    // ROMsignchain state
    val projects by projectEngine.getAllProjectsFlow().collectAsState(initial = emptyList())
    var selectedProjectForChain by remember { mutableStateOf<ProjectModel?>(null) }
    var chainReport by remember { mutableStateOf<RomSignChainReport?>(null) }

    // ROM SIGNER pipeline state
    var selectedProjectForRomSigner by remember { mutableStateOf<ProjectModel?>(null) }
    var isPipelineRunning by remember { mutableStateOf(false) }
    var pipelineProgress by remember { mutableStateOf(0f) }
    var pipelineStatus by remember { mutableStateOf("") }
    var pipelineOutput by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Modals
    if (showApkPicker) {
        FilePickerModal(
            title = "Sélectionner un APK à analyser",
            fileFilterExtensions = listOf("apk"),
            onDismissRequest = { showApkPicker = false },
            onFileSelected = { file ->
                selectedApkPath = file.absolutePath
                showApkPicker = false
            }
        )
    }

    if (showSignApkPicker) {
        FilePickerModal(
            title = "Sélectionner un APK à signer",
            fileFilterExtensions = listOf("apk"),
            onDismissRequest = { showSignApkPicker = false },
            onFileSelected = { file ->
                apkToSignPath = file.absolutePath
                showSignApkPicker = false
            }
        )
    }

    if (showCloneChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showCloneChoiceDialog = false },
            title = { Text("Créer une clé depuis VerifyAPP", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Choisissez le mode de reproduction pour répliquer la signature :",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cert = verificationReport?.certificates?.firstOrNull()
                                val newProfile = signerEngine.createKey(
                                    KeyMakerConfig(
                                        alias = "clone_${verificationReport?.packageName ?: "app"}",
                                        isCloneMode = true,
                                        sourceCertDN = cert?.subjectDN ?: "CN=ClonedApp"
                                    )
                                )
                                keyList = signerEngine.getKeyProfiles()
                                showCloneChoiceDialog = false
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("1. CloneSign (Recommandé)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Recrée une signature avec la même logique, structure, identité DN, type et taille de clé que l'application analysée.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cert = verificationReport?.certificates?.firstOrNull()
                                commonName = cert?.subjectDN?.substringAfter("CN=")?.substringBefore(",") ?: "Custom"
                                currentOption = 1 // Switch to Keymaker form
                                showCloneChoiceDialog = false
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("2. Custom", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                "Reprend la logique de la clé de base mais permet de saisir de nouvelles identités personnalisées.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloneChoiceDialog = false }) {
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
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("signalab_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "SIGNALAB",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Forensique, Keymaker, CloneSign & ROM Signer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vertical options switcher
        TabRow(
            selectedTabIndex = currentOption,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            val tabs = listOf("VerifyAPP", "Keymaker", "SIGNAPP", "ROMsignchain", "ROM SIGNER")
            tabs.forEachIndexed { idx, name ->
                Tab(
                    selected = currentOption == idx,
                    onClick = { currentOption = idx },
                    text = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (currentOption) {
                0 -> {
                    // VERIFYAPP SECTION
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "VerifyAPP — Vérification Forensique de Signature",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = selectedApkPath,
                                    onValueChange = { selectedApkPath = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Fichier APK à analyser") },
                                    placeholder = { Text("/sdcard/LABO/app.apk") },
                                    trailingIcon = {
                                        IconButton(onClick = { showApkPicker = true }) {
                                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Parcourir")
                                        }
                                    },
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick demo apk creator button
                                OutlinedButton(
                                    onClick = {
                                        val sampleDir = StorageUtils.getLaboDirectory(context)
                                        val sampleApk = File(sampleDir, "SystemCore_demo.apk")
                                        if (!sampleApk.exists()) {
                                            StorageUtils.safeWriteBytes(sampleApk, ByteArray(1024 * 64) { 0x50 })
                                        }
                                        selectedApkPath = sampleApk.absolutePath
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Utiliser APK exemple de démonstration")
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (selectedApkPath.isNotBlank()) {
                                            verificationReport = signerEngine.verifyAppSignature(File(selectedApkPath))
                                        }
                                    },
                                    enabled = selectedApkPath.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VERIFY (Lancer l'audit forensique)")
                                }
                            }
                        }
                    }

                    // Verification Report Volets
                    verificationReport?.let { report ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Rapport Forensique : ${report.apkName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Button(
                                            onClick = { showCloneChoiceDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Create from Verify", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Schémas actifs : ${report.signingSchemeDetails}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Évaluation de sécurité : ${report.securityAssessment}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    report.certificates.forEachIndexed { i, cert ->
                                        Text(
                                            text = "Certificat X.509 #${i + 1} (${cert.publicKeyAlgorithm} ${cert.keySize} bits)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Sujet DN : ${cert.subjectDN}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Validité : ${cert.notBefore} au ${cert.notAfter}",
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "SHA-256 : ${cert.sha256Fingerprint}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // KEYMAKER SECTION
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Keymaker — Générateur de Clés & Signatures",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = keyAlias,
                                    onValueChange = { keyAlias = it },
                                    label = { Text("Alias de la clé") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = commonName,
                                    onValueChange = { commonName = it },
                                    label = { Text("Common Name (CN)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = organization,
                                    onValueChange = { organization = it },
                                    label = { Text("Organization (O)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        signerEngine.createKey(
                                            KeyMakerConfig(
                                                alias = keyAlias,
                                                commonName = commonName,
                                                organization = organization
                                            )
                                        )
                                        keyList = signerEngine.getKeyProfiles()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Create, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Créer le set de clés")
                                }
                            }
                        }
                    }

                    // Saved key profiles list
                    item {
                        Text(
                            text = "TROUSSEAU DE CLÉS DISPONIBLES (${keyList.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(keyList) { key ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = key.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (key.isClone) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "CloneSign",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "DN : ${key.certInfo.subjectDN}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Fingerprint : ${key.certInfo.sha256Fingerprint.take(24)}...",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // SIGNAPP SECTION
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "SignAPP — Re-signature d'Applications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = apkToSignPath,
                                    onValueChange = { apkToSignPath = it },
                                    label = { Text("Application APK à signer") },
                                    trailingIcon = {
                                        IconButton(onClick = { showSignApkPicker = true }) {
                                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Parcourir")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val targetKey = keyList.firstOrNull() ?: signerEngine.getKeyProfiles().first()
                                        val signed = signerEngine.signApk(File(apkToSignPath), targetKey)
                                        signSuccessMessage = "Application signée avec succès : ${signed.absolutePath}"
                                    },
                                    enabled = apkToSignPath.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SIGN (Signer l'application)")
                                }
                            }
                        }
                    }

                    signSuccessMessage?.let { msg ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LaboStatusSuccess.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // ROMsignchain SECTION
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ROMsignchain — Chaîne de Confiance Système",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Analyse et extrait la logique de signature, les groupes de clés croisées et les hashes de chaque composant de l'image déballée.",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (projects.isEmpty()) {
                                    Text(
                                        text = "Aucun projet déballé disponible. Déballez d'abord un GSI dans FORGE.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    projects.forEach { proj ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedProjectForChain = proj }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedProjectForChain?.id == proj.id,
                                                onClick = { selectedProjectForChain = proj }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(proj.name, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            val p = selectedProjectForChain ?: return@Button
                                            coroutineScope.launch {
                                                chainReport = signerEngine.runRomSignChain(p.id)
                                            }
                                        },
                                        enabled = selectedProjectForChain != null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("LANCER ROMsignchain")
                                    }
                                }
                            }
                        }
                    }

                    chainReport?.let { report ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Rapport ROMsignchain : ${report.romName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("APKs scannés : ${report.totalApksScanned}")
                                    Text("Groupes de signature détectés : ${report.signatureGroups.size}")
                                    Text("Statut AVB : ${report.avbTrustStatus}", color = LaboStatusSuccess)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    report.signatureGroups.forEach { group ->
                                        Text(group.name, fontWeight = FontWeight.Bold)
                                        Text("Fingerprint : ${group.certFingerprint.take(24)}...", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("Membres : ${group.memberApks.joinToString()}", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // ROM SIGNER PIPELINE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ROM SIGNER — Pipeline Intégral",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Exécute le pipeline autonome complet : Unpack / Chargement -> Analyse ROMsignchain -> Réplication des clés avec CloneSign -> Re-signature globale -> Mise à jour permissions et SELinux -> Repack -> Export vers /sdcard/LABO/ROMSIGN/.",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (projects.isEmpty()) {
                                    Text(
                                        text = "Aucun projet déballé disponible. Déballez une image dans FORGE pour démarrer.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    projects.forEach { proj ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedProjectForRomSigner = proj }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedProjectForRomSigner?.id == proj.id,
                                                onClick = { selectedProjectForRomSigner = proj }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(proj.name, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            val proj = selectedProjectForRomSigner ?: return@Button
                                            isPipelineRunning = true
                                            pipelineOutput = null
                                            coroutineScope.launch {
                                                try {
                                                    val result = signerEngine.runRomSignerPipeline(proj.id) { prog, status ->
                                                        pipelineProgress = prog
                                                        pipelineStatus = status
                                                    }
                                                    pipelineOutput = "Image GSI re-signée : ${result.absolutePath}"
                                                } catch (e: Exception) {
                                                    pipelineStatus = "Erreur : ${e.message}"
                                                } finally {
                                                    isPipelineRunning = false
                                                }
                                            }
                                        },
                                        enabled = selectedProjectForRomSigner != null && !isPipelineRunning,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("DÉMARRER LE PIPELINE ROM SIGNER")
                                    }
                                }
                            }
                        }
                    }

                    if (isPipelineRunning) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = pipelineStatus, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { pipelineProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    pipelineOutput?.let { out ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LaboStatusSuccess.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LaboStatusSuccess)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LaboStatusSuccess)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(out, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
