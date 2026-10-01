package com.eggyhub.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import java.io.InputStream;

import com.google.gson.Gson;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 个人主页活动类
 * 负责展示用户信息和处理用户设置相关操作
 */
public class PersonalHomePageActivity extends BaseActivity {
    private static final String TAG = "PersonalHomePageActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // UI组件
    private TextView userNameTextView;
    private TextView userIdTextView;
    private TextView userEmailTextView;
    private TextView userRoleTextView;
    private TextView userEggyIdTextView;
    private TextView userMyTextView;
    private TextView userFindMeTextView;
    private LinearLayout changeNameLayout;
    private LinearLayout changePasswordLayout;
    private LinearLayout deregistrationLayout;
    private LinearLayout logoutLayout;
    private LinearLayout changeDs;
    private LinearLayout changeTx;
    private LinearLayout selectNameFrameLayout;

    // 数据存储
    private SharedPreferences preferences;

    // 用户信息
    private String userName;
    private int userId;
    private String userEmail;
    private String userRole;
    private String accessToken;
    private String userEggyId;
    private String userMy;
    private String userFindMe;

    /**
     * 活动创建时调用
     * 初始化UI组件、设置监听器、加载用户数据
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_home_page);

        // 初始化UI组件
        initViews();

        // 初始化数据
        initData();

        // 设置监听器
        setupListeners();
    }

    /**
     * 初始化UI组件
     * 从布局中获取所有需要的视图元素
     */
    private void initViews() {
        // 返回按钮
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> onBackPressed());

        userIdTextView = findViewById(R.id.text_view_user_id);
        userNameTextView = findViewById(R.id.text_view_user_name);
        userEmailTextView = findViewById(R.id.text_view_user_email);
        userRoleTextView = findViewById(R.id.text_view_user_role);
        userEggyIdTextView = findViewById(R.id.text_view_eggy_id);
        userMyTextView = findViewById(R.id.text_view_my);
        userFindMeTextView = findViewById(R.id.text_view_findme);
        changeNameLayout = findViewById(R.id.layout_change_name);
        changePasswordLayout = findViewById(R.id.layout_change_password);
        deregistrationLayout = findViewById(R.id.layout_deregistration);
        logoutLayout = findViewById(R.id.layout_logout);
        changeDs = findViewById(R.id.layout_change_ds);
        changeTx = findViewById(R.id.layout_change_tx);
        selectNameFrameLayout = findViewById(R.id.layout_select_name_frame);
    }

    /**
     * 初始化用户数据
     * 从SharedPreferences中读取用户信息
     */
    private void initData() {
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userName = preferences.getString("username", "");
        userId = preferences.getInt("id", 0);
        userEmail = SecureStorageManager.decryptAndRetrieve("email");
        if (userEmail == null) userEmail = "";
        userRole = preferences.getString("role", "");
        userEggyId = preferences.getString("eggyid","");
        userMy = preferences.getString("description","");
        userFindMe = preferences.getString("contact","");
        accessToken = SecureStorageManager.decryptAndRetrieve("access_token");

        // 更新UI显示
        updateUserInfoDisplay();
    }

    /**
     * 更新用户信息显示
     * 将用户数据显示到对应的TextView上
     */
    private void updateUserInfoDisplay() {
        userNameTextView.setText("昵称：" + userName);
        userIdTextView.setText("ID：" + userId);
        userEmailTextView.setText("邮箱：" + userEmail);
        String userRole = preferences.getString("role", "user");
        String sponser = preferences.getString("sponser", "0");
        if("user".equals(userRole)){
            if (!"0.0".equals(sponser)) {
                userRoleTextView.setText("用户组：赞助用户");
            } else {
                userRoleTextView.setText("用户组：标准用户");
            };
        }else{
            if("admin".equals(userRole)){
                userRoleTextView.setText("用户组：管理员");
            }else{
                userRoleTextView.setText("用户组：未知");
            }
        }
        userEggyIdTextView.setText("蛋仔昵称："+userEggyId);
        userMyTextView.setText("自我介绍："+userMy);
        userFindMeTextView.setText("联系方式："+userFindMe);
    }

    /**
     * 设置监听器
     * 为所有可点击元素设置点击事件监听器
     */
    private void setupListeners() {
        changeNameLayout.setOnClickListener(v -> showChangeNameDialog());
        changePasswordLayout.setOnClickListener(v -> showChangePasswordDialog());
        logoutLayout.setOnClickListener(v -> handleLogout());
        deregistrationLayout.setOnClickListener(v -> showDeregistrationDialog());
        changeDs.setOnClickListener(v -> showUpdateProfileDialog());
        changeTx.setOnClickListener(v -> openGallery());
        selectNameFrameLayout.setOnClickListener(v -> showSelectNameFrameDialog());
    }

    /**
     * 打开图库选择图片
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    /**
     * 处理活动结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            uploadImage(selectedImage);
        }
    }

    /**
     * 上传图片到服务器
     * @param imageUri 图片URI
     */
    private void uploadImage(Uri imageUri) {
        if (accessToken == null) {
            showToast("请先登录");
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        try {
            // 获取文件名
            String fileName = getFileNameFromUri(imageUri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "avatar.jpg"; // 默认名称
            }

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", fileName,
                            RequestBody.create(MediaType.parse("image/*"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://eggyhub.top/api/users/upload_avatar")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    AppLogger.e(TAG, "上传图片失败", e);
                    runOnUiThread(() -> showToast("上传失败，请检查网络"));
                }

                @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        if (responseBody != null) {
                            String responseString = responseBody.string();
                            runOnUiThread(() -> {
                                showToast("上传成功");
                                // 更新SharedPreferences中的头像信息
                                try {
                                    Gson gson = new Gson();
                                    AvatarUploadResponse responseModel = gson.fromJson(responseString, AvatarUploadResponse.class);
                                    String avatarUrl = responseModel != null ? responseModel.getCover() : "";
                                    
                                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString("avatar", avatarUrl);
                                        editor.apply();
                                    }
                                } catch (Exception e) {
                                    AppLogger.e(TAG, "解析上传响应失败", e);
                                }
                            });
                        } else {
                            runOnUiThread(() -> showToast("上传成功，但响应为空"));
                        }
                    } else {
                        runOnUiThread(() -> showToast("上传失败，错误码: " + response.code()));
                    }
                }
            }
            });
        } catch (IOException e) {
            AppLogger.e(TAG, "读取图片失败", e);
            showToast("读取图片失败");
        }
    }

    /**
     * 从Uri获取文件名
     * @param uri 图片URI
     * @return 文件名
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (scheme != null && scheme.equals("content")) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        fileName = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
        } else if (scheme != null && scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        }

        return fileName;
    }

    /**
     * 处理退出登录
     * 清除用户数据并跳转到登录界面
     */
    private void handleLogout() {
        // 清除加密存储（token等敏感数据）
        SecureStorageManager.clearAll();

        // 清除 user_prefs
        SharedPreferences.Editor userEditor = preferences.edit();
        userEditor.clear();
        userEditor.apply();

        // 清除 article_prefs (如果存在)
        SharedPreferences articlePrefs = getSharedPreferences("article_prefs", MODE_PRIVATE);
        SharedPreferences.Editor articleEditor = articlePrefs.edit();
        articleEditor.clear();
        articleEditor.apply();

        // 停止所有悬浮窗服务
        stopService(new Intent(this, OverlayService.class));
        stopService(new Intent(this, FirstOverlayService.class));
        stopService(new Intent(this, MinimizeIconService.class));

        Intent intent = new Intent(PersonalHomePageActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /**
     * 显示修改昵称对话框
     * 创建并显示用于输入新昵称的对话框
     */
    private void showChangeNameDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_nickname, null);
        EditText editTextNickname = dialogView.findViewById(R.id.editTextNickname);
        editTextNickname.setHint("请输入新昵称");
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        AlertDialog dialog = builder.create();
        dialog.setView(dialogView, 0, 0, 0, 0); // 移除默认边距

        // 显示对话框
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        
        // 为EditText请求焦点并设置光标位置
        editTextNickname.requestFocus();
        editTextNickname.setSelection(editTextNickname.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String newName = editTextNickname.getText().toString().trim();
                if (!newName.isEmpty()) {
                    updateUserName(newName);
                    dialog.dismiss();
                } else {
                    Toast.makeText(PersonalHomePageActivity.this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 更新用户昵称
     * 发送网络请求修改用户昵称
     * @param newName 新的用户昵称
     */
    private void updateUserName(String newName) {
        if (accessToken == null) {
            showToast("请先登录");
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        Gson gson = new Gson();
        UpdateNameRequest requestModel = new UpdateNameRequest(newName);
        String jsonBody = gson.toJson(requestModel);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/reset_name")
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "修改昵称网络请求失败", e);
                runOnUiThread(() -> showToast("网络异常"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        if (responseBody != null) {
                            String jsonString = responseBody.string();

                            try {
                                Gson gson = new Gson();
                                UpdateNameResponse responseModel = gson.fromJson(jsonString, UpdateNameResponse.class);
                                String updatedName = responseModel != null ? responseModel.getNewUsername() : "";
                                
                                if (updatedName != null && !updatedName.isEmpty() && newName.equals(updatedName)) {
                                    runOnUiThread(() -> {
                                        showToast("修改成功");
                                        // 更新UI和本地存储
                                        userName = updatedName;
                                        updateUserInfoDisplay();
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString("username", updatedName);
                                        editor.apply();
                                    });
                                } else {
                                    runOnUiThread(() -> showToast("修改失败"));
                                }
                            } catch (Exception e) {
                                AppLogger.e(TAG, "解析修改昵称响应失败", e);
                                runOnUiThread(() -> showToast("数据解析失败"));
                            }
                        } else {
                            runOnUiThread(() -> showToast("响应为空"));
                        }
                    } else {
                        runOnUiThread(() -> showToast("请求失败: " + response.code()));
                    }
                }
            }
        });
    }

    /**
     * 显示修改密码对话框
     * 创建并显示用于输入旧密码和新密码的对话框
     */
    private void showChangePasswordDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText editTextOldPassword = dialogView.findViewById(R.id.editTextOldPassword);
        EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        AlertDialog dialog = builder.create();
        dialog.setView(dialogView, 0, 0, 0, 0); // 移除默认边距

        // 显示对话框
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        
        // 为第一个EditText请求焦点并设置光标位置
        editTextOldPassword.requestFocus();
        editTextOldPassword.setSelection(editTextOldPassword.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String oldPassword = editTextOldPassword.getText().toString().trim();
                String newPassword = editTextNewPassword.getText().toString().trim();

                if (oldPassword.isEmpty()) {
                    showToast("旧密码不能为空");
                    return;
                }

                if (newPassword.isEmpty()) {
                    showToast("新密码不能为空");
                    return;
                }

                updatePassword(oldPassword, newPassword);
                dialog.dismiss();
            });
        }
    }

    /**
     * 更新用户密码
     * 发送网络请求修改用户密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    private void updatePassword(String oldPassword, String newPassword) {
        if (accessToken == null) {
            showToast("请先登录");
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        Gson gson = new Gson();
        ChangePasswordRequest requestModel = new ChangePasswordRequest(oldPassword, newPassword);
        String jsonBody = gson.toJson(requestModel);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/reset_pswd")
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "修改密码网络请求失败", e);
                runOnUiThread(() -> showToast("网络异常"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        if (responseBody != null) {
                            String jsonString = responseBody.string();

                            try {
                                Gson gson = new Gson();
                                ChangePasswordResponse responseModel = gson.fromJson(jsonString, ChangePasswordResponse.class);
                                String message = responseModel != null ? responseModel.getMessage() : null;

                                if (message != null) {
                                    if ("Password updated successfully".equals(message)) {
                                        runOnUiThread(() -> showToast("密码修改成功"));
                                    } else {
                                        runOnUiThread(() -> showToast("修改失败: " + message));
                                    }
                                } else {
                                    runOnUiThread(() -> showToast("未知响应格式"));
                                }
                            } catch (Exception e) {
                                AppLogger.e(TAG, "解析修改密码响应失败", e);
                                runOnUiThread(() -> showToast("数据解析失败"));
                            }
                        } else {
                            runOnUiThread(() -> showToast("响应为空"));
                        }
                    } else {
                        runOnUiThread(() -> showToast("密码错误或请求失败"));
                    }
                }
            }
        });
    }

    /**
     * 显示注销账号对话框
     * 创建并显示用于输入密码确认注销的对话框
     */
    private void showDeregistrationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("注销账号（请输入密码）");

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("请输入密码");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("确认", (dialog, which) -> {})
               .setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮颜色
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.black));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.black));

        // 确认按钮点击事件
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (!password.isEmpty()) {
                deregisterAccount(password);
                dialog.dismiss();
            } else {
                showToast("密码不能为空");
            }
        });
    }

    /**
     * 显示更新资料对话框
     * 创建并显示用于输入蛋仔昵称、自我介绍和联系方式的对话框
     */
    private void showUpdateProfileDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_profile, null);
        EditText editTextEggyId = dialogView.findViewById(R.id.editTextEggyId);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        EditText editTextContact = dialogView.findViewById(R.id.editTextContact);
        
        // 设置默认值
        editTextEggyId.setText(userEggyId);
        editTextDescription.setText(userMy);
        editTextContact.setText(userFindMe);
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        AlertDialog dialog = builder.create();
        dialog.setView(dialogView, 0, 0, 0, 0); // 移除默认边距

        // 显示对话框
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        
        // 为第一个EditText请求焦点并设置光标位置
        editTextEggyId.requestFocus();
        editTextEggyId.setSelection(editTextEggyId.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String eggyId = editTextEggyId.getText().toString().trim();
                String description = editTextDescription.getText().toString().trim();
                String contact = editTextContact.getText().toString().trim();

                updateUserProfile(eggyId, description, contact);
                dialog.dismiss();
            });
        }
    }

    /**
     * 更新用户资料
     * 发送网络请求更新用户的蛋仔昵称、自我介绍和联系方式
     * @param eggyId 蛋仔昵称
     * @param description 自我介绍
     * @param contact 联系方式
     */
    private void updateUserProfile(String eggyId, String description, String contact) {
        if (accessToken == null) {
            showToast("请先登录");
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        Gson gson = new Gson();
        UpdateProfileRequest requestModel = new UpdateProfileRequest(eggyId, description, contact);
        String jsonBody = gson.toJson(requestModel);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/users/update_profile")
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "更新资料网络请求失败", e);
                runOnUiThread(() -> showToast("网络异常"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        if (responseBody != null) {
                            // Ensure body is consumed or at least closed
                            String responseString = responseBody.string(); // Consume body
                            runOnUiThread(() -> {
                                showToast("更新成功");
                                // 更新UI和本地存储
                                userEggyId = eggyId;
                                userMy = description;
                                userFindMe = contact;
                                updateUserInfoDisplay();
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("eggyid", eggyId);
                                editor.putString("description", description);
                                editor.putString("contact", contact);
                                editor.apply();
                            });
                        } else {
                            runOnUiThread(() -> showToast("响应为空"));
                        }
                    } else {
                        runOnUiThread(() -> showToast("请求失败: " + response.code()));
                    }
                }
            }
        });
    }

    /**
     * 注销账号
     * 发送网络请求注销用户账号
     * @param password 用户密码
     */
    private void deregisterAccount(String password) {
        if (accessToken == null) {
            showToast("请先登录");
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        Gson gson = new Gson();
        DeleteAccountRequest requestModel = new DeleteAccountRequest(password);
        String jsonBody = gson.toJson(requestModel);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/account/delete")
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "注销账号网络请求失败", e);
                runOnUiThread(() -> showToast("网络异常"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            showToast("注销成功");
                            // 清除用户数据并跳转到登录界面
                            // 清除 user_prefs
                            SharedPreferences.Editor userEditor = preferences.edit();
                            userEditor.clear();
                            userEditor.apply();

                            // 清除 article_prefs (如果存在)
                            SharedPreferences articlePrefs = getSharedPreferences("article_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor articleEditor = articlePrefs.edit();
                            articleEditor.clear();
                            articleEditor.apply();
                            Intent intent = new Intent(PersonalHomePageActivity.this, LoginActivity.class);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> showToast("密码错误或请求失败"));
                    }
                }
            }
        });
    }

    /**
     * 显示昵称框选择对话框
     * 创建并显示用于选择昵称框的对话框
     */
    private void showSelectNameFrameDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_name_frame, null);
        
        // 获取RecyclerView
        androidx.recyclerview.widget.RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewNameFrames);
        
        // 设置网格布局管理器 - 正确的自适应列数计算
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dialogPadding = (int) (24 * getResources().getDisplayMetrics().density); // 对话框内边距
        int itemMargin = (int) (8 * getResources().getDisplayMetrics().density); // 项目边距
        int itemWidth = (int) (80 * getResources().getDisplayMetrics().density); // 项目宽度
        
        // 计算可用宽度（屏幕宽度减去对话框内边距）
        int availableWidth = screenWidth - (2 * dialogPadding);
        
        // 计算每个项目占用的总宽度（项目宽度 + 左右边距）
        int totalItemWidth = itemWidth + (2 * itemMargin);
        
        // 计算最大可能的列数
        int maxSpanCount = availableWidth / totalItemWidth;
        
        // 设置列数范围：最少3列，最多5列
        int spanCount = Math.max(3, maxSpanCount);
        spanCount = Math.min(spanCount, 5);
        
        androidx.recyclerview.widget.GridLayoutManager layoutManager = new androidx.recyclerview.widget.GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);
        
        // 创建昵称框列表
        List<NameFrameItem> nameFrameList = createNameFrameList();
        
        // 创建适配器
        NameFrameAdapter adapter = new NameFrameAdapter(nameFrameList);
        adapter.setUserRole(userRole);
        recyclerView.setAdapter(adapter);
        
        // 获取当前选择的昵称框
        String currentFrame = preferences.getString("selected_name_frame", "name_frame_null");
        setInitialSelection(adapter, nameFrameList, currentFrame);
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        AlertDialog dialog = builder.create();
        dialog.setView(dialogView, 0, 0, 0, 0); // 移除默认边距

        // 显示对话框
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        
        // 设置按钮点击事件
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String selectedFrame = adapter.getSelectedFrameName();
                if (selectedFrame != null) {
                    // 保存选择的昵称框
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selected_name_frame", selectedFrame);
                    editor.apply();
                    
                    showToast("昵称框已选择");
                    dialog.dismiss();
                } else {
                    showToast("请选择一个昵称框");
                }
            });
        }
    }

    /**
     * 创建昵称框列表
     * 根据drawable资源生成昵称框列表，pro昵称框只对管理员可见
     */
    private List<NameFrameItem> createNameFrameList() {
        List<NameFrameItem> list = new ArrayList<>();
        
        // 空白框 - 不使用昵称框
        list.add(new NameFrameItem("name_frame_null", "name_frame_null_round"));
        
        // 基础昵称框 (1-15)
        for (int i = 1; i <= 15; i++) {
            String frameName = "name_frame_" + i;
            String thumbnailName = frameName + "_round";
            list.add(new NameFrameItem(frameName, thumbnailName));
        }
        
        // T3系列昵称框
        String[] t3Frames = {
            "name_frame_t3_lizhitu", "name_frame_t3_magic", "name_frame_t3_muxiahui",
            "name_frame_t3_pengpengxinxiu", "name_frame_t3_sanzhounian", "name_frame_t3_sssx",
            "name_frame_t3_tangguofanmaiji", "name_frame_t3_xiangshuzhiyue", "name_frame_t3_xindongxunhao",
            "name_frame_t3_xunlonggaoshou", "name_frame_t3_yanjiugujuan", "name_frame_t3_yizhichun",
            "name_frame_t3_yuandingxinya", "name_frame_t3_zuichunyan", "name_frame_t3_zuihaodepengyou"
        };
        
        for (String frame : t3Frames) {
            list.add(new NameFrameItem(frame, frame + "_round"));
        }
        
        // T4系列昵称框
        String[] t4Frames = {
            "name_frame_t4_biji", "name_frame_t4_jiejiaohuiyi", "name_frame_t4_piaoliuxinyuan",
            "name_frame_t4_ruchangquan", "name_frame_t4_tianmibaoji", "name_frame_t4_wenminghuixiang",
            "name_frame_t4_xindongpinlv", "name_frame_t4_yonghejiunian"
        };
        
        for (String frame : t4Frames) {
            list.add(new NameFrameItem(frame, frame + "_round"));
        }
        
        // PRO昵称框 - 只对管理员可见
        if ("admin".equals(userRole)) {
            list.add(new NameFrameItem("name_frame_pro", "name_frame_pro_round"));
        }
        
        return list;
    }

    /**
     * 设置初始选择状态
     * 根据当前选择的昵称框设置适配器的选中状态
     */
    private void setInitialSelection(NameFrameAdapter adapter, List<NameFrameItem> nameFrameList, String currentFrame) {
        for (int i = 0; i < nameFrameList.size(); i++) {
            if (nameFrameList.get(i).getFrameName().equals(currentFrame)) {
                adapter.setSelectedPosition(i);
                break;
            }
        }
    }

    /**
     * 显示Toast消息
     * 在UI线程上显示Toast
     * @param message 要显示的消息
     */
    private void showToast(String message) {
        Toast.makeText(PersonalHomePageActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}