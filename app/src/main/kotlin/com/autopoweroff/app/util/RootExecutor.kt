package com.autopoweroff.app.util

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

/**
 * Handles root (su) availability checks and executing privileged commands.
 */
object RootExecutor {
    private const val TAG = "RootExecutor"

    /**
     * Checks if the su binary is installed and executable.
     */
    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) {
                    return true
                }
            } catch (t: Throwable) {
                // Ignore security exceptions on restricted paths
            }
        }

        // Test running su command
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            process.waitFor()
            !line.isNullOrBlank()
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Executes root commands to power off the device.
     * Uses Android `svc power shutdown` primary command, falling back to `reboot -p`
     * and `setprop sys.powerctl shutdown`.
     */
    fun executeShutdown(): Boolean {
        Log.i(TAG, "Attempting hardware shutdown via root...")
        val shutdownCommands = listOf(
            "svc power shutdown",
            "reboot -p",
            "setprop sys.powerctl shutdown",
            "poweroff"
        )

        for (cmd in shutdownCommands) {
            val success = executeCommand(cmd)
            if (success) {
                Log.i(TAG, "Command '$cmd' executed successfully.")
                return true
            }
        }
        return false
    }

    fun executeCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute root command: $command", e)
            false
        }
    }
}

