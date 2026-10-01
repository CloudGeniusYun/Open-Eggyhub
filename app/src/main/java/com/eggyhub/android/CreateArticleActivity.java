package com.eggyhub.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.Gson;

import com.eggyhub.android.utils.OkHttpClientFactory;
import com.eggyhub.android.PublishArticleRequest;
import com.eggyhub.android.BasicResponse;
import java.io.IOException;

/**
 * 文章创建与编辑Activity
 * 提供文章标题和内容的编辑功能，支持HTML预览、分类选择和发布功能
 */
public class CreateArticleActivity extends BaseActivity {
    private static final String ARTICLE_PREFS = "article_prefs";
    private static final String USER_PREFS = "user_prefs";
    private static final String KEY_SELECTED_CATEGORY_NAME = "selected_category_name";
    private static final String KEY_SELECTED_CATEGORY_GROUP = "selected_category_group";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final int DEFAULT_CATEGORY_GROUP = 1;
    private static final String DEFAULT_CATEGORY_NAME = "默认分类";

    private Button mBtnHtml;          // HTML预览按钮
    private Button mBtnPublish;       // 发布按钮
    private Button mBtnDescription;   // 书写说明按钮
    private Button mBtnCategory;      // 分类选择按钮
    private EditText mEtContent;      // 文章内容编辑框
    private EditText mArticleTitleEditText; // 文章标题编辑框
    private int mSelectedGroup;       // 选中的分类组ID
    private ImageButton mBackButton;  // 返回按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_article);

        // 初始化选中的分类组为默认值
        mSelectedGroup = -1;

        // 初始化视图
        initViews();
        // 设置监听器
        setupListeners();
        // 初始化返回按钮
        initBackButton();
        // 处理传入的意图数据
        handleIntentData();
        // 初始化分类选择
        initCategorySelection();
    }

    /**
     * 初始化所有视图组件
     */
    private void initViews() {
        mBtnHtml = findViewById(R.id.html_button);
        mBtnPublish = findViewById(R.id.publish_button);
        mBtnDescription = findViewById(R.id.description_button);
        mBtnCategory = findViewById(R.id.category_button);
        mEtContent = findViewById(R.id.article_content_edit_text);
        mArticleTitleEditText = findViewById(R.id.article_title_edit_text);
    }

    /**
     * 初始化返回按钮并设置点击事件
     */
    private void initBackButton() {
        mBackButton = findViewById(R.id.back_button);
        mBackButton.setOnClickListener(v -> finish());
    }

    /**
     * 处理传入的意图数据，用于编辑现有文章
     */
    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            String articleTitle = bundle.getString("articleTitle");
            String articleContent = bundle.getString("articleContent");

            // 设置文章标题和内容
            mArticleTitleEditText.setText(articleTitle);
            mEtContent.setText(articleContent);
        }
    }

    /**
     * 初始化分类选择
     */
    private void initCategorySelection() {
        // 设置默认分类
        mSelectedGroup = DEFAULT_CATEGORY_GROUP;
        mBtnCategory.setText(DEFAULT_CATEGORY_NAME);

        // 缓存选择项
        SharedPreferences sharedPref = getSharedPreferences(ARTICLE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_SELECTED_CATEGORY_NAME, DEFAULT_CATEGORY_NAME);
        editor.putInt(KEY_SELECTED_CATEGORY_GROUP, DEFAULT_CATEGORY_GROUP);
        editor.apply();
    }

    /**
     * 设置所有按钮的监听器
     */
    private void setupListeners() {
        setupHtmlButtonListener();
        setupPublishButtonListener();
        setupDescriptionButtonListener();
        setupCategoryButtonListener();
    }

    /**
     * 设置HTML预览按钮的监听器
     */
    private void setupHtmlButtonListener() {
        mBtnHtml.setOnClickListener(v -> {
            resetButtonStates();
            mBtnHtml.setSelected(true);
            showHtmlPreviewDialog();
        });
    }

    /**
     * 显示HTML预览对话框
     */
    private void showHtmlPreviewDialog() {
        final Dialog dialog = new Dialog(CreateArticleActivity.this);
        dialog.setContentView(R.layout.dialog_webview);
        dialog.setTitle("HTML预览");

        WebView webView = dialog.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);

        // 加载编辑区内容
        String htmlContent = mEtContent.getText().toString();
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null);

        dialog.show();
        // 设置对话框大小为屏幕宽度的90%和高度的70%
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int)(displayMetrics.widthPixels * 0.9);
        int height = (int)(displayMetrics.heightPixels * 0.7);
        dialog.getWindow().setLayout(width, height);
    }

    /**
     * 设置发布按钮的监听器
     */
    private void setupPublishButtonListener() {
        mBtnPublish.setOnClickListener(v -> {
            resetButtonStates();
            mBtnPublish.setSelected(true);
            publishArticle();
        });
    }

    /**
     * 发布文章
     */
    private void publishArticle() {
        String articleTitle = mArticleTitleEditText.getText().toString().trim();
        String articleContent = mEtContent.getText().toString().trim();

        // 验证输入
        if (articleTitle.isEmpty()) {
            Toast.makeText(CreateArticleActivity.this, "文章还没有名字呢", Toast.LENGTH_SHORT).show();
            return;
        }

        if (articleContent.isEmpty()) {
            Toast.makeText(CreateArticleActivity.this, "文章没有内容吗？", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mSelectedGroup == -1) {
            Toast.makeText(CreateArticleActivity.this, "给文章选一个分类吧", Toast.LENGTH_SHORT).show();
            return;
        }

        // 发送POST请求到服务器
        sendArticleToServer(articleTitle, articleContent);
    }

    /**
     * 将文章发送到服务器
     * @param title 文章标题
     * @param content 文章内容
     */
    private void sendArticleToServer(String title, String content) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        PublishArticleRequest requestModel = new PublishArticleRequest("unstaged1", title, 1, content, mSelectedGroup);
        Gson gson = new Gson();
        String jsonString = gson.toJson(requestModel);

        // 从加密存储读取 access_token
        String token = SecureStorageManager.getAccessToken();

        RequestBody body = RequestBody.create(
                jsonString,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/publish")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> 
                    Toast.makeText(CreateArticleActivity.this, "上传失败，请检查网络连接", Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful()) {
                        String responseBody = body.string();
                        try {
                            BasicResponse responseModel = gson.fromJson(responseBody, BasicResponse.class);
                            String message = responseModel != null ? responseModel.getMessage() : "";
                            runOnUiThread(() -> {
                                Toast.makeText(CreateArticleActivity.this, message, Toast.LENGTH_SHORT).show();
                                finish(); // 关闭当前Activity
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> 
                                Toast.makeText(CreateArticleActivity.this, "响应解析失败", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } else if (response.code() == 500) {
                        runOnUiThread(() -> 
                            Toast.makeText(CreateArticleActivity.this, "上传失败，请检查文章名是否重复", Toast.LENGTH_SHORT).show()
                        );
                    } else {
                        runOnUiThread(() -> 
                            Toast.makeText(CreateArticleActivity.this, "上传失败，错误码: " + response.code(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    /**
     * 设置书写说明按钮的监听器
     */
    private void setupDescriptionButtonListener() {
        mBtnDescription.setOnClickListener(v -> {
            resetButtonStates();
            mBtnDescription.setSelected(true);
            showWritingGuideDialog();
        });
    }

    /**
     * 显示书写教程对话框
     */
    private void showWritingGuideDialog() {
        final Dialog dialog = new Dialog(CreateArticleActivity.this);
        dialog.setContentView(R.layout.dialog_webview);
        dialog.setTitle("书写教程");

        WebView webView = dialog.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);

        String htmlContent = "<h1 style=\"color: #1a56db; text-align: center; border-bottom: 2px solid #1a56db; padding-bottom: 10px;\">书写教程</h1><p style=\"line-height: 1.6;\">在这里可以书写教程文章，分享你的经验！文章发布后，再次发布会发出新的一篇文章（标题差的不多，管理员会帮你删以前版本）<br>文章格式支持HTML（右侧按钮可预览），我会贴心地帮你去掉html里的某些标签哦。</p><h1 style=\"color: #1a56db; text-align: center; border-bottom: 2px solid #1a56db; padding-bottom: 10px;\">HTML格式</h1><div style=\"background-color: #e6f7ff; border-radius: 5px; padding: 15px; margin-bottom: 20px;\"> <h2 style=\"color: #1a56db; margin-top: 0;\">什么是HTML？</h2> <p style=\"line-height: 1.6;\">HTML（超文本标记语言）是一种用于创建网页的标准标记语言。它使用标签来描述网页的结构和内容。</p> </div> <div style=\"margin-bottom: 20px;\"> <h2 style=\"color: #1a56db;\">基本HTML结构</h2> <pre style=\"background-color: #f5f7fa; border-radius: 5px; padding: 10px; overflow-x: auto; font-size: 14px;\">         &lt;h1&gt;这是一个标题&lt;/h1&gt;         &lt;p&gt;这是一个段落。&lt;/p&gt;         &lt;p style=\"color:red;\"&gt;这是一个样式。&lt;/p&gt;    </pre> </div>";

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null);

        dialog.show();
        // 设置对话框大小为屏幕宽度的90%和高度的70%
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int)(displayMetrics.widthPixels * 0.9);
        int height = (int)(displayMetrics.heightPixels * 0.7);
        dialog.getWindow().setLayout(width, height);
    }

    /**
     * 设置分类按钮的监听器
     */
    private void setupCategoryButtonListener() {
        mBtnCategory.setOnClickListener(v -> {
            resetButtonStates();
            mBtnCategory.setSelected(true);
            showCategorySelectionDialog();
        });
    }

    /**
     * 显示分类选择对话框
     */
    private void showCategorySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择分类");

        // 创建 RadioGroup
        final RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(LinearLayout.VERTICAL);

        // 添加单选按钮
        String[] categories = {"默认分类", "蛋码基础", "蛋码技能", "蛋码与编程", "网站"};
        int[] groupValues = {1, 2, 3, 4, 5};

        for (int i = 0; i < categories.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(categories[i]);
            radioButton.setTag(groupValues[i]);
            radioGroup.addView(radioButton);

            // 选中当前分类
            if (groupValues[i] == mSelectedGroup) {
                radioButton.setChecked(true);
            }
        }

        // 设置确定按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    View child = radioGroup.getChildAt(i);
                    if (child.getId() == selectedId && child instanceof RadioButton) {
                        RadioButton selectedRadioButton = (RadioButton) child;
                        int selectedGroupValue = (int) selectedRadioButton.getTag();
                        String selectedCategory = selectedRadioButton.getText().toString();

                        // 缓存选择项
                        SharedPreferences sharedPref = getSharedPreferences(ARTICLE_PREFS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(KEY_SELECTED_CATEGORY_NAME, selectedCategory);
                        editor.putInt(KEY_SELECTED_CATEGORY_GROUP, selectedGroupValue);
                        editor.apply();
                        mSelectedGroup = selectedGroupValue;

                        // 更新按钮文本
                        mBtnCategory.setText(selectedCategory);
                        break;
                    }
                }
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        // 添加RadioGroup到对话框
        builder.setView(radioGroup);

        // 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
        // 设置按钮颜色
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.black));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.black));
    }

    /**
     * 重置所有按钮的选中状态
     */
    private void resetButtonStates() {
        mBtnHtml.setSelected(false);
        mBtnPublish.setSelected(false);
        mBtnDescription.setSelected(false);
        mBtnCategory.setSelected(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清除SharedPreferences中保存的分类信息
        SharedPreferences sharedPref = getSharedPreferences(ARTICLE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(KEY_SELECTED_CATEGORY_GROUP);
        editor.apply();
    }
}