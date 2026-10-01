package com.eggyhub.android;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.eggyhub.android.utils.SignatureUtils;
import com.eggyhub.android.utils.IntegrityChecker;

public class SecurityInterceptor implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SecurityInterceptor";
    private int activityCount = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated: " + activity.getClass().getSimpleName());
        activityCount++;
        
        // 如果当前Activity是AuthActivity或SchemeHandlerActivity，不进行任何检查
        if (activity instanceof AuthActivity || activity instanceof SchemeHandlerActivity) {
            return;
        }
        
        Intent intent = activity.getIntent();
        if (intent != null && intent.getScheme() != null) {
            Log.i(TAG, "Activity started via Scheme: " + intent.getScheme());
            
            if (!EggyApp.verifySignature(activity)) {
                Log.e(TAG, "Signature verification failed for Scheme launch!");
                
                Intent authIntent = new Intent(activity, AuthActivity.class);
                authIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(authIntent);
                activity.finish();
                return;
            }
        }
        
        if (EggyApp.isRestricted) {
            Log.w(TAG, "App is restricted, redirecting to AuthActivity");
            
            Intent authIntent = new Intent(activity, AuthActivity.class);
            authIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(authIntent);
            activity.finish();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d(TAG, "onActivityStarted: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(TAG, "onActivityResumed: " + activity.getClass().getSimpleName());
        
        // 如果应用受限，或当前Activity是AuthActivity或SchemeHandlerActivity，不进行完整性检查
        if (EggyApp.isRestricted || activity instanceof AuthActivity || activity instanceof SchemeHandlerActivity) {
            return;
        }
        
        if (!IntegrityChecker.verifyAll(activity)) {
            Log.e(TAG, "Code integrity check failed!");
            
            Intent authIntent = new Intent(activity, AuthActivity.class);
            authIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(authIntent);
            activity.finish();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "onActivityPaused: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d(TAG, "onActivityStopped: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "onActivitySaveInstanceState: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "onActivityDestroyed: " + activity.getClass().getSimpleName());
        activityCount--;
        
        // 当所有Activity都被销毁时，停止反调试监控
        if (activityCount <= 0) {
            Log.i(TAG, "All activities destroyed, stopping anti-debug monitoring");
            SignatureUtils.stopAntiDebug();
        }
    }
}