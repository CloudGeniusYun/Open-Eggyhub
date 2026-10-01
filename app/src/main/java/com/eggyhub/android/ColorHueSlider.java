package com.eggyhub.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorHueSlider extends View {

    private Paint huePaint;
    private Paint selectorPaint;
    private Paint whiteSelectorBorderPaint; // Add this line
    private RectF drawRect;

    private float hue = 0;
    private float selectorY;

    private OnHueChangedListener onHueChangedListener;

    public interface OnHueChangedListener {
        void onHueChanged(float hue);
    }

    public void setOnHueChangedListener(OnHueChangedListener listener) {
        this.onHueChangedListener = listener;
    }

    public ColorHueSlider(Context context) {
        super(context);
        init();
    }

    public ColorHueSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorHueSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(2);
        selectorPaint.setColor(Color.BLACK);

        whiteSelectorBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteSelectorBorderPaint.setStyle(Paint.Style.STROKE);
        whiteSelectorBorderPaint.setStrokeWidth(4); // Slightly thicker white border
        whiteSelectorBorderPaint.setColor(Color.WHITE);

        drawRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float selectorHorizontalMarginPx = 5 * getResources().getDisplayMetrics().density; // 5dp on each side
        float selectorVerticalMarginPx = 2 * getResources().getDisplayMetrics().density; // Half of selector height (20dp / 2)
        drawRect.set(selectorHorizontalMarginPx, selectorVerticalMarginPx, w - selectorHorizontalMarginPx, h - selectorVerticalMarginPx);
        updateHueShader();
        updateSelectorPosition();
    }

    private void updateHueShader() {
        int[] colors = new int[7];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.HSVToColor(new float[]{i * 60, 1.0f, 1.0f});
        }
        Shader hueShader = new LinearGradient(0, drawRect.top, 0, drawRect.bottom, colors, null, Shader.TileMode.CLAMP);
        huePaint.setShader(hueShader);
    }

    private void updateSelectorPosition() {
        selectorY = drawRect.top + hue / 360f * drawRect.height();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(drawRect, huePaint);

        // Draw selector as a hollow rectangle
        float selectorHeight = 4; // Height of the selector rectangle
        float extraWidthPx = 10 * getResources().getDisplayMetrics().density; // 10dp extra width
        float selectorWidth = drawRect.width() + extraWidthPx;
        float selectorLeft = drawRect.centerX() - selectorWidth / 2;
        float selectorRight = drawRect.centerX() + selectorWidth / 2;
        float selectorTop = selectorY - selectorHeight / 2;
        float selectorBottom = selectorY + selectorHeight / 2;

        RectF selectorRect = new RectF(selectorLeft, selectorTop, selectorRight, selectorBottom);

        // Draw white border first for visibility on dark backgrounds
        canvas.drawRect(selectorRect, whiteSelectorBorderPaint);
        // Then draw the black selector rectangle
        canvas.drawRect(selectorRect, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true); // Disallow parent to intercept touch events
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                // Map touch coordinates to the new drawRect range
                hue = Math.max(0.0f, Math.min(359.9f, 360f * ((y - drawRect.top) / drawRect.height())));
                updateSelectorPosition();
                if (onHueChangedListener != null) {
                    onHueChangedListener.onHueChanged(hue);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false); // Allow parent to intercept touch events again
                return true;
        }
        return super.onTouchEvent(event);
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = hue;
        updateSelectorPosition();
        if (onHueChangedListener != null) {
            onHueChangedListener.onHueChanged(hue);
        }
    }
}
