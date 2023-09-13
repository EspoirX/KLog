package com.lzx.klog.util

fun String.formatCompat(vararg args: Any?): String {
    if (args.isEmpty()) {
        return ""
    }
    var result = ""
    args.forEach {
        result += it.toString()
    }
    return result
}