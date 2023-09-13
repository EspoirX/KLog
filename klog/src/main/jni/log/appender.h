//
// Created by deh001 on 2018/5/15.
// 负责写日志
//

#ifndef __YYFRAMEWORK_LOG_APPENDER_H__
#define __YYFRAMEWORK_LOG_APPENDER_H__

#include <string>
#include <vector>
#include <stdint.h>

// 同步或异步，同步将会每次都刷新内存数据到文件，仅限于debug使用或者完全不使用
enum TAppenderMode
{
    kAppednerAsync,
    kAppednerSync,
};

// 打开日志文件
void appender_open(TAppenderMode _mode, const char* _dir, const char* _mmapdir,
        const char* _nameprefix, const char* _pub_key, const bool isCrypt);

// 刷新缓存到文件中
void appender_flush();

// 刷新缓存到文件中 同步
void appender_flush_sync();

// 关闭
void appender_close();

// 同步保存文件还是异步
void appender_setmode(TAppenderMode _mode);

// 是否打印日志到logcat
void appender_set_console_log(bool _is_open);

// 文件分割的大小，超过会建新文件
void appender_set_max_file_size(uint64_t _max_byte_size);

bool get_is_crypt();

#endif // __YYFRAMEWORK_LOG_APPENDER_H__
