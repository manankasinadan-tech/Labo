package com.example.labo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.labo.engine.CoreEngine
import com.example.labo.model.LogLevel
import com.example.labo.model.TerminalLog
import com.example.ui.theme.LaboDarkBackground
import com.example.ui.theme.LaboDarkBorder
import com.example.ui.theme.LaboDarkSurface
import com.example.ui.theme.LaboStatusError
import com.example.ui.theme.LaboStatusInfo
import com.example.ui.theme.LaboStatusSuccess
import com.example.ui.theme.LaboStatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TerminalDisplayState {
    MINIMIZED,
    HALF,
    FULL
}

@Composable
fun UniversalTerminalView(
    coreEngine: CoreEngine,
    displayState: TerminalDisplayState,
    onStateChange: (TerminalDisplayState) -> Unit,
    modifier: Modifier = Modifier
) {
    val logs by coreEngine.logs.collectAsState()
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && displayState != TerminalDisplayState.MINIMIZED) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val heightFraction = when (displayState) {
        TerminalDisplayState.MINIMIZED -> 0.08f
        TerminalDisplayState.HALF -> 0.45f
        TerminalDisplayState.FULL -> 0.88f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
            .animateContentSize(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = LaboDarkBackground
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, LaboDarkBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStateChange(
                            when (displayState) {
                                TerminalDisplayState.MINIMIZED -> TerminalDisplayState.HALF
                                TerminalDisplayState.HALF -> TerminalDisplayState.MINIMIZED
                                TerminalDisplayState.FULL -> TerminalDisplayState.HALF
                            }
                        )
                    },
                color = LaboDarkSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Console Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TERMINAL UNIVERSEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${logs.size} logs)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick clear button
                        if (displayState != TerminalDisplayState.MINIMIZED) {
                            IconButton(
                                onClick = { coreEngine.clearLogs() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear logs",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Toggle Half / Full
                        if (displayState != TerminalDisplayState.MINIMIZED) {
                            IconButton(
                                onClick = {
                                    onStateChange(
                                        if (displayState == TerminalDisplayState.FULL) TerminalDisplayState.HALF
                                        else TerminalDisplayState.FULL
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (displayState == TerminalDisplayState.FULL) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle full size",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Minimize / Expand button
                        IconButton(
                            onClick = {
                                onStateChange(
                                    if (displayState == TerminalDisplayState.MINIMIZED) TerminalDisplayState.HALF
                                    else TerminalDisplayState.MINIMIZED
                                )
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (displayState == TerminalDisplayState.MINIMIZED) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Minimize or Expand",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Minimized preview single line
            if (displayState == TerminalDisplayState.MINIMIZED) {
                val lastLog = logs.lastOrNull()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastLog?.let { "[${it.engine}] ${it.message}" } ?: "Système prêt. Tapez 'help' pour la console.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                }
            } else {
                // Expanded Log List & Command Prompt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(LaboDarkBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "Aucun log pour le moment. En attente d'opérations...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(logs, key = { it.id }) { logItem ->
                                LogRowItem(logItem)
                            }
                        }
                    }
                }

                // Interactive Command Prompt Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LaboDarkSurface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_command_input"),
                        placeholder = {
                            Text(
                                "Commande (ex: help, status, engines, clear, root)...",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        },
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commandInput.isNotBlank()) {
                                coreEngine.executeTerminalCommand(commandInput)
                                commandInput = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = LaboDarkBorder,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                coreEngine.executeTerminalCommand(commandInput)
                                commandInput = ""
                            }
                        },
                        modifier = Modifier.testTag("terminal_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Command",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogRowItem(log: TerminalLog) {
    val timeStr = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    val (tagColor, tagBg) = when (log.level) {
        LogLevel.INFO -> LaboStatusInfo to LaboStatusInfo.copy(alpha = 0.15f)
        LogLevel.DEBUG -> Color.Gray to Color.Gray.copy(alpha = 0.15f)
        LogLevel.WARN -> LaboStatusWarning to LaboStatusWarning.copy(alpha = 0.15f)
        LogLevel.ERROR -> LaboStatusError to LaboStatusError.copy(alpha = 0.2f)
        LogLevel.SUCCESS -> LaboStatusSuccess to LaboStatusSuccess.copy(alpha = 0.2f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = Color.DarkGray,
            fontSize = 10.sp,
            modifier = Modifier.width(52.dp)
        )

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = tagBg,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = log.engine,
                style = MaterialTheme.typography.labelSmall,
                color = tagColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        Text(
            text = log.message,
            style = MaterialTheme.typography.labelSmall,
            color = when (log.level) {
                LogLevel.ERROR -> LaboStatusError
                LogLevel.WARN -> LaboStatusWarning
                LogLevel.SUCCESS -> LaboStatusSuccess
                else -> Color.LightGray
            },
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
