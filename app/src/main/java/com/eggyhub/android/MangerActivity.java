package com.eggyhub.android;

import androidx.appcompat.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import com.eggyhub.android.log.AppLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;

import com.eggyhub.android.utils.PermissionUtils;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eggyhub.android.ArticleItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 管理界面Activity类
 * 负责展示和管理用户的文章、视频、分享码和文件
 */
public class MangerActivity extends BaseActivity {
    private static final String TAG = "MangerActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    // 数据列表
    private List<ShareCodeItem> shareCodeList = new ArrayList<>();
    private List<ArticleItem> articleList = new ArrayList<>();
    private List<Myvditem> videoList = new ArrayList<>();
    private List<FileItem> fileList = new ArrayList<>();
    private Call currentCall;

    // 当前选中的标签ID
    private int currentTabId = 1; // 1:文章, 2:视频, 3:分享码, 4:文件

    // UI组件
    private TextView tabArticle;    // 文章标签
    private TextView tabVideo;      // 视频标签
    private TextView tabShareCode;  // 分享码标签
    private TextView tabFile;       // 文件标签
    private RecyclerView contentRecyclerView;  // 内容列表
    private CardView cardRepoName;  // 仓库名称卡片
    private CardView cardRepoDesc;  // 仓库描述卡片
    private CardView cardRepoSize;  // 仓库大小卡片
    private TextView repoNameText;  // 仓库名称文本
    private TextView repoDescText;  // 仓库描述文本
    private TextView repoSizeText;  // 仓库大小文本
    private TextView expandStorageBtn;  // 扩容按钮
    private TextView editRepoNameBtn;   // 修改仓库名按钮
    private TextView editRepoDescBtn;   // 修改仓库描述按钮

    // 数据和配置
    private SharedPreferences preferences;
    private String accessToken;
    private RepoData repoData;  // 仓库数据

    // 适配器
    private MangerPageAd articleAdapter;
    private MyvdAd videoAdapter;
    private MycdAd shareCodeAdapter;
    private FileAdapter fileAdapter;
    private neterrAdap loadingAdapter;
    private neterrAdap emptyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manger);

        // 初始化UI组件
        initViews();

        // 初始化数据
        initData();

        // 设置监听器
        setupListeners();

        // 默认选中文章标签
        switchToTab(1);
    }

    /**
     * 初始化UI组件
     */
    private void initViews() {
        tabArticle = findViewById(R.id.MangerW);
        tabVideo = findViewById(R.id.MangerS);
        tabShareCode = findViewById(R.id.MangerC);
        tabFile = findViewById(R.id.MangerF);
        contentRecyclerView = findViewById(R.id.manRecyclerView);
        cardRepoName = findViewById(R.id.cardFilename);
        cardRepoDesc = findViewById(R.id.cardFileds);
        cardRepoSize = findViewById(R.id.cardFilesize);
        repoNameText = findViewById(R.id.Filename);
        repoDescText = findViewById(R.id.Fileds);
        repoSizeText = findViewById(R.id.Filesize);
        expandStorageBtn = findViewById(R.id.ExpandStorage);
        editRepoNameBtn = findViewById(R.id.EditRepoName);
        editRepoDescBtn = findViewById(R.id.EditRepoDescription);

        // 初始化返回按钮
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());

        // 设置RecyclerView布局管理器
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * 初始化数据
     */
    private void initData() {
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        accessToken = SecureStorageManager.getAccessToken();
        loadingAdapter = new neterrAdap("加载中");
        emptyAdapter = new neterrAdap("");
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 标签切换监听器
        tabArticle.setOnClickListener(v -> switchToTab(1));
        tabVideo.setOnClickListener(v -> switchToTab(2));
        tabShareCode.setOnClickListener(v -> switchToTab(3));
        tabFile.setOnClickListener(v -> switchToTab(4));

        // 扩容按钮监听器
        expandStorageBtn.setOnClickListener(v -> showExpandStorageDialog());

        // 修改仓库名按钮监听器
        editRepoNameBtn.setOnClickListener(v -> showEditRepoNameDialog());

        // 修改仓库描述按钮监听器
        editRepoDescBtn.setOnClickListener(v -> showEditRepoDescriptionDialog());
    }

    /**
     * 切换标签
     * @param tabId 标签ID: 1-文章, 2-视频, 3-分享码, 4-文件
     */
    private OkHttpClient client;
    @Override
    protected void onResume() {
        super.onResume();
        checkTutorialStatus();
    }

    private void checkTutorialStatus() {
        TutorialManager manager = TutorialManager.getInstance(this);
        if (manager.isTutorialRunning() && TutorialManager.TUTORIAL_SUPPLEMENT_CODE.equals(manager.getCurrentTutorial())) {
            int stepIndex = manager.getStepIndex();
            // 已经在 MangerActivity 了，现在需要引导点击 “分享码” 标签
            if (stepIndex == 2 && currentTabId != 3) {
                View tabShareCode = findViewById(R.id.MangerC);
                if (tabShareCode != null) {
                    GuideHelper.show(this, tabShareCode, "第三步：点击“分享码”切换到分享码管理页面", true, () -> {
                        manager.nextStep(); // 2 -> 3
                    });
                }
            }
        }
    }

    private void switchToTab(int tabId) {
        contentRecyclerView.setAdapter(loadingAdapter);
        if (currentCall != null) {
            currentCall.cancel();
        }
        
        currentTabId = tabId;
        updateTabColors(tabId);
        hideRepoCards(); // 隐藏仓库卡片
        checkTutorialStatus(); // 切换标签时检查引导状态

        // 根据标签ID加载对应数据
        switch (tabId) {
            case 1:
                articleList.clear();
                fetchArticleData();
                break;
            case 2:
                videoList.clear();
                fetchVideoData();
                break;
            case 3:
                shareCodeList.clear();
                fetchShareCodeData();
                break;
            case 4:
                fileList.clear();
                fetchFileData();
                break;
        }
    }

    /**
     * 更新标签颜色
     * @param selectedTabId 选中的标签ID
     */
    private void updateTabColors(int selectedTabId) {
        // 重置所有标签颜色
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int defaultColor = typedValue.data;

        int selectedColor = Color.parseColor("#66CCFF");

        tabArticle.setTextColor(defaultColor);
        tabVideo.setTextColor(defaultColor);
        tabShareCode.setTextColor(defaultColor);
        tabFile.setTextColor(defaultColor);

        // 设置选中标签颜色
        switch (selectedTabId) {
            case 1:
                tabArticle.setTextColor(selectedColor);
                break;
            case 2:
                tabVideo.setTextColor(selectedColor);
                break;
            case 3:
                tabShareCode.setTextColor(selectedColor);
                break;
            case 4:
                tabFile.setTextColor(selectedColor);
                break;
        }

        Toast.makeText(this, "加载中，请稍等", Toast.LENGTH_SHORT).show();
    }

    /**
     * 隐藏仓库卡片
     */
    private void hideRepoCards() {
        cardRepoName.setVisibility(View.GONE);
        cardRepoDesc.setVisibility(View.GONE);
        cardRepoSize.setVisibility(View.GONE);
    }

    /**
     * 显示仓库卡片
     */
    private void showRepoCards() {
        cardRepoName.setVisibility(View.VISIBLE);
        cardRepoDesc.setVisibility(View.VISIBLE);
        cardRepoSize.setVisibility(View.VISIBLE);
    }

    /**
     * 获取文件数据
     */
    private void fetchFileData() {
        AppLogger.d(TAG, "开始获取文件数据");
        contentRecyclerView.setAdapter(loadingAdapter);

        // 检查accessToken是否为空
        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        // 优先加载本地数据
        String cachedData = preferences.getString("file_data", null);
        if (cachedData != null) {
            handleFileResponse(cachedData);
        }

        if (client == null) {
            client = OkHttpClientFactory.getSharedClient();
        }
        String url = "https://eggyhub.top/api/user/repos";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                AppLogger.e(TAG, "获取文件数据失败: " + e.getMessage());
                runOnUiThread(() -> {
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (call.isCanceled()) return;
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String bodyString = responseBody.string();
                        // 保存数据到SharedPreferences
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("file_data", bodyString);
                        editor.apply();
                        handleFileResponse(bodyString);
                    } else {
                        AppLogger.e(TAG, "获取文件数据失败，响应码: " + response.code());
                        runOnUiThread(() -> {
                            if (cachedData == null) {
                                Toast.makeText(MangerActivity.this, "请求失败，响应码: " + response.code(), Toast.LENGTH_SHORT).show();
                                contentRecyclerView.setAdapter(emptyAdapter);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 处理文件响应数据
     * @param responseBody 响应体字符串
     */
    private void handleFileResponse(String responseBody) {
        AppLogger.d(TAG, "获取文件数据成功，响应体: " + responseBody);

        try {
            Gson gson = new Gson();
            FileResponse fileResponse = gson.fromJson(responseBody, FileResponse.class);

            if (fileResponse != null) {
                repoData = fileResponse.getRepo(); // 更新仓库数据
                if (repoData != null) {
                    int repoId = repoData.getRepoId();
                    AppLogger.d(TAG, "仓库ID: " + repoId);
                    AppLogger.d(TAG, "仓库名称: " + repoData.getName());
                    AppLogger.d(TAG, "文件总数: " + repoData.getFileCount());

                    // 创建临时列表存储新数据
                    List<FileItem> newFileList = new ArrayList<>();

                    // 处理文件列表
                    if (fileResponse.getFiles() != null && !fileResponse.getFiles().isEmpty()) {
                        for (FileData fileData : fileResponse.getFiles()) {
                            FileItem fileItem = FileItem.fromFileData(fileData, repoId);
                            newFileList.add(fileItem);
                            AppLogger.d(TAG, "添加文件: " + fileItem.getOriginalName() + ", 类型: " + fileItem.getFileType());
                        }
                    } else {
                        AppLogger.w(TAG, "文件列表为空");
                    }

                    // 在主线程更新UI和数据
                    runOnUiThread(() -> {
                        // 显示仓库卡片并设置文本
                        showRepoCards();
                        repoNameText.setText("仓库名: " + repoData.getName());
                        repoDescText.setText("描述: " + repoData.getDescription());
                        repoSizeText.setText("大小: " + repoData.getTotalSpace());

                        // 清除旧数据（包括缓存数据）并添加新数据
                        fileList.clear();
                        fileList.addAll(newFileList);

                        if (!fileList.isEmpty()) {
                            fileAdapter = new FileAdapter(MangerActivity.this, fileList);
                            setupFileAdapterListeners(fileAdapter);
                            contentRecyclerView.setAdapter(fileAdapter);
                        } else {
                            contentRecyclerView.setAdapter(emptyAdapter);
                            Toast.makeText(MangerActivity.this, "暂无文件", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    AppLogger.w(TAG, "仓库信息为空");
                    runOnUiThread(() -> contentRecyclerView.setAdapter(emptyAdapter));
                }
                AppLogger.d(TAG, "用户ID: " + fileResponse.getUserId());
            } else {
                AppLogger.w(TAG, "响应解析为空");
                runOnUiThread(() -> contentRecyclerView.setAdapter(emptyAdapter));
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "解析文件数据异常: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(MangerActivity.this, "数据解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                contentRecyclerView.setAdapter(emptyAdapter);
            });
        }
    }

    /**
     * 设置文件适配器监听器
     * @param adapter 文件适配器
     */
    private void setupFileAdapterListeners(FileAdapter adapter) {
        adapter.setOnFileActionListener(new FileAdapter.OnFileActionListener() {
            @Override
            public void onPreviewClick(FileItem fileItem) {
                previewFile(fileItem);
            }

            @Override
            public void onDownloadClick(FileItem fileItem) {
                downloadFile(fileItem);
            }
        });
    }

    /**
     * 预览文件
     * @param fileItem 文件项
     */
    private void previewFile(FileItem fileItem) {
        try {
            // 获取预览链接
            String previewUrl = "https://eggyhub.top/" + fileItem.getPreviewUrl();
            AppLogger.d(TAG, "预览文件链接: " + previewUrl);
            Toast.makeText(this, "预览: " + fileItem.getOriginalName(), Toast.LENGTH_SHORT).show();

            // 使用应用内预览所有文件类型
            Intent intent = new Intent(this, FilePreviewActivity.class);
            intent.putExtra("file_url", previewUrl);
            intent.putExtra("file_type", fileItem.getFileType());
            intent.putExtra("token", accessToken);
            startActivity(intent);
        } catch (Exception e) {
            AppLogger.e(TAG, "预览文件异常: " + e.getMessage());
            Toast.makeText(this, "预览失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 下载文件
     * @param fileItem 文件项
     */
    private void downloadFile(FileItem fileItem) {
        try {
            // 检查存储权限
            if (!PermissionUtils.hasStoragePermission(this)) {
                // 请求存储权限
                PermissionUtils.showPermissionRationaleDialog(
                    this,
                    "下载文件需要存储权限，请允许应用访问存储空间",
                    () -> PermissionUtils.requestStoragePermission(MangerActivity.this)
                );
                return;
            }
            
            // 获取下载链接
            String downloadUrl = "https://eggyhub.top/" + fileItem.getDownloadUrl();
            AppLogger.d(TAG, "下载文件链接: " + downloadUrl);
            Toast.makeText(this, "开始下载: " + fileItem.getOriginalName(), Toast.LENGTH_SHORT).show();

            // 启动下载任务
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));

            // 设置下载目录和文件名
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileItem.getOriginalName());
            request.setTitle(fileItem.getOriginalName());
            request.setDescription("正在下载...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // 添加请求头
            request.addRequestHeader("Authorization", "Bearer " + accessToken);

            // 开始下载
            downloadManager.enqueue(request);
        } catch (Exception e) {
            AppLogger.e(TAG, "下载文件异常: " + e.getMessage());
            Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理无访问令牌的情况
     */
    private void handleNoAccessToken() {
        AppLogger.e(TAG, "accessToken为空");
        runOnUiThread(() -> {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            contentRecyclerView.setAdapter(emptyAdapter);
        });
    }

    /**
     * 获取视频数据
     */
    private void fetchVideoData() {
        contentRecyclerView.setAdapter(loadingAdapter);

        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        // 优先加载本地数据
        String cachedData = preferences.getString("video_data", null);
        if (cachedData != null) {
            List<Myvditem> data = new Gson().fromJson(cachedData, new TypeToken<List<Myvditem>>() {}.getType());
            videoList.clear();
            videoList.addAll(data);
            updateVideoAdapter();
        }

        if (client == null) {
            client = OkHttpClientFactory.getSharedClient();
        }
        String url = "https://eggyhub.top/api/myvideos";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                runOnUiThread(() -> {
                    if (cachedData == null) {
                        Toast.makeText(MangerActivity.this, "获取视频数据失败", Toast.LENGTH_SHORT).show();
                        contentRecyclerView.setAdapter(emptyAdapter);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (call.isCanceled()) return;
                    if (response.isSuccessful()) {
                        String bodyString = responseBody.string();
                        // 保存数据到SharedPreferences
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("video_data", bodyString);
                        editor.apply();

                        List<Myvditem> data = new Gson().fromJson(bodyString, new TypeToken<List<Myvditem>>() {}.getType());

                        runOnUiThread(() -> {
                            videoList.clear();
                            videoList.addAll(data);
                            updateVideoAdapter();
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (cachedData == null) {
                                Toast.makeText(MangerActivity.this, "获取视频数据失败", Toast.LENGTH_SHORT).show();
                                contentRecyclerView.setAdapter(emptyAdapter);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 更新视频适配器
     */
    private void updateVideoAdapter() {
        if (!videoList.isEmpty()) {
            videoAdapter = new MyvdAd(videoList, this);
            contentRecyclerView.setAdapter(videoAdapter);
        } else {
            contentRecyclerView.setAdapter(emptyAdapter);
            Toast.makeText(this, "暂无视频", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取文章数据
     */
    private void fetchArticleData() {
        contentRecyclerView.setAdapter(loadingAdapter);

        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        // 优先加载本地数据
        String cachedData = preferences.getString("article_data", null);
        if (cachedData != null) {
            List<ArticleItem> data = new Gson().fromJson(cachedData, new TypeToken<List<ArticleItem>>() {}.getType());
            articleList.clear();
            articleList.addAll(data);
            updateArticleAdapter();
        }

        if (client == null) {
            client = OkHttpClientFactory.getSharedClient();
        }
        String url = "https://eggyhub.top/api/article_list";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                runOnUiThread(() -> {
                    if (cachedData == null) {
                        Toast.makeText(MangerActivity.this, "获取文章数据失败", Toast.LENGTH_SHORT).show();
                        contentRecyclerView.setAdapter(emptyAdapter);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (call.isCanceled()) return;
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String bodyString = responseBody.string();
                        // 保存数据到SharedPreferences
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("article_data", bodyString);
                        editor.apply();

                        List<ArticleItem> data = new Gson().fromJson(bodyString, new TypeToken<List<ArticleItem>>() {}.getType());

                        runOnUiThread(() -> {
                            articleList.clear();
                            articleList.addAll(data);
                            updateArticleAdapter();
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (cachedData == null) {
                                Toast.makeText(MangerActivity.this, "获取文章数据失败", Toast.LENGTH_SHORT).show();
                                contentRecyclerView.setAdapter(emptyAdapter);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 更新文章适配器
     */
    private void updateArticleAdapter() {
        if (!articleList.isEmpty()) {
            articleAdapter = new MangerPageAd(articleList, this);
            contentRecyclerView.setAdapter(articleAdapter);
        } else {
            contentRecyclerView.setAdapter(emptyAdapter);
            Toast.makeText(this, "暂无文章", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示修改仓库描述对话框
     */
    private void showEditRepoDescriptionDialog() {
        // 检查仓库数据是否存在
        if (repoData == null) {
            Toast.makeText(this, "无法获取仓库信息，请刷新重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_repo, null);
        EditText editTextRepoName = dialogView.findViewById(R.id.editTextRepoName);
        editTextRepoName.setText(repoData.getName()); // 设置当前仓库名作为默认值
        EditText editTextRepoDescription = dialogView.findViewById(R.id.editTextRepoDescription);
        editTextRepoDescription.setText(repoData.getDescription()); // 设置当前仓库描述作为默认值
        
        TextView textViewTitle = dialogView.findViewById(R.id.textViewDialogTitle);
        if (textViewTitle != null) {
            textViewTitle.setText("修改仓库描述");
        }
        
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
        editTextRepoDescription.requestFocus();
        editTextRepoDescription.setSelection(editTextRepoDescription.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String newRepoDescription = editTextRepoDescription.getText().toString().trim();
                // 发送修改仓库描述的请求
                updateRepoDescription(repoData.getRepoId(), repoData.getName(), newRepoDescription);
                dialog.dismiss();
            });
        }
    }

    /**
     * 更新仓库描述
     * @param repoId 仓库ID
     * @param name 仓库名称
     * @param newDescription 新的仓库描述
     */
    private void updateRepoDescription(int repoId, String name, String newDescription) {
        AppLogger.d(TAG, "开始修改仓库描述");

        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        // 创建请求参数
        RepoUpdateRequest requestModel = new RepoUpdateRequest(repoId, name, newDescription);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestModel);

        // 发送POST请求
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/repos/update")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "修改仓库描述请求失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MangerActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) return;
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String responseData = responseBody.string();
                        AppLogger.d(TAG, "修改仓库描述响应: " + responseData);

                        // 更新本地仓库数据
                        repoData.setDescription(newDescription);

                        runOnUiThread(() -> {
                            repoDescText.setText("描述:" + newDescription); // 更新UI显示
                            Toast.makeText(MangerActivity.this, "仓库描述修改成功", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        AppLogger.e(TAG, "修改仓库描述失败，响应码: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(MangerActivity.this, "修改失败，请重试", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    /**
     * 显示修改仓库名对话框
     */
    private void showEditRepoNameDialog() {
        // 检查仓库数据是否存在
        if (repoData == null) {
            Toast.makeText(this, "无法获取仓库信息，请刷新重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_repo, null);
        EditText editTextRepoName = dialogView.findViewById(R.id.editTextRepoName);
        editTextRepoName.setText(repoData.getName()); // 设置当前仓库名作为默认值
        EditText editTextRepoDescription = dialogView.findViewById(R.id.editTextRepoDescription);
        editTextRepoDescription.setText(repoData.getDescription()); // 设置当前仓库描述作为默认值
        
        TextView textViewTitle = dialogView.findViewById(R.id.textViewDialogTitle);
        if (textViewTitle != null) {
            textViewTitle.setText("修改仓库名");
        }
        
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
        editTextRepoName.requestFocus();
        editTextRepoName.setSelection(editTextRepoName.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String newRepoName = editTextRepoName.getText().toString().trim();
                if (newRepoName.isEmpty()) {
                    Toast.makeText(MangerActivity.this, "仓库名不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 发送修改仓库名的请求
                updateRepoName(repoData.getRepoId(), newRepoName, repoData.getDescription());
                dialog.dismiss();
            });
        }
    }

    /**
     * 更新仓库名称
     * @param repoId 仓库ID
     * @param newName 新的仓库名称
     * @param description 仓库描述
     */
    private void updateRepoName(int repoId, String newName, String description) {
        AppLogger.d(TAG, "开始修改仓库名: " + newName);

        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        // 创建请求参数
        RepoUpdateRequest requestModel = new RepoUpdateRequest(repoId, newName, description);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestModel);

        // 发送POST请求
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/repos/update")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "修改仓库名请求失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MangerActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String responseData = responseBody.string();
                        AppLogger.d(TAG, "修改仓库名响应: " + responseData);

                        // 更新本地仓库数据
                        repoData.setName(newName);

                        runOnUiThread(() -> {
                            repoNameText.setText("仓库名:" + newName); // 更新UI显示
                            Toast.makeText(MangerActivity.this, "仓库名修改成功", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        AppLogger.e(TAG, "修改仓库名失败，响应码: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(MangerActivity.this, "修改失败，请重试", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    /**
     * 显示扩容确认对话框
     */
    private void showExpandStorageDialog() {
        // 检查仓库数据是否存在
        if (repoData == null) {
            Toast.makeText(this, "无法获取仓库信息，请刷新重试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算当前扩容次数和所需碎片数量
        int initialCapacityMb = 50; // 初始容量50MB
        int capacityIncreaseMb = 50; // 每次扩容增加50MB

        // 从total_space解析当前总容量(MB)
        int currentTotalSpaceMb;
        try {
            // 处理可能的浮点数格式，如"50.00"
            String totalSpaceStr = repoData.getTotalSpace();
            // 提取数字部分，包括小数点
            String numericPart = totalSpaceStr.replaceAll("[^0-9.]", "");
            // 解析为浮点数然后转换为整数
            currentTotalSpaceMb = (int) Double.parseDouble(numericPart);
        } catch (Exception e) {
            AppLogger.e(TAG, "解析总空间失败: " + e.getMessage());
            Toast.makeText(this, "无法解析仓库容量", Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算扩容次数 (当前容量-初始容量)/每次增加容量
        int expansionCount = (currentTotalSpaceMb - initialCapacityMb) / capacityIncreaseMb;
        int nextExpansionCount = expansionCount + 1; // 下一次扩容是第N次
        int requiredCoins = 100 * nextExpansionCount; // 第N次消耗100*N碎片

        int nextCapacity = currentTotalSpaceMb + capacityIncreaseMb; // 扩容后的容量

        // 使用应用主题创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setTitle("扩容确认")
                .setMessage("当前仓库容量: " + repoData.getTotalSpace() + "\n" +
                        "第 " + nextExpansionCount + " 次扩容，消耗 " + requiredCoins + " 碎片\n" +
                        "扩容后容量: " + nextCapacity + " MB\n" +
                        "确定要进行仓库扩容吗？")
                .setPositiveButton("确定", (dialog, which) -> sendExpandStorageRequest())
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 发送扩容请求
     */
    private void sendExpandStorageRequest() {
        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/coins/tospace";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MangerActivity.this, "扩容请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(MangerActivity.this, "扩容请求成功，请刷新查看结果", Toast.LENGTH_SHORT).show();
                            // 刷新文件界面
                            fetchFileData();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MangerActivity.this, "扩容请求失败，响应码: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    /**
     * 获取分享码数据
     */
    private void fetchShareCodeData() {
        contentRecyclerView.setAdapter(loadingAdapter);

        if (accessToken == null || accessToken.isEmpty()) {
            handleNoAccessToken();
            return;
        }

        // 优先加载本地数据
        String cachedData = preferences.getString("share_code_data", null);
        if (cachedData != null) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ShareCodeItem>>() {}.getType();
            List<ShareCodeItem> fetchedList = gson.fromJson(cachedData, listType);
            shareCodeList.clear();
            shareCodeList.addAll(fetchedList);

            if (!shareCodeList.isEmpty()) {
                contentRecyclerView.setLayoutManager(new LinearLayoutManager(MangerActivity.this));
                shareCodeAdapter = new MycdAd(shareCodeList, preferences.getString("username", null), MangerActivity.this);
                contentRecyclerView.setAdapter(shareCodeAdapter);
            } else {
                contentRecyclerView.setAdapter(emptyAdapter);
            }
        }

        if (client == null) {
            client = OkHttpClientFactory.getSharedClient();
        }
        String url = "https://eggyhub.top/api/mygifts";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return;
                runOnUiThread(() -> {
                    if (cachedData == null) {
                        Toast.makeText(MangerActivity.this, "网络异常", Toast.LENGTH_LONG).show();
                        contentRecyclerView.setAdapter(new neterrAdap("网络错误"));
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        final String responseData = responseBody.string();

                        // 保存数据到SharedPreferences
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("share_code_data", responseData);
                        editor.apply();

                        runOnUiThread(() -> {
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<ShareCodeItem>>() {}.getType();
                            List<ShareCodeItem> fetchedList = gson.fromJson(responseData, listType);
                            shareCodeList.clear();
                            shareCodeList.addAll(fetchedList);

                            if (!shareCodeList.isEmpty()) {
                                contentRecyclerView.setLayoutManager(new LinearLayoutManager(MangerActivity.this));
                                shareCodeAdapter = new MycdAd(shareCodeList, preferences.getString("username", null), MangerActivity.this);
                                contentRecyclerView.setAdapter(shareCodeAdapter);
                            } else {
                                contentRecyclerView.setAdapter(emptyAdapter);
                                Toast.makeText(MangerActivity.this, "暂无分享码", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (cachedData == null) {
                                Toast.makeText(MangerActivity.this, "失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 重启时刷新当前标签页数据
        switchToTab(currentTabId);
    }

    /**
     * 文件响应类
     */
    private static class FileResponse {
        @com.google.gson.annotations.SerializedName("files")
        private List<FileData> files;
        @com.google.gson.annotations.SerializedName("repo")
        private RepoData repo;
        @com.google.gson.annotations.SerializedName("user_id")
        private String userId;

        public List<FileData> getFiles() { return files; }
        public void setFiles(List<FileData> files) { this.files = files; }
        public RepoData getRepo() { return repo; }
        public void setRepo(RepoData repo) { this.repo = repo; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    /**
     * 文件数据类
     */
    public static class FileData {
        @com.google.gson.annotations.SerializedName("file_id")
        private int file_id;
        @com.google.gson.annotations.SerializedName("file_size")
        private String file_size;
        @com.google.gson.annotations.SerializedName("file_size_kb")
        private int file_size_kb;
        @com.google.gson.annotations.SerializedName("file_type")
        private String file_type;
        @com.google.gson.annotations.SerializedName("original_name")
        private String original_name;
        @com.google.gson.annotations.SerializedName("status")
        private int status;
        @com.google.gson.annotations.SerializedName("upload_time")
        private String upload_time;

        public int getFileId() { return file_id; }
        public void setFileId(int file_id) { this.file_id = file_id; }
        public String getFileSize() { return file_size; }
        public void setFileSize(String file_size) { this.file_size = file_size; }
        public int getFileSizeKb() { return file_size_kb; }
        public void setFileSizeKb(int file_size_kb) { this.file_size_kb = file_size_kb; }
        public String getFileType() { return file_type; }
        public void setFileType(String file_type) { this.file_type = file_type; }
        public String getOriginalName() { return original_name; }
        public void setOriginalName(String original_name) { this.original_name = original_name; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getUploadTime() { return upload_time; }
        public void setUploadTime(String upload_time) { this.upload_time = upload_time; }
    }

    /**
     * 仓库数据类
     */
    private static class RepoData {
        @com.google.gson.annotations.SerializedName("repo_id")
        private int repo_id;
        @com.google.gson.annotations.SerializedName("created_at")
        private String created_at;
        @com.google.gson.annotations.SerializedName("description")
        private String description;
        @com.google.gson.annotations.SerializedName("file_count")
        private int file_count;
        @com.google.gson.annotations.SerializedName("likes")
        private int likes;
        @com.google.gson.annotations.SerializedName("name")
        private String name;
        @com.google.gson.annotations.SerializedName("total_space")
        private String total_space;
        @com.google.gson.annotations.SerializedName("total_space_kb")
        private int total_space_kb;
        @com.google.gson.annotations.SerializedName("used_space")
        private String used_space;
        @com.google.gson.annotations.SerializedName("used_space_kb")
        private int used_space_kb;

        public int getRepoId() { return repo_id; }
        public void setRepoId(int repo_id) { this.repo_id = repo_id; }
        public String getCreatedAt() { return created_at; }
        public void setCreatedAt(String created_at) { this.created_at = created_at; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getFileCount() { return file_count; }
        public void setFileCount(int file_count) { this.file_count = file_count; }
        public int getLikes() { return likes; }
        public void setLikes(int likes) { this.likes = likes; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTotalSpace() { return total_space; }
        public void setTotalSpace(String total_space) { this.total_space = total_space; }
        public int getTotalSpaceKb() { return total_space_kb; }
        public void setTotalSpaceKb(int total_space_kb) { this.total_space_kb = total_space_kb; }
        public String getUsedSpace() { return used_space; }
        public void setUsedSpace(String used_space) { this.used_space = used_space; }
        public int getUsedSpaceKb() { return used_space_kb; }
        public void setUsedSpaceKb(int used_space_kb) { this.used_space_kb = used_space_kb; }
    }
    
    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionUtils.REQUEST_CODE_STORAGE) {
            PermissionUtils.handlePermissionResult(this, requestCode, grantResults, "存储");
        }
    }
}