package com.lzx.klog.util

import android.annotation.SuppressLint
import android.util.Log
import com.lzx.klog.api.KLog
import com.lzx.klog.impl.LogConfig
import com.lzx.klog.impl.LogImpl
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern


object LogManager {
    private const val LOG_DATE_WITH_MINUTE_FORMAT_STR = "yyyy_MM_dd_HH_mm"

    /** 7 days.  */
    private const val DAYS_DELETE = 7L * 24 * 60 * 60 * 1000

    /** 1days.**/
    private const val DAYS_COMPRESSED = 1L * 24 * 60 * 60 * 1000

    /** 2 min**/
    private const val MINS_COMPRESSED = 2 * 60 * 1000

    val LOG_DATA_REGEX = Regex("_.*_(19|20)\\d{2}_[0-2]{2}_[0-9]{2}_[0-9]{2}_[0-9]{2}")

    private const val LOG_EXT = ".txt"
    private const val TAG = "LogManager"

    private var taskPeriodMinute = 30L
    private var timeCompressGap = DAYS_COMPRESSED

    private const val PATTERN_STR = "[0-9]{4}_[0-9]{2}_[0-9]{2}_[0-9]{2}"
    private const val PATTERN_WITH_MINUTE_STR = "[0-9]{4}_[0-9]{2}_[0-9]{2}_[0-9]{2}_[0-9]{2}"
    private val PATTERN = Pattern.compile(PATTERN_STR)
    var PATTERN_WITH_MINUTE = Pattern.compile(PATTERN_WITH_MINUTE_STR)


    /**
     * 用来定时删除冗余日志文件
     */
    private var clearService: ScheduledExecutorService = Executors.newScheduledThreadPool(
        1
    ) { r ->
        return@newScheduledThreadPool Thread(r, "logkit")
    }

    /**
     * LoginService开始初始化
     */
    fun start() {
        KLog.mLogImpl = LogImpl()
    }

    fun setCompressPeriod(period: Int) {
        /**
         * 启动2分钟后删一次，后面每隔 2 分钟进行次文件删除，压缩
         */
        if (period > 0) {
            taskPeriodMinute = period.toLong()
            timeCompressGap = (period * 60 * 1000).toLong()
        }
//        KLog.i(TAG, "period=$taskPeriodMinute,gap=$timeCompressGap")
        clearService.scheduleAtFixedRate(
            { rollCacheLogs(LogConfig.getLogPath()) }, 2, taskPeriodMinute,
            TimeUnit.MINUTES
        )
    }

    /**
     * LoginService结束并清除资源
     */
    fun end() {
        clearService.shutdown()
    }

    /**
     * 修改缓存log
     * 删除逾期以及压缩当天以外的log
     * 删除无效文件(超过7天过期文件，不是zip或者text结尾的异常文件)
     * 一天之内的log不压缩
     * @param logDir log缓存目录
     * */
    fun rollCacheLogs(logDir: String?) {
//        KLog.i(TAG, "rollCacheLogs $logDir")
        File(logDir ?: return).apply {
            if (!exists()) return else listFiles().filterNotNull().let { fileList ->
                fileList.forEach {
                    if (isInvalidFile(it)) {
                        it.delete()
                        KLog.i(TAG, "delete more 7Day and invaild file")
                        return@forEach
                    } else if (System.currentTimeMillis() - it.lastModified() > timeCompressGap) {
                        compressLog(it)
                    }
                }
            }
        }
        overflowCallback(logDir)
    }

    /**
     * 是否有效文件
     * 无效文件(超过7天过期文件，不是zip或者text结尾的异常文件, size<200b的zip文件)
     */
    private fun isInvalidFile(file: File): Boolean {
        return (System.currentTimeMillis() - file.lastModified()) > DAYS_DELETE ||
                !(file.name.endsWith(LogCompress.ZIP) || file.name.endsWith(LOG_EXT)) ||
                (file.name.endsWith(LogCompress.ZIP) && file.length() < 200)
    }

    /**
     * 压缩文件
     */
    @SuppressLint("SimpleDateFormat")
    private fun compressLog(file: File) {
        file.apply {
            if (name.endsWith(LOG_EXT) && !name.contains(
                    SimpleDateFormat(LOG_DATE_WITH_MINUTE_FORMAT_STR)
                        .format(Date())
                )
            ) {
                try {
                    LogCompress.compress(this)
                    delete()
                } catch (e: Exception) {
                    Log.w(TAG, "LogCompress", e)
                }
            }
        }
    }

    /**
     * log总大小溢出复查处理(删除时间最早的)
     */
    private fun overflowCallback(logDir: String?) {
        var size = 0L
        File(logDir ?: return).run {
            if (!exists()) {
                return
            }
            val child = listFiles()
            try {
                Arrays.sort(child, TimeComparator())
            } catch (e: Exception) {
                KLog.i(TAG, "overflowCallback ${e.message}")
            }
            child
        }.let {
            for (index in it) {
                if (size + index.length() > LogConfig.getLogCacheMaxSize()) {
                    index.delete()
                    KLog.i(TAG, "delete more 100M zip")
                } else {
                    size += index.length()
                }
            }
        }
    }


    fun compareFileTime(fileName1: String, fileName2: String): Int {
        val matcherMinute1: Matcher = PATTERN_WITH_MINUTE.matcher(fileName1)
        val matcherMinute2: Matcher = PATTERN_WITH_MINUTE.matcher(fileName2)
        return if (matcherMinute1.find() && matcherMinute2.find()) {
            val fileNameTime1 = matcherMinute1.group()
            val fileNameTime2 = matcherMinute2.group()
            com.lzx.klog.util.VersionUtil.compareTime(fileNameTime1, fileNameTime2)
        } else {
            0
        }
    }

    /**
     * 检查文件的文件名是否包含符合正则表达式的串
     *
     * @param file
     * @return
     */
    fun containsPattern(file: File): Boolean {
        val name = file.name
        val matcherMinute: Matcher = PATTERN_WITH_MINUTE.matcher(name)
        if (matcherMinute.find()) {
            return true
        } else {
            val matcher: Matcher = PATTERN.matcher(name)
            if (matcher.find()) {
                return true
            }
        }
        return false
    }

    fun findEndTimeFile(fileName: String?, endTime: String): Boolean {
        val matcherMinute: Matcher = PATTERN_WITH_MINUTE.matcher(fileName)
        return if (matcherMinute.find()) {
            val fileNameTime = matcherMinute.group()
//            fileNameTime.startsWith(endTime) || VersionUtil.compareTime(fileNameTime, endTime) == -1
            fileNameTime.startsWith(endTime)
        } else {
            false
        }
    }
}