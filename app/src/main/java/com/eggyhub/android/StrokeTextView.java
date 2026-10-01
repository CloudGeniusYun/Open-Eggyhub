package com.eggyhub.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.Nullable;

public class StrokeTextView extends TextView {

    private int strokeColor = 0xFF000000; // 黑色描边
    private float strokeWidth = 3f;       // 描边宽度

    public StrokeTextView(Context context) {
        super(context);
        init();
    }

    public StrokeTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StrokeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 什么也不做，或者设置默认
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
        invalidate();
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        requestLayout();   // 重新测量，扩大视图尺寸
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (strokeWidth > 0) {
            // 扩大宽高，给描边留出显示空间
            int extra = (int) Math.ceil(strokeWidth * 2);
            setMeasuredDimension(
                getMeasuredWidth() + extra,
                getMeasuredHeight() + extra
            );
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (strokeWidth > 0) {
            // 先整体向右下平移，让描边完全显示在视图内部
            canvas.save();
            canvas.translate(strokeWidth, strokeWidth);

            Paint paint = getPaint();
            int textColor = getCurrentTextColor();

            // 1. 绘制描边
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            setTextColor(strokeColor);
            super.onDraw(canvas);

            // 2. 绘制填充文本（在描边之上）
            setTextColor(textColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0);
            super.onDraw(canvas);

            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }
}