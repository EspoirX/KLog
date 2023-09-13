//
// Created by deh001 on 2018/5/15.
// 日志格式化
//

#include <assert.h>
#include <stdio.h>
#include <limits.h>
#include <algorithm>

#include "comm/xlogger/xloggerbase.h"
#include "comm/xlogger/loginfo_extract.h"
#include "comm/ptrbuffer.h"

#define __STDC_FORMAT_MACROS
#include <inttypes.h>

// 日志的格式化
void log_formater(const XLoggerInfo* _info, const char* _logbody, PtrBuffer& _log) {
    static const char* levelStrings[] = {
        "U",
        "V",
        "D",  // debug
        "I",  // info
        "W",  // warn
        "E",  // error
        "F"  // fatal
    };

    assert((unsigned int)_log.Pos() == _log.Length());

    static int error_count = 0;
    static int error_size = 0;

    // buffer空间不足了，写入提示信息，返回
    if (_log.MaxLength() <= _log.Length() + 5 * 1024) {
        ++error_count;
        error_size = (int)strnlen(_logbody, 1024 * 1024);

        if (_log.MaxLength() >= _log.Length() + 128) {
            int ret = snprintf((char*)_log.PosPtr(), 1024, "[F]log_size <= 5*1024, err(%d, %d)\n", error_count, error_size);
            _log.Length(_log.Pos() + ret, _log.Length() + ret);
            _log.Write("");

            error_count = 0;
            error_size = 0;
        }

        assert(false);
        return;
    }

    // 格式化日志
    if (NULL != _info) {
        char temp_time[64] = {0};

        if (0 != _info->timeval.tv_sec) {
            time_t sec = _info->timeval.tv_sec;
            tm tm = *localtime((const time_t*)&sec);
            snprintf(temp_time, sizeof(temp_time), "%02d:%02d:%02d.%.3ld", tm.tm_hour, tm.tm_min, tm.tm_sec, _info->timeval.tv_usec / 1000);
        }

        int ret = snprintf((char*)_log.PosPtr(), 1024, "%s %" PRIdMAX "-%" PRIdMAX "(%s) %s/%s ",
                           temp_time, _info->pid, _info->tid, _info->threadname,
                           _logbody ? levelStrings[_info->level] : levelStrings[kLevelFatal],
                           _info->tag ? _info->tag : "");

        assert(0 <= ret);
        _log.Length(_log.Pos() + ret, _log.Length() + ret);

        assert((unsigned int)_log.Pos() == _log.Length());
    }

    // 将日志保存到_log 缓存中
    if (NULL != _logbody) {
        size_t bodylen =  _log.MaxLength() - _log.Length() > 130 ? _log.MaxLength() - _log.Length() - 130 : 0;
        bodylen = bodylen > 0xFFFFU ? 0xFFFFU : bodylen;
        bodylen = strnlen(_logbody, bodylen);
        bodylen = bodylen > 0xFFFFU ? 0xFFFFU : bodylen;
        _log.Write(_logbody, bodylen);
    } else {
        _log.Write("error!! NULL==_logbody");
    }

    char nextline = '\n';

    if (*((char*)_log.PosPtr() - 1) != nextline) _log.Write(&nextline, 1);
}

