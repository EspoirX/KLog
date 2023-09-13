//
// Created by deh001 on 2018/5/15.
// 日志缓存，优先使用mmap，如果失败的话就存在内存中
//

#ifndef __YYFRAMEWORK_LOG_LOGBUFFER_H__
#define __YYFRAMEWORK_LOG_LOGBUFFER_H__

#include <zlib.h>
#include <string>
#include <stdint.h>

#include "comm/ptrbuffer.h"
#include "comm/autobuffer.h"

class LogCrypt;

// log缓存，可能对应mmmap，也可能对应内存
class LogBuffer {
public:
    LogBuffer(void* _pbuffer, size_t _len, bool _is_compress, const char* _pubkey);
    ~LogBuffer();

public:
    static bool GetPeriodLogs(const char* _log_path, int _begin_hour, int _end_hour, unsigned long& _begin_pos, unsigned long& _end_pos, std::string& _err_msg);

public:
    PtrBuffer& GetData();

    void Flush(AutoBuffer& _buff);
    bool Write(const void* _data, size_t _inputlen, AutoBuffer& _out_buff);
    bool Write(const void* _data, size_t _length);

    bool WriteToAutoBuffer(const void* _data, size_t _inputlen, AutoBuffer& _out_buff);

private:

    bool __Reset();
    void __Flush();
    void __Clear();

    void __Fix();

private:
    PtrBuffer buff_; //已写有内容的buffer
    bool is_compress_;
    z_stream cstream_; //zlib 压缩
//    void* len_ptr_;

    class LogCrypt* log_crypt_; //加密
    size_t remain_nocrypt_len_; //因为tea加密都是针对32位进行加密，_remain_nocrypt_len = _input_len % TEA_BLOCK_LEN
};


#endif // __YYFRAMEWORK_LOG_LOGBUFFER_H__
