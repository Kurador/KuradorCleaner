package com.kurador.cleaner.engine

import java.io.File
import java.security.MessageDigest

class CleanerEngine(private val appCacheDirs: List<File>) {

    private val sdcard = File("/sdcard")
    private val androidDir = File("/sdcard/Android")

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
                    CleanModule.JUNK -> cleanTmpFiles()
                    CleanModule.THUMB_CACHE -> cleanThumbnails()
                    CleanModule.APK_DUPES -> findApkDuplicates()
                }
            }.getOrElse { e ->
                CleanBus.log("Error en ${module.title}: ${e.message}", LogEntry.Level.ERROR)
                ModuleResult(module, 0L, "Error: ${e.message}")
            }

            CleanBus.addResult(result)
            CleanBus.log("${module.title} → ${result.detail}", LogEntry.Level.SUCCESS)
            CleanBus.setProgress((index + 1) / ordered.size.toFloat())
        }

        CleanBus.setCurrentModule(null)
    }

    private fun isInAndroidDir(f: File): Boolean = f.path.startsWith(androidDir.path)

    private fun dirSize(f: File): Long = when {
        !f.exists() -> 0L
        f.isFile -> f.length()
        else -> f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun cleanEmptyDirs(): ModuleResult {
        var count = 0
        sdcard.walkBottomUp()
            .filter { it.isDirectory && it != sdcard && !isInAndroidDir(it) }
            .forEach { dir ->
                if (dir.listFiles()?.isEmpty() == true && dir.delete()) count++
            }
        return ModuleResult(CleanModule.EMPTY_DIRS, 0L, "$count carpetas vacías eliminadas")
    }

    private fun cleanAppCache(): ModuleResult {
        var freed = 0L
        appCacheDirs.forEach { dir ->
            freed += dirSize(dir)
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
        return ModuleResult(CleanModule.APP_CACHE, freed, "Caché propia borrada")
    }

    private fun cleanTmpFiles(): ModuleResult {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        var freed = 0L
        var count = 0
        sdcard.walkTopDown()
            .onEnter { !isInAndroidDir(it) }
            .filter { it.isFile && it.name.endsWith(".tmp", ignoreCase = true) && it.lastModified() < cutoff }
            .forEach { f ->
                val size = f.length()
                if (f.delete()) {
                    freed += size
                    count++
                }
            }
        return ModuleResult(CleanModule.JUNK, freed, "$count archivos .tmp eliminados")
    }

    private fun cleanThumbnails(): ModuleResult {
        val dirs = listOf(File(sdcard, "DCIM/.thumbnails"), File(sdcard, "Pictures/.thumbnails"))
        var freed = 0L
        dirs.forEach { d ->
            freed += dirSize(d)
            d.deleteRecursively()
        }
        return ModuleResult(CleanModule.THUMB_CACHE, freed, "Miniaturas eliminadas")
    }

    private fun findApkDuplicates(): ModuleResult {
        val apks = sdcard.walkTopDown()
            .onEnter { !isInAndroidDir(it) }
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            .toList()

        var groupsFound = 0
        var reclaimable = 0L
        apks.groupBy { it.length() }
            .filterValues { it.size > 1 }
            .values
            .forEach { sameSize ->
                sameSize.groupBy { sha256(it) }
                    .values
                    .filter { it.size > 1 }
                    .forEach { dupes ->
                        groupsFound++
                        reclaimable += dupes.drop(1).sumOf { it.length() }
                    }
            }
        return ModuleResult(
            CleanModule.APK_DUPES,
            0L,
            "$groupsFound grupos duplicados, ${formatBytes(reclaimable)} recuperables (no borrado)"
        )
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(8192)
            var n = input.read(buf)
            while (n > 0) {
                md.update(buf, 0, n)
                n = input.read(buf)
            }
        }
        return md.digest().joinToString("") { String.format("%02x", it.toInt() and 0xff) }
    }
}
