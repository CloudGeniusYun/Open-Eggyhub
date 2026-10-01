package com.eggyhub.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;

import com.eggyhub.android.R;

public class StrokeTextView extends AppCompatTextView {

    private Paint strokePaint;
    private Paint fillPaint;
    private int strokeColor = Color.TRANSPARENT;
    private float strokeWidth = 2f;
    private float strokeCompensationFactor = 1f; // ★ 仅用于描边补偿（固定为 1/scale）

    public StrokeTextView(Context context) {
        super(context);
        init(null);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StrokeTextView);
            strokeColor = a.getColor(R.styleable.StrokeTextView_strokeColor, Color.TRANSPARENT);
            strokeWidth = a.getDimension(R.styleable.StrokeTextView_strokeWidth, 2f);
            a.recycle();
        }

        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * ★ 设置描边补偿因子（仅用于描边宽度补偿，固定为 1/scale，不加权）
     */
    public void setStrokeCompensationFactor(float factor) {
        this.strokeCompensationFactor = factor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String text = getText().toString();
        if (text.isEmpty()) {
            return;
        }

        // 同步字体大小和字体
        float textSize = getTextSize();
        strokePaint.setTextSize(textSize);
        fillPaint.setTextSize(textSize);
        strokePaint.setTypeface(getTypeface());
        fillPaint.setTypeface(getTypeface());

        // 计算水平位置（支持 gravity）
        float textWidth = fillPaint.measureText(text);
        int gravity = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
        float x;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int viewWidth = getWidth() - paddingLeft - paddingRight;

        switch (gravity) {
            case Gravity.RIGHT:
            case Gravity.END:
                x = paddingLeft + viewWidth - textWidth;
                break;
            case Gravity.CENTER_HORIZONTAL:
                x = paddingLeft + (viewWidth - textWidth) / 2;
                break;
            default:
                x = paddingLeft;
                break;
        }

        // 计算垂直居中基线
        Paint.FontMetrics fm = fillPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float baseline = (getHeight() - textHeight) / 2 - fm.ascent;

        // ★ 绘制描边（宽度 = 原始宽度 * 补偿因子，补偿因子固定为 1/scale，不加权）
        if (strokeColor != Color.TRANSPARENT && strokeWidth > 0) {
            strokePaint.setColor(strokeColor);
            strokePaint.setStrokeWidth(strokeWidth * strokeCompensationFactor);
            canvas.drawText(text, x, baseline, strokePaint);
        }

        // 绘制填充文字
        fillPaint.setColor(getCurrentTextColor());
        canvas.drawText(text, x, baseline, fillPaint);
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
        invalidate();
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        invalidate();
    }
}