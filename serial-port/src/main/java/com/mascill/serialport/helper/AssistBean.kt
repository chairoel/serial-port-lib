package com.mascill.serialport.helper

import java.io.Serializable

/**
 * AssistBean
 *
 * @author
 * <p>
 * <li><a>1. benjaminwan</a></li>
 * <li><a href ="https://github.com/chairoel">2. Chairul Amri (Translator)</a> 2015-09-01</li>
 */
class AssistBean : Serializable {
    companion object {
        private const val serialVersionUID = -5620661009186692227L
    }

    var isTxt: Boolean = true
    var sendTxtA: String = "$000~TEST%$100~TEST%$200~TEST%"
    var sendTxtB: String = "$000~TEST%$100~TEST%"
    var sendHexA: String = "AA"
    var sendHexB: String = "BB"
    var sTimeA: String = "500"
    var sTimeB: String = "500"

    fun setTxtMode(isTxt: Boolean) {
        this.isTxt = isTxt
    }

    fun getSendA(): String {
        return if (isTxt) sendTxtA else sendHexA
    }

    fun getSendB(): String {
        return if (isTxt) sendTxtB else sendHexB
    }

    fun setSendA(sendA: String) {
        if (isTxt) {
            sendTxtA = sendA
        } else {
            sendHexA = sendA
        }
    }

    fun setSendB(sendB: String) {
        if (isTxt) {
            sendTxtB = sendB
        } else {
            sendHexB = sendB
        }
    }
}
