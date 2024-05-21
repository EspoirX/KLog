package com.lzx.klog.api

import android.util.Log


/**
 * 对外log接口
 */
object KLog {

    var mLogImpl: ILog? = null
    var logBridge: ILogBridge? = null

    val segmentSize = 3 * 1024

    @JvmStatic
    fun v(tag: String, message: String) {
        var copyMsg = message
        val length = copyMsg.length
        if (length <= segmentSize) {
            realV(tag, copyMsg)
        } else {
            while (copyMsg.length > segmentSize) {
                val subLog = copyMsg.substring(0, segmentSize)
                copyMsg = copyMsg.replace(subLog, "")
                realV(tag, subLog)
            }
            if (copyMsg.isNotEmpty()) {
                realV(tag, copyMsg)
            }
        }
    }

    private fun realV(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.v(tag, message)
        }
        logBridge?.v(tag, message)
        mLogImpl?.v(tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        var copyMsg = message
        val length = copyMsg.length
        if (length <= segmentSize) {
            realI(tag, copyMsg)
        } else {
            while (copyMsg.length > segmentSize) {
                val subLog = copyMsg.substring(0, segmentSize)
                copyMsg = copyMsg.replace(subLog, "")
                realI(tag, subLog)
            }
            if (copyMsg.isNotEmpty()) {
                realI(tag, copyMsg)
            }
        }
    }

    private fun realI(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.i(tag, message)
        }
        logBridge?.i(tag, message)
        mLogImpl?.i(tag, message)
    }


    @JvmStatic
    fun d(tag: String, message: String) {
        var copyMsg = message
        val length = copyMsg.length
        if (length <= segmentSize) {
            realD(tag, copyMsg)
        } else {
            while (copyMsg.length > segmentSize) {
                val subLog = copyMsg.substring(0, segmentSize)
                copyMsg = copyMsg.replace(subLog, "")
                realD(tag, subLog)
            }
            if (copyMsg.isNotEmpty()) {
                realD(tag, copyMsg)
            }
        }
    }

    private fun realD(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.d(tag, message)
        }
        logBridge?.d(tag, message)
        mLogImpl?.d(tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        var copyMsg = message
        val length = copyMsg.length
        if (length <= segmentSize) {
            realW(tag, copyMsg)
        } else {
            while (copyMsg.length > segmentSize) {
                val subLog = copyMsg.substring(0, segmentSize)
                copyMsg = copyMsg.replace(subLog, "")
                realW(tag, subLog)
            }
            if (copyMsg.isNotEmpty()) {
                realW(tag, copyMsg)
            }
        }
    }

    private fun realW(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.w(tag, message)
        }
        logBridge?.w(tag, message)
        mLogImpl?.w(tag, message)
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        var copyMsg = message
        val length = copyMsg.length
        if (length <= segmentSize) {
            realE(tag, copyMsg)
        } else {
            while (copyMsg.length > segmentSize) {
                val subLog = copyMsg.substring(0, segmentSize)
                copyMsg = copyMsg.replace(subLog, "")
                realE(tag, subLog)
            }
            if (copyMsg.isNotEmpty()) {
                realE(tag, copyMsg)
            }
        }
    }

    private fun realE(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.e(tag, message)
        }
        logBridge?.e(tag, message)
        mLogImpl?.e(tag, message)
    }
}