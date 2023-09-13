package com.lzx.klog.util

import android.content.Context
import java.io.File

object StorageUtils {

    fun getCacheDir(context: Context, child: String): String {
        val path = context.getExternalFilesDir("sandbox")?.absolutePath
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        val file = File(path + File.separator + child)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }

    fun isFileCanWrite(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val file = File(path)
        if (!file.exists()) return false
        if (file.isFile) return false
        return true
    }
}