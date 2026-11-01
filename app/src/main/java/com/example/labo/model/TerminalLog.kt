package com.example.labo.model

enum class LogLevel {
    INFO,
    DEBUG,
    WARN,
    ERROR,
    SUCCESS
}

data class TerminalLog(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val engine: String,
    val message: String
)
