#include <jni.h>
#include <string>

#include <pthread.h>
#include <android/native_window_jni.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include "GlobalContexts.h"
#include "LogUtils.h"
#include "EGLDisplayYUV.h"
#include "ShaderYUV.h"
#include "ShaderPolygon.h"


ANativeWindow * nativeWindow = NULL;
GlobalContexts *global_context = NULL;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
EGLDisplayYUV *eglDisplayYuv = NULL;
ShaderYUV * shaderYuv = NULL;
ShaderPolygon * shaderPolygon = NULL;

unsigned char* array_data = NULL;
unsigned char* yuv_array_data = NULL;
unsigned char* convertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray);
unsigned char* convertNV21ToYUV420P(unsigned char* array_data, int width, int height);
unsigned char* y_array_data = NULL;
unsigned char* u_array_data = NULL;
unsigned char* v_array_data = NULL;

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_setSurface(JNIEnv *env, jobject thiz, jobject surface) {
    LOGD("setSurface in");
    pthread_mutex_lock(&mutex);
    if (nativeWindow) {
        LOGD("setSurface nativeWindow != NULL");
        ANativeWindow_release(nativeWindow);
        nativeWindow = NULL;
    }

    // 创建新的窗口用于视频显示
    nativeWindow = ANativeWindow_fromSurface(env, surface);
    if(NULL == global_context) {
        LOGD("new GlobalContext");
        global_context = new GlobalContexts();
    }
    global_context->nativeWindow = nativeWindow;
    LOGD("GlobalContext");
    if(NULL != eglDisplayYuv) {
        LOGD("eglDisplayYuv->eglClose");
        eglDisplayYuv->eglClose();
        delete eglDisplayYuv;
        eglDisplayYuv = NULL;
    }
    if(NULL != shaderYuv) {
        delete shaderYuv;
        shaderYuv = NULL;
    }
    pthread_mutex_unlock(&mutex);
    LOGD("setSurface out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_setSurfaceSize(JNIEnv *env, jobject thiz, jint width,
                                                    jint height) {
    LOGD("setSurfaceSize in");
    pthread_mutex_lock(&mutex);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    global_context->gl_window_width = width;
    global_context->gl_window_height = height;
    pthread_mutex_unlock(&mutex);
    LOGD("setSurfaceSize out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendSurface(JNIEnv *env, jobject thiz, jbyteArray data,
                                                 jint width, jint height, jint video_rotation) {
    pthread_mutex_lock(&mutex);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    if(NULL == eglDisplayYuv) {
        eglDisplayYuv = new EGLDisplayYUV(global_context->nativeWindow, global_context);
        eglDisplayYuv->eglOpen();
        global_context->gl_video_width = width;
        global_context->gl_video_height = height;
    }
    if(global_context->gl_video_rotation_angle != video_rotation) {
        global_context->gl_video_rotation_angle = video_rotation;
        LOGD("rotation_angle is change : %d", global_context->gl_video_rotation_angle);
        if(shaderYuv != NULL) {
            shaderYuv->changeVideoRotation();
        }
    }
    if(NULL == shaderYuv) {
        shaderYuv = new ShaderYUV(global_context);
        shaderYuv->CreateProgram();
    }
    if(NULL != array_data) {
        delete array_data;
    }
    if(NULL != yuv_array_data) {
        delete yuv_array_data;
    }
    array_data = convertJByteaArrayToChars(env, data);
    yuv_array_data = convertNV21ToYUV420P(array_data, width, height);
    unsigned char* frame_data[3];
    frame_data[0] = yuv_array_data;
    frame_data[1] = yuv_array_data + width * height;
    frame_data[2] = yuv_array_data + width * height +  width * height / 4;
    shaderYuv->Render(frame_data);
    pthread_mutex_unlock(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_releaseSurface(JNIEnv *env, jobject thiz) {
    LOGD("releaseSurface in");
    pthread_mutex_lock(&mutex);
    if(NULL != shaderYuv) {
        delete shaderYuv;
        shaderYuv = NULL;
    }
    if(NULL != shaderPolygon) {
        delete shaderPolygon;
        shaderPolygon = NULL;
    }
    if(NULL != eglDisplayYuv) {
        eglDisplayYuv->eglClose();
        delete eglDisplayYuv;
        eglDisplayYuv = NULL;
    }
    if(NULL != global_context) {
        delete global_context;
        global_context = NULL;
    }
    if(NULL != array_data) {
        delete array_data;
        array_data = NULL;
    }
    if(NULL != yuv_array_data) {
        delete yuv_array_data;
        yuv_array_data = NULL;
    }
    if(NULL != y_array_data) {
        delete y_array_data;
        y_array_data = NULL;
    }
    if(NULL != u_array_data) {
        delete u_array_data;
        u_array_data = NULL;
    }
    if(NULL != v_array_data) {
        delete v_array_data;
        v_array_data = NULL;
    }
    if (NULL != nativeWindow) {
        ANativeWindow_release(nativeWindow);
        nativeWindow = NULL;
    }
    pthread_mutex_unlock(&mutex);
    LOGD("releaseSurface out");
}

unsigned char* convertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray)
{
    unsigned char *chars = NULL;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(bytearray, 0);
    int chars_len = env->GetArrayLength(bytearray);
    chars = new unsigned char[chars_len + 1];//使用结束后, delete 该数组
    memset(chars,0,chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;
    env->ReleaseByteArrayElements(bytearray, bytes, 0);
    return chars;
}

unsigned char* convertNV21ToYUV420P(unsigned char* array_data, int width, int height) {
    unsigned char *src_chars = array_data;
    unsigned char *dst_chars = NULL;
    dst_chars = new unsigned char[width * height / 2 * 3 + 1];
    int uv_data_size = width * height / 4;
    int y_data_size = width * height;
    for (int i = 0; i < y_data_size; ++i) {
        dst_chars[i] = src_chars[i];
    }
    for (int i = 0; i < uv_data_size; ++i) {
        dst_chars[i + y_data_size] = src_chars[2 * i + 1 + y_data_size];//  U数据存储
        dst_chars[i + y_data_size + uv_data_size] = src_chars[2 * i + y_data_size];//  V数据存储
    }
    return dst_chars;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendBlackWhite(JNIEnv *env, jobject thiz) {
    LOGD("rendBlackWhite in");
    if(NULL != shaderYuv) {
        shaderYuv->blackWhiteRender();
    }
    LOGD("rendBlackWhite out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendWarmColor(JNIEnv *env, jobject thiz) {
    LOGD("rendWarmColor in");
    if(NULL != shaderYuv) {
        shaderYuv->warmColorRender();
    }
    LOGD("rendWarmColor out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendNormalColor(JNIEnv *env, jobject thiz) {
    LOGD("rendNormalColor in");
    if(NULL != shaderYuv) {
        shaderYuv->normalColorRender();
    }
    LOGD("rendNormalColor out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendCoolColor(JNIEnv *env, jobject thiz) {
    LOGD("rendCoolColor in");
    if(NULL != shaderYuv) {
        shaderYuv->coolColorRender();
    }
    LOGD("rendCoolColor out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendSplitScreen(JNIEnv *env, jobject thiz) {
    LOGD("rendSplitScreen in");
    if(NULL != shaderYuv) {
        shaderYuv->splitScreenRender();
    }
    LOGD("rendSplitScreen out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendCamera2Surface(JNIEnv *env, jobject thiz,
                                                        jbyteArray y_data, jbyteArray u_data,
                                                        jbyteArray v_data, jint width, jint height,
                                                        jint video_rotation) {
    pthread_mutex_lock(&mutex);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    if(NULL == eglDisplayYuv) {
        eglDisplayYuv = new EGLDisplayYUV(global_context->nativeWindow, global_context);
        eglDisplayYuv->eglOpen();
        global_context->gl_video_width = width;
        global_context->gl_video_height = height;
    }
    if(global_context->gl_video_rotation_angle != video_rotation) {
        global_context->gl_video_rotation_angle = video_rotation;
        LOGD("rotation_angle is change : %d", global_context->gl_video_rotation_angle);
        if(shaderYuv != NULL) {
            shaderYuv->changeVideoRotation();
        }
    }
    if(NULL == shaderYuv) {
        shaderYuv = new ShaderYUV(global_context);
        shaderYuv->CreateProgram();
    }

    if(NULL != y_array_data) {
        delete y_array_data;
        y_array_data = NULL;
    }
    if(NULL != u_array_data) {
        delete u_array_data;
        u_array_data = NULL;
    }
    if(NULL != v_array_data) {
        delete v_array_data;
        v_array_data = NULL;
    }

    y_array_data = convertJByteaArrayToChars(env, y_data);
    u_array_data = convertJByteaArrayToChars(env, u_data);
    v_array_data = convertJByteaArrayToChars(env, v_data);

    unsigned char* frame_data[3];
    frame_data[0] = y_array_data;
    frame_data[1] = u_array_data;
    frame_data[2] = v_array_data;
    shaderYuv->Render(frame_data);
    pthread_mutex_unlock(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_saveAssetManager(JNIEnv *env, jobject thiz, jobject manager) {
    LOGD("saveAssetManager in");
    pthread_mutex_lock(&mutex);
    AAssetManager *mgr = AAssetManager_fromJava(env, manager);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    global_context->assetManager = mgr;
    pthread_mutex_unlock(&mutex);
    LOGD("saveAssetManager out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendPolygon(JNIEnv *env, jobject thiz) {
    LOGD("rendPolygon in");
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    if(NULL == eglDisplayYuv) {
        eglDisplayYuv = new EGLDisplayYUV(global_context->nativeWindow, global_context);
        eglDisplayYuv->eglOpen();
    }
    if(NULL == shaderPolygon) {
        shaderPolygon = new ShaderPolygon(global_context);
        shaderPolygon->CreateProgram();
    }
    shaderPolygon->Render();
    LOGD("rendPolygon out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendPolygonAdd(JNIEnv *env, jobject thiz) {
    LOGD("rendPolygonAdd in");
    if(NULL != shaderPolygon) {
        shaderPolygon->polygonAdd();
    }
    LOGD("rendPolygonAdd out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_function_OpenGLControl_rendPolygonSubtraction(JNIEnv *env, jobject thiz) {
    LOGD("rendPolygonSubtraction in");
    if(NULL != shaderPolygon) {
        shaderPolygon->polygonSubtraction();
    }
    LOGD("rendPolygonSubtraction out");
}