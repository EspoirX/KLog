package com.lzx.klog.util

import com.lzx.klog.api.KLog
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogCompress {
    private const val TAG = "LogCompress"

    const val ZIP = ".zip"
    private const val TEMP = ".tmp"
    private const val BUFFER = 1024

    /**
     * 压缩
     * @param srcFile
     * @throws Exception
     *
     */
    fun compress(srcFile: File) {
        val name = srcFile.name
        val basePath = srcFile.parent
        val destPath = "$basePath${File.separator}${name.substring(0, name.indexOf(".txt"))}$TEMP"
        compress(null, srcFile, destPath)
        renameZip(
            File(destPath),
            File("$basePath${File.separator}${name.substring(0, name.indexOf(".txt"))}$ZIP")
        )
    }

    /**
     * 压缩文件
     *
     * @param srcFile
     * @param destPath
     * @throws Exception
     */
    private fun compress(output: ZipOutputStream? = null, srcFile: File, destPath: String) {
        val destFile = File(destPath)
        val zos = output ?: ZipOutputStream(FileOutputStream(destFile))
        try {
            compress(srcFile, zos, srcFile.name)
        } catch (e: Exception) {
            KLog.e(TAG, "compressFile = " + e.message)
        } finally {
            zos.flush()
            zos.close()
        }
    }

    /**
     * @param sourceFile
     * 待压缩文件
     * @param zos
     * ZipOutputStream
     * @param name
     * 压缩后的名称
     * @throws Exception
     *
     * 递归压缩文件
     */
    @Throws(Exception::class)
    private fun compress(sourceFile: File, zos: ZipOutputStream, name: String) {
        if (sourceFile.isFile) {
            zos.putNextEntry(ZipEntry(name))
            val bis = BufferedInputStream(FileInputStream(sourceFile))
            var count = 0
            val data = ByteArray(BUFFER)
            while (bis.read(data, 0, BUFFER).apply { count = this } != -1) {
                zos.write(data, 0, count)
            }
            bis.close()
            zos.closeEntry()
        } else {
            val listFiles = sourceFile.listFiles()
            if (listFiles == null || listFiles.isEmpty()) {
                // 空文件夹的处理
                zos.putNextEntry(ZipEntry("$name${File.separator}"))
                zos.closeEntry()
            } else {
                listFiles.forEach { compress(it, zos, "$name${File.separator}${it.name}") }
            }
        }
    }

    /**
     * @param tmpFile 临时压缩文件
     * @param zipFile 压缩文件
     * 重命名压缩临时文件到zip文件
     */
    private fun renameZip(tmpFile: File, zipFile: File) {
        if (tmpFile.exists()) {
            if (zipFile.exists()) {
                zipFile.delete()
            }
            tmpFile.renameTo(zipFile)
        }
    }
}