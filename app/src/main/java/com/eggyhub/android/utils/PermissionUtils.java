package com.eggyhub.android.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 权限申请工具类
 * 用于 Android 6.0+ 的动态权限申请
 */
public class PermissionUtils {
    
    // 存储权限请求码
    public static final int REQUEST_CODE_STORAGE = 100;
    public static final int REQUEST_CODE_CAMERA = 101;
    public static final int REQUEST_CODE_ALL_STORAGE = 102;
    
    /**
     * 检查存储权限是否已授予
     */
    public static boolean hasStoragePermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 6.0 以下不需要动态申请
    }
    
    /**
     * 检查相机权限是否已授予
     */
    public static boolean hasCameraPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    /**
     * 请求存储权限
     */
    public static void requestStoragePermission(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_CODE_STORAGE
            );
        }
    }
    
    /**
     * 请求相机权限
     */
    public static void requestCameraPermission(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA
            );
        }
    }
    
    /**
     * 检查是否有所有文件访问权限（Android 11+）
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static boolean hasAllFilesAccessPermission(@NonNull Context context) {
        return Environment.isExternalStorageManager();
    }
    
    /**
     * 跳转到应用设置页面，引导用户手动开启权限
     */
    public static void openAppSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
    }
    
    /**
     * 显示权限被拒绝的提示对话框
     */
    public static void showPermissionDeniedDialog(@NonNull Activity activity, 
                                                   @NonNull String message,
                                                   Runnable onConfirm) {
        new AlertDialog.Builder(activity)
            .setTitle("权限提示")
            .setMessage(message)
            .setPositiveButton("去设置", (dialog, which) -> {
                if (onConfirm != null) {
                    onConfirm.run();
                }
            })
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show();
    }
    
    /**
     * 显示权限说明对话框
     */
    public static void showPermissionRationaleDialog(@NonNull Activity activity,
                                                      @NonNull String message,
                                                      @NonNull Runnable onConfirm) {
        new AlertDialog.Builder(activity)
            .setTitle("需要权限")
            .setMessage(message)
            .setPositiveButton("允许", (dialog, which) -> {
                if (onConfirm != null) {
                    onConfirm.run();
                }
            })
            .setNegativeButton("拒绝", null)
            .setCancelable(false)
            .show();
    }
    
    /**
     * 处理权限请求结果
     * @return true 表示权限已授予，false 表示被拒绝
     */
    public static boolean handlePermissionResult(@NonNull Activity activity,
                                                  int requestCode,
                                                  @NonNull int[] grantResults,
                                                  @NonNull String permissionName) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "已获得" + permissionName + "权限", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(activity, "未获得" + permissionName + "权限，部分功能可能无法使用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
