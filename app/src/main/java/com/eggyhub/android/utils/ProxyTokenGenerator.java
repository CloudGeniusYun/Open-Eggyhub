package com.eggyhub.android.utils;

import android.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * 代理 Token 生成器
 * 实现 HMAC-SHA256 签名算法
 */
public class ProxyTokenGenerator {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 生成代理请求的鉴权 token
     * 
     * @param method HTTP 方法（GET/POST/PUT/DELETE）
     * @param path   完整路径 + 查询字符串，必须以 / 开头，例如 "/api/xxx?id=1"
     * @return 放在 x-request-id 头里的 token，格式：base64url(sig):timestamp:nonce
     */
    public static String generateToken(String method, String path) {
        // 从 Native 层获取密钥
        String secret = ProxyConfig.getProxySecretNative();
        if (secret == null || secret.isEmpty()) {
            return null;
        }
        
        try {
            // 1. 生成时间戳（秒）
            long timestampSec = System.currentTimeMillis() / 1000;
            
            // 2. 生成 16 字节随机 nonce（32 个 hex 字符）
            byte[] nonceBytes = new byte[16];
            secureRandom.nextBytes(nonceBytes);
            String nonce = bytesToHex(nonceBytes);
            
            // 3. 构造 payload
            String payload = timestampSec + "\n" + 
                            method.toUpperCase() + "\n" + 
                            path + "\n" + 
                            nonce;
            
            // 4. 计算 HMAC-SHA256
            byte[] signature = hmacSha256(secret, payload);
            
            // 5. Base64 URL 编码
            String signatureBase64Url = base64UrlEncode(signature);
            
            // 6. 组合 token
            return signatureBase64Url + ":" + timestampSec + ":" + nonce;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * HMAC-SHA256 计算
     */
    private static byte[] hmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes("UTF-8"), HMAC_SHA256);
        mac.init(keySpec);
        return mac.doFinal(payload.getBytes("UTF-8"));
    }
    
    /**
     * Base64 URL 编码（去除 +/=）
     */
    private static String base64UrlEncode(byte[] data) {
        String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
        // 替换 + 为 -，/ 为 _，去除末尾的 =
        return base64.replace("+", "-").replace("/", "_").replace("=", "");
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}