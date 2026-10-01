package com.eggyhub.android.player;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

/**
 * 视频播放器引擎接口
 * 由主应用定义，dex模块实现
 * DexClassLoader加载后的实现类通过此接口与主应用交互
 */
public interface IVideoPlayerEngine {

    /**
     * 创建播放器视图并添加到容器
     * @param activity 宿主Activity
     * @param container 容器ViewGroup
     * @return 创建的播放器View
     */
    View createPlayerView(Activity activity, ViewGroup container);

    /**
     * 初始化播放器
     * @param videoUrl 视频播放地址
     * @param headers 请求头
     */
    void init(String videoUrl, Map<String, String> headers);

    /**
     * 开始播放
     */
    void startPlay();

    /**
     * 暂停
     */
    void pause();

    /**
     * 恢复播放
     */
    void resume();

    /**
     * 释放播放器
     */
    void destroy();

    /**
     * 配置变化（横竖屏切换）
     */
    void onConfigurationChanged(Configuration newConfig);

    /**
     * 返回键处理
     * @return true表示已处理
     */
    boolean onBackPressed();

    /**
     * 是否正在播放
     */
    boolean isPlaying();

    /**
     * 获取当前播放状态
     */
    int getCurrentState();

    /**
     * 进入画中画模式前的准备
     */
    void onEnterPipMode();

    /**
     * 退出画中画模式
     */
    void onExitPipMode();

    /**
     * 设置播放器回调
     */
    void setPlayerCallback(PlayerCallback callback);

    /**
     * 设置是否启用全屏功能
     */
    void setEnableFullscreen(boolean enable);

    /**
     * 获取播放器View
     */
    View getPlayerView();

    /**
     * 设置视频标题
     */
    void setVideoTitle(String title);

    /**
     * 播放器回调接口
     */
    interface PlayerCallback {
        void onPrepared();
        void onError(String message);
        void onCompletion();
        void onFullscreenChanged(boolean isFullscreen);
    }
}