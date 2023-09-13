//
// Created by deh001 on 2018/5/15.
//

#include "appender.h"
#include <stdio.h>

#define __STDC_FORMAT_MACROS

#include <inttypes.h>
#include <sys/mount.h>

#include <ctype.h>
#include <assert.h>

#include <unistd.h>
#include <zlib.h>

#include <string>
#include <algorithm>

#include "boost/bind.hpp"
#include "boost/iostreams/device/mapped_file.hpp"
#include "boost/filesystem.hpp"

#include "comm/thread/lock.h"
#include "comm/thread/condition.h"
#include "comm/thread/thread.h"
#include "comm/scope_recursion_limit.h"
#include "comm/bootrun.h"
#include "comm/tickcount.h"
#include "comm/autobuffer.h"
#include "comm/ptrbuffer.h"
#include "comm/xlogger/xloggerbase.h"
#include "comm/time_utils.h"
#include "comm/strutil.h"
#include "../comm/mmap_util_bk.h"
#include "comm/tickcount.h"
#include "comm/verinfo.h"

#include "log_buffer.h"

#include <sys/stat.h>
#include <sys/types.h>

#define LOG_EXT "txt"

// 对日志格式化
extern void log_formater(const XLoggerInfo* _info, const char* _logbody, PtrBuffer& _log);
// 打印到logcat
extern void ConsoleLog(const XLoggerInfo* _info, const char* _log);
extern void ConsoleLogShort(const char* _log);
extern void ConsoleLogShortVarar(const char* _tips_format, ...);

//同步还是异步写日志
static TAppenderMode sg_mode = kAppednerAsync;

// 日志目录
static std::string sg_logdir;
// mmap目录 mmap一般都保存在/data/data/xxx/files/log/里面
static std::string sg_mmapdir;
// 日志文件名前缀
static std::string sg_logfileprefix;

// 日志文件，文件操作时使用
static Mutex sg_mutex_log_file;

//当前打开的日志文件
static FILE* sg_logfile = NULL;
//上次打开的文件名称
static char sg_last_file_path[1024] = {0};

// 日志文件打开的时间
static time_t sg_openfiletime = 0;
// 当前的日志目录
static std::string sg_current_dir;
// buffer 操作锁
static Mutex sg_mutex_buffer_async;
// buffer写文件信号量
static Condition sg_cond_buffer_async;
// 日志缓存，写文件时先写入buff，再定期存入文件
static LogBuffer* sg_log_buff = NULL;
// 文件是否打开状态
static volatile bool sg_log_close = true;

// 是否打印日志到控制台，外部可设置
#ifdef DEBUG
static bool sg_consolelog_open = true;
#else
static bool sg_consolelog_open = false;
#endif
//日志文件切分大小，默认不切分
static uint64_t sg_max_file_size = 0; //

// 异步LOG写入线程
static void __async_log_thread();
static Thread sg_thread_async(&__async_log_thread);

// 缓存的大小
static const unsigned int kBufferBlockLength = 150 * 1024;
// 使用的mmap文件
static boost::iostreams::mapped_file sg_mmmap_file;

// log文件名包含年月日时分的名称 如log_2018_05_20_22_21
static std::string __make_logfilenameprefix(const timeval& _tv, const char* _prefix) {
    time_t sec = _tv.tv_sec;
    tm tcur = *localtime((const time_t*)&sec);
    
    char temp [64] = {0};
    snprintf(temp, 64, "_%d_%02d_%02d_%02d_%02d", 1900 + tcur.tm_year, 1 + tcur.tm_mon, tcur.tm_mday, tcur.tm_hour, tcur.tm_min);
    
    std::string filenameprefix = _prefix;
    filenameprefix += temp;
    
    return filenameprefix;
}

static bool __file_size_too_large(std::string logfilepath) {
    uint64_t filesize = 0;
    if (boost::filesystem::exists(logfilepath)) {
        filesize = boost::filesystem::file_size(logfilepath);
    } else {
        return true;
    }

    if (sg_max_file_size <= 0) {
        return false;
    }

    return filesize > sg_max_file_size;
}

// 生成一个文件名
static void __make_logfilename(const timeval& _tv, const std::string& _logdir, const char* _prefix, const std::string& _fileext, char* _filepath, unsigned int _len) {

    std::string logfilenameprefix = __make_logfilenameprefix(_tv, _prefix);

    std::string pathprefix = _logdir + "/" + logfilenameprefix;
    std::string logfilepath;
    logfilepath.clear();

    for (int i = 0; i < 100; ++i) // 一分钟最大文件数，避免不重复
    {
        std::string tmplogfilepath = pathprefix;
        char temp[16] = {0};
        snprintf(temp, 16, "_%d", i);
        if (i > 0)
        {
            tmplogfilepath += temp;
        }

        tmplogfilepath += ".";
        tmplogfilepath += _fileext;

        if (!boost::filesystem::exists(tmplogfilepath)) {
            if (logfilepath.empty()) { // 用当前文件
                logfilepath = tmplogfilepath;
                break;
            }

            if (!__file_size_too_large(logfilepath)) {
                break; // 还是用上一次的文件
            }

            logfilepath = tmplogfilepath; //用当前文件
            break;
        }

        logfilepath = tmplogfilepath;
    }

    strncpy(_filepath, logfilepath.c_str(), _len - 1);
    _filepath[_len - 1] = '\0';
}

// 打印日志到控制台
static void __writetips2console(const char* _tips_format, ...) {
    
    if (NULL == _tips_format) {
        return;
    }
    
    XLoggerInfo info;
    memset(&info, 0, sizeof(XLoggerInfo));
    
    char tips_info[LOG_MSG_MAX_LENGTH] = {0};
    va_list ap;
    va_start(ap, _tips_format);
    vsnprintf(tips_info, sizeof(tips_info), _tips_format, ap);
    va_end(ap);
    ConsoleLog(&info, tips_info);
}

//将data写入文件中
static bool __writefile(const void* _data, size_t _len, FILE* _file) {
    if (NULL == _file) {
        assert(false);
        return false;
    }

    long before_len = ftell(_file);
    if (before_len < 0) return false;

    // 写入文件，失败时，写入错误信息到文件
    if (1 != fwrite(_data, _len, 1, _file)) {
        int err = ferror(_file);

        __writetips2console("write file error:%d", err);

        ftruncate(fileno(_file), before_len);
        fseek(_file, 0, SEEK_END);

        char err_log[256] = {0};
        snprintf(err_log, sizeof(err_log), "\nwrite file error:%d\n", err);

        AutoBuffer tmp_buff;
        sg_log_buff->Write(err_log, strnlen(err_log, sizeof(err_log)), tmp_buff);

        fwrite(tmp_buff.Ptr(), tmp_buff.Length(), 1, _file);

        return false;
    }
    fflush(_file);

    return true;
}

// 打开日志文件
static bool __openlogfile(const std::string& _log_dir) {
    if (sg_logdir.empty()) return false;

    static time_t s_last_time = 0;

    struct timeval tv;
    gettimeofday(&tv, NULL);

    if (NULL != sg_logfile) {
        //  // 同一时间同一个文件并且已经打开的
        //  if (filetm.tm_year == tcur.tm_year && filetm.tm_mon == tcur.tm_mon &&
        //      filetm.tm_mday == tcur.tm_mday && sg_current_dir == _log_dir)
        //      return true;

        if (sg_last_file_path[0] == 0 || !__file_size_too_large(sg_last_file_path)) {
            return true;
        }

        fclose(sg_logfile);
        sg_logfile = NULL;
    }

    time_t now_time = tv.tv_sec;

    sg_openfiletime = tv.tv_sec;
    sg_current_dir = _log_dir;

    char logfilepath[1024] = {0};
    __make_logfilename(tv, _log_dir, sg_logfileprefix.c_str(), LOG_EXT, logfilepath , 1024);

    // 如果当前时间比最后一次写的文件时间还小的话，就用最后一次的文件，不新创建
    if (now_time < s_last_time) {
        sg_logfile = fopen(sg_last_file_path, "a");
        __writetips2console(" !!!! 2 - logfilepath = %s", logfilepath);
        if (NULL == sg_logfile) {
            __writetips2console("open file error:%d %s, path:%s", errno, strerror(errno), sg_last_file_path);
        }

        return NULL != sg_logfile;
    }
    // 打开文件
    sg_logfile = fopen(logfilepath, "a");

    if (NULL == sg_logfile) {
        __writetips2console("open file error:%d %s, path:%s", errno, strerror(errno), logfilepath);
    }

    memcpy(sg_last_file_path, logfilepath, sizeof(sg_last_file_path));
    s_last_time = now_time;

    return NULL != sg_logfile;
}
// 关闭文件
static void __closelogfile() {
    if (NULL == sg_logfile) return;

    sg_openfiletime = 0;
    fclose(sg_logfile);
    sg_logfile = NULL;
    memset(sg_last_file_path, 0, sizeof(sg_last_file_path));
}

// 保存data到文件中
static void __log2file(const void* _data, size_t _len) {
	if (NULL == _data || 0 == _len || sg_logdir.empty()) {
		return;
	}

	ScopedLock lock_file(sg_mutex_log_file);

    if (__openlogfile(sg_logdir)) {
        __writefile(_data, _len, sg_logfile);
//        if (kAppednerAsync == sg_mode) {
//            __closelogfile();
//        }
    }

    return;
}

// 存储内部警告信息到文件中
static void __writetips2file(const char* _tips_format, ...) {

    if (NULL == _tips_format) {
        return;
    }
    
    char tips_info[4096] = {0};
    va_list ap;
    va_start(ap, _tips_format);
    vsnprintf(tips_info, sizeof(tips_info), _tips_format, ap);
    va_end(ap);

    AutoBuffer tmp_buff;
    sg_log_buff->Write(tips_info, strnlen(tips_info, sizeof(tips_info)), tmp_buff);

    __log2file(tmp_buff.Ptr(), tmp_buff.Length());
}

// 异步定期从缓存存储到文件
static void __async_log_thread() {
    while (true) {

        ScopedLock lock_buffer(sg_mutex_buffer_async);

        if (NULL == sg_log_buff) break;

        AutoBuffer tmp;
        sg_log_buff->Flush(tmp);
        lock_buffer.unlock();

		if (NULL != tmp.Ptr())  __log2file(tmp.Ptr(), tmp.Length());

        if (sg_log_close) break;

        sg_cond_buffer_async.wait(15 * 60 *1000);
    }
}

// 同步写文件
static void __appender_sync(const XLoggerInfo* _info, const char* _log) {

    char temp[16 * 1024] = {0};     // tell perry,ray if you want modify size.
    PtrBuffer log(temp, 0, sizeof(temp));
    log_formater(_info, _log, log);

    AutoBuffer tmp_buff;
    if (!sg_log_buff->Write(log.Ptr(), log.Length(), tmp_buff))   return;

    __log2file(tmp_buff.Ptr(), tmp_buff.Length());
}

// 异步写日志
static void __appender_async(const XLoggerInfo* _info, const char* _log) {
    ScopedLock lock(sg_mutex_buffer_async);
    if (NULL == sg_log_buff) return;

    char temp[16*1024] = {0};       //tell perry,ray if you want modify size.
    PtrBuffer log_buff(temp, 0, sizeof(temp));
    log_formater(_info, _log, log_buff);

    if (sg_log_buff->GetData().Length() >= kBufferBlockLength*4/5) {
       int ret = snprintf(temp, sizeof(temp), "[F][ sg_buffer_async.Length() >= BUFFER_BLOCK_LENTH*4/5, len: %d\n", (int)sg_log_buff->GetData().Length());
       log_buff.Length(ret, ret);
    }

    // 写入缓存
    if (!sg_log_buff->Write(log_buff.Ptr(), (unsigned int)log_buff.Length())) return;

    // 判断是否超过大小需要存文件
    if (sg_log_buff->GetData().Length() >= kBufferBlockLength*1/3) {
       sg_cond_buffer_async.notifyAll();
    }

}

// 写log接口
void xlogger_appender(const XLoggerInfo* _info, const char* _log) {
    if (sg_log_close) return;

    DEFINE_SCOPERECURSIONLIMIT(recursion);
    static Tss s_recursion_str(free);
    // 写入到控制台
    if (sg_consolelog_open) ConsoleLog(_info,  _log);

    // 预防循环调用，可能出现在打印log中又调用了打印LOG等
    if (2 <= (int)recursion.Get() && NULL == s_recursion_str.get()) {
        if ((int)recursion.Get() > 10) return;
        char* strrecursion = (char*)calloc(16 * 1024, 1);
        s_recursion_str.set((void*)(strrecursion));

        XLoggerInfo info = *_info;
        info.level = kLevelFatal;

        char recursive_log[256] = {0};
        snprintf(recursive_log, sizeof(recursive_log), "ERROR!!! xlogger_appender Recursive calls!!!, count:%d", (int)recursion.Get());

        PtrBuffer tmp(strrecursion, 0, 16*1024);
        log_formater(&info, recursive_log, tmp);

        strncat(strrecursion, _log, 4096);
        strrecursion[4095] = '\0';

        ConsoleLog(&info,  strrecursion);
    } else {
        if (NULL != s_recursion_str.get()) {
            char* strrecursion = (char*)s_recursion_str.get();
            s_recursion_str.set(NULL);

            __writetips2file(strrecursion);
            free(strrecursion);
        }

        // 写log，同步或者异步
        if (kAppednerSync == sg_mode)
            __appender_sync(_info, _log);
        else
            __appender_async(_info, _log);
    }
}
// 座位文件头，每次打开时，写入进程线程等信息
static void get_mark_info(char* _info, size_t _infoLen) {
	struct timeval tv;
	gettimeofday(&tv, 0);
	time_t sec = tv.tv_sec; 
	struct tm tm_tmp = *localtime((const time_t*)&sec);
	char tmp_time[64] = {0};
	strftime(tmp_time, sizeof(tmp_time), "%Y-%m-%d %H:%M:%S", &tm_tmp);
	snprintf(_info, _infoLen, "[pid = %" PRIdMAX ", tid = %" PRIdMAX "] %s ", xlogger_pid(), xlogger_tid(), tmp_time);
}

// 打开日志读写，包括创建日志文件，创建日志缓存
void appender_open(TAppenderMode _mode, const char* _dir, const char* _mmapdir, const char* _nameprefix, const char* _pub_key) {
	assert(_dir);
	assert(_mmapdir);
	assert(_nameprefix);

    sg_logdir = _dir;
    sg_mmapdir = _mmapdir;

//    ConsoleLogShortVarar( "key is %s ", _pub_key);

    // 已经打开
    if (!sg_log_close) {
        __writetips2file("\nlogfile has already been opened. _dir:%s _nameprefix:%s\n", _dir, _nameprefix);
        return;
    }

    //返回值为old appender指针，不需要用到，暂时忽略
    xlogger_SetAppender(&xlogger_appender);

    // 创建日志目录
	mkdir(_dir, S_IRWXU|S_IRWXG|S_IRWXO);
    mkdir(_mmapdir, S_IRWXU|S_IRWXG|S_IRWXO);
//	boost::filesystem::create_directories(_dir);
//	boost::filesystem::create_directories(_mmapdir);

    // mmap文件名
    char mmap_file_path[512] = {0};
    snprintf(mmap_file_path, sizeof(mmap_file_path), "%s/%s.mmap2", _mmapdir, _nameprefix);

    // 打开mmap，打开失败则使用内存做缓存
    bool use_mmap = false;
    if (OpenMmapFile(mmap_file_path, kBufferBlockLength, sg_mmmap_file))  {
        sg_log_buff = new LogBuffer(sg_mmmap_file.data(), kBufferBlockLength, false, _pub_key);
        use_mmap = true;
    } else {
        char* buffer = new char[kBufferBlockLength];
        memset(buffer, 0, kBufferBlockLength);
        sg_log_buff = new LogBuffer(buffer, kBufferBlockLength, false, _pub_key);
        use_mmap = false;
    }

    if (NULL == sg_log_buff->GetData().Ptr()) {
        if (use_mmap && sg_mmmap_file.is_open())  CloseMmapFile(sg_mmmap_file);
        return;
    }

    AutoBuffer buffer;
    sg_log_buff->Flush(buffer);

	ScopedLock lock(sg_mutex_log_file);
	sg_logdir = _dir;
	sg_logfileprefix = _nameprefix;
	sg_log_close = false;
	appender_setmode(_mode);
    lock.unlock();

    // 文件头
    char mark_info[512] = {0};
    get_mark_info(mark_info, sizeof(mark_info));

    // 如果上次mmap存在没有写完的内容，继续降上次的写入
    if (buffer.Ptr()) {
        __writetips2file("begin of last log \n");
        __log2file(buffer.Ptr(), buffer.Length());
        __writetips2file("\nend of last log \n");
    }

    char appender_info[728] = {0};
    snprintf(appender_info, sizeof(appender_info), "\n%s start", mark_info);
    xlogger_appender(NULL, appender_info);

    char logmsg[64] = {0};
    xlogger_appender(NULL, "LOG_JNI_REVISION: " LOG_JNI_REVISION " [" __DATE__ " " __TIME__ "]");
    snprintf(logmsg, sizeof(logmsg), "logfile mode:%d, use mmap:%d", (int)_mode, use_mmap);
    xlogger_appender(NULL, logmsg);

	BOOT_RUN_EXIT(appender_close);
}
// 异步刷新，通知另外一个线程写
void appender_flush() {
    sg_cond_buffer_async.notifyAll();
}

// 同步刷缓存日志到文件
void appender_flush_sync() {
    if (kAppednerSync == sg_mode) { // 原本同步写的，就不用刷了
        return;
    }

    ScopedLock lock_buffer(sg_mutex_buffer_async);
    
    if (NULL == sg_log_buff) return;
    //读取全部缓存内容写入文件
    AutoBuffer tmp;
    sg_log_buff->Flush(tmp);
    lock_buffer.unlock();
	if (tmp.Ptr())  __log2file(tmp.Ptr(), tmp.Length());

}

//关闭日志文件
void appender_close() {
    // 已关闭
    if (sg_log_close) return;

    // 记录单次日志的开始与结束
    char mark_info[512] = {0};
    get_mark_info(mark_info, sizeof(mark_info));
    char appender_info[728] = {0};
    snprintf(appender_info, sizeof(appender_info), "%s stop\n", mark_info);
    xlogger_appender(NULL, appender_info);

    sg_log_close = true;

    // 通知异步线程，写日志
    sg_cond_buffer_async.notifyAll();
    // 等待写日志线程执行完
    if (sg_thread_async.isruning())
        sg_thread_async.join();

	// mmmap关闭，如果是内存缓存则释放
    ScopedLock buffer_lock(sg_mutex_buffer_async);
    if (sg_mmmap_file.is_open()) {
        if (!sg_mmmap_file.operator !()) memset(sg_mmmap_file.data(), 0, kBufferBlockLength);

		CloseMmapFile(sg_mmmap_file);
    } else {
        delete[] (char*)((sg_log_buff->GetData()).Ptr());
    }

    delete sg_log_buff;
    sg_log_buff = NULL;
    buffer_lock.unlock();

    //关闭文件
    ScopedLock lock(sg_mutex_log_file);
	__closelogfile();
}

// 同步或者异步保存日志到文件，异步会开启异步线程
void appender_setmode(TAppenderMode _mode) {
    sg_mode = _mode;

    sg_cond_buffer_async.notifyAll();

    if (kAppednerAsync == sg_mode && !sg_thread_async.isruning()) {
        sg_thread_async.start();
    }
}

// 设置是否打印控制台LOG
void appender_set_console_log(bool _is_open) {
    sg_consolelog_open = _is_open;
}

//设置单个文件的最大大小，超过就进行文件切分
void appender_set_max_file_size(uint64_t _max_byte_size) {
    sg_max_file_size = _max_byte_size;
}
