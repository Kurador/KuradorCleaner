package com.kurador.cleaner.ui.dashboard

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kurador.cleaner.core.RootEnvironment
import com.kurador.cleaner.core.RootProvider
import com.kurador.cleaner.engine.CleanBus
import com.kurador.cleaner.engine.CleanModule
import com.kurador.cleaner.engine.LogEntry
import com.kurador.cleaner.engine.ModuleResult
import com.kurador.cleaner.service.CleanService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val rootGranted: Boolean? = null,
    val rootProvider: RootProvider = RootProvider.NONE,
    val running: Boolean = false,
    val progress: Float = 0f,
    val currentModule: String? = null,
    val logs: List<LogEntry> = emptyList(),
    val results: List<ModuleResult> = emptyList(),
    val totalFreed: Long = 0L,
    val enabledModules: Set<CleanModule> = CleanModule.entries.toSet()
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshRootStatus()
        bind(CleanBus.running) { copy(running = it) }
        bind(CleanBus.progress) { copy(progress = it) }
        bind(CleanBus.currentModule) { copy(currentModule = it) }
        bind(CleanBus.logs) { copy(logs = it) }
        bind(CleanBus.results) { copy(results = it) }
        bind(CleanBus.totalFreed) { copy(totalFreed = it) }
    }

    private fun <T> bind(flow: Flow<T>, map: DashboardUiState.(T) -> DashboardUiState) {
        viewModelScope.launch {
            flow.collect { value -> _uiState.update { it.map(value) } }
        }
    }

    fun refreshRootStatus() = viewModelScope.launch {
        val info = RootEnvironment.detect()
        _uiState.update { it.copy(rootGranted = info.granted, rootProvider = info.provider) }
    }

    fun toggleModule(module: CleanModule) = _uiState.update { state ->
        state.copy(
            enabledModules = if (module in state.enabledModules)
                state.enabledModules - module
            else
                state.enabledModules + module
        )
    }

    fun startExpressClean() {
        val state = _uiState.value
        if (state.running) return
        if (state.rootGranted != true) {
            refreshRootStatus()
            return
        }
        CleanBus.reset()
        CleanBus.log("Iniciando limpieza express", LogEntry.Level.CMD)
        val context = getApplication<Application>()
        val intent = Intent(context, CleanService::class.java).putExtra(
            CleanService.EXTRA_MODULES,
            state.enabledModules.map { it.id }.toTypedArray()
        )
        context.startForegroundService(intent)
    }
}
