package com.eggyhub.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import com.eggyhub.android.log.AppLogger;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 上传文件活动类
 * 用于处理文件选择和上传操作
 */
public class NewFileActivity extends BaseActivity {
    private static final String TAG = "NewFileActivity";
    private static final int PICK_FILE_REQUEST = 1;

    // UI组件
    private TextView mFileNameTextView; // 显示选中的文件名
    private Button mUploadButton;       // 上传按钮
    private LinearLayout mFileSelectLayout; // 文件选择区域

    // 数据变量
    private SharedPreferences mPreferences; // 共享偏好设置
    private String mAccessToken;       // 用户访问令牌
    private Uri mFileUri;              // 选中的文件URI
    private String mFileName;          // 文件名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_file);

        // 初始化UI组件
        initViews();

        // 初始化数据
        mPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        mAccessToken = SecureStorageManager.getAccessToken();

        // 设置监听器
        setupListeners();
    }

    /**
     * 初始化UI组件
     * 绑定布局中的视图元素
     */
    private void initViews() {
        mFileNameTextView = findViewById(R.id.file_name_text_view);
        mUploadButton = findViewById(R.id.upload_button);
        mFileSelectLayout = findViewById(R.id.file_select_layout);
        ImageButton backButton = findViewById(R.id.back_button);

        // 设置返回按钮点击事件
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * 设置事件监听器
     * 为按钮和其他交互元素添加点击事件处理
     */
    private void setupListeners() {
        // 文件选择区域点击事件
        mFileSelectLayout.setOnClickListener(v -> openFileChooser());

        // 上传按钮点击事件
        mUploadButton.setOnClickListener(v -> uploadFile(mFileUri));
    }

    /**
     * 打开文件选择器
     * 允许用户选择要上传的文件
     */
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // 选择所有类型的文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    /**
     * 处理活动结果
     * 在这里处理文件选择的结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mFileUri = data.getData();
            mFileName = getFileName(mFileUri);
            mFileNameTextView.setText(mFileName);
            Toast.makeText(this, "文件已选择", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 上传文件
     * @param fileUri 要上传的文件URI
     */
    private void uploadFile(Uri fileUri) {
        if (fileUri == null) {
            Toast.makeText(this, "请先选择文件", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAccessToken == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 读取文件内容
            byte[] fileBytes = readFileBytes(fileUri);
            if (fileBytes == null) {
                Toast.makeText(this, "文件读取失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建请求体
            RequestBody requestBody = createRequestBody(fileBytes);
            if (requestBody == null) {
                Toast.makeText(this, "请求创建失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建请求
            Request request = createUploadRequest(requestBody);

            // 执行请求
            executeUploadRequest(request);

        } catch (Exception e) {
            AppLogger.e(TAG, "上传异常: " + e.getMessage(), e);
            Toast.makeText(this, "上传异常", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 读取文件字节
     * @param fileUri 文件URI
     * @return 文件字节数组
     */
    private byte[] readFileBytes(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            return getBytes(inputStream);
        } catch (Exception e) {
            AppLogger.e(TAG, "读取文件失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从输入流获取字节数组
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException 异常
     */
    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    /**
     * 创建请求体
     * @param fileBytes 文件字节数组
     * @return 请求体
     */
    private RequestBody createRequestBody(byte[] fileBytes) {
        try {
            return new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", mFileName,
                            RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes))
                    .build();
        } catch (Exception e) {
            AppLogger.e(TAG, "创建请求体失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建上传请求
     * @param requestBody 请求体
     * @return 请求对象
     */
    private Request createUploadRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url("https://eggyhub.top/api/repos/upload")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Connection", "keep-alive")
                .build();
    }

    /**
     * 执行上传请求
     * @param request 请求对象
     */
    private void executeUploadRequest(Request request) {
        // 使用全局 OkHttpClient（包含代理拦截器）
        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                AppLogger.e(TAG, "上传失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    String errorMsg = "上传失败";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("timeout") || e.getMessage().contains("timed out")) {
                            errorMsg = "上传超时，请检查网络连接或尝试压缩图片";
                        } else if (e.getMessage().contains("Connection")) {
                            errorMsg = "网络连接异常，请检查网络设置";
                        }
                    }
                    Toast.makeText(NewFileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(NewFileActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    AppLogger.e(TAG, "上传失败，响应码: " + response.code());
                    runOnUiThread(() -> Toast.makeText(NewFileActivity.this, "文件类型不支持或其他错误", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * 获取文件名
     * @param uri 文件URI
     * @return 文件名
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (scheme == null || scheme.equals("content")) {
            String[] projection = {MediaStore.Files.FileColumns.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    fileName = cursor.getString(columnIndex);
                }
            }
        } else if (scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        }

        // 如果文件名获取失败，生成一个默认文件名
        if (fileName == null || fileName.isEmpty()) {
            fileName = "file_" + System.currentTimeMillis() + ".dat";
        }

        return fileName;
    }
}
