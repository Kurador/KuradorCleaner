package com.kurador.cleaner.engine

import java.util.Locale

enum class CleanModule(
    val id: String,
    val title: String,
    val description: String,
    val risky: Boolean = false
) {
    EMPTY_DIRS("empty_dirs", "Carpetas vacías", "Directorios huérfanos"),
    APP_CACHE("app_cache", "Caché apps", "Caché interno y externo"),
    DALVIK("dalvik", "Dalvik / ART", "Dalvik-cache", true),
    JUNK("junk", "Basura", "Logs, tmp y thumbnails"),
    FSTRIM("fstrim", "FSTRIM", "Reciclaje de espacio libre"),
    TEMP_FILES("temp_files", "Temp / logs", "Archivos temporales"),
    APK_DUPES("apk_dupes", "APK duplicados", "Detecta APK repetidos"),
    THUMB_CACHE("thumb_cache", "Miniaturas", "Caché de thumbnails")
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
