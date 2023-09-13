//
// Created by deh001 on 2018/5/14.
//

#ifndef __YYFRAMEWORK_ILOG_H__
#define __YYFRAMEWORK_ILOG_H__

/**
 * log native层对外接口
 * 使用
 *      1 如果在代码中直接调用打印日志，不做处理，建议使用Log_* 如：
 *          Log_v("tag", "this is a test log")
 *          Log_i("tag", "my name is %s", "xxx")
 *
 *      2 如果调用自己日志接口，并且有可能跨线程再调用到打印日志，请使用 ILog相关接口，传入线程ID等，否则可能打印错误的线程ID
 *          NLog().v("tag", tid, "this is a test log")
 *          NLog().i("tag", tid, "my name is %s", "xxx")
 */
class ILog {
public:
    virtual ~ILog() {}

public:
    virtual void v(const char* tag, int tid, const char* format, ...) = 0;

    virtual void i(const char* tag, int tid, const char* format, ...) = 0;

    virtual void d(const char* tag, int tid, const char* format, ...) = 0;

    virtual void w(const char* tag, int tid, const char* format, ...) = 0;

    virtual void e(const char* tag, int tid, const char* format, ...) = 0;
};

extern "C" {

extern ILog *NLog();

}

#define Log_v(tag, format, ...)   NLog()->v(tag, -1, format, ##__VA_ARGS__)
#define Log_i(tag, format, ...)   NLog()->i(tag, -1, format, ##__VA_ARGS__)
#define Log_d(tag, format, ...)   NLog()->d(tag, -1, format, ##__VA_ARGS__)
#define Log_w(tag, format, ...)   NLog()->w(tag, -1, format, ##__VA_ARGS__)
#define Log_e(tag, format, ...)   NLog()->e(tag, -1, format, ##__VA_ARGS__)

#endif // __YYFRAMEWORK_ILOG_H__
