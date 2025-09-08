package com.mascill.serialport.helper

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * ShellUtils
 *
 * ### Check root
 * - `checkRootPermission()` [checkRootPermission]
 *
 * ### Execute command (overloads)
 * see [execCommand]
 * - `execCommand(command: String, isRoot: Boolean)`
 * - `execCommand(command: String, isRoot: Boolean, isNeedResultMsg: Boolean)`
 * - `execCommand(commands: List<String>?, isRoot: Boolean)`
 * - `execCommand(commands: List<String>?, isRoot: Boolean, isNeedResultMsg: Boolean)`
 * - `execCommand(commands: Array<String>?, isRoot: Boolean)`
 * - `execCommand(commands: Array<String>?, isRoot: Boolean, isNeedResultMsg: Boolean)`
 *
 * @author
 *  <p>
 *  <li><a href="https://github.com/cepr/android-serialport-api">1. Cedric</a> 2012-xx-xx </li>
 *  <li><a href="https://gitee.com/zyl1221/android_serialport_api">2. Qinlian</a> 2013-05-16 </li>
 *  <li><a href ="https://github.com/chairoel">3. Chairul Amri (Translator)</a> 2015-09-01</li>
 */


object ShellUtils {

    /** Root command */
    const val COMMAND_SU = "su"

    /** Normal shell command */
    const val COMMAND_SH = "sh"

    /** Exit command */
    const val COMMAND_EXIT = "exit\n"

    /** Line break */
    const val COMMAND_LINE_END = "\n"

    private fun noInstance(): Nothing = throw AssertionError()

    /**
     * Check whether has root permission
     *
     * @return true if device has root and can execute `echo root`
     */
    @JvmStatic
    fun checkRootPermission(): Boolean {
        return execCommand("echo root", true, false).result == 0
    }

    /**
     * Execute single shell command, default return result msg
     *
     * @param command shell command string
     * @param isRoot whether need to run with root
     * @return [CommandResult]
     */
    @JvmStatic
    fun execCommand(command: String, isRoot: Boolean): CommandResult {
        return execCommand(arrayOf(command), isRoot, true)
    }

    /**
     * Execute list of shell commands, default return result msg
     *
     * @param commands list of command strings
     * @param isRoot whether need to run with root
     * @return [CommandResult]
     */
    @JvmStatic
    fun execCommand(commands: List<String>?, isRoot: Boolean): CommandResult {
        return execCommand(commands?.toTypedArray(), isRoot, true)
    }

    /**
     * Execute array of shell commands, default return result msg
     *
     * @param commands array of command strings
     * @param isRoot whether need to run with root
     * @return [CommandResult]
     */
    @JvmStatic
    fun execCommand(commands: Array<String>?, isRoot: Boolean): CommandResult {
        return execCommand(commands, isRoot, true)
    }

    /**
     * Execute single shell command
     *
     * @param command command string
     * @param isRoot whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return [CommandResult]
     */
    @JvmStatic
    fun execCommand(command: String, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
        return execCommand(arrayOf(command), isRoot, isNeedResultMsg)
    }

    /**
     * Execute list of shell commands
     *
     * @param commands list of command strings
     * @param isRoot whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return [CommandResult]
     */
    @JvmStatic
    fun execCommand(commands: List<String>?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
        return execCommand(commands?.toTypedArray(), isRoot, isNeedResultMsg)
    }

    /**
     * Execute array of shell commands
     *
     * @param commands array of command strings
     * @param isRoot whether need to run with root
     * @param isNeedResultMsg whether need result msg
     *
     * @return
     * <ul>
     *   <li>if isNeedResultMsg is false, [CommandResult.successMsg] and [CommandResult.errorMsg] are null</li>
     *   <li>if [CommandResult.result] is -1, there may be some exception</li>
     * </ul>
     */
    @JvmStatic
    fun execCommand(commands: Array<String>?, isRoot: Boolean, isNeedResultMsg: Boolean): CommandResult {
        var result = -1
        if (commands == null || commands.isEmpty()) {
            return CommandResult(result, null, null)
        }

        var process: Process? = null
        var successResult: BufferedReader? = null
        var errorResult: BufferedReader? = null
        var successMsg: StringBuilder? = null
        var errorMsg: StringBuilder? = null
        var os: DataOutputStream? = null

        try {
            process = Runtime.getRuntime().exec(if (isRoot) COMMAND_SU else COMMAND_SH)
            os = DataOutputStream(process.outputStream)

            for (command in commands) {
                if (command == null) continue
                // do not use os.writeBytes(command), avoid charset error
                os.write(command.toByteArray())
                os.writeBytes(COMMAND_LINE_END)
                os.flush()
            }

            os.writeBytes(COMMAND_EXIT)
            os.flush()

            result = process.waitFor()

            if (isNeedResultMsg) {
                successMsg = StringBuilder()
                errorMsg = StringBuilder()
                successResult = BufferedReader(InputStreamReader(process.inputStream))
                errorResult = BufferedReader(InputStreamReader(process.errorStream))

                var line: String?
                while (successResult.readLine().also { line = it } != null) {
                    successMsg.append(line)
                }
                while (errorResult.readLine().also { line = it } != null) {
                    errorMsg.append(line)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                os?.close()
                successResult?.close()
                errorResult?.close()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
            process?.destroy()
        }

        return CommandResult(
            result,
            successMsg?.toString(),
            errorMsg?.toString()
        )
    }

    /**
     * Result of shell command execution
     *
     * - [result] : 0 if success, else error code (same as Linux shell)
     * - [successMsg] : output of command
     * - [errorMsg] : error output
     */
    class CommandResult @JvmOverloads constructor(
        var result: Int,
        var successMsg: String? = null,
        var errorMsg: String? = null
    )
}
