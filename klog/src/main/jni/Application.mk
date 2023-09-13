APP_LOCAL_PATH :=$(call my-dir)
APP_ABI := armeabi-v7a arm64-v8a x86
APP_PLATFORM := android-9
APP_CPPFLAGS += -std=c++11
#APP_STL := c++_static
APP_STL := c++_shared

#GLOCAL_CFLAGS += -fvisibility=hidden
#LOCAL_CFLAGS += -DBOOST_NO_EXCEPTIONS=1
#LOCAL_CFLAGS += -DBOOST_EXCEPTION_DISABLE=1
#APP_CPPFLAGS +=-std=gnu++11
#APP_STL := stlport_static

