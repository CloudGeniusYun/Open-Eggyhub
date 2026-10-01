package com.eggyhub.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.graphics.PorterDuff;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Callback;
import com.eggyhub.android.utils.OkHttpClientFactory;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageButton;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Rect;
import com.eggyhub.android.log.AppLogger;
import android.util.DisplayMetrics;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.app.NotificationCompat;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.ServiceInfo;
import android.os.Build;

public class OverlayService extends Service {

    private static boolean isOverlayServiceRunning = false;
    public static boolean isSecondOverlayShown = false;

    public static boolean isOverlayServiceActive() {
        return isOverlayServiceRunning;
    }

    private WindowManager windowManager;
    private View secondOverlayView;
    private WindowManager.LayoutParams secondOverlayLayoutParams; // Add this line
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "OverlayServicePrefs";
    private static final String SHARE_CODE_LIST_KEY = "shareCodeList";
    private static final String LAST_FETCH_TIME_KEY = "lastFetchTime";
    public static final String ACTION_REFRESH_SHARE_CODE_LIST = "com.eggyhub.android.ACTION_REFRESH_SHARE_CODE_LIST"; // Added constant
    private static final long CACHE_DURATION = 2 * 60 * 1000; // 2 minutes
    private static final String PREF_BACKGROUND_COLOR = "pref_background_color";
    private static final String PREF_FONT_COLOR = "pref_font_color";
    public static final String ACTION_REFRESH_SHARE_CODE = "ACTION_REFRESH_SHARE_CODE";
    public static final String ACTION_SHOW_SECOND_OVERLAY = "ACTION_SHOW_SECOND_OVERLAY";
    private static final String PREF_SHOW_HOSTED_SHARE_CODES = "pref_show_hosted_share_codes";
    private static final String PREF_ENABLE_HEIGHT_EDIT = "pref_enable_height_edit";
    private static final String PREF_CUSTOM_HEIGHT_PX = "pref_custom_height_px";
    private static final String HOSTED_SHARE_CODE_API_URL = "https://eggyhub.top/api/admin/hosting/list";
    private boolean showHostedShareCodes = false;
    private int currentFontColor; // Member variable to store font color

    private OkHttpClient client;
    private Handler mainHandler;

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private View trashIconView;
    private WindowManager.LayoutParams trashIconParams;
    private View viewResizeHandle;
    private RecyclerView secondOverlayRecyclerView; // Declare RecyclerView as a member variable
    private TextView textViewLoading; // Declare TextView as a member variable
    private BroadcastReceiver refreshReceiver; // Declare BroadcastReceiver
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private long lastRefreshClickTime = 0L; // Added for refresh button rate limiting
    private int singleItemHeightPx; // Declare as member variable

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isOverlayServiceRunning = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        client = OkHttpClientFactory.getSharedClient();
        mainHandler = new Handler(Looper.getMainLooper());

        preferenceChangeListener = (sharedPrefs, key) -> {
            if (PREF_ENABLE_HEIGHT_EDIT.equals(key)) {
                boolean enabled = sharedPrefs.getBoolean(PREF_ENABLE_HEIGHT_EDIT, false);
                mainHandler.post(() -> {
                    if (viewResizeHandle != null) {
                        viewResizeHandle.setVisibility(enabled ? View.VISIBLE : View.GONE);
                    }
                });
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // Register BroadcastReceiver
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_REFRESH_SHARE_CODE_LIST.equals(intent.getAction())) {
                    AppLogger.d("OverlayService", "BroadcastReceiver: Received ACTION_REFRESH_SHARE_CODE_LIST.");
                    refreshSecondOverlayData(); // 调用 refreshSecondOverlayData()
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(refreshReceiver, new IntentFilter(ACTION_REFRESH_SHARE_CODE_LIST));

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "OverlayServiceChannel";
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

        trashIconView = LayoutInflater.from(this).inflate(R.layout.overlay_trash_icon, null);
        trashIconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        trashIconParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        trashIconParams.y = 0; // At the bottom of the screen

        // Initialize singleItemHeightPx here
        singleItemHeightPx = dpToPx(76);
    }

    private void saveShareCodeList(List<ShareCodeAdapterForOverlay.ShareCodeItem> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        sharedPreferences.edit().putString(SHARE_CODE_LIST_KEY, json).apply();

        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
    String userRole = userPrefs.getString("role", "user");

    if (!"admin".equals(userRole)) {
            // 只有非管理员用户才存储刷新时间
            sharedPreferences.edit().putLong(LAST_FETCH_TIME_KEY, System.currentTimeMillis()).apply();
            AppLogger.d("OverlayService", "saveShareCodeList: Storing LAST_FETCH_TIME_KEY for regular user.");
        } else {
            AppLogger.d("OverlayService", "saveShareCodeList: Admin user, skipping LAST_FETCH_TIME_KEY storage.");
        }
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

    private long getLastFetchTime() {
        return sharedPreferences.getLong(LAST_FETCH_TIME_KEY, 0L);
    }

    private void fetchShareCodeDataFromApi(RecyclerView recyclerView, boolean fetchHostedShareCodes) {
        AppLogger.d("OverlayService", "fetchShareCodeDataFromApi called. fetchHostedShareCodes: " + fetchHostedShareCodes + ", current showHostedShareCodes member = " + showHostedShareCodes);
        String accessToken = SecureStorageManager.getAccessToken();
        AppLogger.d("OverlayService", "Access Token: " + (accessToken != null ? "present" : "null or empty"));
        if (accessToken == null || accessToken.isEmpty()) {
            mainHandler.post(() -> Toast.makeText(OverlayService.this, "用户未登录", Toast.LENGTH_SHORT).show());
            return;
        }

        List<ShareCodeAdapterForOverlay.ShareCodeItem> combinedList = new ArrayList<>();
        final Object lock = new Object(); // Lock object for synchronized access to combinedList
        final int[] pendingRequests = {1}; // Counter for pending requests, initialized to 1 for regular API

        if (fetchHostedShareCodes) {
            pendingRequests[0]++; // Increment if hosted share codes are also being fetched
        }

        // Fetch regular share codes
        String regularUrl = "https://eggyhub.top/api/mygifts";
        AppLogger.d("OverlayService", "Fetching regular share codes from: " + regularUrl);
        Request regularRequest = new Request.Builder()
                .url(regularUrl)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(regularRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                AppLogger.e("OverlayService", "Failed to fetch regular share codes: " + e.getMessage(), e);
                synchronized (lock) {
                    pendingRequests[0]--;
                    if (pendingRequests[0] == 0) {
                        AppLogger.d("OverlayService", "Regular API failed, all requests completed. Handling combined results.");
                        handleCombinedResults(combinedList, recyclerView);
                    }
                }
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                try {
                    try (okhttp3.ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful()) {
                            String responseData = responseBody.string();
                            AppLogger.d("OverlayService", "Regular API successful. Response: " + responseData);
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<ShareCodeAdapterForOverlay.ShareCodeItem>>() {}.getType();
                            List<ShareCodeAdapterForOverlay.ShareCodeItem> fetchedList = gson.fromJson(responseData, listType);
                            synchronized (lock) {
                                combinedList.addAll(fetchedList);
                                AppLogger.d("OverlayService", "Added " + fetchedList.size() + " regular share codes to combined list.");
                            }
                        } else {
                            AppLogger.e("OverlayService", "Regular API failed. Code: " + response.code() + ", Message: " + response.message());
                        }
                    }
                } catch (Exception e) {
                    AppLogger.e("OverlayService", "Error parsing regular share codes response: " + e.getMessage(), e);
                } finally {
                    synchronized (lock) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            AppLogger.d("OverlayService", "Regular API response processed, all requests completed. Handling combined results.");
                            handleCombinedResults(combinedList, recyclerView);
                        }
                    }
                }
            }
        });

        // Fetch hosted share codes if enabled
        if (fetchHostedShareCodes) {
            AppLogger.d("OverlayService", "Fetching hosted share codes from: " + HOSTED_SHARE_CODE_API_URL);
            Request hostedRequest = new Request.Builder()
                    .url(HOSTED_SHARE_CODE_API_URL)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(hostedRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    AppLogger.e("OverlayService", "Failed to fetch hosted share codes: " + e.getMessage(), e);
                    synchronized (lock) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            AppLogger.d("OverlayService", "Hosted API failed, all requests completed. Handling combined results.");
                            handleCombinedResults(combinedList, recyclerView);
                        }
                    }
                }

                @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                try {
                    try (okhttp3.ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful()) {
                            String responseData = responseBody.string();
                            AppLogger.d("OverlayService", "Hosted API successful. Response: " + responseData);
                            Gson gson = new Gson();
                            // 解析外部对象，然后获取 "list" 数组
                            JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);
                            JsonArray jsonArray = jsonObject.getAsJsonArray("list");
                            Type listType = new TypeToken<List<ShareCodeAdapterForOverlay.ShareCodeItem>>() {}.getType();
                            List<ShareCodeAdapterForOverlay.ShareCodeItem> fetchedList = gson.fromJson(jsonArray, listType);
                            // 过滤掉 hosting_status 为 2 的项
                            List<ShareCodeAdapterForOverlay.ShareCodeItem> filteredList = new ArrayList<>();
                            for (ShareCodeAdapterForOverlay.ShareCodeItem item : fetchedList) {
                                if (item.getHosting_status() != 2) {
                                    filteredList.add(item);
                                }
                            }
                            synchronized (lock) {
                                for (ShareCodeAdapterForOverlay.ShareCodeItem item : filteredList) {
                                    item.setName(item.getName() + "(托管)*"); // Add suffix
                                }
                                combinedList.addAll(filteredList);
                                AppLogger.d("OverlayService", "Added " + filteredList.size() + " filtered hosted share codes to combined list.");
                            }
                        } else {
                            AppLogger.e("OverlayService", "Hosted API failed. Code: " + response.code() + ", Message: " + response.message());
                        }
                    }
                } catch (Exception e) {
                    AppLogger.e("OverlayService", "Error parsing hosted share codes response: " + e.getMessage(), e);
                } finally {
                    synchronized (lock) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            AppLogger.d("OverlayService", "Hosted API response processed, all requests completed. Handling combined results.");
                            handleCombinedResults(combinedList, recyclerView);
                        }
                    }
                }
            }
            });
        }
    }

    private void handleCombinedResults(List<ShareCodeAdapterForOverlay.ShareCodeItem> combinedList, RecyclerView recyclerView) {
        AppLogger.d("OverlayService", "handleCombinedResults called. Combined list size: " + combinedList.size());
        if (combinedList.isEmpty()) {
            // If both API calls failed or returned empty, try to load cached data
            AppLogger.d("OverlayService", "Combined list is empty. Attempting to load cached data.");
            List<ShareCodeAdapterForOverlay.ShareCodeItem> cachedList = loadShareCodeList();
            mainHandler.post(() -> {
                displayShareCodeOverlay(cachedList);
                Toast.makeText(OverlayService.this, "分享码列表为空", Toast.LENGTH_SHORT).show();
                AppLogger.d("OverlayService", "Displayed cached share codes. Cached list size: " + cachedList.size());
            });
        } else {
            AppLogger.d("OverlayService", "Combined list is not empty. Saving and displaying new data.");
            saveShareCodeList(combinedList);
            mainHandler.post(() -> {
                displayShareCodeOverlay(combinedList);
                AppLogger.d("OverlayService", "Displayed new share codes. Combined list size: " + combinedList.size());
            });
        }
    }

    private void refreshSecondOverlayData() {
        if (textViewLoading != null) {
            textViewLoading.setVisibility(View.VISIBLE);
        }

        // 重新从 SharedPreferences 读取 showHostedShareCodes 的最新状态
        showHostedShareCodes = sharedPreferences.getBoolean(PREF_SHOW_HOSTED_SHARE_CODES, false);
        AppLogger.d("OverlayService", "refreshSecondOverlayData: showHostedShareCodes after reading from prefs = " + showHostedShareCodes);

        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userRole = userPrefs.getString("role", "user");

        if ("admin".equals(userRole)) {
            // 管理员账号：先加载本地缓存并显示
            List<ShareCodeAdapterForOverlay.ShareCodeItem> cachedList = loadShareCodeList();
            if (!cachedList.isEmpty()) {
                mainHandler.post(() -> {
                    displayShareCodeOverlay(cachedList);
                    AppLogger.d("OverlayService", "Admin: Displayed cached share codes first. Cached list size: " + cachedList.size());
                });
            }
        }
        // 无论是否管理员，都继续发起网络请求获取最新数据
        fetchShareCodeDataFromApi(secondOverlayRecyclerView, showHostedShareCodes);
    }

    private void updateRecyclerView(RecyclerView recyclerView, List<ShareCodeAdapterForOverlay.ShareCodeItem> list) {
        if (recyclerView != null) {
            ShareCodeAdapterForOverlay adapter = new ShareCodeAdapterForOverlay(list, item -> {
                // Handle item click: show first overlay with item details
                hideSecondOverlay(); // Close the second overlay
                Intent firstOverlayIntent = new Intent(OverlayService.this, FirstOverlayService.class);
                firstOverlayIntent.setAction("ACTION_SHOW_FIRST_OVERLAY");
                firstOverlayIntent.putExtra("share_code_item", item);
                startService(firstOverlayIntent);
            }, currentFontColor);
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();

            // Update the height of the second overlay based on the new list size
            if (secondOverlayView != null && secondOverlayLayoutParams != null) {
                int customHeight = sharedPreferences.getInt(PREF_CUSTOM_HEIGHT_PX, -1);
                
                if (customHeight != -1) {
                    secondOverlayLayoutParams.height = customHeight;
                } else {
                    int maxVisibleItems = 5;
                    int itemCount = list.size();
                    int desiredHeightPx;

                    // 计算头部和内边距的高度
                    int headerHeightPx = dpToPx(60); // 估计标题栏 + 分割线 + padding

                    if (itemCount == 0) {
                        desiredHeightPx = WindowManager.LayoutParams.WRAP_CONTENT;
                    } else if (itemCount < maxVisibleItems) {
                        desiredHeightPx = headerHeightPx + (singleItemHeightPx * itemCount);
                    } else {
                        desiredHeightPx = headerHeightPx + (singleItemHeightPx * maxVisibleItems);
                    }
                    secondOverlayLayoutParams.height = desiredHeightPx;
                }
                windowManager.updateViewLayout(secondOverlayView, secondOverlayLayoutParams);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 将服务设置为前台服务
        String CHANNEL_ID = "OverlayServiceChannel";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Eggyhub 悬浮窗服务")
                .setContentText("正在运行中...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换为你的应用图标
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }

        if (intent != null) {
            AppLogger.d("OverlayService", "onStartCommand: Action = " + intent.getAction() + ", Initial showHostedShareCodes = " + showHostedShareCodes);
            showHostedShareCodes = intent.getBooleanExtra(PREF_SHOW_HOSTED_SHARE_CODES, false);
            String action = intent.getAction();
            if (ACTION_REFRESH_SHARE_CODE.equals(intent.getAction())) {
                boolean newShowHostedShareCodes = intent.getBooleanExtra(PREF_SHOW_HOSTED_SHARE_CODES, showHostedShareCodes);
                AppLogger.d("OverlayService", "onStartCommand: ACTION_REFRESH_SHARE_CODE received. newShowHostedShareCodes = " + newShowHostedShareCodes);
                if (newShowHostedShareCodes != showHostedShareCodes) {
                    showHostedShareCodes = newShowHostedShareCodes;
                    refreshSecondOverlayData();
                } else if (isSecondOverlayShown()) {
                    refreshSecondOverlayData();
                } else {
                    showSecondOverlay(true);
                }
            } else if (ACTION_SHOW_SECOND_OVERLAY.equals(intent.getAction())) {
                showSecondOverlay(false); // 默认不强制刷新
            } else if ("ACTION_HIDE_SECOND_OVERLAY".equals(intent.getAction())) {
                hideSecondOverlay();
            }
        }
        return START_NOT_STICKY;
    }

    private void displayShareCodeOverlay(List<ShareCodeAdapterForOverlay.ShareCodeItem> list) {
        updateRecyclerView(secondOverlayRecyclerView, list);
        if (textViewLoading != null) {
            textViewLoading.setVisibility(View.GONE); // Hide loading text
        }
    }

    private void showSecondOverlay(boolean forceRefresh) {
        if (secondOverlayView == null) {
            // 使用 ContextThemeWrapper 包装 Context，提供主题信息
            Context themedContext = new ContextThemeWrapper(this, R.style.Theme_Eggyhub);
            LayoutInflater inflater = (LayoutInflater) themedContext.getSystemService(LAYOUT_INFLATER_SERVICE);
            secondOverlayView = inflater.inflate(R.layout.overlay_second_window, null);

            // 从SharedPreferences加载背景颜色，如果不存在则使用默认颜色 (例如，白色)
            int savedBackgroundColor = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt(PREF_BACKGROUND_COLOR, Color.BLACK);
            int savedOpacity = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt("pref_background_opacity", 70); // Default to 70%
            int savedFontColor = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt(PREF_FONT_COLOR, Color.WHITE);
            currentFontColor = savedFontColor;

            TextView textViewTitle = secondOverlayView.findViewById(R.id.textViewTitle);
            if (textViewTitle != null) {
                textViewTitle.setTextColor(currentFontColor);
            }

            View dividerView = secondOverlayView.findViewById(R.id.dividerView);
            if (dividerView != null) {
                dividerView.setBackgroundColor(currentFontColor);
            }

            TextView textViewMinimizeSecond = secondOverlayView.findViewById(R.id.textViewMinimizeSecond);
            if (textViewMinimizeSecond != null) {
                textViewMinimizeSecond.setTextColor(currentFontColor);
                textViewMinimizeSecond.setOnClickListener(v -> {
                    hideSecondOverlay();
                    Intent minimizeIntent = new Intent(OverlayService.this, MinimizeIconService.class);
                    minimizeIntent.setAction(MinimizeIconService.ACTION_SHOW_MINIMIZE_ICON);
                    minimizeIntent.putExtra(MinimizeIconService.EXTRA_OVERLAY_TYPE, MinimizeIconService.OVERLAY_TYPE_SECOND);
                    startService(minimizeIntent);
                });
            }
            int alpha = (int) (255 * (savedOpacity / 100.0f));

            // Combine alpha with the saved background color
            int finalBackgroundColor = Color.argb(alpha, Color.red(savedBackgroundColor), Color.green(savedBackgroundColor), Color.blue(savedBackgroundColor));

            // 获取背景Drawable并设置颜色，以保留圆角
            Drawable background = secondOverlayView.getBackground();
            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(finalBackgroundColor);
            } else {
                // 如果背景不是GradientDrawable，则直接设置颜色（这可能会移除圆角）
                secondOverlayView.setBackgroundColor(finalBackgroundColor);
            }

            secondOverlayRecyclerView = secondOverlayView.findViewById(R.id.recyclerViewShareCodes);
            secondOverlayRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            textViewLoading = secondOverlayView.findViewById(R.id.textViewLoading); // Initialize textViewLoading

            viewResizeHandle = secondOverlayView.findViewById(R.id.viewResizeHandle);
            if (viewResizeHandle != null) {
                boolean isHeightEditEnabled = sharedPreferences.getBoolean(PREF_ENABLE_HEIGHT_EDIT, false);
                viewResizeHandle.setVisibility(isHeightEditEnabled ? View.VISIBLE : View.GONE);
                viewResizeHandle.setOnTouchListener(new View.OnTouchListener() {
                    private float initialTouchY;
                    private int initialHeight;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (secondOverlayLayoutParams == null) return false;
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialTouchY = event.getRawY();
                                initialHeight = secondOverlayLayoutParams.height;
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                float dy = event.getRawY() - initialTouchY;
                                int newHeight = initialHeight + (int) dy;

                                // Limit min and max height
                                int minHeight = dpToPx(120); // Header + some space
                                DisplayMetrics dm = getResources().getDisplayMetrics();
                                int maxHeight = (int) (dm.heightPixels * 0.8f);

                                if (newHeight < minHeight) newHeight = minHeight;
                                if (newHeight > maxHeight) newHeight = maxHeight;

                                secondOverlayLayoutParams.height = newHeight;
                                windowManager.updateViewLayout(secondOverlayView, secondOverlayLayoutParams);
                                return true;
                            case MotionEvent.ACTION_UP:
                                // Save the custom height
                                sharedPreferences.edit().putInt(PREF_CUSTOM_HEIGHT_PX, secondOverlayLayoutParams.height).apply();
                                return true;
                        }
                        return false;
                    }
                });
            }

            if (textViewLoading != null) {
                textViewLoading.setVisibility(View.VISIBLE); // Show loading text
            }

            if (forceRefresh) {
                refreshSecondOverlayData();
            } else {
                List<ShareCodeAdapterForOverlay.ShareCodeItem> cachedList = loadShareCodeList();
                // Add a GlobalLayoutListener to ensure height is adjusted after initial layout
                secondOverlayView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Remove the listener to avoid multiple calls
                        secondOverlayView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        // Now that the view is laid out, update the RecyclerView height
                        displayShareCodeOverlay(cachedList);
                    }
                });
            }
            isSecondOverlayShown = true;

            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;

            secondOverlayLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, // Set width to WRAP_CONTENT
                    WindowManager.LayoutParams.WRAP_CONTENT, // Set height to WRAP_CONTENT initially, will be updated by updateRecyclerView
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);

            secondOverlayLayoutParams.gravity = Gravity.TOP | Gravity.START;
            secondOverlayLayoutParams.x = 100;
            secondOverlayLayoutParams.y = 100;

            windowManager.addView(secondOverlayView, secondOverlayLayoutParams);



            AppCompatImageButton refreshButton = secondOverlayView.findViewById(R.id.imageButtonRefresh);
            if (refreshButton != null) {
                refreshButton.setColorFilter(currentFontColor, PorterDuff.Mode.SRC_IN);
                refreshButton.setOnClickListener(v -> {
                    SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    String userRole = userPrefs.getString("role", "user");

                    if (!"admin".equals(userRole)) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastRefreshClickTime < 60 * 1000) { // 60 seconds
                            Toast.makeText(OverlayService.this, "刷新过于频繁，请稍后再试。", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        lastRefreshClickTime = currentTime;
                    }

                    // 1. 清除本地相关数据
                    saveShareCodeList(new ArrayList<>());
                    // 2. 清空列表显示，并显示“正在拉取数据”
                    updateRecyclerView(secondOverlayRecyclerView, new ArrayList<>());
                    if (textViewLoading != null) {
                        textViewLoading.setVisibility(View.VISIBLE); // Show loading text
                    }
                    // 3. 发送广播，通知服务刷新数据
                    LocalBroadcastManager.getInstance(OverlayService.this).sendBroadcast(new Intent(ACTION_REFRESH_SHARE_CODE_LIST));
                });
            }

            secondOverlayView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) secondOverlayView.getLayoutParams();
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
                                // Only start dragging if movement exceeds a threshold
                                float dx = event.getRawX() - initialTouchX;
                                float dy = event.getRawY() - initialTouchY;
                                if (Math.sqrt(dx * dx + dy * dy) > 10) { // Threshold of 10 pixels
                                    isDragging = true;
                                    showTrashIcon();
                                }
                            }

                            if (isDragging) {
                                overlayParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                                overlayParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(secondOverlayView, overlayParams);
                                // TODO: Check if overlay is over trash icon and highlight trash icon
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (isDragging) {
                                hideTrashIcon();
                                int[] overlayLocation = new int[2];
                                secondOverlayView.getLocationOnScreen(overlayLocation);
                                Rect overlayRect = new Rect(overlayLocation[0], overlayLocation[1], overlayLocation[0] + secondOverlayView.getWidth(), overlayLocation[1] + secondOverlayView.getHeight());

                                int[] trashLocation = new int[2];
                                trashIconView.getLocationOnScreen(trashLocation);
                                Rect trashRect = new Rect(trashLocation[0], trashLocation[1], trashLocation[0] + trashIconView.getWidth(), trashLocation[1] + trashIconView.getHeight());

                                if (Rect.intersects(overlayRect, trashRect)) {
                                    hideSecondOverlay();
                                    stopSelf(); // 停止服务，确保下次进入设置页面时状态为关闭
                                }
                            }
                            isDragging = false;
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    private void hideSecondOverlay() {
        if (secondOverlayView != null) {
            windowManager.removeView(secondOverlayView);
            secondOverlayView = null;
            secondOverlayLayoutParams = null;
            isSecondOverlayShown = false;


        }
    }

    public boolean isSecondOverlayShown() {
        return secondOverlayView != null;
    }

    public static boolean isSecondOverlayActive() {
        return isSecondOverlayShown;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (secondOverlayView != null) {
            // Remove the view first to re-add it with updated parameters
            windowManager.removeView(secondOverlayView);
            secondOverlayView = null; // Set to null so showSecondOverlay recreates it
            showSecondOverlay(false); // Re-add the overlay with updated dimensions
        }
    }

    private void showTrashIcon() {
        if (trashIconView != null && trashIconView.getParent() == null) {
            windowManager.addView(trashIconView, trashIconParams);
        }
    }

    private void hideTrashIcon() {
        if (trashIconView != null && trashIconView.getParent() != null) {
            windowManager.removeView(trashIconView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isOverlayServiceRunning = false;
        if (sharedPreferences != null && preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        hideSecondOverlay();
        if (trashIconView != null && trashIconView.getParent() != null) {
            windowManager.removeView(trashIconView);
        }
        stopForeground(true); // 停止前台服务并移除通知
    }
}