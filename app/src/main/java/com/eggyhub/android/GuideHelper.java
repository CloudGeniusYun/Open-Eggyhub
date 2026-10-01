package com.eggyhub.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class GuideHelper {

    public interface OnGuideClickListener {
        void onTargetClick();
    }

    public static void show(Activity activity, View targetView, String tipText, boolean clickable, OnGuideClickListener listener) {
        dismiss(activity); // 先清除旧的
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        GuideView guideView = new GuideView(activity, targetView, tipText, clickable, listener);
        
        // 关键修复：初始设为全透明
        guideView.setAlpha(0f);
        rootView.addView(guideView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        // 延迟显示，等待位置计算完成
        targetView.post(() -> {
            guideView.animate()
                    .alpha(1f)
                    .setDuration(250) // 稍微拉长一点点，过渡更自然
                    .start();
        });
    }

    public static void dismiss(Activity activity) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;
        
        // 从后往前遍历删除，避免索引偏移问题
        for (int i = rootView.getChildCount() - 1; i >= 0; i--) {
            View child = rootView.getChildAt(i);
            if (child instanceof GuideView) {
                rootView.removeView(child);
            }
        }
    }

    private static class GuideView extends FrameLayout {
        private View targetView;
        private String tipText;
        private boolean clickable;
        private OnGuideClickListener listener;
        private Paint maskPaint;
        private Paint transparentPaint;
        private RectF targetRect;
        private int[] location = new int[2];

        public GuideView(Context context, View targetView, String tipText, boolean clickable, OnGuideClickListener listener) {
            super(context);
            this.targetView = targetView;
            this.tipText = tipText;
            this.clickable = clickable;
            this.listener = listener;

            setWillNotDraw(false);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
            init();
            addTipView();
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                // 点击返回键，强制结束所有教程并消失
                TutorialManager.getInstance(getContext()).finishTutorial();
                dismiss();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        private void init() {
            maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            maskPaint.setColor(Color.parseColor("#99000000"));

            transparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            targetRect = new RectF();
            updateTargetRect();
        }

        private void updateTargetRect() {
            targetView.getLocationOnScreen(location);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;

            // 获取 View 的实际位置
            float left = location[0];
            float top = location[1];
            float right = left + targetView.getWidth();
            float bottom = top + targetView.getHeight();

            // 如果 View 太大（比如占据了屏幕 80% 以上高度），则收缩高亮范围，只高亮顶部一部分，防止提示框被挤出去
            if ((bottom - top) > screenHeight * 0.7f) {
                bottom = top + screenHeight * 0.4f;
            }

            targetRect.set(left, top, right, bottom);
        }

        private void addTipView() {
            CardView cardView = new CardView(getContext());
            cardView.setRadius(16);
            cardView.setCardElevation(8);
            cardView.setCardBackgroundColor(Color.WHITE);

            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 24, 32, 24);

            TextView tvTip = new TextView(getContext());
            tvTip.setText(tipText);
            tvTip.setTextColor(Color.BLACK);
            tvTip.setTextSize(16);
            layout.addView(tvTip);

            if (!clickable) {
                TextView tvNext = new TextView(getContext());
                tvNext.setText("我知道了");
                tvNext.setTextColor(Color.parseColor("#0096FF"));
                tvNext.setPadding(0, 16, 0, 0);
                tvNext.setGravity(Gravity.END);
                tvNext.setOnClickListener(v -> {
                    if (listener != null) listener.onTargetClick();
                    dismiss();
                });
                layout.addView(tvNext);
            }

            cardView.addView(layout);

            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            // 改进的定位逻辑
            if (targetRect.bottom < screenHeight * 0.6f) {
                // 如果高亮区域在屏幕偏上方，提示框放在下方
                lp.topMargin = (int) targetRect.bottom + 60;
                lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            } else if (targetRect.top > screenHeight * 0.4f) {
                // 如果高亮区域在屏幕偏下方，提示框放在上方
                lp.bottomMargin = (int) (screenHeight - targetRect.top) + 60;
                lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            } else {
                // 如果 View 实在太大（如占满全屏），则强行居中显示提示框，不避让了
                lp.gravity = Gravity.CENTER;
            }

            addView(cardView, lp);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            updateTargetRect();
            
            // 使用离屏缓冲实现镂空效果
            int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
            canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);
            canvas.drawRoundRect(targetRect, 20, 20, transparentPaint);
            canvas.restoreToCount(saveCount);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (clickable && targetRect.contains(event.getX(), event.getY())) {
                // 如果允许穿透，我们在按下时就触发回调
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (listener != null) listener.onTargetClick();
                    
                    // 关键修复：如果是穿透点击（通常意味着要跳转页面），我们不立即消失
                    // 而是延迟一小会儿再消失，这样引导层的黑色背景可以覆盖住页面跳转前的间隙，防止“闪亮”
                    postDelayed(this::dismiss, 500);
                }
                return false; // 不拦截，让 DOWN 及后续事件传递给底部的 targetView
            }
            // 拦截非目标区域的点击
            return true;
        }

        private void dismiss() {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) {
                parent.removeView(this);
            }
        }
    }
}
