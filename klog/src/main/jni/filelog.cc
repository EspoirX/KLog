//
// Created by deh001 on 2018/5/16.
// jni接口
//

#include <jni.h>
#include <vector>
#include <string>

#include "comm/xlogger/xlogger.h"
#include "comm/jni/util/scoped_jstring.h"
#include "log/appender.h"

extern "C" {

// 打开log
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_open
	(JNIEnv *env, jclass, jstring _log_dir, jstring _mmap_dir,jstring _nameprefix,
			jint level, jint mode, jstring _pubkey, jboolean isCrypt) {
	if (NULL == _log_dir || NULL == _mmap_dir || NULL == _nameprefix) {
		return;
	}

	ScopedJstring log_dir_jstr(env, _log_dir);
	ScopedJstring data_data_dir_jstr(env, _mmap_dir);
    ScopedJstring nameprefix_jstr(env, _nameprefix);
	const char* pubkey = NULL;
	ScopedJstring jstr_pubkey(env, _pubkey);
	if (NULL != _pubkey) {
		pubkey = jstr_pubkey.GetChar();
	}
	appender_open((TAppenderMode)mode, log_dir_jstr.GetChar(), data_data_dir_jstr.GetChar(),
			nameprefix_jstr.GetChar(), pubkey, isCrypt);
	xlogger_SetLevel((TLogLevel)level);
}

// 刷缓存到log文件
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_flush(JNIEnv *env, jclass clazz, jboolean _is_sync) {
	if (_is_sync) {
		appender_flush_sync();
	} else {
		appender_flush();
	}
}

// 关闭log
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_close(JNIEnv *env, jclass clazz) {
	appender_close();
}

// 设置log等级
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_level(JNIEnv *, jclass, jint _log_level) {
	xlogger_SetLevel((TLogLevel)_log_level);
}

// 设计log写模式
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_mode(JNIEnv *, jclass, jint _mode) {
	appender_setmode((TAppenderMode)_mode);
}

// 单个日志文件的最大大小，超过会分文件
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_fileMaxSize(JNIEnv *, jclass, jint size) {
    appender_set_max_file_size(size);
}

// 设置是否输出日志到控制台
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_useConsoleLog(JNIEnv *env, jclass, jboolean _is_open) {
	appender_set_console_log((bool)_is_open);
}

// 写log
JNIEXPORT void JNICALL Java_com_lzx_klog_writer_FileLog_logWrite
  (JNIEnv *env, jclass, int _level, jstring _tag, jstring _filename,
		  jstring _funcname, jint _line, jint _pid, jlong _tid, jlong _maintid,
		  jstring _log, jstring _threadname) {

	if (!xlogger_IsEnabledFor((TLogLevel)_level)) {
		return;
	}

	XLoggerInfo xlog_info;
	gettimeofday(&xlog_info.timeval, NULL);
	xlog_info.level = (TLogLevel)_level;
	xlog_info.line = (int)_line;
	xlog_info.pid = (int)_pid;
	xlog_info.tid = LONGTHREADID2INT(_tid);
	xlog_info.maintid = LONGTHREADID2INT(_maintid);

	const char* tag_cstr = NULL;
	const char* filename_cstr = NULL;
	const char* funcname_cstr = NULL;
	const char* log_cstr = NULL;
	const char* threadname_cstr = NULL;

	if (NULL != _tag) {
		tag_cstr = env->GetStringUTFChars(_tag, NULL);
	}

	if (NULL != _filename) {
		filename_cstr = env->GetStringUTFChars(_filename, NULL);
	}

	if (NULL != _funcname) {
		funcname_cstr = env->GetStringUTFChars(_funcname, NULL);
	}

	if (NULL != _log) {
		log_cstr = env->GetStringUTFChars(_log, NULL);
	}

	if (NULL != _threadname) {
    	threadname_cstr = env->GetStringUTFChars(_threadname, NULL);
    }

	xlog_info.tag = NULL == tag_cstr ? "" : tag_cstr;
	xlog_info.filename = NULL == filename_cstr ? "" : filename_cstr;
	xlog_info.func_name = NULL == funcname_cstr ? "" : funcname_cstr;
	xlog_info.threadname = NULL == threadname_cstr ? "" : threadname_cstr;
	xlogger_Write_Info(&xlog_info, NULL == log_cstr ? "NULL == log" : log_cstr);

	if (NULL != _tag) {
		env->ReleaseStringUTFChars(_tag, tag_cstr);
	}

	if (NULL != _filename) {
		env->ReleaseStringUTFChars(_filename, filename_cstr);
	}

	if (NULL != _funcname) {
		env->ReleaseStringUTFChars(_funcname, funcname_cstr);
	}

	if (NULL != _log) {
		env->ReleaseStringUTFChars(_log, log_cstr);
	}

	if (NULL != _threadname) {
    	env->ReleaseStringUTFChars(_threadname, threadname_cstr);
    }
}
}
