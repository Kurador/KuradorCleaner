package com.kurador.cleaner.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RootProvider { NONE, MAGISK, KERNELSU, UNKNOWN }

data class RootInfo(
    val granted: Boolean,
    val provider: RootProvider,
    val modules: List<String>
)

object RootEnvironment {
    suspend fun detect(): RootInfo = withContext(Dispatchers.IO) {
        // Abrir la shell primero: aquí es donde KernelSU/Magisk pide el permiso
        val shell = runCatching { Shell.getShell() }.getOrNull()
        val granted = shell != null && shell.isRoot
        if (!granted) {
            return@withContext RootInfo(false, RootProvider.NONE, emptyList())
        }

        val ksu = runCatching {
            Shell.cmd("test -d /data/adb/ksu && echo ksu || echo no").exec()
        }.getOrNull()

        val magisk = runCatching {
            Shell.cmd("test -d /data/adb/modules && echo magisk || echo no").exec()
        }.getOrNull()

        val modules = runCatching {
            Shell.cmd("ls -1 /data/adb/modules 2>/dev/null || true").exec()
        }.getOrNull()?.out?.filter { it.isNotBlank() }.orEmpty()

        val provider = when {
            ksu?.out?.any { it.trim() == "ksu" } == true -> RootProvider.KERNELSU
            magisk?.out?.any { it.trim() == "magisk" } == true -> RootProvider.MAGISK
            modules.isNotEmpty() -> RootProvider.MAGISK
            else -> RootProvider.UNKNOWN
        }

        RootInfo(true, provider, modules.distinct())
    }
}
