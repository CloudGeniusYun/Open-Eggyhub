package com.eggyhub.android;

import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.widget.ImageButton;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 创作者主页Activity
 * 展示创作者的详细信息和统计数据
 */
public class CreatorHomePageActivity extends BaseActivity {
    private static final String TAG = "CreatorHomePageActivity";
    private static final String API_BASE_URL = "https://eggyhub.top/api";

    // UI组件
    private TextView tvUserName;       // 用户名
    private TextView tvUserId;         // 用户ID
    private TextView tvPublishedGifts; // 发布分享数
    private TextView tvTotalLikes;     // 收获点赞数
    private TextView tvContributedCodes; // 上传分享码数
    private TextView tvClaimedCodes;   // 分享码被领取数
    private TextView tvTopClaimedGift; // 最受欢迎的分享
    private ImageView ivUserAvatar;    // 用户头像

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_homepage);

        // 初始化UI控件
        initViews();

        // 加载用户头像
        loadUserAvatar();

        // 设置返回按钮点击事件
        setupBackButton();

        // 获取并验证创作者ID
        int creatorId = getIntent().getIntExtra("id", -1);
        if (creatorId == -1) {
            Toast.makeText(this, "未获取到创作者ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 加载创作者数据
        loadCreatorData(creatorId);
    }

    /**
     * 初始化UI控件
     */
    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserId = findViewById(R.id.tv_user_id);
        tvPublishedGifts = findViewById(R.id.tv_published_gifts);
        tvTotalLikes = findViewById(R.id.tv_total_likes);
        tvContributedCodes = findViewById(R.id.tv_contributed_codes);
        tvClaimedCodes = findViewById(R.id.tv_claimed_codes);
        tvTopClaimedGift = findViewById(R.id.tv_top_claimed_gift);
        ivUserAvatar = findViewById(R.id.viewUserAvatar);
    }

    /**
     * 加载用户头像
     */
    private void loadUserAvatar() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String avatarUrl = sharedPreferences.getString("avatar", null);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.ic_avatar_null) // 设置默认头像
                    .error(R.drawable.ic_avatar_null) // 加载失败时显示默认头像
                    .into(ivUserAvatar);
        } else {
            ivUserAvatar.setImageResource(R.drawable.ic_avatar_null);
        }
    }

    /**
     * 设置返回按钮点击事件
     */
    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 加载创作者数据
     * @param creatorId 创作者ID
     */
    private void loadCreatorData(int creatorId) {
        fetchCreatorData(creatorId, new CreatorDataCallback() {
            @Override
            public void onSuccess(CreatorResponse data) {
                updateCreatorUI(data);
            }

            @Override
            public void onFailure(String errorMessage) {
                AppLogger.e(TAG, "获取创作者数据失败: " + errorMessage);
                showToast(errorMessage);
            }
        });
    }

    /**
     * 异步获取创作者数据
     * @param creatorId 创作者ID
     * @param callback 回调接口
     */
    private void fetchCreatorData(int creatorId, @NonNull CreatorDataCallback callback) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = API_BASE_URL + "/creaters?id=" + creatorId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        if (responseBody != null) {
                            String jsonString = responseBody.string();
                            AppLogger.d(TAG, "获取创作者数据成功: " + jsonString);

                            try {
                                Gson gson = new Gson();
                                CreatorResponse creatorResponse = gson.fromJson(jsonString, CreatorResponse.class);
                                callback.onSuccess(creatorResponse);
                            } catch (Exception e) {
                                callback.onFailure("数据格式错误");
                            }
                        } else {
                            callback.onFailure("响应为空");
                        }
                    } else {
                        callback.onFailure("请求失败，响应码: " + response.code());
                    }
                }
            }
        });
    }

    /**
     * 更新创作者UI数据
     * @param data 创作者数据
     */
    private void updateCreatorUI(CreatorResponse data) {
        if (data == null) return;
        
        String username = data.getUsername();
        int userId = data.getUserId();
        int publishedGifts = data.getPublishedGifts();
        String totalLikes = data.getTotalLikes();
        int contributedCodes = data.getContributedCodes();
        int claimedCodes = data.getClaimedCodes();
        String topClaimedGift = data.getTopClaimedGift();

        runOnUiThread(() -> {
            tvUserName.setText("昵称: " + (username != null ? username : ""));
            tvUserId.setText("ID: " + userId);
            tvPublishedGifts.setText(String.valueOf(publishedGifts));
            tvTotalLikes.setText(totalLikes != null ? totalLikes : "0");
            tvContributedCodes.setText(String.valueOf(contributedCodes));
            tvClaimedCodes.setText(String.valueOf(claimedCodes));
            tvTopClaimedGift.setText(topClaimedGift != null && !topClaimedGift.isEmpty() ? topClaimedGift : "暂无");
        });
    }

    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(CreatorHomePageActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * 创作者数据回调接口
     */
    private interface CreatorDataCallback {
        void onSuccess(CreatorResponse data);
        void onFailure(String errorMessage);
    }
}