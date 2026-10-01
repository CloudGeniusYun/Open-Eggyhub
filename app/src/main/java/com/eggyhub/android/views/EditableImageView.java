package com.eggyhub.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * 可编辑的图片视图
 * 支持缩放、移动手势，裁剪到视图边界
 */
public class EditableImageView extends View {

    private Bitmap bitmap;
    private Matrix matrix;
    private Paint paint;
    private Path clipPath;
    private RectF viewRect;

    // 缩放相关
    private ScaleGestureDetector scaleDetector;
    private float minScale = 0.1f;
    private float maxScale = 5.0f;
    private float currentScale = 1.0f;

    // 移动相关
    private GestureDetector gestureDetector;
    private float lastX, lastY;

    // 图片位置和尺寸
    private float bitmapWidth;
    private float bitmapHeight;
    private int viewWidth;
    private int viewHeight;

    // 透明度
    private float alpha = 1.0f;

    public EditableImageView(Context context) {
        super(context);
        init(context);
    }

    public EditableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        matrix = new Matrix();
        paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        clipPath = new Path();
        viewRect = new RectF();

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        setClickable(true);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        if (bitmap != null) {
            bitmapWidth = bitmap.getWidth();
            bitmapHeight = bitmap.getHeight();
            
            // 初始居中并适配视图（保持原比例）
            fitBitmapToView();
        }
        invalidate();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        paint.setAlpha((int)(alpha * 255));
        invalidate();
    }

    /**
     * 适配图片到视图（居中，fitCenter）
     */
    private void fitBitmapToView() {
        if (bitmap == null || viewWidth <= 0 || viewHeight <= 0) return;

        float scaleX = (float) viewWidth / bitmapWidth;
        float scaleY = (float) viewHeight / bitmapHeight;
        float scale = Math.min(scaleX, scaleY);

        matrix.reset();
        matrix.postScale(scale, scale);

        // 居中
        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;
        float dx = (viewWidth - scaledWidth) / 2;
        float dy = (viewHeight - scaledHeight) / 2;
        matrix.postTranslate(dx, dy);

        currentScale = scale;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        viewRect.set(0, 0, w, h);

        if (bitmap != null) {
            fitBitmapToView();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap != null) {
            // 保存画布状态
            canvas.save();

            // 裁剪到视图边界
            clipPath.reset();
            clipPath.addRect(viewRect, Path.Direction.CW);
            canvas.clipPath(clipPath);

            // 绘制图片
            canvas.drawBitmap(bitmap, matrix, paint);

            // 恢复画布状态
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (bitmap == null) return false;

            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            if (newScale >= minScale && newScale <= maxScale) {
                currentScale = newScale;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                matrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                invalidate();
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            lastX = e.getX();
            lastY = e.getY();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (bitmap == null) return false;

            matrix.postTranslate(-distanceX, -distanceY);
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 双击重置
            fitBitmapToView();
            return true;
        }
    }

    /**
     * 获取当前的变换矩阵
     * @return 变换矩阵
     */
    public Matrix getTransformMatrix() {
        return matrix;
    }
}