package com.eggyhub.android;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import com.eggyhub.android.log.AppLogger;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eggyhub.android.utils.PermissionUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 文件活动类，用于展示仓库中的文件列表
 * 支持文件预览和下载功能
 */
public class FileActivity extends BaseActivity {
    // 日志标签
    private static final String TAG = "FileActivity";
    // API基础URL
    private static final String BASE_URL = "https://eggyhub.top/api";

    // 成员变量
    private TextView mFilenameTextView;     // 文件名文本视图
    private TextView mDescriptionTextView;  // 描述文本视图
    private int mRepoId;                    // 仓库ID
    private Intent mIntent;                 // 意图对象
    private neterrAdap mNetworkAdapter;  // 网络错误适配器
    private String mAccessToken;            // 访问令牌
    private RecyclerView mFileRecyclerView; // 文件列表 recyclerView
    private SharedPreferences mPreferences; // 共享偏好设置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filefilefilefile);
        initViews();
        initData();
        fetchFileData();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        mFileRecyclerView = findViewById(R.id.file_recycler_view);
        mFilenameTextView = findViewById(R.id.filename_text_view);
        mDescriptionTextView = findViewById(R.id.description_text_view);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        mAccessToken = SecureStorageManager.getAccessToken();
        mNetworkAdapter = new neterrAdap("加载中");
        mIntent = getIntent();
    }

    /**
     * 获取文件数据
     */
    private void fetchFileData() {
        AppLogger.d(TAG, "开始获取文件数据");
        mFileRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFileRecyclerView.setAdapter(mNetworkAdapter);
        mRepoId = mIntent.getIntExtra("id", 0);

        // 创建OkHttpClient实例
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = BASE_URL + "/repos/" + mRepoId + "/files";

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "获取文件数据失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(FileActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mFileRecyclerView.setAdapter(mNetworkAdapter);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            try (ResponseBody responseBody = response.body()) {
                if (response.isSuccessful() && responseBody != null) {
                    String bodyString = responseBody.string();
                    AppLogger.d(TAG, "获取文件数据成功，响应体: " + bodyString);
                    handleFileResponse(bodyString);
                } else {
                    AppLogger.e(TAG, "获取文件数据失败，响应码: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(FileActivity.this, "请求失败，响应码: " + response.code(), Toast.LENGTH_SHORT).show();
                        mFileRecyclerView.setAdapter(mNetworkAdapter);
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
        try {
            Gson gson = new Gson();
            FileResponse fileResponse = gson.fromJson(responseBody, FileResponse.class);
            List<FileItem> fileList = new ArrayList<>();

            if (fileResponse != null) {
                AppLogger.d(TAG, "文件总数: " + fileResponse.getFileCount());
                AppLogger.d(TAG, "仓库ID: " + fileResponse.getRepoId());
                AppLogger.d(TAG, "请求成功: " + fileResponse.isSuccess());

                if (fileResponse.isSuccess()) {
                    updateRepositoryInfo(fileResponse);

                    if (fileResponse.getFiles() != null && !fileResponse.getFiles().isEmpty()) {
                        for (FileData fileData : fileResponse.getFiles()) {
                            FileItem fileItem = FileItem.fromFileData(fileData, mRepoId);
                            fileList.add(fileItem);
                            AppLogger.d(TAG, "添加文件: " + fileItem.getOriginalName() + ", 类型: " + fileItem.getFileType());
                        }
                    } else {
                        AppLogger.w(TAG, "文件列表为空");
                    }
                } else {
                    AppLogger.w(TAG, "仓库信息为空");
                }
                AppLogger.d(TAG, "用户ID: " + fileResponse.getUserId());
            } else {
                AppLogger.w(TAG, "响应解析为空");
            }

            final List<FileItem> finalFileList = fileList;
            runOnUiThread(() -> {
                if (!finalFileList.isEmpty()) {
                    setupFileAdapter(finalFileList);
                } else {
                    mFileRecyclerView.setAdapter(mNetworkAdapter);
                    Toast.makeText(FileActivity.this, "暂无文件", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            AppLogger.e(TAG, "解析文件数据异常: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(FileActivity.this, "数据解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                mFileRecyclerView.setAdapter(mNetworkAdapter);
            });
        }
    }

    /**
     * 更新仓库信息
     * @param fileResponse 文件响应对象
     */
    private void updateRepositoryInfo(FileResponse fileResponse) {
        runOnUiThread(() -> {
            mFilenameTextView.setText("仓库名: " + mIntent.getStringExtra("name"));
            mDescriptionTextView.setText("描述: " + mIntent.getStringExtra("ds"));
        });
    }

    /**
     * 设置文件适配器
     * @param fileList 文件列表
     */
    private void setupFileAdapter(List<FileItem> fileList) {
        FileAdapter fileAdapter = new FileAdapter(FileActivity.this, fileList);
        fileAdapter.setOnFileActionListener(new FileAdapter.OnFileActionListener() {
            @Override
            public void onPreviewClick(FileItem fileItem) {
                handleFilePreview(fileItem);
            }

            @Override
            public void onDownloadClick(FileItem fileItem) {
                handleFileDownload(fileItem);
            }
        });
        mFileRecyclerView.setAdapter(fileAdapter);
        mFileRecyclerView.setLayoutManager(new LinearLayoutManager(FileActivity.this));
    }

    /**
     * 处理文件预览
     * @param fileItem 文件项
     */
    private void handleFilePreview(FileItem fileItem) {
        try {
            // 获取预览链接
            String previewUrl = "https://eggyhub.top/" + fileItem.getPreviewUrl();
            AppLogger.d(TAG, "预览文件链接: " + previewUrl);
            Toast.makeText(FileActivity.this, "预览: " + fileItem.getOriginalName(), Toast.LENGTH_SHORT).show();

            // 使用应用内预览所有文件类型
            Intent intent = new Intent(FileActivity.this, FilePreviewActivity.class);
            intent.putExtra("file_url", previewUrl);
            intent.putExtra("file_type", fileItem.getFileType());
            intent.putExtra("token", mAccessToken);
            startActivity(intent);
        } catch (Exception e) {
            AppLogger.e(TAG, "预览文件异常: " + e.getMessage());
            Toast.makeText(FileActivity.this, "预览失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理文件下载
     * @param fileItem 文件项
     */
    private void handleFileDownload(FileItem fileItem) {
        try {
            // 检查存储权限
            if (!PermissionUtils.hasStoragePermission(this)) {
                // 请求存储权限
                PermissionUtils.showPermissionRationaleDialog(
                    this,
                    "下载文件需要存储权限，请允许应用访问存储空间",
                    () -> PermissionUtils.requestStoragePermission(FileActivity.this)
                );
                return;
            }
            
            // 获取下载链接
            String downloadUrl = "https://eggyhub.top/" + fileItem.getDownloadUrl();
            AppLogger.d(TAG, "下载文件链接: " + downloadUrl);
            Toast.makeText(FileActivity.this, "开始下载: " + fileItem.getOriginalName(), Toast.LENGTH_SHORT).show();

            // 启动下载任务
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));

            // 设置下载目录和文件名
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileItem.getOriginalName());
            request.setTitle(fileItem.getOriginalName());
            request.setDescription("正在下载...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // 添加请求头
            request.addRequestHeader("Authorization", "Bearer " + mAccessToken);

            // 开始下载
            downloadManager.enqueue(request);
        } catch (Exception e) {
            AppLogger.e(TAG, "下载文件异常: " + e.getMessage());
            Toast.makeText(FileActivity.this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 文件响应类，用于解析服务器返回的文件数据
     */
    private static class FileResponse {
        private int file_count;
        private List<FileData> files;
        private int repo_id;
        private boolean success;
        private String userId;

        public int getFileCount() {
            return file_count;
        }

        public void setFileCount(int file_count) {
            this.file_count = file_count;
        }

        public List<FileData> getFiles() {
            return files;
        }

        public void setFiles(List<FileData> files) {
            this.files = files;
        }

        public int getRepoId() {
            return repo_id;
        }

        public void setRepoId(int repo_id) {
            this.repo_id = repo_id;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    /**
     * 文件数据类，表示单个文件的详细信息
     */
    public static class FileData {
        private int file_id;
        private String file_size;
        private int file_size_kb;
        private String file_type;
        private String original_name;
        private int status;
        private String upload_time;

        public int getFileId() {
            return file_id;
        }

        public void setFileId(int file_id) {
            this.file_id = file_id;
        }

        public String getFileSize() {
            return file_size;
        }

        public void setFileSize(String file_size) {
            this.file_size = file_size;
        }

        public int getFileSizeKb() {
            return file_size_kb;
        }

        public void setFileSizeKb(int file_size_kb) {
            this.file_size_kb = file_size_kb;
        }

        public String getFileType() {
            return file_type;
        }

        public void setFileType(String file_type) {
            this.file_type = file_type;
        }

        public String getOriginalName() {
            return original_name;
        }

        public void setOriginalName(String original_name) {
            this.original_name = original_name;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getUploadTime() {
            return upload_time;
        }

        public void setUploadTime(String upload_time) {
            this.upload_time = upload_time;
        }
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
