package com.lzx.klog.writer;

/**
 * Created by deh001 on 2018/5/16
 * 对应native中方法，通过mmap写日志到文件中
 */
public class FileLog {

    public static final int MODE_ASYNC = 0;
    public static final int MODE_SYNC = 1;

    public static native void open(String logDir, String mmapDir, String nameprefix, int logLevel, int mode,
                                   String publicKey, boolean isCrypt);

    public static native void flush(boolean sync);

    public static native void close();

    public static native void level(int logLevel);

    public static native void mode(int mode);

    public static native void fileMaxSize(int size);

    public static native void useConsoleLog(boolean use);

    public static native void logWrite(int level, String tag, String fileName, String funcName, int line, int pid,
                                       long tid, long maintid, String ms, String threadName);
}
