package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.labo.data.LaboDatabase
import com.example.labo.engine.AnalystEngine
import com.example.labo.engine.ArchiveEngine
import com.example.labo.engine.ArtifactEngine
import com.example.labo.engine.CoreEngine
import com.example.labo.engine.FileEngine
import com.example.labo.engine.FilesystemEngine
import com.example.labo.engine.ForgeImgEngine
import com.example.labo.engine.MappingEngine
import com.example.labo.engine.PartitionEngine
import com.example.labo.engine.ProjectEngine
import com.example.labo.engine.SignerEngine
import com.example.labo.labx.LabxFramework
import com.example.labo.labx.LabxInstaller
import com.example.labo.ui.LaboMainScreen
import com.example.ui.theme.LaboPrimarySlate
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database & Core Foundation
        val database = LaboDatabase.getInstance(this)
        val coreEngine = CoreEngine.getInstance(this)

        // Initialize 11 Specialized Engines
        val fileEngine = FileEngine(this, coreEngine)
        val projectEngine = ProjectEngine(this, coreEngine, fileEngine, database)
        val analystEngine = AnalystEngine(coreEngine, fileEngine)
        val filesystemEngine = FilesystemEngine(coreEngine)
        val partitionEngine = PartitionEngine(coreEngine)
        val archiveEngine = ArchiveEngine(coreEngine)
        val signerEngine = SignerEngine(this, coreEngine, fileEngine, projectEngine)
        val artifactEngine = ArtifactEngine(coreEngine)
        val mappingEngine = MappingEngine(coreEngine)
        val forgeEngine = ForgeImgEngine(this, coreEngine, fileEngine, projectEngine, analystEngine, filesystemEngine)

        // Initialize LABX Extensibility System
        val labxFramework = LabxFramework(this, coreEngine)
        val labxInstaller = LabxInstaller(coreEngine, archiveEngine, labxFramework)

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var accentColor by remember { mutableStateOf(LaboPrimarySlate) }

            MyApplicationTheme(
                themeMode = themeMode,
                accentColor = accentColor,
                useDynamicColor = false
            ) {
                LaboMainScreen(
                    coreEngine = coreEngine,
                    projectEngine = projectEngine,
                    forgeEngine = forgeEngine,
                    signerEngine = signerEngine,
                    artifactEngine = artifactEngine,
                    labxFramework = labxFramework,
                    labxInstaller = labxInstaller,
                    currentThemeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                    currentAccentColor = accentColor,
                    onAccentColorChange = { accentColor = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
