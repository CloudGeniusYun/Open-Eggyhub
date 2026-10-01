package com.eggyhub.android;

import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.WindowManager;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.UUID;
import java.util.Date;

import android.util.Base64;
import javax.crypto.Cipher;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import android.content.SharedPreferences;

import com.eggyhub.android.utils.ProxyConfig;
import com.eggyhub.android.utils.OkHttpClientFactory;

import java.net.HttpURLConnection;
import java.net.URL;

// ★ 导入 PAG
import org.libpag.PAGImageView;

/**
 * 登录页面Activity
 * 负责用户登录功能的实现，包括输入验证、网络请求和登录状态处理
 */
public class LoginActivity extends BaseActivity {
    private AlertDialog loadingDialog;
    
    int id;
    String model = Build.MODEL;
    String brand = Build.BRAND;
    String role;
    String Email;
    private UpdateManager mUpdateManager;


    /**
     * 日志标签，用于在Logcat中标识该类的日志
     */
    private static final String TAG = "LoginActivity";

    /**
     * 用户名输入框
     */
    private EditText usernameEditText;
    /**
     * 密码输入框
     */
    private EditText passwordEditText;
    /**
     * 登录按钮
     */
    private Button loginButton;
    /**
     * 注册按钮
     */
    private Button registerButton;

    private ImageView jloginButton;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoginPage(); // 显示登录页面

        // 检查网络可用性
        checkNetworkAvailability();

        // 检查更新
        mUpdateManager = new UpdateManager(this);
        mUpdateManager.checkForUpdate();

        // 处理返回键逻辑
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private long lastBackPressTime = 0;

            @Override
            public void handleOnBackPressed() {
                if (System.currentTimeMillis() - lastBackPressTime < 2000) {
                    // 如果在2秒内再次按下返回键，则退出Activity
                    finish();
                } else {
                    // 第一次按下返回键，提示用户再按一次退出
                    Toast.makeText(LoginActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                    lastBackPressTime = System.currentTimeMillis();
                }
            }
        });
    }

    /**
     * 显示登录页面
     * 设置布局文件并初始化视图组件
     */
    private void showLoginPage() {
        setContentView(R.layout.activity_login); // 设置登录页面布局
        initViews(); // 初始化视图组件
    }

    /**
     * 初始化视图组件
     * 获取各UI元素的引用并设置按钮点击事件监听器
     */
    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText); // 获取用户名输入框
        passwordEditText = findViewById(R.id.passwordEditText); // 获取密码输入框
        loginButton = findViewById(R.id.loginButton); // 获取登录按钮
        Button registerButton = findViewById(R.id.registerButton); // 获取注册按钮
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton); // 获取找回密码按钮
        jloginButton = findViewById(R.id.jloginButton);

        // 初始化网络切换按钮
        ImageButton btnNetworkSwitch = findViewById(R.id.btnNetworkSwitch);
        if (btnNetworkSwitch != null) {
            updateLoginNetworkButtonIcon(btnNetworkSwitch); // 更新按钮图标
            btnNetworkSwitch.setOnClickListener(v -> {
                showLoginNetworkSwitchDialog(btnNetworkSwitch); // 显示网络切换对话框
            });
        }

        // ★ 测试加载弹窗按钮（仅 Debug 模式）
        Button testLoadingButton = findViewById(R.id.testLoadingButton);
        if (testLoadingButton != null) {
            if (BuildConfig.DEBUG) {
                testLoadingButton.setVisibility(View.VISIBLE);
                testLoadingButton.setOnClickListener(v -> {
                    showLoadingDialog();
                    // 3秒后自动关闭，便于反复测试
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        dismissLoadingDialog();
                    }, 3000);
                });
            } else {
                testLoadingButton.setVisibility(View.GONE);
            }
        }

        TextView skipButton = findViewById(R.id.skipButton);
        if (skipButton != null) {
            skipButton.setOnClickListener(v -> {
                // 清空所有用户数据（包括加密存储）
                SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                preferences.edit().clear().apply();
                SecureStorageManager.clearAll();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // 设置密码输入框的回车键监听器
        passwordEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        // 设置登录按钮点击事件监听器
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = usernameEditText.getText().toString().trim(); // 获取输入的邮箱
                String password = passwordEditText.getText().toString().trim(); // 获取输入的密码
                attemptLogin(email, password); // 尝试登录
            }
        });

        // 设置注册按钮点击事件监听器
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class); // 创建跳转到注册页面的Intent
                startActivity(intent); // 启动注册页面
            }
        });

        // 设置找回密码按钮点击事件监听器
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class); // 创建跳转到找回密码页面的Intent
                startActivity(intent); // 启动找回密码页面
            }
        });

        jloginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, JLoginActivity.class);
                startActivity(intent);
            }
        });



        // Debug 模式下长按“用户登录”标题跳转主页
        TextView loginTitleTextView = findViewById(R.id.loginTitleTextView);
        if (loginTitleTextView != null) {
            loginTitleTextView.setOnLongClickListener(v -> {
                if (BuildConfig.DEBUG) {
                    AppLogger.d(TAG, "Debug mode: Long click detected on title, skipping login.");
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * 显示加载对话框
     */
    private void showLoadingDialog() {
        if (loadingDialog == null) {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
            loadingDialog = builder.create();
            loadingDialog.setView(dialogView, 0, 0, 0, 0); // 移除默认边距
            loadingDialog.setCanceledOnTouchOutside(false); // 点击外部不消失
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            // ★ 新增：获取 PAGImageView 并播放动画
            PAGImageView pagView = dialogView.findViewById(R.id.pagLoading);
            if (pagView != null) {
                pagView.setPath("assets://loading.pag");
                pagView.setRepeatCount(-1); // 无限循环
                pagView.play();
            }
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    /**
     * 关闭加载对话框
     */
    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            // ★ 新增：停止 PAG 动画（使用 pause() 代替 stop()）
            View dialogView = loadingDialog.getWindow().getDecorView();
            PAGImageView pagView = dialogView.findViewById(R.id.pagLoading);
            if (pagView != null) {
                pagView.pause(); // PAGImageView 使用 pause() 暂停动画
            }
            loadingDialog.dismiss();
        }
    }

    /**
     * 尝试登录
     * 验证用户输入并发送登录请求到服务器
     *
     * @param email    用户输入的邮箱
     * @param password 用户输入的密码
     */
    private void attemptLogin(String email, String password) {
        // 显示加载对话框
        showLoadingDialog();
        
        OkHttpClient client = OkHttpClientFactory.getSharedClient(); // 创建OkHttpClient实例
        MediaType JSON = MediaType.get("application/json; charset=utf-8"); // 设置请求媒体类型

        // 对登录数据进行加密
        String encryptedLoginData = encryptLoginData(email, password);
        if (encryptedLoginData == null) {
            runOnUiThread(() -> {
                dismissLoadingDialog();
                Toast.makeText(LoginActivity.this, "登录数据处理失败", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // 构建请求体JSON对象
        EncryptedLoginRequest loginRequest = new EncryptedLoginRequest(encryptedLoginData);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(loginRequest);

        // 创建请求体和请求对象
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(LOGIN_URL) // 登录API接口
                .post(body) // 设置请求方法为POST
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
                    dismissLoadingDialog();
                    showErrorDialog(e.getMessage());
                    showLoginPage(); // 登录失败，显示登录页面
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
                try (okhttp3.ResponseBody responseBodyObj = response.body()) {
                    final String responseBody = responseBodyObj != null ? responseBodyObj.string() : "";
                    
                    if (response.isSuccessful()) { // 响应成功（2xx状态码）
                        if (responseBody.isEmpty()) { // 响应体为空
                            runOnUiThread(() -> {
                                dismissLoadingDialog();
                                Toast.makeText(LoginActivity.this, "服务器返回空响应", Toast.LENGTH_SHORT).show();
                                System.out.println("Login Response: Empty");
                                return;
                            });
                            return;
                        }
                        // 解析响应JSON
                        try {
                            Gson gson = new Gson();
                            LoginResponse loginResponse = gson.fromJson(responseBody, LoginResponse.class);
                            String status = loginResponse.getStatus();
                            
                            if ("success".equals(status)) { // 登录成功
                                // 登录成功，保存数据并跳转
                                String accessToken = loginResponse.getAccessToken(); // 获取访问令牌
                                LoginResponse.User userObject = loginResponse.getUser(); // 获取用户对象
                                String username = "";
                                String sponser = "0"; // 默认值
                                if (userObject != null) {
                                    username = userObject.getUsername(); // 获取用户名
                                    id = userObject.getId(); // 获取用户ID
                                    Email = userObject.getEmail();
                                    role = userObject.getRole();
                                    sponser = userObject.getSponser(); // 获取sponser字段
                                    if (sponser == null) sponser = "0";
                                }
                                // 保存用户数据到SharedPreferences
                                SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                
                                // 敏感数据加密存储
                                SecureStorageManager.encryptAndStore("email", Email);
                                SecureStorageManager.encryptAndStore("password", password);
                                SecureStorageManager.encryptAndStore("access_token", accessToken);
                                
                                // 非敏感数据明文存储
                                editor.putString("username", username);
                                editor.putInt("id", id);
                                editor.putString("role", role);
                                editor.putString("sponser", sponser);
                                editor.apply();

                                // 获取用户资料
                                if (id != -1) {
                                    fetchUserProfile(id);
                                }

                                navigateToMain();
                            } else { // 登录失败
                                runOnUiThread(() -> {
                                    dismissLoadingDialog();
                                    showLoginPage(); // 登录失败，显示登录页面
                                    String errorMessage;
                                    String encodedMessage = loginResponse.getMessage();
                                    if (encodedMessage != null) {
                                        try {
                                            errorMessage = java.net.URLDecoder.decode(encodedMessage, "UTF-8");
                                        } catch (java.io.UnsupportedEncodingException e1) {
                                            errorMessage = encodedMessage; // Fallback if decoding fails
                                        }
                                    } else {
                                        errorMessage = "登录失败: " + responseBody;
                                    }
                                    showErrorDialog(errorMessage);
                                });
                            }
                        } catch (Exception e) { // JSON解析异常
                            runOnUiThread(() -> {
                                dismissLoadingDialog();
                                showErrorDialog("响应解析失败: " + e.getMessage());
                            });
                        }
                    } else { // 响应不成功
                        runOnUiThread(() -> {
                            dismissLoadingDialog();
                            showErrorDialog("登录失败: " + response.code() + " - " + responseBody);
                        });
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        showErrorDialog("读取响应失败: " + e.getMessage());
                    });
                }
            }
        });

    }

    /**
     * 获取用户资料
     *
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
                    Toast.makeText(LoginActivity.this, "获取用户资料失败: 网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBodyObj = response.body()) {
                    if (response.isSuccessful()) {
                        String responseBody = responseBodyObj != null ? responseBodyObj.string() : "";
                        if (responseBody.isEmpty()) {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "获取用户资料失败: 服务器返回空响应", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        Gson gson = new Gson();
                        UserProfileResponse userProfileResponse = gson.fromJson(responseBody, UserProfileResponse.class);
                        
                        if (userProfileResponse.isSuccess()) {
                            UserProfileResponse.Data data = userProfileResponse.getData();
                            if (data != null) {
                                String avatar = data.getAvatar();
                                String contact = data.getContact();
                                String description = data.getDescription();
                                String eggyid = data.getEggyid();

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
                                Toast.makeText(LoginActivity.this, "获取用户资料失败: " + userProfileResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "获取用户资料失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "获取用户资料失败: 解析错误", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mUpdateManager != null) {
            mUpdateManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 检查网络可用性
     * 检测默认网址是否可达，不可达时引导用户使用代理
     */
    private void checkNetworkAvailability() {
        new Thread(() -> {
            try {
                // 测试默认网址是否可达
                URL url = new URL("https://eggyhub.top/api/ping");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.connect();
                
                int responseCode = conn.getResponseCode();
                boolean reachable = responseCode == HttpURLConnection.HTTP_OK || 
                                    responseCode == HttpURLConnection.HTTP_NOT_FOUND; // 404 也算可达（服务器在线）
                
                conn.disconnect();
                
                if (!reachable) {
                    runOnUiThread(() -> showProxyDialog());
                }
            } catch (Exception e) {
                AppLogger.e(TAG, "Network check failed: " + e.getMessage());
                runOnUiThread(() -> showProxyDialog());
            }
        }).start();
    }

    /**
     * 显示代理选择对话框
     * 引导用户选择使用代理网络
     */
    private void showProxyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setTitle("网络连接提示");
        builder.setMessage("无法连接到默认服务器，是否切换到代理网络？\n\n代理网络可能更稳定，但需要额外验证。");
        
        builder.setPositiveButton("使用代理", (dialog, which) -> {
            ProxyConfig.setProxyEnabled(this, true);
            Toast.makeText(this, "已切换到代理网络", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("继续使用默认", (dialog, which) -> {
            ProxyConfig.setProxyEnabled(this, false);
        });
        
        builder.setNeutralButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 设置按钮样式
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.blue_eggyhub, null));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.gray, null));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.gray, null));
        }
    }

    private void navigateToMain() {
        runOnUiThread(() -> {
            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();

            // 跳转到主页
            Intent intent = new Intent(LoginActivity.this, MainActivity.class); // 创建跳转到主页的Intent
            startActivity(intent); // 启动主页
            finish(); // 关闭当前登录页面
        });
    }

    /**
     * 加密密码
     * 使用RSA算法和公钥对登录数据进行加密
     * @return 加密后的登录数据（十六进制字符串）
     */
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
            AppLogger.e(TAG, String.valueOf(sb.toString().length()));
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



private void showErrorDialog(String errorInfo) {
    runOnUiThread(() -> {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_login_error);

        // 设置弹窗背景透明，显示圆角
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // 设置弹窗宽度
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(layoutParams);
        }

        TextView errorContent = dialog.findViewById(R.id.dialog_content);
        // 将 Unicode 转换为中文
        String decodedErrorInfo = decodeUnicode(errorInfo);
        errorContent.setText("错误信息：\n" + decodedErrorInfo);

        Button copyButton = dialog.findViewById(R.id.button_copy_error);
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("errorInfo", decodedErrorInfo); // 复制解码后的信息
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "错误信息已复制", Toast.LENGTH_SHORT).show();
        });

        Button contactButton = dialog.findViewById(R.id.button_contact_developer);
        contactButton.setOnClickListener(v -> {
            if (!joinQQGroup("Hu3GvTDPlrBDWLB-4S_jFdcrG4Jxd3-t")) {
                Toast.makeText(this, "未安装QQ或版本过低", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    });
}

/**
 * 发起添加群流程
 */
public boolean joinQQGroup(String key) {
    Intent intent = new Intent();
    intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
    try {
        startActivity(intent);
        return true;
    } catch (Exception e) {
        return false;
    }
}

// 辅助方法：将 Unicode 字符串转换为中文
private String decodeUnicode(String unicodeStr) {
    if (unicodeStr == null) {
        return "";
    }
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < unicodeStr.length()) {
        if (unicodeStr.charAt(i) == '\\') {
            if (i + 1 < unicodeStr.length() && unicodeStr.charAt(i + 1) == 'u') {
                // 找到 \\uXXXX 格式的 Unicode 字符
                if (i + 5 < unicodeStr.length()) {
                    String hex = unicodeStr.substring(i + 2, i + 6);
                    try {
                        int codePoint = Integer.parseInt(hex, 16);
                        sb.append(Character.toChars(codePoint));
                        i += 6;
                        continue;
                    } catch (NumberFormatException e) {
                        // 如果解析失败，则按原样处理
                    }
                }
            }
        }
        sb.append(unicodeStr.charAt(i));
        i++;
    }
    return sb.toString();
}

/**
 * 更新登录页面网络切换按钮图标
 * 根据当前网络模式显示不同的图标
 */
private void updateLoginNetworkButtonIcon(ImageButton btnNetworkSwitch) {
    boolean isProxyEnabled = ProxyConfig.isProxyEnabled(this);
    if (isProxyEnabled) {
        btnNetworkSwitch.setImageResource(R.drawable.ic_cloud_blue);
    } else {
        btnNetworkSwitch.setImageResource(R.drawable.ic_cloud_gray);
    }
}

/**
 * 显示登录页面网络切换对话框
 * 让用户选择使用默认网络或代理网络
 */
private void showLoginNetworkSwitchDialog(ImageButton btnNetworkSwitch) {
    boolean isProxyEnabled = ProxyConfig.isProxyEnabled(this);

    // 创建 Dialog
    Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.dialog_network_switch);

    // 设置 Dialog 背景（透明，让 CardView 显示白色背景）
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    // 获取控件
    RadioGroup radioGroup = dialog.findViewById(R.id.radioGroupNetwork);
    RadioButton radioDefault = dialog.findViewById(R.id.radioDefault);
    RadioButton radioProxy = dialog.findViewById(R.id.radioProxy);
    MaterialCardView cardDefault = dialog.findViewById(R.id.cardDefault);
    MaterialCardView cardProxy = dialog.findViewById(R.id.cardProxy);
    Button btnCancel = dialog.findViewById(R.id.btnCancel);
    Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

    // 获取颜色
    int blueEggyhub = getResources().getColor(R.color.blue_eggyhub);

    // 设置当前选中状态和描边
    if (isProxyEnabled) {
        radioProxy.setChecked(true);
        cardProxy.setStrokeWidth(5);
        cardProxy.setStrokeColor(blueEggyhub);
        cardDefault.setStrokeWidth(0);
    } else {
        radioDefault.setChecked(true);
        cardDefault.setStrokeWidth(5);
        cardDefault.setStrokeColor(blueEggyhub);
        cardProxy.setStrokeWidth(0);
    }

    // 点击CardView触发RadioButton选择（手动管理单选）
    cardDefault.setOnClickListener(v -> {
        radioDefault.setChecked(true);
        radioProxy.setChecked(false);
        cardDefault.setStrokeWidth(5);
        cardDefault.setStrokeColor(blueEggyhub);
        cardProxy.setStrokeWidth(0);
    });

    cardProxy.setOnClickListener(v -> {
        radioProxy.setChecked(true);
        radioDefault.setChecked(false);
        cardProxy.setStrokeWidth(5);
        cardProxy.setStrokeColor(blueEggyhub);
        cardDefault.setStrokeWidth(0);
    });

    // 取消按钮
    btnCancel.setOnClickListener(v -> dialog.dismiss());

    // 确定按钮
    btnConfirm.setOnClickListener(v -> {
        boolean newProxyEnabled = radioProxy.isChecked();
        ProxyConfig.setProxyEnabled(this, newProxyEnabled);
        updateLoginNetworkButtonIcon(btnNetworkSwitch);
        Toast.makeText(this, "已切换到" + (newProxyEnabled ? "代理网络" : "默认网络"), Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    });
    
    dialog.show();
}
}