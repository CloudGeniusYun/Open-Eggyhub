package com.eggyhub.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.ContextThemeWrapper;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.appcompat.widget.AppCompatButton;

import android.content.ClipboardManager;
import android.content.SharedPreferences;
import com.eggyhub.android.log.AppLogger;

import com.eggyhub.android.UploadCodeRequest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import com.eggyhub.android.utils.OkHttpClientFactory;

import androidx.appcompat.app.AppCompatDelegate;

import android.util.DisplayMetrics;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.eggyhub.android.OverlayService;

public class FirstOverlayService extends Service {

    public static final String ACTION_SHOW_FIRST_OVERLAY = "ACTION_SHOW_FIRST_OVERLAY";

    private BroadcastReceiver clipboardResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("com.eggyhub.android.CLIPBOARD_RESULT")) {
                String clipboardText = intent.getStringExtra(ClipboardHelperActivity.EXTRA_CLIPBOARD_TEXT);
                if (clipboardText != null) {
                    if (validateShareCode(clipboardText)) {
                        uploadShareCode(clipboardText);
                    }
                } else {
                    Toast.makeText(FirstOverlayService.this, "剪贴板内容为空", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private static boolean isFirstOverlayServiceRunning = false;
    public static boolean isFirstOverlayShown = false;
    private static ShareCodeAdapterForOverlay.ShareCodeItem cachedShareCodeItem;
    private static int lastX = 100; // Default initial position
    private static int lastY = 100; // Default initial position
    private static final String SHARE_CODE_LIST_KEY = "shareCodeList"; // Added constant

    private WindowManager windowManager;
    private View firstOverlayView;
    private ShareCodeAdapterForOverlay.ShareCodeItem currentShareCodeItem;
    private SharedPreferences sharedPreferences;
    private OkHttpClient client;

    private static final String REGEX_SHARE_CODE = "^2y[a-z0-9]{11}$";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        isFirstOverlayServiceRunning = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        client = OkHttpClientFactory.getSharedClient();
        LocalBroadcastManager.getInstance(this).registerReceiver(clipboardResultReceiver, new IntentFilter("com.eggyhub.android.CLIPBOARD_RESULT"));

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "FirstOverlayServiceChannel";
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗服务通知",
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
        String CHANNEL_ID = "FirstOverlayServiceChannel";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Eggyhub 悬浮窗服务")
                .setContentText("正在运行中...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换为你的应用图标
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(2, notification);
        }

        if (intent != null) {
            String action = intent.getAction();
            if ("ACTION_SHOW_FIRST_OVERLAY".equals(action)) {
                currentShareCodeItem = (ShareCodeAdapterForOverlay.ShareCodeItem) intent.getSerializableExtra("share_code_item");
                if (intent.hasExtra("lastX") && intent.hasExtra("lastY")) {
                    lastX = intent.getIntExtra("lastX", lastX);
                    lastY = intent.getIntExtra("lastY", lastY);
                }
                showFirstOverlay();
            } else if ("ACTION_HIDE_FIRST_OVERLAY".equals(action)) {
                hideFirstOverlay();
            }
        }
        return START_NOT_STICKY;
    }

    private void showFirstOverlay() {
        if (firstOverlayView == null) {
            ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_Eggyhub);
            firstOverlayView = LayoutInflater.from(themedContext).inflate(R.layout.overlay_first_window, null);

            // Populate data if available
            // 优先使用 Intent 中传递的 currentShareCodeItem，如果 Intent 中没有，则尝试使用缓存
            if (currentShareCodeItem == null && cachedShareCodeItem != null) {
                currentShareCodeItem = cachedShareCodeItem;
                cachedShareCodeItem = null; // 使用后清除缓存
            }

            if (currentShareCodeItem != null) {
                TextView textViewName = firstOverlayView.findViewById(R.id.textViewShareCodeName);
                TextView textViewDescription = firstOverlayView.findViewById(R.id.textViewShareCodeDescription);
                TextView textViewShareCodeStock = firstOverlayView.findViewById(R.id.textViewShareCodeStock); // Initialize stock TextView
                ImageView imageViewShareCode = firstOverlayView.findViewById(R.id.imageViewShareCode);

                textViewName.setText(currentShareCodeItem.getName());
                textViewDescription.setText(truncateText(currentShareCodeItem.getDescription(), 30));
                textViewShareCodeStock.setText("库存: " + currentShareCodeItem.getStock()); // Set stock text
                Glide.with(this)
                        .load(currentShareCodeItem.getImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(imageViewShareCode);
            }
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int desiredWidth = 942; // 用户指定宽度
            int desiredHeight = 610; // 用户指定高度

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    desiredWidth,
                    desiredHeight,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = lastX;
            params.y = lastY;

            // Hide the second overlay if it's active
            if (OverlayService.isSecondOverlayActive()) {
                Intent hideSecondOverlayIntent = new Intent(this, OverlayService.class);
                hideSecondOverlayIntent.setAction("ACTION_HIDE_SECOND_OVERLAY");
                startService(hideSecondOverlayIntent);
            }

            windowManager.addView(firstOverlayView, params);
            FirstOverlayService.isFirstOverlayShown = true;

            firstOverlayView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) firstOverlayView.getLayoutParams();
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
                                if (Math.sqrt(dx * dx + dy * dy) > 20) { // 拖动阈值增加到20
                                    isDragging = true;
                                }
                            }
                            if (isDragging) {
                                overlayParams.x = initialX + (int) (event.getRawX() - initialTouchX); // 修正X轴拖动方向
                                overlayParams.y = initialY + (int) (event.getRawY() - initialTouchY); // 修正Y轴拖动方向
                                windowManager.updateViewLayout(firstOverlayView, overlayParams);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            return isDragging; // 如果是拖动，则消耗事件
                    }
                    return false;
                }
            });

            // Set up click listeners
            TextView textViewBack = firstOverlayView.findViewById(R.id.textViewBack);
            textViewBack.setOnClickListener(v -> {
                hideFirstOverlay();
                Intent secondOverlayIntent = new Intent(FirstOverlayService.this, OverlayService.class);
                secondOverlayIntent.setAction("ACTION_SHOW_SECOND_OVERLAY");
                startService(secondOverlayIntent);
            });

            TextView textViewMinimize = firstOverlayView.findViewById(R.id.textViewMinimize);
            textViewMinimize.setOnClickListener(v -> {
                hideFirstOverlay();
                Intent minimizeIconIntent = new Intent(this, MinimizeIconService.class);
                minimizeIconIntent.setAction("ACTION_SHOW_MINIMIZE_ICON");
                startService(minimizeIconIntent);
            });

            AppCompatButton pasteButton = firstOverlayView.findViewById(R.id.pasteButton);
            pasteButton.setOnClickListener(v -> {
                Intent intent = new Intent(FirstOverlayService.this, ClipboardHelperActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            });
        }
    }

    private void hideFirstOverlay() {
        if (firstOverlayView != null) {
            cachedShareCodeItem = currentShareCodeItem; // Store current item
            WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) firstOverlayView.getLayoutParams();
            lastX = overlayParams.x;
            lastY = overlayParams.y;
            windowManager.removeView(firstOverlayView);
            firstOverlayView = null;
            isFirstOverlayShown = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFirstOverlayServiceRunning = false;
        hideFirstOverlay();
        stopForeground(true); // 停止前台服务并移除通知
        LocalBroadcastManager.getInstance(this).unregisterReceiver(clipboardResultReceiver);
    }

    public static boolean isFirstOverlayActive() {
        return isFirstOverlayShown;
    }

    public static int getLastX() {
        return lastX;
    }

    public static int getLastY() {
        return lastY;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (firstOverlayView != null) {
            // Remove the view first to re-add it with updated parameters
            windowManager.removeView(firstOverlayView);
            firstOverlayView = null; // Set to null so showFirstOverlay recreates it
            showFirstOverlay(); // Re-add the overlay with updated dimensions
        }
    }

    private boolean validateShareCode(String code) {
        if (code == null || code.isEmpty()) {
            Toast.makeText(this, "分享码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }

        Pattern pattern = Pattern.compile(REGEX_SHARE_CODE);
        Matcher matcher = pattern.matcher(code);

        if (matcher.matches()) {
            return true;
        } else {
            Toast.makeText(this, "分享码格式不正确，请检查", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void uploadShareCode(String code) {
        if (currentShareCodeItem == null) {
            Toast.makeText(this, "没有可用的分享码信息", Toast.LENGTH_SHORT).show();
            return;
        }

        String accessToken = SecureStorageManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        UploadCodeRequest requestBody = new UploadCodeRequest(code, currentShareCodeItem.getId());
        String jsonBody = gson.toJson(requestBody);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/gifts/update")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e("FirstOverlayService", "Failed to upload share code", e);
                new android.os.Handler(getMainLooper()).post(() -> Toast.makeText(FirstOverlayService.this, "上传分享码失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    new android.os.Handler(getMainLooper()).post(() -> {
                        Toast.makeText(FirstOverlayService.this, "分享码上传成功", Toast.LENGTH_SHORT).show();
                        if (currentShareCodeItem != null) {
                            currentShareCodeItem.setStock(currentShareCodeItem.getStock() + 1); // Increment stock
                            // Update local cache
                            List<ShareCodeAdapterForOverlay.ShareCodeItem> cachedList = loadShareCodeList();
                            for (int i = 0; i < cachedList.size(); i++) {
                                if (cachedList.get(i).getId().equals(currentShareCodeItem.getId())) {
                                    cachedList.set(i, currentShareCodeItem);
                                    break;
                                }
                            }
                            saveShareCodeList(cachedList);

                            // Update the stock TextView in the first overlay
                            TextView textViewShareCodeStock = firstOverlayView.findViewById(R.id.textViewShareCodeStock);
                            if (textViewShareCodeStock != null) {
                                textViewShareCodeStock.setText("库存: " + currentShareCodeItem.getStock());
                            }

                            // Send broadcast to OverlayService to refresh RecyclerView
                            Intent refreshIntent = new Intent(OverlayService.ACTION_REFRESH_SHARE_CODE_LIST);
                            LocalBroadcastManager.getInstance(FirstOverlayService.this).sendBroadcast(refreshIntent);
                        }
                        // Optionally, refresh the list in OverlayService or hide this overlay
                    });
                } else {
                    AppLogger.e("FirstOverlayService", "Failed to upload share code, response code: " + response.code());
                    new android.os.Handler(getMainLooper()).post(() -> Toast.makeText(FirstOverlayService.this, "上传分享码失败: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private void saveShareCodeList(List<ShareCodeAdapterForOverlay.ShareCodeItem> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        sharedPreferences.edit().putString(SHARE_CODE_LIST_KEY, json).apply();
    }

    private List<ShareCodeAdapterForOverlay.ShareCodeItem> loadShareCodeList() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(SHARE_CODE_LIST_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<ShareCodeAdapterForOverlay.ShareCodeItem>>() {}.getType();
        return gson.fromJson(json, type);
    }
}