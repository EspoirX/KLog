//
// Created by deh001 on 2018/5/15.
// jni入口
//

#include <jni.h>

extern void initNativeLogApi();
extern void uninitNativeLogApi();

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *)
{
    initNativeLogApi();

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM*, void*)
{
    uninitNativeLogApi();
}

}
