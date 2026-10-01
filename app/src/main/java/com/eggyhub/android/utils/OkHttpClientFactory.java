package com.eggyhub.android.utils;

import android.content.Context;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

/**
 * OkHttpClient 工具类
 * 统一配置 OkHttpClient，包括代理拦截器和超时设置
 */
public class OkHttpClientFactory {
    
    private static OkHttpClient sharedInstance = null;
    private static Context appContext = null;
    
    /**
     * 初始化工厂（在 Application 中调用）
     * @param context Application Context
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }
    
    /**
     * 获取全局共享的 OkHttpClient 实例
     * 包含代理拦截器和默认超时设置
     * @return OkHttpClient 实例
     */
    public static OkHttpClient getSharedClient() {
        if (sharedInstance == null) {
            synchronized (OkHttpClientFactory.class) {
                if (sharedInstance == null) {
                    sharedInstance = createDefaultClient();
                }
            }
        }
        return sharedInstance;
    }
    
    /**
     * 创建默认的 OkHttpClient
     * 包含代理拦截器和超时设置
     * @return OkHttpClient 实例
     */
    private static OkHttpClient createDefaultClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS);
        
        // 添加代理拦截器（如果已初始化）
        if (appContext != null) {
            builder.addInterceptor(new ProxyInterceptor(appContext));
        }
        
        return builder.build();
    }
    
    /**
     * 创建自定义 OkHttpClient（用于特殊场景）
     * @param connectTimeout 连接超时（秒）
     * @param writeTimeout 写入超时（秒）
     * @param readTimeout 读取超时（秒）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createCustomClient(int connectTimeout, int writeTimeout, int readTimeout) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS);
        
        // 添加代理拦截器
        if (appContext != null) {
            builder.addInterceptor(new ProxyInterceptor(appContext));
        }
        
        return builder.build();
    }
    
    /**
     * 创建不带代理拦截器的 OkHttpClient（用于特殊场景）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createClientWithoutProxy() {
        return new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }
}