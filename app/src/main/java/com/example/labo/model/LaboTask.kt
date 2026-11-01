package com.example.labo.model

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class LaboTask(
    val id: String,
    val title: String,
    val description: String,
    val engineName: String,
    val progress: Float = 0f,
    val status: TaskStatus = TaskStatus.PENDING,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val resultSummary: String? = null,
    val errorMessage: String? = null
)
