package com.eggyhub.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 仓库列表Activity
 * 用于展示仓库列表并处理相关交互
 */
public class FileRepoActivity extends BaseActivity {
    // 日志标签
    private static final String TAG = "FileRepoActivity";
    // 超时时间(秒)
    private static final int TIMEOUT = 10;
    // API基础URL
    private static final String BASE_URL = "https://eggyhub.top/api";
    // 仓库列表API路径
    private static final String REPO_LIST_URL = BASE_URL + "/reposlist?page=1&all=1";

    private static final String KEY_REPO_LIST = "repo_list";

    // 成员变量
    private RecyclerView mRecyclerView;           // 仓库列表RecyclerView
    private FileRepoAdapter mAdapter;             // 仓库列表适配器
    private List<RepoItem> mRepoList;             // 仓库数据列表
    private OkHttpClient mOkHttpClient;           // OkHttp客户端
    private Call mRepoCall;                       // 仓库数据请求
    private SharedPreferences mPreferences;       // 共享偏好设置
    private String mAccessToken;                  // 访问令牌
    private neterrAdap mNetAdapter;               // 网络错误适配器
    private ImageButton mBackButton;              // 返回按钮
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filerepo);

        initViews();
        initData();
        setupListeners();
        loadLocalRepoData();
        fetchRepoData();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        mBackButton = findViewById(R.id.back_button);
        mRecyclerView = findViewById(R.id.file_recycler_view);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 初始化 SharedPreferences
        mPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        mAccessToken = SecureStorageManager.getAccessToken();
        sharedPreferences = getSharedPreferences("FileRepoActivity", MODE_PRIVATE);

        // 初始化OkHttpClient（使用工厂类，自动添加代理拦截器）
        mOkHttpClient = OkHttpClientFactory.createCustomClient(TIMEOUT, TIMEOUT, TIMEOUT);

        // 初始化加载中适配器
        mNetAdapter = new neterrAdap("加载中");

        // 初始化RecyclerView
        initRecyclerView();
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mNetAdapter);
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 设置返回按钮点击事件
        mBackButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadLocalRepoData() {
        String cachedRepoList = sharedPreferences.getString(KEY_REPO_LIST, null);
        if (cachedRepoList != null) {
            Gson gson = new Gson();
            mRepoList = gson.fromJson(cachedRepoList, new TypeToken<List<RepoItem>>() {}.getType());
            if (mRepoList != null && !mRepoList.isEmpty()) {
                setupRepoAdapter(mRepoList);
            }
        }
    }

    /**
     * 获取仓库数据
     */
    private void fetchRepoData() {
        AppLogger.d(TAG, "开始获取仓库数据");

        Request.Builder requestBuilder = new Request.Builder()
                .url(REPO_LIST_URL)
                .get();

        // 添加认证令牌（如果有）
        if (mAccessToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + mAccessToken);
        }

        Request request = requestBuilder.build();

        mRepoCall = mOkHttpClient.newCall(request);
        mRepoCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) {
                    AppLogger.d(TAG, "请求已取消");
                    return;
                }

                AppLogger.e(TAG, "获取仓库数据失败: " + e.getMessage());
                runOnUiThread(() -> {
                    if (mRepoList == null || mRepoList.isEmpty()) {
                        Toast.makeText(FileRepoActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        mRecyclerView.setAdapter(mNetAdapter);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (call.isCanceled()) {
                    AppLogger.d(TAG, "请求已取消");
                    if (response.body() != null) {
                        response.body().close();
                    }
                    return;
                }

                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        String responseData = responseBody.string();
                        AppLogger.d(TAG, "获取仓库数据成功，响应体: " + responseData);

                        // 解析数据
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);

                        // 检查请求是否成功
                        if (jsonObject.get("success").getAsBoolean()) {
                            // 解析仓库列表
                            String reposJson = jsonObject.get("repos").toString();
                            sharedPreferences.edit().putString(KEY_REPO_LIST, reposJson).apply();
                            mRepoList = gson.fromJson(
                                    reposJson,
                                    new TypeToken<List<RepoItem>>() {}.getType()
                            );

                            final List<RepoItem> finalRepoList = mRepoList;
                            runOnUiThread(() -> {
                                if (finalRepoList != null && !finalRepoList.isEmpty()) {
                                    setupRepoAdapter(finalRepoList);
                                } else {
                                    mRecyclerView.setAdapter(mNetAdapter);
                                    Toast.makeText(FileRepoActivity.this, "暂无仓库数据", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            String errorMsg = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "获取数据失败";
                            AppLogger.e(TAG, "获取仓库数据失败: " + errorMsg);
                            runOnUiThread(() -> {
                                if (mRepoList == null || mRepoList.isEmpty()) {
                                    Toast.makeText(FileRepoActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                    mRecyclerView.setAdapter(mNetAdapter);
                                }
                            });
                        }
                    } else {
                        AppLogger.e(TAG, "获取仓库数据失败，响应码: " + response.code());
                        runOnUiThread(() -> {
                            if (mRepoList == null || mRepoList.isEmpty()) {
                                Toast.makeText(FileRepoActivity.this, "请求失败，响应码: " + response.code(), Toast.LENGTH_SHORT).show();
                                mRecyclerView.setAdapter(mNetAdapter);
                            }
                        });
                    }
                } catch (Exception e) {
                    AppLogger.e(TAG, "解析仓库数据异常: " + e.getMessage());
                    runOnUiThread(() -> {
                        if (mRepoList == null || mRepoList.isEmpty()) {
                            Toast.makeText(FileRepoActivity.this, "数据解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            mRecyclerView.setAdapter(mNetAdapter);
                        }
                    });
                }
            }
        });
    }

    /**
     * 设置仓库列表适配器
     * @param repoList 仓库数据列表
     */
    private void setupRepoAdapter(List<RepoItem> repoList) {
        mAdapter = new FileRepoAdapter(FileRepoActivity.this, repoList, new FileRepoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int repoId, String repoName, String description) {
                navigateToFileActivity(repoId, repoName, description);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * 导航到文件Activity
     * @param repoId 仓库ID
     * @param repoName 仓库名称
     * @param description 仓库描述
     */
    private void navigateToFileActivity(int repoId, String repoName, String description) {
        Intent intent = new Intent(FileRepoActivity.this, FileActivity.class);
        intent.putExtra("id", repoId);
        intent.putExtra("name", repoName);
        intent.putExtra("ds", description);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消网络请求
        if (mRepoCall != null && !mRepoCall.isCanceled()) {
            mRepoCall.cancel();
            AppLogger.d(TAG, "取消仓库数据请求");
        }
    }

    /**
     * 仓库响应类
     */
    private static class RepoResponse {
        private List<RepoItem> repos;
        private boolean success;
        private String message;

        public List<RepoItem> getRepos() {
            return repos;
        }

        public void setRepos(List<RepoItem> repos) {
            this.repos = repos;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }   
}
