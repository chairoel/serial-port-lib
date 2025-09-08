package com.mascill.serialport.sample

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Thread antrian display yang independen dari Activity.
 * Kirim data via [addQueue], bangunkan via [setResume], dan hentikan via [stopRunning].
 *
 * @param onData callback untuk setiap paket yang di-pop dari antrian
 * @param perItemSleepMs jeda kecil antar item (default 5ms) agar UI tidak janky
 */
class DispQueueThread(
    private val onData: (ByteArray) -> Unit,
    private val perItemSleepMs: Long = 5L
) : Thread("DispQueue") {

    private val queueList: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    @Volatile private var running: Boolean = true

    override fun run() {
        while (!isInterrupted) {
            if (!running) break

            while (queueList.isNotEmpty()) {
                if (!running) break
                val comData = queueList.poll()
                if (comData != null) {
                    onData(comData)
                }
                try {
                    sleep(perItemSleepMs)
                } catch (_: InterruptedException) {
                    // ignore
                }
            }

            synchronized(this) {
                try {
                    (this as Object).wait()
                } catch (_: InterruptedException) {
                    // ignore
                }
            }
        }
    }

    @Synchronized
    fun addQueue(data: ByteArray) {
        queueList.add(data)
    }

    @Synchronized
    fun setResume() {
        (this as Object).notify()
    }

    /** Hentikan loop dan bangunkan thread kalau sedang wait */
    fun stopRunning() {
        running = false
        setResume()
        interrupt()
    }
}
