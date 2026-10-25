package com.kurador.cleaner.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurador.cleaner.core.RootProvider
import com.kurador.cleaner.engine.CleanModule
import com.kurador.cleaner.engine.LogEntry
import com.kurador.cleaner.engine.formatBytes
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF05070D), Color(0xFF0A0F1A), Color(0xFF05070D)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Header(state)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    HeroSection(state = state, onScan = {
                        onRequestAllFiles()
                        vm.startExpressClean()
                    })
                }
                item { StatusTiles(state) }
                item { ModuleSelector(state = state, onToggle = vm::toggleModule) }
                if (state.results.isNotEmpty()) {
                    item { ResultsPanel(state) }
                }
                item { LogPanel(state) }
            }
        }
    }
}

@Composable
private fun Header(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "KURADOR CLEANER",
                color = NeonCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "KernelSU / Magisk • Android 16 • HyperOS 3",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        RootBadge(rootGranted = state.rootGranted, provider = state.rootProvider)
    }
}

@Composable
private fun RootBadge(rootGranted: Boolean?, provider: RootProvider) {
    val (label, color) = when {
        rootGranted == true && provider == RootProvider.KERNELSU -> "KERNELSU" to NeonGreen
        rootGranted == true && provider == RootProvider.MAGISK -> "MAGISK" to NeonGreen
        rootGranted == true -> "ROOT OK" to NeonGreen
        rootGranted == false -> "ROOT REQ" to NeonRed
        else -> "VERIFYING" to NeonYellow
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PanelCard)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeroSection(state: DashboardUiState, onScan: () -> Unit) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(210.dp),
                color = TrackDark,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(210.dp),
                color = if (state.running) NeonCyan else NeonGreen,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatBytes(state.totalFreed),
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(text = "LIBERADOS", color = TextSecondary, fontSize = 10.sp, letterSpacing = 3.sp)
                state.currentModule?.let {
                    Text(text = it, color = NeonCyan, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onScan,
            enabled = !state.running && state.rootGranted == true,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(
                    1.dp,
                    if (state.running) GridLine else NeonCyan.copy(alpha = 0.6f),
                    RoundedCornerShape(30.dp)
                ),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color(0xFF00363D),
                disabledContainerColor = PanelCard,
                disabledContentColor = TextSecondary
            )
        ) {
            Icon(
                imageVector = if (state.running) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (state.running) "LIMPIEZA EN CURSO…" else "ESCANEO Y LIMPIEZA EXPRESS",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        if (state.rootGranted == false) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Concede acceso root en KernelSU / Magisk y pulsa otra vez",
                color = NeonRed,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusTiles(state: DashboardUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            title = "ROOT",
            value = if (state.rootGranted == true) "ACTIVO" else "PENDIENTE",
            accent = if (state.rootGranted == true) NeonGreen else NeonRed
        )
        StatCard(title = "MÓDULOS", value = "${state.enabledModules.size}", accent = NeonCyan)
        StatCard(title = "TOTAL", value = formatBytes(state.totalFreed), accent = NeonPurple)
    }
}

@Composable
private fun StatCard(title: String, value: String, accent: Color) {
    Column(
        modifier = Modifier
            .width(110.dp)
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
private fun ModuleSelector(state: DashboardUiState, onToggle: (CleanModule) -> Unit) {
    SectionPanel("MÓDULOS DE LIMPIEZA") {
        CleanModule.entries.forEach { module ->
            ModuleRow(
                module = module,
                checked = module in state.enabledModules,
                enabled = !state.running,
                onToggle = { onToggle(module) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
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
        if (module.risky) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Precaución",
                tint = NeonYellow,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ResultsPanel(state: DashboardUiState) {
    SectionPanel("RESULTADOS") {
        state.results.forEach { result ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonGreen))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = result.module.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = result.detail, color = TextSecondary, fontSize = 9.sp)
                }
                Text(text = formatBytes(result.freedBytes), color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LogPanel(state: DashboardUiState) {
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
                Text(
                    text = "[${entry.time}]",
                    color = color.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = entry.message, color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
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
