package com.lzx.klog.util

import java.io.File
import java.util.regex.Pattern

/**
 *  按时间倒序排序
 */
class TimeComparator : Comparator<File> {

    private val pattern = Pattern.compile("(19|20)\\d{2}_[0-1][0-9]_[0-9]{2}_[0-9]{2}_[0-9]{2}")
    override fun compare(lhs: File?, rhs: File?): Int {

        val lInValid = (lhs == null || !lhs.exists())
        val rInValid = (rhs == null || !rhs.exists())
        val bothInValid = lInValid && rInValid
        if (bothInValid) {
            return 0
        }

        if (lInValid) {
            return 1
        }

        if (rInValid) {
            return -1
        }

        val rhsTime = rhs?.lastModified()!!
        val lhsTime = lhs?.lastModified()!!

        return rhsTime.compareTo(lhsTime)
//        val lrhs = getMatcher(rhs.name)
//        val llhs = getMatcher(lhs.name)
//        return lrhs.compareTo(llhs)
    }

    private fun getMatcher(source: String): Long {
        var result = 0L
        val matcher = pattern.matcher(source)
        while (matcher.find()) {
            result = ((matcher.group()).replace("_", "")).toLong()
        }
        return result
    }
}