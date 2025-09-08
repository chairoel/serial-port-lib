package com.mascill.serialport.helper.thread

class SendThread(
    private val sendProvider: () -> Unit,
    private val delayProvider: () -> Int
) : Thread() {

    @Volatile private var suspendFlag = true    // 控制线程的执行 (Control thread execution)
    @Volatile private var killThread = false

    override fun run() {
        super.run()
        while (!isInterrupted) {
            synchronized(this) {
                if (suspendFlag) {
                    try {
                        (this as java.lang.Object).wait()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
            if (killThread) break
            sendProvider()
            try {
                sleep(delayProvider().toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    // 线程暂停 (Thread pause)
    fun pauseThread() {
        suspendFlag = true
    }

    // 唤醒线程 (Wake up the thread)
    @Synchronized
    fun resumeThread() {
        suspendFlag = false
        (this as java.lang.Object).notify()
    }

    // 终止线程 (Terminate a thread)
    fun killThread() {
        killThread = true
    }
}
