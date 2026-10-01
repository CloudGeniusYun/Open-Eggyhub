package com.eggyhub.android;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class GuideDialog extends Dialog {

    private LinearLayout containerCategories;
    private View dragHandle;
    private float lastX, lastY;

    public GuideDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_guide_custom);

        // 设置对话框背景透明，防止白色背景框冲突
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // 允许对话框在外部移动
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        }

        containerCategories = findViewById(R.id.container_categories);
        dragHandle = findViewById(R.id.drag_handle);
        findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        setupDragLogic();
        setupCategories();
    }

    private void setupDragLogic() {
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - lastX;
                    float deltaY = event.getRawY() - lastY;
                    
                    Window window = getWindow();
                    if (window != null) {
                        WindowManager.LayoutParams params = window.getAttributes();
                        params.x += (int) deltaX;
                        params.y += (int) deltaY;
                        window.setAttributes(params);
                    }
                    
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    break;
            }
            return true;
        });
    }

    private void setupCategories() {
        MainActivity activity = getMainActivity();
        // 1. 分享码
        addCategory("1. 分享码", new String[]{"如何获取蛋码碎片", "如何补充分享码"}, (index) -> {
            TutorialManager manager = TutorialManager.getInstance(getContext());
            if (index == 0) {
                manager.startTutorial(TutorialManager.TUTORIAL_TASK);
                if (activity != null) activity.startTaskGuide();
            } else {
                manager.startTutorial(TutorialManager.TUTORIAL_SUPPLEMENT_CODE);
                if (activity != null) activity.startSupplementCodeGuide();
            }
            dismiss();
        });

        // 2. 文章
        addCategory("2. 文章", new String[]{"如何发布文章"}, (index) -> {
            TutorialManager manager = TutorialManager.getInstance(getContext());
            if (index == 0) {
                manager.startTutorial(TutorialManager.TUTORIAL_PUBLISH);
                if (activity != null) activity.startPublishGuide();
            }
            dismiss();
        });

        // 3. 视频
        addCategory("3. 视频", new String[]{"如何发布视频(暂未开放)"}, (index) -> {});

        // 4. 文件
        addCategory("4. 文件", new String[]{"如何管理文件(暂未开放)"}, (index) -> {});

        // 5. 软件功能
        addCategory("5. 软件功能", new String[]{"查看个人统计"}, (index) -> {
            TutorialManager manager = TutorialManager.getInstance(getContext());
            if (index == 0) {
                manager.startTutorial(TutorialManager.TUTORIAL_PROFILE);
                if (activity != null) activity.startProfileGuide();
            }
            dismiss();
        });
    }

    private MainActivity getMainActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof MainActivity) {
                return (MainActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private void addCategory(String name, String[] items, OnItemClickListener listener) {
        View categoryView = LayoutInflater.from(getContext()).inflate(R.layout.item_guide_category, containerCategories, false);
        
        TextView tvName = categoryView.findViewById(R.id.category_name);
        ImageView ivArrow = categoryView.findViewById(R.id.category_arrow);
        LinearLayout containerItems = categoryView.findViewById(R.id.container_items);
        View header = categoryView.findViewById(R.id.category_header);

        tvName.setText(name);

        header.setOnClickListener(v -> {
            boolean isExpanded = containerItems.getVisibility() == View.VISIBLE;
            containerItems.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ivArrow.setRotation(isExpanded ? 0 : 180);
        });

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            TextView itemView = new TextView(getContext());
            itemView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            itemView.setText(items[i]);
            itemView.setPadding(48, 32, 32, 32);
            itemView.setTextColor(Color.parseColor("#666666"));
            itemView.setTextSize(14);
            itemView.setClickable(true);
            itemView.setFocusable(true);
            itemView.setBackgroundResource(android.R.drawable.list_selector_background);
            itemView.setOnClickListener(v -> listener.onItemClick(index));
            containerItems.addView(itemView);
        }

        containerCategories.addView(categoryView);
    }

    private int spToPx(float sp) {
        return (int) (sp * getContext().getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }

    interface OnItemClickListener {
        void onItemClick(int index);
    }
}