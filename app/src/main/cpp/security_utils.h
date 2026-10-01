#ifndef SECURITY_UTILS_H
#define SECURITY_UTILS_H

#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// --- 全局变量 ---
extern bool g_is_emulator;
extern volatile bool g_should_stop_monitor;

// --- 资源文件读取工具函数 ---
char* get_hidden_string_from_res(JNIEnv* env, jobject context, const char* res_name_str);

// --- 反调试相关函数 ---
void check_anti_debug_tracer_pid();
void check_hook_framework();
void check_anti_debug_ptrace();
void* anti_debug_monitor_thread(void* arg);
void check_ptrace_behavior();
void prevent_ptrace_injection();
void check_syscall_interception();
void set_emulator_mode(bool is_emu);

// --- 签名验证相关函数 ---
jboolean verify_signature_rsa(JNIEnv* env, jobject context, jstring current_hash_jstr);
jstring get_current_signature_sha256(JNIEnv* env, jobject context);

// --- 模拟器检测相关函数 ---
bool is_emulator(JNIEnv* env);
bool is_emulator_enhanced(JNIEnv* env);
bool is_emulator_strict(JNIEnv* env, jobject context);
bool check_hardware_features(JNIEnv* env, jobject context);
bool check_system_properties();
bool check_telephony_info(JNIEnv* env, jobject context);
bool check_network_features();
bool check_emulator_processes();
bool check_filesystem_features();
bool check_network_mac();
bool check_system_apps();
bool check_cpu_features();
bool check_battery_features();
bool check_device_id(JNIEnv* env, jobject context);
bool check_sensors_missing(JNIEnv* env, jobject context);
bool check_bluetooth_missing(JNIEnv* env, jobject context);
bool check_sim_info(JNIEnv* env, jobject context);
bool check_operator_info(JNIEnv* env, jobject context);

// --- JNI导出函数 ---
extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_checkSignatureNative(
        JNIEnv* env,
        jclass clazz,
        jobject context);

extern "C" JNIEXPORT jboolean JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_verifyLicenseNative(
        JNIEnv* env,
        jclass clazz,
        jobject context,
        jstring device_id,
        jstring auth_code);

extern "C" JNIEXPORT void JNICALL
Java_com_eggyhub_android_utils_SignatureUtils_startAntiDebug(
        JNIEnv* env,
        jclass clazz,
        jobject context);

#ifdef __cplusplus
}
#endif

#endif // SECURITY_UTILS_H