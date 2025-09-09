package com.mascill.serialport.access

import com.mascill.serialport.helper.SerialHelper
import com.mascill.serialport.helper.thread.DispatchQueueThread

class SerialControl : SerialHelper {

    private var dispatchQueue: DispatchQueueThread? = null

    constructor() : super()

    constructor(sPort: String, dispatchQueue: DispatchQueueThread) : super(sPort) {
        this.dispatchQueue = dispatchQueue
    }

    constructor(sPort: String) : super(sPort)

    override fun onDataReceived(comRecData: ByteArray) {
        dispatchQueue?.apply {
            addQueue(comRecData)
            setResume()
        }
    }
}
