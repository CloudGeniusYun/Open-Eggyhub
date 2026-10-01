package com.eggyhub.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;


import androidx.cardview.widget.CardView;

public class PublishActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        // Add a click listener to the root view to debug touch events
        findViewById(android.R.id.content).getRootView().setOnClickListener(v -> {
        });

        // 设置发布选项点击事件

        setupPublishOptions();

        
        // 设置底部导航
        setupBottomNavigation();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(PublishActivity.this, MainActivity.class);
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
    }

    private void checkTutorialStatus() {
        TutorialManager manager = TutorialManager.getInstance(this);
        if (manager.isTutorialRunning() && TutorialManager.TUTORIAL_PUBLISH.equals(manager.getCurrentTutorial())) {
            int stepIndex = manager.getStepIndex();
            // 只有当步骤为 1 时（即从首页点击“发布”后）才显示
            if (stepIndex == 1) {
                View cardArticle = findViewById(R.id.cardArticle);
                if (cardArticle != null) {
                    // 使用 post 确保布局完成后再显示引导
                    cardArticle.post(() -> {
                        GuideHelper.show(this, cardArticle, "第二步：选择你想要发布的内容类型（例如：文章）", false, () -> {
                            manager.finishTutorial();
                        });
                    });
                }
            }
        }
    }

    private void setupPublishOptions() {
        // 发布文章
        CardView cardArticle = findViewById(R.id.cardArticle);
        cardArticle.setOnClickListener(v -> {
            Intent intent = new Intent(PublishActivity.this, CreateArticleActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 发布视频
        CardView cardVideo = findViewById(R.id.cardVideo);
        cardVideo.setOnClickListener(v -> {
            Intent intent = new Intent(PublishActivity.this, linkedVideoActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 发布分享码
        CardView cardShareCode = findViewById(R.id.cardShareCode);
        cardShareCode.setOnClickListener(v -> {
            Intent intent = new Intent(PublishActivity.this, CodeupActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        });
        CardView cardFile = findViewById(R.id.cardFile);
        cardFile.setOnClickListener(v -> {
            Intent intent = new Intent(PublishActivity.this, NewFileActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });




    }

    private void setupBottomNavigation() {
        // 首页按钮
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(PublishActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
        
        // 发布按钮（当前页面）
        View navPublish = findViewById(R.id.navPublish);
        if (navPublish != null) {
            navPublish.setSelected(true);
        }
        
        // 任务按钮
        View navTask = findViewById(R.id.navTask);
        if (navTask != null) {
            navTask.setOnClickListener(v -> {
                Intent intent = new Intent(PublishActivity.this, TaskActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
        
        // 我的按钮
        View navMy = findViewById(R.id.navProfile);
        if (navMy != null) {
            navMy.setOnClickListener(v -> {
                Intent intent = new Intent(PublishActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }
}