package com.example.labo.engine

import android.content.Context
import com.example.labo.model.LaboTask
import com.example.labo.model.LogLevel
import com.example.labo.model.TaskStatus
import com.example.labo.model.TerminalLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface BaseEngine {
    val name: String
    val description: String
    val isReady: Boolean
}

class CoreEngine private constructor(val context: Context) : BaseEngine {
    override val name: String = "CoreEngine"
    override val description: String = "Orchestrateur central, Task Manager et Bus d'événements de LABO"
    override val isReady: Boolean = true

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Log Stream for Universal Terminal
    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    // Tasks Management
    private val _tasks = MutableStateFlow<List<LaboTask>>(emptyList())
    val tasks: StateFlow<List<LaboTask>> = _tasks.asStateFlow()

    private val _activeTask = MutableStateFlow<LaboTask?>(null)
    val activeTask: StateFlow<LaboTask?> = _activeTask.asStateFlow()

    // Registered Engines
    private val _registeredEngines = MutableStateFlow<Map<String, BaseEngine>>(emptyMap())
    val registeredEngines: StateFlow<Map<String, BaseEngine>> = _registeredEngines.asStateFlow()

    // Root status
    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    init {
        log(LogLevel.INFO, name, "Initialisation du CoreEngine LABO...")
        log(LogLevel.SUCCESS, name, "Système modulaire et bus réactif prêts.")
        checkRootAccess()
    }

    fun registerEngine(engine: BaseEngine) {
        _registeredEngines.update { current ->
            current + (engine.name to engine)
        }
        log(LogLevel.DEBUG, name, "Moteur enregistré : [${engine.name}] - ${engine.description}")
    }

    fun log(level: LogLevel, engine: String, message: String) {
        val entry = TerminalLog(
            level = level,
            engine = engine,
            message = message
        )
        _logs.update { current ->
            // Keep last 400 logs to optimize memory
            if (current.size > 400) current.drop(1) + entry else current + entry
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log(LogLevel.INFO, name, "Journal du terminal réinitialisé.")
    }

    fun createTask(title: String, description: String, engineName: String): LaboTask {
        val task = LaboTask(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            engineName = engineName,
            status = TaskStatus.RUNNING,
            progress = 0f
        )
        _tasks.update { it + task }
        _activeTask.value = task
        log(LogLevel.INFO, engineName, "Démarrage de la tâche : $title")
        return task
    }

    fun updateTaskProgress(taskId: String, progress: Float, statusMessage: String? = null) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    val updated = task.copy(progress = progress.coerceIn(0f, 1f))
                    if (_activeTask.value?.id == taskId) {
                        _activeTask.value = updated
                    }
                    updated
                } else task
            }
        }
        if (statusMessage != null) {
            log(LogLevel.DEBUG, "TaskManager", statusMessage)
        }
    }

    fun completeTask(taskId: String, summary: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    val completed = task.copy(
                        progress = 1f,
                        status = TaskStatus.COMPLETED,
                        completedAt = System.currentTimeMillis(),
                        resultSummary = summary
                    )
                    if (_activeTask.value?.id == taskId) {
                        _activeTask.value = completed
                    }
                    completed
                } else task
            }
        }
        log(LogLevel.SUCCESS, "TaskManager", "Tâche terminée avec succès : $summary")
    }

    fun failTask(taskId: String, errorMsg: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    val failed = task.copy(
                        status = TaskStatus.FAILED,
                        completedAt = System.currentTimeMillis(),
                        errorMessage = errorMsg
                    )
                    if (_activeTask.value?.id == taskId) {
                        _activeTask.value = failed
                    }
                    failed
                } else task
            }
        }
        log(LogLevel.ERROR, "TaskManager", "Échec de la tâche : $errorMsg")
    }

    fun cancelActiveTask() {
        val current = _activeTask.value ?: return
        _tasks.update { list ->
            list.map { task ->
                if (task.id == current.id) {
                    task.copy(
                        status = TaskStatus.CANCELLED,
                        completedAt = System.currentTimeMillis(),
                        errorMessage = "Annulé par l'utilisateur"
                    )
                } else task
            }
        }
        _activeTask.value = null
        log(LogLevel.WARN, "TaskManager", "Tâche annulée : ${current.title}")
    }

    fun checkRootAccess() {
        scope.launch(Dispatchers.IO) {
            val rootPossible = File("/system/xbin/su").exists() ||
                    File("/system/bin/su").exists() ||
                    File("/sbin/su").exists() ||
                    File("/data/local/xbin/su").exists()
            _isRootAvailable.value = rootPossible
            if (rootPossible) {
                log(LogLevel.INFO, name, "Privilèges Root détectés sur le système hôte.")
            } else {
                log(LogLevel.DEBUG, name, "Mode utilisateur standard (non-root).")
            }
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        log(LogLevel.INFO, "USER", "> $trimmed")

        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0].lowercase(Locale.ROOT)

        when (command) {
            "help" -> {
                log(LogLevel.INFO, "Terminal", "Commandes disponibles :")
                log(LogLevel.INFO, "Terminal", "  help       - Afficher cette aide")
                log(LogLevel.INFO, "Terminal", "  status     - État général de LABO et des moteurs")
                log(LogLevel.INFO, "Terminal", "  engines    - Liste de tous les moteurs enregistrés")
                log(LogLevel.INFO, "Terminal", "  tasks      - Liste des tâches actives et passées")
                log(LogLevel.INFO, "Terminal", "  clear      - Effacer le journal")
                log(LogLevel.INFO, "Terminal", "  root       - Vérifier les autorisations superutilisateur")
                log(LogLevel.INFO, "Terminal", "  storage    - Vérifier les espaces de travail et de sortie")
                log(LogLevel.INFO, "Terminal", "  date       - Afficher la date et l'heure système")
            }
            "status" -> {
                val enginesCount = _registeredEngines.value.size
                val tasksCount = _tasks.value.size
                val active = _activeTask.value?.title ?: "Aucune action en cours"
                log(LogLevel.SUCCESS, "Terminal", "LABO Workbench v1.0.0 [OK]")
                log(LogLevel.INFO, "Terminal", "Moteurs actifs : $enginesCount")
                log(LogLevel.INFO, "Terminal", "Action en cours : $active")
                log(LogLevel.INFO, "Terminal", "Historique tâches : $tasksCount")
                log(LogLevel.INFO, "Terminal", "Statut root : ${if (_isRootAvailable.value) "Actif" else "Non détecté"}")
            }
            "engines" -> {
                log(LogLevel.INFO, "Terminal", "Moteurs LABO enregistrés :")
                _registeredEngines.value.values.forEach { engine ->
                    log(LogLevel.INFO, "Terminal", "  • [${engine.name}] : ${engine.description}")
                }
            }
            "tasks" -> {
                val current = _tasks.value
                if (current.isEmpty()) {
                    log(LogLevel.INFO, "Terminal", "Aucune tâche enregistrée.")
                } else {
                    current.takeLast(10).forEach { t ->
                        log(LogLevel.INFO, "Terminal", "  [${t.status}] ${t.title} (${(t.progress * 100).toInt()}%)")
                    }
                }
            }
            "clear" -> {
                clearLogs()
            }
            "root" -> {
                checkRootAccess()
                log(LogLevel.INFO, "Terminal", "Vérification root en cours...")
            }
            "storage" -> {
                val internal = context.filesDir.absolutePath
                val exportDir = com.example.labo.util.StorageUtils.getLaboDirectory(context).absolutePath
                log(LogLevel.INFO, "Terminal", "Espace de travail isolé : $internal/workspace")
                log(LogLevel.INFO, "Terminal", "Espace d'exportation : $exportDir")
            }
            "date" -> {
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                log(LogLevel.INFO, "Terminal", "Date système : $dateStr")
            }
            else -> {
                log(LogLevel.WARN, "Terminal", "Commande inconnue : '$command'. Tapez 'help' pour la liste.")
            }
        }
    }

    companion object {
        @Volatile
        private var instance: CoreEngine? = null

        fun getInstance(context: Context): CoreEngine {
            return instance ?: synchronized(this) {
                instance ?: CoreEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
