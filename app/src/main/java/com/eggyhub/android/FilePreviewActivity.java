package com.eggyhub.android;

import android.content.Intent;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;




public class FilePreviewActivity extends AppCompatActivity {
    private static final String TAG = "FilePreviewActivity";

    private WebView webViewPreview;
    private ProgressBar progressBar;
    private TextView errorTextView;

    private String fileUrl;
    private String fileType;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_preview);

        // 初始化视图
        webViewPreview = findViewById(R.id.webViewPreview);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);

        // 设置WebView背景为黑色
        webViewPreview.setBackgroundColor(getResources().getColor(android.R.color.black));

        // 获取传入的文件URL、类型和认证token
        Intent intent = getIntent();
        if (intent != null) {
            fileUrl = intent.getStringExtra("file_url");
            fileType = intent.getStringExtra("file_type");
            token = intent.getStringExtra("token");

            if (fileUrl != null && !fileUrl.isEmpty() && fileType != null && !fileType.isEmpty()) {
                AppLogger.d(TAG, "预览文件URL: " + fileUrl);
                AppLogger.d(TAG, "文件类型: " + fileType);
                // 统一使用WebView预览所有文件类型
                previewDocument(fileUrl, token);
            } else {
                AppLogger.e(TAG, "文件URL或类型为空");
                showError("无法预览文件: 参数不完整");
            }
        } else {
            AppLogger.e(TAG, "Intent为空");
            showError("无法预览文件: 缺少参数");
        }
    }

    /**
     * 统一使用WebView预览所有文件类型
     * @param fileUrl 文件URL
     * @param fileType 文件类型
     * @param token 认证token
     */
    private void previewFile(String fileUrl, String fileType, String token) {
        previewDocument(fileUrl, token);
    }

    /**
     * 预览文档 (统一使用WebView)
     */
    private void previewDocument(String documentUrl, String token) {
        AppLogger.d(TAG, "开始预览文档: " + documentUrl);
        webViewPreview.setVisibility(View.VISIBLE);
        WebSettings webSettings = webViewPreview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webViewPreview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoading();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showError("加载失败: " + description);
            }
        });

        // 直接加载URL，不添加任何请求头
        webViewPreview.loadUrl(documentUrl);
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        webViewPreview.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showError(String message) {
        hideLoading();
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webViewPreview != null) {
            webViewPreview.destroy();
        }
    }
}