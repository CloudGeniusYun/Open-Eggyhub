package com.eggyhub.android;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.Gson;
import com.eggyhub.android.ForgotPasswordRequest;
import com.eggyhub.android.BasicResponse;
import com.eggyhub.android.utils.OkHttpClientFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 找回密码Activity
 * 用于处理用户密码重置请求
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    // 日志标签
    private static final String TAG = "ForgotPasswordActivity";
    // 超时时间(秒)
    private static final int TIMEOUT = 10;
    // API基础URL
    private static final String BASE_URL = "https://eggyhub.top/api";
    // 密码重置请求URL
    private static final String FORGOT_PASSWORD_URL = BASE_URL + "/password/forgot";

    // 成员变量
    private EditText mEmailEditText;        // 邮箱输入框
    private Button mResetPasswordButton;    // 重置密码按钮
    private OkHttpClient mOkHttpClient;     // OkHttp客户端
    private Call mForgotPasswordCall;       // 密码重置请求

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        initData();
        setupListeners();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        mEmailEditText = findViewById(R.id.email_edit_text);
        mResetPasswordButton = findViewById(R.id.reset_password_button);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 初始化OkHttpClient（使用工厂类，自动添加代理拦截器）
        mOkHttpClient = OkHttpClientFactory.createCustomClient(TIMEOUT, TIMEOUT, TIMEOUT);
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 设置邮箱输入框文本变化监听器
        mEmailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 设置重置密码按钮点击事件
        mResetPasswordButton.setOnClickListener(v -> {
            if (validateEmail()) {
                sendResetPasswordRequest(mEmailEditText.getText().toString().trim());
            }
        });
    }

    /**
     * 发送重置密码请求
     * @param email 用户邮箱
     */
    private void sendResetPasswordRequest(String email) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        ForgotPasswordRequest requestModel = new ForgotPasswordRequest(email);
        Gson gson = new Gson();
        String jsonString = gson.toJson(requestModel);

        RequestBody body = RequestBody.create(jsonString, JSON);
        Request request = new Request.Builder()
                .url(FORGOT_PASSWORD_URL)
                .post(body)
                .build();

        mForgotPasswordCall = mOkHttpClient.newCall(request);
        mForgotPasswordCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    return;
                }
                runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (call.isCanceled()) {
                        return;
                    }

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(ForgotPasswordActivity.this, "重置密码链接已发送至邮箱", Toast.LENGTH_SHORT).show();
                            finish(); // 关闭当前页面
                        });
                    } else {
                        final String[] errorMessage = {""};
                        if (responseBody != null) {
                            String bodyString = responseBody.string();
                            try {
                                BasicResponse errorModel = gson.fromJson(bodyString, BasicResponse.class);
                                errorMessage[0] = errorModel != null ? errorModel.getMessage() : "未知错误";
                            } catch (Exception e) {
                                errorMessage[0] = "响应解析失败";
                            }
                        }
                        final String error = errorMessage[0];
                        runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "重置密码失败: " + error, Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /**
     * 验证邮箱格式
     * @return 邮箱格式是否有效
     */
    private boolean validateEmail() {
        String email = mEmailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            mEmailEditText.setError("邮箱不能为空");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailEditText.setError("请输入有效的邮箱地址");
            return false;
        } else {
            mEmailEditText.setError(null);
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消网络请求
        if (mForgotPasswordCall != null && !mForgotPasswordCall.isCanceled()) {
            mForgotPasswordCall.cancel();
        }
    }
}