package com.lzx.klog.api

/**
 * @time 2018/5/12 16:00
 * 提供给外面的ILog的配置信息
 */
interface ILogConfig {

    /**
     * 指定log进程标识, 最终的日志文件名以此为前缀,如传入ylog,则文件名为ylog_2018_05_26_16_53.txt
     */
    fun processTag(processTag: String): ILogConfig

    /**
     * 设置log等级，超过指定等级的log将会被输出
     */
    fun logLevel(level: Int): ILogConfig

    /**
     * 设置是否输出logcat，默认APP debug版本输出，release版本不输出
     */
    fun logcat(visible: Boolean): ILogConfig

    /**
     * 设置单个log文件的大小，当达到该大小时，自动执行文件切分
     */
    fun singleLogMaxSize(maxSize: Int): ILogConfig

    /**
     * 设置log缓存目录最大值
     * */
    fun logCacheMaxSiz(maxSize: Long): ILogConfig

    /**
     * 设置写log的文件目录
     */
    fun logPath(path: String?): ILogConfig

    /**
     * 指定用于加密的publickey， 如果不加密可以不设置，默认为空
     */
    fun publicKey(key: String): ILogConfig

    /**
     * 指定字段海外版本不需要加密
     */
    fun isCrypt(crypt: Boolean): ILogConfig

    /**
     * 设置线程优先级
     */
    fun setPriority(prority: Int): ILogConfig

    /**
     * 获取线程优先级
     */
    fun getPriority(): Int

    /**
     * log压缩周期
     */
    fun logCompressPeriod(minute: Int): ILogConfig

    /**
     * 使当前设置生效
     */
    fun apply()
}
