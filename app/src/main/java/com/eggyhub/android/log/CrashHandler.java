package com.eggyhub.android.log;

import android.content.Context;
import android.os.Process;
import androidx.annotation.NonNull;

/**
 * 全局异常捕获器，用于记录闪退日志到本地数据库
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static CrashHandler sInstance;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private Context mContext;

    private CrashHandler() {}

    public static CrashHandler getInstance() {
        if (sInstance == null) {
            sInstance = new CrashHandler();
        }
        return sInstance;
    }

    /**
     * 初始化异常处理器
     * @param context 上下文
     */
    public void init(Context context) {
        mContext = context.getApplicationContext();
        // 获取系统默认的异常处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置当前类为全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        // 1. 记录闪退日志到本地数据库，使用 AndroidRuntime 标签模拟系统闪退日志
        AppLogger.e("AndroidRuntime", "FATAL EXCEPTION: " + t.getName(), e);

        // 2. 延迟一下确保日志写入数据库（AppLogger 是异步执行的）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // 3. 让系统处理剩下的事情（比如弹出“应用已停止”对话框）
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }
}
