#include "security_utils.h"
#include <android/log.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/stat.h>
#include <dirent.h>

#define LOG_TAG "EmulatorDetection"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * 判断是否运行在模拟器上 (增强版)
 */
bool is_emulator(JNIEnv* env) {
    LOGE("Emulator Check: Starting comprehensive scan...");
    
    // 1. 检查底层驱动文件和特有库 (模拟器通信管道、调试库等)
    const char* emulator_files[] = {
        "/dev/socket/qemud",
        "/dev/qemu_pipe",
        "/system/lib/libc_malloc_debug_qemu.so",
        "/sys/qemu_trace",
        "/system/bin/qemu-props"
    };
    for (int i = 0; i < 5; i++) {
        if (access(emulator_files[i], F_OK) == 0) {
            LOGE("Emulator detected via file: %s", emulator_files[i]);
            return true;
        }
    }

    // 2. 检查 CPU 信息 (/proc/cpuinfo)
    FILE* fp = fopen("/proc/cpuinfo", "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            // 常见的模拟器 CPU 特征
            if (strstr(line, "Goldfish") || 
                strstr(line, "vbox86") || 
                strstr(line, "qemu")) {
                LOGE("Emulator detected via CPU trait in /proc/cpuinfo: %s", line);
                fclose(fp);
                return true;
            }
        }
        fclose(fp);
    }

    // 3. 检查 Build 信息 (原有逻辑作为兜底)
    jclass build_class = env->FindClass("android/os/Build");
    
    // 获取 Build.FINGERPRINT
    jfieldID fingerprint_id = env->GetStaticFieldID(build_class, "FINGERPRINT", "Ljava/lang/String;");
    jstring fingerprint = (jstring)env->GetStaticObjectField(build_class, fingerprint_id);
    const char* fp_ptr = env->GetStringUTFChars(fingerprint, NULL);

    // 获取 Build.MODEL
    jfieldID model_id = env->GetStaticFieldID(build_class, "MODEL", "Ljava/lang/String;");
    jstring model = (jstring)env->GetStaticObjectField(build_class, model_id);
    const char* model_ptr = env->GetStringUTFChars(model, NULL);

    // 获取 Build.HARDWARE
    jfieldID hardware_id = env->GetStaticFieldID(build_class, "HARDWARE", "Ljava/lang/String;");
    jstring hardware = (jstring)env->GetStaticObjectField(build_class, hardware_id);
    const char* hardware_ptr = env->GetStringUTFChars(hardware, NULL);

    bool result = false;
    // 模拟器特征关键词检测
    if (strstr(fp_ptr, "generic") || 
        strstr(fp_ptr, "vbox") || 
        strstr(fp_ptr, "emulator") ||
        strstr(model_ptr, "Emulator") ||
        strstr(model_ptr, "Android SDK built for x86") ||
        strstr(hardware_ptr, "goldfish") ||
        strstr(hardware_ptr, "ranchu") ||
        strstr(hardware_ptr, "vbox86")) {
        LOGE("Emulator detected via Build info (Fingerprint/Model/Hardware).");
        result = true;
    }

    env->ReleaseStringUTFChars(fingerprint, fp_ptr);
    env->ReleaseStringUTFChars(model, model_ptr);
    env->ReleaseStringUTFChars(hardware, hardware_ptr);
    
    if (!result) {
        LOGE("Emulator Check: Device seems to be physical.");
    }
    return result;
}

/**
 * 增强的模拟器检测
 * 结合ptrace和其他技术
 */
bool is_emulator_enhanced(JNIEnv* env) {
    LOGE("Enhanced Emulator Check: Starting...");
    
    if (is_emulator(env)) {
        return true;
    }
    
    long ptrace_result = ptrace(PTRACE_TRACEME, 0, 0, 0);
    if (ptrace_result != 0 && ptrace_result != -1) {
        LOGE("Unusual ptrace behavior - possible emulator");
        return true;
    }
    ptrace(PTRACE_DETACH, 0, 0, 0);
    
    FILE* fp = fopen("/proc/cpuinfo", "r");
    if (fp) {
        char line[512];
        bool has_virtualization = false;
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, "flags") || strstr(line, "Features")) {
                if (strstr(line, "hypervisor") || strstr(line, "svm") || 
                    strstr(line, "vmx")) {
                    has_virtualization = true;
                }
            }
        }
        fclose(fp);
        
        if (has_virtualization) {
            LOGE("Virtualization detected - possible emulator");
            return true;
        }
    }
    
    LOGE("Enhanced Emulator Check: Device seems to be physical.");
    return false;
}

/**
 * 检测硬件特征（传感器、电池、网络等）
 */
bool check_hardware_features(JNIEnv* env, jobject context) {
    jclass context_class = env->FindClass("android/content/Context");
    if (context_class == NULL) {
        return false;
    }
    
    jmethodID getSystemService = env->GetMethodID(context_class, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getSystemService == NULL) {
        return false;
    }
    
    // 检查传感器
    jstring sensor_service = env->NewStringUTF("sensor");
    jobject sensor_manager = env->CallObjectMethod(context, getSystemService, sensor_service);
    
    // 检查是否有异常
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (sensor_manager == NULL) {
        LOGE("Sensor manager not available - possible emulator");
        return true;
    }
    
    // 检查电话服务
    jstring telephony_service = env->NewStringUTF("phone");
    jobject telephony_manager = env->CallObjectMethod(context, getSystemService, telephony_service);
    
    // 检查是否有异常
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (telephony_manager == NULL) {
        LOGE("Telephony manager not available - possible emulator");
        return true;
    }
    
    return false;
}

/**
 * 检测系统属性
 */
bool check_system_properties() {
    const char* prop_files[] = {
        "/system/build.prop",
        "/default.prop",
        "/system/default.prop"
    };
    
    for (int i = 0; i < 3; i++) {
        FILE* fp = fopen(prop_files[i], "r");
        if (fp) {
            char line[512];
            while (fgets(line, sizeof(line), fp)) {
                // 检查模拟器相关的属性
                if (strstr(line, "qemu") || 
                    strstr(line, "goldfish") ||
                    strstr(line, "vbox") ||
                    strstr(line, "genymotion") ||
                    strstr(line, "nox") ||
                    strstr(line, "bluestacks") ||
                    strstr(line, "memu")) {
                    LOGE("Emulator detected via prop file: %s", line);
                    fclose(fp);
                    return true;
                }
            }
            fclose(fp);
        }
    }
    return false;
}

/**
 * 检测电话信息
 */
bool check_telephony_info(JNIEnv* env, jobject context) {
    jclass telephony_class = env->FindClass("android/telephony/TelephonyManager");
    if (telephony_class == NULL) {
        return false;
    }
    
    // 检查是否有异常
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    // 获取设备ID
    jmethodID getDeviceId = env->GetMethodID(telephony_class, "getDeviceId", "()Ljava/lang/String;");
    if (getDeviceId == NULL) {
        return false;
    }
    
    jstring device_id = (jstring)env->CallObjectMethod(context, getDeviceId);
    
    // 检查调用是否成功
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (device_id != NULL) {
        const char* device_id_str = env->GetStringUTFChars(device_id, NULL);
        if (device_id_str == NULL) {
            return false;
        }
        
        // 检查是否为空或异常值
        if (strlen(device_id_str) == 0 || strstr(device_id_str, "000000000000000")) {
            LOGE("Invalid device ID - possible emulator");
            env->ReleaseStringUTFChars(device_id, device_id_str);
            return true;
        }
        env->ReleaseStringUTFChars(device_id, device_id_str);
    }
    
    return false;
}

/**
 * 检测网络特征
 */
bool check_network_features() {
    // 检查是否有以太网接口（模拟器通常有）
    const char* network_files[] = {
        "/sys/class/net/eth0",
        "/sys/class/net/eth1"
    };
    
    for (int i = 0; i < 2; i++) {
        if (access(network_files[i], F_OK) == 0) {
            LOGE("Ethernet interface detected - possible emulator: %s", network_files[i]);
            return true;
        }
    }
    
    return false;
}

/**
 * 检测进程列表中的模拟器进程
 */
bool check_emulator_processes() {
    FILE* fp = fopen("/proc/self/status", "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, "Name:")) {
                if (strstr(line, "qemu") || 
                    strstr(line, "vbox") ||
                    strstr(line, "vmware")) {
                    LOGE("Emulator process detected: %s", line);
                    fclose(fp);
                    return true;
                }
            }
        }
        fclose(fp);
    }
    return false;
}

/**
 * 检测文件系统特征
 */
bool check_filesystem_features() {
    // 检查模拟器特有的目录结构
    const char* emulator_dirs[] = {
        "/data/data/com.android.settings",
        "/data/data/com.android.development",
        "/system/app/SdkSetup"
    };
    
    for (int i = 0; i < 3; i++) {
        struct stat st;
        if (stat(emulator_dirs[i], &st) == 0) {
            LOGE("Emulator directory detected: %s", emulator_dirs[i]);
            return true;
        }
    }
    
    return false;
}

/**
 * 检测网络MAC地址特征
 */
bool check_network_mac() {
    const char* network_files[] = {
        "/sys/class/net/wlan0/address",
        "/sys/class/net/eth0/address"
    };
    
    for (int i = 0; i < 2; i++) {
        FILE* fp = fopen(network_files[i], "r");
        if (fp) {
            char mac[32];
            if (fgets(mac, sizeof(mac), fp)) {
                // 检查是否是已知的模拟器MAC地址
                if (strstr(mac, "02:00:00:00:00:00") || 
                    strstr(mac, "52:54:00:12:34:56") ||
                    strstr(mac, "00:1A:11:00:01:01")) {
                    LOGE("Emulator MAC address detected: %s", mac);
                    fclose(fp);
                    return true;
                }
            }
            fclose(fp);
        }
    }
    return false;
}

/**
 * 检测系统应用特征
 */
bool check_system_apps() {
    const char* emulator_apps[] = {
        "/system/app/Development",
        "/system/app/CustomLocale",
        "/system/app/GpsLocationTest",
        "/system/app/SdkSetup"
    };
    
    for (int i = 0; i < 4; i++) {
        struct stat st;
        if (stat(emulator_apps[i], &st) == 0) {
            LOGE("Emulator system app detected: %s", emulator_apps[i]);
            return true;
        }
    }
    return false;
}

/**
 * 检测CPU核心数和频率特征
 */
bool check_cpu_features() {
    FILE* fp = fopen("/proc/cpuinfo", "r");
    if (fp) {
        int processor_count = 0;
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, "processor")) {
                processor_count++;
            }
        }
        fclose(fp);
        
        // 检查CPU核心数是否异常（模拟器通常核心数较少）
        if (processor_count <= 2) {
            LOGE("Suspicious CPU core count: %d", processor_count);
            return true;
        }
    }
    return false;
}

/**
 * 检测电池特征
 */
bool check_battery_features() {
    FILE* fp = fopen("/sys/class/power_supply/battery/capacity", "r");
    if (fp) {
        int capacity;
        if (fscanf(fp, "%d", &capacity) == 1) {
            fclose(fp);
            // 检查电池容量是否异常（模拟器通常容量固定）
            if (capacity == 50 || capacity == 100) {
                LOGE("Suspicious battery capacity: %d", capacity);
                return true;
            }
        }
        fclose(fp);
    }
    return false;
}

/**
 * 检测设备ID特征
 */
bool check_device_id(JNIEnv* env, jobject context) {
    jclass context_class = env->FindClass("android/content/Context");
    if (context_class == NULL) {
        return false;
    }
    
    jmethodID getSystemService = env->GetMethodID(context_class, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getSystemService == NULL) {
        return false;
    }
    
    jstring telephony_service = env->NewStringUTF("phone");
    jobject telephony_manager = env->CallObjectMethod(context, getSystemService, telephony_service);
    
    if (telephony_manager == NULL || env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    jclass telephony_class = env->GetObjectClass(telephony_manager);
    jmethodID getDeviceId = env->GetMethodID(telephony_class, "getDeviceId", "()Ljava/lang/String;");
    
    jstring device_id = (jstring)env->CallObjectMethod(telephony_manager, getDeviceId);
    
    // 检查是否有异常（Android 10+可能抛出SecurityException）
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (device_id != NULL) {
        const char* device_id_str = env->GetStringUTFChars(device_id, NULL);
        if (device_id_str != NULL) {
            // 检查设备ID是否为空或异常
            if (strlen(device_id_str) == 0 || 
                strstr(device_id_str, "000000000000000") ||
                strstr(device_id_str, "15555215554")) {
                LOGE("Suspicious device ID: %s", device_id_str);
                env->ReleaseStringUTFChars(device_id, device_id_str);
                return true;
            }
            env->ReleaseStringUTFChars(device_id, device_id_str);
        }
    }
    
    return false;
}

/**
 * 检测传感器缺失情况
 * 模拟器通常缺少某些传感器或传感器数据异常
 */
bool check_sensors_missing(JNIEnv* env, jobject context) {
    jclass context_class = env->FindClass("android/content/Context");
    if (context_class == NULL) {
        return false;
    }
    
    jmethodID getSystemService = env->GetMethodID(context_class, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getSystemService == NULL) {
        return false;
    }
    
    jstring sensor_service = env->NewStringUTF("sensor");
    jobject sensor_manager = env->CallObjectMethod(context, getSystemService, sensor_service);
    
    if (sensor_manager == NULL || env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    jclass sensor_manager_class = env->GetObjectClass(sensor_manager);
    jmethodID getSensorList = env->GetMethodID(sensor_manager_class, "getSensorList", "(I)Ljava/util/List;");
    
    // 检查TYPE_ALL传感器列表
    jobject sensor_list = env->CallObjectMethod(sensor_manager, getSensorList, -1);
    
    if (sensor_list == NULL || env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    jclass list_class = env->GetObjectClass(sensor_list);
    jmethodID size_method = env->GetMethodID(list_class, "size", "()I");
    jint sensor_count = env->CallIntMethod(sensor_list, size_method);
    
    // 模拟器通常传感器数量很少（<5）
    if (sensor_count < 5) {
        LOGE("Suspicious sensor count: %d", sensor_count);
        return true;
    }
    
    // 检查是否有温度传感器（模拟器通常没有）
    jmethodID get_method = env->GetMethodID(list_class, "get", "(I)Ljava/lang/Object;");
    jclass sensor_class = env->FindClass("android/hardware/Sensor");
    jmethodID getType = env->GetMethodID(sensor_class, "getType", "()I");
    
    bool has_temperature = false;
    bool has_pressure = false;
    bool has_humidity = false;
    
    for (int i = 0; i < sensor_count; i++) {
        jobject sensor = env->CallObjectMethod(sensor_list, get_method, i);
        if (sensor != NULL) {
            jint type = env->CallIntMethod(sensor, getType);
            
            // TYPE_AMBIENT_TEMPERATURE = 13
            if (type == 13) {
                has_temperature = true;
            }
            // TYPE_PRESSURE = 6
            if (type == 6) {
                has_pressure = true;
            }
            // TYPE_RELATIVE_HUMIDITY = 12
            if (type == 12) {
                has_humidity = true;
            }
            
            env->DeleteLocalRef(sensor);
        }
    }
    
    // 真实设备通常有温度、压力、湿度传感器中的至少一个
    if (!has_temperature && !has_pressure && !has_humidity) {
        LOGE("Missing environmental sensors (temperature/pressure/humidity)");
        return true;
    }
    
    return false;
}

/**
 * 检测蓝牙功能
 * 模拟器通常没有真实的蓝牙硬件
 */
bool check_bluetooth_missing(JNIEnv* env, jobject context) {
    jclass context_class = env->FindClass("android/content/Context");
    if (context_class == NULL) {
        return false;
    }
    
    jmethodID getSystemService = env->GetMethodID(context_class, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getSystemService == NULL) {
        return false;
    }
    
    jstring bluetooth_service = env->NewStringUTF("bluetooth");
    jobject bluetooth_manager = env->CallObjectMethod(context, getSystemService, bluetooth_service);
    
    if (bluetooth_manager == NULL || env->ExceptionCheck()) {
        env->ExceptionClear();
        LOGE("Bluetooth manager not available - possible emulator");
        return true;
    }
    
    return false;
}

/**
 * 检测SIM卡信息
 * 模拟器通常没有真实的SIM卡
 */
bool check_sim_info(JNIEnv* env, jobject context) {
    jclass context_class = env->FindClass("android/content/Context");
    if (context_class == NULL) {
        return false;
    }
    
    jmethodID getSystemService = env->GetMethodID(context_class, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getSystemService == NULL) {
        return false;
    }
    
    jstring telephony_service = env->NewStringUTF("phone");
    jobject telephony_manager = env->CallObjectMethod(context, getSystemService, telephony_service);
    
    if (telephony_manager == NULL || env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    jclass telephony_class = env->GetObjectClass(telephony_manager);
    
    // 检查SIM卡序列号
    jmethodID getSimSerialNumber = env->GetMethodID(telephony_class, "getSimSerialNumber", "()Ljava/lang/String;");
    jstring sim_serial = (jstring)env->CallObjectMethod(telephony_manager, getSimSerialNumber);
    
    // 检查是否有异常（Android 10+可能抛出SecurityException）
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (sim_serial != NULL) {
        const char* sim_serial_str = env->GetStringUTFChars(sim_serial, NULL);
        if (sim_serial_str != NULL) {
            // 检查SIM卡序列号是否为空或异常
            if (strlen(sim_serial_str) == 0 || 
                strstr(sim_serial_str, "89014103211118510720") ||
                strstr(sim_serial_str, "000000000000000")) {
                LOGE("Suspicious SIM serial number: %s", sim_serial_str);
                env->ReleaseStringUTFChars(sim_serial, sim_serial_str);
                return true;
            }
            env->ReleaseStringUTFChars(sim_serial, sim_serial_str);
        }
    }
    
    // 检查IMSI
    jmethodID getSubscriberId = env->GetMethodID(telephony_class, "getSubscriberId", "()Ljava/lang/String;");
    jstring imsi = (jstring)env->CallObjectMethod(telephony_manager, getSubscriberId);
    
    // 检查是否有异常（Android 10+可能抛出SecurityException）
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (imsi != NULL) {
        const char* imsi_str = env->GetStringUTFChars(imsi, NULL);
        if (imsi_str != NULL) {
            // 检查IMSI是否为空或异常
            if (strlen(imsi_str) == 0 || 
                strstr(imsi_str, "310260000000000") ||
                strstr(imsi_str, "000000000000000")) {
                LOGE("Suspicious IMSI: %s", imsi_str);
                env->ReleaseStringUTFChars(imsi, imsi_str);
                return true;
            }
            env->ReleaseStringUTFChars(imsi, imsi_str);
        }
    }
    
    return false;
}

/**
 * 检测运营商信息
 * 模拟器通常没有真实的运营商信息
 */
bool check_operator_info(JNIEnv* env, jobject context) {
    jclass context_class = env->FindClass("android/content/Context");
    if (context_class == NULL) {
        return false;
    }
    
    jmethodID getSystemService = env->GetMethodID(context_class, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getSystemService == NULL) {
        return false;
    }
    
    jstring telephony_service = env->NewStringUTF("phone");
    jobject telephony_manager = env->CallObjectMethod(context, getSystemService, telephony_service);
    
    if (telephony_manager == NULL || env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    jclass telephony_class = env->GetObjectClass(telephony_manager);
    
    // 检查运营商名称
    jmethodID getNetworkOperatorName = env->GetMethodID(telephony_class, "getNetworkOperatorName", "()Ljava/lang/String;");
    jstring operator_name = (jstring)env->CallObjectMethod(telephony_manager, getNetworkOperatorName);
    
    // 检查是否有异常（Android 10+可能抛出SecurityException）
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    if (operator_name != NULL) {
        const char* operator_name_str = env->GetStringUTFChars(operator_name, NULL);
        if (operator_name_str != NULL) {
            // 检查运营商名称是否为空或异常
            if (strlen(operator_name_str) == 0 || 
                strstr(operator_name_str, "Android") ||
                strstr(operator_name_str, "unknown")) {
                LOGE("Suspicious operator name: %s", operator_name_str);
                env->ReleaseStringUTFChars(operator_name, operator_name_str);
                return true;
            }
            env->ReleaseStringUTFChars(operator_name, operator_name_str);
        }
    }
    
    return false;
}

/**
 * 超严格模拟器检测
 * 综合所有检测方法
 */
bool is_emulator_strict(JNIEnv* env, jobject context) {
    LOGE("Strict Emulator Check: Starting comprehensive detection...");
    
    int emulator_score = 0;
    
    // 基础检测
    if (is_emulator(env)) {
        emulator_score += 3;
        LOGE("Basic emulator check passed (+3)");
    }
    
    // 系统属性检测
    if (check_system_properties()) {
        emulator_score += 2;
        LOGE("System properties check passed (+2)");
    }
    
    // 网络特征检测
    if (check_network_features()) {
        emulator_score += 1;
        LOGE("Network features check passed (+1)");
    }
    
    // 进程检测
    if (check_emulator_processes()) {
        emulator_score += 2;
        LOGE("Emulator processes check passed (+2)");
    }
    
    // 文件系统检测
    if (check_filesystem_features()) {
        emulator_score += 1;
        LOGE("Filesystem features check passed (+1)");
    }
    
    // MAC地址检测
    if (check_network_mac()) {
        emulator_score += 2;
        LOGE("Network MAC check passed (+2)");
    }
    
    // 系统应用检测
    if (check_system_apps()) {
        emulator_score += 1;
        LOGE("System apps check passed (+1)");
    }
    
    // CPU特征检测
    if (check_cpu_features()) {
        emulator_score += 1;
        LOGE("CPU features check passed (+1)");
    }
    
    // 电池特征检测
    if (check_battery_features()) {
        emulator_score += 1;
        LOGE("Battery features check passed (+1)");
    }
    
    // 设备ID检测 - 在非模拟器模式下启用
    if (!g_is_emulator && check_device_id(env, context)) {
        emulator_score += 2;
        LOGE("Device ID check passed (+2)");
    }
    
    // 传感器缺失检测（高级检测）
    if (check_sensors_missing(env, context)) {
        emulator_score += 3;
        LOGE("Sensors missing check passed (+3)");
    }
    
    // 蓝牙缺失检测（高级检测）
    if (check_bluetooth_missing(env, context)) {
        emulator_score += 2;
        LOGE("Bluetooth missing check passed (+2)");
    }
    
    // SIM卡信息检测（高级检测）
    if (check_sim_info(env, context)) {
        emulator_score += 2;
        LOGE("SIM info check passed (+2)");
    }
    
    // 运营商信息检测（高级检测）
    if (check_operator_info(env, context)) {
        emulator_score += 2;
        LOGE("Operator info check passed (+2)");
    }
    
    // Ptrace行为检测
    long ptrace_result = ptrace(PTRACE_TRACEME, 0, 0, 0);
    if (ptrace_result != 0 && ptrace_result != -1) {
        emulator_score += 1;
        LOGE("Ptrace behavior check passed (+1)");
    }
    ptrace(PTRACE_DETACH, 0, 0, 0);
    
    // CPU虚拟化检测
    FILE* fp = fopen("/proc/cpuinfo", "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, "hypervisor") || strstr(line, "svm") || 
                strstr(line, "vmx")) {
                emulator_score += 2;
                LOGE("CPU virtualization check passed (+2)");
                break;
            }
        }
        fclose(fp);
    }
    
    LOGE("Total emulator score: %d", emulator_score);
    
    // 阈值判断：得分 >= 2 认为是模拟器
    if (emulator_score >= 2) {
        LOGE("STRICT CHECK: Emulator detected (score=%d)", emulator_score);
        return true;
    }
    
    LOGE("STRICT CHECK: Device seems to be physical (score=%d)", emulator_score);
    return false;
}