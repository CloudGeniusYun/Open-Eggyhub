package com.eggyhub.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;

/**
 * 注册活动
 * 负责用户注册功能，包括表单验证、网络请求和响应处理
 */
public class RegisterActivity extends BaseActivity {
    // 静态常量定义
    private static final String BASE_URL = "https://eggyhub.top/api";
    private static final String REGISTER_URL = BASE_URL + "/auth/register";
    private static final int TIMEOUT = 30; // 超时时间，单位：秒

    // UI组件
    private TextView mTvAlreadyHaveAccount;
    private EditText mEtUserName;
    private EditText mEtEmail;
    private EditText mEtPassword;
    private EditText mEtConfirmPass;
    private EditText mEtInviteCode;
    private Button mBtnRegister;

    // 网络请求客户端和调用对象
    private OkHttpClient mHttpClient;
    private final Gson gson = new Gson();
    private Call mRegisterCall;

    /**
     * 活动创建时调用的方法
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化网络客户端
        initHttpClient();

        // 初始化UI组件
        initViews();

        // 设置事件监听器
        setupListeners();
    }

    /**
     * 初始化网络客户端
     */
    private void initHttpClient() {
        mHttpClient = OkHttpClientFactory.createCustomClient(TIMEOUT, TIMEOUT, TIMEOUT);
    }

    /**
     * 初始化UI组件
     */
    private void initViews() {
        mTvAlreadyHaveAccount = findViewById(R.id.tv_already_have_account);
        mEtUserName = findViewById(R.id.et_user_name);
        mEtEmail = findViewById(R.id.et_email);
        mEtPassword = findViewById(R.id.et_password);
        mEtConfirmPass = findViewById(R.id.et_confirm_pass);
        mEtInviteCode = findViewById(R.id.et_invite_code);
        mBtnRegister = findViewById(R.id.btn_register);
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 设置"已有账号"文本的点击事件
        mTvAlreadyHaveAccount.setOnClickListener(v -> navigateToLogin());

        // 设置注册按钮的点击事件
        mBtnRegister.setOnClickListener(v -> handleRegister());
    }

    /**
     * 跳转到登录页面
     */
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 处理注册逻辑
     */
    private void handleRegister() {
        // 获取输入数据
        String username = mEtUserName.getText().toString().trim();
        String email = mEtEmail.getText().toString().trim();
        String password = mEtPassword.getText().toString().trim();
        String confirmPassword = mEtConfirmPass.getText().toString().trim();
        String inviteCode = mEtInviteCode.getText().toString().trim();

        // 验证输入
        if (!validateInput(username, email, password, confirmPassword)) {
            return;
        }

        // 创建请求对象
        RegisterRequest registerRequest = new RegisterRequest(username, email, password, inviteCode);

        // 发送注册请求
        sendRegisterRequest(registerRequest);
    }

    /**
     * 验证用户输入
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @param confirmPassword 确认密码
     * @return 验证是否通过
     */
    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        // 验证用户名
        if (username.isEmpty()) {
            showErrorDialog("请输入用户名");
            return false;
        }

        // 验证邮箱
        if (email.isEmpty() || !email.contains("@")) {
            showErrorDialog("请输入有效的邮箱地址");
            return false;
        }

        // 验证密码
        if (password.length() < 6) {
            showErrorDialog("密码长度不能小于6位");
            return false;
        }

        // 验证密码一致性
        if (!password.equals(confirmPassword)) {
            showErrorDialog("两次输入的密码不一致");
            return false;
        }

        return true;
    }

    /**
     * 发送注册请求
     * @param registerRequest 请求体
     */
    private void sendRegisterRequest(RegisterRequest registerRequest) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(gson.toJson(registerRequest), JSON);

        Request request = new Request.Builder()
                .url(REGISTER_URL)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 取消之前的请求（如果存在）
        if (mRegisterCall != null && !mRegisterCall.isCanceled()) {
            mRegisterCall.cancel();
        }

        // 执行新请求
        mRegisterCall = mHttpClient.newCall(request);
        mRegisterCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    return; // 请求已取消，不处理
                }

                runOnUiThread(() -> showErrorDialog("注册失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) {
                    return; // 请求已取消，不处理
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        String bodyString = responseBody.string();
                        handleRegisterResponse(response, bodyString);
                    }
                }
            }
        });
    }

    /**
     * 处理注册响应
     * @param response 响应对象
     * @param responseBody 响应体内容
     * @throws IOException IO异常
     */
    private void handleRegisterResponse(Response response, String responseBody) throws IOException {
        if (response.isSuccessful()) {
            try {
                BasicResponse basicResponse = gson.fromJson(responseBody, BasicResponse.class);
                String message = basicResponse != null ? basicResponse.getMessage() : "请前往邮箱验证";
                if (message == null) message = "请前往邮箱验证";
                
                final String finalMessage = message;
                runOnUiThread(() -> {
                    showErrorDialog(finalMessage);
                    navigateToLogin();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showErrorDialog("请前往邮箱验证");
                    navigateToLogin();
                });
            }
        } else {
            try {
                BasicResponse errorResponse = gson.fromJson(responseBody, BasicResponse.class);
                String errorMessage = errorResponse != null ? errorResponse.getMessage() : "未知错误";
                if (errorMessage == null) errorMessage = "未知错误";
                
                final String finalErrorMessage = errorMessage;
                runOnUiThread(() -> showErrorDialog("注册失败: " + finalErrorMessage));
            } catch (Exception e) {
                runOnUiThread(() -> showErrorDialog("注册失败: " + responseBody));
            }
        }
    }

    /**
     * 活动销毁时调用
     * 取消网络请求，避免内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRegisterCall != null && !mRegisterCall.isCanceled()) {
            mRegisterCall.cancel();
        }
    }

    /**
     * 显示错误提示弹窗
     * @param message 错误消息
     */
    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_error_simple, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 设置弹窗背景透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // 设置消息内容
        TextView messageText = dialogView.findViewById(R.id.dialog_message);
        messageText.setText(message);
        
        // 设置确定按钮
        Button okButton = dialogView.findViewById(R.id.button_ok);
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}