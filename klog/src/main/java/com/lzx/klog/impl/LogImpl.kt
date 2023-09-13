package com.lzx.klog.impl

import android.util.Log
import com.lzx.klog.api.ILog
import com.lzx.klog.api.LogLevel
import com.lzx.klog.util.RuntimeInfo
import com.lzx.klog.util.formatCompat
import com.lzx.klog.writer.FileWriter


class LogImpl : ILog {
    private var mLogcatVisible = RuntimeInfo.sIsDebuggable
    private fun shouldLog(priority: Int): Boolean {
        val level = LogConfig.getLogLevel()
        return when (priority) {
            LogLevel.LEVEL_VERBOSE -> level <= LogLevel.LEVEL_VERBOSE
            LogLevel.LEVEL_DEBUG -> level <= LogLevel.LEVEL_DEBUG
            LogLevel.LEVEL_INFO -> level <= LogLevel.LEVEL_INFO
            else -> true
        }
    }

    override fun logcatVisible(visible: Boolean) {
        mLogcatVisible = visible
    }

    override fun v(tag: String, message: () -> Any?) {
        if (mLogcatVisible) {
            Log.v(tag, message().toString())
        }
        if (shouldLog(LogLevel.LEVEL_VERBOSE)) {
            FileWriter.v(tag, "", "", 0, Thread.currentThread().id, message().toString())
        }
    }

//    override fun v(tag: String, format: String, vararg args: Any?) {
//        if (mLogcatVisible) {
//            Log.v(tag, format.formatCompat(*args))
//        }
//        if (shouldLog(LogLevel.LEVEL_VERBOSE)) {
//            FileWriter.v(tag, "", "", 0, Thread.currentThread().id, format.formatCompat(*args))
//        }
//    }

    override fun v(tag: String, format: String, vararg args: Any?) {
        if (mLogcatVisible) {
            Log.v(tag, format.formatCompat(*args))
        }
        if (shouldLog(LogLevel.LEVEL_VERBOSE)) {
            FileWriter.v(tag, "", "", 0, Thread.currentThread().id, format, *args)
        }
    }

    override fun v(tag: String, message: String) {
        if (mLogcatVisible) {
            Log.v(tag, message)
        }

        if (shouldLog(LogLevel.LEVEL_VERBOSE)) {
            FileWriter.v(tag, "", "", 0, Thread.currentThread().id, message)
        }
    }

    override fun d(tag: String, message: () -> Any?) {
        if (mLogcatVisible) {
            Log.d(tag, message().toString())
        }
        if (shouldLog(LogLevel.LEVEL_DEBUG)) {
            FileWriter.d(tag, "", "", 0, Thread.currentThread().id, message().toString())
        }
    }

//    override fun d(tag: String, format: String, vararg args: Any?) {
//        if (mLogcatVisible) {
//            Log.d(tag, format.formatCompat(*args))
//        }
//        if (shouldLog(LogLevel.LEVEL_DEBUG)) {
//            FileWriter.d(tag, "", "", 0, Thread.currentThread().id, format.formatCompat(*args))
//        }
//    }

    override fun d(tag: String, format: String, vararg args: Any?) {
        if (mLogcatVisible) {
            Log.d(tag, format.formatCompat(*args))
        }
        if (shouldLog(LogLevel.LEVEL_DEBUG)) {
            FileWriter.d(tag, "", "", 0, Thread.currentThread().id, format, *args)
        }
    }

    override fun d(tag: String, message: String) {
        if (mLogcatVisible) {
            Log.d(tag, message)
        }

        if (shouldLog(LogLevel.LEVEL_DEBUG)) {
            FileWriter.d(tag, "", "", 0, Thread.currentThread().id, message)
        }
    }

    override fun i(tag: String, message: () -> Any?) {
        if (mLogcatVisible) {
            Log.i(tag, message().toString())
        }
        if (shouldLog(LogLevel.LEVEL_INFO)) {
            FileWriter.i(tag, "", "", 0, Thread.currentThread().id, message().toString())
        }
    }


    override fun i(tag: String, format: String, vararg args: Any?) {
        if (mLogcatVisible) {
            Log.i(tag, format.formatCompat(*args))
        }
        if (shouldLog(LogLevel.LEVEL_INFO)) {
            FileWriter.i(tag, "", "", 0, Thread.currentThread().id, format, *args)
        }
    }

    override fun i(tag: String, message: String) {
        if (mLogcatVisible) {
            Log.i(tag, message)
        }

        if (shouldLog(LogLevel.LEVEL_INFO)) {
            FileWriter.i(tag, "", "", 0, Thread.currentThread().id, message)
        }
    }

    override fun w(tag: String, message: () -> Any?) {
        if (mLogcatVisible) {
            Log.w(tag, message().toString())
        }
        FileWriter.w(tag, "", "", 0, Thread.currentThread().id, message().toString())
    }

    override fun w(tag: String, format: String, vararg args: Any?) {
        if (mLogcatVisible) {
            Log.w(tag, format.formatCompat(*args))
        }
        FileWriter.w(tag, "", "", 0, Thread.currentThread().id, format, *args)
    }

    override fun w(tag: String, message: String) {
        if (mLogcatVisible) {
            Log.w(tag, message)
        }

        if (shouldLog(LogLevel.LEVEL_INFO)) {
            FileWriter.w(tag, "", "", 0, Thread.currentThread().id, message)
        }
    }

    override fun e(tag: String, message: () -> Any?, error: Throwable?) {
        val messageStr = message()
        if (mLogcatVisible) {
            Log.e(tag, if (error != null) "$messageStr \nException occurs at ${Log.getStackTraceString(error)} \n"
            else message().toString())
        }
        FileWriter.e(tag, "", "", 0, Thread.currentThread().id,
            if (error != null) "$messageStr \nException occurs at ${Log.getStackTraceString(error)} \n"
            else messageStr.toString())
    }

    override fun e(tag: String, format: String, error: Throwable?, vararg args: Any?) {
        if (mLogcatVisible) {
            Log.e(tag, if (error != null) ("$format \nException occurs " +
                "at ${Log.getStackTraceString(error)}").formatCompat(*args) else format.formatCompat(*args))
        }
        if (error != null) {
            FileWriter.e(tag, "", "", 0, Thread.currentThread().id,
                    "$format \nException occurs at ${Log.getStackTraceString(error)}", *args)
        } else {
            FileWriter.e(tag, "", "", 0, Thread.currentThread().id, format, *args)
        }
    }

    override fun e(tag: String, message: String, error: Throwable?) {
        if (mLogcatVisible) {
            Log.e(tag, if (error != null) ("$message \nException occurs " +
                "at ${Log.getStackTraceString(error)}") else message)
        }
        FileWriter.e(tag, "", "", 0, Thread.currentThread().id,
            if (error != null) "$message \nException occurs at ${Log.getStackTraceString(error)}"
            else message)
    }
}