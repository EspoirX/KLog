package com.lzx.klog.api

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.min


/**
 * 对外log接口
 */
object KLog {

    enum class Type {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    interface LogHook {
        fun hook(type: Type = Type.INFO, tag: String, message: String)
    }

    var enableWriteLogFile = true
    var mLogImpl: ILog? = null
    val logHooks by lazy { ArrayList<LogHook>() }

    /**
     * 添加日志拦截器
     */
    fun addHook(hook: LogHook) {
        logHooks.add(hook)
    }

    /**
     * 删除日志拦截器
     */
    fun removeHook(hook: LogHook) {
        logHooks.remove(hook)
    }

    @JvmOverloads
    @JvmStatic
    fun v(tag: String, message: String) {
        print(Type.VERBOSE, tag, message)
    }

    @JvmOverloads
    @JvmStatic
    fun i(tag: String, message: String) {
        print(Type.INFO, tag, message)
    }

    @JvmOverloads
    @JvmStatic
    fun d(tag: String, message: String) {
        print(Type.DEBUG, tag, message)
    }

    @JvmOverloads
    @JvmStatic
    fun w(tag: String, message: String) {
        print(Type.WARN, tag, message)
    }

    @JvmOverloads
    @JvmStatic
    fun e(tag: String, message: String) {
        print(Type.ERROR, tag, message)
    }

    @JvmOverloads
    @JvmStatic
    fun json(
        json: String?,
        tag: String,
        msg: String = "",
        type: Type = Type.INFO,
    ) {
        var message = json.toString()
        if (message.isBlank()) {
            print(type, tag, "$msg\n$message")
            return
        }
        val tokener = JSONTokener(message)
        val obj = try {
            tokener.nextValue()
        } catch (e: Exception) {
            "Parse json error"
        }
        message = when (obj) {
            is JSONObject -> obj.toString(2)
            is JSONArray -> obj.toString(2)
            else -> obj.toString()
        }
        print(type, tag, "$msg\n$message")
    }

    private fun print(type: Type = Type.INFO, tag: String, message: String) {
        for (logHook in logHooks) {
            logHook.hook(type, tag, message)
        }
        val max = 3800
        val length = message.length
        if (length > max) {
            synchronized(this) {
                var startIndex = 0
                var endIndex = max
                while (startIndex < length) {
                    endIndex = min(length, endIndex)
                    val substring = message.substring(startIndex, endIndex)
                    log(type, tag, substring)
                    startIndex += max
                    endIndex += max
                }
            }
        } else {
            log(type, tag, message)
        }
    }

    private fun log(type: Type, tag: String, msg: String) {
        when (type) {
            Type.VERBOSE -> {
                mLogImpl?.v(tag, msg)
            }

            Type.DEBUG -> {
                mLogImpl?.d(tag, msg)
            }

            Type.INFO -> {
                mLogImpl?.i(tag, msg)
            }

            Type.WARN -> {
                mLogImpl?.w(tag, msg)
            }

            Type.ERROR -> {
                mLogImpl?.e(tag, msg)
            }
        }
    }
}