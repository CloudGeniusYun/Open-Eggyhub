package com.eggyhub.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 代理配置类
 * 管理网络模式（默认/代理）和获取密钥
 */
public class ProxyConfig {
    
    private static final String PREFS_NAME = "proxy_config";
    private static final String KEY_PROXY_ENABLED = "proxy_enabled";
    
    // API 基底 URL
    public static final String DEFAULT_BASE_URL = "https://eggyhub.top";
    public static final String PROXY_BASE_URL = "http://proxy.eggyhub.top";
    
    // 加载 Native 库
    static {
        System.loadLibrary("eggyhub_native");
    }
    
    /**
     * 从 Native 层获取代理密钥
     * @return 代理密钥
     */
    public static native String getProxySecretNative();
    
    /**
     * 检查是否启用代理
     * @param context 上下文
     * @return 是否启用代理
     */
    public static boolean isProxyEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PROXY_ENABLED, false);
    }
    
    /**
     * 设置是否启用代理
     * @param context 上下文
     * @param enabled 是否启用代理
     */
    public static void setProxyEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PROXY_ENABLED, enabled).apply();
    }
    
    /**
     * 获取当前 API 基底 URL
     * @param context 上下文
     * @return 当前基底 URL
     */
    public static String getBaseUrl(Context context) {
        if (isProxyEnabled(context)) {
            return PROXY_BASE_URL;
        }
        return DEFAULT_BASE_URL;
    }
    
    /**
     * 替换 URL（将默认 URL 替换为代理 URL）
     * @param context 上下文
     * @param originalUrl 原始 URL
     * @return 替换后的 URL
     */
    public static String replaceUrl(Context context, String originalUrl) {
        if (!isProxyEnabled(context)) {
            return originalUrl; // 不启用代理，返回原始 URL
        }
        
        // 替换 URL
        if (originalUrl.startsWith(DEFAULT_BASE_URL)) {
            return originalUrl.replace(DEFAULT_BASE_URL, PROXY_BASE_URL);
        }
        
        return originalUrl;
    }
}