package com.mascill.serial_port.helper

import com.mascill.serial_port.SerialPort
import com.mascill.serial_port.helper.thread.ReadThread
import com.mascill.serial_port.helper.thread.SendThread
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidParameterException


/**
 * SerialHelper
 *
 * 串口辅助工具类 (Serial port auxiliary tools)
 *
 * @author
 * <p>
 * <li><a>1. benjaminwan</a></li>
 * <li><a href ="https://github.com/chairoel">2. Chairul Amri (Translator)</a> 2015-09-01</li>
 */

abstract class SerialHelper @JvmOverloads constructor(
    private var sPort: String = "/dev/tty",
    private var iBaudRate: Int = 115200
) {
    private var mSerialPort: SerialPort? = null
    private var mOutputStream: OutputStream? = null
    private var mInputStream: InputStream? = null

    private var mReadThread: ReadThread? = null
    private var mSendThread: SendThread? = null

    private var nPort: Byte = 0
    private var iParity: Int = 0

    private var _isOpen = false
    private var _bLoopData: ByteArray = byteArrayOf(0x30)
    private var iDelay = 500

    @Throws(SecurityException::class, IOException::class, InvalidParameterException::class)
    fun open() {
        mSerialPort = SerialPort(File(sPort), iBaudRate, true, iParity)
        mOutputStream = mSerialPort?.outputStream
        mInputStream = mSerialPort?.inputStream

        if (mReadThread == null) {
            mReadThread = ReadThread(
                inputProvider = { mInputStream },
                nPort = nPort,
                onDataReceived = { data -> onDataReceived(data) }
            ).apply { name = "mReadThread" }
            mReadThread?.start()
        }

        if (mSendThread == null) {
            mSendThread = SendThread(
                sendProvider = { send(bLoopData) },
                delayProvider = { iDelay }
            ).apply {
                name = "mSendThread"
                pauseThread()
            }
            mSendThread?.start()
        }

        _isOpen = true
    }

    fun close() {
        mReadThread?.stopReading()
        mReadThread = null

        mSerialPort?.close()
        mSerialPort = null

        _isOpen = false
    }

    fun send(bOutArray: ByteArray) {
        try {
            mOutputStream?.write(bOutArray)
        } catch (_: IOException) {
        }
    }

    fun sendHex(sHex: String) {
        val bOutArray = DataFormatUtils.hexToByteArr(sHex)
        send(bOutArray)
    }

    fun sendTxt(sTxt: String) {
        send(sTxt.toByteArray())
    }

    fun sendData(sTxt: CharArray, isTxt: Boolean) {
        val bOutArray = if (isTxt) {
            DataFormatUtils.getBytes(sTxt)
        } else {
            DataFormatUtils.hexToByteArr(String(sTxt))
        }
        send(bOutArray)
    }

    val baudRate: Int get() = iBaudRate

    fun setBaudRate(iBaud: Int): Boolean {
        return if (_isOpen) false else {
            iBaudRate = iBaud
            true
        }
    }

    fun setBaudRate(sBaud: String): Boolean = setBaudRate(sBaud.toInt())

    val port: String get() = sPort

    fun setPort(sPort: String): Boolean {
        return if (_isOpen) false else {
            this.sPort = sPort
            true
        }
    }

    val portIndex: Int get() = nPort.toInt()

    fun setPortIndex(nPort: Int): Boolean {
        return if (_isOpen) false else {
            this.nPort = nPort.toByte()
            true
        }
    }

    val isOpen: Boolean get() = _isOpen

    var bLoopData: ByteArray
        get() = _bLoopData
        set(value) { _bLoopData = value }

    fun setTxtLoopData(sTxt: CharArray) {
        _bLoopData = DataFormatUtils.getBytes(sTxt)
    }

    fun setHexLoopData(sHex: CharArray) {
        _bLoopData = DataFormatUtils.hexToByteArr(String(sHex))
    }

    var delay: Int
        get() = iDelay
        set(value) { iDelay = value }

    fun startSend() {
        mSendThread?.resumeThread()
    }

    fun stopSend() {
        mSendThread?.pauseThread()
    }

    fun destroySend() {
        mSendThread?.apply {
            killThread()
            // bangunkan supaya loop bisa keluar
            resumeThread()
        }
        mSendThread = null
    }

    fun setParity(parity: Int) {
        iParity = parity
    }

    // ------------------------------------------------------------------------
    /**
     * Callback saat data diterima.
     * Format bRec: `[nPort][payload…]`
     */
    protected abstract fun onDataReceived(comRecData: ByteArray)
}
