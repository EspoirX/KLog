package com.lzx.klog.impl

import android.util.Log
import com.lzx.klog.api.ILogConfig
import com.lzx.klog.api.KLog
import com.lzx.klog.util.LogManager
import com.lzx.klog.util.ProcessorUtils
import com.lzx.klog.util.RuntimeInfo
import com.lzx.klog.util.StorageUtils
import com.lzx.klog.writer.FileWriter
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


object LogConfig : ILogConfig {

    private val TAG: String = "LogConfig"
    private var level: Int = 0
    private var logcat: Boolean = RuntimeInfo.sIsDebuggable
    private var maxSize: Int = 0
    private var logCacheMaxSize: Long = 1024 * 1024 * 100L

    private var logPath: String? = null
    private var processTag: String = ""
    private var publicKey: String = ""
    private var isCrypt: Boolean = true

    private var klogPriority: Int = android.os.Process.THREAD_PRIORITY_FOREGROUND //默认优先级

    private var logCompressPeriod: Int = 0 //单位：分钟

    /**
     * 是否已经apply
     * apply后只能设置logLevel singleLogMaxSize，而logPath，processTag, publicKey则不能再进行重复设置了
     */
    private val consume: AtomicBoolean = AtomicBoolean(false)

    /**
     * 指定log进程标识, 最终的日志文件名以此为前缀,如传入ylog,则文件名为ylogpid_2018_05_26_16_53.txt
     */
    override fun processTag(processTag: String): ILogConfig = apply {
        if (!consume.get()) {
            val replace = ProcessorUtils.getMyProcessName()?.replace(".", "-")?.replace(":", "-")
                ?: ""
            LogConfig.processTag = "${processTag}_$replace"
        }
    }

    /**
     * 设置log等级，超过指定等级的log将会被输出
     */
    override fun logLevel(level: Int): ILogConfig = apply {
        LogConfig.level = level
        if (consume.get()) FileWriter.setLogLevel(level)
    }

    override fun logcat(visible: Boolean): ILogConfig = apply {
        logcat = visible
        if (consume.get()) {
            KLog.mLogImpl?.logcatVisible(logcat)
        }
    }

    /**
     * 设置单个log文件的大小，当达到该大小时，自动执行文件切分
     */
    override fun singleLogMaxSize(maxSize: Int): ILogConfig = apply {
        LogConfig.maxSize = maxSize
        if (consume.get()) FileWriter.setFileMaxSize(maxSize)
    }

    /**
     * 获取log输出的等级
     */
    fun getLogLevel(): Int = level

    /**
     * 设置写log的文件目录
     */
    override fun logPath(path: String?): ILogConfig = apply { if (!consume.get()) logPath = path }

    /**
     * 用于加密的client public key
     */
    override fun publicKey(key: String): ILogConfig = apply { if (!consume.get()) publicKey = key }

    override fun isCrypt(crypt: Boolean): ILogConfig = apply {
        if (!consume.get()) {
            isCrypt = crypt
        }
    }

    /**
     * 获取log的文件目录
     */
    fun getLogPath(): String? {
        return logPath
    }

    /**
     * 设置log缓存目录最大值
     * */
    override fun logCacheMaxSiz(maxSize: Long): ILogConfig = apply { logCacheMaxSize = maxSize }

    /**
     *
     * 获取缓存日志总之最大值
     * */
    fun getLogCacheMaxSize(): Long = logCacheMaxSize

    /**
     * 设置线程优先级
     */
    override fun setPriority(prority: Int): ILogConfig = apply { klogPriority = prority }

    /**
     * 获取线程优先级
     */
    override fun getPriority(): Int = klogPriority

    /**
     * log压缩周期
     */
    override fun logCompressPeriod(period: Int): ILogConfig = apply {
        if (!consume.get()) {
            logCompressPeriod = period
        }
    }

    /**
     * 应用log配置，只需调用一次
     */
    override fun apply() {
        KLog.i(TAG, "apply")
        if (!consume.getAndSet(true)) {
            if (logPath == null) {
                logPath = File(StorageUtils.getCacheDir(RuntimeInfo.sAppContext, "logs")).path
            }
            if (!StorageUtils.isFileCanWrite(logPath!!)) {
                logPath = File(RuntimeInfo.sAppContext.filesDir, "logs")
                    .apply {
                        if (!exists()) mkdirs()
                    }.absolutePath
            }

            Log.e("LogService", "path = $logPath")

            LogManager.setCompressPeriod(logCompressPeriod)

            FileWriter.run {
                setPriority(klogPriority)
                init()
                setLogLevel(level)
                setFileMaxSize(maxSize)
                useConsoleLog(false)
                open(
                    logPath!!, File(RuntimeInfo.sAppContext.filesDir, "log").path, processTag,
                    level, publicKey, isCrypt
                )
            }
            KLog.mLogImpl?.logcatVisible(logcat)
        }
    }
}