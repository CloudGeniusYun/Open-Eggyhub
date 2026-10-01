package com.eggyhub.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.eggyhub.android.R;

/**
 * 自定义形状按钮（梯形 / 平行四边形）
 * 使用 FrameLayout 作为容器，确保裁剪、阴影、点击区域全部跟随形状
 */
public class ShapeButton extends FrameLayout {

    public static final int SHAPE_LEFT_TRAPEZOID = 0;
    public static final int SHAPE_RIGHT_TRAPEZOID = 1;
    public static final int SHAPE_PARALLELOGRAM = 2;

    private int shapeType = SHAPE_LEFT_TRAPEZOID;
    private float skewRatio = 0.25f;
    private float cornerRadius = 0f;

    private Path path = new Path();
    private RectF rect = new RectF();
    private AppCompatImageView imageView;

    public ShapeButton(Context context) {
        this(context, null);
    }

    public ShapeButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShapeButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 创建内部的 ImageView
        imageView = new AppCompatImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // 关键：让 ImageView 也跟随父布局裁剪（其实父布局裁剪后它自动跟随，但为了保险）
        imageView.setClipToOutline(true);
        addView(imageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ShapeButton);
            shapeType = ta.getInt(R.styleable.ShapeButton_shapeType, SHAPE_LEFT_TRAPEZOID);
            skewRatio = ta.getFloat(R.styleable.ShapeButton_skewRatio, 0.25f);
            cornerRadius = ta.getDimension(R.styleable.ShapeButton_cornerRadius, 0f);
            Drawable src = ta.getDrawable(R.styleable.ShapeButton_srcCompat);
            if (src != null) {
                imageView.setImageDrawable(src);
            }
            int scaleTypeIndex = ta.getInt(R.styleable.ShapeButton_scaleType, 1);
            setScaleTypeByIndex(scaleTypeIndex);
            ta.recycle();
        }

        // 设置背景透明，避免遮挡
        setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // 开启裁剪
        setClipToOutline(true);
        setClipChildren(true);

        // 设置轮廓提供者
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (!rect.isEmpty()) {
                    outline.setConvexPath(path);
                }
            }
        });

        // 设置阴影
        setElevation(4f);
        // 点击涟漪（使用系统的 selectableItemBackgroundBorderless）
        setForeground(getContext().obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackgroundBorderless})
                .getDrawable(0));
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0, 0, w, h);
        buildPath(w, h);
        // 刷新轮廓
        invalidateOutline();
        // 让 ImageView 也使用相同的轮廓（确保裁剪）
        if (imageView != null) {
            imageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (!rect.isEmpty()) {
                        outline.setConvexPath(path);
                    }
                }
            });
            imageView.invalidateOutline();
        }
    }

    private void buildPath(int w, int h) {
        path.reset();
        float topWidth = w * (1 - skewRatio);
        float r = Math.min(cornerRadius, Math.min(w, h) / 4f);

        // 计算四个顶点
        float x1, y1, x2, y2, x3, y3, x4, y4;
        if (shapeType == SHAPE_LEFT_TRAPEZOID) {
            // 直角在左，上底左对齐
            x1 = 0; y1 = 0;
            x2 = topWidth; y2 = 0;
            x3 = w; y3 = h;
            x4 = 0; y4 = h;
        } else if (shapeType == SHAPE_RIGHT_TRAPEZOID) {
            // 直角在右，上底右对齐
            x1 = w - topWidth; y1 = 0;
            x2 = w; y2 = 0;
            x3 = w; y3 = h;
            x4 = 0; y4 = h;
        } else { // 平行四边形
            float skew = w * skewRatio;
            x1 = skew; y1 = 0;
            x2 = w; y2 = 0;
            x3 = w - skew; y3 = h;
            x4 = 0; y4 = h;
        }

        if (r <= 0) {
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            path.lineTo(x3, y3);
            path.lineTo(x4, y4);
            path.close();
            return;
        }

        // 带圆角的路径（使用 quadTo 平滑过渡）
        float[] pts = new float[]{x1, y1, x2, y2, x3, y3, x4, y4};
        int[] next = new int[]{1, 2, 3, 0};
        int[] prev = new int[]{3, 0, 1, 2};

        float[] edgeX = new float[4];
        float[] edgeY = new float[4];
        float[] edgeLen = new float[4];
        for (int i = 0; i < 4; i++) {
            int j = next[i];
            edgeX[i] = pts[j * 2] - pts[i * 2];
            edgeY[i] = pts[j * 2 + 1] - pts[i * 2 + 1];
            edgeLen[i] = (float) Math.sqrt(edgeX[i] * edgeX[i] + edgeY[i] * edgeY[i]);
            if (edgeLen[i] > 0) {
                edgeX[i] /= edgeLen[i];
                edgeY[i] /= edgeLen[i];
            }
        }

        path.moveTo(pts[0] + r * edgeX[3], pts[1] + r * edgeY[3]);

        for (int i = 0; i < 4; i++) {
            float px = pts[i * 2];
            float py = pts[i * 2 + 1];
            float ex1 = edgeX[prev[i]];
            float ey1 = edgeY[prev[i]];
            float ex2 = edgeX[i];
            float ey2 = edgeY[i];

            float x1_corner = px + r * ex1;
            float y1_corner = py + r * ey1;
            float x2_corner = px + r * ex2;
            float y2_corner = py + r * ey2;

            path.lineTo(x1_corner, y1_corner);
            path.quadTo(px, py, x2_corner, y2_corner);
        }
        path.close();
    }

    private void setScaleTypeByIndex(int index) {
        switch (index) {
            case 0:
                imageView.setScaleType(ImageView.ScaleType.CENTER);
                break;
            case 1:
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case 2:
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case 3:
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            case 4:
                imageView.setScaleType(ImageView.ScaleType.FIT_START);
                break;
            case 5:
                imageView.setScaleType(ImageView.ScaleType.FIT_END);
                break;
            case 6:
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
            case 7:
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                break;
            default:
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
        }
    }

    // 公共方法
    public void setImageResource(int resId) {
        imageView.setImageResource(resId);
    }

    public void setImageDrawable(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    public void setImageURI(android.net.Uri uri) {
        imageView.setImageURI(uri);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setShapeType(int shapeType) {
        this.shapeType = shapeType;
        requestLayout();
    }

    public void setSkewRatio(float ratio) {
        this.skewRatio = Math.max(0, Math.min(0.5f, ratio));
        requestLayout();
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        requestLayout();
    }
}