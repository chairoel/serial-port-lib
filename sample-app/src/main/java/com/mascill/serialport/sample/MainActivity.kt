package com.mascill.serialport.sample

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import androidx.appcompat.widget.SwitchCompat
import com.mascill.serialport.constant.BaudRates
import com.mascill.serialport.constant.ParityOptions
import com.mascill.serialport.helper.AssistBean
import com.mascill.serialport.helper.SerialHelper
import com.mascill.serialport.helper.ShellUtils
import com.mascill.serialport.sample.databinding.ActivityMainBinding
import java.io.*
import java.security.InvalidParameterException
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private val PREFS_SPINNER = "spinner_prefs"
    private val KEY_BAUD_IDX = "baud_idx"
    private val KEY_PORT_NAME = "port_name"
    private val KEY_PARITY_IDX = "parity_idx"
    private lateinit var binding: ActivityMainBinding

    private lateinit var serialControl: SerialControl
    private var dispQueue: DispQueueThread? = null

    private lateinit var etDisplayData: EditText
    private lateinit var etInputData: EditText
    private lateinit var etTimeSendData: EditText
    private lateinit var etManualPortName: EditText
    private lateinit var swMode: SwitchCompat

    @Volatile private var isRun: Boolean = true
    private var assistData: AssistBean = AssistBean()
    private var isConnected: Boolean = false

    private val sFilename = "ComAssistant"
    private val sLinename = "AssistData"

    private var isAdaro: Boolean = false

    private lateinit var portAdapter: HintAdapter

    private val spinnerPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_SPINNER, Context.MODE_PRIVATE)
    }

    // Handler untuk memastikan update UI dijalankan di main thread
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private class HintAdapter(
        context: Context,
        layoutId: Int,
        private val data: MutableList<String>
    ) : ArrayAdapter<String>(context, layoutId, data) {

        override fun isEnabled(position: Int): Boolean {
            // posisi 0 = hint -> tidak bisa dipilih
            return position != 0
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = super.getDropDownView(position, convertView, parent)
            // opsional: bisa styling hint di sini
            return v
        }
    }

    private fun setupBaudRateSpinner(spinner: Spinner) {
        val adapter = BaudRates.adapter(this)
        spinner.adapter = adapter

        // restore index (default = BaudRates.defaultIndex)
        val savedIdx = spinnerPrefs
            .getInt(KEY_BAUD_IDX, BaudRates.defaultIndex)
            .coerceIn(0, adapter.count - 1)

        spinner.setSelection(savedIdx)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinnerPrefs.edit().putInt(KEY_BAUD_IDX, position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPortSpinner(spinner: Spinner) {
        // awal: cuma ada hint
        val items = mutableListOf("Pilih port…")
        portAdapter = HintAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = portAdapter
        spinner.setSelection(0)
        spinner.isEnabled = false

        // load daftar port di background (biar UI nggak freeze)
        Thread {
            val ports: List<String> = getListPort()
            val finalList = buildList {
                add("Pilih port…")
                addAll(ports.filter { it.isNotBlank() }.distinct())
            }

            runOnUiThread {
                portAdapter.clear()
                portAdapter.addAll(finalList)
                spinner.isEnabled = finalList.size > 1

                // restore pilihan sebelumnya kalau ada
                val savedPort = spinnerPrefs.getString(KEY_PORT_NAME, null)
                val idx = if (savedPort != null) portAdapter.getPosition(savedPort) else 0
                spinner.setSelection(if (idx >= 0) idx else 0)

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0) { // bukan hint
                            portAdapter.getItem(position)?.let {
                                spinnerPrefs.edit().putString(KEY_PORT_NAME, it).apply()
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        etDisplayData = binding.editTextRecDisp1
        etInputData = binding.editTextCOMA
        etTimeSendData = binding.editTextTimeCOMA
        etManualPortName = binding.editTextManualPort
        swMode = binding.swMode

        swMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this@MainActivity, "Fitur diaktifkan", Toast.LENGTH_SHORT).show()
                isAdaro = true
                Log.d("TAG", "CheckComA: masuk ${getSerialDeviceNodeByName()}")
            } else {
                Toast.makeText(this@MainActivity, "Fitur dinonaktifkan", Toast.LENGTH_SHORT).show()
                isAdaro = false
            }
        }

        binding.btnClear.setOnClickListener {
            etDisplayData.setText("")
            etInputData.setText("")
        }

        findViewById<Button>(R.id.btnSendData).setOnClickListener {
            val textChars: CharArray = etInputData.text.toString().toCharArray()
            sendPortData(serialControl, textChars)
        }

        val spinnerBaudRateCOMA: Spinner = binding.SpinnerBaudRateCOMA
        setupBaudRateSpinner(spinnerBaudRateCOMA)

        val spinnerPortList: Spinner = binding.spinnerPortList
        setupPortSpinner(spinnerPortList)

        val toggleButtonCOMA: ToggleButton = findViewById(R.id.toggleButtonCOMA)
        toggleButtonCOMA.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkComA(spinnerPortList, spinnerBaudRateCOMA, toggleButtonCOMA)
                isConnected = true
            } else {
                isConnected = false
                closeComPort(serialControl)
            }
        }
    }

    override fun onPause() {
        saveAssistData(assistData)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (dispQueue != null) return

        isRun = true
        // gunakan kelas terpisah DispQueueThread, update UI via mainHandler
        dispQueue = DispQueueThread(
            onData = { comData -> mainHandler.post { displayRecData(comData) } }
        )
        dispQueue?.start()

        assistData = getAssistData()
        displayAssistData(assistData)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        closeComPort(serialControl)
    }

    /**
     * Adtrishan : /dev/ttyUSB0
     * Magnetic North : /dev/ttyS5
     */
    private fun checkComA(spinnerPort: Spinner, spinnerBaudRate: Spinner, toggleButtonCOMA: ToggleButton) {
        val manualInput = etManualPortName.text.toString()

        if (!isAdaro && manualInput.isEmpty() && spinnerPort.selectedItemPosition <= 0) {
            showMessage("Pilih port terlebih dahulu")
            toggleButtonCOMA.isChecked = false
            return
        }

        val portName: String = when {
            isAdaro -> "/dev/${getSerialDeviceNodeByName()}"
            manualInput.isEmpty() -> "/dev/${spinnerPort.selectedItem.toString()}"
            else -> "/dev/$manualInput"
        }

        serialControl = SerialControl(portName).apply {
            val baudVal = BaudRates.valueAt(spinnerBaudRate.selectedItemPosition)
            setBaudRate(baudVal)

            val parity = ParityOptions.valueAt(ParityOptions.defaultIndex)
            setParity(parity)
        }
        openComPort(serialControl, toggleButtonCOMA)
    }

    private inner class SerialControl(sPort: String) : SerialHelper(sPort) {
        override fun onDataReceived(comRecData: ByteArray) {
            dispQueue?.addQueue(comRecData)
            dispQueue?.setResume()
        }
    }

    private fun displayRecData(comRecData: ByteArray) {
        val nComPort = comRecData[0].toInt()
        val size = comRecData.size - 1
        val payload = ByteArray(size)
        System.arraycopy(comRecData, 1, payload, 0, size)

        val sMsg = StringBuilder().apply { append(String(payload)) }

        if (::serialControl.isInitialized && nComPort == serialControl.portIndex && isConnected) {
            etDisplayData.append(sMsg)
        }
    }

    private fun displayAssistData(data: AssistBean) {
        etInputData.setText(data.getSendA())
        etTimeSendData.setText(data.sTimeA)
    }

    private fun saveAssistData(data: AssistBean) {
        data.sTimeA = etTimeSendData.text.toString()
        data.isTxt = true
        data.sendTxtA = etInputData.text.toString()

        val sp: SharedPreferences = getSharedPreferences(sFilename, Context.MODE_PRIVATE)
        try {
            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(data)
                val sBase64 = String(Base64.encode(baos.toByteArray(), 0))
                sp.edit().putString(sLinename, sBase64).apply()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getAssistData(): AssistBean {
        val sp: SharedPreferences = getSharedPreferences(sFilename, Context.MODE_PRIVATE)
        var data = AssistBean()
        try {
            val personBase64: String = sp.getString(sLinename, "") ?: ""
            val base64Bytes = Base64.decode(personBase64.toByteArray(), 0)
            ByteArrayInputStream(base64Bytes).use { bais ->
                ObjectInputStream(bais).use { ois ->
                    data = ois.readObject() as AssistBean
                    return data
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    private fun sendPortData(comPort: SerialHelper?, sOut: CharArray) {
        if (comPort != null && comPort.isOpen) {
            comPort.sendData(sOut, true)
        }
    }

    private fun closeComPort(comPort: SerialHelper?) {
        if (comPort != null) {
            comPort.destroySend()
            comPort.close()
        }
    }

    private fun openComPort(comPort: SerialHelper, toggleButtonCOMA: ToggleButton) {
        try {
            comPort.open()
        } catch (e: SecurityException) {
            if (comPort.portIndex == 0) toggleButtonCOMA.isChecked = false
            showMessage(getString(R.string.nopermission))
        } catch (e: IOException) {
            if (comPort.portIndex == 0) toggleButtonCOMA.isChecked = false
            showMessage(getString(R.string.unknownerr))
        } catch (e: InvalidParameterException) {
            if (comPort.portIndex == 0) toggleButtonCOMA.isChecked = false
            showMessage(getString(R.string.parametererr))
        }
    }

    override fun onDestroy() {
        if (dispQueue != null) {
            isRun = false
            dispQueue?.stopRunning()
            dispQueue = null
        }
        closeComPort(if (::serialControl.isInitialized) serialControl else null)
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
        super.onDestroy()
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun getListPort(): List<String> {
        val rst = ShellUtils.execCommand("ls -l /sys/class/tty/tty*", false)
        val pattern = Pattern.compile("/sys/class/tty/(\\w+) -> .*?/tty/(\\w+)")
        val portList = mutableListOf<String>()

        Log.d("TAG", "getListPort: rst: ${Gson().toJson(rst)}")

        if (rst.result == 0) {
            val info = rst.successMsg
            Log.d("TAG", "getListPort: info: $info")
            if (info == null) {
                return listOf("unknown")
            }
            val matcher = pattern.matcher(info)
            while (matcher.find()) {
                val data = matcher.group(1)
                portList.add(data)
            }
        }
        return if (portList.isEmpty()) listOf("unknown") else portList
    }

    // Versi aktif
    protected fun getSerialDeviceNodeByName(): String {
        val USB_DEVICE_PATH =
            "devices/platform/soc/4e00000.ssusb/4e00000.dwc3/xhci-hcd.2.auto/usb1/1-1/1-1.5/1-1.5:1.0"
        val LIST_TTY_COMMAND = "ls -la /sys/class/tty/"

        val result = ShellUtils.execCommand(LIST_TTY_COMMAND, false)

        if (result.result != 0 || result.successMsg == null) {
            Log.e("SerialDevice", "Failed to execute command or empty result: ${result.errorMsg}")
            return "unknown"
        }

        val lines = result.successMsg!!.split("\n")
        val ttyUSBPattern = Pattern.compile("ttyUSB(\\d+)")

        for (line in lines) {
            if (!line.contains(USB_DEVICE_PATH)) continue
            val matcher = ttyUSBPattern.matcher(line)
            if (matcher.find()) {
                val deviceNode = matcher.group()
                Log.d("SerialDevice", "Found serial device: $deviceNode")
                return deviceNode
            }
        }

        Log.w("SerialDevice", "No matching serial device found for path: $USB_DEVICE_PATH")
        return "unknown"
    }
}
