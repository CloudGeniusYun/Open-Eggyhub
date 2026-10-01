package com.eggyhub.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.eggyhub.android.log.AppLogger;
import com.eggyhub.android.utils.SignatureUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 安全存储管理器
 * 提供敏感数据的加密存储和解密读取功能
 * 
 * 特性：
 * - AES-256-GCM 加密算法
 * - PBKDF2 密钥派生（一机一码）
 * - 自动迁移机制（明文→加密）
 * - 完整性校验（防止篡改）
 */
public class SecureStorageManager {
    
    private static final String TAG = "SecureStorageManager";
    private static final String PREFS_NAME = "secure_storage";
    private static final String KEY_SALT = "crypto_salt";
    private static final String PREFIX_ENCRYPTED = "encrypted_";
    
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    
    private static Context appContext;
    private static SharedPreferences securePrefs;
    private static SharedPreferences userPrefs;
    private static Gson gson;
    private static Set<String> migratedKeys;
    private static SecretKey cachedKey;
    
    public static void init(Context context) {
        AppLogger.d(TAG, "=== SecureStorageManager.init() called ===");
        appContext = context.getApplicationContext();
        securePrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userPrefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        gson = new Gson();
        migratedKeys = new HashSet<>();
        
        AppLogger.d(TAG, "Checking salt...");
        if (!hasSalt()) {
            AppLogger.d(TAG, "No salt found, generating new salt");
            generateAndSaveSalt();
        } else {
            AppLogger.d(TAG, "Salt found, using existing salt");
        }
        
        AppLogger.d(TAG, "SecureStorageManager initialized");
    }
    
    public static void encryptAndStore(String key, String plaintext) {
        if (appContext == null) {
            AppLogger.e(TAG, "SecureStorageManager not initialized");
            return;
        }
        
        if (plaintext == null || plaintext.isEmpty()) {
            remove(key);
            return;
        }
        
        try {
            SecretKey secretKey = getOrCreateSecretKey();
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            EncryptedData encryptedData = new EncryptedData(
                Base64.encodeToString(iv, Base64.NO_WRAP),
                Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            );
            
            String encryptedJson = gson.toJson(encryptedData);
            
            String encryptedKey = PREFIX_ENCRYPTED + key;
            securePrefs.edit().putString(encryptedKey, encryptedJson).apply();
            
            userPrefs.edit().remove(key).apply();
            
            AppLogger.d(TAG, "Data encrypted and stored: " + key);
            
        } catch (Exception e) {
            AppLogger.e(TAG, "Failed to encrypt data: " + key, e);
        }
    }
    
    public static String decryptAndRetrieve(String key) {
        if (appContext == null) {
            AppLogger.e(TAG, "SecureStorageManager not initialized");
            return null;
        }
        
        String encryptedKey = PREFIX_ENCRYPTED + key;
        String encryptedJson = securePrefs.getString(encryptedKey, null);
        
        AppLogger.d(TAG, "Attempting to decrypt: " + key);
        AppLogger.d(TAG, "Encrypted data exists: " + (encryptedJson != null));
        
        if (encryptedJson != null) {
            try {
                SecretKey secretKey = getOrCreateSecretKey();
                
                EncryptedData encryptedData = gson.fromJson(encryptedJson, EncryptedData.class);
                
                byte[] iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP);
                byte[] ciphertext = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP);
                
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                
                byte[] plaintext = cipher.doFinal(ciphertext);
                
                AppLogger.d(TAG, "Data decrypted successfully: " + key);
                return new String(plaintext, StandardCharsets.UTF_8);
                
            } catch (Exception e) {
                AppLogger.e(TAG, "Failed to decrypt data: " + key, e);
                return null;
            }
        }
        
        String plaintext = userPrefs.getString(key, null);
        if (plaintext != null) {
            AppLogger.d(TAG, "Migrating plaintext data: " + key);
            encryptAndStore(key, plaintext);
            return plaintext;
        }
        
        try {
            int intValue = userPrefs.getInt(key, Integer.MIN_VALUE);
            if (intValue != Integer.MIN_VALUE) {
                AppLogger.d(TAG, "Migrating integer data: " + key);
                String strValue = String.valueOf(intValue);
                encryptAndStore(key, strValue);
                return strValue;
            }
        } catch (ClassCastException e) {
            AppLogger.d(TAG, "Key is not an integer: " + key);
        }
        
        AppLogger.d(TAG, "No data found for key: " + key);
        return null;
    }
    
    /**
     * 获取访问令牌（统一入口）
     * 从加密存储中获取 access_token，所有需要 token 的地方都应该使用此方法
     * 
     * @return 解密后的 access_token，如果不存在则返回 null
     */
    public static String getAccessToken() {
        return decryptAndRetrieve("access_token");
    }
    
    public static void encryptAndStoreInt(String key, int value) {
        encryptAndStore(key, String.valueOf(value));
    }
    
    public static int decryptAndRetrieveInt(String key, int defaultValue) {
        String value = decryptAndRetrieve(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            AppLogger.e(TAG, "Failed to parse int: " + key, e);
            return defaultValue;
        }
    }
    
    public static void remove(String key) {
        String encryptedKey = PREFIX_ENCRYPTED + key;
        securePrefs.edit().remove(encryptedKey).apply();
        userPrefs.edit().remove(key).apply();
        AppLogger.d(TAG, "Data removed: " + key);
    }
    
    public static void clearAll() {
        securePrefs.edit().clear().apply();
        cachedKey = null;
        AppLogger.d(TAG, "All encrypted data cleared");
    }
    
    public static boolean isMigrated(String key) {
        String encryptedKey = PREFIX_ENCRYPTED + key;
        return migratedKeys.contains(key) || securePrefs.contains(encryptedKey);
    }
    
    public static void migrateAll() {
        String[] keysToMigrate = {
            "email", "password", "access_token", 
            "username", "role", "sponser", "id"
        };
        
        for (String key : keysToMigrate) {
            String value = decryptAndRetrieve(key);
            if (value != null) {
                migratedKeys.add(key);
                AppLogger.d(TAG, "Migrated: " + key);
            }
        }
        
        AppLogger.d(TAG, "Migration completed");
    }
    
    private static boolean hasSalt() {
        String saltBase64 = securePrefs.getString(KEY_SALT, null);
        return saltBase64 != null && !saltBase64.isEmpty();
    }
    
    private static void generateAndSaveSalt() {
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        securePrefs.edit().putString(KEY_SALT, saltBase64).apply();
        AppLogger.d(TAG, "Salt generated and saved");
    }
    
    private static byte[] getSalt() {
        String saltBase64 = securePrefs.getString(KEY_SALT, null);
        if (saltBase64 == null) {
            return null;
        }
        return Base64.decode(saltBase64, Base64.NO_WRAP);
    }
    
    private static SecretKey getOrCreateSecretKey() throws Exception {
        if (cachedKey != null) {
            AppLogger.d(TAG, "Using cached secret key");
            return cachedKey;
        }
        
        String deviceId = SignatureUtils.getDeviceID(appContext);
        AppLogger.d(TAG, "Device ID: " + deviceId);
        
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = "default_device_id";
            AppLogger.w(TAG, "Device ID is empty, using default");
        }
        
        String packageName = appContext.getPackageName();
        AppLogger.d(TAG, "Package name: " + packageName);
        
        byte[] salt = getSalt();
        if (salt == null) {
            throw new Exception("Salt not found");
        }
        
        String keyMaterial = deviceId + packageName;
        AppLogger.d(TAG, "Key material length: " + keyMaterial.length());
        
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            keyMaterial.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        );
        
        SecretKey tmp = factory.generateSecret(spec);
        cachedKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        
        AppLogger.d(TAG, "Secret key derived successfully");
        return cachedKey;
    }
    
    private static class EncryptedData {
        @SerializedName("iv")
        String iv;
        
        @SerializedName("ciphertext")
        String ciphertext;
        
        EncryptedData(String iv, String ciphertext) {
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }
}
