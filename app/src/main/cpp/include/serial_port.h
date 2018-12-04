#include <jni.h>

#ifndef machinecontrol_serialport_SERIAL_PORT_H
#define machinecontrol_serialport_SERIAL_PORT_H
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL Java_com_qmyan_fujitestdemo_serialport_SerialPort_open
        (JNIEnv *, jobject, jstring, jint, jint);

JNIEXPORT void JNICALL Java_com_qmyan_fujitestdemo_serialport_SerialPort_close
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif