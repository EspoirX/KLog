//
// Created by deh001 on 2018/5/16.
// native写日志实现
//

#include <stdarg.h>
#include "inc/ilog.h"
#include "comm/xlogger/xlogger.h"

class LogImpl : public ILog {
	virtual void v(const char* tag, int tid, const char* format, ...) {
        va_list  va;
        va_start(va, format);
        log(kLevelVerbose, tag, tid, format, va);
        va_end(va);
	}

	virtual void i(const char* tag, int tid, const char* format, ...) {
        va_list  va;
        va_start(va, format);
        log(kLevelInfo, tag, tid, format, va);
        va_end(va);
	}

	virtual void d(const char* tag, int tid, const char* format, ...) {
        va_list  va;
        va_start(va, format);
        log(kLevelDebug, tag, tid, format, va);
        va_end(va);
	}

	virtual void w(const char* tag, int tid, const char* format, ...) {
        va_list  va;
        va_start(va, format);
        log(kLevelWarn, tag, tid, format, va);
        va_end(va);
	}

	virtual void e(const char* tag, int tid, const char* format, ...) {
        va_list  va;
        va_start(va, format);
        log(kLevelError, tag, tid, format, va);
        va_end(va);
	}

    void log(TLogLevel leve, const char* tag, int tid,
             const char* format, va_list list) {
        char buff[LOG_MSG_MAX_LENGTH] = {0};
        vsnprintf(buff, LOG_MSG_MAX_LENGTH, format, list);
        xlogger_Write(leve, 0, xlogger_pid(), tid, xlogger_maintid(), tag, "", "", buff);
    }
};

ILog* s_Log = NULL;

ILog *NLog() {
    return s_Log;
}

void initNativeLogApi() {
    s_Log = new LogImpl();
}

void uninitNativeLogApi() {
    delete s_Log;
    s_Log = NULL;
}