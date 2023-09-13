LOCAL_PATH :=$(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := lzx_klog

LOCAL_CPP_EXTENSION := .cc .cpp

LOCAL_SRC_FILES :=  comm/android/callstack.cc \
                    comm/assert/__assert.c \
                    comm/autobuffer.cc \
                    comm/jni/util/scoped_jstring.cc \
                    comm/jni/xlogger_threadinfo.cc \
                    comm/ptrbuffer.cc \
                    comm/strutil.cc \
                    comm/tickcount.cc \
                    comm/time_utils.c \
                    comm/xlogger/loginfo_extract.c \
                    comm/xlogger/xloggerbase.c \
                    micro-ecc-master/uECC.c \
					log/formater.cc \
					log/log_buffer.cc \
					log/crypt/log_crypt.cc\
					log/consolelog.cc \
					main.cc filelog.cc logimpl.cc \
					comm/klogmapfile.cpp \
					comm/mmap_util.cc \
					log/appender.cc

LOCAL_LDLIBS += -llog -lz

LOCAL_C_INCLUDES := $(LOCAL_PATH)/. $(LOCAL_PATH)/ $(LOCAL_PATH)/comm/

LOCAL_CPPFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden -fvisibility-inlines-hidden -fexceptions
LOCAL_CFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden
LOCAL_LDFLAGS += -Wl,--gc-sections
include $(BUILD_SHARED_LIBRARY)
