package com.lzx.klog.util

import android.app.Application

object RuntimeInfo {
    lateinit var sAppContext: Application
    var sIsDebuggable: Boolean = true
}