package com.eggyhub.android.theme;

import android.graphics.RectF;

import java.util.UUID;

/**
 * 贴纸数据模型
 */
public class Sticker {
    private String id;              // 唯一标识
    private String imagePath;       // 图片路径
    private float x;                // X坐标
    private float y;                // Y坐标
    private float width;            // 宽度
    private float height;           // 高度
    private float scale = 1.0f;     // 缩放比例
    private float rotation = 0f;    // 旋转角度（度）
    private float alpha = 1.0f;     // 透明度（0-1）
    private int layerIndex = 0;     // 图层顺序
    private boolean clipToRoundedCorners = true; // 是否参与圆角裁剪
    
    // 位置绑定属性（类似蛋仔派对的界面编辑）
    private boolean bindLeft = false;   // 绑定左侧
    private boolean bindRight = false;  // 绑定右侧
    private boolean bindTop = false;    // 绑定上侧
    private boolean bindBottom = false; // 绑定下侧

    public Sticker() {
        this.id = UUID.randomUUID().toString();
    }

    public Sticker(String imagePath, float x, float y, float width, float height) {
        this.id = UUID.randomUUID().toString();
        this.imagePath = imagePath;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public void setLayerIndex(int layerIndex) {
        this.layerIndex = layerIndex;
    }

    public boolean isClipToRoundedCorners() {
        return clipToRoundedCorners;
    }

    public void setClipToRoundedCorners(boolean clipToRoundedCorners) {
        this.clipToRoundedCorners = clipToRoundedCorners;
    }

    public boolean isBindLeft() {
        return bindLeft;
    }

    public void setBindLeft(boolean bindLeft) {
        this.bindLeft = bindLeft;
    }

    public boolean isBindRight() {
        return bindRight;
    }

    public void setBindRight(boolean bindRight) {
        this.bindRight = bindRight;
    }

    public boolean isBindTop() {
        return bindTop;
    }

    public void setBindTop(boolean bindTop) {
        this.bindTop = bindTop;
    }

    public boolean isBindBottom() {
        return bindBottom;
    }

    public void setBindBottom(boolean bindBottom) {
        this.bindBottom = bindBottom;
    }

    /**
     * 检查是否有任何绑定
     * @return 是否有绑定
     */
    public boolean hasAnyBinding() {
        return bindLeft || bindRight || bindTop || bindBottom;
    }

    /**
     * 检查是否全部绑定
     * @return 是否全部绑定
     */
    public boolean isFullyBound() {
        return bindLeft && bindRight && bindTop && bindBottom;
    }

    /**
     * 获取贴纸的边界矩形
     * @return 边界矩形
     */
    public RectF getBounds() {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        return new RectF(x, y, x + scaledWidth, y + scaledHeight);
    }

    /**
     * 获取贴纸的中心点
     * @return 中心点坐标（float[2]: x, y）
     */
    public float[] getCenter() {
        RectF bounds = getBounds();
        return new float[]{bounds.centerX(), bounds.centerY()};
    }

    /**
     * 检测点击是否在贴纸范围内
     * @param touchX 触摸X坐标（相对于贴纸坐标系）
     * @param touchY 触摸Y坐标（相对于贴纸坐标系）
     * @return 是否在范围内
     */
    public boolean containsPoint(float touchX, float touchY) {
        // 将触摸点反向旋转，转换到贴纸的本地坐标系
        float[] center = getCenter();
        float dx = touchX - center[0];
        float dy = touchY - center[1];
        
        if (rotation != 0) {
            // 反向旋转触摸点
            double radians = Math.toRadians(-rotation);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            
            float rotatedX = center[0] + dx * cos - dy * sin;
            float rotatedY = center[1] + dx * sin + dy * cos;
            
            // 检测是否在未旋转的边界内
            RectF bounds = getBounds();
            return bounds.contains(rotatedX, rotatedY);
        } else {
            // 没有旋转，直接检测
            RectF bounds = getBounds();
            return bounds.contains(touchX, touchY);
        }
    }

    /**
     * 获取四角控件的位置
     * @param cornerType 角类型：0=左上, 1=右上, 2=左下, 3=右下
     * @return 控件位置（float[2]: x, y）
     */
    public float[] getControlPosition(int cornerType) {
        RectF bounds = getBounds();
        float[] center = getCenter();
        
        float cornerX = 0, cornerY = 0;
        switch (cornerType) {
            case 0: // 左上
                cornerX = bounds.left;
                cornerY = bounds.top;
                break;
            case 1: // 右上
                cornerX = bounds.right;
                cornerY = bounds.top;
                break;
            case 2: // 左下
                cornerX = bounds.left;
                cornerY = bounds.bottom;
                break;
            case 3: // 右下
                cornerX = bounds.right;
                cornerY = bounds.bottom;
                break;
        }
        
        // 如果有旋转，需要计算旋转后的位置
        if (rotation != 0) {
            double radians = Math.toRadians(rotation);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            
            // 相对于中心点的偏移
            float dx = cornerX - center[0];
            float dy = cornerY - center[1];
            
            // 旋转后的坐标
            float rotatedX = center[0] + dx * cos - dy * sin;
            float rotatedY = center[1] + dx * sin + dy * cos;
            
            return new float[]{rotatedX, rotatedY};
        }
        
        return new float[]{cornerX, cornerY};
    }

    @Override
    public String toString() {
        return "Sticker{" +
                "id='" + id + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", scale=" + scale +
                ", rotation=" + rotation +
                ", layerIndex=" + layerIndex +
                '}';
    }
}