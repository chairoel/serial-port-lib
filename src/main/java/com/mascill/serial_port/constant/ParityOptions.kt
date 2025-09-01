package com.mascill.serial_port.constant

import android.content.Context
import android.widget.ArrayAdapter

/**
 * Parity options untuk spinner & mapping ke kode parity JNI:
 * 0=none, 1=even, 2=odd, 3=mark, 4=space
 */
object ParityOptions {
    /** Label yang ditampilkan di UI */
    val names: List<String> = listOf("none", "even", "odd", "mark", "space")

    /** Nilai int yang dikirim ke native (sesuai SerialPort.c) */
    val values: IntArray = intArrayOf(0, 1, 2, 3, 4)

    /** Index default (none) */
    val defaultIndex: Int = 0

    /** Ambil nilai parity berdasarkan index spinner dengan fallback aman */
    fun valueAt(index: Int): Int = values.getOrElse(index) { values[defaultIndex] }

    /** Cari index spinner dari nilai parity (untuk restore) */
    fun indexOfValue(value: Int): Int = values.indexOf(value).let { if (it >= 0) it else defaultIndex }

    /** Adapter siap pakai untuk Spinner */
    fun adapter(ctx: Context): ArrayAdapter<String> =
        ArrayAdapter(ctx, android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
}
