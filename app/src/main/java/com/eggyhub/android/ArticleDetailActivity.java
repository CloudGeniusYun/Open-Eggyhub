package com.eggyhub.android;

import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 文章详情Activity
 * 用于展示单篇文章的详细内容
 */
public class ArticleDetailActivity extends BaseActivity {

    /**
     * 日志标签
     */
    private static final String TAG = "ArticleDetailActivity";
    /**
     * 文章标题TextView
     */
    private TextView detailTitle;
    /**
     * 文章作者TextView
     */
    private TextView detailAuthor;
    /**
     * 用于显示文章内容的WebView
     */
    private WebView detailWebView;

    /**
     * Activity创建时调用的方法
     * 初始化界面并获取传递的文章数据
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail); // 设置布局文件

        // 初始化视图组件
        detailTitle = findViewById(R.id.detailTitle);
        detailAuthor = findViewById(R.id.detailAuthor);
        detailWebView = findViewById(R.id.detailWebView);

        // 启用WebView中的JavaScript
        detailWebView.getSettings().setJavaScriptEnabled(true);
        // 设置WebViewClient以在WebView内部处理页面导航
        detailWebView.setWebViewClient(new WebViewClient());

        // 从Intent获取数据
        String title = getIntent().getStringExtra("title");
        String author = getIntent().getStringExtra("author");
        int articleId = getIntent().getIntExtra("id", -1);

        // 设置标题和作者
        detailTitle.setText(title);
        detailAuthor.setText(author);

        // 如果文章ID有效，则获取文章内容
        if (articleId != -1) {
            fetchArticleContent(articleId);
        } else {
            // 文章ID无效的情况
            AppLogger.e(TAG, "Invalid article ID");
        }
    }

    /**
     * 获取文章内容
     * @param articleId 文章ID
     */
    private void fetchArticleContent(int articleId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        // 构建API请求URL
        String url = "https://eggyhub.top/api/article?id=" + articleId;

        // 创建请求对象
        Request request = new Request.Builder()
                .url(url)
                .build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            /**
             * 请求失败时调用
             * @param call 请求对象
             * @param e 异常信息
             */
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "Failed to fetch article content", e);
                // 在UI线程中显示错误信息
                runOnUiThread(() -> detailWebView.loadDataWithBaseURL("https://eggyhub.top/", "<html><body><h1>Error loading content.</h1></body></html>", "text/html", "UTF-8", null));
            }

            /**
             * 请求成功时调用
             * @param call 请求对象
             * @param response 响应对象
             * @throws IOException IO异常
             */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        // 响应成功，获取响应体
                        String bodyString = responseBody.string();
                        AppLogger.d(TAG, "Successfully fetched article content");

                        // 在UI线程中加载文章内容到WebView
                        runOnUiThread(() -> detailWebView.loadDataWithBaseURL("https://eggyhub.top/", bodyString, "text/html", "UTF-8", null));
                    } else {
                        // 响应不成功
                        AppLogger.e(TAG, "Failed to fetch article content: " + response.code());
                        // 在UI线程中显示错误信息
                        runOnUiThread(() -> detailWebView.loadDataWithBaseURL("https://eggyhub.top/", "<html><body><h1>Error: " + response.code() + "</h1></body></html>", "text/html", "UTF-8", null));
                    }
                }
            }
        });
    }
}