package com.kurador.cleaner.engine

import com.kurador.cleaner.core.ShellManager

class CleanerEngine(private val shell: ShellManager = ShellManager) {

    suspend fun runModules(modules: Set<CleanModule>) {
        val ordered = CleanModule.entries.filter { it in modules }
        if (ordered.isEmpty()) {
            CleanBus.log("No hay módulos seleccionados.", LogEntry.Level.WARN)
            return
        }

        ordered.forEachIndexed { index, module ->
            CleanBus.setCurrentModule(module.title)
            CleanBus.log("── Módulo: ${module.title} ──", LogEntry.Level.CMD)

            val result = runCatching {
                when (module) {
                    CleanModule.EMPTY_DIRS -> cleanEmptyDirs()
                    CleanModule.APP_CACHE -> cleanAppCache()
                    CleanModule.DALVIK -> cleanDalvik()
                    CleanModule.JUNK -> cleanJunk()
                    CleanModule.FSTRIM -> runFstrim()
                    CleanModule.TEMP_FILES -> cleanTempFiles()
                    CleanModule.APK_DUPES -> cleanApkDuplicates()
                    CleanModule.THUMB_CACHE -> cleanThumbnails()
                }
            }.getOrElse { e ->
                CleanBus.log("Error en ${module.title}: ${e.message}", LogEntry.Level.ERROR)
                ModuleResult(module, 0L, "Error: ${e.message}")
            }

            CleanBus.addResult(result)
            CleanBus.log("${module.title} → liberado: ${formatBytes(result.freedBytes)}", LogEntry.Level.SUCCESS)
            CleanBus.setProgress((index + 1) / ordered.size.toFloat())
        }

        CleanBus.setCurrentModule(null)
    }

    private suspend fun cleanEmptyDirs(): ModuleResult {
        val base = "find /sdcard -mindepth 1 -depth -type d -empty -not -path '/sdcard/Android*' 2>/dev/null"
        val count = shell.exec("$base | wc -l").out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
        if (count == 0) return ModuleResult(CleanModule.EMPTY_DIRS, 0L, "Sin carpetas vacías")
        shell.exec("$base -exec rmdir {} + 2>/dev/null")
        return ModuleResult(CleanModule.EMPTY_DIRS, count * 4096L, "$count carpetas eliminadas")
    }

    private suspend fun cleanAppCache(): ModuleResult {
        val globs = "/data/user/0/*/cache /data/user_de/0/*/cache /sdcard/Android/data/*/cache"
        val before = ShellManager.parseDuKb(shell.exec("du -sk $globs 2>/dev/null"))
        shell.exec("for c in $globs; do [ -d \"\$c\" ] && rm -rf \"\$c\"/* 2>/dev/null; done")
        shell.exec("pm trim-caches 999999999999 2>/dev/null")
        return ModuleResult(CleanModule.APP_CACHE, before, "Caché interno + externo purgado")
    }

    private suspend fun cleanDalvik(): ModuleResult {
        val before = ShellManager.parseDuKb(shell.exec("du -sk /data/dalvik-cache /cache/dalvik-cache 2>/dev/null"))
        shell.exec("rm -rf /data/dalvik-cache/* /cache/dalvik-cache/* 2>/dev/null")
        return ModuleResult(CleanModule.DALVIK, before, "Recompilación ART diferida")
    }

    private suspend fun cleanJunk(): ModuleResult {
        val paths = "/data/log /data/tombstones /data/anr /data/system/dropbox /data/vendor/log /data/vendor/tombstones"
        var freed = ShellManager.parseDuKb(shell.exec("du -sk $paths 2>/dev/null"))
        shell.exec("for p in $paths; do [ -d \"\$p\" ] && rm -rf \"\$p\"/* 2>/dev/null; done")

        freed += ShellManager.parseDuKb(
            shell.exec("du -sk /sdcard/DCIM/.thumbnails /sdcard/Pictures/.thumbnails 2>/dev/null")
        )
        val tmpCount = shell.exec(
            "find /sdcard -type f -name '*.tmp' -mtime +0 -not -path '*/Android/*' 2>/dev/null | wc -l"
        ).out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
        shell.exec("find /sdcard -type f -name '*.tmp' -mtime +0 -not -path '*/Android/*' -delete 2>/dev/null")

        return ModuleResult(CleanModule.JUNK, freed, "Logs y miniaturas purgados; $tmpCount tmp eliminados")
    }

    private suspend fun runFstrim(): ModuleResult {
        var total = 0L
        for (target in listOf("/data", "/cache", "/system", "/vendor", "/metadata")) {
            val output = shell.exec("fstrim -v $target 2>&1").combinedOutput
            val match = Regex("""([\d,]+)\s+bytes trimmed""").find(output)
            total += match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
        }
        return ModuleResult(CleanModule.FSTRIM, total, "Celdas UFS recicladas")
    }

    private suspend fun cleanTempFiles(): ModuleResult {
        val before = ShellManager.parseDuKb(shell.exec("du -sk /data/local/tmp 2>/dev/null"))
        shell.exec("rm -rf /data/local/tmp/* 2>/dev/null")
        return ModuleResult(CleanModule.TEMP_FILES, before, "Directorios temporales vaciados")
    }

    private suspend fun cleanApkDuplicates(): ModuleResult {
        val cmd = "find /sdcard -type f -iname '*.apk' 2>/dev/null | while read -r f; do " +
            "sha256sum \"\$f\" 2>/dev/null | awk '{print \$1}'; done | sort | uniq -d | wc -l"
        val count = shell.exec(cmd).out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
        return ModuleResult(CleanModule.APK_DUPES, 0L, "Grupos de APK duplicados: $count")
    }

    private suspend fun cleanThumbnails(): ModuleResult {
        val before = ShellManager.parseDuKb(
            shell.exec("du -sk /sdcard/DCIM/.thumbnails /sdcard/Pictures/.thumbnails 2>/dev/null")
        )
        shell.exec("rm -rf /sdcard/DCIM/.thumbnails /sdcard/Pictures/.thumbnails 2>/dev/null")
        return ModuleResult(CleanModule.THUMB_CACHE, before, "Miniaturas purgadas")
    }
}
