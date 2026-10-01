package com.eggyhub.android;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GratitudeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gratitude);

        WebView webView = findViewById(R.id.webview_gratitude);
        // 获取 WebView 的设置
        android.webkit.WebSettings webSettings = webView.getSettings();

        // 启用 JavaScript
        webSettings.setJavaScriptEnabled(true);

        // 启用 DOM 存储，这对于一些网页的正常运行很重要
        webSettings.setDomStorageEnabled(true);


        // 允许混合内容（HTTPS 页面加载 HTTP 资源），这可能是图片不加载的原因之一
        webSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 设置 WebViewClient，处理各种通知和请求事件
        webView.setWebViewClient(new WebViewClient());

        // 设置 WebChromeClient，处理 JavaScript 对话框、网站图标、加载进度等
        webView.setWebChromeClient(new android.webkit.WebChromeClient());

        webView.loadUrl("https://cloudgenius.eggyhub.top/page/gratitude/");

    }
}