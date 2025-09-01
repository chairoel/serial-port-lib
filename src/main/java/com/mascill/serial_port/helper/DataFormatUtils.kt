package com.mascill.serial_port.helper

/**
 * DataFormatUtils
 *
 *  Util konversi data (Hex <-> Byte)
 *
 * @author
 * <p>
 * <li><a>1. benjaminwan</a></li>
 * <li><a href ="https://github.com/chairoel">2. Chairul Amri (Translator)</a> 2015-09-01</li>
 */
object DataFormatUtils {

    // -------------------------------------------------------
    /** Cek ganjil/genap dengan bitwise; return 1 jika ganjil, 0 jika genap */
    @JvmStatic
    fun isOdd(num: Int): Int = num and 0x1

    // -------------------------------------------------------
    /** Hex string -> Int */
    @JvmStatic
    fun hexToInt(inHex: String): Int = inHex.toInt(16)

    // -------------------------------------------------------
    /** 2-digit Hex string -> Byte */
    @JvmStatic
    fun hexToByte(inHex: String): Byte = inHex.toInt(16).toByte()

    // -------------------------------------------------------
    /** 1 byte -> 2-digit HEX uppercase ("0A") */
    @JvmStatic
    fun byte2Hex(inByte: Byte): String = "%02X".format(inByte)

    // -------------------------------------------------------
    /** ByteArray -> HEX string dengan spasi antar byte ("0A FF 12 ") */
    @JvmStatic
    fun byteArrToHex(inBytArr: ByteArray): String {
        val sb = StringBuilder(inBytArr.size * 3)
        for (b in inBytArr) {
            sb.append(byte2Hex(b)).append(' ')
        }
        return sb.toString()
    }

    // -------------------------------------------------------
    /** ByteArray (subset) -> HEX string tanpa spasi antar byte ("0AFF12") */
    @JvmStatic
    fun byteArrToHex(inBytArr: ByteArray, offset: Int, byteCount: Int): String {
        val end = (offset + byteCount).coerceAtMost(inBytArr.size)
        val sb = StringBuilder((end - offset) * 2)
        for (i in offset until end) {
            sb.append(byte2Hex(inBytArr[i]))
        }
        return sb.toString()
    }

    // -------------------------------------------------------
    /** Hex string (boleh mengandung spasi) -> ByteArray */
    @JvmStatic
    fun hexToByteArr(inHexRaw: String): ByteArray {
        var inHex = inHexRaw.replace("\\s".toRegex(), "")
        var hexLen = inHex.length

        // Jika panjang ganjil, prepend '0'
        if (isOdd(hexLen) == 1) {
            inHex = "0$inHex"
            hexLen++
        }

        val result = ByteArray(hexLen / 2)
        var j = 0
        var i = 0
        while (i < hexLen) {
            result[j] = hexToByte(inHex.substring(i, i + 2))
            j++
            i += 2
        }
        return result
    }

    // -------------------------------------------------------
    /** CharArray -> ByteArray (UTF-8) */
    @JvmStatic
    fun getBytes(chars: CharArray): ByteArray {
        // Pakai UTF-8 yang benar (panjang byte bisa > panjang char jika multibyte)
        return String(chars).toByteArray(Charsets.UTF_8)
    }
}
