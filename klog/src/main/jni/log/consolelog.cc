//
// Created by deh001 on 2018/5/15.
// logcat打印日志
//

#include <stdio.h>
#include <android/log.h>
#include <cstring>

#include "comm/xlogger/xloggerbase.h"
#include "comm/xlogger/loginfo_extract.h"


//这里不能加日志，会导致循环调用
void ConsoleLog(const XLoggerInfo* _info, const char* _log) {
	char result_log[LOG_MSG_MAX_LENGTH];
	memset(result_log, 0, LOG_MSG_MAX_LENGTH);
    if (_info) {
        char strFuncName[128];
        memset(strFuncName, 0, 128);
        ExtractFunctionName(_info->func_name, strFuncName, sizeof(strFuncName));

        snprintf(result_log, LOG_MSG_MAX_LENGTH, "%s", _log?_log:"NULL==logmsg!!!");
        __android_log_write(_info->level+1, _info->tag?_info->tag:"", (const char*)result_log);
    } else {
    	snprintf(result_log, LOG_MSG_MAX_LENGTH, "%s", _log?_log:"NULL==logmsg!!!");
        __android_log_write(ANDROID_LOG_WARN, "", (const char*)result_log);
    }
    
}

void ConsoleLogShort(const char* _log) {
    char result_log[LOG_MSG_MAX_LENGTH];
    memset(result_log, 0, LOG_MSG_MAX_LENGTH);
    snprintf(result_log, LOG_MSG_MAX_LENGTH, "%s", _log?_log:"NULL==logmsg!!!");
    __android_log_write(ANDROID_LOG_DEBUG, "ConsoleLog", (const char*)result_log);
}

// 打印日志到控制台
void ConsoleLogShortVarar(const char* _tips_format, ...) {
    if (NULL == _tips_format) {
        return;
    }
    char tips_info[LOG_MSG_MAX_LENGTH];
    memset(tips_info, 0, LOG_MSG_MAX_LENGTH);
    va_list ap;
    va_start(ap, _tips_format);
    vsnprintf(tips_info, sizeof(tips_info), _tips_format, ap);
    va_end(ap);
    ConsoleLogShort(tips_info);
}