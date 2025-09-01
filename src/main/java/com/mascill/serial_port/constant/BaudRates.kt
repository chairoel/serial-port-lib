package com.mascill.serial_port.constant

import android.content.Context
import android.widget.ArrayAdapter

object BaudRates {
    // Label yang ditampilkan di Spinner
    val names: List<String> = listOf(
        "50","75","110","134","150","200","300","600",
        "1200","1800","2400","4800","9600","19200","38400",
        "57600","115200","230400","460800","921600"
    )

    // Nilai integer yang dipakai untuk set baudrate
    val values: IntArray = intArrayOf(
        50,  75, 110, 134, 150, 200, 300, 600,
        1200,1800,2400,4800,9600,19200,38400,
        57600,115200,230400,460800,921600
    )

    // Default ke 115200 kalau ada
    val defaultIndex: Int = values.indexOf(115200).let { if (it >= 0) it else 0 }
    fun valueAt(index: Int): Int = values.getOrElse(index) { values[defaultIndex] }

    fun adapter(ctx: Context): ArrayAdapter<String> =
        ArrayAdapter(ctx, android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
}