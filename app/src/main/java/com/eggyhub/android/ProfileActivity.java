package com.eggyhub.android;

import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.Dialog;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import com.eggyhub.android.StrokeTextView;
import com.eggyhub.android.log.AppLogger;
import androidx.cardview.widget.CardView;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;
import com.eggyhub.android.utils.ProxyConfig;

/**
 * 用户个人资料页面
 * 展示用户信息并提供各种功能入口
 */
public class ProfileActivity extends BaseActivity_false {
    LinearLayout openhp;
    SharedPreferences preferences;

    LinearLayout cardManger;
    LinearLayout cardSettingsPage;
    
    private TextView tvPublishedShares;
    private TextView tvLikes;
    private TextView tvStats;
    private TextView textViewDescription;

    private static final String LAST_FETCH_TIME_KEY = "lastFetchTime";
    private static final long CACHE_DURATION = 60 * 60 * 1000; // 1 hour
    private ImageButton btnNetworkSwitch;
    private void checkTutorialStatus() {
        TutorialManager manager = TutorialManager.getInstance(this);
        if (manager.isTutorialRunning()) {
            String currentTutorial = manager.getCurrentTutorial();
            int stepIndex = manager.getStepIndex();
            
            if (TutorialManager.TUTORIAL_PROFILE.equals(currentTutorial) && stepIndex == 1) {
                // 高亮第一个统计项
                View layoutStats = findViewById(R.id.layoutStats);
                if (layoutStats != null) {
                    layoutStats.post(() -> {
                        GuideHelper.show(this, layoutStats, "第二步：这里展示了你的发布数量、获赞和统计数据", false, () -> {
                            manager.finishTutorial();
                        });
                    });
                }
            } else if (TutorialManager.TUTORIAL_SUPPLEMENT_CODE.equals(currentTutorial) && stepIndex == 1) {
                View cardManger = findViewById(R.id.cardManger);
                if (cardManger != null) {
                    cardManger.post(() -> {
                        GuideHelper.show(this, cardManger, "第二步：点击“内容管理”查看你发布的所有资源", true, () -> {
                            manager.nextStep(); // 1 -> 2
                        });
                    });
                }
            }
        }
    }

    /**
     * Activity创建时调用的方法
     * 初始化UI组件、设置用户信息和功能菜单
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
         preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        openhp = findViewById(R.id.openhp);
        openhp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, PersonalHomePageActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        cardManger = findViewById(R.id.cardManger);
        cardManger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, MangerActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);}}
        );

        cardSettingsPage = findViewById(R.id.cardSettingsPage);
        cardSettingsPage.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 初始化统计视图
        tvPublishedShares = findViewById(R.id.tvPublishedShares);
        tvLikes = findViewById(R.id.tvLikes);
        tvStats = findViewById(R.id.tvStats);
        textViewDescription = findViewById(R.id.textViewDescription);

        // 初始化网络切换按钮
        btnNetworkSwitch = findViewById(R.id.btnNetworkSwitch);
        updateNetworkButtonIcon(); // 更新按钮图标
        btnNetworkSwitch.setOnClickListener(v -> {
            showNetworkSwitchDialog(); // 显示网络切换对话框
        });

        // 设置用户信息
        setupUserInfo();

        // 设置功能菜单点击事件
        setupMenuItems();

        // 设置底部导航
        setupBottomNavigation();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    /**
     * 设置用户信息
     * 从SharedPreferences获取用户信息并显示
     */
    private void setupUserInfo() {
        // 从SharedPreferences获取用户信息
        int id = preferences.getInt("id", -1);
        String username = preferences.getString("username", "");
        String avatar = preferences.getString("avatar", null);
        String sponser = preferences.getString("sponser", "0");
        String role = preferences.getString("role", "user");
        String description = preferences.getString("description", "这个人很懒，什么也没有留下！");

        // 获取缓存的统计数据
        String publishedGifts = preferences.getString("cache_published_gifts", "0");
        String totalLikes = preferences.getString("cache_total_likes", "0");
        String contributedCodes = preferences.getString("cache_contributed_codes", "0");

        TextView textViewUserId = findViewById(R.id.textViewUserId);
        ShapeableImageView imageViewAvatar = findViewById(R.id.viewUserAvatar);
        android.widget.ImageView imageViewNameFrame = findViewById(R.id.imageViewNameFrame);
        android.widget.ImageView imageViewRoleIconOutside = findViewById(R.id.imageViewRoleIconOutside);
        StrokeTextView textViewUserName = findViewById(R.id.textViewUserName);

        textViewUserName.setText(formatUsername(username));
        textViewUserId.setText("ID: " + (id != -1 ? String.valueOf(id) : ""));
        
        if (textViewDescription != null) {
            textViewDescription.setText(description);
        }

        // 更新统计视图（从缓存加载）
        if (tvPublishedShares != null) tvPublishedShares.setText(publishedGifts);
        if (tvLikes != null) tvLikes.setText(totalLikes);
        if (tvStats != null) tvStats.setText(contributedCodes);

        // 设置自定义字体
        try {
            Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/huawenyuanti.ttf");
            // 使用更细的字体粗细 (300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textViewUserName.setTypeface(Typeface.create(typeface, 600, false)); // 300是比标准更细的粗细
            } else {
                // 对于低版本Android，使用标准粗细
                textViewUserName.setTypeface(typeface, Typeface.NORMAL);
            }
        } catch (Exception e) {
            // 如果字体加载失败，使用默认字体
            AppLogger.e("ProfileActivity", "加载自定义字体失败: " + e.getMessage());
        }

        // 设置真正的硬描边效果 - 白字黑边
        textViewUserName.setStrokeColor(Color.BLACK);
        textViewUserName.setStrokeWidth(6f); // 加粗到 6 像素的黑色描边

        // 检查是否选择了昵称框
        String selectedNameFrame = preferences.getString("selected_name_frame", "name_frame_null");
        boolean hasNameFrame = selectedNameFrame != null && !selectedNameFrame.isEmpty() && 
                              !"name_frame_null".equals(selectedNameFrame);

        if ("user".equals(role)){// 根据sponser值设置用户名样式
            if (!"0.0".equals(sponser)) {
                textViewUserName.setTextColor(Color.parseColor("#fac75e")); // 设置VIP颜色
            
                // 添加VIP图标 - 根据是否有昵称框调整位置
                Drawable vipIcon = getResources().getDrawable(R.drawable.vip);
                int vipIconSize = (int) (25 * getResources().getDisplayMetrics().density);
                
                if (hasNameFrame) {
                    // 如果有昵称框，图标放在昵称框外面（垂直居中）
                    vipIcon.setBounds(0, 0, vipIconSize, vipIconSize);
                    textViewUserName.setCompoundDrawables(null, null, null, null);
                    imageViewRoleIconOutside.setImageDrawable(vipIcon);
                    imageViewRoleIconOutside.setVisibility(View.VISIBLE);
                } else {
                    // 如果没有昵称框，图标放在文本后面（向上偏移）
                    int verticalOffset = (int) (0 * getResources().getDisplayMetrics().density); // 向上偏移2dp
                    vipIcon.setBounds(0, verticalOffset, vipIconSize, vipIconSize + verticalOffset);
                    textViewUserName.setCompoundDrawables(null, null, vipIcon, null);
                    imageViewRoleIconOutside.setVisibility(View.GONE);
                }
                textViewUserName.setCompoundDrawablePadding(8); // 恢复原来的间距
            }
        }else{
            if ("admin".equals(role)) {
            textViewUserName.setTextColor(Color.parseColor("#ff0000")); // 设置管理员颜色
            
            // 添加Guan图标 - 根据是否有昵称框调整位置
            Drawable guanIcon = getResources().getDrawable(R.drawable.guan);
            int guanIconSize = (int) (25 * getResources().getDisplayMetrics().density);
            
            if (hasNameFrame) {
                // 如果有昵称框，图标放在昵称框外面（垂直居中）
                guanIcon.setBounds(0, 0, guanIconSize, guanIconSize);
                textViewUserName.setCompoundDrawables(null, null, null, null);
                imageViewRoleIconOutside.setImageDrawable(guanIcon);
                imageViewRoleIconOutside.setVisibility(View.VISIBLE);
            } else {
                // 如果没有昵称框，图标放在文本后面（向上偏移）
                int verticalOffset = (int) (0 * getResources().getDisplayMetrics().density); // 向上偏移2dp
                guanIcon.setBounds(0, verticalOffset, guanIconSize, guanIconSize + verticalOffset);
                textViewUserName.setCompoundDrawables(null, null, guanIcon, null);
                imageViewRoleIconOutside.setVisibility(View.GONE);
            }
            textViewUserName.setCompoundDrawablePadding(6); // 恢复原来的间距
            }
        }

        // 加载昵称框
        String userRole = preferences.getString("role", "user");
        
        // 检查是否为pro昵称框且用户不是管理员
        boolean isProFrame = "name_frame_pro".equals(selectedNameFrame);
        boolean isAdmin = "admin".equals(userRole);
        
        // 如果选择的是空白框或默认框，不显示昵称框
        boolean shouldShowFrame = selectedNameFrame != null && !selectedNameFrame.isEmpty() && 
                                  !"name_frame_null".equals(selectedNameFrame) && 
                                  !(isProFrame && !isAdmin);
        
        if (shouldShowFrame) {
            int frameResourceId = getResources().getIdentifier(selectedNameFrame, "drawable", getPackageName());
            if (frameResourceId != 0) {
                imageViewNameFrame.setImageResource(frameResourceId);
                imageViewNameFrame.setVisibility(View.VISIBLE);
            } else {
                imageViewNameFrame.setVisibility(View.GONE);
            }
        } else {
            imageViewNameFrame.setVisibility(View.GONE);
        }

        // 使用Glide加载图片前检查Activity是否已销毁
        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_arrow_right) // 加载中显示的图片
                    .error(R.drawable.ic_arrow_right)       // 加载失败显示的图片
                    .into(imageViewAvatar);
        }
    }

    /**
     * 设置功能菜单点击事件
     * 初始化各个功能菜单并设置点击事件处理
     */
    private void setupMenuItems() {
        // 加入官方QQ群
        LinearLayout cardFileManage = findViewById(R.id.cardFileManage);
        cardFileManage.setOnClickListener(v -> {
            if (!joinQQGroup("Hu3GvTDPlrBDWLB-4S_jFdcrG4Jxd3-t")) {
                Toast.makeText(this, "未安装QQ或版本过低", Toast.LENGTH_SHORT).show();
            }
        });

        // 联系管理员
        findViewById(R.id.cardContactAdmin).setOnClickListener(v -> showContactAdminDialog());
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

    private void showContactAdminDialog() {
        AppLogger.d("ProfileActivity", "Showing ContactAdminDialog");
        
        // 使用自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contact_admin, null);
        EditText input = dialogView.findViewById(R.id.edit_text_message);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 设置背景透明，以便使用自定义背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 设置按钮点击事件
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                sendToAdmin(message);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void sendToAdmin(String message) {
        AppLogger.i("ProfileActivity", "Sending message to admin: " + message);
        String token = SecureStorageManager.decryptAndRetrieve("access_token");
        if (token == null) token = "";

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Gson gson = new Gson();
        java.util.HashMap<String, String> data = new java.util.HashMap<>();
        data.put("text", message + "      ——移动端用户消息");
        String json = gson.toJson(data);
        MediaType JSON = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(JSON, json);
        
        Request request = new Request.Builder()
                 .url("https://eggyhub.top/api/tell")
                 .addHeader("Authorization", "Bearer " + token)
                 .post(body)
                 .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e("ProfileActivity", "Failed to send message: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        AppLogger.i("ProfileActivity", "Message sent successfully");
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "消息已发送", Toast.LENGTH_SHORT).show());
                    } else {
                        AppLogger.w("ProfileActivity", "Failed to send message: " + response.message());
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "发送失败: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /**
     * 当 Activity 恢复时调用
     * 重新加载用户信息，确保头像已更新
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkTutorialStatus();
        setupUserInfo();
        
        int id = preferences.getInt("id", -1);
        if (id != -1) {
            // 检查是否需要刷新数据（1 小时内不重复获取）
            long lastFetchTime = preferences.getLong(LAST_FETCH_TIME_KEY, 0);
            long currentTime = System.currentTimeMillis();
            boolean shouldFetch = (currentTime - lastFetchTime) >= CACHE_DURATION;
            
            if (shouldFetch) {
                AppLogger.d("ProfileActivity", "刷新用户数据");
                fetchCreatorData(id);
                fetchUserProfile(id);
                // 更新最后刷新时间
                preferences.edit().putLong(LAST_FETCH_TIME_KEY, currentTime).apply();
            } else {
                AppLogger.d("ProfileActivity", "使用缓存数据，距离上次刷新：" + (currentTime - lastFetchTime) / 1000 + "秒");
            }
        }

        Switch switchQuickReplenish = findViewById(R.id.switchQuickReplenish);
        if (switchQuickReplenish != null) {
            boolean isQuickReplenishEnabled = preferences.getBoolean("quick_replenish_enabled", false);
            switchQuickReplenish.setChecked(isQuickReplenishEnabled);

            if (!isQuickReplenishEnabled) {
                // 如果SharedPreferences中的开关状态为关闭，则停止所有服务
                stopService(new Intent(ProfileActivity.this, OverlayService.class).setAction("ACTION_HIDE_SECOND_OVERLAY"));
                stopService(new Intent(ProfileActivity.this, FirstOverlayService.class));
                stopService(new Intent(ProfileActivity.this, MinimizeIconService.class));
            } else {
                // 如果SharedPreferences中的开关状态为打开，但服务未运行，则启动服务
                boolean isOverlayServiceRunning = isServiceRunning(OverlayService.class);
                boolean isFirstOverlayServiceRunning = isServiceRunning(FirstOverlayService.class);
                boolean isMinimizeIconServiceRunning = isServiceRunning(MinimizeIconService.class);

                if (!isOverlayServiceRunning) {
                    Intent serviceIntent = new Intent(ProfileActivity.this, OverlayService.class);
                    serviceIntent.setAction("ACTION_SHOW_SECOND_OVERLAY");
                    startService(serviceIntent);
                }
                if (!isFirstOverlayServiceRunning) {
                    startService(new Intent(ProfileActivity.this, FirstOverlayService.class));
                }
                if (!isMinimizeIconServiceRunning) {
                    startService(new Intent(ProfileActivity.this, MinimizeIconService.class));
                }
            }
        }
    }

    private void setupBottomNavigation() {
        // 首页按钮
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 清除栈顶所有Activity
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // 设置过渡动画
            });
        }

        // 发布按钮
        View navPublish = findViewById(R.id.navPublish);
        if (navPublish != null) {
            navPublish.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, PublishActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // 任务按钮
        View navTask = findViewById(R.id.navTask);
        if (navTask != null) {
            navTask.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, TaskActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // 我的按钮（当前页面）
        View navMy = findViewById(R.id.navProfile);
        if (navMy != null) {
            navMy.setSelected(true); // 设置为选中状态
        }
    }

    private void fetchUserProfile(int userId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/users/profile?id=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e("ProfileActivity", "获取用户资料失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        String bodyString = responseBody.string();
                        try {
                            Gson gson = new Gson();
                            UserProfileResponse userProfileResponse = gson.fromJson(bodyString, UserProfileResponse.class);
                            
                            if (userProfileResponse != null && userProfileResponse.isSuccess()) {
                                UserProfileResponse.Data data = userProfileResponse.getData();
                                if (data != null) {
                                    String avatar = data.getAvatar();
                                    String description = data.getDescription();
                                    String contact = data.getContact();
                                    String eggyid = data.getEggyid();

                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("avatar", avatar);
                                    editor.putString("description", description);
                                    editor.putString("contact", contact);
                                    editor.putString("eggyid", eggyid);
                                    editor.apply();

                                    runOnUiThread(() -> setupUserInfo());
                                }
                            }
                        } catch (Exception e) {
                            AppLogger.e("ProfileActivity", "解析用户资料失败: " + e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private void fetchCreatorData(int creatorId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/creaters?id=" + creatorId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e("ProfileActivity", "获取创作者数据失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String jsonString = responseBody.string();
                        try {
                            Gson gson = new Gson();
                            CreatorResponse creatorResponse = gson.fromJson(jsonString, CreatorResponse.class);
                            
                            if (creatorResponse != null) {
                                final int publishedGifts = creatorResponse.getPublishedGifts();
                                final String totalLikes = creatorResponse.getTotalLikes();
                                final int contributedCodes = creatorResponse.getContributedCodes();

                            // 持久化存储到本地
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("cache_published_gifts", String.valueOf(publishedGifts));
                            editor.putString("cache_total_likes", totalLikes);
                            editor.putString("cache_contributed_codes", String.valueOf(contributedCodes));
                            editor.apply();

                            runOnUiThread(() -> {
                                if (tvPublishedShares != null) tvPublishedShares.setText(String.valueOf(publishedGifts));
                                if (tvLikes != null) tvLikes.setText(totalLikes);
                                if (tvStats != null) tvStats.setText(String.valueOf(contributedCodes));
                            });
                        } 
                    }catch (Exception e) {
                        AppLogger.e("ProfileActivity", "解析创作者数据失败: " + e.getMessage());
                    }
                    }
                }
            }
        });
    }

    /**
     * 检查服务是否正在运行
     * @param serviceClass 要检查的服务类
     * @return 如果服务正在运行则返回true，否则返回false
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(android.content.Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化用户名：超过 6 个字显示省略号
     * @param username 原始用户名
     * @return 格式化后的用户名
     */
    private String formatUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "";
        }
        if (username.length() > 6) {
            return username.substring(0, 6) + "…";
        }
        return username;
    }

    /**
     * 更新网络切换按钮图标
     * 根据当前网络模式显示不同的图标
     */
    private void updateNetworkButtonIcon() {
        boolean isProxyEnabled = ProxyConfig.isProxyEnabled(this);
        if (isProxyEnabled) {
            btnNetworkSwitch.setImageResource(R.drawable.ic_cloud_blue);
        } else {
            btnNetworkSwitch.setImageResource(R.drawable.ic_cloud_gray);
        }
    }

    /**
     * 显示网络切换对话框
     * 让用户选择使用默认网络或代理网络
     */
    private void showNetworkSwitchDialog() {
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
            updateNetworkButtonIcon();
            Toast.makeText(this, "已切换到" + (newProxyEnabled ? "代理网络" : "默认网络"), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.show();
    }
}