package com.kurador.cleaner.engine

import java.util.Locale

enum class CleanModule(
    val id: String,
    val title: String,
    val description: String,
    val risky: Boolean = false
) {
    EMPTY_DIRS("empty_dirs", "Carpetas vacías", "Carpetas vacías en /sdcard (excepto Android)"),
    APP_CACHE("app_cache", "Caché de la app", "Caché propia de Kurador Lite"),
    JUNK("junk", "Archivos .tmp", "Archivos .tmp de más de 1 día"),
    THUMB_CACHE("thumb_cache", "Miniaturas", "Miniaturas de la galería"),
    APK_DUPES("apk_dupes", "APK duplicados", "Solo detecta, no borra")
}

data class LogEntry(
    val time: String,
    val level: Level,
    val message: String
) {
    enum class Level { INFO, SUCCESS, WARN, ERROR, CMD }
}

data class ModuleResult(
    val module: CleanModule,
    val freedBytes: Long,
    val detail: String
)

fun formatBytes(bytes: Long): String = when {
    bytes >= (1L shl 30) -> String.format(Locale.US, "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= (1L shl 20) -> String.format(Locale.US, "%.1f MB", bytes / (1L shl 20).toDouble())
    bytes >= (1L shl 10) -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes > 0L -> "$bytes B"
    else -> "0 B"
}
