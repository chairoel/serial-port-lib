package com.mascill.serialport.sample

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.mascill.serialport.constant.BaudRates
import com.mascill.serialport.helper.SerialHelper

class MainActivity : ComponentActivity() {

    // ganti sesuai device kamu saat testing (mis. /dev/ttyS0, /dev/ttyS3, dsb.)
    private var devicePath = "/dev/ttyS0"
    private var baudRate = BaudRates.B9600

    private var helper: SerialHelper? = null

    private val serial by lazy {
        object : SerialHelper(devicePath, baudRate) {
            override fun onDataReceived(buffer: ByteArray?) {
                buffer ?: return
                val hex = buffer.joinToString(" ") { b -> "%02X".format(b) }
                runOnUiThread {
                    findViewById<TextView>(R.id.tvLog).append("\nRX: $hex")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etDevice = findViewById<EditText>(R.id.etDevice)
        val etBaud   = findViewById<EditText>(R.id.etBaud)
        val etHex    = findViewById<EditText>(R.id.etHex)
        val tvLog    = findViewById<TextView>(R.id.tvLog)

        etDevice.setText(devicePath)
        etBaud.setText("9600")

        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            devicePath = etDevice.text.toString().trim().ifEmpty { "/dev/ttyS0" }
            baudRate = when (etBaud.text.toString().trim()) {
                "1200" -> BaudRates.B1200
                "2400" -> BaudRates.B2400
                "4800" -> BaudRates.B4800
                "9600" -> BaudRates.B9600
                "19200" -> BaudRates.B19200
                "38400" -> BaudRates.B38400
                "57600" -> BaudRates.B57600
                "115200" -> BaudRates.B115200
                else -> BaudRates.B9600
            }
            helper = serial.also {
                it.mDev = devicePath
                it.mBaudRate = baudRate
                it.open()
            }
            tvLog.append("\n[✓] Open $devicePath @ $baudRate")
            Toast.makeText(this, "Opened $devicePath", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val hex = etHex.text.toString().trim()
            if (hex.isEmpty()) {
                Toast.makeText(this, "Hex is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            helper?.sendHex(hex)
            tvLog.append("\nTX: $hex")
        }

        findViewById<Button>(R.id.btnClose).setOnClickListener {
            helper?.close()
            tvLog.append("\n[×] Close")
            Toast.makeText(this, "Closed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        helper?.close()
        super.onDestroy()
    }
}
