#include "security_utils.h"
#include <android/log.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <stdlib.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <signal.h>
#include <errno.h>
#include <string.h>

#define LOG_TAG "AntiDebug"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool g_is_emulator = false;
volatile bool g_should_stop_monitor = false;

void set_emulator_mode(bool is_emu) {
    g_is_emulator = is_emu;
}

/**
 * TracerPid 检测 (核心反调试逻辑)
 */
void check_anti_debug_tracer_pid() {
    // 模拟器模式下跳过 TracerPid 检测，因为 Android Studio 调试器会设置 TracerPid
    if (g_is_emulator) {
        return;
    }
    
    // LOGE("Anti-Debug: Checking TracerPid via /proc/self/status...");
    const int MAX_LINE = 512;
    char line[MAX_LINE];
    FILE* fp = fopen("/proc/self/status", "r");
    
    if (fp) {
        int tracer_pid = 0;
        while (fgets(line, MAX_LINE, fp)) {
            if (strncmp(line, "TracerPid:", 10) == 0) {
                tracer_pid = atoi(&line[10]);
                break;
            }
        }
        fclose(fp);

        if (tracer_pid != 0) {
            // 检查TracerPid是否是当前进程的父进程（正常调试情况）
            pid_t ppid = getppid();
            if (tracer_pid == ppid) {
                LOGE("Anti-Debug: TracerPid=%d matches parent process (normal debug), skipping", tracer_pid);
                return;
            }
            
            // 检查父进程是否是已知的调试器进程
            char parent_cmdline[256] = {0};
            snprintf(parent_cmdline, sizeof(parent_cmdline), "/proc/%d/cmdline", ppid);
            
            FILE* fp2 = fopen(parent_cmdline, "r");
            if (fp2) {
                char cmdline[256];
                if (fgets(cmdline, sizeof(cmdline), fp2)) {
                    if (strstr(cmdline, "adb") || strstr(cmdline, "studio64") || 
                        strstr(cmdline, "java") || strstr(cmdline, "dalvik")) {
                        LOGE("Anti-Debug: Parent is known debugger (%s), TracerPid=%d is safe", cmdline, tracer_pid);
                        fclose(fp2);
                        return;
                    }
                }
                fclose(fp2);
            }
            
            // 如果不是正常调试情况，才认为是真正的调试器攻击
            LOGE("Anti-Debug: Debugger detected! TracerPid = %d (not parent process)", tracer_pid);
            exit(0);
        } else {
            // LOGE("Anti-Debug: No tracer detected (TracerPid = 0).");
        }
    } else {
        LOGE("Anti-Debug: Could not open /proc/self/status");
    }
}

/**
 * Hook 检测
 * 扫描 /proc/self/maps 查找 Frida/Xposed 特征库
 */
void check_hook_framework() {
    // LOGE("Checking Hook framework...");
    char line[512];
    FILE* fp = fopen("/proc/self/maps", "r");
    if (fp) {
        while (fgets(line, sizeof(line), fp)) {
            // 检查常见的 Hook 框架特征字符串
            if (strstr(line, "frida") || 
                strstr(line, "xposed") || 
                strstr(line, "edxp") || 
                strstr(line, "lsposed") || 
                strstr(line, "substrate")) {
                
                LOGE("Anti-Hook: Hook framework detected in map: %s", line);
                fclose(fp);
                // 发现 Hook 框架，立即自杀
                kill(getpid(), SIGKILL);
                exit(0);
            }
        }
        fclose(fp);
    }
    // LOGE("Hook framework check passed.");
}

/**
 * 传统的 ptrace 反调试
 * 仅在非模拟器环境或已知安全的架构上调用
 */
void check_anti_debug_ptrace() {
    LOGE("Anti-Debug: Entering ptrace check...");
    long result = ptrace(PTRACE_TRACEME, 0, 0, 0);
    if (result < 0) {
        LOGE("Anti-Debug: ptrace failed! Debugger might be present.");
        exit(0);
    }
    LOGE("Anti-Debug: ptrace check passed.");
}

/**
 * 实时反调试监控线程
 * 每隔 3 秒检查一次 TracerPid 和 Hook 框架
 */
void* anti_debug_monitor_thread(void* arg) {
    LOGE("Anti-Debug Monitor: Thread started. Watching you...");
    
    while (!g_should_stop_monitor) {
        check_anti_debug_tracer_pid();
        check_hook_framework();
        
        if (!g_is_emulator) {
            check_ptrace_behavior();
            check_syscall_interception();
        }
        
        // 使用sleep(1)循环3次，这样可以更快响应退出信号
        for (int i = 0; i < 3 && !g_should_stop_monitor; i++) {
            sleep(1);
        }
    }
    LOGE("Anti-Debug Monitor: Thread stopped.");
    return nullptr;
}

/**
 * Ptrace行为差异检测
 * 检测模拟器的ptrace实现异常
 */
void check_ptrace_behavior() {
    long result = ptrace(PTRACE_TRACEME, 0, 0, 0);
    
    if (result == -1) {
        int error_code = errno;
        if (error_code == EPERM || error_code == EACCES) {
            LOGE("Ptrace permission denied (errno=%d) - normal on Android, no debugger", error_code);
            ptrace(PTRACE_DETACH, 0, 0, 0);
            return;
        }
        LOGE("Debugger detected via ptrace behavior (errno=%d)", error_code);
        exit(0);
    }
    
    if (result != 0) {
        LOGE("Suspicious ptrace behavior detected - possible emulator");
    }
    
    ptrace(PTRACE_DETACH, 0, 0, 0);
}

/**
 * 防止Ptrace注入
 * 多次ptrace检测，检查父进程
 */
void prevent_ptrace_injection() {
    for (int i = 0; i < 3; i++) {
        long result = ptrace(PTRACE_TRACEME, 0, 0, 0);
        if (result == -1) {
            int error_code = errno;
            if (error_code == EPERM || error_code == EACCES) {
                LOGE("Ptrace permission denied (errno=%d) - normal on Android, no injection", error_code);
                ptrace(PTRACE_DETACH, 0, 0, 0);
                continue;
            }
            LOGE("Ptrace injection attempt detected (errno=%d)", error_code);
            exit(0);
        }
        ptrace(PTRACE_DETACH, 0, 0, 0);
    }
    
    pid_t ppid = getppid();
    char parent_cmdline[256] = {0};
    snprintf(parent_cmdline, sizeof(parent_cmdline), "/proc/%d/cmdline", ppid);
    
    FILE* fp = fopen(parent_cmdline, "r");
    if (fp) {
        char cmdline[256];
        if (fgets(cmdline, sizeof(cmdline), fp)) {
            if (strstr(cmdline, "gdb") || strstr(cmdline, "lldb") || 
                strstr(cmdline, "frida") || strstr(cmdline, "xposed")) {
                LOGE("Suspicious parent process: %s", cmdline);
                fclose(fp);
                exit(0);
            }
        }
        fclose(fp);
    }
}

/**
 * 系统调用拦截检测
 * 检查TracerPid和可疑的库映射
 */
void check_syscall_interception() {
    FILE* fp = fopen("/proc/self/status", "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strncmp(line, "TracerPid:", 10) == 0) {
                int tracer_pid = atoi(&line[10]);
                if (tracer_pid != 0) {
                    LOGE("Syscall interception detected (TracerPid=%d)", tracer_pid);
                    fclose(fp);
                    exit(0);
                }
                break;
            }
        }
        fclose(fp);
    }
    
    fp = fopen("/proc/self/maps", "r");
    if (fp) {
        char line[1024];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, "ptrace") || strstr(line, "injection")) {
                LOGE("Suspicious library detected: %s", line);
                fclose(fp);
                exit(0);
            }
        }
        fclose(fp);
    }
}