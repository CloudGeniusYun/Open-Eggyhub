package com.eggyhub.android.utils;

import android.content.Context;
import android.util.Log;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * 代理拦截器
 * 自动替换 URL 并添加 x-request-id 头
 */
public class ProxyInterceptor implements Interceptor {
    
    private static final String TAG = "ProxyInterceptor";
    private static final String X_REQUEST_ID = "x-request-id";
    
    private final Context context;
    
    public ProxyInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // 检查是否启用代理
        if (!ProxyConfig.isProxyEnabled(context)) {
            // 不启用代理，直接请求
            return chain.proceed(originalRequest);
        }
        
        // 获取原始 URL
        HttpUrl originalUrl = originalRequest.url();
        String originalUrlStr = originalUrl.toString();
        
        // 检查是否是 eggyhub.top 的请求
        if (!originalUrlStr.startsWith(ProxyConfig.DEFAULT_BASE_URL)) {
            // 不是 eggyhub.top 的请求，直接请求
            return chain.proceed(originalRequest);
        }
        
        // 替换 URL
        String newUrlStr = originalUrlStr.replace(
            ProxyConfig.DEFAULT_BASE_URL, 
            ProxyConfig.PROXY_BASE_URL
        );
        HttpUrl newUrl = HttpUrl.parse(newUrlStr);
        
        if (newUrl == null) {
            Log.e(TAG, "Failed to parse new URL: " + newUrlStr);
            return chain.proceed(originalRequest);
        }
        
        // 获取请求方法和路径
        String method = originalRequest.method();
        String path = newUrl.encodedPath();
        if (newUrl.encodedQuery() != null) {
            path = path + "?" + newUrl.encodedQuery();
        }
        
        // 生成 token
        String token = ProxyTokenGenerator.generateToken(method, path);
        
        if (token == null) {
            Log.e(TAG, "Failed to generate token");
            return chain.proceed(originalRequest);
        }
        
        // 构造新请求
        Request newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .addHeader(X_REQUEST_ID, token)
            .build();
        
        Log.d(TAG, "Proxy request: " + method + " " + path);
        Log.d(TAG, "Token: " + token);
        
        return chain.proceed(newRequest);
    }
}