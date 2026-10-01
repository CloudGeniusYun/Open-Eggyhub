#include "security_utils.h"
#include <android/log.h>
#include <stdlib.h>

#define LOG_TAG "SignatureVerify"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * 核心校验逻辑：JNI 反射调用 Java RSA 引擎
 */
jboolean verify_signature_rsa(JNIEnv* env, jobject context, jstring current_hash_jstr) {
    LOGE("Starting RSA verification...");
    
    if (current_hash_jstr == NULL) {
        LOGE("Error: current_hash_jstr is NULL");
        return JNI_FALSE;
    }

    // 1. 将 C++ 字符串转为 Java 对象
    
    // 1.1 从资源中读取隐藏的公钥
    char* hidden_key = get_hidden_string_from_res(env, context, "anim_interpolator_config");
    if (hidden_key == NULL) {
        LOGE("SECURITY ALERT: Failed to restore public key!");
        return JNI_FALSE;
    }
    jstring public_key_jstr = env->NewStringUTF(hidden_key);
    free(hidden_key); 
    
    // 1.2 从资源中读取隐藏的签名哈希
    char* hidden_hash = get_hidden_string_from_res(env, context, "anim_interpolator_values");
    if (hidden_hash == NULL) {
        LOGE("SECURITY ALERT: Failed to restore signed hash!");
        return JNI_FALSE;
    }
    jstring signed_hash_jstr = env->NewStringUTF(hidden_hash);
    free(hidden_hash);

    // 2. 获取 java.security.Signature
    jclass signature_class = env->FindClass("java/security/Signature");
    if (signature_class == NULL) { LOGE("Failed to find Signature class"); return JNI_FALSE; }
    
    jmethodID sig_get_instance = env->GetStaticMethodID(signature_class, "getInstance", "(Ljava/lang/String;)Ljava/security/Signature;");
    jobject sig_obj = env->CallStaticObjectMethod(signature_class, sig_get_instance, env->NewStringUTF("SHA256withRSA"));
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        LOGE("Exception in Signature.getInstance");
        return JNI_FALSE;
    }
    if (sig_obj == NULL) { LOGE("Failed to get Signature instance"); return JNI_FALSE; }

    // 3. 获取 PublicKey 对象 (通过 KeyFactory)
    jclass base64_class = env->FindClass("android/util/Base64");
    jmethodID base64_decode = env->GetStaticMethodID(base64_class, "decode", "(Ljava/lang/String;I)[B");
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
        return JNI_FALSE;
    }

    // 4. 初始化验证
    jmethodID sig_init_verify = env->GetMethodID(signature_class, "initVerify", "(Ljava/security/PublicKey;)V");
    env->CallVoidMethod(sig_obj, sig_init_verify, public_key_obj);

    // 5. 更新原始哈希数据
    jmethodID sig_update = env->GetMethodID(signature_class, "update", "([B)V");
    jclass string_class = env->FindClass("java/lang/String");
    jmethodID string_get_bytes = env->GetMethodID(string_class, "getBytes", "()[B");
    jbyteArray hash_bytes = (jbyteArray)env->CallObjectMethod(current_hash_jstr, string_get_bytes);
    env->CallVoidMethod(sig_obj, sig_update, hash_bytes);

    // 6. 执行验证
    jmethodID sig_verify_actual = env->GetMethodID(signature_class, "verify", "([B)Z");
    jbyteArray signed_bytes = (jbyteArray)env->CallStaticObjectMethod(base64_class, base64_decode, signed_hash_jstr, 0);
    
    jboolean result = env->CallBooleanMethod(sig_obj, sig_verify_actual, signed_bytes);
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        LOGE("Exception during Signature.verify");
        return JNI_FALSE;
    }

    LOGE("RSA verification result: %d", result);
    return result;
}

/**
 * 获取当前 App 的签名 SHA-256 哈希值
 */
jstring get_current_signature_sha256(JNIEnv* env, jobject context) {
    LOGE("Getting current signature hash...");
    // 1. Context.getPackageManager()
    jclass context_class = env->GetObjectClass(context);
    jmethodID get_pm = env->GetMethodID(context_class, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject pm = env->CallObjectMethod(context, get_pm);
    if (pm == NULL) { LOGE("Failed to get PackageManager"); return NULL; }

    // 2. Context.getPackageName()
    jmethodID get_pn = env->GetMethodID(context_class, "getPackageName", "()Ljava/lang/String;");
    jstring package_name = (jstring)env->CallObjectMethod(context, get_pn);
    if (package_name == NULL) { LOGE("Failed to get PackageName"); return NULL; }

    // 3. PackageManager.getPackageInfo(packageName, GET_SIGNATURES)
    jclass pm_class = env->GetObjectClass(pm);
    jmethodID get_pi = env->GetMethodID(pm_class, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    
    // 64 = PackageManager.GET_SIGNATURES
    jobject package_info = env->CallObjectMethod(pm, get_pi, package_name, 64);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        LOGE("Exception in getPackageInfo");
        return NULL;
    }
    if (package_info == NULL) { LOGE("Failed to get PackageInfo"); return NULL; }

    // 4. 获取签名数组
    jclass pi_class = env->GetObjectClass(package_info);
    jfieldID signatures_field = env->GetFieldID(pi_class, "signatures", "[Landroid/content/pm/Signature;");
    jobjectArray signatures = (jobjectArray)env->GetObjectField(package_info, signatures_field);
    if (signatures == NULL || env->GetArrayLength(signatures) == 0) { 
        LOGE("Failed to get signatures array or array is empty"); 
        return NULL; 
    }
    
    jobject first_signature = env->GetObjectArrayElement(signatures, 0);
    if (first_signature == NULL) { LOGE("First signature is NULL"); return NULL; }

    // 5. Signature.toByteArray()
    jclass sig_class = env->GetObjectClass(first_signature);
    jmethodID to_byte_array = env->GetMethodID(sig_class, "toByteArray", "()[B");
    jbyteArray cert_bytes = (jbyteArray)env->CallObjectMethod(first_signature, to_byte_array);
    if (cert_bytes == NULL) { LOGE("toByteArray returned NULL"); return NULL; }

    // 6. 计算 SHA-256
    jclass md_class = env->FindClass("java/security/MessageDigest");
    jmethodID md_get_instance = env->GetStaticMethodID(md_class, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jobject md_obj = env->CallStaticObjectMethod(md_class, md_get_instance, env->NewStringUTF("SHA-256"));
    if (md_obj == NULL) { LOGE("Failed to get MessageDigest instance"); return NULL; }
    
    jmethodID md_digest = env->GetMethodID(md_class, "digest", "([B)[B");
    jbyteArray sha256_bytes = (jbyteArray)env->CallObjectMethod(md_obj, md_digest, cert_bytes);
    if (sha256_bytes == NULL) { LOGE("digest returned NULL"); return NULL; }

    // 7. 转换为 Hex String
    jclass hex_class = env->FindClass("java/lang/StringBuilder");
    jmethodID hex_init = env->GetMethodID(hex_class, "<init>", "()V");
    jobject hex_builder = env->NewObject(hex_class, hex_init);
    jmethodID hex_append = env->GetMethodID(hex_class, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

    jsize len = env->GetArrayLength(sha256_bytes);
    jbyte* bytes = env->GetByteArrayElements(sha256_bytes, NULL);
    
    char hex_buf[3];
    for (int i = 0; i < len; i++) {
        sprintf(hex_buf, "%02X", (unsigned char)bytes[i]);
        env->CallObjectMethod(hex_builder, hex_append, env->NewStringUTF(hex_buf));
    }
    env->ReleaseByteArrayElements(sha256_bytes, bytes, JNI_ABORT);

    jmethodID to_string = env->GetMethodID(hex_class, "toString", "()Ljava/lang/String;");
    jstring result = (jstring)env->CallObjectMethod(hex_builder, to_string);
    
    const char* result_ptr = env->GetStringUTFChars(result, NULL);
    LOGE("Current signature hash obtained: %s", result_ptr);
    env->ReleaseStringUTFChars(result, result_ptr);
    
    return result;
}