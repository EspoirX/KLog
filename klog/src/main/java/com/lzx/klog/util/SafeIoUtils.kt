package com.lzx.klog.util

import java.io.Closeable
import java.io.IOException

object SafeIoUtils {
    @JvmStatic
    fun safeClose(vararg closeables: Closeable?) {
        for (closeable in closeables) {
            closeable?.safeClose()
        }
    }
}

/**
 * 安全关闭资源
 */
fun Closeable?.safeClose() {
    try {
        this?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}