package com.eggyhub.android.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppLogger {
    private static final String TAG = "AppLogger";
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    private static Context sAppContext;

    public static void init(Context context) {
        sAppContext = context.getApplicationContext();
    }

    public static void v(String tag, String message) {
        Log.v(tag, message);
        saveLog("VERBOSE", tag, message);
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
        saveLog("DEBUG", tag, message);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
        saveLog("INFO", tag, message);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
        saveLog("WARN", tag, message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        saveLog("ERROR", tag, message);
    }

    public static void e(String tag, String message, Throwable tr) {
        Log.e(tag, message, tr);
        saveLog("ERROR", tag, message + "\n" + Log.getStackTraceString(tr));
    }

    @Deprecated
    public static void i(Context context, String tag, String message) { i(tag, message); }
    @Deprecated
    public static void e(Context context, String tag, String message) { e(tag, message); }
    @Deprecated
    public static void d(Context context, String tag, String message) { d(tag, message); }

    private static void saveLog(String level, String tag, String message) {
        if (sAppContext == null) return;
        
        // 检查是否开启了本地日志存储
        SharedPreferences prefs = sAppContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isLocalLogEnabled = prefs.getBoolean("local_log_enabled", true);
        if (!isLocalLogEnabled) {
            return;
        }
        
        logExecutor.execute(() -> {
            try {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put("timestamp", System.currentTimeMillis());
                values.put("level", level);
                values.put("tag", tag);
                values.put("message", message);
                
                sAppContext.getContentResolver().insert(LogProvider.CONTENT_URI, values);
            } catch (Exception e) {
                // Fallback to direct DAO insert if provider fails
                try {
                    LogDatabase db = LogDatabase.getDatabase(sAppContext);
                    db.logDao().insert(new LogEntity(System.currentTimeMillis(), level, tag, message));
                } catch (Exception ignored) {}
            }
        });
    }
}
