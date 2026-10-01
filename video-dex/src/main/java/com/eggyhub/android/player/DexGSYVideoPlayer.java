package com.eggyhub.android.player;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

/**
 * 自定义 GSYVideoPlayer，重写 getActivityContext() 返回 dex Context
 * 这样 inflate 布局时会使用 dex APK 的 Resources
 */
public class DexGSYVideoPlayer extends StandardGSYVideoPlayer {

    // 使用 ThreadLocal 传递 dexContext，因为 super() 会在字段初始化之前调用 init()
    private static final ThreadLocal<Context> tempDexContext = new ThreadLocal<>();

    private Context dexContext;

    /**
     * 创建 DexGSYVideoPlayer
     * 使用 ThreadLocal 传递 dexContext，绕过 Java 构造函数执行顺序的限制
     */
    public static DexGSYVideoPlayer create(Context context, Context dexContext) {
        tempDexContext.set(dexContext);
        try {
            return new DexGSYVideoPlayer(context);
        } finally {
            tempDexContext.remove();
        }
    }

    private DexGSYVideoPlayer(Context context) {
        super(context);
        this.dexContext = tempDexContext.get();
    }

    /**
     * 重写 getActivityContext()，返回 dex Context
     * 这样 GSYVideoView.init() 会使用 dex Context 来 inflate 布局
     */
    @Override
    public Context getActivityContext() {
        if (dexContext != null) {
            return dexContext;
        }
        // 如果 dexContext 还没设置，尝试从 ThreadLocal 获取（在 super() 调用期间）
        Context temp = tempDexContext.get();
        if (temp != null) {
            return temp;
        }
        return super.getActivityContext();
    }
}