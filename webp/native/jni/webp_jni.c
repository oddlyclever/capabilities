// The JNI binding for :webp on Android: libwebp's simple encoding API over an RGBA byte array.
//
// Methods are registered by name in JNI_OnLoad rather than found by symbol (Java_<package>_<class>_<method>), so
// the Kotlin class is named in one place, WEBP_JNI_CLASS below, and a mismatch fails loudly when the library loads
// instead of at the first call. Moving the Kotlin class still means changing that name and rebuilding.
#include <jni.h>
#include <stdint.h>
#include <android/log.h>
#include "webp/encode.h"

#define WEBP_JNI_CLASS "com/oddlyclever/webp/NativeWebp"
#define LOG_TAG "webp"

// Returns the encoded WebP, or null when libwebp refuses the image (for instance, larger than 16383 pixels a side).
static jbyteArray encodeRgba(JNIEnv *env, jclass clazz, jbyteArray rgba, jint width, jint height, jint stride,
                             jfloat quality, jboolean lossless) {
    (void) clazz;
    uint8_t *output = NULL;
    size_t size;
    jbyte *pixels = (*env)->GetPrimitiveArrayCritical(env, rgba, NULL);
    if (pixels == NULL) return NULL;
    if (lossless) {
        size = WebPEncodeLosslessRGBA((const uint8_t *) pixels, width, height, stride, &output);
    } else {
        size = WebPEncodeRGBA((const uint8_t *) pixels, width, height, stride, quality, &output);
    }
    (*env)->ReleasePrimitiveArrayCritical(env, rgba, pixels, JNI_ABORT);
    if (size == 0 || output == NULL) {
        WebPFree(output);
        return NULL;
    }
    jbyteArray result = (*env)->NewByteArray(env, (jsize) size);
    if (result != NULL) (*env)->SetByteArrayRegion(env, result, 0, (jsize) size, (const jbyte *) output);
    WebPFree(output);
    return result;
}

// libwebp's encoder version as 0xMMmmpp, so a test can tell which build it loaded.
static jint encoderVersion(JNIEnv *env, jclass clazz) {
    (void) env;
    (void) clazz;
    return WebPGetEncoderVersion();
}

static const JNINativeMethod METHODS[] = {
        {"encodeRgba",     "([BIIIFZ)[B", (void *) encodeRgba},
        {"encoderVersion", "()I",         (void *) encoderVersion},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass clazz = (*env)->FindClass(env, WEBP_JNI_CLASS);
    if (clazz == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "class %s not found", WEBP_JNI_CLASS);
        return JNI_ERR;
    }
    if ((*env)->RegisterNatives(env, clazz, METHODS, sizeof(METHODS) / sizeof(METHODS[0])) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "registering %s's natives failed", WEBP_JNI_CLASS);
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
