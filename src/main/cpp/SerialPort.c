/*
 * Copyright 2009-2011 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>

#include "SerialPort.h"

#include "android/log.h"

static const char *TAG = "serial";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

#define MSM_ENABLE_CLOCK 0x5441
int fd;

static speed_t getBaudrate(jint baudrate) {
    switch (baudrate) {
        case 0:
            return B0;
        case 50:
            return B50;
        case 75:
            return B75;
        case 110:
            return B110;
        case 134:
            return B134;
        case 150:
            return B150;
        case 200:
            return B200;
        case 300:
            return B300;
        case 600:
            return B600;
        case 1200:
            return B1200;
        case 1800:
            return B1800;
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 500000:
            return B500000;
        case 576000:
            return B576000;
        case 921600:
            return B921600;
        case 1000000:
            return B1000000;
        case 1152000:
            return B1152000;
        case 1500000:
            return B1500000;
        case 2000000:
            return B2000000;
        case 2500000:
            return B2500000;
        case 3000000:
            return B3000000;
        case 3500000:
            return B3500000;
        case 4000000:
            return B4000000;
        default:
            return -1;
    }
}

int set_serial_port(int baud, int databits, int parity, int stopbits, int flow, int rts_value) {
    //
    struct termios options;

    if (ioctl(fd, MSM_ENABLE_CLOCK, 2) < 0) {
        LOGE("ioctl msm enable clock error \n");
    }

    if (tcgetattr(fd, &options) != 0) {
        LOGE("SetupSerial 1");
        return -1;
    }

    options.c_cflag |= CLOCAL;
    options.c_cflag |= CREAD;


    switch (flow) {
        case 0:
            options.c_cflag &= ~CRTSCTS;
            break;

        case 1:
            options.c_cflag |= CRTSCTS;
            break;
        case 2:
            options.c_cflag |= IXON | IXOFF | IXANY;
            break;
    }

    options.c_cflag &= ~CSIZE;  //0x00030
    switch (databits) {
        case 5:
            options.c_cflag |= CS5;
            break;
        case 6:
            options.c_cflag |= CS5;
            break;
        case 7:
            options.c_cflag |= CS7;
            break;
        case 8:
            options.c_cflag |= CS8;
            break;
        default:
            options.c_cflag |= CS8;
            break;
//            return -1;
    }


    switch (stopbits) {
        case 1:
            options.c_cflag &= ~CSTOPB;
            break;
        case 2:
            options.c_cflag |= CSTOPB;
            break;
        default:
//            return -1;
            options.c_cflag &= ~CSTOPB;
            break;
    }



    LOGE("parity = %d\n", parity);
    switch (parity) {
        case 0:
            options.c_cflag &= ~PARENB;
            options.c_cflag &= ~CMSPAR;
            options.c_iflag &= ~INPCK;
            break;
        case 1:
            options.c_cflag |= PARENB;
            options.c_cflag &= ~PARODD;
            options.c_cflag &= ~CMSPAR;
            options.c_iflag |= INPCK;
            break;
        case 2:
            options.c_cflag |= (PARODD | PARENB);
            options.c_cflag &= ~CMSPAR;
            options.c_iflag |= INPCK;
            break;

        case 3:
            options.c_cflag |= PARENB | CMSPAR | PARODD; //mark
            options.c_iflag |= INPCK;
            break;
        case 4:
            options.c_cflag |= PARENB | CMSPAR;
            options.c_cflag &= ~PARODD;             //space
            options.c_iflag |= INPCK;
            break;
        default:
            options.c_cflag &= ~PARENB;
            options.c_cflag &= ~CMSPAR;
            options.c_iflag &= ~INPCK;
            break;
    }


    options.c_oflag &= ~OPOST;

    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);

    options.c_cc[VTIME] = 1; /* ????????1*(1/10)s */
    options.c_cc[VMIN] = 1;  /* ??????????1 */

    tcflush(fd, TCIFLUSH);

    if (tcsetattr(fd, TCSANOW, &options) != 0) {
        LOGE("com set error!\n");
        return -1;
    }

    //RTS control
    int ctrlbits;

    if (1 == rts_value) {
        //set RTS enable
        ioctl(fd, TIOCMGET, &ctrlbits);
        ctrlbits &= ~TIOCM_RTS;
        ioctl(fd, TIOCMSET, &ctrlbits);
    } else if (0 == rts_value) {
        ioctl(fd, TIOCMGET, &ctrlbits);
        ctrlbits |= TIOCM_RTS;//send enable
        ioctl(fd, TIOCMSET, &ctrlbits);
    }

    return 0;
}

/*
 * Class:     android_serialport_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */

JNIEXPORT jobject JNICALL Java_com_mascill_serialport_SerialPort_open
        (JNIEnv *env, jclass clazz, jstring path, jint baudrate, jboolean flags, jint parity) {

    speed_t speed;
    jobject mFileDescriptor;

    /* Opening device */
    {
        jboolean iscopy;
        const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
//		LOGD("Opening serial port %s with flags 0x%x", path_utf, O_RDWR | flags);
        fd = open(path_utf, O_RDWR);
        LOGD("fd = %d, path = %s\n", fd, path_utf);
        (*env)->ReleaseStringUTFChars(env, path, path_utf);
        if (fd < 0) {
            /* Throw an exception */
            LOGE("Cannot open port");
            /* TODO: throw an exception */
            return NULL;
        }
    }

    speed = getBaudrate(baudrate);
    if (speed == -1) {
        /* TODO: throw an exception */
        LOGE("Invalid baudrate");
        return NULL;
    }
    struct termios cfg;
    LOGD("Configuring serial port");
    if (tcgetattr(fd, &cfg)) {
        LOGE("tcgetattr() failed");
        close(fd);
        /* TODO: throw an exception */
        return NULL;
    }

    cfmakeraw(&cfg);
    cfsetispeed(&cfg, speed);
    cfsetospeed(&cfg, speed);

    if (tcsetattr(fd, TCSANOW, &cfg)) {
        LOGE("tcsetattr() failed");
        close(fd);
        /* TODO: throw an exception */
        return NULL;
    }

    if (flags) {
        set_serial_port(baudrate, 8, parity, 1, 0, 0);
    }

    /* Create a corresponding file descriptor */
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        (*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint) fd);
    }

    return mFileDescriptor;
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mascill_serialport_SerialPort_close
        (JNIEnv *env, jobject thiz) {
    jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}

JNIEXPORT void JNICALL Java_com_mascill_serialport_SerialPort_setParity
        (JNIEnv *env, jobject thiz, jint parity) {
    // TODO: implement setParity()
}