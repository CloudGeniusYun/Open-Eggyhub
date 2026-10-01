package com.eggyhub.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.eggyhub.android.log.AppLogger;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 分享码管理活动
 * 负责分享码的查看、编辑、删除等操作
 */
public class ManagerCodeActivity extends BaseActivity {
    private static final String TAG = "ManagerCodeActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String REGEX_SHARE_CODE = "^2y[a-z0-9]{11}$";

    private String shareCode = "";
    private int codeId;
    private Intent intent;
    private TextView codeNameTextView;
    private TextView codeDescriptionTextView;
    private LinearLayout shareCodesContainer;
    private TextView codeGroupTextView;
    private CardView changeGroupCardView;
    private CardView deleteCardView;
    private CardView changeDescriptionCardView;
    private CardView newCodeCardView;
    private ImageView coverImageView;
    private androidx.core.widget.NestedScrollView nestedScrollView;
    private SharedPreferences preferences;
    private String accessToken;
    private int selectedGroup;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manger_cd);

        // 初始化成员变量
        httpClient = OkHttpClientFactory.getSharedClient();
        intent = getIntent();
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        accessToken = SecureStorageManager.getAccessToken();
        codeId = intent.getIntExtra("id", 0);

        // 初始化UI组件
        initViews();

        setupButtonListeners();

        // 设置初始数据
        updateUIWithIntentData();

        // 加载分享码列表
        fetchShareCodeList();
    }

    /**
     * 初始化UI组件
     */
    private void initViews() {
        codeNameTextView = findViewById(R.id.tv_title);
        codeDescriptionTextView = findViewById(R.id.tv_description);
        shareCodesContainer = findViewById(R.id.ll_share_codes_container);
        codeGroupTextView = findViewById(R.id.tv_group);
        changeGroupCardView = findViewById(R.id.cardCodechgr);
        deleteCardView = findViewById(R.id.cardCodedel);
        changeDescriptionCardView = findViewById(R.id.cardCodechds);
        newCodeCardView = findViewById(R.id.cardCodenew);
        coverImageView = findViewById(R.id.iv_cover_image);
        nestedScrollView = findViewById(R.id.nested_scroll_view);

        // 初始化返回按钮并设置点击事件
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkTutorialStatus();
    }

    private void checkTutorialStatus() {
        TutorialManager manager = TutorialManager.getInstance(this);
        if (manager.isTutorialRunning() && TutorialManager.TUTORIAL_SUPPLEMENT_CODE.equals(manager.getCurrentTutorial())) {
            int stepIndex = manager.getStepIndex();
            if (stepIndex == 4 && newCodeCardView != null) {
                newCodeCardView.post(() -> {
                    // 确保按钮可见，如果不可见则滚动
                    if (nestedScrollView != null) {
                        int[] location = new int[2];
                        newCodeCardView.getLocationInWindow(location);
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        
                        // 如果按钮在屏幕下方不可见，则滚动到底部
                        if (location[1] + newCodeCardView.getHeight() > screenHeight) {
                            nestedScrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                    
                    GuideHelper.show(this, newCodeCardView, "最后一步：点击“补充分享码”上传新的2y分享码", false, () -> {
                        manager.finishTutorial();
                    });
                });
            }
        }
    }

    /**
     * 设置RecyclerView
     */


    /**
     * 设置按钮监听器
     */
    private void setupButtonListeners() {
        changeGroupCardView.setOnClickListener(v -> showGroupSelectionDialog());
        changeDescriptionCardView.setOnClickListener(v -> showChangeDescriptionDialog());
        newCodeCardView.setOnClickListener(v -> showUploadCodeDialog());
        deleteCardView.setOnClickListener(v -> showDeleteAllConfirmationDialog());
    }

    /**
     * 使用Intent数据更新UI
     */
    private void updateUIWithIntentData() {
        codeNameTextView.setText(intent.getStringExtra("name"));
        codeDescriptionTextView.setText("描述：" + intent.getStringExtra("ds"));
        setGroupText(intent.getIntExtra("gr", 0));

        String coverUrl = intent.getStringExtra("cover");
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl).into(coverImageView);
        }

        AppLogger.d(TAG, "Code ID: " + codeId);
    }

    /**
     * 获取分享码列表
     */
    private void fetchShareCodeList() {
        String url = "https://eggyhub.top/api/all_codes?id=" + codeId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "Failed to fetch share codes", e);
                runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "获取分享码列表失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        try {
                            String jsonData = responseBody.string();
                            Type listType = new TypeToken<List<DisplayShareCodeItem>>() {}.getType();
                            List<DisplayShareCodeItem> shareCodeList = gson.fromJson(jsonData, listType);
                            if (shareCodeList == null) shareCodeList = new ArrayList<>();

                            final List<DisplayShareCodeItem> finalShareCodeList = shareCodeList;
                            runOnUiThread(() -> displayShareCodes(finalShareCodeList));
                        } catch (Exception e) {
                            AppLogger.e(TAG, "JSON parsing error", e);
                            runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "数据解析错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        AppLogger.e(TAG, "Failed to fetch share codes, response code: " + response.code());
                        runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "获取分享码列表失败", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void displayShareCodes(List<DisplayShareCodeItem> shareCodeList) {
        shareCodesContainer.removeAllViews(); // Clear existing views
        for (DisplayShareCodeItem item : shareCodeList) {
            // Inflate the list_item_share_code.xml layout
            View itemView = getLayoutInflater().inflate(R.layout.list_item_share_code, shareCodesContainer, false);

            TextView textViewShareCode = itemView.findViewById(R.id.textViewShareCode);
            TextView buttonDelete = itemView.findViewById(R.id.buttonDelete);

            textViewShareCode.setText(item.getCode());

            buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog(item.getId(), item.getCode()));

            shareCodesContainer.addView(itemView);
        }
    }

    /**
     * 显示分类选择对话框
     */
    private void showGroupSelectionDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_sharecode_category, null);
        RecyclerView recyclerViewCategories = dialogView.findViewById(R.id.recyclerViewCategories);
        
        // 分类数据
        final String[] options = {"默认分类", "玩法逻辑", "蛋码技能", "精美模型", "整活区", "界面控件"};
        final int[] groupValues = {1, 2, 3, 4, 5, 6};
        
        // 设置RecyclerView
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        CategoryAdapter adapter = new CategoryAdapter(options, groupValues);
        recyclerViewCategories.setAdapter(adapter);
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        AlertDialog dialog = builder.create();
        dialog.setView(dialogView, 0, 0, 0, 0); // 移除默认边距

        // 显示对话框
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        
        // 设置适配器点击监听
        adapter.setOnItemClickListener((position, categoryName) -> {
            int selectedGroupValue = groupValues[position];
            updateCodeGroup(selectedGroupValue);
            dialog.dismiss();
        });
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        // 确认按钮在分类选择中不需要，因为点击分类项就直接确认了
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        if (btnConfirm != null) {
            btnConfirm.setVisibility(View.GONE);
        }
    }

    /**
     * 更新分享码分类
     */
    private void updateCodeGroup(int groupId) {
        String url = "https://eggyhub.top/api/alter_sharegroup?id=" + codeId + "&grid=" + groupId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "Failed to update code group", e);
                runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "更新分类失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ManagerCodeActivity.this, "更新分类成功", Toast.LENGTH_SHORT).show();
                        setGroupText(groupId);
                    });
                } else {
                    AppLogger.e(TAG, "Failed to update code group, response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "更新分类失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * 设置分类文本
     */
    private void setGroupText(int groupId) {
        String groupName;
        switch (groupId) {
            case 1:
                groupName = "默认分类";
                break;
            case 2:
                groupName = "玩法逻辑";
                break;
            case 3:
                groupName = "蛋码技能";
                break;
            case 4:
                groupName = "精美模型";
                break;
            case 5:
                groupName = "整活区";
                break;
            case 6:
                groupName = "界面控件";
                break;
            default:
                groupName = "未知分类";
        }
        codeGroupTextView.setText("分类：" + groupName);
        selectedGroup = groupId;
    }

    /**
     * 显示修改描述对话框
     */
    private void showChangeDescriptionDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_sharecode_description, null);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        String currentDescription = codeDescriptionTextView.getText().toString().replace("描述：", "");
        editTextDescription.setText(currentDescription);
        
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
        editTextDescription.requestFocus();
        editTextDescription.setSelection(editTextDescription.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String newDescription = editTextDescription.getText().toString().trim();
                if (!newDescription.isEmpty()) {
                    updateCodeDescription(newDescription, dialog);
                    codeDescriptionTextView.setText("描述：" + newDescription);
                } else {
                    Toast.makeText(ManagerCodeActivity.this, "描述不能为空", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 更新分享码描述
     */
    private void updateCodeDescription(String newDescription, AlertDialog dialog) {
        UpdateDescriptionRequest updateRequest = new UpdateDescriptionRequest(newDescription);
        RequestBody body = RequestBody.create(gson.toJson(updateRequest), JSON);
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/alter_sharedes?id=" + codeId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "Failed to update description", e);
                runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "网络错误", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(ManagerCodeActivity.this, "更新描述成功", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } else {
                        AppLogger.e(TAG, "Failed to update description, response code: " + response.code());
                        runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "更新描述失败", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /**
     * 显示上传分享码对话框
     */
    private void showUploadCodeDialog() {
        // 创建自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_sharecode, null);
        EditText editTextShareCode = dialogView.findViewById(R.id.editTextShareCode);
        
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
        editTextShareCode.requestFocus();
        editTextShareCode.setSelection(editTextShareCode.getText().length());
        
        // 设置自定义按钮点击事件
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String code = editTextShareCode.getText().toString().trim();
                if (code.isEmpty()) {
                    Toast.makeText(ManagerCodeActivity.this, "分享码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (validateShareCode(code)) {
                    dialog.dismiss();
                    uploadShareCode(code);
                }
            });
        }
    }

    /**
     * 验证分享码格式
     */
    private boolean validateShareCode(String code) {
        // 检查是否为空
        if (code == null || code.isEmpty()) {
            Toast.makeText(this, "分享码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 特殊 ID 校验：如果 ID 为 983，跳过格式校验
        if (codeId == 983) {
            return true;
        }

        // 检查是否是多个分享码
        if (code.length() > 13 && code.length() % 13 == 0) {
            int segmentCount = code.length() / 13;
            for (int i = 0; i < segmentCount; i++) {
                String segment = code.substring(i * 13, (i + 1) * 13);
                if (!segment.matches(REGEX_SHARE_CODE)) {
                    Toast.makeText(this, "第" + (i + 1) + "段分享码格式不正确", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            return true;
        } else {
            // 单个分享码验证
            if (code.matches(REGEX_SHARE_CODE)) {
                return true;
            } else {
                Toast.makeText(this, "分享码格式不正确，请检查", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }

    /**
     * 上传分享码
     */
    private void uploadShareCode(String code) {
        final AtomicInteger processedCount = new AtomicInteger(0);

        // 如果 ID 为 983，不进行 13 位切割，直接作为单条上传
        if (codeId == 983) {
            uploadSingleShareCode(code, processedCount, new AtomicInteger(0), 1);
            return;
        }

        // 检查是否是多个分享码
        if (code.length() > 13 && code.length() % 13 == 0) {
            int segmentCount = code.length() / 13;
            final AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < segmentCount; i++) {
                String segment = code.substring(i * 13, (i + 1) * 13);
                uploadSingleShareCode(segment, processedCount, successCount, segmentCount);
            }
        } else {
            // 单个分享码上传
            uploadSingleShareCode(code, processedCount, new AtomicInteger(0), 1);
        }
    }

    /**
     * 上传单个分享码
     */
    private void uploadSingleShareCode(String code, AtomicInteger processedCount, AtomicInteger successCount, int totalCount) {
        UploadCodeRequest uploadRequest = new UploadCodeRequest(code, String.valueOf(codeId));
        String jsonBody = gson.toJson(uploadRequest);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/gifts/update")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "Failed to upload share code", e);
                processedCount.incrementAndGet();
                checkUploadCompletion(processedCount, successCount, totalCount);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        successCount.incrementAndGet();
                    }
                    processedCount.incrementAndGet();
                    checkUploadCompletion(processedCount, successCount, totalCount);
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * 检查上传是否完成
     */
    private void checkUploadCompletion(AtomicInteger processedCount, AtomicInteger successCount, int totalCount) {
        if (processedCount.get() == totalCount) {
            runOnUiThread(() -> {
                if (successCount.get() == totalCount) {
                    Toast.makeText(ManagerCodeActivity.this, "所有分享码上传成功", Toast.LENGTH_SHORT).show();
                } else if (successCount.get() > 0) {
                    Toast.makeText(ManagerCodeActivity.this, "部分分享码上传成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManagerCodeActivity.this, "所有分享码上传失败", Toast.LENGTH_SHORT).show();
                }
                fetchShareCodeList(); // 刷新列表
            });
        }
    }

    /**
     * 显示删除单个分享码确认对话框
     */
    private void showDeleteConfirmationDialog(int id, String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("是否确认删除分享码，内容为：" + code);

        builder.setPositiveButton("确认", (dialog, which) -> deleteSingleShareCode(id));
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
    }

    /**
     * 删除单个分享码
     */
    private void deleteSingleShareCode(int id) {
        String url = "https://eggyhub.top/api/del_code?id=" + id;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "Failed to delete share code", e);
                runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "网络错误", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(ManagerCodeActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            fetchShareCodeList(); // 刷新列表
                        });
                    } else {
                        AppLogger.e(TAG, "Failed to delete share code, response code: " + response.code());
                        runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "删除失败", Toast.LENGTH_SHORT).show());
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * 显示删除所有分享码确认对话框
     */
    private void showDeleteAllConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("是否确认删除此分享码");

        builder.setPositiveButton("确认", (dialog, which) -> {
            deleteAllShareCodes();
            Intent intent = new Intent(ManagerCodeActivity.this, MangerActivity.class);
            startActivity(intent);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // 显式设置按钮文本颜色，确保可见
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
    }

    /**
     * 删除所有分享码
     */
    private void deleteAllShareCodes() {
        String url = "https://eggyhub.top/api/share_del?id=" + codeId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppLogger.e(TAG, "Failed to delete all share codes", e);
                runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "网络错误", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(ManagerCodeActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            fetchShareCodeList(); // 刷新列表
                        });
                    } else {
                        AppLogger.e(TAG, "Failed to delete all share codes, response code: " + response.code());
                        runOnUiThread(() -> Toast.makeText(ManagerCodeActivity.this, "删除失败", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /**
     * 分享码列表项数据类
     */
    public static class DisplayShareCodeItem {
        private int id;
        private String code;

        public DisplayShareCodeItem(int id, String code) {
            this.id = id;
            this.code = code;
        }

        public int getId() {
            return id;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * 分享码列表适配器
     */
    public static class ShareCodeListAdapter extends RecyclerView.Adapter<ShareCodeListAdapter.ViewHolder> {
        private List<DisplayShareCodeItem> shareCodeList;
        private OnDeleteClickListener onDeleteClickListener;

        public interface OnDeleteClickListener {
            void onDeleteClick(int id, String code);
        }

        public ShareCodeListAdapter(List<DisplayShareCodeItem> shareCodeList) {
            this.shareCodeList = shareCodeList;
        }

        public void setOnDeleteClickListener(OnDeleteClickListener listener) {
            this.onDeleteClickListener = listener;
        }

        public void updateList(List<DisplayShareCodeItem> newList) {
            this.shareCodeList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(), R.layout.item_share_code, null);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DisplayShareCodeItem item = shareCodeList.get(position);
            holder.codeTextView.setText(item.getCode());
            holder.deleteButton.setOnClickListener(v -> {
                if (onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(item.getId(), item.getCode());
                }
            });
        }

        @Override
        public int getItemCount() {
            return shareCodeList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView codeTextView;
            Button deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                codeTextView = itemView.findViewById(R.id.tv_share_code_name);
                deleteButton = itemView.findViewById(R.id.btn_claim);
            }
        }
    }

    /**
     * 分类适配器
     */
    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final String[] categories;
        private final int[] categoryValues;
        private OnItemClickListener onItemClickListener;

        public CategoryAdapter(String[] categories, int[] categoryValues) {
            this.categories = categories;
            this.categoryValues = categoryValues;
        }

        public interface OnItemClickListener {
            void onItemClick(int position, String categoryName);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.onItemClickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.categoryNameTextView.setText(categories[position]);
            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position, categories[position]);
                }
            });
        }

        @Override
        public int getItemCount() {
            return categories.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView categoryNameTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                categoryNameTextView = itemView.findViewById(R.id.textViewCategoryName);
            }
        }
    }
}
