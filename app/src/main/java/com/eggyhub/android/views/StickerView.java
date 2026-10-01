package com.eggyhub.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.eggyhub.android.theme.Sticker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 用于在CardView中显示贴纸的View
 */
public class StickerView extends View {
    private static final String TAG = "StickerView";

    private List<Sticker> stickers = new ArrayList<>();
    private float cornerRadius = 8f;

    // 预览区域尺寸（用于坐标转换）
    private float previewWidth = 0f;
    private float previewHeight = 0f;

    private Paint bitmapPaint;
    
    public StickerView(Context context) {
        super(context);
        init();
    }
    
    public StickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public StickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        setWillNotDraw(false);
    }
    
    /**
     * 设置贴纸列表
     */
    public void setStickers(List<Sticker> stickers) {
        this.stickers.clear();
        if (stickers != null) {
            this.stickers.addAll(stickers);
            sortStickersByLayer();
        }
        invalidate();
    }
    
    /**
     * 设置圆角半径
     */
    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        invalidate();
    }

    /**
     * 设置预览区域尺寸（用于坐标转换）
     */
    public void setPreviewSize(float width, float height) {
        this.previewWidth = width;
        this.previewHeight = height;
    }
    
    /**
     * 按图层排序
     */
    private void sortStickersByLayer() {
        Collections.sort(stickers, new Comparator<Sticker>() {
            @Override
            public int compare(Sticker s1, Sticker s2) {
                return Integer.compare(s1.getLayerIndex(), s2.getLayerIndex());
            }
        });
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (stickers.isEmpty()) {
            return;
        }
        
        // 应用圆角裁剪
        RectF rect = new RectF(0, 0, getWidth(), getHeight());
        Path clipPath = new Path();
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
        
        // 保存画布状态
        canvas.save();
        
        // 绘制所有贴纸（不裁剪圆角）
        for (Sticker sticker : stickers) {
            drawSticker(canvas, sticker, false);
        }
        
        // 恢复画布状态
        canvas.restore();
        
        // 对需要裁剪的贴纸应用圆角裁剪
        canvas.save();
        canvas.clipPath(clipPath);
        
        for (Sticker sticker : stickers) {
            if (sticker.isClipToRoundedCorners()) {
                drawSticker(canvas, sticker, true);
            }
        }
        
        canvas.restore();
    }
    
    /**
     * 绘制单个贴纸
     */
    private void drawSticker(Canvas canvas, Sticker sticker, boolean applyClip) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(sticker.getImagePath());
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap: " + sticker.getImagePath());
                return;
            }

            // 计算实际位置（基于绑定状态）
            float[] actualPosition = calculateActualPosition(sticker);
            float actualX = actualPosition[0];
            float actualY = actualPosition[1];

            canvas.save();

            // 应用变换
            canvas.translate(actualX, actualY);
            canvas.scale(sticker.getScale(), sticker.getScale());
            canvas.rotate(sticker.getRotation(),
                sticker.getWidth() / 2f, sticker.getHeight() / 2f);

            // 设置透明度
            bitmapPaint.setAlpha((int)(sticker.getAlpha() * 255));

            // 绘制位图
            canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);

            canvas.restore();

            // 回收位图
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Failed to draw sticker: " + sticker.getId(), e);
        }
    }

    /**
     * 根据绑定状态计算贴纸的实际位置
     * @return float[2]: {actualX, actualY}
     */
    private float[] calculateActualPosition(Sticker sticker) {
        // 如果没有设置预览尺寸,直接使用原始坐标
        if (previewWidth <= 0 || previewHeight <= 0) {
            return new float[]{sticker.getX(), sticker.getY()};
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // 计算缩放比例
        float scaleX = viewWidth / previewWidth;
        float scaleY = viewHeight / previewHeight;

        // 默认按左上角比例计算
        float actualX = sticker.getX() * scaleX;
        float actualY = sticker.getY() * scaleY;

        // 如果有绑定,使用绑定逻辑
        if (sticker.hasAnyBinding()) {
            if (sticker.isFullyBound()) {
                // 全绑定:按比例缩放整个图片
                // 不需要调整,保持相对位置
            } else {
                // 部分绑定:根据绑定方向计算
                RectF bounds = sticker.getBounds();

                if (sticker.isBindLeft()) {
                    // 绑定左侧:保持左边距离的比例
                    actualX = sticker.getX() * scaleX;
                }

                if (sticker.isBindRight()) {
                    // 绑定右侧:保持右边距离的比例
                    float rightGap = previewWidth - (sticker.getX() + bounds.width());
                    actualX = viewWidth - rightGap * scaleX - bounds.width();
                }

                if (sticker.isBindTop()) {
                    // 绑定上侧:保持上边距离的比例
                    actualY = sticker.getY() * scaleY;
                }

                if (sticker.isBindBottom()) {
                    // 绑定下侧:保持下边距离的比例
                    float bottomGap = previewHeight - (sticker.getY() + bounds.height());
                    actualY = viewHeight - bottomGap * scaleY - bounds.height();
                }
            }
        }

        Log.d(TAG, "Sticker " + sticker.getId() + " position: "
            + sticker.getX() + "," + sticker.getY() + " -> "
            + actualX + "," + actualY
            + " (bind: L" + sticker.isBindLeft() + " R" + sticker.isBindRight()
            + " T" + sticker.isBindTop() + " B" + sticker.isBindBottom() + ")");

        return new float[]{actualX, actualY};
    }
}