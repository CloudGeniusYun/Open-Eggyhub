package com.eggyhub.android;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.eggyhub.android.player.DexLoader;
import com.eggyhub.android.player.IVideoPlayerEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频播放页面
 * 通过 DexLoader 动态加载播放器引擎，实现播放器功能的动态加载
 */
public class BiliPlayerActivity extends AppCompatActivity {

    private static final String ACTION_MEDIA_CONTROL = "media_control";
    private static final String EXTRA_CONTROL_TYPE = "control_type";
    private static final int CONTROL_TYPE_PLAY_PAUSE = 1;

    private IVideoPlayerEngine engine;
    private FrameLayout playerContainer;
    private ImageButton btnClose;
    private ImageButton btnPip;
    private ProgressBar pbBuffering;
    private String videoUrl;
    private String bvid;

    // 文件选择器
    private ActivityResultLauncher<String[]> filePickerLauncher;

    private final BroadcastReceiver pipReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_MEDIA_CONTROL.equals(intent.getAction())) return;

            int controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0);
            if (controlType == CONTROL_TYPE_PLAY_PAUSE && engine != null) {
                if (engine.isPlaying()) {
                    engine.pause();
                } else {
                    engine.resume();
                }
                updatePipActions();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bili_player);

        // 注册文件选择器
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        importDexApk(uri);
                    } else {
                        Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        playerContainer = findViewById(R.id.player_container);
        btnClose = findViewById(R.id.btn_close);
        btnPip = findViewById(R.id.btn_pip);
        pbBuffering = findViewById(R.id.pb_buffering);

        btnClose.setOnClickListener(v -> finish());
        btnPip.setOnClickListener(v -> enterPipMode());

        videoUrl = getIntent().getStringExtra("video_url");
        bvid = getIntent().getStringExtra("bvid");

        if (videoUrl == null) {
            Toast.makeText(this, "视频地址无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查 dex 是否可用
        if (!DexLoader.isDexAvailable(this)) {
            showImportDialog();
            return;
        }

        // 加载播放器
        loadPlayer();
    }

    /**
     * 显示导入对话框
     */
    private void showImportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("导入播放器模块")
                .setMessage("播放器模块未安装，请选择 video_player.dex.apk 文件进行导入。\n\n" +
                        "提示：该文件需要单独构建，运行命令：\n./gradlew app:prepareVideoDex")
                .setPositiveButton("选择文件", (dialog, which) -> {
                    // 打开文件选择器，选择 APK 文件
                    filePickerLauncher.launch(new String[]{"application/vnd.android.package-archive"});
                })
                .setNegativeButton("取消", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * 导入 dex APK
     */
    private void importDexApk(Uri uri) {
        pbBuffering.setVisibility(View.VISIBLE);
        new Thread(() -> {
            boolean success = DexLoader.importDexApk(this, uri);
            runOnUiThread(() -> {
                pbBuffering.setVisibility(View.GONE);
                if (success) {
                    Toast.makeText(this, "播放器模块导入成功", Toast.LENGTH_SHORT).show();
                    loadPlayer();
                } else {
                    Toast.makeText(this, "导入失败，请检查文件是否正确", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }).start();
    }

    /**
     * 加载播放器引擎并开始播放
     */
    private void loadPlayer() {
        // 动态加载播放器引擎
        engine = DexLoader.getEngine(this);
        if (engine == null) {
            Toast.makeText(this, "播放器加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置播放回调
        engine.setPlayerCallback(new IVideoPlayerEngine.PlayerCallback() {
            @Override
            public void onPrepared() {
                runOnUiThread(() -> {
                    pbBuffering.setVisibility(View.GONE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode()) {
                        updatePipActions();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    pbBuffering.setVisibility(View.GONE);
                    Toast.makeText(BiliPlayerActivity.this, "播放错误: " + message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCompletion() {
                runOnUiThread(() -> pbBuffering.setVisibility(View.GONE));
            }

            @Override
            public void onFullscreenChanged(boolean isFullscreen) {
                runOnUiThread(() -> updateLayoutForOrientation(
                        isFullscreen ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT));
            }
        });

        // 构造请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.bilibili.com");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

        // 通过引擎初始化播放器
        engine.init(videoUrl, headers);
        engine.setEnableFullscreen(true);
        engine.setVideoTitle("");
        engine.createPlayerView(this, playerContainer);
        engine.startPlay();

        // 注册 PIP 广播
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(pipReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
        }

        // 初始布局调整
        updateLayoutForOrientation(getResources().getConfiguration().orientation);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (engine != null) {
            engine.onConfigurationChanged(newConfig);
        }
        updateLayoutForOrientation(newConfig.orientation);
    }

    private void updateLayoutForOrientation(int orientation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return;
        }

        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) playerContainer.getLayoutParams();
        View bottomBg = findViewById(R.id.view_bottom_bg);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            lp.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            lp.dimensionRatio = null;
            if (bottomBg != null) bottomBg.setVisibility(View.GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            lp.width = 0;
            lp.height = 0;
            lp.dimensionRatio = "H,16:9";
            if (bottomBg != null) bottomBg.setVisibility(View.VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        playerContainer.setLayoutParams(lp);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 画中画模式下不暂停播放
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            if (engine != null) engine.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (engine != null) {
            engine.destroy();
            engine = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                unregisterReceiver(pipReceiver);
            } catch (Exception e) {
                // ignore
            }
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (engine != null && engine.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    private void updatePipActions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || engine == null) return;

        List<RemoteAction> actions = new ArrayList<>();

        Intent intent = new Intent(ACTION_MEDIA_CONTROL);
        intent.putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY_PAUSE);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, CONTROL_TYPE_PLAY_PAUSE, intent, flags);

        Icon icon;
        String title;
        if (engine.isPlaying()) {
            icon = Icon.createWithResource(this, android.R.drawable.ic_media_pause);
            title = "暂停";
        } else {
            icon = Icon.createWithResource(this, android.R.drawable.ic_media_play);
            title = "播放";
        }

        actions.add(new RemoteAction(icon, title, title, pendingIntent));

        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setActions(actions)
                .build();
        setPictureInPictureParams(params);
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational rational = new Rational(16, 9);

            List<RemoteAction> actions = new ArrayList<>();
            Intent intent = new Intent(ACTION_MEDIA_CONTROL);
            intent.putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY_PAUSE);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, CONTROL_TYPE_PLAY_PAUSE, intent, flags);
            boolean isPlaying = engine != null && engine.isPlaying();
            Icon icon = Icon.createWithResource(this, isPlaying ?
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            String title = isPlaying ? "暂停" : "播放";
            actions.add(new RemoteAction(icon, title, title, pendingIntent));

            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(rational)
                    .setActions(actions)
                    .build();
            enterPictureInPictureMode(params);
        } else {
            Toast.makeText(this, "当前设备不支持小窗模式", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (engine == null) return;

        if (isInPictureInPictureMode) {
            btnClose.setVisibility(View.GONE);
            btnPip.setVisibility(View.GONE);
            engine.onEnterPipMode();
        } else {
            btnClose.setVisibility(View.VISIBLE);
            btnPip.setVisibility(View.VISIBLE);
            engine.onExitPipMode();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // 返回桌面时自动进入画中画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && engine != null && engine.isPlaying()) {
            enterPipMode();
        }
    }
}