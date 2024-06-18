package com.lzx.klog.writer

import android.os.Looper
import android.os.Process
import android.util.Log
import com.lzx.klog.api.IKLogFlush
import com.lzx.klog.api.KLog
import com.lzx.klog.api.LogLevel
import com.lzx.klog.util.BundleMessage
import com.lzx.klog.util.formatCompat
import com.lzx.klog.writer.FileWriter.CLOSE
import com.lzx.klog.writer.FileWriter.FLUSH
import com.lzx.klog.writer.FileWriter.OPEN
import com.lzx.klog.writer.FileWriter.SET_FILE_MAX_SIZE
import com.lzx.klog.writer.FileWriter.SET_LOG_LEVEL
import com.lzx.klog.writer.FileWriter.USE_CONSOLE_LOG
import com.lzx.klog.writer.FileWriter.WRITE
import com.lzx.klog.writer.FileWriter.condition
import com.lzx.klog.writer.FileWriter.lock
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


object FileWriter {
    private var mInited = AtomicBoolean(false)

    private var mProcessId: Int = 0
    private var mMainTid: Long = 0
    private var mQueue = LinkedBlockingQueue<BundleMessage>()
    private var wThread: WriteLogThread? = null

    const val OPEN = 1
    const val FLUSH = 2
    const val CLOSE = 3
    const val SET_LOG_LEVEL = 4
    const val SET_FILE_MAX_SIZE = 5
    const val USE_CONSOLE_LOG = 6
    const val WRITE = 7

    private const val THREAD_NAME = "writer_klog"

    val lock: Lock = ReentrantLock()
    val condition: Condition = lock.newCondition()

    private var priority: Int = Process.THREAD_PRIORITY_FOREGROUND

    fun init() {
        if (!mInited.get()) {
            wThread = WriteLogThread(mQueue, priority)
            wThread?.start()
            wThread?.name = THREAD_NAME
            try {
                System.loadLibrary("lzx_klog")
            } catch (e: Throwable) {
                e.printStackTrace()
                Log.w("FileWriter", "load lzx_klog.so failed!!!")
                mInited.set(false)
            }
            mInited.set(true)
        }
    }

    fun setPriority(logPriority: Int) {
        priority = logPriority
    }

    /**
     * mmapDir一般定义在/data/data/xxx/files/log/里面
     */
    fun open(
        logDir: String, mmapDir: String, namePrefix: String, logLevel: Int, publicKey: String, isCrypt: Boolean = false,
    ) {
        if (!mInited.get()) {
            return
        }
        mProcessId = Process.myPid()
        mMainTid = Looper.getMainLooper().thread.id

        postMessage(BundleMessage.obtain().apply {
            what = OPEN
            this.logDir = logDir
            this.mmapDir = mmapDir
            this.namePrefix = namePrefix
            this.level = logLevel
            this.publicKey = publicKey
            this.isCrypt = isCrypt
        })
    }

    fun flush(flushCallback: IKLogFlush) {
        if (!mInited.get()) {
            return
        }
        postMessage(BundleMessage.obtain().apply {
            what = FLUSH
            this.flushCallback = flushCallback
        })
    }

    fun flush() {
        if (!mInited.get()) {
            return
        }
        postMessage(BundleMessage.obtain().apply {
            what = FLUSH
        })
    }

    fun close() {
        if (!mInited.get()) {
            return
        }

        postMessage(BundleMessage.obtain().apply {
            what = CLOSE
        })
    }

    fun setLogLevel(logLevel: Int) {
        if (!mInited.get()) {
            return
        }

        postMessage(BundleMessage.obtain().apply {
            what = SET_LOG_LEVEL
            this.level = logLevel
        })
    }

    fun setFileMaxSize(size: Int) {
        if (!mInited.get()) {
            return
        }

        postMessage(BundleMessage.obtain().apply {
            what = SET_FILE_MAX_SIZE
            this.size = size
        }
        )
    }

    fun useConsoleLog(use: Boolean) {
        if (!mInited.get()) {
            return
        }
        postMessage(BundleMessage.obtain().apply {
            this.use = use
            this.what = USE_CONSOLE_LOG
        })
    }

    fun v(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        msg: String,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_VERBOSE, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, msg
        )
    }

    fun v(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        format: String,
        vararg args: Any?,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_VERBOSE, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, format, *args
        )
    }

    fun i(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        msg: String,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_INFO, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, msg
        )
    }

    fun i(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        format: String,
        vararg args: Any?,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_INFO, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, format, *args
        )
    }

    fun d(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        msg: String,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_DEBUG, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, msg
        )
    }

    fun d(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        format: String,
        vararg args: Any?,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_DEBUG, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, format, *args
        )
    }

    fun w(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        msg: String,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_WARN, tag, fileName, funcName,
            line, mProcessId, tid, mMainTid, msg
        )
    }

    fun w(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        format: String,
        vararg args: Any?,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_WARN, tag, fileName, funcName,
            line, mProcessId, tid, mMainTid, format, *args
        )
    }

    fun e(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        msg: String,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_ERROR, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, msg
        )
    }

    fun e(
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        tid: Long,
        format: String,
        vararg args: Any?,
    ) {
        if (!mInited.get()) {
            return
        }
        logWrite(
            LogLevel.LEVEL_ERROR, tag, fileName,
            funcName, line, mProcessId, tid, mMainTid, format, *args
        )
    }

    private fun logWrite(
        level: Int,
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        pid: Int,
        tid: Long,
        maintid: Long,
        msg: String,
    ) {
        if (!KLog.enableWriteLogFile) return
        postMessage(BundleMessage.obtain().apply {
            what = WRITE
            this.level = level
            this.tag = tag
            this.fileName = fileName
            this.funcName = funcName
            this.msg = msg
            this.line = line
            this.pid = pid
            this.tid = tid
            this.mid = maintid
            this.threadName = Thread.currentThread().name
        })
    }

    private fun logWrite(
        level: Int,
        tag: String,
        fileName: String,
        funcName: String,
        line: Int,
        pid: Int,
        tid: Long,
        maintid: Long,
        format: String,
        vararg args: Any?,
    ) {
        if (!KLog.enableWriteLogFile) return
        postMessage(BundleMessage.obtain().apply {
            what = WRITE
            this.level = level
            this.tag = tag
            this.fileName = fileName
            this.funcName = funcName
            this.format = format
            this.args = args
            this.line = line
            this.pid = pid
            this.tid = tid
            this.mid = maintid
            this.threadName = Thread.currentThread().name
        })
    }

    private fun postMessage(msg: BundleMessage) {
        mQueue.add(msg)
        try {
            if (lock.tryLock()) {
                try {
                    condition.signal()
                } finally {
                    lock.unlock()
                }
            }
        } catch (e: Exception) {
            Log.w("postMessage", e.message ?: "")
        }
    }
}

class WriteLogThread constructor(private val mQueue: Queue<BundleMessage>, private var prority: Int) : Thread() {
    val TAG = "WriteLogThread"

    private fun init() {}


    private fun handleMessage(msg: BundleMessage?) {
        try {
            when (msg?.what) {
                WRITE -> {
                    if (msg.format == "") {
                        msg.apply {
                            com.lzx.klog.writer.FileLog.logWrite(
                                level,
                                tag,
                                fileName,
                                funcName,
                                line,
                                pid,
                                tid,
                                mid,
                                this.msg,
                                threadName
                            )
                        }
                    } else {
                        msg.apply {
                            com.lzx.klog.writer.FileLog.logWrite(
                                level,
                                tag,
                                fileName,
                                funcName,
                                line,
                                pid,
                                tid,
                                mid,
                                this.format.formatCompat(*this.args),
                                threadName
                            )
                        }
                    }
                }

                OPEN -> {
                    msg.apply {
                        com.lzx.klog.writer.FileLog.open(
                            logDir,
                            mmapDir,
                            namePrefix,
                            level,
                            com.lzx.klog.writer.FileLog.MODE_ASYNC,
                            publicKey,
                            isCrypt
                        )
                    }
                }

                FLUSH -> {
                    com.lzx.klog.writer.FileLog.flush(true)
                    msg.flushCallback?.callback(true)
                }

                CLOSE -> com.lzx.klog.writer.FileLog.close()
                SET_LOG_LEVEL -> com.lzx.klog.writer.FileLog.level(msg.level)
                SET_FILE_MAX_SIZE -> com.lzx.klog.writer.FileLog.fileMaxSize(msg.size)
                USE_CONSOLE_LOG -> com.lzx.klog.writer.FileLog.useConsoleLog(msg.use)
            }
            msg?.recycleUnchecked()
        } catch (e: UnsatisfiedLinkError) {
            KLog.i(TAG, e.message ?: "")
        }
    }

    override fun run() {
        Process.setThreadPriority(prority)
        while (true) {
            try {
                if (mQueue.isEmpty()) {
                    try {
                        if (lock.tryLock()) {
                            try {
                                condition.await()
                            } finally {
                                lock.unlock()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, e.message ?: "")
                    }
                } else {
                    mQueue?.poll()?.let { handleMessage(it) }
                }
            } catch (e: Exception) {
                Log.w(TAG, e.message ?: "")
            }
        }
    }
}
