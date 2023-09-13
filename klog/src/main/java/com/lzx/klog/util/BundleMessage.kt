package com.lzx.klog.util

import com.lzx.klog.api.IKLogFlush


class BundleMessage {
    var what: Int = 0
    var level: Int = 0
    var tag: String = ""
    var fileName: String = ""
    var funcName: String = ""
    var msg: String = ""
    var line: Int = 0
    var pid: Int = 0
    var tid: Long = 0
    var mid: Long = 0
    var use: Boolean = false
    var size: Int = 0
    var namePrefix: String = ""
    var logDir: String = ""
    var publicKey: String = ""
    var mmapDir: String = ""
    var flushCallback: IKLogFlush? = null
    var format: String = ""
    var args: Array<out Any?> = arrayOf("")
    var isCrypt: Boolean = true
    var threadName: String = ""
    private val FLAG_IN_USE = 1 shl 0

    private val MAX_POOL_SIZE = 50
    var next: BundleMessage? = null
//    private var flags: Int = 0

    companion object {
        //        private val sPoolSync = Any()
        private var sPool: BundleMessage? = null
        private var sPoolSize = 0
        fun obtain(): BundleMessage {
            synchronized(BundleMessage::class.java) {
                if (sPool != null) {
                    val m = sPool
                    sPool = m!!.next
                    m.next = null
//                    m.flags = 0
                    sPoolSize--
                    return m
                }
            }
            return BundleMessage()
        }
    }

    fun recycleUnchecked() {

        level = 0
        tag = ""
        fileName = ""
        funcName = ""
        msg = ""
        line = 0
        pid = 0
        tid = 0
        mid = 0
        use = false
        size = 0
        namePrefix = ""
        logDir = ""
        publicKey = ""
        mmapDir = ""
        flushCallback = null
        format = ""
        args = arrayOf("")
        isCrypt = true

        synchronized(BundleMessage::class.java) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool
                sPool = this
                sPoolSize++
            }
        }
    }
}