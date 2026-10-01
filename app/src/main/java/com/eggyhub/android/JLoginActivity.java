package com.eggyhub.android;

import android.view.WindowManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.graphics.PorterDuff;
import android.content.SharedPreferences;
import android.content.Intent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.Gson;
import com.eggyhub.android.utils.OkHttpClientFactory;
import java.io.IOException;
import android.widget.Toast;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;

public class JLoginActivity extends BaseActivity {

    private static final String TAG = "JLoginActivity";
    private static final String PROFILE_URL = "https://eggyhub.top/api/users/profile";

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressInit, progressJsoft, progressEggyhub;
    private ImageView iconInitSuccess, iconJsoftSuccess, iconEggyhubSuccess, iconInitFail, iconJsoftFail, iconEggyhubFail;
    private TextView statusTextInit, statusTextJsoft, statusTextEggyhub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jlogin);

        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);
        backButton.setOnClickListener(v -> finish());

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        progressInit = findViewById(R.id.progress_init);
        progressJsoft = findViewById(R.id.progress_jsoft);
        progressEggyhub = findViewById(R.id.progress_eggyhub);

        iconInitSuccess = findViewById(R.id.icon_init_success);
        iconJsoftSuccess = findViewById(R.id.icon_jsoft_success);
        iconEggyhubSuccess = findViewById(R.id.icon_eggyhub_success);
        iconInitFail = findViewById(R.id.icon_init_fail);
        iconJsoftFail = findViewById(R.id.icon_jsoft_fail);
        iconEggyhubFail = findViewById(R.id.icon_eggyhub_fail);

        statusTextInit = findViewById(R.id.status_text_init);
        statusTextJsoft = findViewById(R.id.status_text_jsoft);
        statusTextEggyhub = findViewById(R.id.status_text_eggyhub);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重置所有状态
                progressInit.setVisibility(View.GONE);
                progressJsoft.setVisibility(View.GONE);
                progressEggyhub.setVisibility(View.GONE);
                iconInitSuccess.setVisibility(View.GONE);
                iconJsoftSuccess.setVisibility(View.GONE);
                iconEggyhubSuccess.setVisibility(View.GONE);
                iconInitFail.setVisibility(View.GONE);
                iconJsoftFail.setVisibility(View.GONE);
                iconEggyhubFail.setVisibility(View.GONE);
                statusTextInit.setText("正在尝试初始化...");
                statusTextJsoft.setText("正在尝试登录到JsoftStudio...");
                statusTextEggyhub.setText("正在尝试登录到Eggyhub...");

                // 步骤1：初始化
                progressInit.setVisibility(View.VISIBLE);
                statusTextInit.setText("正在尝试初始化...");

                // 获取用户名和密码
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                // 1. OAuth授权初始化
                OkHttpClient client = OkHttpClientFactory.getSharedClient();
                String authorizeUrl = "https://more.jsoftstudio.top/api/oauth/authorize";
                Request authorizeRequest = new Request.Builder()
                        .url(authorizeUrl)
                        .header("X-API-Key", "ml_66bb7fd71e93fb7b16f62f446269a86401205f7633adfbf3d690586e7035f5c1")
                        .build();

                client.newCall(authorizeRequest).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        runOnUiThread(() -> {
                            progressInit.setVisibility(View.GONE);
                                        iconInitFail.setVisibility(View.VISIBLE);
                                        iconInitSuccess.setVisibility(View.GONE);
                                        statusTextInit.setText("初始化失败: " + e.getMessage());
                        });
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                        try (okhttp3.ResponseBody responseBody = response.body()) {
                            if (response.isSuccessful() && responseBody != null) {
                                try {
                                    Gson gson = new Gson();
                                    OAuthInitResponse oauthResponse = gson.fromJson(responseBody.string(), OAuthInitResponse.class);
                                    
                                    if (oauthResponse != null && oauthResponse.isSuccess()) {
                                        String state = oauthResponse.getState();
                                        runOnUiThread(() -> {
                                            progressInit.setVisibility(View.GONE);
                                            iconInitSuccess.setVisibility(View.VISIBLE);
                                            statusTextInit.setText("初始化成功");
                                        });

                                        // 2. 用户登录获取授权码
                                        String loginUrl = "https://more.jsoftstudio.top/api/oauth/login";
                                        JSoftLoginRequest loginRequestModel = new JSoftLoginRequest(username, password, state);
                                        String loginJson = gson.toJson(loginRequestModel);
                                        
                                        RequestBody body = RequestBody.create(MediaType.parse("application/json"), loginJson);
                                        Request loginRequest = new Request.Builder()
                                                .url(loginUrl)
                                                .header("X-API-Key", "ml_66bb7fd71e93fb7b16f62f446269a86401205f7633adfbf3d690586e7035f5c1")
                                                .post(body)
                                                .build();

                                        client.newCall(loginRequest).enqueue(new okhttp3.Callback() {
                                            @Override
                                            public void onFailure(okhttp3.Call call, IOException e) {
                                                runOnUiThread(() -> {
                                                    progressJsoft.setVisibility(View.GONE);
                                                    iconJsoftFail.setVisibility(View.VISIBLE);
                                                    iconJsoftSuccess.setVisibility(View.GONE);
                                                    String errorMessage = "登录到JSoftStudio失败: " + e.getMessage();
                                                    statusTextJsoft.setText(errorMessage);
                                                    showErrorDialog(errorMessage);
                                                });
                                            }

                                            @Override
                                            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                                                try (okhttp3.ResponseBody jsoftResponseBody = response.body()) {
                                                    if (response.isSuccessful() && jsoftResponseBody != null) {
                                                        try {
                                                            Gson gson = new Gson();
                                                            JSoftLoginResponse jsoftResponse = gson.fromJson(jsoftResponseBody.string(), JSoftLoginResponse.class);
                                                            if (jsoftResponse != null && jsoftResponse.isSuccess()) {
                                                                runOnUiThread(() -> {
                                                                    progressJsoft.setVisibility(View.GONE);
                                                                    iconJsoftSuccess.setVisibility(View.VISIBLE);
                                                                    statusTextJsoft.setText("成功登录到JSoftStudio");
                                                                    // 步骤3：登录到Eggyhub
                                                                    progressEggyhub.setVisibility(View.VISIBLE);
                                                                    statusTextEggyhub.setText("正在尝试登录到Eggyhub...");

                                                                    String eggyhubLoginUrl = "https://eggyhub.top/api/auth/jlogin";
                                                                    JSoftLoginResponse.User userObject = jsoftResponse.getUser();
                                                                    String email = userObject != null ? userObject.getEmail() : "";
                                                                    int id = userObject != null ? userObject.getId() : 0;
                                                                    String userIdVal = userObject != null ? userObject.getUserId() : username;
                                                                    String userNameVal = userObject != null ? userObject.getUsername() : username;
                                                                    EggyhubLoginRequest eggyhubRequestModel = new EggyhubLoginRequest(
                                                                        state, 
                                                                        jsoftResponse.getCode(), 
                                                                        email, 
                                                                        id, 
                                                                        userIdVal, 
                                                                        password, 
                                                                        userNameVal
                                                                    );
                                                                    
                                                                    String eggyhubLoginJson = gson.toJson(eggyhubRequestModel);
                                                                    RequestBody eggyhubBody = RequestBody.create(MediaType.parse("application/json"), eggyhubLoginJson);
                                                                    Request eggyhubRequest = new Request.Builder()
                                                                            .url(eggyhubLoginUrl)
                                                                            .post(eggyhubBody)
                                                                            .build();

                                                                    client.newCall(eggyhubRequest).enqueue(new okhttp3.Callback() {
                                                                        @Override
                                                                        public void onFailure(okhttp3.Call call, IOException e) {
                                                                             runOnUiThread(() -> {
                                                                                 progressEggyhub.setVisibility(View.GONE);
                                                                                 iconEggyhubSuccess.setVisibility(View.GONE);
                                                                                 iconEggyhubFail.setVisibility(View.VISIBLE);
                                                                                 statusTextEggyhub.setText("登录到Eggyhub失败: " + e.getMessage());
                                                                             });
                                                                         }

                                                                        @Override
                                                                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                                                                            try (okhttp3.ResponseBody eggyhubResponseBody = response.body()) {
                                                                                if (response.isSuccessful() && eggyhubResponseBody != null) {
                                                                                    final String responseBody = eggyhubResponseBody.string();
                                                                                    runOnUiThread(() -> {
                                                                                        progressEggyhub.setVisibility(View.GONE);
                                                                                        iconEggyhubSuccess.setVisibility(View.VISIBLE);
                                                                                        statusTextEggyhub.setText("成功登录到Eggyhub");

                                                                                        // 解析登录响应JSON
                                                                                        try {
                                                                                            Gson gson = new Gson();
                                                                                            LoginResponse loginResponse = gson.fromJson(responseBody, LoginResponse.class);
                                                                                            String status = loginResponse != null ? loginResponse.getStatus() : "";
                                                                                            
                                                                                            if ("success".equals(status)) { // 验证登录成功状态
                                                                                                // 获取核心登录数据
                                                                                                String accessToken = loginResponse.getAccessToken();
                                                                                                LoginResponse.User userObject = loginResponse.getUser();
                                                                                                String username = "";
                                                                                                int id = -1;
                                                                                                String Email = "";
                                                                                                String role = "";
                                                                                                String sponser = "0";

                                                                                                if (userObject != null) {
                                                                                                    username = userObject.getUsername();
                                                                                                    id = userObject.getId();
                                                                                                    Email = userObject.getEmail();
                                                                                                    role = userObject.getRole();
                                                                                                    sponser = userObject.getSponser();
                                                                                                }

                                                                                                // 存储用户数据到标准 SharedPreferences 容器
                                                                                                SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                                                                                SharedPreferences.Editor editor = preferences.edit();
                                                                                                editor.putString("email", Email);
                                                                                                editor.putString("password", password); // 假设 password 在当前作用域可用
                                                                                                SecureStorageManager.encryptAndStore("access_token", accessToken);
                                                                                                editor.putString("username", username);
                                                                                                editor.putInt("id", id);
                                                                                                editor.putString("role", role);
                                                                                                editor.putString("sponser", sponser);
                                                                                                editor.apply();

                                                                                                // 获取详细用户资料（需确保fetchUserProfile方法已在JLoginActivity中实现）
                                                                                                if (id != -1) {
                                                                                                    fetchUserProfile(id);
                                                                                                }

                                                                                                // 跳转到主页面（可根据fetchUserProfile结果调整跳转时机）
                                                                                                Intent intent = new Intent(JLoginActivity.this, MainActivity.class);
                                                                                                startActivity(intent);
                                                                                                finish();
                                                                                            } else {
                                                                                                // 处理登录失败的情况，例如显示错误信息
                                                                                                String errorMessage = loginResponse != null ? loginResponse.getMessage() : "登录失败";
                                                                                                showErrorDialog(errorMessage);
                                                                                            }
                                                                                        } catch (Exception e) {
                                                                                            Toast.makeText(JLoginActivity.this, "登录响应解析失败", Toast.LENGTH_SHORT).show();
                                                                                            e.printStackTrace();
                                                                                        }
                                                                                    });
                                                                                } else {
                                                                                    String errorBody = response.body() != null ? response.body().string() : null;
                                                                                    String errorMessage = "";
                                                                                    if (errorBody != null) {
                                                                                        try {
                                                                                            Gson gson = new Gson();
                                                                                            LoginResponse errorResponse = gson.fromJson(errorBody, LoginResponse.class);
                                                                                            errorMessage = errorResponse != null ? errorResponse.getMessage() : "";
                                                                                            if ("用户名重复".equals(errorMessage)) {
                                                                                                errorMessage = "用户名与Eggyhub已有账号\n的用户名重复，受着";
                                                                                            }
                                                                                        } catch (Exception e) {
                                                                                            errorMessage = "错误详情解析失败";
                                                                                        }
                                                                                    }
                                                                                    String finalErrorMessage = errorMessage;
                                                                                     runOnUiThread(() -> {
                                                                                         progressEggyhub.setVisibility(View.GONE);
                                                                                         iconEggyhubSuccess.setVisibility(View.VISIBLE);
                                                                                         iconEggyhubFail.setVisibility(View.VISIBLE);
                                                                                         statusTextEggyhub.setText("登录到Eggyhub失败: " + (finalErrorMessage.isEmpty() ? String.valueOf(response.code()) : finalErrorMessage));
                                                                                     });
                                                                                }
                                                                            } finally {
                                                                                if (response.body() != null) {
                                                                                    response.body().close();
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                                });
                                                            } else {
                                                                String errorMessage = jsoftResponse.getMessage();
                                                                if (errorMessage == null) errorMessage = "未知错误";
                                                                String finalErrorMessage = errorMessage;
                                                                runOnUiThread(() -> {
                                                                    progressJsoft.setVisibility(View.GONE);
                                                                    iconJsoftFail.setVisibility(View.VISIBLE);
                                                                    iconJsoftSuccess.setVisibility(View.GONE);
                                                                    String statusMessage = "登录到JSoftStudio失败: " + finalErrorMessage;
                                                                    statusTextJsoft.setText(statusMessage);
                                                                    showErrorDialog(gson.toJson(jsoftResponse));
                                                                });
                                                            }
                                                        } catch (Exception e) {
                                                            runOnUiThread(() -> {
                                                                progressJsoft.setVisibility(View.GONE);
                                                                iconJsoftFail.setVisibility(View.VISIBLE);
                                                                iconJsoftSuccess.setVisibility(View.GONE);
                                                                String statusMessage = "登录到JSoftStudio失败: 数据解析错误";
                                                                statusTextJsoft.setText(statusMessage);
                                                                showErrorDialog(e.getMessage());
                                                            });
                                                        }
                                                    } else {
                                                        String errorBody = null;
                                                        try {
                                                            errorBody = response.body() != null ? response.body().string() : null;
                                                            final String finalErrorBody = errorBody;
                                                            if (errorBody != null) {
                                                                Gson gson = new Gson();
                                                                JSoftLoginResponse errorResponse = gson.fromJson(errorBody, JSoftLoginResponse.class);
                                                                String errorMessage = errorResponse != null ? errorResponse.getMessage() : "";
                                                                
                                                                if (errorMessage != null && !errorMessage.isEmpty()) {
                                                                    runOnUiThread(() -> {
                                                                        progressJsoft.setVisibility(View.GONE);
                                                                        iconJsoftFail.setVisibility(View.VISIBLE);
                                                                        iconJsoftSuccess.setVisibility(View.GONE);
                                                                        statusTextJsoft.setText("登录到JSoftStudio失败: " + errorMessage);
                                                                        showErrorDialog(finalErrorBody);
                                                                    });
                                                                } else {
                                                                    runOnUiThread(() -> {
                                                                        progressJsoft.setVisibility(View.GONE);
                                                                        iconJsoftFail.setVisibility(View.VISIBLE);
                                                                        iconJsoftSuccess.setVisibility(View.GONE);
                                                                        statusTextJsoft.setText("登录到JSoftStudio失败: " + response.code());
                                                                        showErrorDialog(finalErrorBody);
                                                                    });
                                                                }
                                                            } else {
                                                                runOnUiThread(() -> {
                                                                    progressJsoft.setVisibility(View.GONE);
                                                                    iconJsoftFail.setVisibility(View.VISIBLE);
                                                                    iconJsoftSuccess.setVisibility(View.GONE);
                                                                    statusTextJsoft.setText("登录到JSoftStudio失败: " + response.code());
                                                                    showErrorDialog(String.valueOf(response.code()));
                                                                });
                                                            }
                                                        } catch (Exception e) {
                                                            final String finalErrorBody1 = errorBody;
                                                            runOnUiThread(() -> {
                                                                progressJsoft.setVisibility(View.GONE);
                                                                iconJsoftFail.setVisibility(View.VISIBLE);
                                                                iconJsoftSuccess.setVisibility(View.GONE);
                                                                statusTextJsoft.setText("登录到JSoftStudio失败: " + response.code() + " (错误详情解析失败)");
                                                                showErrorDialog(finalErrorBody1 != null ? finalErrorBody1 : e.getMessage());
                                                            });
                                                        }
                                                    }
                                                } finally {
                                                    if (response.body() != null) {
                                                        response.body().close();
                                                    }
                                                }
                                            }
                                        });

                                    } else {
                                        String errorMessage = oauthResponse != null ? oauthResponse.getMessage() : "数据解析为空";
                                        if (errorMessage == null) errorMessage = "未知错误";
                                        String finalErrorMessage = errorMessage;
                                        runOnUiThread(() -> {
                                            progressInit.setVisibility(View.GONE);
                                            iconInitFail.setVisibility(View.VISIBLE);
                                            iconInitSuccess.setVisibility(View.GONE);
                                            statusTextInit.setText("初始化失败: " + finalErrorMessage);
                                            showErrorDialog(oauthResponse != null ? gson.toJson(oauthResponse) : "NULL Response");
                                        });
                                    }
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        progressInit.setVisibility(View.GONE);
                                        iconInitFail.setVisibility(View.VISIBLE);
                                        iconInitSuccess.setVisibility(View.GONE);
                                        statusTextInit.setText("初始化失败: 数据解析错误");
                                        showErrorDialog(e.getMessage());
                                    });
                                }
                            } else {
                                String errorBody = null;
                                try {
                                    errorBody = response.body() != null ? response.body().string() : null;
                                    final String finalErrorBody = errorBody;
                                    if (errorBody != null) {
                                        Gson gson = new Gson();
                                        OAuthInitResponse errorResponse = gson.fromJson(errorBody, OAuthInitResponse.class);
                                        String errorMessage = errorResponse != null ? errorResponse.getMessage() : "";
                                        
                                        if (errorMessage != null && !errorMessage.isEmpty()) {
                                            runOnUiThread(() -> {
                                                progressInit.setVisibility(View.GONE);
                                                iconInitFail.setVisibility(View.VISIBLE);
                                                iconInitSuccess.setVisibility(View.GONE);
                                                statusTextInit.setText("初始化失败: " + errorMessage);
                                                showErrorDialog(finalErrorBody);
                                            });
                                        } else {
                                            runOnUiThread(() -> {
                                                progressInit.setVisibility(View.GONE);
                                                iconInitFail.setVisibility(View.VISIBLE);
                                                iconInitSuccess.setVisibility(View.GONE);
                                                statusTextInit.setText("初始化失败: " + response.code());
                                                showErrorDialog(finalErrorBody);
                                            });
                                        }
                                    } else {
                                        runOnUiThread(() -> {
                                            progressInit.setVisibility(View.GONE);
                                            iconInitFail.setVisibility(View.VISIBLE);
                                            iconInitSuccess.setVisibility(View.GONE);
                                            statusTextInit.setText("初始化失败: " + response.code());
                                            showErrorDialog(String.valueOf(response.code()));
                                        });
                                    }
                                } catch (Exception e) {
                                    final String finalErrorBody1 = errorBody;
                                    runOnUiThread(() -> {
                                        progressInit.setVisibility(View.GONE);
                                        iconInitFail.setVisibility(View.VISIBLE);
                                        iconInitSuccess.setVisibility(View.GONE);
                                        statusTextInit.setText("初始化失败: " + response.code() + " (错误详情解析失败)");
                                        showErrorDialog(finalErrorBody1 != null ? finalErrorBody1 : e.getMessage());
                                    });
                                }
                            }
                        } finally {
                            if (response.body() != null) {
                                response.body().close();
                            }
                        }
                    }
                });
            }
        });
    }

    private void showErrorDialog(String errorInfo) {
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

        TextView dialogContent = dialog.findViewById(R.id.dialog_content);
        Button copyButton = dialog.findViewById(R.id.button_copy_error);
        Button contactButton = dialog.findViewById(R.id.button_contact_developer);
        // 将 Unicode 转换为中文
        String decodedErrorInfo = decodeUnicode(errorInfo);
        dialogContent.setText("错误信息：\n" + decodedErrorInfo);

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("error_info", dialogContent.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "错误信息已复制", Toast.LENGTH_SHORT).show();
        });

        contactButton.setOnClickListener(v -> {
            if (!joinQQGroup("Hu3GvTDPlrBDWLB-4S_jFdcrG4Jxd3-t")) {
                Toast.makeText(this, "未安装QQ或版本过低", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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
    private void fetchUserProfile(int userId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = PROFILE_URL + "?id=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(JLoginActivity.this, "获取用户资料失败: 网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBodyObj = response.body()) {
                    if (response.isSuccessful()) {
                        String responseBody = responseBodyObj != null ? responseBodyObj.string() : "";
                        if (responseBody.isEmpty()) {
                            runOnUiThread(() -> {
                                Toast.makeText(JLoginActivity.this, "获取用户资料失败: 服务器返回空响应", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        Gson gson = new Gson();
                        UserProfileResponse userProfile = gson.fromJson(responseBody, UserProfileResponse.class);
                        
                        if (userProfile != null && userProfile.isSuccess()) {
                            UserProfileResponse.Data data = userProfile.getData();
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

                                runOnUiThread(() -> {
                                    //响应成功逻辑
                                });
                            }
                        } else {
                            String message = userProfile != null ? userProfile.getMessage() : "获取用户资料失败";
                            runOnUiThread(() -> {
                                Toast.makeText(JLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(JLoginActivity.this, "获取用户资料失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(JLoginActivity.this, "用户资料响应解析失败", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }
        });
    }
}