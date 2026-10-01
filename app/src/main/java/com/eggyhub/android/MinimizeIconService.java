package com.eggyhub.android;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.eggyhub.android.log.AppLogger;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class MinimizeIconService extends Service {

    private static boolean isMinimizeIconServiceRunning = false;

    public static final String ACTION_SHOW_MINIMIZE_ICON = "ACTION_SHOW_MINIMIZE_ICON";
    public static final String ACTION_HIDE_MINIMIZE_ICON = "ACTION_HIDE_MINIMIZE_ICON";
    public static final String EXTRA_OVERLAY_TYPE = "extra_overlay_type";
    public static final int OVERLAY_TYPE_FIRST = 1;
    public static final int OVERLAY_TYPE_SECOND = 2;

    private WindowManager windowManager;
    private View minimizeIconView;
    private int currentOverlayType;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isMinimizeIconServiceRunning = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "MinimizeIconServiceChannel";
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "最小化图标服务通知",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 将服务设置为前台服务
        String CHANNEL_ID = "MinimizeIconServiceChannel";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Eggyhub 悬浮窗服务")
                .setContentText("正在运行中...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换为你的应用图标
                .setContentIntent(pendingIntent)
                .build();

        startForeground(3, notification); // 3 是通知的唯一ID，与OverlayService和FirstOverlayService不同

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_SHOW_MINIMIZE_ICON.equals(action)) {
                currentOverlayType = intent.getIntExtra(EXTRA_OVERLAY_TYPE, OVERLAY_TYPE_FIRST); // Default to first overlay
                showMinimizeIcon();
            } else if (ACTION_HIDE_MINIMIZE_ICON.equals(action)) {
                hideMinimizeIcon();
            }
        }
        return START_NOT_STICKY;
    }

    private void showMinimizeIcon() {
        if (minimizeIconView == null) {
            minimizeIconView = LayoutInflater.from(this).inflate(R.layout.overlay_minimize_icon, null);
            ImageView minimizeIconImageView = minimizeIconView.findViewById(R.id.minimizeIconImageView);
            updateMinimizeIconImage(minimizeIconImageView);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = FirstOverlayService.getLastX();
            params.y = FirstOverlayService.getLastY();

            windowManager.addView(minimizeIconView, params);

            minimizeIconView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) minimizeIconView.getLayoutParams();
                private boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = overlayParams.x;
                            initialY = overlayParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isDragging = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (!isDragging) {
                                float dx = event.getRawX() - initialTouchX;
                                float dy = event.getRawY() - initialTouchY;
                                if (Math.sqrt(dx * dx + dy * dy) > 50) { // 拖动阈值增加到50
                                    isDragging = true;
                                }
                            }
                            if (isDragging) {
                                overlayParams.x = initialX + (int) (event.getRawX() - initialTouchX); // 修正X轴拖动方向
                                overlayParams.y = initialY + (int) (event.getRawY() - initialTouchY); // 修正Y轴拖动方向
                                windowManager.updateViewLayout(minimizeIconView, overlayParams);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (isDragging) {
                                return true; // 消耗拖动事件
                            } else {
                                // When the minimize icon is clicked, show the correct overlay
                                if (currentOverlayType == OVERLAY_TYPE_FIRST) {
                                    Intent firstOverlayServiceIntent = new Intent(MinimizeIconService.this, FirstOverlayService.class);
                                    firstOverlayServiceIntent.setAction(FirstOverlayService.ACTION_SHOW_FIRST_OVERLAY);
                                    // Update FirstOverlayService's last known position to current minimize icon position
                                    WindowManager.LayoutParams currentParams = (WindowManager.LayoutParams) minimizeIconView.getLayoutParams();
                                    firstOverlayServiceIntent.putExtra("lastX", currentParams.x);
                                    firstOverlayServiceIntent.putExtra("lastY", currentParams.y);
                                    startService(firstOverlayServiceIntent);
                                } else if (currentOverlayType == OVERLAY_TYPE_SECOND) {
                                    Intent secondOverlayServiceIntent = new Intent(MinimizeIconService.this, OverlayService.class);
                                    secondOverlayServiceIntent.setAction(OverlayService.ACTION_SHOW_SECOND_OVERLAY);
                                    startService(secondOverlayServiceIntent);
                                }
                                hideMinimizeIcon();
                                return true; // 消耗点击事件
                            }
                    }
                    return false;
                }
            });
        } else {
            // If minimizeIconView already exists, just update its image
            ImageView minimizeIconImageView = minimizeIconView.findViewById(R.id.minimizeIconImageView);
            updateMinimizeIconImage(minimizeIconImageView);
        }
    }

    private void hideMinimizeIcon() {
        if (minimizeIconView != null) {
            windowManager.removeView(minimizeIconView);
            minimizeIconView = null;
        }
    }

    private void updateMinimizeIconImage(ImageView imageView) {
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String minimizeIconUriString = preferences.getString("minimize_icon_uri", null);
        AppLogger.d("MinimizeIconService", "Loaded minimize_icon_uri: " + minimizeIconUriString);

        if (minimizeIconUriString != null) {
            AppLogger.d("MinimizeIconService", "Loading custom image: " + minimizeIconUriString);
            Glide.with(this)
                    .load(Uri.parse(minimizeIconUriString))
                    .signature(new ObjectKey(System.currentTimeMillis())) // 添加签名以强制刷新缓存
                    .circleCrop()
                    .into(imageView);
        } else {
            AppLogger.d("MinimizeIconService", "Loading default image.");
            Glide.with(this)
                    .load(R.drawable.mini_circle)
                    .circleCrop()
                    .into(imageView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isMinimizeIconServiceRunning = false;
        hideMinimizeIcon();
        stopForeground(true); // 停止前台服务并移除通知
    }

    public static boolean isMinimizeIconActive() {
        return isMinimizeIconServiceRunning;
    }
}