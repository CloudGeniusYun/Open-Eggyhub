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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback;
import okhttp3.ResponseBody;
import com.google.gson.JsonObject;

import com.eggyhub.android.utils.OkHttpClientFactory;
import androidx.annotation.NonNull;

/**
 * 发布分享码活动类
 * 用于创建和发布新的分享码
 */
public class CodeupActivity extends BaseActivity {
    private static final String TAG = "CodeupActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI组件
    private TextView mFileNameTextView;     // 显示选中的文件名
    private EditText mNameEditText;         // 分享码名称输入框
    private EditText mFirstCodeEditText;    // 第一个分享码输入框
    private EditText mDescriptionEditText;  // 内容描述输入框
    private Button mPublishButton;          // 发布按钮
    private LinearLayout mImageSelectLayout;// 图片选择区域
    private SeekBar mEggCodeQuantitySeekBar; // 蛋码碎片数量滑动条
    private TextView mEggCodeQuantityTextView; // 蛋码碎片数量显示

    // 数据变量
    private SharedPreferences mPreferences; // 共享偏好设置
    private static final String PREF_MIN_PRICING = "min_pricing";
    private static final String PREF_MAX_PRICING = "max_pricing";
    private String mAccessToken;            // 用户访问令牌
    private Uri mImageUri;                  // 选中的图片URI
    private String mFileName;               // 文件名
    private String mCoverUrl;               // 封面图片URL
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_sharecode);

        // 初始化UI组件
        initViews();

        // 初始化数据
        mPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        mAccessToken = SecureStorageManager.getAccessToken();

        // 获取动态定价范围
        fetchPricingRange();

        // 设置监听器
        setupListeners();
    }

    /**
     * 从API获取动态定价范围并更新SeekBar
     */
    private void fetchPricingRange() {
        // 优先从本地缓存加载
        int cachedMin = mPreferences.getInt(PREF_MIN_PRICING, 40); // 默认 40
        int cachedMax = mPreferences.getInt(PREF_MAX_PRICING, 80); // 默认 80
        updateSeekBarRange(cachedMin, cachedMax);
        AppLogger.d(TAG, "从缓存加载定价范围: " + cachedMin + " - " + cachedMax);

        if (mAccessToken == null) return;

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/creator/pricing")
                .get()
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "获取定价范围失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        String json = responseBody.string();
                        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                        if (jsonObject.has("max_pricing") && jsonObject.has("min_pricing")) {
                            int maxPricing = jsonObject.get("max_pricing").getAsInt();
                            int minPricing = jsonObject.get("min_pricing").getAsInt();
                            
                            // 保存到本地缓存
                            mPreferences.edit()
                                    .putInt(PREF_MIN_PRICING, minPricing)
                                    .putInt(PREF_MAX_PRICING, maxPricing)
                                    .apply();

                            runOnUiThread(() -> {
                                updateSeekBarRange(minPricing, maxPricing);
                                AppLogger.d(TAG, "定价范围已从API更新并保存: " + minPricing + " - " + maxPricing);
                            });
                        }
                    }
                } catch (Exception e) {
                    AppLogger.e(TAG, "解析定价范围异常: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 更新SeekBar的范围和进度
     */
    private void updateSeekBarRange(int min, int max) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mEggCodeQuantitySeekBar.setMin(min);
        }
        mEggCodeQuantitySeekBar.setMax(max);

        // 如果当前进度不在新范围内，进行调整
        int currentProgress = mEggCodeQuantitySeekBar.getProgress();
        if (currentProgress < min) {
            mEggCodeQuantitySeekBar.setProgress(min);
            mEggCodeQuantityTextView.setText(String.valueOf(min));
        } else if (currentProgress > max) {
            mEggCodeQuantitySeekBar.setProgress(max);
            mEggCodeQuantityTextView.setText(String.valueOf(max));
        }
    }

    /**
     * 初始化UI组件
     * 绑定布局中的视图元素
     */
    private void initViews() {
        mFileNameTextView = findViewById(R.id.file_name_text_view);
        mNameEditText = findViewById(R.id.sharecode_name_edit_text);
        mFirstCodeEditText = findViewById(R.id.first_code_edit_text);
        mDescriptionEditText = findViewById(R.id.description_edit_text);
        mPublishButton = findViewById(R.id.publish_button);
        mImageSelectLayout = findViewById(R.id.image_select_layout);
        mEggCodeQuantitySeekBar = findViewById(R.id.egg_code_quantity_seekbar);
        mEggCodeQuantityTextView = findViewById(R.id.egg_code_quantity_text);

        // 初始化返回按钮
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * 设置事件监听器
     * 为按钮和其他交互元素添加点击事件处理
     */
    private void setupListeners() {
        // 图片选择区域点击事件
        mImageSelectLayout.setOnClickListener(v -> openImageChooser());

        // 发布按钮点击事件
        mPublishButton.setOnClickListener(v -> {
            mPublishButton.setEnabled(false); // 禁用按钮防止重复点击
            uploadImage(mImageUri);
        });

        // 设置蛋码碎片数量滑动条监听器
        mEggCodeQuantitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEggCodeQuantityTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不做任何操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 不做任何操作
            }
        });
    }

    /**
     * 打开图片选择器
     * 允许用户选择要上传的图片
     */
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * 处理图片选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mImageUri = data.getData();
            mFileName = getFileName(mImageUri);
            mFileNameTextView.setText(mFileName);
            Toast.makeText(this, "图片已选择", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 上传图片和分享码信息
     * @param imageUri 图片URI
     */
    private void uploadImage(Uri imageUri) {
        // 禁用按钮防止重复点击
        mPublishButton.setEnabled(false);
        try {
            // 获取输入数据
            String name = mNameEditText.getText().toString().trim();
            String firstCode = mFirstCodeEditText.getText().toString().trim();
            String description = mDescriptionEditText.getText().toString().trim();

            // 验证输入
            if (!validateInput(name, firstCode, description, imageUri)) {
                return;
            }

            // 创建JSON对象
            String infoJson = createInfoJson(name, description, firstCode);
            if (infoJson == null) {
                Toast.makeText(this, "创建数据失败", Toast.LENGTH_SHORT).show();
            mPublishButton.setEnabled(true); // 重新启用按钮
            return;
            }

            // 读取图片字节
            byte[] imageBytes = readImageBytes(imageUri);
            if (imageBytes == null) {
                Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
                mPublishButton.setEnabled(true); // 重新启用按钮
                return;
            }

            // 创建请求体
            RequestBody requestBody = createRequestBody(imageBytes, infoJson);

            // 创建请求
            Request request = createUploadRequest(requestBody);

            // 执行请求
            executeUploadRequest(request);

        } catch (Exception e) {
            AppLogger.e(TAG, "上传异常: " + e.getMessage(), e);
            Toast.makeText(this, "上传异常", Toast.LENGTH_SHORT).show();
            mPublishButton.setEnabled(true); // 发生异常时重新启用按钮
        }
    }

    /**
     * 验证输入数据
     * @param name 分享码名称
     * @param firstCode 第一个分享码
     * @param description 描述
     * @param imageUri 图片URI
     * @return 验证是否通过
     */
    private boolean validateInput(String name, String firstCode, String description, Uri imageUri) {
        if (name.isEmpty() || description.isEmpty() || firstCode.isEmpty() || imageUri == null) {
            Toast.makeText(this, "数据不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }

        String validationResult = validateFirstCode(firstCode);
        if (!validationResult.equals("1")) {
            Toast.makeText(this, validationResult, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * 创建信息JSON对象
     * @param name 分享码名称
     * @param description 描述
     * @param firstCode 第一个分享码
     * @return JSON字符串
     */
    private String createInfoJson(String name, String description, String firstCode) {
        CodeInfo info = new CodeInfo(name, description, firstCode, String.valueOf(mEggCodeQuantitySeekBar.getProgress()));
        return gson.toJson(info);
    }

    /**
     * 读取图片字节
     * @param imageUri 图片URI
     * @return 图片字节数组
     */
    private byte[] readImageBytes(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            return getBytes(inputStream);
        } catch (Exception e) {
            AppLogger.e(TAG, "读取图片失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建请求体
     * @param imageBytes 图片字节数组
     * @param infoJson 信息JSON字符串
     * @return 请求体
     */
    private RequestBody createRequestBody(byte[] imageBytes, String infoJson) {
        return new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", mFileName,
                        RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                .addFormDataPart(
                        "info",
                        null,
                        RequestBody.create(
                                MediaType.parse("application/json"),
                                infoJson
                        )
                )
                .build();
    }

    /**
     * 创建上传请求
     * @param requestBody 请求体
     * @return 请求对象
     */
    private Request createUploadRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url("https://eggyhub.top/api/gifts/sub")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();
    }

    /**
     * 执行上传请求
     * @param request 请求对象
     */
    private void executeUploadRequest(Request request) {
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
                    Toast.makeText(CodeupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    mPublishButton.setEnabled(true);
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        String bodyString = body.string();
                        handleSuccessResponse(bodyString);
                    } else {
                        runOnUiThread(() -> Toast.makeText(CodeupActivity.this, "上传失败", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /**
     * 处理成功响应
     * @param jsonString 响应体内容
     */
    private void handleSuccessResponse(String jsonString) {
        try {
            CodeupResponse codeupResponse = gson.fromJson(jsonString, CodeupResponse.class);
            
            if (codeupResponse != null) {
                mCoverUrl = codeupResponse.getCover();
                int giftId = codeupResponse.getId();

                if (mCoverUrl != null && !mCoverUrl.isEmpty()) {
                    // 调用新的API
                    sendUpdateGiftRequest(giftId, mFirstCodeEditText.getText().toString().trim());
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(CodeupActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } else {
                throw new IOException("解析响应为空");
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            AppLogger.e(TAG, "解析响应失败: " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(CodeupActivity.this, "解析异常", Toast.LENGTH_SHORT).show();
                mPublishButton.setEnabled(true); // 发生异常时重新启用按钮
            });
        }
    }

    /**
     * 发送更新礼品请求 (gifts/update)
     * @param giftId 礼品ID
     * @param code 分享码
     */
    private void sendUpdateGiftRequest(int giftId, String code) {
        UploadCodeRequest uploadRequest = new UploadCodeRequest(code, String.valueOf(giftId));
        String jsonBody = gson.toJson(uploadRequest);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/gifts/update")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .post(body)
                .build();

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                AppLogger.e(TAG, "更新分享码失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(CodeupActivity.this, "更新分享码失败", Toast.LENGTH_SHORT).show();
                    mPublishButton.setEnabled(true); // 重新启用按钮
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(CodeupActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        AppLogger.e(TAG, "更新分享码失败, 响应码: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(CodeupActivity.this, "更新分享码失败", Toast.LENGTH_SHORT).show();
                            mPublishButton.setEnabled(true); // 重新启用按钮
                        });
                    }
                }
            }
        });
    }

    /**
     * 将输入流转换为字节数组
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
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
     * 从URI获取文件名
     * @param uri 图片URI
     * @return 文件名
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (scheme == null || scheme.equals("content")) {
            String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    fileName = cursor.getString(columnIndex);
                }
            }
        } else if (scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        }

        // 如果文件名获取失败，生成一个默认文件名
        if (fileName == null || fileName.isEmpty()) {
            fileName = "cover_" + System.currentTimeMillis() + ".jpg";
        }

        return fileName;
    }

    /**
     * 验证第一个分享码
     * @param firstCode 分享码
     * @return 验证结果，1表示验证通过，其他为错误信息
     */
    public static String validateFirstCode(String firstCode) {
        if (firstCode == null || firstCode.isEmpty()) {
            return "第一个分享码不能为空";
        }

        // 检查长度
        if (firstCode.length() != 13) {
            return "分享码长度必须为13位";
        }

        // 检查前缀
        String prefix = firstCode.substring(0, 2);
        if (!prefix.equalsIgnoreCase("2y")) {
            return "分享码必须以2y开头";
        }

        // 检查后缀是否只包含字母和数字
        String suffix = firstCode.substring(2);
        for (char c : suffix.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return "分享码只能包含字母和数字";
            }
        }

        return "1";
    }
}

