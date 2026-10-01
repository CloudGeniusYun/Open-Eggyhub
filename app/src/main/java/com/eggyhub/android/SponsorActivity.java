package com.eggyhub.android;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SponsorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sponsor);

        WebView webView = findViewById(R.id.webview_sponsor);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://cloudgenius.eggyhub.top/page/eggyhub/sponsor/");

    }
}