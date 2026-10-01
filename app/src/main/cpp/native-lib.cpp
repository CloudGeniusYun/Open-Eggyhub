#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include "security_utils.h"

#define LOG_TAG "EggySecurity"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 全局变量：标记AuthActivity是否存在
static bool g_auth_activity_exists = false;

// 检查AuthActivity类是否被篡改
static bool check_auth_activity_integrity(JNIEnv* env) {
    // 1. 检查AuthActivity类是否存在
    jclass auth_activity_class = env->FindClass("com/eggyhub/android/AuthActivity");
    if (auth_activity_class == NULL) {
        LOGE("AuthActivity class not found - possible tampering!");
        return false;
    }
    
    // 2. 检查notifyAuthActivityExists方法是否存在
    jmethodID notify_method = env->GetStaticMethodID(auth_activity_class, "notifyAuthActivityExists", "()Z");
    if (notify_method == NULL) {
        LOGE("notifyAuthActivityExists method not found - possible tampering!");
        return false;
    }
    
    // 3. 检查onCreate方法是否存在
    jmethodID oncreate_method = env->GetMethodID(auth_activity_class, "onCreate", "(Landroid/os/Bundle;)V");
    if (oncreate_method == NULL) {
        LOGE("onCreate method not found - possible tampering!");
        return false;
    }
    
    LOGE("AuthActivity integrity check passed");
    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_AuthActivity_notifyAuthActivityExists(
        JNIEnv* env,
        jclass clazz) {
    
    LOGE("AuthActivity notified as exists");
    g_auth_activity_exists = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_checkAuthActivityExists(
        JNIEnv* env,
        jclass clazz,
        jobject context) {
    
    LOGE("checkAuthActivityExists started");
    
    // 只检查AuthActivity类完整性，不检查是否已创建
    // 因为应用启动时AuthActivity还没被创建
    if (!check_auth_activity_integrity(env)) {
        LOGE("AuthActivity integrity check failed!");
        return JNI_FALSE;
    }
    
    LOGE("AuthActivity integrity check passed");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_verifyAuthActivityCreated(
        JNIEnv* env,
        jclass clazz) {
    
    LOGE("verifyAuthActivityCreated started");
    
    // 检查AuthActivity是否已创建并通知native层
    if (g_auth_activity_exists) {
        LOGE("AuthActivity verified as created");
        return JNI_TRUE;
    }
    
    LOGE("AuthActivity not created - possible tampering!");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_AuthActivity_verifyAuthActivityCreated(
        JNIEnv* env,
        jclass clazz) {
    
    LOGE("AuthActivity.verifyAuthActivityCreated started");
    
    // 检查AuthActivity是否已创建并通知native层
    if (g_auth_activity_exists) {
        LOGE("AuthActivity verified as created");
        return JNI_TRUE;
    }
    
    LOGE("AuthActivity not created - possible tampering!");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_checkSignatureNative(
        JNIEnv* env,
        jclass clazz,
        jobject context) {
    
    LOGE("checkSignatureNative started");

    jstring current_hash = get_current_signature_sha256(env, context);
    if (current_hash == NULL) {
        LOGE("Could not get current signature hash, failing verification.");
        return JNI_FALSE;
    }

    jboolean is_valid = verify_signature_rsa(env, context, current_hash);
    
    if (!is_valid) {
        LOGE("SECURITY ALERT: Signature verification failed!");
        return JNI_FALSE; 
    }

    LOGE("checkSignatureNative passed successfully");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_verifyLicenseNative(
        JNIEnv* env,
        jclass clazz,
        jobject context,
        jstring device_id,
        jstring auth_code) {

    LOGE("verifyLicenseNative started");

    if (device_id == NULL || auth_code == NULL) {
        LOGE("Error: device_id or auth_code is NULL");
        return JNI_FALSE;
    }

    const char* device_id_str = env->GetStringUTFChars(device_id, NULL);
    const char* auth_code_str = env->GetStringUTFChars(auth_code, NULL);
    LOGE("Device ID: %s", device_id_str);
    LOGE("Auth Code: %s", auth_code_str);

    jboolean result = JNI_FALSE;

    do {
        jclass base64_class = env->FindClass("android/util/Base64");
        if (base64_class == NULL) { LOGE("Failed to find Base64 class"); break; }

        jmethodID base64_decode = env->GetStaticMethodID(base64_class, "decode", "(Ljava/lang/String;I)[B");
        jbyteArray auth_code_bytes = (jbyteArray)env->CallStaticObjectMethod(base64_class, base64_decode, auth_code, 0);
        if (auth_code_bytes == NULL || env->ExceptionCheck()) {
            env->ExceptionClear();
            LOGE("Failed to decode auth_code");
            break;
        }

        char* hidden_key = get_hidden_string_from_res(env, context, "anim_interpolator_config");
        if (hidden_key == NULL) {
            LOGE("SECURITY ALERT: Failed to restore public key!");
            break;
        }
        jstring public_key_jstr = env->NewStringUTF(hidden_key);
        free(hidden_key);

        jbyteArray public_key_bytes = (jbyteArray)env->CallStaticObjectMethod(base64_class, base64_decode, public_key_jstr, 0);

        jclass x509_spec_class = env->FindClass("java/security/spec/X509EncodedKeySpec");
        jmethodID x509_spec_init = env->GetMethodID(x509_spec_class, "<init>", "([B)V");
        jobject x509_spec_obj = env->NewObject(x509_spec_class, x509_spec_init, public_key_bytes);

        jclass key_factory_class = env->FindClass("java/security/KeyFactory");
        jmethodID kf_get_instance = env->GetStaticMethodID(key_factory_class, "getInstance", "(Ljava/lang/String;)Ljava/security/KeyFactory;");
        jobject kf_obj = env->CallStaticObjectMethod(key_factory_class, kf_get_instance, env->NewStringUTF("RSA"));

        jmethodID kf_gen_public = env->GetMethodID(key_factory_class, "generatePublic", "(Ljava/security/spec/KeySpec;)Ljava/security/PublicKey;");
        jobject public_key_obj = env->CallObjectMethod(kf_obj, kf_gen_public, x509_spec_obj);

        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            LOGE("Exception during PublicKey generation");
            break;
        }

        jclass signature_class = env->FindClass("java/security/Signature");
        jmethodID sig_get_instance = env->GetStaticMethodID(signature_class, "getInstance", "(Ljava/lang/String;)Ljava/security/Signature;");
        jobject sig_obj = env->CallStaticObjectMethod(signature_class, sig_get_instance, env->NewStringUTF("SHA256withRSA"));

        if (sig_obj == NULL || env->ExceptionCheck()) {
            env->ExceptionClear();
            LOGE("Failed to get Signature instance");
            break;
        }

        jmethodID sig_init_verify = env->GetMethodID(signature_class, "initVerify", "(Ljava/security/PublicKey;)V");
        env->CallVoidMethod(sig_obj, sig_init_verify, public_key_obj);

        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            LOGE("Exception during Signature.initVerify");
            break;
        }

        jmethodID sig_update = env->GetMethodID(signature_class, "update", "([B)V");
        jclass string_class = env->FindClass("java/lang/String");
        jmethodID string_get_bytes = env->GetMethodID(string_class, "getBytes", "()[B");
        jbyteArray device_id_bytes = (jbyteArray)env->CallObjectMethod(device_id, string_get_bytes);
        env->CallVoidMethod(sig_obj, sig_update, device_id_bytes);

        jmethodID sig_verify = env->GetMethodID(signature_class, "verify", "([B)Z");
        result = env->CallBooleanMethod(sig_obj, sig_verify, auth_code_bytes);

        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            LOGE("Exception during Signature.verify");
            result = JNI_FALSE;
        }

        if (result) {
            LOGE("License verification SUCCESS - Signature matches!");
        } else {
            LOGE("License verification FAILED - Signature does not match");
        }

    } while (false);

    env->ReleaseStringUTFChars(device_id, device_id_str);
    env->ReleaseStringUTFChars(auth_code, auth_code_str);

    LOGE("License verification result: %d", result);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_startAntiDebugNative(
        JNIEnv* env,
        jclass clazz,
        jobject context) {
    
    LOGE("Starting anti-debug monitoring...");
    
    bool is_emu = is_emulator_strict(env, context);
    set_emulator_mode(is_emu);
    
    if (is_emu) {
        LOGE("Emulator detected - using relaxed security checks");
        check_anti_debug_tracer_pid();
        check_hook_framework();
    } else {
        LOGE("Physical device detected - using full security checks");
        check_anti_debug_tracer_pid();
        check_ptrace_behavior();
        prevent_ptrace_injection();
        check_syscall_interception();
    }
    
    pthread_t monitor_thread;
    int pthread_res = pthread_create(&monitor_thread, NULL, anti_debug_monitor_thread, NULL);
    if (pthread_res != 0) {
        LOGE("Failed to create anti-debug monitor thread!");
    } else {
        pthread_detach(monitor_thread);
    }
    
    check_hook_framework();
    
    LOGE("Anti-debug monitoring started in background thread.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_stopAntiDebug(
        JNIEnv* env,
        jclass clazz) {
    
    LOGE("Stopping anti-debug monitoring...");
    g_should_stop_monitor = true;
    LOGE("Anti-debug monitoring stop signal sent.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_forceShowAuthActivity(
        JNIEnv* env,
        jclass clazz,
        jobject context) {
    
    LOGE("forceShowAuthActivity started");
    
    // 设置 EggyApp.isRestricted = true
    jclass eggy_app_class = env->FindClass("com/eggyhub/android/EggyApp");
    if (eggy_app_class != NULL) {
        jfieldID is_restricted_field = env->GetStaticFieldID(eggy_app_class, "isRestricted", "Z");
        if (is_restricted_field != NULL) {
            env->SetStaticBooleanField(eggy_app_class, is_restricted_field, JNI_TRUE);
            LOGE("Set EggyApp.isRestricted = true");
        } else {
            LOGE("Failed to find isRestricted field");
        }
    } else {
        LOGE("Failed to find EggyApp class");
    }
    
    // 使用Intent的空构造函数，然后设置Component
    jclass intent_class = env->FindClass("android/content/Intent");
    jmethodID intent_init = env->GetMethodID(intent_class, "<init>", "()V");
    jobject intent = env->NewObject(intent_class, intent_init);
    
    if (intent == NULL) {
        LOGE("Failed to create Intent");
        return;
    }
    
    // 创建ComponentName
    jclass component_name_class = env->FindClass("android/content/ComponentName");
    jmethodID component_name_init = env->GetMethodID(component_name_class, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
    jobject component_name = env->NewObject(component_name_class, component_name_init, 
        env->NewStringUTF("com.eggyhub.android"), 
        env->NewStringUTF("com.eggyhub.android.AuthActivity"));
    
    if (component_name == NULL) {
        LOGE("Failed to create ComponentName");
        return;
    }
    
    // 设置Component
    jmethodID set_component = env->GetMethodID(intent_class, "setComponent", "(Landroid/content/ComponentName;)Landroid/content/Intent;");
    jobject result = env->CallObjectMethod(intent, set_component, component_name);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        LOGE("Exception setting component");
        return;
    }
    
    // 添加FLAG_ACTIVITY_NEW_TASK标志（从Application Context启动Activity必须）
    jmethodID add_flags = env->GetMethodID(intent_class, "addFlags", "(I)Landroid/content/Intent;");
    const int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    env->CallObjectMethod(intent, add_flags, FLAG_ACTIVITY_NEW_TASK);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        LOGE("Exception adding flags");
        return;
    }
    
    // 启动Activity
    jclass context_class = env->FindClass("android/content/Context");
    jmethodID start_activity = env->GetMethodID(context_class, "startActivity", "(Landroid/content/Intent;)V");
    env->CallVoidMethod(context, start_activity, intent);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        LOGE("Exception starting AuthActivity");
        return;
    }
    
    LOGE("forceShowAuthActivity completed");
}

// ==================== 代理密钥相关 ====================

// 密钥片段（XOR 混淆）
// 原始密钥: EggyHub_Proxy_Secret_2024_Key_!@# (32 字符)
static const unsigned char KEY_XOR[] = {
    0x5f, 0x4c, 0x5b, 0x34, 0x16, 0x1a, 0x18, 0xd4, 
    0x4a, 0x59, 0x53, 0x35, 0x27, 0x30, 0x29, 0xee, 
    0x79, 0x59, 0x59, 0x39, 0x01, 0x5d, 0x4a, 0xb9, 
    0x2e, 0x74, 0x77, 0x28, 0x27, 0x30, 0x5b, 0xcb, 
    0x39, 0x00
};

// XOR 掩码
static const unsigned char XOR_MASK[] = {0x1a, 0x2b, 0x3c, 0x4d, 0x5e, 0x6f, 0x7a, 0x8b};

// 获取代理密钥（通过 XOR 解混淆）
extern "C" JNIEXPORT jstring JNICALL
Java_com_eggyhub_android_utils_ProxyConfig_getProxySecretNative(
        JNIEnv* env,
        jclass clazz) {
    
    LOGE("getProxySecretNative called");
    
    // XOR 解混淆计算密钥
    char full_key[64] = {0};
    int pos = 0;
    
    for (int i = 0; KEY_XOR[i] != 0x00; i++) {
        full_key[pos++] = KEY_XOR[i] ^ XOR_MASK[i % 8];
    }
    full_key[pos] = '\0';
    
    LOGE("Decrypted key length: %d", pos);
    
    // 返回计算后的密钥
    jstring result = env->NewStringUTF(full_key);
    
    // 清除内存中的密钥（安全措施）
    memset(full_key, 0, sizeof(full_key));
    
    LOGE("Proxy secret retrieved successfully");
    return result;
}