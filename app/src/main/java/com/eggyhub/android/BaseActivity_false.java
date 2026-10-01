package com.eggyhub.android;

import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowInsetsController;
import android.util.Log;
import android.content.res.Configuration;
import android.content.res.TypedArray;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

/**
 * 基础Activity类
 * 所有Activity的基类，提供通用功能如刘海屏适配、全屏显示和ActionBar返回键等
 */
public class BaseActivity_false extends AppCompatActivity {

    /**
     * Activity创建时调用的方法
     * 初始化刘海屏区域、全屏显示和ActionBar返回键
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Current theme ID: " + getTheme().toString());
        // 检查主题是否设置了 windowFullscreen
        TypedArray a = getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowFullscreen});
        boolean windowFullscreen = a.getBoolean(0, false);
        a.recycle();
        Log.d(TAG, "onCreate: Theme windowFullscreen attribute: " + windowFullscreen);
        // 设置刘海屏区域
        setupNotchArea();
        Log.d(TAG, "onCreate: setupNotchArea() called.");

        // 核心逻辑：让内容延伸到状态栏底层，但底部避开导航栏
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // 针对底部导航栏进行适配
            View bottomNav = findViewById(R.id.bottom_nav_bar);
            if (bottomNav != null) {
                // 使用 paddingBottom 而不是 margin，这样导航栏背景可以延伸到屏幕最底部
                // 而菜单按钮会被 padding 挤上去，保持在系统导航栏上方
                bottomNav.setPadding(bottomNav.getPaddingLeft(), 
                                   bottomNav.getPaddingTop(), 
                                   bottomNav.getPaddingRight(), 
                                   systemBars.bottom);
            }
            
            return insets;
        });

        // 对于Android R及以上版本，设置全屏显示
        // 启用ActionBar的返回键
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 处理ActionBar返回键的点击事件
     * @return true 表示事件已处理
     */
    @Override
    public boolean onSupportNavigateUp() {
        // 返回上一页
        finish();
        return true;
    }

    private static final String TAG = "BaseActivity_false";

    /**
     * 设置刘海屏区域适配
     * 处理不同Android版本的刘海屏适配逻辑
     */
    protected void setupNotchArea() {
        try {
            Log.d(TAG, "setupNotchArea: 开始执行");
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            Log.d(TAG, "setupNotchArea: 清除全屏和无限制布局标志");

            // Enable edge-to-edge layout
            WindowCompat.setDecorFitsSystemWindows(window, false);
            Log.d(TAG, "setupNotchArea: 设置边缘到边缘布局 (false)");

            // For Android P and above, set layout in display cutout area
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.setAttributes(params);
                Log.d(TAG, "setupNotchArea: Android P+ 设置刘海屏区域布局模式");
            }

            // Handle icon contrast (light/dark) for status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
                if (insetsController != null) {
                    int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                        // Dark theme, set light status bar icons
                        insetsController.setAppearanceLightStatusBars(false);
                        Log.d(TAG, "setupNotchArea: Android R+ 深色主题，设置浅色状态栏图标");
                    } else {
                        // Light theme, set dark status bar icons
                        insetsController.setAppearanceLightStatusBars(true);
                        Log.d(TAG, "setupNotchArea: Android R+ 浅色主题，设置深色状态栏图标");
                    }
                    insetsController.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
                    Log.d(TAG, "setupNotchArea: Android R+ 显示状态栏和导航栏");
                } else {
                    Log.d(TAG, "setupNotchArea: Android R+ insetsController 为空");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android M (API 23) to Android Q (API 29), set status bar icons to dark
                int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                    // Light theme, set dark status bar icons
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    Log.d(TAG, "setupNotchArea: Android M-Q 浅色主题，设置深色状态栏图标");
                } else {
                    // Dark theme, do not set light status bar icons (keep them light by default)
                    window.getDecorView().setSystemUiVisibility(0); // Clear the flag
                    Log.d(TAG, "setupNotchArea: Android M-Q 深色主题，清除状态栏图标标志");
                }
            }
            Log.d(TAG, "setupNotchArea: 执行完毕，无异常");
            Log.d(TAG, "setupNotchArea: Post-setup window flags: " + window.getAttributes().flags);
        } catch (Exception e) {
            Log.e(TAG, "Error in setupNotchArea: " + e.getMessage(), e);
        }
    }

}
