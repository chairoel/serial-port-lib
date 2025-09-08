package com.mascill.serialport

import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * SerialPort

 * @author
 *  <p>
 *  <li><a>1. benjaminwan</a></li>
 *  <li><a href ="https://github.com/chairoel">2. Chairul Amri (Translator)</a> 2015-09-01</li>
 */

class SerialPort(
    device: File,
    baudrate: Int,
    flags: Boolean,
    parity: Int
) {
    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private var mFd: FileDescriptor? = null
    private val mFileInputStream: FileInputStream
    private val mFileOutputStream: FileOutputStream

    val inputStream: InputStream
        get() = mFileInputStream

    val outputStream: OutputStream
        get() = mFileOutputStream

    external fun close()
    external fun setParity(parity: Int)

    init {
        mFd = open(device.absolutePath, baudrate, flags, parity)
        if (mFd == null) {
            Log.e(TAG, "native open returns null")
            throw IOException("Cannot open serial port: ${device.absolutePath}")
        }
        mFileInputStream = FileInputStream(mFd)
        mFileOutputStream = FileOutputStream(mFd)
    }

    companion object {
        private var TAG = SerialPort::class.java.simpleName

        // JNI: harus static agar cocok dengan C function
        @JvmStatic
        private external fun open(
            path: String,
            baudrate: Int,
            flags: Boolean,
            parity: Int
        ): FileDescriptor?

        init {
            System.loadLibrary("serial_port") // sesuai dengan nama .so di CMakeLists
        }
    }
}
