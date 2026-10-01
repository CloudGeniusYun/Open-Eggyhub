package com.eggyhub.android;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.eggyhub.android.log.AppLogger;

public class ClipboardHelperActivity extends Activity {

    public static final String EXTRA_CLIPBOARD_TEXT = "extra_clipboard_text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置为透明主题，不显示任何UI

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            String clipboardText = null;

            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence pasteData = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (pasteData != null) {
                    clipboardText = pasteData.toString();
                    AppLogger.d("ClipboardHelperActivity", "Clipboard content read: " + clipboardText);
                } else {
                    AppLogger.d("ClipboardHelperActivity", "Clipboard content is null.");
                }
            } else {
                AppLogger.d("ClipboardHelperActivity", "Clipboard is empty or has no primary clip.");
            }

            Intent resultIntent = new Intent("com.eggyhub.android.CLIPBOARD_RESULT");
            resultIntent.putExtra(EXTRA_CLIPBOARD_TEXT, clipboardText);
            LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
            finish(); // 读取完剪贴板内容后立即关闭
            overridePendingTransition(0, 0); // 禁用Activity切换动画
        }, 300); // 300毫秒延迟
    }
}
