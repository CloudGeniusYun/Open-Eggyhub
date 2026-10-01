package com.eggyhub.android.player;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.util.Map;

/**
 * GSYVideoPlayer 播放器引擎实现
 * 运行在动态加载的 dex 中，由主应用的 BiliPlayerActivity 通过 IVideoPlayerEngine 接口调用
 */
public class DexPlayerEngineImpl implements IVideoPlayerEngine {

    private DexGSYVideoPlayer videoPlayer;
    private Activity activity;
    private ViewGroup container;
    private PlayerCallback callback;
    private boolean isFullscreen = false;
    private boolean enableFullscreen = true;
    private String videoUrl;
    private Map<String, String> headers;
    private String videoTitle = "";
    private OrientationUtils orientationUtils;
    private Context dexContext; // dex APK 的 Context，用于访问资源

    // 保存原始布局参数用于恢复
    private ViewGroup.LayoutParams originalLayoutParams;
    private int originalContainerWidth;
    private int originalContainerHeight;

    @Override
    public void setDexContext(Context dexContext) {
        this.dexContext = dexContext;
    }

    @Override
    public View createPlayerView(Activity activity, ViewGroup container) {
        this.activity = activity;
        this.container = container;

        // 使用 dex Context 创建播放器
        Context viewContext = dexContext != null ? dexContext : activity;

        // 使用工厂方法创建播放器，确保 dexContext 在 super() 调用期间可用
        videoPlayer = DexGSYVideoPlayer.create(viewContext, dexContext);

        // 设置播放器参数
        videoPlayer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        videoPlayer.setBackgroundColor(activity.getResources().getColor(android.R.color.black));

        // 保持屏幕常亮
        videoPlayer.setKeepScreenOn(true);

        // 添加到容器
        container.addView(videoPlayer);

        // 保存原始布局参数
        originalLayoutParams = videoPlayer.getLayoutParams();
        originalContainerWidth = container.getWidth();
        originalContainerHeight = container.getHeight();

        return videoPlayer;
    }

    @Override
    public void init(String videoUrl, Map<String, String> headers) {
        this.videoUrl = videoUrl;
        this.headers = headers;
    }

    @Override
    public void startPlay() {
        if (videoPlayer == null || videoUrl == null) return;

        // 初始化方向控制
        orientationUtils = new OrientationUtils(activity, videoPlayer);
        orientationUtils.setEnable(false); // 不自动旋转

        // 配置 GSYVideoPlayer
        new GSYVideoOptionBuilder()
                .setIsTouchWiget(true)
                .setRotateViewAuto(false)
                .setLockLand(false)
                .setShowFullAnimation(false)
                .setNeedLockFull(true)
                .setUrl(videoUrl)
                .setMapHeadData(headers)
                .setCacheWithPlay(true)
                .setVideoTitle(videoTitle)
                .setVideoAllCallBack(new GSYSampleCallBack() {
                    @Override
                    public void onPrepared(String url, Object... objects) {
                        super.onPrepared(url, objects);
                        if (callback != null) {
                            callback.onPrepared();
                        }
                    }

                    @Override
                    public void onAutoComplete(String url, Object... objects) {
                        super.onAutoComplete(url, objects);
                        if (callback != null) {
                            callback.onCompletion();
                        }
                    }

                    @Override
                    public void onClickStartError(String url, Object... objects) {
                        super.onClickStartError(url, objects);
                        if (callback != null) {
                            callback.onError("播放失败");
                        }
                    }

                    @Override
                    public void onQuitFullscreen(String url, Object... objects) {
                        super.onQuitFullscreen(url, objects);
                        isFullscreen = false;
                        if (callback != null) {
                            callback.onFullscreenChanged(false);
                        }
                    }

                    @Override
                    public void onEnterFullscreen(String url, Object... objects) {
                        super.onEnterFullscreen(url, objects);
                        isFullscreen = true;
                        if (callback != null) {
                            callback.onFullscreenChanged(true);
                        }
                    }
                })
                .build(videoPlayer);

        // 设置全屏按钮（使用GSY内置全屏）
        if (enableFullscreen && videoPlayer.getFullscreenButton() != null) {
            videoPlayer.getFullscreenButton().setOnClickListener(v -> {
                // 使用GSY全屏模式（在同一个Activity中全屏）
                videoPlayer.startWindowFullscreen(activity, true, true);
            });
        } else if (videoPlayer.getFullscreenButton() != null) {
            videoPlayer.getFullscreenButton().setVisibility(View.GONE);
        }

        // 开始播放
        videoPlayer.startPlayLogic();
    }

    @Override
    public void pause() {
        if (videoPlayer != null) {
            videoPlayer.onVideoPause();
        }
    }

    @Override
    public void resume() {
        if (videoPlayer != null) {
            videoPlayer.onVideoResume(false);
        }
    }

    @Override
    public void destroy() {
        if (videoPlayer != null) {
            videoPlayer.setVideoAllCallBack(null);
            videoPlayer.release();
            videoPlayer = null;
        }
        if (orientationUtils != null) {
            orientationUtils.releaseListener();
            orientationUtils = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 由 GSYVideoPlayer 内部处理
        if (videoPlayer != null) {
            videoPlayer.onConfigurationChanged(activity, newConfig, orientationUtils, true, false);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (videoPlayer != null && GSYVideoManager.backFromWindowFull(activity)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPlaying() {
        return videoPlayer != null &&
                videoPlayer.getCurrentState() == StandardGSYVideoPlayer.CURRENT_STATE_PLAYING;
    }

    @Override
    public int getCurrentState() {
        if (videoPlayer == null) return -1;
        return videoPlayer.getCurrentState();
    }

    @Override
    public void onEnterPipMode() {
        if (videoPlayer == null) return;

        // 隐藏GSY的顶部栏
        View topContainer = videoPlayer.findViewById(
                com.shuyu.gsyvideoplayer.R.id.layout_top);
        if (topContainer != null) topContainer.setVisibility(View.GONE);

        // 优化底部栏
        View bottomContainer = videoPlayer.findViewById(
                com.shuyu.gsyvideoplayer.R.id.layout_bottom);
        if (bottomContainer != null) {
            bottomContainer.setVisibility(View.VISIBLE);
            View progress = bottomContainer.findViewById(
                    com.shuyu.gsyvideoplayer.R.id.progress);
            View current = bottomContainer.findViewById(
                    com.shuyu.gsyvideoplayer.R.id.current);
            View total = bottomContainer.findViewById(
                    com.shuyu.gsyvideoplayer.R.id.total);
            if (progress != null) progress.setVisibility(View.GONE);
            if (current != null) current.setVisibility(View.GONE);
            if (total != null) total.setVisibility(View.GONE);
            if (videoPlayer.getFullscreenButton() != null) {
                videoPlayer.getFullscreenButton().setVisibility(View.VISIBLE);
            }
        }

        // 隐藏中央播放按钮
        if (videoPlayer.getStartButton() != null) {
            videoPlayer.getStartButton().setVisibility(View.GONE);
        }
    }

    @Override
    public void onExitPipMode() {
        if (videoPlayer == null) return;

        View topContainer = videoPlayer.findViewById(
                com.shuyu.gsyvideoplayer.R.id.layout_top);
        if (topContainer != null) topContainer.setVisibility(View.VISIBLE);

        View bottomContainer = videoPlayer.findViewById(
                com.shuyu.gsyvideoplayer.R.id.layout_bottom);
        if (bottomContainer != null) {
            bottomContainer.setVisibility(View.VISIBLE);
            View progress = bottomContainer.findViewById(
                    com.shuyu.gsyvideoplayer.R.id.progress);
            View current = bottomContainer.findViewById(
                    com.shuyu.gsyvideoplayer.R.id.current);
            View total = bottomContainer.findViewById(
                    com.shuyu.gsyvideoplayer.R.id.total);
            if (progress != null) progress.setVisibility(View.VISIBLE);
            if (current != null) current.setVisibility(View.VISIBLE);
            if (total != null) total.setVisibility(View.VISIBLE);
        }
        if (videoPlayer.getBackButton() != null) {
            videoPlayer.getBackButton().setVisibility(View.VISIBLE);
        }
        if (videoPlayer.getFullscreenButton() != null) {
            videoPlayer.getFullscreenButton().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setPlayerCallback(PlayerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void setEnableFullscreen(boolean enable) {
        this.enableFullscreen = enable;
    }

    @Override
    public View getPlayerView() {
        return videoPlayer;
    }

    @Override
    public void setVideoTitle(String title) {
        this.videoTitle = title != null ? title : "";
    }
}