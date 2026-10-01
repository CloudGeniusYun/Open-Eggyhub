package com.eggyhub.android;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.Intent;
import android.graphics.Paint;
import android.widget.Toast;
import com.eggyhub.android.log.AppLogger;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.Color;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import com.eggyhub.android.utils.OkHttpClientFactory;

import com.google.gson.annotations.SerializedName;


/**
 * 分享码活动
 * 展示分享码列表，处理分类切换、点赞和领取分享码等操作
 */
public class ShareCodeActivity extends BaseActivity {

    // 定义每个分类下最多缓存的分享码数量
    private static final int MAX_CACHED_SHARE_CODES = 100;

    /**
     * 分类列表ChipGroup（Material Design 2）
     */
    private com.google.android.material.chip.ChipGroup chipGroupCategory;
    /**
     * 蛋码块数量显示TextView
     */
    private TextView tvEggBlockCount;
    /**
     * 蛋码碎片数量显示TextView
     */
    private TextView tvEggFragmentCount;
    /**
     * 分享码列表RecyclerView
     */
    private RecyclerView shareCodeRecyclerView;
    /**
     * 分享码适配器
     */
    private ShareCodeAdapter shareCodeAdapter;
    /**
     * 分享码数据列表
     */
    private List<ShareCodeItem> shareCodeList = new ArrayList<>();

    /**
     * 警告Banner（仅id=9分类显示）
     */
    private TextView tvWarningBanner;

    /**
     * OkHttpClient实例，用于网络请求
     */
    private OkHttpClient client = OkHttpClientFactory.getSharedClient();
    /**
     * 日志标签
     */
    private static final String TAG = "ShareCodeActivity";
    /**
     * Gson实例，用于JSON解析
     */
    private Gson gson = new Gson();

    /**
     * 当前选中的分类ID，默认为1
     */
    private int currentCategoryId = 1; // Default category ID
    private int currentPage = 0; // 当前加载的起始索引，用于无限滚动
    private boolean isLoading = false; // 是否正在加载数据
    private boolean hasMoreData = true; // 是否还有更多数据可加载

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ShareCodePrefs";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_SHARE_CODES_PREFIX = "share_codes_";
    private static final String KEY_USER_ASSETS_BLOCKS = "user_assets_blocks";
    private static final String KEY_USER_ASSETS_FRAGMENTS = "user_assets_fragments";
    // 弹窗相关
    private android.app.Dialog loadingDialog;
    private com.eggyhub.android.StatusLoadingView statusLoadingView;
    private TextView tvLoadingMessage;
    private android.widget.LinearLayout layoutShareCode;
    private TextView tvShareCode;
    private TextView btnCopyCode;

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new android.app.Dialog(this);
            loadingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            loadingDialog.setContentView(R.layout.dialog_loading_success);
            loadingDialog.setCancelable(false); // 禁止点击外部关闭，防止误触

            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = (int) (metrics.widthPixels * 0.65);
                loadingDialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            statusLoadingView = loadingDialog.findViewById(R.id.status_loading_view);
            tvLoadingMessage = loadingDialog.findViewById(R.id.tv_loading_message);
            layoutShareCode = loadingDialog.findViewById(R.id.layout_share_code);
            tvShareCode = loadingDialog.findViewById(R.id.tv_share_code);
            btnCopyCode = loadingDialog.findViewById(R.id.btn_copy_code);
        }
        
        // 重置状态
        if (layoutShareCode != null) layoutShareCode.setVisibility(android.view.View.GONE);
        // 重新初始化加载动画
        if (statusLoadingView != null) {
            statusLoadingView.startLoading(); 
        }

        if (tvLoadingMessage != null) {
            tvLoadingMessage.setText(message);
        }
        
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void showSuccessDialog(String code) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            if (statusLoadingView != null) statusLoadingView.startSuccess();
            if (tvLoadingMessage != null) tvLoadingMessage.setText("领取成功");
            
            if (layoutShareCode != null && code != null && !code.isEmpty()) {
                layoutShareCode.setVisibility(android.view.View.VISIBLE);
                if (tvShareCode != null) tvShareCode.setText(code);
                
                if (btnCopyCode != null) {
                    btnCopyCode.setOnClickListener(v -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("EggyHub Code", code);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(ShareCodeActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            // 5秒后自动关闭
            new android.os.Handler().postDelayed(() -> {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            }, 5000);
        }
    }

    private void showErrorDialog(String errorMessage) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            if (statusLoadingView != null) statusLoadingView.startError();
            if (tvLoadingMessage != null) tvLoadingMessage.setText(errorMessage);
            
            // 1.5秒后自动关闭
            new android.os.Handler().postDelayed(() -> {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            }, 1500);
        } else {
             // 如果弹窗没显示（例如直接失败），则用Toast兜底
             Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    /**
     * 活动创建时调用
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_code);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 初始化返回按钮
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());

        initViews();
        setupChipGroup();
        loadLocalData();
        fetchCategories();
        fetchShareCodeData();
        fetchUserAssets();
    }

    private void loadLocalData() {
        loadLocalCategories();
        loadLocalShareCodes();
        loadLocalUserAssets();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        chipGroupCategory = findViewById(R.id.chipGroupCategory);

        tvEggBlockCount = findViewById(R.id.tv_egg_block_count);
        tvEggFragmentCount = findViewById(R.id.tv_egg_fragment_count);

        // 初始化警告Banner
        tvWarningBanner = findViewById(R.id.tv_warning_banner);

        shareCodeRecyclerView = findViewById(R.id.share_code_list);

        // 初始化已领取分享码文本视图，添加下划线并设置点击事件
        TextView tvClaimedShareCodes = findViewById(R.id.tv_claimed_share_codes);
        tvClaimedShareCodes.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        tvClaimedShareCodes.getPaint().setAntiAlias(true);
        tvClaimedShareCodes.setOnClickListener(v -> {
            Intent intent = new Intent(ShareCodeActivity.this, ClaimedShareCodeActivity.class);
            startActivity(intent);
        });

        // 观察主题变化，通知adapter刷新
        observeThemeForRecyclerView();
    }

    /**
     * 观察主题变化并通知RecyclerView adapter刷新
     */
    private void observeThemeForRecyclerView() {
        themeViewModel.getCardViewTheme().observe(this, cardViewTheme -> {
            if (shareCodeAdapter != null) {
                // 更新adapter的主题并刷新
                shareCodeAdapter.setCardViewTheme(cardViewTheme);
                shareCodeAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 设置分类ChipGroup（使用Material Design 2 Chip组件）
     */
    private void setupChipGroup() {
        // ChipGroup默认设置已在XML中配置（singleSelection=true）
    }

    /**
     * 更新分类Button列表（使用MaterialButton代替Chip，样式完全一致）
     */
    private void updateCategoryChips(List<CategoryItem> categories) {
        chipGroupCategory.removeAllViews();

        for (int i = 0; i < categories.size(); i++) {
            CategoryItem category = categories.get(i);
            
            // 使用MaterialButton，统一样式：胶囊状
            com.google.android.material.button.MaterialButton button = 
                new com.google.android.material.button.MaterialButton(this);
            
            button.setText(category.getName());
            button.setCheckable(true);
            
            // 统一基础样式（所有按钮都一样）
            // 禁用MaterialButton默认样式
            button.setStrokeWidth(0);
            button.setRippleColorResource(android.R.color.transparent);
            button.setElevation(0f);
            button.setStateListAnimator(null);
            
            // 【关键】禁用backgroundTint，否则自定义背景会被覆盖（显示白色）
            androidx.core.view.ViewCompat.setBackgroundTintList(button, null);
            
            // 固定尺寸（全部使用具体数值）
            button.setCornerRadius(15); // 15px圆角
            button.setTextSize(13f); // 字体大小13f（比10f大一点）
            button.setAllCaps(false);
            
            // 清除MaterialButton的默认最小高度和宽度
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            
            // 固定尺寸计算（具体数值，不用比例）
            float density = getResources().getDisplayMetrics().density;
            int fixedHeight = (int)(40f * density); // 固定高度40dp
            int fixedWidth = (int)(90f * density); // 固定宽度90dp
            
            // 使用LinearLayout.LayoutParams强制设置固定尺寸（ChipGroup继承自LinearLayout）
            android.widget.LinearLayout.LayoutParams layoutParams = 
                new android.widget.LinearLayout.LayoutParams(fixedWidth, fixedHeight);
            button.setLayoutParams(layoutParams);
            
            // 同时设置min/max确保固定尺寸
            button.setMinHeight(fixedHeight);
            button.setMaxHeight(fixedHeight);
            button.setMinWidth(fixedWidth);
            button.setMaxWidth(fixedWidth);
            
            // 最小padding（高度更小）
            int paddingH = (int)(8f * density); // 左右padding 8dp
            int paddingV = 0; // 上下padding为0
            button.setPadding(paddingH, paddingV, paddingH, paddingV);
            
            // 特殊分类（id=9）使用紫色渐变，其他分类使用普通样式
            if (category.getId() == 9) {
                // 特殊分类：创建紫色渐变背景选择器
                StateListDrawable stateListDrawable = new StateListDrawable();
                
                // 选中状态：紫色渐变
                GradientDrawable gradientSelected = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{
                        Color.parseColor("#9b59b6"),
                        Color.parseColor("#8e44ad"),
                        Color.parseColor("#6c3483")
                    }
                );
                gradientSelected.setCornerRadius(15f); // 圆角15px
                gradientSelected.setShape(GradientDrawable.RECTANGLE);
                
                // 未选中状态：浅灰色（统一颜色）
                GradientDrawable gradientUnselected = new GradientDrawable();
                gradientUnselected.setColor(Color.parseColor("#F5F5F5")); // 统一使用#F5F5F5
                gradientUnselected.setCornerRadius(15f); // 圆角15px
                gradientUnselected.setShape(GradientDrawable.RECTANGLE);
                
                stateListDrawable.addState(new int[]{android.R.attr.state_checked}, gradientSelected);
                stateListDrawable.addState(new int[]{-android.R.attr.state_checked}, gradientUnselected);
                
                button.setBackground(stateListDrawable);
                button.setTextColor(getResources().getColorStateList(R.color.chip_text_selector));
            } else {
                // 普通分类：创建普通背景选择器（和id=9一样的样式）
                StateListDrawable stateListDrawable = new StateListDrawable();
                
                // 选中状态：blue_eggyhub颜色
                GradientDrawable gradientSelected = new GradientDrawable();
                gradientSelected.setColor(getResources().getColor(R.color.blue_eggyhub));
                gradientSelected.setCornerRadius(15f); // 圆角15px
                gradientSelected.setShape(GradientDrawable.RECTANGLE);
                
                // 未选中状态：浅灰色
                GradientDrawable gradientUnselected = new GradientDrawable();
                gradientUnselected.setColor(Color.parseColor("#F5F5F5"));
                gradientUnselected.setCornerRadius(15f); // 圆角15px
                gradientUnselected.setShape(GradientDrawable.RECTANGLE);
                
                stateListDrawable.addState(new int[]{android.R.attr.state_checked}, gradientSelected);
                stateListDrawable.addState(new int[]{-android.R.attr.state_checked}, gradientUnselected);
                
                button.setBackground(stateListDrawable);
                button.setTextColor(getResources().getColorStateList(R.color.chip_text_selector));
            }
            
            // 设置第一个Button为选中状态
            if (i == 0) {
                button.setChecked(true);
                currentCategoryId = category.getId();
            }

            button.setOnClickListener(v -> {
                AppLogger.d(TAG, "Category clicked: " + category.getName() + ", ID: " + category.getId());
                // 更新所有按钮的选中状态
                for (int j = 0; j < chipGroupCategory.getChildCount(); j++) {
                    com.google.android.material.button.MaterialButton childButton = 
                        (com.google.android.material.button.MaterialButton) chipGroupCategory.getChildAt(j);
                    childButton.setChecked(j == chipGroupCategory.indexOfChild(button));
                }
                
                currentCategoryId = category.getId();
                
                // 根据分类ID显示/隐藏警告Banner
                if (currentCategoryId == 9) {
                    tvWarningBanner.setVisibility(android.view.View.VISIBLE);
                } else {
                    tvWarningBanner.setVisibility(android.view.View.GONE);
                }
                
                // 更新adapter的当前分类ID（用于item渐变背景）
                if (shareCodeAdapter != null) {
                    shareCodeAdapter.setCurrentCategoryId(currentCategoryId);
                }
                
                currentPage = 0;
                hasMoreData = true;
                loadLocalShareCodes();
                fetchShareCodeData();
            });

            chipGroupCategory.addView(button);
        }
    }

    private void loadLocalCategories() {
        String cachedCategories = sharedPreferences.getString(KEY_CATEGORIES, null);
        if (cachedCategories != null) {
            Type listType = new TypeToken<List<CategoryItem>>() {}.getType();
            List<CategoryItem> categories = gson.fromJson(cachedCategories, listType);
            updateCategoryChips(categories);
        }
    }

    /**
     * 获取分类数据
     */
    private void fetchCategories() {
        String url = "https://eggyhub.top/api/giftgroups";
        Request request = new Request
        .Builder()
        .url(url)
        .get()
        .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    String cachedCategories = sharedPreferences.getString(KEY_CATEGORIES, null);
                    if (cachedCategories == null) {
                        Toast.makeText(ShareCodeActivity.this, "获取分类失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        final String responseData = responseBody.string();
                        sharedPreferences.edit().putString(KEY_CATEGORIES, responseData).apply();
                        runOnUiThread(() -> {
                            Type listType = new TypeToken<List<CategoryItem>>() {}.getType();
                            List<CategoryItem> categories = gson.fromJson(responseData, listType);
                            AppLogger.d(TAG, "Fetched categories: " + categories.size());
                            updateCategoryChips(categories);
                            if (!categories.isEmpty()) {
                                fetchShareCodeData();
                                AppLogger.d(TAG, "Initial category selected: " + categories.get(0).getName() + ", ID: " + categories.get(0).getId());
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            String cachedCategories = sharedPreferences.getString(KEY_CATEGORIES, null);
                            if (cachedCategories == null) {
                                Toast.makeText(ShareCodeActivity.this, "获取分类失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 设置分享码RecyclerView
     */
    private void setupRecyclerView() {
        android.util.Log.d("ShareCodeActivity", "setupRecyclerView: 开始设置RecyclerView");
        
        shareCodeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shareCodeAdapter = new ShareCodeAdapter(shareCodeList, new ShareCodeAdapter.OnLikeClickListener() {
            @Override
            public void onLikeClick(int shareCodeId) {
                AppLogger.d(TAG, "Like clicked for ID: " + shareCodeId);
                sendLikeRequest(shareCodeId);
            }
        }, new ShareCodeAdapter.OnClaimClickListener() {
            @Override
            public void onClaimClick(ShareCodeItem item) {
                AppLogger.d(TAG, "Claim clicked for ID: " + item.getId());
                sendClaimRequest(item);
            }
        });

        // 设置当前主题到adapter
        if (themeViewModel.getCardViewTheme().getValue() != null) {
            shareCodeAdapter.setCardViewTheme(themeViewModel.getCardViewTheme().getValue());
        }

        shareCodeRecyclerView.setAdapter(shareCodeAdapter);
        
        android.util.Log.d("ShareCodeActivity", "setupRecyclerView: RecyclerView已设置，列表大小=" + shareCodeList.size());
        shareCodeRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            // 滚动到底部，加载更多数据
                            AppLogger.d(TAG, "Loading more data...");
                            currentPage += 6; // 每次加载6条
                            fetchShareCodeData(); // 重新调用fetchShareCodeData，它会使用更新后的currentPage
                        }
                    }
                }
            }
        });
    }

    private void loadLocalShareCodes() {
        android.util.Log.d("ShareCodeActivity", "loadLocalShareCodes: 开始加载本地分享码");
        
        String key = KEY_SHARE_CODES_PREFIX + currentCategoryId;
        String cachedShareCodes = sharedPreferences.getString(key, null);
        
        if (cachedShareCodes != null) {
            android.util.Log.d("ShareCodeActivity", "loadLocalShareCodes: 找到缓存数据");
            
            Type listType = new TypeToken<List<ShareCodeItem>>() {}.getType();
            List<ShareCodeItem> fetchedList = gson.fromJson(cachedShareCodes, listType);
            
            android.util.Log.d("ShareCodeActivity", "loadLocalShareCodes: 缓存数据数量=" + fetchedList.size());
            
            // 始终清除列表，因为 fetchedList 已经包含了从缓存中读取的所有（合并后的）数据
            shareCodeList.clear();
            if (currentPage == 0) {
                shareCodeRecyclerView.scrollToPosition(0);
            }
            shareCodeList.addAll(fetchedList);
            
            if (shareCodeAdapter == null) {
                setupRecyclerView();
            }
            shareCodeAdapter.notifyDataSetChanged();
        } else {
            android.util.Log.d("ShareCodeActivity", "loadLocalShareCodes: 没有缓存数据");
            
            if (shareCodeAdapter == null) {
                setupRecyclerView();
            }
        }
    }

    /**
     * 获取指定分类的分享码数据
     */
    private void fetchShareCodeData() {
        if (isLoading) return; // 避免重复加载
        isLoading = true;
        String url = "https://eggyhub.top/api/gifts?start=" + currentPage + "&grid=" + currentCategoryId;
        
        AppLogger.d(TAG, "Fetching share code data for category ID: " + currentCategoryId + ", URL: " + url);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    String key = KEY_SHARE_CODES_PREFIX + currentCategoryId;
                    String cachedShareCodes = sharedPreferences.getString(key, null);
                    if (cachedShareCodes == null) {
                        Toast.makeText(ShareCodeActivity.this, "获取分享码失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        final String responseData = responseBody.string();
                        String key = KEY_SHARE_CODES_PREFIX + currentCategoryId;

                        runOnUiThread(() -> {
                            Type listType = new TypeToken<List<ShareCodeItem>>() {}.getType();
                            List<ShareCodeItem> fetchedList = gson.fromJson(responseData, listType);

                            if (currentPage == 0) {
                                // 1. API 响应后清空当前列表（包含之前的缓存数据）
                                shareCodeList.clear();
                                shareCodeRecyclerView.scrollToPosition(0);
                                shareCodeList.addAll(fetchedList);

                                // 2. 更新本地缓存（覆盖旧缓存）
                                sharedPreferences.edit().putString(key, responseData).apply();
                            } else {
                                // 3. 分页加载，追加数据
                                shareCodeList.addAll(fetchedList);

                                // 4. 更新本地缓存：将当前完整的 shareCodeList 存入缓存，覆盖旧缓存
                                //    并限制缓存数量
                                List<ShareCodeItem> listToCache = new ArrayList<>(shareCodeList); // 创建一个副本以进行裁剪
                                if (listToCache.size() > MAX_CACHED_SHARE_CODES) {
                                    listToCache = listToCache.subList(listToCache.size() - MAX_CACHED_SHARE_CODES, listToCache.size());
                                    AppLogger.d(TAG, "Cached share codes trimmed to " + MAX_CACHED_SHARE_CODES + " items for category " + currentCategoryId);
                                }
                                sharedPreferences.edit().putString(key, gson.toJson(listToCache)).apply();
                            }

                            if (shareCodeAdapter == null) {
                                setupRecyclerView();
                            }
                            shareCodeAdapter.notifyDataSetChanged();
                            isLoading = false;

                            if (fetchedList == null || fetchedList.isEmpty()) {
                                hasMoreData = false;
                                if (currentPage > 0) {
                                    Toast.makeText(ShareCodeActivity.this, "已经到底了", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                hasMoreData = true;
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            isLoading = false;
                            String key = KEY_SHARE_CODES_PREFIX + currentCategoryId;
                            String cachedShareCodes = sharedPreferences.getString(key, null);
                            if (cachedShareCodes == null) {
                                AppLogger.e(TAG, "Unsuccessful response for share code data: " + response.code());
                                Toast.makeText(ShareCodeActivity.this, "获取分享码失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 发送点赞请求
     * @param shareCodeId 分享码 ID
     */
    private void sendLikeRequest(int shareCodeId) {
        String accessToken = SecureStorageManager.getAccessToken();

        String url = "https://eggyhub.top/api/gifts/like?id=" + shareCodeId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                 .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() -> Toast.makeText(ShareCodeActivity.this, "点赞请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        // 假设成功点赞响应不返回特定消息
                        runOnUiThread(() -> {
                            Toast.makeText(ShareCodeActivity.this, "点赞成功", Toast.LENGTH_SHORT).show();
                            // 不刷新整个列表，而是更新本地数据并刷新对应项
                            for (int i = 0; i < shareCodeList.size(); i++) {
                                ShareCodeItem item = shareCodeList.get(i);
                                if (item.getId() == shareCodeId) {
                                    item.setLikes(item.getLikes() + 1);
                                    if (shareCodeAdapter != null) {
                                        shareCodeAdapter.notifyItemChanged(i);
                                    }
                                    break;
                                }
                            }
                        });
                    } else if (response.code() == 403) {
                        // 处理403错误（已点赞）
                        runOnUiThread(() -> Toast.makeText(ShareCodeActivity.this, "您已经点过赞了", Toast.LENGTH_SHORT).show());
                    } else {
                        final String responseData = responseBody != null ? responseBody.string() : "";
                        LikeResponse likeResponse = gson.fromJson(responseData, LikeResponse.class);
                        runOnUiThread(() -> {
                            if (likeResponse != null && "have liked".equals(likeResponse.getMessage())) {
                                Toast.makeText(ShareCodeActivity.this, "您已经点过赞了", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ShareCodeActivity.this, "点赞请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 发送领取请求
     * @param item 分享码对象
     */
    private void sendClaimRequest(ShareCodeItem item) {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前的蛋码块数量
        int eggBlockCount = 0;
        try {
            // 注意：这里需要确保 tvEggBlockCount 内容是纯数字，或者进行清理
            String countStr = tvEggBlockCount.getText().toString();
            // 简单的正则提取数字，以防有其他字符
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(countStr);
            if (matcher.find()) {
                eggBlockCount = Integer.parseInt(matcher.group());
            }
        } catch (NumberFormatException e) {
            AppLogger.e(TAG, "Error parsing egg block count: " + e.getMessage());
        }

        // 只有当需要消耗蛋码块（item.getValue() == 50）且蛋码块数量为0时，才进行兑换
        if (item.getValue() == 50 && eggBlockCount == 0) {
            // 如果蛋码块数量为0，先尝试兑换蛋码块
            showLoadingDialog("正在兑换蛋码块...");
            sendExchangeEggBlockRequest(item.getId()); // 传入shareCodeId以便兑换成功后继续领取
            return;
        }

        // 正常领取逻辑
        performClaimRequest(item.getId());
    }

    private void performClaimRequest(int shareCodeId) {
        // 如果当前没有显示弹窗，则显示加载弹窗（可能是从兑换流程过来的，已经有弹窗了）
        if (loadingDialog == null || !loadingDialog.isShowing()) {
            showLoadingDialog("正在领取...");
        } else if (tvLoadingMessage != null) {
            tvLoadingMessage.setText("正在领取...");
        }
        
        String accessToken = SecureStorageManager.getAccessToken();

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        ClaimRequest claimRequest = new ClaimRequest(shareCodeId);
        Gson gson = new Gson();
        String jsonBody = gson.toJson(claimRequest);

        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/claim")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("领取请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        final String responseData = responseBody != null ? responseBody.string() : "";
                        runOnUiThread(() -> {
                            try {
                                ClaimResponse claimResponse = gson.fromJson(responseData, ClaimResponse.class);
                                if (claimResponse != null && claimResponse.getCode() != null) {
                                    // 成功，显示领取码
                                    showSuccessDialog(claimResponse.getCode());
                                    fetchShareCodeData(); // 刷新列表
                                    fetchUserAssets(); // 刷新用户资产
                                } else {
                                    showErrorDialog("领取失败: 无法获取兑换码");
                                }
                            } catch (Exception e) {
                                AppLogger.e(TAG, "JSON parsing error: " + e.getMessage());
                                showErrorDialog("领取失败: 数据解析错误");
                            }
                        });
                    } else {
                        final String errorBody = responseBody != null ? responseBody.string() : "";
                        runOnUiThread(() -> {
                            try {
                                ErrorResponse errorResponse = gson.fromJson(errorBody, ErrorResponse.class);
                                if (errorResponse != null && errorResponse.getMessage() != null) {
                                    showErrorDialog("领取失败: " + errorResponse.getMessage());
                                } else {
                                    showErrorDialog("领取失败: " + response.code());
                                }
                            } catch (Exception e) {
                                AppLogger.e(TAG, "Error parsing error response: " + e.getMessage());
                                showErrorDialog("领取失败: " + response.code());
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 发送兑换蛋码块请求
     * 参考 TaskActivity 的实现，使用 api/coins/tolimit
     * @param shareCodeId 领取成功后继续领取
     */
    private void sendExchangeEggBlockRequest(int shareCodeId) {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show());
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        
        // Correct API from TaskActivity
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/coins/tolimit")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("兑换请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    final String responseData = responseBody != null ? responseBody.string() : "";
                    runOnUiThread(() -> {
                        try {
                            if (response.isSuccessful()) {
                                // 兑换成功，更新状态文字，然后发起领取
                                // 注意：TaskActivity 中没有使用 giftid，所以这里直接使用传入的 shareCodeId
                                if (tvLoadingMessage != null) tvLoadingMessage.setText("正在领取...");
                                performClaimRequest(shareCodeId);
                            } else {
                                // 尝试解析错误信息
                                try {
                                    ErrorResponse errorResponse = gson.fromJson(responseData, ErrorResponse.class);
                                    if (errorResponse != null && errorResponse.getMessage() != null) {
                                        showErrorDialog("兑换失败: " + errorResponse.getMessage());
                                    } else {
                                        showErrorDialog("兑换失败: " + response.code());
                                    }
                                } catch (Exception e) {
                                    showErrorDialog("兑换失败: " + response.code());
                                }
                            }
                        } catch (Exception e) {
                            AppLogger.e(TAG, "Error processing exchange response: " + e.getMessage());
                            showErrorDialog("兑换处理错误");
                        }
                    });
                }
            }
        });
    }

    private void loadLocalUserAssets() {
        String cachedBlocks = sharedPreferences.getString(KEY_USER_ASSETS_BLOCKS, null);
        if (cachedBlocks != null) {
            tvEggBlockCount.setText(cachedBlocks);
        }
        String cachedFragments = sharedPreferences.getString(KEY_USER_ASSETS_FRAGMENTS, null);
        if (cachedFragments != null) {
            tvEggFragmentCount.setText(cachedFragments);
        }
    }

    /**
     * 获取用户资产（蛋码块和蛋码碎片数量）
     */
    private void fetchUserAssets() {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            tvEggBlockCount.setText("0");
            tvEggFragmentCount.setText("0");
            return;
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();

        // 获取蛋码碎片
        Request fragmentsRequest = new Request.Builder()
                .url("https://eggyhub.top/api/coins")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(fragmentsRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (sharedPreferences.getString(KEY_USER_ASSETS_FRAGMENTS, null) == null) {
                        Toast.makeText(ShareCodeActivity.this, "获取蛋码碎片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvEggFragmentCount.setText("0");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String responseData = responseBody != null ? responseBody.string() : "";
                        try {
                            CoinsResponse coinsResponse = gson.fromJson(responseData, CoinsResponse.class);
                            int coins = coinsResponse != null ? coinsResponse.getCoins() : 0;
                            sharedPreferences.edit().putString(KEY_USER_ASSETS_FRAGMENTS, String.valueOf(coins)).apply();
                            runOnUiThread(() -> tvEggFragmentCount.setText(String.valueOf(coins)));
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                if (sharedPreferences.getString(KEY_USER_ASSETS_FRAGMENTS, null) == null) {
                                    Toast.makeText(ShareCodeActivity.this, "解析蛋码碎片数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    tvEggFragmentCount.setText("0");
                                }
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (sharedPreferences.getString(KEY_USER_ASSETS_FRAGMENTS, null) == null) {
                                Toast.makeText(ShareCodeActivity.this, "获取蛋码碎片失败: " + response.message(), Toast.LENGTH_SHORT).show();
                                tvEggFragmentCount.setText("0");
                            }
                        });
                    }
                }
            }
        });

        // 获取蛋码块
        Request blocksRequest = new Request.Builder()
                .url("https://eggyhub.top/api/remains")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(blocksRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (sharedPreferences.getString(KEY_USER_ASSETS_BLOCKS, null) == null) {
                        Toast.makeText(ShareCodeActivity.this, "获取蛋码块数量失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvEggBlockCount.setText("0");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String responseData = responseBody != null ? responseBody.string() : "";
                        try {
                            RemainsResponse remainsResponse = gson.fromJson(responseData, RemainsResponse.class);
                            int message = remainsResponse != null ? remainsResponse.getMessage() : 0;
                            sharedPreferences.edit().putString(KEY_USER_ASSETS_BLOCKS, String.valueOf(message)).apply();
                            runOnUiThread(() -> tvEggBlockCount.setText(String.valueOf(message)));
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                if (sharedPreferences.getString(KEY_USER_ASSETS_BLOCKS, null) == null) {
                                    Toast.makeText(ShareCodeActivity.this, "解析蛋码块数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    tvEggBlockCount.setText("0");
                                }
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (sharedPreferences.getString(KEY_USER_ASSETS_BLOCKS, null) == null) {
                                Toast.makeText(ShareCodeActivity.this, "获取蛋码块数量失败: " + response.message(), Toast.LENGTH_SHORT).show();
                                tvEggBlockCount.setText("0");
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 点赞响应类
     */
    private static class LikeResponse {
        @SerializedName("message")
        private String message;

        public String getMessage() {
            return message;
        }
    }

    /**
     * 领取响应类
     */
    private static class ClaimResponse {
        @SerializedName("code")
        private String code;

        public String getCode() {
            return code;
        }
    }

    /**
     * 错误响应类
     */
    private static class ErrorResponse {
        @SerializedName("message")
        private String message;

        public String getMessage() {
            return message;
        }
    }

    /**
     * 用户资产响应类
     */
    private static class UserAssetsResponse {
        @SerializedName("egg_block_count")
        private int eggBlockCount;
        @SerializedName("egg_fragment_count")
        private int eggFragmentCount;

        public int getEggBlockCount() {
            return eggBlockCount;
        }

        public int getEggFragmentCount() {
            return eggFragmentCount;
        }
    }
}