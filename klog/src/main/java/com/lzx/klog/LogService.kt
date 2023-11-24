package com.lzx.klog

import android.util.Log
import com.lzx.klog.api.IKLogFlush
import com.lzx.klog.api.ILogConfig
import com.lzx.klog.api.ILogService
import com.lzx.klog.api.KLog
import com.lzx.klog.api.OnSubmitLogListener
import com.lzx.klog.impl.LogConfig
import com.lzx.klog.util.LogManager
import com.lzx.klog.util.TimeComparator
import com.lzx.klog.util.submit.SubmitUtils
import com.lzx.klog.writer.FileWriter
import java.io.File
import java.util.Arrays
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport

class LogService : ILogService {

    override fun config(): ILogConfig {
        return LogConfig
    }

    /**
     * 初始化
     */
    fun init() {
        Log.e("LogService", "------LogService init--------")
        LogManager.start()
    }

    /**
     *  结束定时任务
     */
    fun unInit() {
        LogManager.end()
    }

    /**
     * 获取log文件所在的目录
     * @return log文件目录
     */
    override fun catalog(): String? = LogConfig.getLogPath()

    /**
     * 获取当前APP文件log列表
     * @return log文件列表，按时间倒序排列
     */
    override fun fileLogList(): Array<File> {
        return (File(LogConfig.getLogPath() ?: "")
            .takeIf { it.exists() }
            ?.listFiles()
            ?.filter { it.name.endsWith(".txt") || it.name.endsWith(".zip") }
            ?.let {
                return if (it.isNotEmpty()) {
                    val array = it.toTypedArray()
                    try {
                        Arrays.sort(array, TimeComparator())
                    } catch (e: Exception) {
                        KLog.i("LogService", e.message ?: "")
                    }
                    array
                } else {
                    arrayOf()
                }
            }
            ?: arrayOf())
    }

    /**
     * 获取当前APP指定进程标识的文件log列表
     * @param processId log进程标识
     * @return log文件列表，按时间倒序排列
     */
    override fun fileLogList(processId: String): Array<File> {
        return (File(LogConfig.getLogPath() ?: "")
            .takeIf { it.exists() }
            ?.listFiles()
            ?.filter {
                (it.name.split("__"))[0] == processId &&
                        (it.name.endsWith(".txt") || it.name.endsWith(".zip"))
            }
            ?.let {
                return if (it.isNotEmpty()) {
                    val array = it.toTypedArray()
                    try {
                        Arrays.sort(array, TimeComparator())
                    } catch (e: Exception) {
                        KLog.i("LogService", e.message ?: "")
                    }
                    array
                } else {
                    arrayOf()
                }
            }
            ?: arrayOf())
    }

    /**
     * flush内存日志到文件(例如崩溃捕获的时候调用)
     */
    override fun flush() {
        FileWriter.flush()
    }

    override fun flush(flushCallback: IKLogFlush) {
        FileWriter.flush(flushCallback)
    }

    override fun flushBlocking(milliseconds: Long) {
        val locked = AtomicBoolean(true)
        val thread = Thread.currentThread()
        flush(object : IKLogFlush {
            override fun callback(finish: Boolean) {
                locked.compareAndSet(true, false)
                LockSupport.unpark(thread)
            }
        })
        if (locked.get()) LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds))
    }


    override fun submitLog(fileName: String?, fileNum: Int, endTime: String, listener: OnSubmitLogListener?) {
        val latestNLogsZip = SubmitUtils.getLatestNLogsZip(this, fileNum, endTime)
        flushBlocking(1000) //阻断刷新日志
        flush(object : IKLogFlush {  //上报前先刷新
            override fun callback(finish: Boolean) {
                SubmitUtils.submitFeedback(fileName, latestNLogsZip, listener)
            }
        })
    }
}