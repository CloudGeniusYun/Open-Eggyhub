#include "security_utils.h"
#include <android/log.h>
#include <stdlib.h>

#define LOG_TAG "ResourceUtils"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * 从资源文件中读取隐藏的字符串 (公钥或哈希)
 * res_name: 资源名，如 "anim_interpolator_config"
 */
char* get_hidden_string_from_res(JNIEnv* env, jobject context, const char* res_name_str) {
    // 1. 获取 Resources 对象
    jclass context_class = env->GetObjectClass(context);
    jmethodID get_resources = env->GetMethodID(context_class, "getResources", "()Landroid/content/res/Resources;");
    jobject resources = env->CallObjectMethod(context, get_resources);
    if (resources == NULL) { LOGE("Failed to get Resources"); return NULL; }

    // 2. 获取资源 ID
    jclass res_class = env->GetObjectClass(resources);
    jmethodID get_identifier = env->GetMethodID(res_class, "getIdentifier", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I");
    jstring name = env->NewStringUTF(res_name_str);
    jstring type = env->NewStringUTF("array");
    
    // 动态获取包名
    jmethodID get_pkg_name = env->GetMethodID(context_class, "getPackageName", "()Ljava/lang/String;");
    jstring pkg = (jstring)env->CallObjectMethod(context, get_pkg_name);
    
    jint res_id = env->CallIntMethod(resources, get_identifier, name, type, pkg);
    
    if (res_id == 0) { LOGE("Failed to find hidden resource ID: %s", res_name_str); return NULL; }

    // 3. 读取整数数组
    jmethodID get_int_array = env->GetMethodID(res_class, "getIntArray", "(I)[I");
    jintArray int_array = (jintArray)env->CallObjectMethod(resources, get_int_array, res_id);
    if (int_array == NULL) { LOGE("Failed to read int array"); return NULL; }

    // 4. 还原为字符串
    jsize len = env->GetArrayLength(int_array);
    jint* elements = env->GetIntArrayElements(int_array, NULL);
    
    char* key_buffer = (char*)malloc(len + 1);
    for (int i = 0; i < len; i++) {
        key_buffer[i] = (char)elements[i];
    }
    key_buffer[len] = '\0'; // 结尾补零

    env->ReleaseIntArrayElements(int_array, elements, 0);
    
    // LOGE("Hidden key restored: %s", key_buffer);
    return key_buffer;
}