package com.eggyhub.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 视频管理Activity
 * 用于显示视频详情、更改视频分类和删除视频
 */
public class MangerVideoActivity extends BaseActivity {
    private static final String TAG = "MangerVideoActivity";
    private static final String BASE_URL = "https://eggyhub.top/api";

    // 请求参数
    private int videoId;
    private int videoGroupId;
    private String videoDescription;
    private String videoName;
    private String videoBv;
    private String videoCoverUrl;

    // UI组件
    private TextView videoNameText;
    private TextView videoDescriptionText;
    private TextView videoBvText;
    private TextView videoGroupText;
    private CardView changeGroupCard;
    private CardView deleteVideoCard;
    private ImageView videoCoverImage;

    // 数据和配置
    private SharedPreferences preferences;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_video); // 更新为更规范的布局ID

        // 初始化视图
        initViews();

        // 初始化数据
        initData();

        // 设置监听器
        setupListeners();

        // 加载视频封面
        loadVideoCover();

        // 设置视频分类文本
        setVideoGroupText(videoGroupId);
    }

    /**
     * 初始化视图组件
     * 使用更规范的XML布局ID
     */
    private void initViews() {
        videoNameText = findViewById(R.id.text_video_name);
        videoDescriptionText = findViewById(R.id.text_video_description);
        videoBvText = findViewById(R.id.text_video_bv);
        videoGroupText = findViewById(R.id.text_video_group);
        changeGroupCard = findViewById(R.id.card_change_group);
        deleteVideoCard = findViewById(R.id.card_delete_video);
        videoCoverImage = findViewById(R.id.image_video_cover);

        // 初始化返回按钮
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * 初始化数据
     * 从Intent中获取视频信息
     */
    private void initData() {
        Intent intent = getIntent();
        videoId = intent.getIntExtra("id", 0);
        videoGroupId = intent.getIntExtra("gr", 0);
        videoCoverUrl = intent.getStringExtra("cover");

        // 获取 SharedPreferences 和 accessToken
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        accessToken = SecureStorageManager.getAccessToken();

        // 处理中文字符编码
        try {
            videoDescription = new String(intent.getStringExtra("ds").getBytes(), "UTF-8");
            videoName = new String(intent.getStringExtra("name").getBytes(), "UTF-8");
            videoBv = new String(intent.getStringExtra("bv").getBytes(), "UTF-8");

            // 设置文本内容
            videoDescriptionText.setText("描述：" + videoDescription);
            videoBvText.setText("BV：" + videoBv);
            videoNameText.setText(videoName);
        } catch (UnsupportedEncodingException e) {
            AppLogger.e(TAG, "字符串编码转换失败: " + e.getMessage());
            Toast.makeText(this, "数据加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 更改分类按钮点击事件
        changeGroupCard.setOnClickListener(v -> showGroupSelectionDialog());

        // 删除视频按钮点击事件
        deleteVideoCard.setOnClickListener(v -> deleteVideo());
    }

    /**
     * 加载视频封面
     */
    private void loadVideoCover() {
        if (videoCoverUrl != null && !videoCoverUrl.isEmpty()) {
            Glide.with(this)
                    .load(videoCoverUrl)
                    .into(videoCoverImage);
        } else {
            AppLogger.w(TAG, "视频封面URL为空");
            // 设置默认封面
            videoCoverImage.setImageResource(R.drawable.video);
        }
    }

    /**
     * 显示分类选择对话框
     */
    private void showGroupSelectionDialog() {
        final String[] groupOptions = {"默认分类", "蛋码基础", "蛋码技能", "工坊教程"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择视频分类");
        builder.setItems(groupOptions, (dialog, which) -> {
            int selectedGroupId = which + 1; // 分类ID从1开始
            updateVideoGroup(selectedGroupId);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 更新视频分类
     * @param groupId 新的分类ID
     */
    private void updateVideoGroup(int groupId) {
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BASE_URL + "/alter_videogroup?id=" + videoId + "&grid=" + groupId;
        AppLogger.d(TAG, "更新视频分类请求URL: " + url);

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "更新视频分类失败: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MangerVideoActivity.this, "更新分类失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    AppLogger.d(TAG, "更新视频分类成功");
                    videoGroupId = groupId; // 更新本地分类ID
                    runOnUiThread(() -> {
                        Toast.makeText(MangerVideoActivity.this, "更新分类成功", Toast.LENGTH_SHORT).show();
                        setVideoGroupText(groupId); // 更新UI显示
                    });
                } else {
                    AppLogger.e(TAG, "更新视频分类失败，响应码: " + response.code());
                    runOnUiThread(() -> Toast.makeText(MangerVideoActivity.this, "更新分类失败，请重试", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * 删除视频
     */
    private void deleteVideo() {
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示确认对话框
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除该视频吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 执行删除操作
                    performDeleteVideo();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 执行删除视频请求
     */
    private void performDeleteVideo() {
        String url = BASE_URL + "/video_del?id=" + videoId;
        AppLogger.d(TAG, "删除视频请求URL: " + url);

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "删除视频失败: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MangerVideoActivity.this, "删除视频失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    AppLogger.d(TAG, "删除视频成功");
                    runOnUiThread(() -> {
                        Toast.makeText(MangerVideoActivity.this, "删除视频成功", Toast.LENGTH_SHORT).show();
                        finish(); // 关闭当前Activity
                    });
                } else {
                    AppLogger.e(TAG, "删除视频失败，响应码: " + response.code());
                    runOnUiThread(() -> Toast.makeText(MangerVideoActivity.this, "删除视频失败，请联系管理员", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * 设置视频分类文本
     * @param groupId 分类ID
     */
    private void setVideoGroupText(int groupId) {
        String groupName;
        switch (groupId) {
            case 1:
                groupName = "默认分类";
                break;
            case 2:
                groupName = "蛋码基础";
                break;
            case 3:
                groupName = "蛋码技能";
                break;
            case 4:
                groupName = "工坊教程";
                break;
            default:
                groupName = "未知分类";
                AppLogger.w(TAG, "无效的视频分类ID: " + groupId);
                break;
        }
        videoGroupText.setText("分类：" + groupName);
    }
}