package com.example.wirelessadb

import java.io.DataOutputStream

/**
 * Thin wrapper around the `su` binary.
 *
 * Every command batch runs in ONE `su` shell so the user only sees a single
 * root prompt. The shell is always closed with `exit`.
 */
object RootManager {

    data class RootResult(
        val success: Boolean,
        val output: String,
        val exitCode: Int
    )

    /** Runs `su -c id` and checks whether we actually got uid=0. */
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            code == 0 && stdout.contains("uid=0") || stderr.contains("uid=0")
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Runs one or more shell commands inside a single `su` invocation.
     * Returns the combined stdout/stderr and the exit code.
     */
    fun run(vararg commands: String): RootResult {
        if (commands.isEmpty()) return RootResult(true, "", 0)
        return try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { out ->
                for (cmd in commands) {
                    out.writeBytes(cmd)
                    out.writeBytes("\n")
                }
                out.writeBytes("exit\n")
                out.flush()
            }
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            val combined = buildString {
                if (stdout.isNotBlank()) append(stdout.trim())
                if (stderr.isNotBlank()) {
                    if (isNotEmpty()) append('\n')
                    append(stderr.trim())
                }
            }
            RootResult(code == 0, combined, code)
        } catch (t: Throwable) {
            RootResult(false, t.message ?: "Unknown root error", -1)
        }
    }
}
