package com.eggyhub.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eggyhub.android.dialog.NoticeBoardDialog;
import com.eggyhub.android.log.AppLogger;
import com.eggyhub.android.utils.OkHttpClientFactory;
import com.eggyhub.android.utils.NoticeDataManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 应用主页面Activity
 * 负责显示应用的主要内容，包括地图列表、导航功能和搜索功能等
 */

public class MainActivity extends BaseActivity {
    private static final String API_URL = "https://eggyhub.top/static/app/update.json";
    private long latestVersionCode = 1;
    private int mPage = 1;
    private int mType = 1;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean mIsFirstLaunch = true;
    private RecyclerView mMapRecyclerView;
    private MapAdapter mMapAdapter;
    private List<MapItem> mMapList;
    private List<String> mMapCodeCacheList;
    private Spinner mSpinnerCategory;
    private EditText mSearchEditText;
    private ImageView mSearchButton;
    private Random mRandom = new Random();
    private HashSet<Integer> mUsedPageNumbers = new HashSet<>();
    private UpdateManager mUpdateManager;
    // 任务领取相关变量
    private int totalClaimRequests = 0;
    private int completedClaimRequests = 0;
    private int successfulClaimRequests = 0;
    private int failedClaimRequests = 0;

    /**
     * Activity创建时调用的方法
     * 初始化UI组件、设置布局和加载数据
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置主页面布局

        // 阻止EditText自动获取焦点并弹出输入法
        // 在布局的根视图上设置focusableInTouchMode，并请求焦点
        findViewById(R.id.main_root_layout).requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // 初始化UI组件
        initUIComponents();

        // 初始化数据    
        initData(savedInstanceState);

        // 设置监听器 
        setupListeners();

        // 设置底部导航
        setupBottomNavigation();

        // 检查更新
        mUpdateManager = new UpdateManager(this);
        mUpdateManager.checkForUpdate();

        // 预加载公告数据
        NoticeDataManager.getInstance().preloadNoticeData();

        // 自动发送ID为2的完成任务请求
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String sponser = preferences.getString("sponser", "0.0");
        if (!"0.0".equals(sponser)) {
            claimTask(2, 1, 0);
        }

        // 处理返回键逻辑
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private long lastBackPressTime = 0;

            @Override
            public void handleOnBackPressed() {
                if (System.currentTimeMillis() - lastBackPressTime < 2000) {
                    // 如果在2秒内再次按下返回键，则退出Activity
                    finish();
                } else {
                    // 第一次按下返回键，提示用户再按一次退出
                    Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                    lastBackPressTime = System.currentTimeMillis();
                }
            }
        });

        // 启动后台同步用户信息
        syncUserData();
    }

    //公告弹窗方向控制（基于物理方向记录，强制恢复）
    private void showNoticeBoard() {
        // 1. 获取当前实际屏幕方向（横屏 or 竖屏）
        int currentOrientation = getResources().getConfiguration().orientation;
        final int targetOrientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            targetOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else {
            targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }

        // 2. 强制横屏（显示弹窗）
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // 3. 创建弹窗
        NoticeBoardDialog dialog = new NoticeBoardDialog(this);
        dialog.setOnDismissListener(d -> {
            // 4. 关闭后强制设置为原来的方向（竖屏或横屏）
            setRequestedOrientation(targetOrientation);
            // 5. 刷新 Activity 让方向立即生效
            new Handler(Looper.getMainLooper()).postDelayed(() -> recreate(), 100);
        });
        dialog.show();
    }

    /**
     * 后台同步用户信息和创作者数据
     */
    private void syncUserData() {
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int id = preferences.getInt("id", -1);
        if (id != -1) {
            fetchUserProfile(id);
            fetchCreatorData(id);
        }

        // 检查并显示加入QQ群弹窗
        checkShowQQGroupInviteDialog();
    }

    private void checkShowQQGroupInviteDialog() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("qq_group_invite_never_show", false)) {
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_qq_group_invite);
        dialog.setCancelable(false);

        // 设置弹窗宽度
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setAttributes(lp);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        TextView btnJoinLater = dialog.findViewById(R.id.btnJoinLater);
        TextView btnJoinNow = dialog.findViewById(R.id.btnJoinNow);

        String message = "我们为你写了一封信，可能稍微有一点长，已经尽可能的压缩了，麻烦认真看一下吧[抱拳]\n\n欢迎您来到Eggyhub移动端应用，您可能是通过Eggyhub网页下载的apk，也可能是其他渠道，感谢您对Eggyhub的支持，为了您的良好体验极力建议您加入Eggyhub移动端应用的官方讨论群（与网站端不互通，如果在官方群聊“蛋码研究院”中反馈问题不能及时看到），这个群专门用来反馈Eggyhub在使用过程中的任何问题，您所遇到的所有问题都会在这个群里得到明确的回复，您对Eggyhub移动端的畅想也可以尽情的提出来，一些关于软件的通知也会在这个群里发，您当然可以不用加群，或者在您需要的时候再选择加群，但是为了您的良好体验，还是建议您加入群聊。\n\n如果您选择暂时不加入群聊，可以在“我的”页面找到“移动端QQ群”的按钮点击后就可以加入群聊。";
        tvMessage.setText(message);

        // 5秒倒计时逻辑
        btnJoinLater.setEnabled(false);
        btnJoinLater.setAlpha(0.5f);

        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnJoinLater.setText("暂不加入 (" + (millisUntilFinished / 1000 + 1) + "s)");
            }

            @Override
            public void onFinish() {
                btnJoinLater.setText("暂不加入");
                btnJoinLater.setEnabled(true);
                btnJoinLater.setAlpha(1.0f);
            }
        }.start();

        btnJoinLater.setOnClickListener(v -> {
            prefs.edit().putBoolean("qq_group_invite_never_show", true).apply();
            dialog.dismiss();
        });

        btnJoinNow.setOnClickListener(v -> {
            if (joinQQGroup("Hu3GvTDPlrBDWLB-4S_jFdcrG4Jxd3-t")) {
                prefs.edit().putBoolean("qq_group_invite_never_show", true).apply();
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "未安装QQ或版本过低", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

         /**
      * 发起添加群流程
      */
    public boolean joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(android.net.Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e("MainActivity", "获取用户资料失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        try {
                            String responseBodyString = responseBody.string();
                            Gson gson = new Gson();
                            UserProfileResponse userProfileResponse = gson.fromJson(responseBodyString, UserProfileResponse.class);

                            if (userProfileResponse.isSuccess()) {
                                UserProfileResponse.Data data = userProfileResponse.getData();
                                if (data != null) {
                                    SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("avatar", data.getAvatar());
                                    editor.putString("description", data.getDescription());
                                    editor.putString("contact", data.getContact());
                                    editor.putString("eggyid", data.getEggyid());
                                    editor.apply();
                                    AppLogger.i("MainActivity", "用户信息已同步");
                                }
                            }
                        } catch (Exception e) {
                            AppLogger.e("MainActivity", "解析用户资料失败: " + e.getMessage());
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e("MainActivity", "获取创作者数据失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        try {
                            String jsonString = response.body().string();
                            Gson gson = new Gson();
                            CreatorResponse creatorResponse = gson.fromJson(jsonString, CreatorResponse.class);

                            if (creatorResponse != null) {
                                SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("cache_published_gifts", String.valueOf(creatorResponse.getPublishedGifts()));
                                editor.putString("cache_total_likes", creatorResponse.getTotalLikes());
                                editor.putString("cache_contributed_codes", String.valueOf(creatorResponse.getContributedCodes()));
                                editor.apply();
                                AppLogger.i("MainActivity", "创作者数据已同步");
                            }
                        } catch (Exception e) {
                            AppLogger.e("MainActivity", "解析创作者数据失败: " + e.getMessage());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mUpdateManager != null) {
            mUpdateManager.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkTutorialStatus();
    }

    private void checkTutorialStatus() {
        TutorialManager manager = TutorialManager.getInstance(this);
        if (manager.isTutorialRunning()) {
            String currentTutorial = manager.getCurrentTutorial();
            int stepIndex = manager.getStepIndex();
            
            // 如果是在首页，且处于发布教程的第一步（实际上第一步已经点过了，现在应该是回到首页或者在特定页面）
            // 这里的逻辑需要根据具体的教程步骤来设计
            // 示例：如果发布教程第一步是在首页点“发布”，那么点完后会进入 PublishActivity
        }
    }

        /**
     * Activity销毁时调用的方法
     * 解注册广播接收器，防止内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateManager != null) {
            mUpdateManager.unregisterReceiver();
        }
    }

        /**
     * 初始化UI组件
     */
    private void initUIComponents() {
        // 搜索相关组件
        mSearchEditText = findViewById(R.id.search_edit_text);
        if (mSearchEditText != null) {
            mSearchEditText.clearFocus();
            mSearchEditText.setFocusable(false);
            mSearchEditText.setFocusableInTouchMode(true);
        }

        // 地图列表相关组件
        mMapRecyclerView = findViewById(R.id.map_recycler_view);
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mMapRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 类别选择器和搜索按钮
        mSpinnerCategory = findViewById(R.id.category_spinner);
        mSearchButton = findViewById(R.id.search_image_view);
    }

/**
     * 初始化数据
     * @param savedInstanceState 保存的实例状态
     */
    private void initData(Bundle savedInstanceState) {
        mMapList = new ArrayList<>();
        mMapCodeCacheList = new ArrayList<>();
        mMapAdapter = new MapAdapter(mMapList, mMapCodeCacheList);
        mMapRecyclerView.setAdapter(mMapAdapter);

        // 恢复保存的状态或加载初始数据
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        } else if (mIsFirstLaunch) {
            fetchMapData();
            mIsFirstLaunch = false;
        }

        // 设置类别选择器
        setupCategorySpinner();

        // 设置搜索框
        setupSearchBox();
    }

    /**
     * 恢复保存的实例状态
     * @param savedInstanceState 保存状态的Bundle对象
     */
    private void restoreInstanceState(@NonNull Bundle savedInstanceState) {
        mMapList = savedInstanceState.getParcelableArrayList("mapList");
        mMapCodeCacheList = savedInstanceState.getStringArrayList("mapCodeCacheList");
        if (mMapList == null) {
            mMapList = new ArrayList<>();
        }
        if (mMapCodeCacheList == null) {
            mMapCodeCacheList = new ArrayList<>();
        }
        mMapAdapter.updateMapList(mMapList);
    }

        /**
     * 设置监听器
     */
    private void setupListeners() {
        // 下拉刷新监听器
        mSwipeRefreshLayout.setOnRefreshListener(() -> fetchMapData());

        // 公告卡片点击事件
        androidx.cardview.widget.CardView cardNotice = findViewById(R.id.cardNotice);
        if (cardNotice != null) {
            cardNotice.setOnClickListener(v -> showNoticeBoard());
            // 长按进入布局编辑器
            cardNotice.setOnLongClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, com.eggyhub.android.activity.NoticeBoardLayoutEditorActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // 仓库点击事件
        LinearLayout repoLayout = findViewById(R.id.repo_layout);
        repoLayout.setOnClickListener(v -> navigateToFileRepo());

        // 邮箱点击事件
        LinearLayout mailLayout = findViewById(R.id.mail_layout);
        mailLayout.setOnClickListener(v -> navigateToMail());

        // 跳转网站点击事件
        setupJumpWebsite();
        setupBhxq();
        setupGuideButton();

        // ★ 圆形图标导航（分享码、文章、视频、比赛）
        setupCircleIconsNavigation();
    }

    /**
     * 设置功能指引按钮点击事件
     */
    private void setupGuideButton() {
        View guideButton = findViewById(R.id.btn_guide_tutorial);
        if (guideButton != null) {
            guideButton.setOnClickListener(v -> showGuideDialog());
        }
    }

    private void showGuideDialog() {
        new GuideDialog(this).show();
    }

    /**
     * 显示公告板弹窗
     */
    private void setupCircleIconsNavigation() {
        View shareCodeLayout = findViewById(R.id.code_layout);
        if (shareCodeLayout != null) {
            shareCodeLayout.setOnClickListener(v -> navigateToShareCode());
        }

        View articleLayout = findViewById(R.id.article_layout);
        if (articleLayout != null) {
            articleLayout.setOnClickListener(v -> navigateToArticle());
        }

        View videoLayout = findViewById(R.id.video_layout);
        if (videoLayout != null) {
            videoLayout.setOnClickListener(v -> navigateToVideo());
        }

        View competitionLayout = findViewById(R.id.competition_layout);
        if (competitionLayout != null) {
            competitionLayout.setOnClickListener(v -> navigateToCompetition());
        }
    }

    public void startSupplementCodeGuide() {
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            GuideHelper.show(this, navProfile, "第一步：点击底部“我的”进入个人中心", true, () -> {
                TutorialManager.getInstance(this).nextStep(); // 0 -> 1
            });
        }
    }

    public void startPublishGuide() {
        View navPublish = findViewById(R.id.navPublish);
        if (navPublish != null) {
            GuideHelper.show(this, navPublish, "第一步：点击底部“发布”按钮进入发布页面", true, () -> {
                TutorialManager.getInstance(this).nextStep();
            });
        }
    }

    public void startTaskGuide() {
        View navTask = findViewById(R.id.navTask);
        if (navTask != null) {
            GuideHelper.show(this, navTask, "第一步：点击底部“任务”按钮查看任务大厅", true, () -> {
                TutorialManager.getInstance(this).nextStep();
            });
        }
    }

    public void startProfileGuide() {
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            GuideHelper.show(this, navProfile, "第一步：点击底部“我的”查看个人数据", true, () -> {
                TutorialManager.getInstance(this).nextStep(); // 0 -> 1
            });
        }
    }

    /**
     * 设置“白鹤星球”点击事件
     * 初始化白鹤星球布局并设置点击事件
     */
    private void setupBhxq() {
        LinearLayout bhxqLayout = findViewById(R.id.bhxq_layout);
        if (bhxqLayout != null) {
            bhxqLayout.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.starparty365.cn/"));
                startActivity(browserIntent);
            });
        }
    }
    /**
     * 保存实例状态时调用的方法
     * 用于保存地图数据列表，以便在Activity重建时恢复
     * @param outState 保存状态的Bundle对象
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("mapList", (ArrayList<? extends Parcelable>) mMapList);
        outState.putStringArrayList("mapCodeCacheList", (ArrayList<String>) mMapCodeCacheList);
    }

    /**
     * 获取地图数据
     * 从网络API请求地图数据并更新UI
     */
    private void fetchMapData() {
        int maxPageNum = 49; // 假设最大页数为49，可以根据实际情况调整
        int randomPageNum;
        do {
            randomPageNum = mRandom.nextInt(maxPageNum) + 1; // 生成1到maxPageNum之间的随机数
        } while (mUsedPageNumbers.contains(randomPageNum));
        mUsedPageNumbers.add(randomPageNum);
        mPage = randomPageNum;

        String url = "https://s3.game.163.com/7f5ec8225c3ea603/user/mapList?sortType=" + mType + "&pageNum=" + mPage + "&pageSize=20";

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleFetchFailure("网络异常");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        String responseData = responseBody.string();
                        handleFetchSuccess(responseData);
                    } else {
                        handleFetchFailure("网络异常");
                    }
                }
            }
        });
    }

    /**
     * 处理获取数据失败的情况
     * @param errorMessage 错误信息
     */
    private void handleFetchFailure(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            mMapRecyclerView.setAdapter(new neterrAdap(errorMessage));
            mMapRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, true));
            mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    /**
     * 处理获取数据成功的情况
     * @param responseData 响应数据
     */
    private void handleFetchSuccess(String responseData) {
        runOnUiThread(() -> {
            try {
                Gson gson = new Gson();
                MapResponse mapResponse = gson.fromJson(responseData, MapResponse.class);
                if (mapResponse != null && mapResponse.getData() != null) {
                    List<MapItem> newMapList = mapResponse.getData().getGameMapInfoList();
                    if (newMapList == null) newMapList = new ArrayList<>();
                    updateMapAdapter(newMapList);
                } else {
                    handleFetchFailure("数据格式错误");
                }
                mSwipeRefreshLayout.setRefreshing(false);
            } catch (Exception e) {
                handleFetchFailure("解析异常: " + e.getMessage());
            }
        });
    }

    /**
     * 更新地图适配器数据
     * @param newMapList 新的地图列表
     */
    private void updateMapAdapter(List<MapItem> newMapList) {
        mMapCodeCacheList.clear();
        for (MapItem item : newMapList) {
            mMapCodeCacheList.add(item.getMapCode());
        }
        mMapAdapter.updateMapList(newMapList);
    }

    /**
     * 设置底部导航
     * 初始化底部导航栏并设置各按钮的点击事件
     */
    private void setupBottomNavigation() {
        // 首页按钮（当前页面）
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setSelected(true);
        }

        // 发布按钮
        View navPublish = findViewById(R.id.navPublish);
        if (navPublish != null) {
            navPublish.setOnClickListener(v -> navigateToPublish());
        }

        // 任务按钮
        View navTask = findViewById(R.id.navTask);
        if (navTask != null) {
            navTask.setOnClickListener(v -> navigateToTask());
        }

        // 我的按钮
        View navMy = findViewById(R.id.navProfile);
        if (navMy != null) {
            navMy.setOnClickListener(v -> navigateToProfile());
        }
    }

    private void setupCategorySpinner() {
        mSpinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                filterContentByCategory(selectedCategory);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 不做任何操作
            }
        });
    }

    /**
     * 设置搜索框
     * 初始化搜索框和搜索图标，并设置点击事件
     */
    private void setupSearchBox() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.search_types));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCategory.setAdapter(adapter);

        // 设置搜索图标点击事件
        mSearchButton.setOnClickListener(v -> performSearch());

        // 设置回车键搜索功能
        mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    /**
     * 根据类别过滤内容
     * @param category 选择的类别
     */
    private void filterContentByCategory(String category) {
        // 这里实现根据类别过滤内容的逻辑
        // Toast.makeText(this, "已选择类别: " + category, Toast.LENGTH_SHORT).show();
        // 实际应用中，这里应该调用API或从数据库获取对应类别的内容
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        String keyword = mSearchEditText.getText().toString().trim();
        int searchType = mSpinnerCategory.getSelectedItemPosition();

        if (keyword.isEmpty()) {
            Toast.makeText(MainActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        intent.putExtra("keyword", keyword);
        intent.putExtra("searchType", searchType);
        startActivity(intent);
    }

    /**
     * 设置“跳转网站”点击事件
     * 初始化跳转网站布局并设置点击事件
     */
    private void setupJumpWebsite() {
        LinearLayout jumpWebsiteLayout = findViewById(R.id.jump_website_layout);
        if (jumpWebsiteLayout != null) {
            jumpWebsiteLayout.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://eggyhub.top"));
                startActivity(browserIntent);
            });
        }
    }

    // 以下是导航相关的辅助方法
    private void navigateToFileRepo() {
        startActivity(new Intent(MainActivity.this, FileRepoActivity.class));
    }

    private void navigateToMail() {
        startActivity(new Intent(MainActivity.this, MailActivity.class));
    }

    private void navigateToShareCode() {
        startActivity(new Intent(MainActivity.this, ShareCodeActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToArticle() {
        startActivity(new Intent(MainActivity.this, ArticleActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToVideo() {
        startActivity(new Intent(MainActivity.this, VideoActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToCompetition() {
        startActivity(new Intent(MainActivity.this, CompetitionActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToPublish() {
        startActivity(new Intent(MainActivity.this, PublishActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToTask() {
        startActivity(new Intent(MainActivity.this, TaskActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToProfile() {
        startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // 任务领取方法
    public void claimTask(int taskId, int completed, int claimed) {
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = SecureStorageManager.decryptAndRetrieve("access_token");

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        int claimableTimes = completed - claimed;
        if (claimableTimes <= 0) {
            return;
        }

        totalClaimRequests = claimableTimes;
        completedClaimRequests = 0;
        successfulClaimRequests = 0;
        failedClaimRequests = 0;

        for (int i = 0; i < claimableTimes; i++) {
            claimTaskInternal(taskId, accessToken);
        }
    }

    private void claimTaskInternal(int taskId, String accessToken) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/tasks/claim?taskid=" + taskId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    failedClaimRequests++;
                    completedClaimRequests++;
                    MainActivity.this.checkAllClaimsCompleted();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            successfulClaimRequests++;
                        } else {
                            failedClaimRequests++;
                        }
                        completedClaimRequests++;
                        MainActivity.this.checkAllClaimsCompleted();
                    });
                }
            }
        });
    }

    private void checkAllClaimsCompleted() {
        if (completedClaimRequests == totalClaimRequests) {
            // 所有任务领取请求已完成
        }
    }
}