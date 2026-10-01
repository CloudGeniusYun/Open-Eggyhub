package com.eggyhub.android.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class IntegrityChecker {
    private static final String TAG = "IntegrityChecker";
    
    private static final Map<String, String> DEX_HASHES = new HashMap<>();
    private static final Map<String, String> CLASS_HASHES = new HashMap<>();
    private static final Map<String, String> RESOURCE_HASHES = new HashMap<>();
    private static final Map<String, String> NATIVE_LIB_HASHES = new HashMap<>();
    
    private static boolean initialized = false;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 2 * 60 * 1000;
    
    static {
        initHashMaps();
    }
    
    private static void initHashMaps() {
        DEX_HASHES.clear();
        CLASS_HASHES.clear();
        RESOURCE_HASHES.clear();
        NATIVE_LIB_HASHES.clear();
    }
    
    public static void initialize(Context context) {
        if (initialized) {
            Log.i(TAG, "IntegrityChecker already initialized");
            return;
        }
        
        Log.i(TAG, "Initializing IntegrityChecker...");
        
        try {
            String apkPath = context.getPackageCodePath();
            Log.i(TAG, "APK path: " + apkPath);
            
            ZipFile zipFile = new ZipFile(apkPath);
            Log.i(TAG, "ZIP file opened successfully");
            
            initDexHashes(zipFile);
            initClassHashes(context);
            initResourceHashes(zipFile);
            initNativeLibHashes(zipFile);
            
            zipFile.close();
            
            initialized = true;
            Log.i(TAG, "IntegrityChecker initialized successfully");
            Log.i(TAG, "DEX hashes: " + DEX_HASHES.size());
            Log.i(TAG, "Class hashes: " + CLASS_HASHES.size());
            Log.i(TAG, "Resource hashes: " + RESOURCE_HASHES.size());
            Log.i(TAG, "Native lib hashes: " + NATIVE_LIB_HASHES.size());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize IntegrityChecker", e);
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Exception message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void initDexHashes(ZipFile zipFile) throws Exception {
        Log.i(TAG, "Initializing DEX hashes...");
        
        int dexIndex = 0;
        while (true) {
            String dexName = dexIndex == 0 ? "classes.dex" : "classes" + dexIndex + ".dex";
            ZipEntry entry = zipFile.getEntry(dexName);
            
            if (entry == null) {
                break;
            }
            
            String hash = calculateZipEntryHash(zipFile, entry);
            DEX_HASHES.put(dexName, hash);
            
            Log.i(TAG, "DEX hash: " + dexName + " = " + hash);
            
            dexIndex++;
        }
    }
    
    private static void initClassHashes(Context context) throws Exception {
        Log.i(TAG, "Initializing class hashes...");
        
        String[] criticalClasses = {
            "com.eggyhub.android.EggyApp",
            "com.eggyhub.android.SecurityInterceptor",
            "com.eggyhub.android.AuthActivity",
            "com.eggyhub.android.utils.SignatureUtils"
        };
        
        for (String className : criticalClasses) {
            String hash = calculateClassHash(context, className);
            if (hash != null) {
                CLASS_HASHES.put(className, hash);
                Log.i(TAG, "Class hash: " + className + " = " + hash);
            }
        }
    }
    
    private static void initResourceHashes(ZipFile zipFile) throws Exception {
        Log.i(TAG, "Initializing resource hashes...");
        
        String[] criticalResources = {
            "AndroidManifest.xml",
            "res/values/strings.xml",
            "res/values/arrays.xml"
        };
        
        for (String resource : criticalResources) {
            ZipEntry entry = zipFile.getEntry(resource);
            if (entry != null) {
                String hash = calculateZipEntryHash(zipFile, entry);
                RESOURCE_HASHES.put(resource, hash);
                Log.i(TAG, "Resource hash: " + resource + " = " + hash);
            }
        }
    }
    
    private static void initNativeLibHashes(ZipFile zipFile) throws Exception {
        Log.i(TAG, "Initializing native lib hashes...");
        
        String[] abis = getSupportedAbis();
        for (String abi : abis) {
            String libPath = "lib/" + abi + "/libeggyhub_native.so";
            ZipEntry entry = zipFile.getEntry(libPath);
            
            if (entry != null) {
                String hash = calculateZipEntryHash(zipFile, entry);
                NATIVE_LIB_HASHES.put(libPath, hash);
                Log.i(TAG, "Native lib hash: " + libPath + " = " + hash);
                break;
            }
        }
    }
    
    public static boolean verifyAll(Context context) {
        if (!initialized) {
            Log.w(TAG, "IntegrityChecker not initialized, skipping verification");
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            Log.i(TAG, "Skipping check, interval not reached");
            return true;
        }
        
        lastCheckTime = currentTime;
        
        Log.i(TAG, "Starting comprehensive integrity check...");
        
        boolean dexValid = verifyDexFiles(context);
        boolean classValid = verifyCriticalClasses(context);
        boolean resourceValid = verifyResources(context);
        boolean nativeLibValid = verifyNativeLibraries(context);
        
        boolean allValid = dexValid && classValid && resourceValid && nativeLibValid;
        
        if (allValid) {
            Log.i(TAG, "All integrity checks passed");
        } else {
            Log.e(TAG, "Integrity check failed!");
            Log.e(TAG, "DEX valid: " + dexValid);
            Log.e(TAG, "Class valid: " + classValid);
            Log.e(TAG, "Resource valid: " + resourceValid);
            Log.e(TAG, "Native lib valid: " + nativeLibValid);
        }
        
        return allValid;
    }
    
    public static boolean verifyDexFiles(Context context) {
        Log.i(TAG, "Verifying DEX files...");
        
        try {
            String apkPath = context.getPackageCodePath();
            ZipFile zipFile = new ZipFile(apkPath);
            
            for (Map.Entry<String, String> entry : DEX_HASHES.entrySet()) {
                String dexName = entry.getKey();
                String expectedHash = entry.getValue();
                
                ZipEntry zipEntry = zipFile.getEntry(dexName);
                if (zipEntry == null) {
                    Log.e(TAG, "DEX file not found: " + dexName);
                    zipFile.close();
                    return false;
                }
                
                String actualHash = calculateZipEntryHash(zipFile, zipEntry);
                if (!expectedHash.equals(actualHash)) {
                    Log.e(TAG, "DEX file hash mismatch: " + dexName);
                    Log.e(TAG, "Expected: " + expectedHash);
                    Log.e(TAG, "Actual: " + actualHash);
                    zipFile.close();
                    return false;
                }
            }
            
            zipFile.close();
            Log.i(TAG, "DEX files verification passed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify DEX files", e);
            return false;
        }
    }
    
    public static boolean verifyCriticalClasses(Context context) {
        Log.i(TAG, "Verifying critical classes...");
        
        for (Map.Entry<String, String> entry : CLASS_HASHES.entrySet()) {
            String className = entry.getKey();
            String expectedHash = entry.getValue();
            
            String actualHash = calculateClassHash(context, className);
            if (actualHash == null || !expectedHash.equals(actualHash)) {
                Log.e(TAG, "Class hash mismatch: " + className);
                Log.e(TAG, "Expected: " + expectedHash);
                Log.e(TAG, "Actual: " + actualHash);
                return false;
            }
        }
        
        Log.i(TAG, "Critical classes verification passed");
        return true;
    }
    
    public static boolean verifyResources(Context context) {
        Log.i(TAG, "Verifying resources...");
        
        try {
            String apkPath = context.getPackageCodePath();
            ZipFile zipFile = new ZipFile(apkPath);
            
            for (Map.Entry<String, String> entry : RESOURCE_HASHES.entrySet()) {
                String resourceName = entry.getKey();
                String expectedHash = entry.getValue();
                
                ZipEntry zipEntry = zipFile.getEntry(resourceName);
                if (zipEntry == null) {
                    Log.e(TAG, "Resource not found: " + resourceName);
                    zipFile.close();
                    return false;
                }
                
                String actualHash = calculateZipEntryHash(zipFile, zipEntry);
                if (!expectedHash.equals(actualHash)) {
                    Log.e(TAG, "Resource hash mismatch: " + resourceName);
                    Log.e(TAG, "Expected: " + expectedHash);
                    Log.e(TAG, "Actual: " + actualHash);
                    zipFile.close();
                    return false;
                }
            }
            
            zipFile.close();
            Log.i(TAG, "Resources verification passed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify resources", e);
            return false;
        }
    }
    
    public static boolean verifyNativeLibraries(Context context) {
        Log.i(TAG, "Verifying native libraries...");
        
        try {
            String apkPath = context.getPackageCodePath();
            ZipFile zipFile = new ZipFile(apkPath);
            
            for (Map.Entry<String, String> entry : NATIVE_LIB_HASHES.entrySet()) {
                String libPath = entry.getKey();
                String expectedHash = entry.getValue();
                
                ZipEntry zipEntry = zipFile.getEntry(libPath);
                if (zipEntry == null) {
                    Log.e(TAG, "Native library not found: " + libPath);
                    zipFile.close();
                    return false;
                }
                
                String actualHash = calculateZipEntryHash(zipFile, zipEntry);
                if (!expectedHash.equals(actualHash)) {
                    Log.e(TAG, "Native library hash mismatch: " + libPath);
                    Log.e(TAG, "Expected: " + expectedHash);
                    Log.e(TAG, "Actual: " + actualHash);
                    zipFile.close();
                    return false;
                }
            }
            
            zipFile.close();
            Log.i(TAG, "Native libraries verification passed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify native libraries", e);
            return false;
        }
    }
    
    private static String calculateZipEntryHash(ZipFile zipFile, ZipEntry entry) throws Exception {
        InputStream is = zipFile.getInputStream(entry);
        return calculateStreamHash(is);
    }
    
    private static String calculateClassHash(Context context, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            String classFilePath = className.replace('.', '/') + ".class";
            
            String apkPath = context.getPackageCodePath();
            ZipFile zipFile = new ZipFile(apkPath);
            
            ZipEntry entry = zipFile.getEntry(classFilePath);
            if (entry == null) {
                zipFile.close();
                return null;
            }
            
            String hash = calculateZipEntryHash(zipFile, entry);
            zipFile.close();
            
            return hash;
        } catch (Exception e) {
            Log.e(TAG, "Failed to calculate class hash: " + className, e);
            return null;
        }
    }
    
    private static String calculateStreamHash(InputStream is) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = is.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        
        is.close();
        
        byte[] digest = md.digest();
        return bytesToHex(digest);
    }
    
    private static String calculateFileHash(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        return calculateStreamHash(fis);
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private static String[] getSupportedAbis() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                return android.os.Build.SUPPORTED_ABIS;
            } else {
                return new String[]{android.os.Build.CPU_ABI, android.os.Build.CPU_ABI2};
            }
        } catch (Exception e) {
            return new String[]{"armeabi-v7a", "arm64-v8a", "x86", "x86_64"};
        }
    }
    
    public static void forceRecheck() {
        lastCheckTime = 0;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
