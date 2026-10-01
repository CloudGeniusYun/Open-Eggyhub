package com.eggyhub.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eggyhub.android.utils.SignatureUtils;

public class AuthActivity extends AppCompatActivity {

    private TextView deviceIdTextView;
    private EditText authCodeEditText;
    private Button verifyButton;
    private Button copyButton;

    static {
        try {
            System.loadLibrary("eggyhub_native");
        } catch (UnsatisfiedLinkError e) {
            android.util.Log.e("AuthActivity", "Native library not found!");
        }
    }

    public static native boolean notifyAuthActivityExists();
    public static native boolean verifyAuthActivityCreated();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // 通知native层AuthActivity已创建
        notifyAuthActivityExists();

        // 检查是否有有效授权码
        SharedPreferences prefs = getSharedPreferences("eggy_security", MODE_PRIVATE);
        String savedAuthCode = prefs.getString("auth_code", "");
        
        if (!TextUtils.isEmpty(savedAuthCode)) {
            String deviceId = SignatureUtils.getDeviceID(this);
            if (SignatureUtils.verifyLicenseNative(this, deviceId, savedAuthCode)) {
                // 授权码有效，直接跳转到SplashActivity
                android.util.Log.i("AuthActivity", "Valid auth code found, redirecting to SplashActivity");
                android.content.Intent intent = new android.content.Intent(AuthActivity.this, SplashActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return;
            } else {
                // 授权码无效，清除并显示授权界面
                android.util.Log.i("AuthActivity", "Saved auth code is invalid, clearing it");
                prefs.edit().remove("auth_code").apply();
            }
        }

        // 没有有效授权码，显示授权界面
        deviceIdTextView = findViewById(R.id.deviceIdTextView);
        authCodeEditText = findViewById(R.id.authCodeEditText);
        verifyButton = findViewById(R.id.verifyButton);
        copyButton = findViewById(R.id.copyButton);

        // 获取并显示设备ID
        String deviceId = SignatureUtils.getDeviceID(this);
        deviceIdTextView.setText(deviceId);

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ID", deviceId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "设备ID已复制", Toast.LENGTH_SHORT).show();
        });

        verifyButton.setOnClickListener(v -> {
            String authCode = authCodeEditText.getText().toString().trim();
            if (TextUtils.isEmpty(authCode)) {
                Toast.makeText(this, "请输入授权码", Toast.LENGTH_SHORT).show();
                return;
            }

            // 二次验证：检查AuthActivity是否真的被创建
            if (!verifyAuthActivityCreated()) {
                android.util.Log.e("AuthActivity", "AuthActivity creation verification failed!");
                Toast.makeText(this, "验证失败，请重新启动应用", Toast.LENGTH_LONG).show();
                System.exit(0);
                return;
            }

            // 在后台线程中验证授权码
            new Thread(() -> {
                try {
                    final boolean verificationResult = SignatureUtils.verifyLicenseNative(AuthActivity.this, deviceId, authCode);
                    
                    runOnUiThread(() -> {
                        if (verificationResult) {
                            // 验证成功
                            Toast.makeText(AuthActivity.this, "授权成功！正在重启应用...", Toast.LENGTH_SHORT).show();
                            
                            // 保存授权码
                            SharedPreferences prefs2 = getSharedPreferences("eggy_security", MODE_PRIVATE);
                            prefs2.edit().putString("auth_code", authCode).apply();

                            // 移除受限标记
                            EggyApp.isRestricted = false;

                            // 延迟重启应用，让Toast有时间显示
                            new android.os.Handler().postDelayed(() -> {
                                // 重启应用
                                android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                                System.exit(0);
                            }, 1500);
                        } else {
                            Toast.makeText(AuthActivity.this, "授权码错误或不匹配", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AuthActivity.this, "验证异常，请重新启动应用", Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }

    @Override
    public void onBackPressed() {
        // 验证页面不允许后退，直接退出应用
        // 如果是测试模式进入，也应该严格限制，模拟真实场景
        // super.onBackPressed();
        System.exit(0);
    }
}
