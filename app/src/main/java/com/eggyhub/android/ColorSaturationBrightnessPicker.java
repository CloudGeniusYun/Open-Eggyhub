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

public class ColorSaturationBrightnessPicker extends View {

    private Paint saturationPaint;
    private Paint brightnessPaint;
    private Paint selectorPaint;
    private RectF drawRect;

    private float hue = 0;
    private float saturation = 1.0f;
    private float brightness = 1.0f;

    private float selectorX;
    private float selectorY;

    private OnColorChangedListener onColorChangedListener;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.onColorChangedListener = listener;
    }

    public ColorSaturationBrightnessPicker(Context context) {
        super(context);
        init();
    }

    public ColorSaturationBrightnessPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorSaturationBrightnessPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brightnessPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(2);
        selectorPaint.setColor(Color.BLACK);

        drawRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Leave a margin for the selector circle (radius 10)
        float margin = 10 * getResources().getDisplayMetrics().density; // Convert 10dp to pixels
        drawRect.set(margin, margin, w - margin, h - margin);
        updatePaints();
        updateSelectorPosition();
    }

    private void updatePaints() {
        // Saturation gradient (left to right, from white to hue color)
        int startColor = Color.WHITE;
        int endColor = Color.HSVToColor(new float[]{hue, 1.0f, 1.0f});
        Shader saturationShader = new LinearGradient(0, 0, drawRect.width(), 0, startColor, endColor, Shader.TileMode.CLAMP);
        saturationPaint.setShader(saturationShader);

        // Brightness gradient (top to bottom, from transparent to black)
        Shader brightnessShader = new LinearGradient(0, 0, 0, drawRect.height(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
        brightnessPaint.setShader(brightnessShader);
    }

    private void updateSelectorPosition() {
        selectorX = drawRect.left + drawRect.width() * saturation;
        selectorY = drawRect.top + drawRect.height() * (1.0f - brightness);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(drawRect, saturationPaint);
        canvas.drawRect(drawRect, brightnessPaint);

        // Draw selector
        // Draw a white border first for visibility on dark backgrounds
        Paint whiteBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteBorderPaint.setStyle(Paint.Style.STROKE);
        whiteBorderPaint.setStrokeWidth(4); // Slightly thicker white border
        whiteBorderPaint.setColor(Color.WHITE);
        canvas.drawCircle(selectorX, selectorY, 10, whiteBorderPaint);

        // Then draw the black selector circle
        canvas.drawCircle(selectorX, selectorY, 10, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true); // Disallow parent to intercept touch events
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                // Map touch coordinates to the new drawRect range
                saturation = Math.max(0.0f, Math.min(1.0f, (x - drawRect.left) / drawRect.width()));
                brightness = Math.max(0.0f, Math.min(1.0f, 1.0f - ((y - drawRect.top) / drawRect.height())));

                updateSelectorPosition();
                if (onColorChangedListener != null) {
                    onColorChangedListener.onColorChanged(getColor());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false); // Allow parent to intercept touch events again
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setHue(float hue) {
        this.hue = hue;
        updatePaints();
        invalidate();
        if (onColorChangedListener != null) {
            onColorChangedListener.onColorChanged(getColor());
        }
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{hue, saturation, brightness});
    }

    public float getSaturation() {
        return saturation;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
        updateSelectorPosition();
        if (onColorChangedListener != null) {
            onColorChangedListener.onColorChanged(getColor());
        }
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
        updateSelectorPosition();
        if (onColorChangedListener != null) {
            onColorChangedListener.onColorChanged(getColor());
        }
    }
}
