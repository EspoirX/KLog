package com.lzx.klog.api

import java.io.File

/**
 * Log对外服务，提供log初始化、获取文件log列表及flush所有log的功能
 */
interface ILogService {

    /**
     * 当前的LOG配置，可获取进行修改，修改完成后调用apply生效
     * @return 返回当前配置实例
     */
    fun config(): ILogConfig

    /**
     * 获取log文件所在的目录
     * @return log文件目录
     */
    fun catalog(): String?

    /**
     * 获取当前APP文件log列表
     * @return log文件列表，按时间倒序排列
     */
    fun fileLogList(): Array<File>

    /**
     * 获取当前APP指定进程标识的文件log列表
     * @param processId log进程标识
     * @return log文件列表，按时间倒序排列
     */
    fun fileLogList(processId: String): Array<File>

    /**
     * 异步flush缓存，确保缓存的LOG全部输出完毕
     */
    fun flush()

    /**
     * 异步flush缓存，带回调
     * */
    fun flush(flushCallback: IKLogFlush)

    /**
     * 同步阻塞等待flush执行完毕
     */
    fun flushBlocking(milliseconds: Long)


    /**
     * 提交日志
     * fileName 默认 uuid的md5
     * fileNum:提交日期数量，默认7
     * endTime:提交指定日期日志，格式 yyyy_MM_dd
     */
    fun submitLog(fileName: String?, fileNum: Int = 7, endTime: String = "", listener: OnSubmitLogListener?)
}

interface OnSubmitLogListener {
    /**
     * zipFilePath 日志压缩包路径
     */
    fun onSubmitLog(zipFilePath: String)
}