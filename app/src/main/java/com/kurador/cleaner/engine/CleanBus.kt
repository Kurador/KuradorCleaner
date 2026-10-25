package com.kurador.cleaner.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CleanBus {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private const val MAX_LOG_LINES = 500

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _currentModule = MutableStateFlow<String?>(null)
    val currentModule = _currentModule.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _results = MutableStateFlow<List<ModuleResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _totalFreed = MutableStateFlow(0L)
    val totalFreed = _totalFreed.asStateFlow()

    fun reset() {
        _logs.value = emptyList()
        _results.value = emptyList()
        _totalFreed.value = 0L
        _progress.value = 0f
        _currentModule.value = null
    }

    fun setRunning(value: Boolean) { _running.value = value }
    fun setProgress(value: Float) { _progress.value = value.coerceIn(0f, 1f) }
    fun setCurrentModule(name: String?) { _currentModule.value = name }

    fun log(message: String, level: LogEntry.Level = LogEntry.Level.INFO) {
        val entry = LogEntry(timeFormat.format(Date()), level, message)
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_LINES)
    }

    fun addResult(result: ModuleResult) {
        _results.value = _results.value + result
        _totalFreed.value = _totalFreed.value + result.freedBytes
    }
}
