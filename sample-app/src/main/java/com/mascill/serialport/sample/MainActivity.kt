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
import androidx.appcompat.widget.SwitchCompat
import com.mascill.serialport.access.SerialModuleAccess
import com.mascill.serialport.access.SerialControl
import com.mascill.serialport.constant.BaudRates
import com.mascill.serialport.constant.ParityOptions
import com.mascill.serialport.helper.AssistBean
import com.mascill.serialport.helper.SerialHelper
import com.mascill.serialport.helper.thread.DispatchQueueThread
import com.mascill.serialport.sample.databinding.ActivityMainBinding
import java.io.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // --- Serial helpers
    private lateinit var serialControl: SerialHelper
    private val serialAccess by lazy { SerialModuleAccess() }
    private var dispatchQueue: DispatchQueueThread? = null

    // --- UI
    private lateinit var etDisplayData: EditText
    private lateinit var etInputData: EditText
    private lateinit var etTimeSendData: EditText
    private lateinit var etManualPortName: EditText
    private lateinit var swMode: SwitchCompat

    @Volatile private var isRun: Boolean = true
    private var assistData: AssistBean = AssistBean()
    private var isConnected: Boolean = false

    private val sFileName = "ComAssistant"
    private val sLineName = "AssistData"

    private var isDockModel: Boolean = false

    private lateinit var portAdapter: HintAdapter

    private val spinnerPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_SPINNER, MODE_PRIVATE)
    }

    // Ensure UI updates run on main thread
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private class HintAdapter(
        context: Context,
        layoutId: Int,
        data: MutableList<String>
    ) : ArrayAdapter<String>(context, layoutId, data) {
        override fun isEnabled(position: Int): Boolean = position != 0 // 0 = hint
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getDropDownView(position, convertView, parent)
        }
    }

    private fun setupBaudRateSpinner(spinner: Spinner) {
        val adapter = BaudRates.adapter(this)
        spinner.adapter = adapter
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
        val items = mutableListOf("Pilih port…")
        portAdapter = HintAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = portAdapter
        spinner.setSelection(0)
        spinner.isEnabled = false

        Thread {
            val ports: List<String?> = serialAccess.getListPort()
            val finalList = buildList {
                add("Pilih port…")
                addAll(ports.filterNotNull().filter { it.isNotBlank() }.distinct())
            }
            runOnUiThread {
                portAdapter.clear()
                portAdapter.addAll(finalList)
                spinner.isEnabled = finalList.size > 1

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
            isDockModel = isChecked
            showMessage(if (isChecked) "Fitur diaktifkan" else "Fitur dinonaktifkan")
            if (isChecked) Log.d(TAG, "CheckComA: masuk ${serialAccess.getSerialDeviceTtyUSB()}")
        }

        binding.btnClear.setOnClickListener {
            etDisplayData.setText("")
            etInputData.setText("")
        }

        binding.btnSendData.setOnClickListener {
            if (!::serialControl.isInitialized || !serialControl.isOpen) {
                showMessage("Port belum terhubung")
                return@setOnClickListener
            }
            val data = etInputData.text.toString()
            serialAccess.sendDataSerial(serialControl, data) { ok, msg ->
                if (!ok) Log.e("SerialSend", msg)
                showMessage(if (ok) "Kirim berhasil" else msg)
            }
        }

        val spinnerBaudRateCOMA: Spinner = binding.SpinnerBaudRateCOMA
        setupBaudRateSpinner(spinnerBaudRateCOMA)

        val spinnerPortList: Spinner = binding.spinnerPortList
        setupPortSpinner(spinnerPortList)

        val toggleButtonCOMA: ToggleButton = binding.toggleButtonCOMA
        toggleButtonCOMA.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                connectUsingAccess(spinnerPortList, spinnerBaudRateCOMA, toggleButtonCOMA)
            } else {
                isConnected = false
                closeComPort(serialControl)
            }
        }
    }

    private fun ensureDispatchQueue() {
        if (dispatchQueue != null) return
        isRun = true
        dispatchQueue = DispatchQueueThread(onData = { comData ->
            mainHandler.post { displayRecData(comData) }
        }).also { it.start() }
    }

    private fun connectUsingAccess(spinnerPort: Spinner, spinnerBaudRate: Spinner, toggle: ToggleButton) {
        val manualInput = etManualPortName.text.toString().trim()

        if (!isDockModel && manualInput.isEmpty() && spinnerPort.selectedItemPosition <= 0) {
            showMessage("Pilih port terlebih dahulu")
            toggle.isChecked = false
            return
        }

        val selectedName: String = when {
            isDockModel -> serialAccess.getSerialDeviceTtyUSB()
            manualInput.isNotEmpty() -> manualInput
            else -> spinnerPort.selectedItem.toString()
        }

        // Pakai SerialControl bawaan yang sudah override onDataReceived -> DispatchQueueThread
        ensureDispatchQueue()
        serialControl = SerialControl(selectedName, dispatchQueue!!)

        // Set parity & parameter lain sebelum connect
        val parity = ParityOptions.valueAt(ParityOptions.defaultIndex)
        serialControl.setParity(parity)

        val baudVal = BaudRates.valueAt(spinnerBaudRate.selectedItemPosition)

        // Note: SerialModuleAccess.connect akan setPort("/dev/$selectedName") lagi (idempoten)
        serialAccess.connect(serialControl, selectedName, baudVal) { ok, msg ->
            if (!ok) {
                toggle.isChecked = false
                showMessage(msg)
                return@connect
            }
            isConnected = true
            showMessage("Terhubung ke $selectedName @ $baudVal")
        }
    }

    override fun onPause() {
        saveAssistData(assistData)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        ensureDispatchQueue()
        assistData = getAssistData()
        displayAssistData(assistData)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        closeComPort(if (::serialControl.isInitialized) serialControl else null)
    }

    private fun displayRecData(comRecData: ByteArray) {
        val nComPort = comRecData[0].toInt()
        val size = comRecData.size - 1
        val payload = ByteArray(size)
        System.arraycopy(comRecData, 1, payload, 0, size)
        val sMsg = String(payload)

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

        val sp: SharedPreferences = getSharedPreferences(sFileName, MODE_PRIVATE)
        try {
            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(data)
                val sBase64 = String(Base64.encode(baos.toByteArray(), 0))
                sp.edit().putString(sLineName, sBase64).apply()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getAssistData(): AssistBean {
        val sp: SharedPreferences = getSharedPreferences(sFileName, MODE_PRIVATE)
        var data = AssistBean()
        try {
            val personBase64: String = sp.getString(sLineName, "") ?: ""
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

    private fun closeComPort(comPort: SerialHelper?) {
        if (comPort != null) {
            isConnected = false
            try {
                comPort.destroySend()
                comPort.close()
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dispatchQueue != null) {
            isRun = false
            dispatchQueue?.stopRunning()
            dispatchQueue = null
        }
        closeComPort(if (::serialControl.isInitialized) serialControl else null)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private var TAG = MainActivity::class.java.simpleName
        private const val PREFS_SPINNER = "spinner_prefs"
        private const val KEY_BAUD_IDX = "baud_idx"
        private const val KEY_PORT_NAME = "port_name"
    }
}
