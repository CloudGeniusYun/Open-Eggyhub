package com.eggyhub.android;

import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.res.Configuration;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.eggyhub.android.utils.OkHttpClientFactory;

import com.eggyhub.android.utils.VideoPlayerHelper;
import android.app.ProgressDialog;

/**
 * 视频页面活动
 * 展示视频分类列表和视频列表，处理分类切换和视频点击事件
 */
public class VideoActivity extends BaseActivity {
    // 常量定义
    private static final String TAG = "VideoActivity"; // 日志标签
    private static final String API_BASE_URL = "https://eggyhub.top/api"; // API基础URL
    private static final int PAGE_SIZE = 8; // 每页加载的视频数量
    // UI组件
    private RecyclerView rvVideoCategories; // 视频分类列表
    private VideoCategoryAdapter videoCategoryAdapter; // 视频分类适配器
    private RecyclerView rvVideoList; // 视频列表
    private VideoListAdapter videoListAdapter; // 视频列表适配器

    // 数据
    private final List<VideoItem> videoItems = new ArrayList<>(); // 视频数据列表
    private final List<VideoCategory> videoCategories = new ArrayList<>(); // 视频分类数据列表
    private int currentCategoryId = -1; // 当前选中的分类ID
    private int currentPage = 0; // 当前页码，从0开始
    private boolean isLoading = false; // 是否正在加载数据
    private boolean hasMoreData = true; // 是否还有更多数据

    // 网络和解析
    private OkHttpClient okHttpClient; // OkHttpClient实例（延迟初始化）
    private final Gson gson = new Gson(); // Gson实例
    private VideoPlayerHelper videoPlayerHelper;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // 初始化 OkHttpClient（使用工厂类，自动添加代理拦截器）
        okHttpClient = OkHttpClientFactory.getSharedClient();

        sharedPreferences = getSharedPreferences("video_cache", MODE_PRIVATE);
        videoPlayerHelper = new VideoPlayerHelper(this);

        initViews(); // 初始化视图
        setupListeners(); // 设置监听器
        loadAndFetchData();
    }

    private void loadAndFetchData() {
        loadLocalCategories();
        fetchVideoCategories();
        loadLocalVideos();
        fetchVideos(currentCategoryId);
    }

    private void loadLocalCategories() {
        String cachedCategories = sharedPreferences.getString("video_categories", null);
        if (cachedCategories != null) {
            parseAndUpdateCategories(cachedCategories, false);
        }
    }

    private void loadLocalVideos() {
        String cachedVideos = sharedPreferences.getString("videos_" + currentCategoryId, null);
        if (cachedVideos != null) {
            try {
                List<VideoItem> newVideos = gson.fromJson(cachedVideos, new TypeToken<List<VideoItem>>() {}.getType());
                updateVideoList(newVideos);
             } catch (Exception e) {
                AppLogger.e(TAG, "解析本地视频数据失败: " + e.getMessage());
            }
        }
    }

/**
     * 初始化视图组件
     */
    private void initViews() {
        // 返回按钮
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        // 视频分类列表
        rvVideoCategories = findViewById(R.id.rv_video_categories);
        rvVideoCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoCategoryAdapter = new VideoCategoryAdapter(videoCategories);
        rvVideoCategories.setAdapter(videoCategoryAdapter);

        // 视频列表
        rvVideoList = findViewById(R.id.rv_video_list);
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 2 : 1; // 横屏2列，竖屏1列
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        rvVideoList.setLayoutManager(layoutManager);
        videoListAdapter = new VideoListAdapter(videoItems);
        rvVideoList.setAdapter(videoListAdapter);
    }

    /**
     * 设置各种监听器
     */
    private void setupListeners() {

        // 视频列表滚动监听器
        rvVideoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        // 到底部了，加载更多数据
                        fetchVideos(currentCategoryId);
                    }
                }
            }
        });

        // 视频列表点击监听
        videoListAdapter.setOnItemClickListener(videoItem -> {
            if (videoItem != null && videoItem.getLink() != null) {
                videoPlayerHelper.fetchBiliPlayUrl(videoItem.getLink());
            }
        });

        // 视频分类点击事件监听器
        videoCategoryAdapter.setOnItemClickListener(position -> {
            if (position >= 0 && position < videoCategories.size()) {
                updateSelectedCategory(position);
                currentCategoryId = videoCategories.get(position).getId();
                refreshVideoList();
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(VideoActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * 更新选中的分类
     * @param position 选中的位置
     */
    private void updateSelectedCategory(int position) {
        for (int i = 0; i < videoCategories.size(); i++) {
            videoCategories.get(i).setSelected(i == position);
        }
        videoCategoryAdapter.notifyDataSetChanged();
    }

    /**
     * 刷新视频列表
     */
    private void refreshVideoList() {
        currentPage = 0;
        hasMoreData = true;
        videoItems.clear();
        videoListAdapter.notifyDataSetChanged();
        fetchVideos(currentCategoryId);
    }

    /**
     * 获取视频数据
     * @param categoryId 分类ID
     */
    private void fetchVideos(int categoryId) {
        if (isLoading) return; // 避免重复加载
        // 根据最新需求，此处不再显示“已经到底了”Toast，仅在API返回数据不足时显示。
        // if (!hasMoreData && videoItems.isEmpty()) {
        //     showToast("已经到底了");
        //     return;
        // }

        isLoading = true;
        String url = API_BASE_URL + "/videos?start=" + (currentPage * PAGE_SIZE) + "&grid=" + categoryId;

        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleVideoFetchFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleVideoFetchResponse(response);
            }
        });
    }

    /**
     * 处理视频获取失败
     * @param e 异常
     */
    private void handleVideoFetchFailure(IOException e) {
        AppLogger.e(TAG, "获取视频数据失败: " + e.getMessage());
        isLoading = false;
        String cachedVideos = sharedPreferences.getString("videos_" + currentCategoryId, null);
        if (cachedVideos == null) {
            showToast("获取视频数据失败，请重试");
        }
    }

    /**
     * 处理视频获取响应
     * @param response 响应
     */
    private void handleVideoFetchResponse(Response response) throws IOException {
        try (ResponseBody responseBody = response.body()) {
            isLoading = false;
            if (response.isSuccessful()) {
                String responseData = responseBody.string();
                AppLogger.d(TAG, "视频数据: " + responseData);

                SharedPreferences.Editor editor = sharedPreferences.edit();
            if (currentPage == 0) {
                editor.putString("videos_" + currentCategoryId, responseData);
            } else {
                String cachedVideos = sharedPreferences.getString("videos_" + currentCategoryId, null);
                if (cachedVideos != null) {
                    try {
                        List<VideoItem> cachedList = gson.fromJson(cachedVideos, new TypeToken<List<VideoItem>>() {}.getType());
                        List<VideoItem> newList = gson.fromJson(responseData, new TypeToken<List<VideoItem>>() {}.getType());
                        cachedList.addAll(newList);
                        editor.putString("videos_" + currentCategoryId, gson.toJson(cachedList));
                    } catch (Exception e) {
                        editor.putString("videos_" + currentCategoryId, responseData);
                    }
                } else {
                    editor.putString("videos_" + currentCategoryId, responseData);
                }
            }
            editor.apply();

            try {
                List<VideoItem> newVideos = gson.fromJson(responseData, new TypeToken<List<VideoItem>>() {}.getType());
                updateVideoList(newVideos);
            } catch (Exception e) {
                AppLogger.e(TAG, "解析视频数据失败: " + e.getMessage());
                showToast("解析视频数据失败");
            }
        } else {
            AppLogger.e(TAG, "获取视频数据失败，状态码: " + response.code());
            String cachedVideos = sharedPreferences.getString("videos_" + currentCategoryId, null);
            if (cachedVideos == null) {
                showToast("获取视频数据失败，状态码: " + response.code());
            }
        }
    }
    }

    /**
     * 更新视频列表
     * @param newVideos 新的视频数据
     */
    private void updateVideoList(List<VideoItem> newVideos) {
        runOnUiThread(() -> {
            if (currentPage == 0) {
                videoItems.clear();
                rvVideoList.scrollToPosition(0); // 滚动到顶部
            }

            if (newVideos != null && !newVideos.isEmpty()) {
                videoItems.addAll(newVideos);
                videoListAdapter.notifyDataSetChanged();
                currentPage++;
                hasMoreData = true; // 假设还有更多数据，直到API返回空
            } else {
                hasMoreData = false;
                showToast("已经到底了");
            }
        });
    }

    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(VideoActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * 获取视频分类数据
     */
    private void fetchVideoCategories() {
        String url = API_BASE_URL + "/videogroups";

        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "获取视频分类失败: " + e.getMessage());
                String cachedCategories = sharedPreferences.getString("video_categories", null);
                if (cachedCategories == null) {
                    showToast("获取视频分类失败，请重试");
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBodyObj = response.body()) {
                    if (response.isSuccessful() && responseBodyObj != null) {
                        String responseBody = responseBodyObj.string();
                        parseAndUpdateCategories(responseBody, true);
                    } else {
                        AppLogger.e(TAG, "获取视频分类失败，状态码: " + response.code());
                        String cachedCategories = sharedPreferences.getString("video_categories", null);
                        if (cachedCategories == null) {
                            showToast("获取视频分类失败，状态码: " + response.code());
                        }
                    }
                }
            }
        });
    }

    /**
     * 解析并更新视频分类
     * @param responseBody 响应体
     */
    private void parseAndUpdateCategories(String responseBody, boolean fromRemote) {
        try {
            if (fromRemote) {
                sharedPreferences.edit().putString("video_categories", responseBody).apply();
            }

            java.lang.reflect.Type listType = new TypeToken<List<VideoCategory>>(){}.getType();
            List<VideoCategory> newCategories = gson.fromJson(responseBody, listType);

            updateCategoryList(newCategories, fromRemote);
        } catch (Exception e) {
            AppLogger.e(TAG, "解析视频分类失败: " + e.getMessage());
            if (fromRemote) {
                showToast("解析视频分类失败");
            }
        }
    }

    /**
     * 更新分类列表
     * @param newCategories 新的分类数据
     */
    private void updateCategoryList(List<VideoCategory> newCategories, boolean fromRemote) {
        runOnUiThread(() -> {
            videoCategories.clear();
            videoCategories.addAll(newCategories);

            if (!videoCategories.isEmpty()) {
                videoCategories.get(0).setSelected(true);
                currentCategoryId = videoCategories.get(0).getId();
                if (fromRemote) {
                    refreshVideoList();
                }
            }

            videoCategoryAdapter.notifyDataSetChanged();
        });
    }
}