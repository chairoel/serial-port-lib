package com.mascill.serialport.helper.thread

class ReadThread(
    private val inputProvider: () -> java.io.InputStream?,
    private val nPort: Byte,
    private val onDataReceived: (ByteArray) -> Unit
) : Thread() {

    // 控制线程的执行 (Controlling thread execution)
    @Volatile var readFlag = true

    override fun run() {
        super.run()
        while (!isInterrupted) {
            try {
                if (!readFlag) break
                val input = inputProvider() ?: return
                val buffer = ByteArray(1024)
                val size = input.read(buffer)
                if (size > 0) {
                    val bRec = ByteArray(size + 1)
                    bRec[0] = nPort
                    System.arraycopy(buffer, 0, bRec, 1, size)
                    onDataReceived(bRec)
                }
                sleep(50)
            } catch (e: Throwable) {
                e.printStackTrace()
                return
            }
        }
    }

    // 线程停止 (Thread Stop)
    fun stopReading() {
        readFlag = false
    }
}
