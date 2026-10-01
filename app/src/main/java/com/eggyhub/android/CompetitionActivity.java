package com.eggyhub.android;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

import java.io.IOException;

/**
 * 竞赛活动页面
 * 用于展示竞赛相关内容，通过WebView加载竞赛数据
 */
public class CompetitionActivity extends BaseActivity {

    /**
     * 用于展示竞赛内容的WebView
     */
    private WebView webView;
    /**
     * OkHttpClient实例，用于网络请求
     */
    private OkHttpClient client = OkHttpClientFactory.getSharedClient();
    /**
     * 竞赛数据API URL
     */
    private static final String API_URL = "https://eggyhub.top/competition";

    /**
     * Activity创建时调用的方法
     * 初始化界面并设置WebView
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_competition); // 设置布局文件

        // 初始化WebView
        webView = findViewById(R.id.competition_webview);
        // 启用JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        // 设置WebViewClient以在WebView内部处理页面导航
        webView.setWebViewClient(new WebViewClient());

        // 获取竞赛数据
        fetchCompetitionData();
    }

    /**
     * 获取竞赛数据
     * 通过OkHttp从API获取竞赛内容并加载到WebView
     */
    private void fetchCompetitionData() {
        // 创建GET请求
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
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
                runOnUiThread(() -> {
                    webView.loadDataWithBaseURL(API_URL, "<html><body><h1>Error loading data.</h1></body></html>", "text/html", "utf-8", null);
                });
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
                    if (response.isSuccessful()) {
                        // 响应成功，获取响应数据
                        final String responseData = responseBody != null ? responseBody.string() : "";
                        // 在UI线程中加载数据到WebView
                        runOnUiThread(() -> {
                            webView.loadDataWithBaseURL(API_URL, responseData, "text/html", "utf-8", null);
                        });
                    } else {
                        // 响应不成功
                        runOnUiThread(() -> {
                            webView.loadDataWithBaseURL(API_URL, "<html><body><h1>Failed to load data: " + response.code() + "</h1></body></html>", "text/html", "utf-8", null);
                        });
                    }
                }
            }
        });
    }
}