package com.kurador.cleaner.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShellResult(
    val code: Int,
    val out: List<String>,
    val err: List<String>
) {
    val isSuccess: Boolean get() = code == 0
    val combinedOutput: String get() = (out + err).joinToString("\n").trim()
}

object ShellManager {
    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        runCatching {
            val result = Shell.cmd(command).exec()
            ShellResult(result.code, result.out.toList(), result.err.toList())
        }.getOrElse { e ->
            ShellResult(-1, emptyList(), listOf(e.message ?: "Error desconocido"))
        }
    }

    fun parseDuKb(result: ShellResult): Long {
        val totalKb = result.out.sumOf { line ->
            line.trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull() ?: 0L
        }
        return totalKb * 1024L
    }
}
