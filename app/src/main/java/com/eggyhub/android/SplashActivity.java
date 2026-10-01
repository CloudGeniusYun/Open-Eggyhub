package com.eggyhub.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import android.content.SharedPreferences;
import com.eggyhub.android.log.AppLogger;

import android.widget.EditText;
import android.widget.Toast;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.value.LottieValueCallback;

import com.auth0.android.jwt.DecodeException;
import com.auth0.android.jwt.JWT;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.UUID;
import java.util.Date;

import android.util.Base64;
import javax.crypto.Cipher;

import com.eggyhub.android.EncryptedLoginRequest;
import com.eggyhub.android.LoginCredentials;
import com.eggyhub.android.LoginResponse;
import com.eggyhub.android.UserProfileResponse;
import com.google.gson.Gson;

/**
 * 启动页面Activity
 * 负责应用启动时的欢迎界面展示、登录状态检查以及页面导航
 */
public class SplashActivity extends BaseActivity {
    String model = Build.MODEL;
    String brand = Build.BRAND;
    /**
     * SharedPreferences实例，用于存储和读取用户偏好设置
     */
    private SharedPreferences preferences;

    /**
     * 日志标签，用于在Logcat中标识该类的日志
     */
    private static final String TAG = "SplashActivity";

    /**
     * 启动页面显示时长（毫秒）
     */
    private static final int SPLASH_DISPLAY_LENGTH = 1000; // 1秒

    private LottieAnimationView splashAnimation;

    /**
     * API基础URL
     */
    private static final String BASE_URL = "https://eggyhub.top/api";
    /**
     * 登录API端点
     */
    private static final String LOGIN_URL = BASE_URL + "/auth/login";
    /**
     * 用户资料API端点
     */
    private static final String PROFILE_URL = BASE_URL + "/users/profile";

    /**
     * Activity创建时调用的方法
     * 初始化界面并设置延迟处理登录逻辑
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否显示过引导页
        SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (!appPrefs.getBoolean("intro_shown", false)) {
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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
                    Intent intent = new Intent(SplashActivity.this, AuthActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // 显示启动动画
                    showSplashAnimation();
                }
            });
        }).start();
    }
    
    /**
     * 显示启动动画
     */
    private void showSplashAnimation() {
        getWindow().setBackgroundDrawableResource(R.color.white);
        setContentView(R.layout.activity_splash);

        splashAnimation = findViewById(R.id.splashAnimation);
        
        // 检测深色模式，动态修改动画颜色
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            // 深色模式下，设置动画颜色为白色
            splashAnimation.addValueCallback(
                new KeyPath("**"),
                LottieProperty.COLOR,
                new LottieValueCallback<>(android.graphics.Color.WHITE)
            );
        }
        
        // Lottie 动画自动播放（XML 中已配置）
        // 延迟执行登录检查逻辑
        new Handler().postDelayed(() -> {
            checkLoginStatusAndNavigate();
        }, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (splashAnimation != null) {
            splashAnimation.pauseAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (splashAnimation != null) {
            splashAnimation.resumeAnimation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 检查登录状态并进行相应导航
     * 从SharedPreferences中读取保存的登录凭证，如果存在则尝试自动登录
     * 如果不存在则直接导航到登录页面
     */
    private void checkLoginStatusAndNavigate() {
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String savedEmail = SecureStorageManager.decryptAndRetrieve("email");
        String savedPassword = SecureStorageManager.decryptAndRetrieve("password");
        String accessToken = SecureStorageManager.decryptAndRetrieve("access_token");

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                JWT jwt = new JWT(accessToken);
                // 检查token是否过期，给予10秒的宽限期
                if (!jwt.isExpired(10)) {
                    // Token有效且未过期，导航到主Activity
                    runOnUiThread(() -> {
                        Toast.makeText(SplashActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    });
                } else {
                    // Token已过期，尝试使用保存的凭证重新自动登录
                    AppLogger.e(TAG, "JWT token expired.");
                    runOnUiThread(() -> Toast.makeText(SplashActivity.this, "Token已过期，正在尝试重新登录", Toast.LENGTH_SHORT).show());
                    if (savedEmail != null && savedPassword != null) {
                        attemptAutoLogin(savedEmail, savedPassword);
                    } else {
                        navigateToLogin();
                    }
                }
            } catch (DecodeException e) {
                // JWT token无效，尝试使用保存的凭证重新自动登录
                AppLogger.e(TAG, "Invalid JWT token: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(SplashActivity.this, "无效的Token，尝试重新登录", Toast.LENGTH_SHORT).show());
                if (savedEmail != null && savedPassword != null) {
                    attemptAutoLogin(savedEmail, savedPassword);
                } else {
                    navigateToLogin();
                }
            }
        } else if (savedEmail != null && savedPassword != null) {
            // 没有access token，但存在保存的email和password，尝试自动登录
            attemptAutoLogin(savedEmail, savedPassword);
        } else {
            // 没有保存的凭证，导航到登录页面
            navigateToLogin();
        }
    }

    /**
     * 尝试使用保存的凭证进行自动登录
     * @param email 保存的邮箱
     * @param password 保存的密码
     */
    private void attemptAutoLogin(String email, String password) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // 对登录数据进行加密
        String encryptedLoginData = encryptLoginData(email, password);
        if (encryptedLoginData == null) {
            runOnUiThread(() -> Toast.makeText(SplashActivity.this, "登录数据处理失败", Toast.LENGTH_SHORT).show());
            navigateToLogin(); // JSON构建失败，导航到登录页面
            return;
        }

        // 构建请求体JSON对象
        EncryptedLoginRequest requestModel = new EncryptedLoginRequest(encryptedLoginData);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestModel);

        // 创建请求体和请求对象
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(LOGIN_URL) // 登录API接口
                .post(body)
                .build();

        // 异步执行登录请求
        client.newCall(request).enqueue(new Callback() {
            /**
             * 请求失败时调用
             * @param call 请求对象
             * @param e 异常信息
             */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(SplashActivity.this, "登录凭证失效，请重新登录", Toast.LENGTH_SHORT).show();
                    navigateToLogin(); // 自动登录失败，显示登录页面
                });
            }

            /**
             * 请求成功时调用
             * @param call 请求对象
             * @param response 响应对象
             * @throws IOException IO异常
             */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) { // 响应成功（2xx状态码）
                        String bodyString = responseBody != null ? responseBody.string() : "";
                        if (bodyString.isEmpty()) { // 响应体为空
                            runOnUiThread(() -> Toast.makeText(SplashActivity.this, "服务器返回空响应", Toast.LENGTH_SHORT).show());
                            navigateToLogin();
                            return;
                        }
                        // 解析响应JSON
                        LoginResponse loginResponse = gson.fromJson(bodyString, LoginResponse.class);
                        String status = loginResponse.getStatus();
                        if ("success".equals(status)) { // 登录成功
                            // 登录成功，保存数据并跳转
                            String accessToken = loginResponse.getAccessToken(); // 获取访问令牌
                            LoginResponse.User userObject = loginResponse.getUser(); // 获取用户对象
                            String username = "";
                            int id = -1;
                            if (userObject != null) {
                                username = userObject.getUsername(); // 获取用户名
                                id = userObject.getId(); // 获取用户ID
                            }

                            // 保存用户数据到SharedPreferences
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.putString("access_token", accessToken);
                            editor.putString("username", username);
                            editor.putInt("id", id);
                            editor.apply();

                            // 获取用户资料
                            if (id != -1) {
                                fetchUserProfile(id);
                            }

                            runOnUiThread(() -> {
                                Toast.makeText(SplashActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                navigateToMain(); // 跳转到主页
                            });
                        } else { // 登录失败
                            runOnUiThread(() -> {
                                String toastMessage = "自动登录失败: " + bodyString;
                                Toast.makeText(SplashActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                                navigateToLogin(); // 自动登录失败，显示登录页面
                            });
                        }
                        
                    } else { // 响应不成功
                        String errorBody = responseBody != null ? responseBody.string() : "";
                        runOnUiThread(() -> Toast.makeText(SplashActivity.this, "自动登录失败: " + response.code() + " - " + errorBody, Toast.LENGTH_SHORT).show());
                        
                        navigateToLogin(); // 自动登录失败，显示登录页面
                    }
                } catch (Exception e) { // JSON解析异常
                    runOnUiThread(() -> Toast.makeText(SplashActivity.this, "响应解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    
                    navigateToLogin(); // 响应解析失败，显示登录页面
                }
            }
        });
    }

    /**
     * 获取用户资料
     * @param userId 用户ID
     */
    private void fetchUserProfile(int userId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = PROFILE_URL + "?id=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(SplashActivity.this, "获取用户资料失败: 网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String bodyString = responseBody != null ? responseBody.string() : "";
                        if (bodyString.isEmpty()) {
                            runOnUiThread(() -> {
                                Toast.makeText(SplashActivity.this, "获取用户资料失败: 服务器返回空响应", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        Gson gson = new Gson();
                        UserProfileResponse userProfileResponse = gson.fromJson(bodyString, UserProfileResponse.class);
                        boolean success = userProfileResponse.isSuccess();
                        if (success) {
                            UserProfileResponse.Data data = userProfileResponse.getData();
                            if (data != null) {
                                String avatar = data.getAvatar();
                                String contact = data.getContact();
                                String description = data.getDescription();
                                String eggyid = data.getEggyid();
                                AppLogger.e(TAG, avatar+contact+description+eggyid);

                                // 保存到SharedPreferences
                                SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("avatar", avatar);
                                editor.putString("contact", contact);
                                editor.putString("description", description);
                                editor.putString("eggyid", eggyid);
                                editor.apply();
                            }
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(SplashActivity.this, "获取用户资料失败: " + userProfileResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(SplashActivity.this, "获取用户资料失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(SplashActivity.this, "获取用户资料失败: 解析错误", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * 导航到主页面
     * 直接跳转到MainActivity
     */
    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // 结束当前Activity
    }


    /**
     * 导航到登录页面
     * 创建跳转到LoginActivity的Intent并启动
     */
    private void navigateToLogin() {
        // 检查是否受限，如果受限则跳转到授权页面
        if (EggyApp.isRestricted) {
            Intent authIntent = new Intent(SplashActivity.this, AuthActivity.class);
            startActivity(authIntent);
            finish();
            return;
        }
        
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 结束当前Activity
    }

    private String encryptLoginData(String email, String password) {
        try {
            // 生成浮点型时间戳
            double timestamp = System.currentTimeMillis() / 1000.0;

            // 构建JSON字符串
            LoginCredentials credentials = new LoginCredentials(email, password, timestamp);
            Gson gson = new Gson();
            String data = gson.toJson(credentials);

            // 定义公钥
            String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoGJBrgcKyxkFVSLF8kTX\n" +
                    "9bW7tkoJ1IKwDxC9UpZe7uKwB+t3tU+fegu/d6zhOeEUmLLfSGmvp3ZI1RrB9Y02\n" +
                    "k8AGotz9NLmr9zQciBEXmV/YkmoyK72cZnMJbq2hYODc02tEV8ITBwAwbhvD81g5\n" +
                    "H/WTN16MXjA1Mpdt33qGQ87SEPTsQmWZjWfzBWq5vbC2mUxvzN6hgBs/NpLuBOLt\n" +
                    "Wy7e2hvLkTTQnqnhJIzg/H2xDYHUQXZSCqi8uVYnma1SRy+lV+wZ+h26zavesJTX\n" +
                    "qqD5CUE2jINiT/84DywH8W4gJSD99fa58QDgpr1MFUzRsRs9skJBq80Ds8joOxCO\n" +
                    "jQIDAQAB\n" +
                    "-----END PUBLIC KEY-----";


            String publicKeyPEM = PUBLIC_KEY
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Base64解码公钥
            byte[] keyBytes = Base64.decode(publicKeyPEM, Base64.DEFAULT);

            // 创建X509EncodedKeySpec对象
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            // 获取KeyFactory实例并生成公钥
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // 创建加密对象并初始化
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // 准备要加密的数据（JSON字符串）
            byte[] dataBytes = data.getBytes("UTF-8");

            // 加密数据
            byte[] encryptedBytes = cipher.doFinal(dataBytes);

            // 将加密后的字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : encryptedBytes) {
                sb.append(String.format("%02x", b));
            }
            // AppLogger.e(TAG, String.valueOf(sb.toString().length())); // 暂时注释掉，SplashActivity中没有TAG
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}