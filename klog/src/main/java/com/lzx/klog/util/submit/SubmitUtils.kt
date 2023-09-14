package com.lzx.klog.util.submit

import android.annotation.SuppressLint
import android.util.Log
import com.lzx.klog.api.ILogService
import com.lzx.klog.api.KLog
import com.lzx.klog.api.OnSubmitLogListener
import com.lzx.klog.impl.LogConfig
import com.lzx.klog.util.LogManager
import com.lzx.klog.util.Md5Util
import com.lzx.klog.util.SafeIoUtils
import com.lzx.klog.util.compress.LogZipCompress
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object SubmitUtils {

    private var UPLOAD_FILE_SIZE = (15 * 1024 * 1024).toLong()

    fun getLatestNLogsZip(logService: ILogService, num: Int, endTime: String): MutableList<String> {
        var fileNum = num
        val result = LinkedList<String>()
        val fileListOld = logService.fileLogList()
        val sb = StringBuilder()
        sb.append("\n")

        val fileList = mutableListOf<File>()
        for (file in fileListOld) {
            fileList.add(file)
        }

        Collections.sort(fileList, object : Comparator<File> {

            override fun equals(other: Any?): Boolean {
                return super.equals(other)
            }

            override fun compare(o1: File, o2: File): Int {
                return -LogManager.compareFileTime(o1.name.orEmpty(), o2.name.orEmpty())
            }
        })

        var findFirstFile = true //当前在编辑的文件不压缩
        fileList.forEach { file ->
            KLog.i("KLog", "###getLastestNLogsZip " + file.name)
            sb.append("    log file: ").append(file.absolutePath).append("\n")
            if (file.name.endsWith(".txt") && file.name != "pushsvc_log.txt" &&
                file.name != "state.txt"
            ) {
                if (findFirstFile && file.name.endsWith(".txt")) {
                    findFirstFile = false
                    val userLogFilePath = file.absolutePath
                    KLog.i("KLog", "userLogFilePath =  $userLogFilePath")
                } else {
                    try {
                        LogZipCompress.getInstance().compress(file)
                        file.delete()
                    } catch (e: Exception) {
                        KLog.i("KLog", "printStackTrace " + e.message)
                    }
                }
            }
        }
        KLog.i("KLog", "all log files: $sb")
        //KLog end
        if (fileNum < 1) {
            return result
        }

        val dir: String = LogConfig.getLogPath().orEmpty()
        if (dir.isNotEmpty()) {
            val dirFile = File(dir)
            if (dirFile.exists() && dirFile.isDirectory) {
                val files = dirFile.listFiles()
                val filesArr = ArrayList<File>()
                files?.forEach {
                    if (LogManager.containsPattern(it)) {
                        filesArr.add(it)
                    }
                }
                if (filesArr.size > 0) {
                    Collections.sort(filesArr, object : Comparator<File> {
                        override fun compare(o1: File, o2: File): Int {
                            return -LogManager.compareFileTime(o1.name.orEmpty(), o2.name.orEmpty())
                        }

                        override fun equals(other: Any?): Boolean {
                            return true
                        }
                    })
                }
                var allLen = 0
                var startCount = true
                if (endTime.isNotEmpty()) {
                    startCount = false
                }
                for (file in filesArr) {
                    if (fileNum <= 0) {
                        break
                    }
                    if (!startCount && LogManager.findEndTimeFile(file.name, endTime)) {
                        startCount = true
                    }
                    if (startCount) {
                        if (!file.name.endsWith(".txt")) { //txt文件不算入文件大小，防止一个txt文件已经超限制
                            if (allLen + file.length() > UPLOAD_FILE_SIZE) {
                                KLog.i(
                                    "KLog", "log file over size total size=" + UPLOAD_FILE_SIZE +
                                            " fileNum = " + fileNum +
                                            "cur=" + allLen + " ,new size=" + file.length()
                                )
                                break
                            }
                            allLen += file.length().toInt()
                        }
                        result.add(file.absolutePath)
                        fileNum--
                    }
                }
            }
        }
        KLog.i("KLog", "getLastestNLogsZip first size = " + result.size)
        return result
    }

    private var sDumpDirectory: String? = null

    private fun getDumpDirectory(): String? {
        if (sDumpDirectory != null) {
            return sDumpDirectory
        }
        val tempFile = File(LogConfig.getLogPath() + "/feedback")
        if (!tempFile.exists()) {
            tempFile.mkdirs()
        }
        sDumpDirectory = tempFile.absolutePath
        return sDumpDirectory
    }

    fun submitFeedback(logList: MutableList<String>, listener: OnSubmitLogListener?) {
        var uuid = UUID.randomUUID().toString()
        uuid = Md5Util.getMD5(uuid)
        val zipFilePath = (getDumpDirectory() + File.separator + uuid) + ".zip"

        val fileList = mutableListOf<String>()

        if (logList.isNotEmpty()) {
            fileList.addAll(logList)
        }

        val outZip: ZipOutputStream?
        if (fileList.size > 0) {
            outZip = try {
                ZipOutputStream(FileOutputStream(zipFilePath))
            } catch (e: Exception) {
                KLog.i("KLog", String.format("zipFilePath open file=%s error= %s", zipFilePath, e.message))
                return
            }
            var allLen: Long = 0
            val zipEntryList = mutableListOf<String>()
            run breaking@{
                fileList.forEach continuing@{ fileName ->
                    val tmpFile: File
                    val inputStream: FileInputStream
                    try {
                        KLog.i("KLog", "uploadReport file = $fileName")
                        if (zipEntryList.contains(fileName)) {
                            KLog.i("KLog", "duplicate entry: $fileName")
                            return@continuing
                        }
                        tmpFile = File(fileName)
                        inputStream = FileInputStream(tmpFile)
                    } catch (e: java.lang.Exception) {
                        Log.e("KLog", "uploadReport file not exist : $fileName")
                        return@continuing
                    }

                    if (fileName.endsWith("dmp")) {
                        Log.e("KLog", "fileName = $fileName")
                    } else if (allLen + tmpFile.length() > UPLOAD_FILE_SIZE) {
                        SafeIoUtils.safeClose(inputStream)
                        KLog.i(
                            "KLog", "zip over size " +
                                    "total size=" + UPLOAD_FILE_SIZE +
                                    "cur=" + allLen + " ,new size=" + tmpFile.length()
                        )
                        return@continuing
                    }

                    allLen += tmpFile.length()

                    val zipEntry = ZipEntry(tmpFile.name)
                    try {
                        zipEntryList.add(fileName)
                        outZip?.putNextEntry(zipEntry)
                        var len: Int
                        val buffer = ByteArray(10 * 1024)
                        while (inputStream.read(buffer).also { len = it } != -1) {
                            outZip?.write(buffer, 0, len)
                        }
                        inputStream.close()
                        outZip?.closeEntry()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        KLog.i("KLog", e.message.toString())
                    }
                }

                try {
                    outZip?.flush()
                    outZip?.finish()
                    outZip?.close()
                } catch (e: Exception) {
                    KLog.i("KLog", "outZip close ERROR:" + e.message)
                    return
                }
                listener?.onSubmitLog(zipFilePath)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val dft = SimpleDateFormat("yyyy_MM_dd")

    fun getOldDate(distanceDay: Int): String {
        val beginDate = Date()
        val date = Calendar.getInstance()
        date.time = beginDate
        date.set(Calendar.DATE, date.get(Calendar.DATE) + distanceDay)
        var endDate: Date? = null
        try {
            endDate = dft.parse(dft.format(date.time))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return if (endDate == null) "" else dft.format(endDate)
    }
}