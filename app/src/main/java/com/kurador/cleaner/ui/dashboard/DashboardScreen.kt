package com.kurador.cleaner.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurador.cleaner.engine.CleanModule
import com.kurador.cleaner.engine.LogEntry
import com.kurador.cleaner.engine.formatBytes
import com.kurador.cleaner.ui.theme.DeepSpace
import com.kurador.cleaner.ui.theme.GridLine
import com.kurador.cleaner.ui.theme.NeonCyan
import com.kurador.cleaner.ui.theme.NeonGreen
import com.kurador.cleaner.ui.theme.NeonPurple
import com.kurador.cleaner.ui.theme.NeonRed
import com.kurador.cleaner.ui.theme.NeonYellow
import com.kurador.cleaner.ui.theme.PanelCard
import com.kurador.cleaner.ui.theme.TextPrimary
import com.kurador.cleaner.ui.theme.TextSecondary
import com.kurador.cleaner.ui.theme.TrackDark

@Composable
fun DashboardScreen(
    onRequestAllFiles: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "KURADOR LITE",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(text = "Sin root • solo permisos de app", color = TextSecondary, fontSize = 10.sp)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { Hero(state.totalFreed, state.progress, state.running, state.currentModule) }
            item {
                Button(
                    onClick = {
                        onRequestAllFiles()
                        vm.startExpressClean()
                    },
                    enabled = !state.running,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF00363D),
                        disabledContainerColor = PanelCard,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (state.running) "LIMPIEZA EN CURSO…" else "LIMPIEZA EXPRESS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
            item { Stats(state.enabledModules.size, state.totalFreed) }
            item {
                SectionPanel("MÓDULOS DE LIMPIEZA") {
                    CleanModule.entries.forEach { module ->
                        ModuleRow(
                            module = module,
                            checked = module in state.enabledModules,
                            enabled = !state.running,
                            onToggle = { vm.toggleModule(module) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
            if (state.results.isNotEmpty()) {
                item {
                    SectionPanel("RESULTADOS") {
                        state.results.forEach { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = r.module.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = r.detail, color = TextSecondary, fontSize = 9.sp)
                                }
                                Text(text = formatBytes(r.freedBytes), color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                SectionPanel("LOG") {
                    if (state.logs.isEmpty()) {
                        Text(text = "Esperando actividad del motor…", color = TextSecondary, fontSize = 10.sp)
                    }
                    state.logs.forEach { entry ->
                        val color = when (entry.level) {
                            LogEntry.Level.INFO -> TextSecondary
                            LogEntry.Level.SUCCESS -> NeonGreen
                            LogEntry.Level.WARN -> NeonYellow
                            LogEntry.Level.ERROR -> NeonRed
                            LogEntry.Level.CMD -> NeonCyan
                        }
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(text = "[${entry.time}]", color = color.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = entry.message, color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(totalFreed: Long, progress: Float, running: Boolean, currentModule: String?) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(220.dp)) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(200.dp),
            color = TrackDark,
            strokeWidth = 14.dp
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(200.dp),
            color = if (running) NeonCyan else NeonGreen,
            strokeWidth = 14.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = formatBytes(totalFreed), color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(text = "LIBERADOS", color = TextSecondary, fontSize = 10.sp, letterSpacing = 3.sp)
            currentModule?.let {
                Text(text = it, color = NeonCyan, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Stats(modulesCount: Int, totalFreed: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("MÓDULOS", "$modulesCount", NeonCyan, Modifier.weight(1f))
        StatCard("TOTAL", formatBytes(totalFreed), NeonPurple, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, accent: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(PanelCard)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Text(text = title, color = TextSecondary, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModuleRow(module: CleanModule, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val accent = if (checked) NeonCyan else TextSecondary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = if (checked) 0.8f else 0.3f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onToggle() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (checked) NeonGreen else TextSecondary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = module.title,
                color = if (checked) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = module.description, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectionPanel(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelCard)
            .border(1.dp, GridLine, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}
