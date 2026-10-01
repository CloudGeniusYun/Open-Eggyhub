package com.eggyhub.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Scheme跳转处理Activity
 * 用于处理 eggyhub://android/open?path=xxx 格式的Scheme调用
 */
public class SchemeHandlerActivity extends BaseActivity {
    private static final String TAG = "SchemeHandlerActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 等待安全检查完成
        new Thread(() -> {
            // 最多等待5秒
            long startTime = System.currentTimeMillis();
            while (!EggyApp.isSecurityCheckCompleted() && System.currentTimeMillis() - startTime < 5000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            // 安全检查完成后，检查是否受限
            runOnUiThread(() -> {
                if (EggyApp.isRestricted) {
                    // 如果受限，跳转到授权页面
                    startActivity(new Intent(SchemeHandlerActivity.this, AuthActivity.class));
                    finish();
                } else {
                    // 安全检查通过，处理Scheme跳转
                    handleSchemeIntent(getIntent());
                }
            });
        }).start();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        // 检查是否受限
        if (EggyApp.isRestricted) {
            // 如果受限，跳转到授权页面
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        } else {
            // 安全检查通过，处理Scheme跳转
            handleSchemeIntent(intent);
        }
    }
    
    /**
     * 处理Scheme Intent
     */
    private void handleSchemeIntent(Intent intent) {
        if (intent == null || !intent.getAction().equals(Intent.ACTION_VIEW)) {
            finish();
            return;
        }
        
        // 获取path参数
        String path = intent.getData() != null ? intent.getData().getQueryParameter("path") : null;
        Log.d(TAG, "Scheme path: " + path);
        
        // 根据path参数跳转到对应页面
        if (path != null) {
            switch (path) {
                case "home":
                    // 跳转到主页
                    startActivity(new Intent(this, MainActivity.class));
                    break;
                case "login":
                    // 跳转到登录页
                    startActivity(new Intent(this, LoginActivity.class));
                    break;
                case "settings":
                    // 跳转到设置页
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
                case "profile":
                    // 跳转到个人中心
                    startActivity(new Intent(this, ProfileActivity.class));
                    break;
                case "publish":
                    // 跳转到发布页
                    startActivity(new Intent(this, PublishActivity.class));
                    break;
                default:
                    // 默认跳转到主页
                    startActivity(new Intent(this, MainActivity.class));
                    break;
            }
        } else {
            // 没有path参数，默认跳转到主页
            startActivity(new Intent(this, MainActivity.class));
        }
        
        // 处理完成后 finish
        finish();
    }
}