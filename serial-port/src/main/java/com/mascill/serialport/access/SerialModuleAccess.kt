package com.mascill.serialport.access

import com.mascill.serialport.constant.SerialAccess
import com.mascill.serialport.helper.SerialHelper
import com.mascill.serialport.helper.ShellUtils.execCommand
import java.util.regex.Matcher
import java.util.regex.Pattern


class SerialModuleAccess {

    fun connect(
        serialControl: SerialHelper,
        portName: String,
        baudRate: Int,
        result: (Boolean, String) -> Unit
    ) {
        try {
            val port = "/dev/$portName"
            serialControl.setPort(port)
            serialControl.setBaudRate(baudRate)
            serialControl.setParity(0)
            serialControl.open()
            result(true, "Connected") // isConnected
        } catch (e: Exception) {
            val errorMessage = "Connection failed: ${e.toString()}"
            result(false, errorMessage)
        }
    }

    // sendData
    fun sendDataSerial(
        serialControl: SerialHelper,
        data: String,
        result: (Boolean, String) -> Unit
    ) {
        try {
            val t: CharSequence = data
            val text = CharArray(t.length)
            for (i in 0..<t.length) {
                text[i] = t[i]
            }
            sendPortData(serialControl, text)
            result(true, "SendData Successfully")
        } catch (e: Exception) {
            val errorMessage = "SendData Failed  ${e.message}"
            result(false, errorMessage)
        }
    }

    private fun sendPortData(comPort: SerialHelper?, sOut: CharArray) {
        if (comPort != null && comPort.isOpen) {
            comPort.sendData(sOut, true)
        } else {
            throw Exception("Port tidak tersedia atau belum terbuka")
        }
    }

    // getListPort
    fun getListPort(): MutableList<String?> {
        val commandResult = execCommand(SerialAccess.LIST_PORT_COMMAND_EXEC, false)
        val pattern: Pattern = Pattern.compile(SerialAccess.LIST_PORT_PATTERN)
        val detectedPorts: MutableList<String?> = ArrayList()

        if (commandResult.result == 0) {
            val commandOutput = commandResult.successMsg
            if (commandOutput == null) {
                return mutableListOf("unknown")
            }

            val matcher: Matcher = pattern.matcher(commandOutput)

            while (matcher.find()) {
                val ttyDeviceName = matcher.group(1)
                detectedPorts.add(ttyDeviceName)
            }
        }

        return if (detectedPorts.isEmpty()) mutableListOf("unknown") else detectedPorts
    }

    fun getSerialDeviceTtyUSB(): String {
        val result = execCommand(SerialAccess.LIST_TTY_COMMAND, false)
        if (result.result != 0 || result.successMsg == null) {
            return "unknown"
        }

        val lines =
            result.successMsg!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val ttyUSBPattern = Pattern.compile(SerialAccess.TTY_USB_PATTERN)

        for (line in lines) {
            if (!line.contains(SerialAccess.USB_DEVICE_PATH)) {
                continue
            }

            val matcher = ttyUSBPattern.matcher(line)
            if (matcher.find()) {
                return matcher.group()
            }
        }

        return "unknown"
    }
}

