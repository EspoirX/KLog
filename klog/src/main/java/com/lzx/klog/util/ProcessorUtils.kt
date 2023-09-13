package com.lzx.klog.util

import android.app.Application
import android.os.Build
import android.text.TextUtils

object ProcessorUtils {
    fun getMyProcessName(): String? {
        //1)通过Application的API获取当前进程名
        var currentProcessName = getCurrentProcessNameByApplication()
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName
        }

        //2)通过反射ActivityThread获取当前进程名
        currentProcessName = getCurrentProcessNameByActivityThread()
        return if (!TextUtils.isEmpty(currentProcessName)) {
            currentProcessName
        } else currentProcessName

        //3)通过ActivityManager获取当前进程名
        //currentProcessName = getCurrentProcessNameByActivityManager(context);
    }

    /**
     * 通过Application新的API获取进程名，无需反射，无需IPC，效率最高。
     */
    fun getCurrentProcessNameByApplication(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else null
    }

    /**
     * 通过反射ActivityThread获取进程名，避免了ipc
     */
    fun getCurrentProcessNameByActivityThread(): String? {
        var processName: String? = null
        try {
            val declaredMethod = Class.forName(
                "android.app.ActivityThread", false,
                Application::class.java.classLoader
            ).getDeclaredMethod("currentProcessName", *arrayOfNulls<Class<*>?>(0))
            declaredMethod.isAccessible = true
            val invoke = declaredMethod.invoke(null, *arrayOfNulls(0))
            if (invoke is String) {
                processName = invoke
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return processName
    }
}