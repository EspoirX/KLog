package com.lzx.klog.api

import android.util.Log


/**
 * 对外log接口
 */
object KLog {

    var mLogImpl: ILog? = null
    var logBridge: ILogBridge? = null

    @JvmStatic
    fun v(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.v(tag, message)
        }
        logBridge?.v(tag, message)
        mLogImpl?.v(tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.i(tag, message)
        }
        logBridge?.i(tag, message)
        mLogImpl?.i(tag, message)
    }


    @JvmStatic
    fun d(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.d(tag, message)
        }
        logBridge?.d(tag, message)
        mLogImpl?.d(tag, message)
    }


    @JvmStatic
    fun w(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.w(tag, message)
        }
        mLogImpl?.w(tag, message)
    }


    @JvmStatic
    fun e(tag: String, message: String) {
        if (mLogImpl == null) {
            Log.e(tag, message)
        }
        logBridge?.e(tag, message)
        mLogImpl?.e(tag, message)
    }

}