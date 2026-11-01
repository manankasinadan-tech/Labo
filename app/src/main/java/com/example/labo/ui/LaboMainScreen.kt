package com.example.labo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.labo.engine.ArtifactEngine
import com.example.labo.engine.CoreEngine
import com.example.labo.engine.ForgeImgEngine
import com.example.labo.engine.ProjectEngine
import com.example.labo.engine.SignerEngine
import com.example.labo.labx.LabxFramework
import com.example.labo.labx.LabxInstaller
import com.example.labo.model.LabxExtension
import com.example.ui.theme.ThemeMode

enum class RailDestination {
    ACCUEIL,
    ACTION,
    PARAMETRES,
    TERMINAL
}

@Composable
fun LaboMainScreen(
    coreEngine: CoreEngine,
    projectEngine: ProjectEngine,
    forgeEngine: ForgeImgEngine,
    signerEngine: SignerEngine,
    artifactEngine: ArtifactEngine,
    labxFramework: LabxFramework,
    labxInstaller: LabxInstaller,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currentAccentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTarget by remember { mutableStateOf(NavigationTarget.HOME) }
    var activeExtension by remember { mutableStateOf<LabxExtension?>(null) }
    var railExpanded by remember { mutableStateOf(false) }
    var terminalState by remember { mutableStateOf(TerminalDisplayState.MINIMIZED) }

    val activeTask by coreEngine.activeTask.collectAsState()

    // Handle back gesture to return to Home if inside a subscreen
    BackHandler(enabled = activeTarget != NavigationTarget.HOME) {
        activeTarget = NavigationTarget.HOME
        activeExtension = null
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Persistent Material 3 Navigation Rail
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .animateContentSize()
                        .width(if (railExpanded) 160.dp else 68.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Rail Expand/Collapse Toggle Button
                        IconButton(
                            onClick = { railExpanded = !railExpanded },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("rail_toggle_expand")
                        ) {
                            Icon(
                                imageVector = if (railExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                                contentDescription = "Déplier/Replier le rail",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Destination 1: Accueil
                        RailItem(
                            icon = Icons.Default.Home,
                            label = "Accueil",
                            selected = activeTarget == NavigationTarget.HOME,
                            expanded = railExpanded,
                            testTag = "rail_item_accueil",
                            onClick = {
                                activeTarget = NavigationTarget.HOME
                                activeExtension = null
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Destination 2: Action
                        RailItem(
                            icon = Icons.Default.PlayArrow,
                            label = "Action",
                            selected = activeTarget == NavigationTarget.ACTION,
                            expanded = railExpanded,
                            badgeCount = if (activeTask != null) 1 else 0,
                            testTag = "rail_item_action",
                            onClick = {
                                if (activeTask != null) {
                                    activeTarget = NavigationTarget.ACTION
                                } else {
                                    activeTarget = NavigationTarget.HOME
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Destination 3: Paramètres
                        RailItem(
                            icon = Icons.Default.Settings,
                            label = "Paramètres",
                            selected = activeTarget == NavigationTarget.SETTINGS,
                            expanded = railExpanded,
                            testTag = "rail_item_settings",
                            onClick = {
                                activeTarget = NavigationTarget.SETTINGS
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Destination 4: Terminal
                        RailItem(
                            icon = Icons.Default.Terminal,
                            label = "Terminal",
                            selected = terminalState != TerminalDisplayState.MINIMIZED,
                            expanded = railExpanded,
                            testTag = "rail_item_terminal",
                            onClick = {
                                terminalState = if (terminalState == TerminalDisplayState.MINIMIZED) {
                                    TerminalDisplayState.HALF
                                } else {
                                    TerminalDisplayState.MINIMIZED
                                }
                            }
                        )
                    }
                }

                // Main Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (activeTarget) {
                        NavigationTarget.HOME -> HomeScreen(
                            labxFramework = labxFramework,
                            onNavigate = { target, ext ->
                                activeTarget = target
                                activeExtension = ext
                            }
                        )
                        NavigationTarget.FORGE -> ForgeScreen(
                            forgeEngine = forgeEngine,
                            projectEngine = projectEngine,
                            onBack = { activeTarget = NavigationTarget.HOME },
                            onGoToAction = { activeTarget = NavigationTarget.ACTION }
                        )
                        NavigationTarget.FILE_MANAGER -> FileManagerScreen(
                            projectEngine = projectEngine,
                            onBack = { activeTarget = NavigationTarget.HOME }
                        )
                        NavigationTarget.SIGNALAB -> SignaLabScreen(
                            signerEngine = signerEngine,
                            projectEngine = projectEngine,
                            onBack = { activeTarget = NavigationTarget.HOME }
                        )
                        NavigationTarget.LABX_EXTENSION -> {
                            activeExtension?.let { ext ->
                                LabxExtensionScreen(
                                    extension = ext,
                                    projectEngine = projectEngine,
                                    artifactEngine = artifactEngine,
                                    onBack = { activeTarget = NavigationTarget.HOME }
                                )
                            } ?: run {
                                activeTarget = NavigationTarget.HOME
                            }
                        }
                        NavigationTarget.ACTION -> ActionScreen(
                            coreEngine = coreEngine,
                            onReturnHome = { activeTarget = NavigationTarget.HOME }
                        )
                        NavigationTarget.SETTINGS -> SettingsScreen(
                            coreEngine = coreEngine,
                            labxInstaller = labxInstaller,
                            currentThemeMode = currentThemeMode,
                            onThemeModeChange = onThemeModeChange,
                            currentAccentColor = currentAccentColor,
                            onAccentColorChange = onAccentColorChange,
                            onBack = { activeTarget = NavigationTarget.HOME }
                        )
                    }
                }
            }

            // Universal Terminal overlay docked at bottom
            UniversalTerminalView(
                coreEngine = coreEngine,
                displayState = terminalState,
                onStateChange = { terminalState = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    testTag: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = if (expanded) 12.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, RoundedCornerShape(4.dp))
                            .align(Alignment.TopEnd)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
