package com.eggyhub.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.content.Intent;
import com.eggyhub.android.EggyApp;

public class SignatureUtils {
    private static final String TAG = "SignatureUtils";
    
    static {
        try {
            System.loadLibrary("eggyhub_native");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Native library not found!");
        }
    }

    public static native boolean checkSignatureNative(Context context);
    public static native boolean verifyLicenseNative(Context context, String deviceId, String authCode);
    public static native boolean verifyPluginNative(Context context, String apkPath);
    public static native boolean checkAuthActivityExists(Context context);
    public static native void forceShowAuthActivity(Context context);
    public static native void stopAntiDebug();
    
    public static String getDeviceID(Context context) {
        return android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }
    
    public static void verifyAndShowAuth(Context context) {
        Log.i(TAG, "Starting signature verification...");
        
        Log.i(TAG, "Step 1: Starting anti-debug...");
        startAntiDebug(context);
        Log.i(TAG, "Step 1 completed");
        
        Log.i(TAG, "Step 2: Checking AuthActivity existence...");
        if (!checkAuthActivityExists(context)) {
            Log.e(TAG, "AuthActivity not found! Possible tampering detected.");
            System.exit(0);
            return;
        }
        Log.i(TAG, "Step 2 completed: AuthActivity exists.");
        
        Log.i(TAG, "All verification steps completed successfully");
    }
    
    public static void startAntiDebug(Context context) {
        Log.i(TAG, "Starting anti-debug in background thread...");
        new Thread(() -> {
            startAntiDebugNative(context);
        }).start();
    }
    
    private static native void startAntiDebugNative(Context context);
    
    public static void saveAuthCode(Context context, String authCode) {
        SharedPreferences prefs = context.getSharedPreferences("eggy_security", Context.MODE_PRIVATE);
        prefs.edit().putString("auth_code", authCode).apply();
        Log.i(TAG, "Auth code saved.");
    }
    
    public static void clearAuthCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("eggy_security", Context.MODE_PRIVATE);
        prefs.edit().remove("auth_code").apply();
        Log.i(TAG, "Auth code cleared.");
    }
}
