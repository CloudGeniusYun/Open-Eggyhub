package com.eggyhub.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.transition.TransitionManager;
import android.view.ViewGroup;

import android.graphics.Color;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;

import com.eggyhub.android.utils.OkHttpClientFactory;
import java.util.List;
import android.content.SharedPreferences;

/**
 * 任务活动页面
 * 负责展示任务列表、任务领取和奖励兑换等功能
 */
public class TaskActivity extends BaseActivity {

    /**
     * 任务列表的RecyclerView组件
     */
    private RecyclerView recyclerViewTasks;
    /**
     * 任务列表的适配器
     */
    private TaskAdapter taskAdapter;
    /**
     * 显示蛋码碎片数量的TextView
     */
    private TextView textViewEggCodeFragments;
    /**
     * 显示蛋码块数量的TextView
     */
    private TextView textViewEggCodeBlocks;
    private MaterialButton btnExchange;
    private MaterialButton btnToggleExpand;
    private MaterialCardView expandableExchangeCard;
    private Slider exchangeSlider;
    private TextView textViewExchangeFragments;
    private TextView textViewExchangeBlocks;
    private MaterialButton btnMinus;
    private MaterialButton btnPlus;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private int totalExchangeRequests = 0;
    private int completedExchangeRequests = 0;
    private int successfulExchangeRequests = 0;
    private int failedExchangeRequests = 0;

    private void checkTutorialStatus() {
        TutorialManager manager = TutorialManager.getInstance(this);
        if (manager.isTutorialRunning() && TutorialManager.TUTORIAL_TASK.equals(manager.getCurrentTutorial())) {
            int step = manager.getStepIndex();
            // 只有当步骤 >= 1 时（即从首页点击“任务”后）才显示
            if (step == 1) {
                // 第一步：展示资产信息
                View topSummary = findViewById(R.id.topSummaryContainer);
                if (topSummary != null) {
                    topSummary.post(() -> {
                        GuideHelper.show(this, topSummary, "第二步：这里显示你拥有的蛋码碎片和蛋码块数量", false, () -> {
                            manager.nextStep();
                            checkTutorialStatus();
                        });
                    });
                }
            } else if (step == 2) {
                // 第二步：引导领取任务
                if (recyclerViewTasks != null) {
                    recyclerViewTasks.post(() -> {
                        GuideHelper.show(this, recyclerViewTasks, "第三步：你可以在任务列表中领取并完成各种任务来获取奖励", false, () -> {
                            manager.nextStep();
                            checkTutorialStatus();
                        });
                    });
                }
            } else if (step == 3) {
                // 第三步：引导展开兑换面板
                if (btnToggleExpand != null) {
                    btnToggleExpand.post(() -> {
                        GuideHelper.show(this, btnToggleExpand, "第四步：点击展开按钮以显示兑换面板", true, () -> {
                            // nextStep 将在 toggleExchangeCard 中处理
                        });
                    });
                }
            } else if (step == 4) {
                // 第四步：引导点击兑换按钮
                if (btnExchange != null) {
                    btnExchange.post(() -> {
                        GuideHelper.show(this, btnExchange, "第五步：点击“兑换”可以将蛋码碎片兑换为蛋码块", false, () -> {
                            manager.finishTutorial();
                        });
                    });
                }
            }
        }
    }

    /**
     * Activity创建时调用的方法
     * 初始化UI组件、设置布局和加载数据
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_task); // 设置任务页面布局
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "布局加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 初始化RecyclerView
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this)); // 设置布局管理器

        // 创建并设置适配器
        taskAdapter = new TaskAdapter(new ArrayList<>()); // 初始为空列表
        recyclerViewTasks.setAdapter(taskAdapter); // 设置适配器

        // 设置底部导航
        setupBottomNavigation();

        // 获取任务数据
        fetchTasks();

        // 初始化蛋码碎片和蛋码块TextView
        textViewEggCodeFragments = findViewById(R.id.textViewEggCodeFragments);
        textViewEggCodeBlocks = findViewById(R.id.textViewEggCodeBlocks);

        // 获取蛋码碎片数据
        fetchCoins();

        // 获取蛋码块数据
        fetchEggCodeBlocks();

        // 初始化兑换按钮并设置点击事件
        btnExchange = findViewById(R.id.btnExchange);
        btnExchange.setOnClickListener(v -> exchangeCoins());

        // 初始化可展开区域
        expandableExchangeCard = findViewById(R.id.expandableExchangeCard);
        btnToggleExpand = findViewById(R.id.btnToggleExpand);
        exchangeSlider = findViewById(R.id.exchangeSlider);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        textViewExchangeFragments = findViewById(R.id.textViewExchangeFragments);
        textViewExchangeBlocks = findViewById(R.id.textViewExchangeBlocks);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(0xFF0096FF); // 设置刷新进度条颜色为蓝色
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 下拉刷新逻辑
            fetchTasks();
            fetchCoins();
            fetchEggCodeBlocks();
        });

        btnToggleExpand.setOnClickListener(v -> toggleExchangeCard());
        
        setupSliderLogic();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(TaskActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkTutorialStatus();
        // 每次页面可见时自动刷新数据，确保任务状态同步
        fetchTasks();
        fetchCoins();
        fetchEggCodeBlocks();
    }

    /**
     * 设置底部导航
     * 初始化底部导航栏并设置各按钮的点击事件
     */
    private void setupBottomNavigation() {
        LinearLayout bottomNavBar = findViewById(R.id.bottom_nav_bar); // 获取底部导航栏
        
        // 首页按钮
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(TaskActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 清除栈顶所有Activity
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // 设置过渡动画
            });
        }
        
        // 发布按钮
        View navPublish = findViewById(R.id.navPublish);
        if (navPublish != null) {
            navPublish.setOnClickListener(v -> {
                Intent intent = new Intent(TaskActivity.this, PublishActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
        
        // 任务按钮（当前页面）
        View navTask = findViewById(R.id.navTask);
        if (navTask != null) {
            navTask.setSelected(true); // 设置为选中状态
        }
        
        // 我的按钮
        View navMy = findViewById(R.id.navProfile);
        if (navMy != null) {
            navMy.setOnClickListener(v -> {
                Intent intent = new Intent(TaskActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void toggleExchangeCard() {
        boolean isVisible = expandableExchangeCard.getVisibility() == View.VISIBLE;
        TransitionManager.beginDelayedTransition((ViewGroup) expandableExchangeCard.getParent());
        if (isVisible) {
            expandableExchangeCard.setVisibility(View.GONE);
            btnToggleExpand.setIconResource(R.drawable.ic_arrow_down);
        } else {
            expandableExchangeCard.setVisibility(View.VISIBLE);
            btnToggleExpand.setIconResource(R.drawable.ic_arrow_up);
            updateSliderRange();
            
            // 引导逻辑：如果是教程中，且在展开这一步，则进入下一步
            TutorialManager manager = TutorialManager.getInstance(this);
            if (manager.isTutorialRunning() && TutorialManager.TUTORIAL_TASK.equals(manager.getCurrentTutorial())) {
                if (manager.getStepIndex() == 3) {
                    manager.nextStep();
                    // 延迟一小会儿显示下一个引导，等待展开动画完成
                    btnToggleExpand.postDelayed(this::checkTutorialStatus, 300);
                }
            }
        }
    }

    private void setupSliderLogic() {
        exchangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            int blocks = (int) value;
            if (slider.isEnabled()) {
                updateExchangePreview(blocks);
                boolean canExchange = blocks > 0;
                btnExchange.setEnabled(canExchange);
                btnExchange.setTextColor(canExchange ? Color.WHITE : Color.parseColor("#80FFFFFF"));
            }
        });

        // 强制在停止拖动后重绘，解决部分设备上提示框残留的问题
        exchangeSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                slider.invalidate();
                if (slider.getParent() != null) {
                    ((View) slider.getParent()).invalidate();
                }
            }
        });

        btnMinus.setOnClickListener(v -> {
            if (exchangeSlider.isEnabled()) {
                float newValue = exchangeSlider.getValue() - 1.0f;
                if (newValue >= exchangeSlider.getValueFrom()) {
                    exchangeSlider.setValue(newValue);
                }
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (exchangeSlider.isEnabled()) {
                float newValue = exchangeSlider.getValue() + 1.0f;
                if (newValue <= exchangeSlider.getValueTo()) {
                    exchangeSlider.setValue(newValue);
                }
            }
        });
    }

    private void updateSliderRange() {
        try {
            String fragmentText = textViewEggCodeFragments.getText().toString();
            int currentFragments = 0;
            if (!fragmentText.isEmpty()) {
                currentFragments = Integer.parseInt(fragmentText);
            }
            
            int maxBlocks = currentFragments / 50;
            
            // Slider 必须满足 valueFrom < valueTo
            // 我们统一设置 valueFrom 为 0.0，在布局中已设置
            
            if (maxBlocks >= 1) {
                exchangeSlider.setEnabled(true);
                btnPlus.setEnabled(true);
                btnMinus.setEnabled(true);
                
                // 确保 valueTo 始终大于 valueFrom (0.0)
                exchangeSlider.setValueTo((float) maxBlocks);
                
                // 确保当前值在合理范围内 [1, maxBlocks]
                float currentValue = exchangeSlider.getValue();
                if (currentValue < 1.0f) {
                    exchangeSlider.setValue(1.0f);
                } else if (currentValue > (float) maxBlocks) {
                    exchangeSlider.setValue((float) maxBlocks);
                } else {
                    updateExchangePreview((int) currentValue);
                }
                
                btnExchange.setEnabled(true);
                btnExchange.setTextColor(Color.WHITE);
            } else {
                // 不足兑换1个的情况
                exchangeSlider.setEnabled(false);
                btnPlus.setEnabled(false);
                btnMinus.setEnabled(false);
                btnExchange.setEnabled(false);
                btnExchange.setTextColor(Color.parseColor("#80FFFFFF"));
                
                // 即使不足，也让 valueTo > valueFrom (0.0)
                exchangeSlider.setValueTo(1.0f);
                exchangeSlider.setValue(0.0f); // 不足时设为0
                
                updateExchangePreview(1); // 预览仍显示1个所需碎片
            }
        } catch (NumberFormatException e) {
            exchangeSlider.setEnabled(false);
            btnExchange.setEnabled(false);
        }
    }

    private void updateExchangePreview(int blocks) {
        int fragments = blocks * 50;
        textViewExchangeFragments.setText(String.valueOf(fragments));
        textViewExchangeBlocks.setText(String.valueOf(blocks));
    }

    /**
     * 获取任务数据
     * 从服务器获取任务列表数据并更新 UI
     */
    private void fetchTasks() {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            // 跳转到登录页面
            Intent intent = new Intent(TaskActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // 结束当前 Activity
            return;
        }

        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        
        // 尝试从缓存中加载数据并显示
        String cachedTasksJson = preferences.getString("cached_tasks", null);
        if (cachedTasksJson != null) {
            try {
                List<TaskItem> cachedTasks = parseTasksJson(cachedTasksJson);
                runOnUiThread(() -> taskAdapter.updateTasks(cachedTasks));
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/tasks")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            /**
             * 请求失败时调用
             * @param call 请求对象
             * @param e 异常信息
             */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(TaskActivity.this, "获取任务失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
                    if (response.isSuccessful()) {
                        String responseBody = body.string();
                        try {
                            Gson gson = new Gson();
                            TaskResponse taskResponse = gson.fromJson(responseBody, TaskResponse.class);
                            List<TaskResponse.TaskData> tasksData = taskResponse.getTasks();
                            
                            List<TaskItem> newTasks = new ArrayList<>();
                            if (tasksData != null) {
                                for (TaskResponse.TaskData data : tasksData) {
                                    newTasks.add(convertTaskDataToItem(data));
                                }
                            }

                            // 更新UI并缓存最新数据
                            runOnUiThread(() -> {
                                taskAdapter.updateTasks(newTasks);
                            });
                            // Cache the list of tasks as JSON array to match previous behavior
                            preferences.edit().putString("cached_tasks", gson.toJson(tasksData)).apply();

                        } catch (JsonSyntaxException e) {
                            runOnUiThread(() -> Toast.makeText(TaskActivity.this, "解析任务数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(TaskActivity.this, "获取任务失败: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private TaskItem convertTaskDataToItem(TaskResponse.TaskData data) {
        int claimed = 0;
        int completed = 0;
        if (data.getUser() != null) {
            claimed = data.getUser().getClaimed();
            completed = data.getUser().getCompleted();
        }
        
        String status;
        boolean canClaim = false;
        if (completed >= data.getMaxTimes()) {
            status = "已完成";
            canClaim = (claimed < completed);
        } else if (completed > 0) {
            status = "进行中";
            canClaim = (completed > claimed);
        } else {
            status = "未开始";
        }
        
        return new TaskItem(data.getId(), data.getName(), data.getDescription(), data.getReward(), status, canClaim, data.getMaxTimes(), completed, claimed, data.getRefresh());
    }

    private List<TaskItem> parseTasksJson(String jsonString) throws JsonSyntaxException {
        Gson gson = new Gson();
        java.lang.reflect.Type listType = new TypeToken<List<TaskResponse.TaskData>>(){}.getType();
        List<TaskResponse.TaskData> dataList = gson.fromJson(jsonString, listType);
        
        List<TaskItem> tasks = new ArrayList<>();
        if (dataList != null) {
            for (TaskResponse.TaskData data : dataList) {
                tasks.add(convertTaskDataToItem(data));
            }
        }
        return tasks;
    }

    /**
     * 任务领取方法
     * @param taskId 要领取的任务ID
     */
    private int totalClaimRequests = 0;
    private int completedClaimRequests = 0;
    private int successfulClaimRequests = 0;
    private int failedClaimRequests = 0;

    public void claimTask(int taskId, int completed, int claimed) {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        int claimableTimes = completed - claimed;
        if (claimableTimes <= 0) {
            Toast.makeText(this, "没有可领取的任务次数", Toast.LENGTH_SHORT).show();
            return;
        }

        totalClaimRequests = claimableTimes;
        completedClaimRequests = 0;
        successfulClaimRequests = 0;
        failedClaimRequests = 0;

        for (int i = 0; i < claimableTimes; i++) {
            claimTaskInternal(taskId, accessToken);
        }
    }

    private void claimTaskInternal(int taskId, String accessToken) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/tasks/claim?taskid=" + taskId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    completedClaimRequests++;
                    failedClaimRequests++;
                    checkAllClaimsCompleted();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    runOnUiThread(() -> {
                        completedClaimRequests++;
                        if (response.isSuccessful()) {
                            successfulClaimRequests++;
                        } else {
                            failedClaimRequests++;
                        }
                        checkAllClaimsCompleted();
                    });
                }
            }
        });
    }

    private void checkAllClaimsCompleted() {
        if (completedClaimRequests == totalClaimRequests) {
            if (successfulClaimRequests > 0) {
                Toast.makeText(TaskActivity.this, "成功领取 " + successfulClaimRequests + " 次任务", Toast.LENGTH_SHORT).show();
            }
            if (failedClaimRequests > 0) {
                Toast.makeText(TaskActivity.this, "有 " + failedClaimRequests + " 次任务领取失败", Toast.LENGTH_SHORT).show();
            }
            fetchTasks();
            fetchCoins();
            fetchEggCodeBlocks();
        }
    }

    /**
     * 获取蛋码块数量
     * 从服务器获取用户的蛋码块数量并更新 UI
     */
    private void fetchEggCodeBlocks() {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(TaskActivity.this, "请先登录", Toast.LENGTH_SHORT).show());
            return;
        }

        // 先显示缓存数据
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int cachedBlocks = preferences.getInt("cached_egg_code_blocks", 0);
        runOnUiThread(() -> textViewEggCodeBlocks.setText(String.valueOf(cachedBlocks)));

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/remains")
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(TaskActivity.this, "获取蛋码块数量失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    fetchCoins();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
                    if (response.isSuccessful()) {
                        String responseBody = body.string();
                        try {
                            Gson gson = new Gson();
                            EggCodeBlocksResponse blocksResponse = gson.fromJson(responseBody, EggCodeBlocksResponse.class);
                            int message = blocksResponse.getCount();
                            runOnUiThread(() -> {
                                textViewEggCodeBlocks.setText(String.valueOf(message));
                            });
                            preferences.edit().putInt("cached_egg_code_blocks", message).apply();
                        } catch (JsonSyntaxException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(TaskActivity.this, "解析蛋码块数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                fetchCoins();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(TaskActivity.this, "获取蛋码块数量失败: " + response.message(), Toast.LENGTH_SHORT).show();
                            fetchCoins();
                        });
                    }
                }
            }
        });
    }



    /**
     * 获取蛋码碎片数量
     * 从服务器获取用户的蛋码碎片数量并更新 UI
     */
    private void fetchCoins() {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            textViewEggCodeFragments.setText("0");
            return;
        }

        // 先显示缓存数据
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int cachedFragments = preferences.getInt("cached_egg_code_fragments", 0);
        runOnUiThread(() -> textViewEggCodeFragments.setText(String.valueOf(cachedFragments)));

        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/coins")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(TaskActivity.this, "获取蛋码碎片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    textViewEggCodeFragments.setText("0");
                    updateSliderRange();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
                    if (response.isSuccessful()) {
                        String responseBody = body.string();
                        try {
                            Gson gson = new Gson();
                            CoinsResponse coinsResponse = gson.fromJson(responseBody, CoinsResponse.class);
                            int coins = coinsResponse.getCoins();
                            runOnUiThread(() -> {
                                textViewEggCodeFragments.setText(String.valueOf(coins));
                                // 更新滑块范围以匹配最新的碎片数量
                                updateSliderRange();
                                if (btnExchange != null) {
                                    // 这里的逻辑在 updateSliderRange 中也会处理，但保留作为双重保障
                                    btnExchange.setEnabled(coins >= 50);
                                }
                            });
                            preferences.edit().putInt("cached_egg_code_fragments", coins).apply();
                        } catch (JsonSyntaxException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(TaskActivity.this, "解析蛋码碎片数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                textViewEggCodeFragments.setText("0");
                            });
                        }
                    }
                }
            }
        });
    }

    /**
     * 内部兑换方法，用于兑换操作
     * @param accessToken 访问令牌
     */
    private void exchangeCoinsInternal(String accessToken) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/coins/tolimit")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    completedExchangeRequests++;
                    failedExchangeRequests++;
                    checkAllExchangesCompleted();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    runOnUiThread(() -> {
                        completedExchangeRequests++;
                        if (response.isSuccessful()) {
                            successfulExchangeRequests++;
                        } else {
                            failedExchangeRequests++;
                        }
                        checkAllExchangesCompleted();
                    });
                }
            }
        });
    }

    /**
     * 兑换蛋码块
     * 向服务器发送兑换请求
     */
    private void exchangeCoins() {
        String accessToken = SecureStorageManager.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        int n = (int) exchangeSlider.getValue();
        if (n <= 0) {
            Toast.makeText(this, "请选择要兑换的数量", Toast.LENGTH_SHORT).show();
            return;
        }

        totalExchangeRequests = n;
        completedExchangeRequests = 0;
        successfulExchangeRequests = 0;
        failedExchangeRequests = 0;

        for (int i = 0; i < n; i++) {
            exchangeCoinsInternal(accessToken);
        }
    }

    private void checkAllExchangesCompleted() {
        if (completedExchangeRequests == totalExchangeRequests) {
            String message;
            if (successfulExchangeRequests == totalExchangeRequests) {
                message = "兑换成功";
            } else if (failedExchangeRequests == totalExchangeRequests) {
                message = "兑换失败";
            } else {
                message = "兑换完成：成功 " + successfulExchangeRequests + " 次，失败 " + failedExchangeRequests + " 次";
            }
            Toast.makeText(TaskActivity.this, message, Toast.LENGTH_LONG).show();
            fetchCoins(); // 刷新蛋码碎片数量
            fetchEggCodeBlocks(); // 刷新蛋码块数量
        }
    }
}