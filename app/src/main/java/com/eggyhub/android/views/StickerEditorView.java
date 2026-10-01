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
import android.view.MotionEvent;
import android.view.View;

import com.eggyhub.android.theme.Sticker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 贴纸编辑器自定义View
 */
public class StickerEditorView extends View {
    private static final String TAG = "StickerEditorView";
    
    // 贴纸列表
    private List<Sticker> stickers = new ArrayList<>();
    
    // 当前选中的贴纸
    private Sticker selectedSticker = null;
    
    // 位图缓存
    private Map<String, Bitmap> bitmapCache = new HashMap<>();
    
    // 画笔
    private Paint stickerPaint;
    private Paint controlPaint;
    private Paint controlIconPaint;
    private Paint maskPaint;
    private Paint highlightPaint;
    private Paint selectionPaint; // 选中贴纸的描边画笔
    
    // 控件尺寸
    private float controlRadius = 20f;
    private float controlTouchRadius = 40f;
    private float bindingButtonRadius = 16f; // 绑定按钮半径
    
    // 预览区域（CardView区域）
    private RectF previewRect = new RectF();
    private float cornerRadius = 8f;
    
    // 触摸状态
    private enum TouchState {
        IDLE,           // 空闲
        DRAGGING,       // 拖动贴纸
        SCALING,        // 缩放贴纸
        ROTATING        // 旋转贴纸
    }
    private TouchState touchState = TouchState.IDLE;
    
    // 触摸相关
    private float lastTouchX, lastTouchY;
    private float lastAngle;
    
    // 控件类型
    private static final int CONTROL_DELETE = 0;   // 左上角
    private static final int CONTROL_SCALE = 1;    // 右上角
    private static final int CONTROL_COPY = 2;     // 左下角
    private static final int CONTROL_ROTATE = 3;   // 右下角

    // 绑定按钮类型
    private static final int BINDING_LEFT = 0;     // 左边中点
    private static final int BINDING_TOP = 1;      // 上边中点
    private static final int BINDING_RIGHT = 2;    // 右边中点
    private static final int BINDING_BOTTOM = 3;   // 下边中点

    // 回调接口
    public interface OnStickerInteractionListener {
        void onStickerDeleted(Sticker sticker);
        void onStickerCopied(Sticker sticker);
        void onStickerSelected(Sticker sticker);
        void onStickerDeselected();
        void onBindingChanged(Sticker sticker, int bindingType, boolean enabled);
    }
    private OnStickerInteractionListener interactionListener;
    
    public StickerEditorView(Context context) {
        super(context);
        init();
    }
    
    public StickerEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public StickerEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化画笔
        stickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        
        controlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        controlPaint.setColor(Color.WHITE);
        controlPaint.setStyle(Paint.Style.FILL);
        
        controlIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        controlIconPaint.setColor(Color.BLACK);
        controlIconPaint.setTextSize(24f);
        controlIconPaint.setTextAlign(Paint.Align.CENTER);
        
        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.parseColor("#80000000"));
        maskPaint.setStyle(Paint.Style.FILL);
        
        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setStyle(Paint.Style.FILL);
        
        selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint.setColor(Color.WHITE);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(6f); // 加大描边，从3f改为6f
        selectionPaint.setStrokeCap(Paint.Cap.ROUND);
        selectionPaint.setStrokeJoin(Paint.Join.ROUND);
        
        setWillNotDraw(false);
    }
    
    /**
     * 设置贴纸列表
     */
    public void setStickers(List<Sticker> stickers) {
        this.stickers.clear();
        if (stickers != null) {
            this.stickers.addAll(stickers);
            // 按图层排序
            sortStickersByLayer();
            // 预加载位图
            for (Sticker sticker : this.stickers) {
                loadStickerBitmap(sticker);
            }
        }
        selectedSticker = null;
        invalidate();
    }
    
    /**
     * 添加贴纸
     */
    public void addSticker(Sticker sticker) {
        this.stickers.add(sticker);
        loadStickerBitmap(sticker);
        sortStickersByLayer();
        invalidate();
    }
    
    /**
     * 移除贴纸
     */
    public void removeSticker(String stickerId) {
        for (int i = 0; i < stickers.size(); i++) {
            if (stickers.get(i).getId().equals(stickerId)) {
                stickers.remove(i);
                break;
            }
        }
        if (selectedSticker != null && selectedSticker.getId().equals(stickerId)) {
            selectedSticker = null;
        }
        invalidate();
    }
    
    /**
     * 获取所有贴纸
     */
    public List<Sticker> getStickers() {
        return new ArrayList<>(stickers);
    }
    
    /**
     * 设置预览区域
     */
    public void setPreviewRect(float left, float top, float right, float bottom, float cornerRadius) {
        this.previewRect.set(left, top, right, bottom);
        this.cornerRadius = cornerRadius;
        invalidate();
    }
    
    /**
     * 设置交互监听器
     */
    public void setOnStickerInteractionListener(OnStickerInteractionListener listener) {
        this.interactionListener = listener;
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
    
    /**
     * 加载贴纸位图
     */
    private void loadStickerBitmap(Sticker sticker) {
        if (sticker.getImagePath() != null && !bitmapCache.containsKey(sticker.getId())) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(sticker.getImagePath());
                if (bitmap != null) {
                    bitmapCache.put(sticker.getId(), bitmap);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load bitmap: " + sticker.getImagePath(), e);
            }
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 1. 绘制深色背景
        canvas.drawColor(Color.parseColor("#1A1A1A"));
        
        // 2. 绘制预览区域高亮（白色背景）
        if (!previewRect.isEmpty()) {
            Path clipPath = new Path();
            clipPath.addRoundRect(previewRect, cornerRadius, cornerRadius, Path.Direction.CW);
            canvas.drawPath(clipPath, highlightPaint);
        }
        
        // 3. 绘制所有贴纸（相对于预览区域）
        canvas.save();
        if (!previewRect.isEmpty()) {
            // 将画布移动到预览区域左上角
            canvas.translate(previewRect.left, previewRect.top);
        }
        for (Sticker sticker : stickers) {
            drawSticker(canvas, sticker);
        }
        canvas.restore();
        
        // 4. 绘制选中贴纸的控件（相对于预览区域）
        if (selectedSticker != null) {
            canvas.save();
            if (!previewRect.isEmpty()) {
                canvas.translate(previewRect.left, previewRect.top);
            }
            drawControls(canvas, selectedSticker);
            canvas.restore();
        }
        
        // 5. 绘制遮罩层（仅在预览区外）
        drawMask(canvas);
    }
    
    /**
     * 绘制单个贴纸
     */
    private void drawSticker(Canvas canvas, Sticker sticker) {
        Bitmap bitmap = bitmapCache.get(sticker.getId());
        if (bitmap == null) {
            return;
        }

        canvas.save();

        // 应用变换
        float[] center = sticker.getCenter();
        canvas.translate(sticker.getX(), sticker.getY());
        canvas.scale(sticker.getScale(), sticker.getScale());
        canvas.rotate(sticker.getRotation(),
            sticker.getWidth() / 2f, sticker.getHeight() / 2f);

        // 设置透明度
        stickerPaint.setAlpha((int)(sticker.getAlpha() * 255));

        // 绘制位图
        canvas.drawBitmap(bitmap, 0, 0, stickerPaint);

        canvas.restore();

        // 如果是选中的贴纸，绘制白色描边和绑定按钮（在全局坐标系下）
        if (sticker == selectedSticker) {
            // 绘制高亮边框（固定粗细）
            canvas.save();
            RectF bounds = sticker.getBounds();
            Paint fixedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fixedStrokePaint.setColor(Color.WHITE);
            fixedStrokePaint.setStyle(Paint.Style.STROKE);
            fixedStrokePaint.setStrokeWidth(6f); // 固定6像素粗细
            fixedStrokePaint.setStrokeCap(Paint.Cap.ROUND);
            fixedStrokePaint.setStrokeJoin(Paint.Join.ROUND);

            if (sticker.getRotation() != 0) {
                // 如果有旋转，需要应用旋转矩阵
                canvas.rotate(sticker.getRotation(), bounds.centerX(), bounds.centerY());
            }
            canvas.drawRect(bounds, fixedStrokePaint);
            canvas.restore();

            // 绘制绑定按钮（在边框中点）
            drawBindingButtons(canvas, sticker);
        }
    }
    
    /**
     * 绘制四角控件
     */
    private void drawControls(Canvas canvas, Sticker sticker) {
        float[][] positions = new float[4][2];
        for (int i = 0; i < 4; i++) {
            positions[i] = sticker.getControlPosition(i);
        }

        String[] icons = {"×", "⟡", "+1", "⟳"};
        int[] colors = {Color.parseColor("#FF4444"), Color.WHITE, Color.parseColor("#44AAFF"), Color.WHITE};

        for (int i = 0; i < 4; i++) {
            float x = positions[i][0];
            float y = positions[i][1];

            // 绘制控件背景
            controlPaint.setColor(colors[i]);
            canvas.drawCircle(x, y, controlRadius, controlPaint);

            // 绘制图标
            controlIconPaint.setColor(Color.BLACK);
            controlIconPaint.setTextSize(20f);
            canvas.drawText(icons[i], x, y + 7, controlIconPaint);
        }
    }

    /**
     * 绘制绑定按钮（在边框各边中点）
     */
    private void drawBindingButtons(Canvas canvas, Sticker sticker) {
        RectF bounds = sticker.getBounds();

        // 计算四边中点位置（考虑旋转）
        float[][] positions = new float[4][2];
        positions[BINDING_LEFT] = new float[]{bounds.left, bounds.centerY()};
        positions[BINDING_TOP] = new float[]{bounds.centerX(), bounds.top};
        positions[BINDING_RIGHT] = new float[]{bounds.right, bounds.centerY()};
        positions[BINDING_BOTTOM] = new float[]{bounds.centerX(), bounds.bottom};

        // 如果有旋转，需要计算旋转后的位置
        if (sticker.getRotation() != 0) {
            float[] center = sticker.getCenter();
            double radians = Math.toRadians(sticker.getRotation());
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);

            for (int i = 0; i < 4; i++) {
                float dx = positions[i][0] - center[0];
                float dy = positions[i][1] - center[1];
                positions[i][0] = center[0] + dx * cos - dy * sin;
                positions[i][1] = center[1] + dx * sin + dy * cos;
            }
        }

        // 绘制四个绑定按钮
        boolean[] bindings = {
            sticker.isBindLeft(),
            sticker.isBindTop(),
            sticker.isBindRight(),
            sticker.isBindBottom()
        };

        for (int i = 0; i < 4; i++) {
            float x = positions[i][0];
            float y = positions[i][1];

            // 绘制按钮背景（选中为黄色，未选中为白色）
            Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            buttonPaint.setColor(bindings[i] ? Color.parseColor("#FFB800") : Color.WHITE);
            buttonPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, bindingButtonRadius, buttonPaint);

            // 绘制箭头图标（使用简单的线条表示方向）
            Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(bindings[i] ? Color.WHITE : Color.parseColor("#FFB800"));
            arrowPaint.setStyle(Paint.Style.STROKE);
            arrowPaint.setStrokeWidth(3f);
            arrowPaint.setStrokeCap(Paint.Cap.ROUND);

            float arrowSize = 8f;
            switch (i) {
                case BINDING_LEFT: // 向左的箭头
                    canvas.drawLine(x + arrowSize, y - arrowSize/2, x - arrowSize, y, arrowPaint);
                    canvas.drawLine(x - arrowSize, y, x + arrowSize, y + arrowSize/2, arrowPaint);
                    break;
                case BINDING_TOP: // 向上的箭头
                    canvas.drawLine(x - arrowSize/2, y + arrowSize, x, y - arrowSize, arrowPaint);
                    canvas.drawLine(x, y - arrowSize, x + arrowSize/2, y + arrowSize, arrowPaint);
                    break;
                case BINDING_RIGHT: // 向右的箭头
                    canvas.drawLine(x - arrowSize, y - arrowSize/2, x + arrowSize, y, arrowPaint);
                    canvas.drawLine(x + arrowSize, y, x - arrowSize, y + arrowSize/2, arrowPaint);
                    break;
                case BINDING_BOTTOM: // 向下的箭头
                    canvas.drawLine(x - arrowSize/2, y - arrowSize, x, y + arrowSize, arrowPaint);
                    canvas.drawLine(x, y + arrowSize, x + arrowSize/2, y - arrowSize, arrowPaint);
                    break;
            }
        }
    }
    
    /**
     * 绘制遮罩层
     */
    private void drawMask(Canvas canvas) {
        if (!previewRect.isEmpty()) {
            // 在预览区域外绘制半透明遮罩
            Path maskPath = new Path();
            maskPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
            
            Path previewPath = new Path();
            previewPath.addRoundRect(previewRect, cornerRadius, cornerRadius, Path.Direction.CW);
            
            maskPath.op(previewPath, Path.Op.DIFFERENCE);
            canvas.drawPath(maskPath, maskPaint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        
        // 转换为相对于预览区域的坐标
        float relativeX = touchX - (previewRect.isEmpty() ? 0 : previewRect.left);
        float relativeY = touchY - (previewRect.isEmpty() ? 0 : previewRect.top);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = relativeX;
                lastTouchY = relativeY;
                return handleDown(relativeX, relativeY);
                
            case MotionEvent.ACTION_MOVE:
                if (touchState != TouchState.IDLE) {
                    boolean handled = handleMove(relativeX, relativeY);
                    lastTouchX = relativeX;
                    lastTouchY = relativeY;
                    return handled;
                }
                return false;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleUp();
        }
        
        return super.onTouchEvent(event);
    }
    
    private boolean handleDown(float touchX, float touchY) {
        lastTouchX = touchX;
        lastTouchY = touchY;

        // 1. 检查是否点击了控件（仅在已有选中贴纸时）
        if (selectedSticker != null) {
            for (int i = 0; i < 4; i++) {
                float[] pos = selectedSticker.getControlPosition(i);
                float dx = touchX - pos[0];
                float dy = touchY - pos[1];
                if (Math.sqrt(dx * dx + dy * dy) <= controlTouchRadius) {
                    // 点击了控件，设置对应的状态
                    if (i == CONTROL_SCALE) {
                        touchState = TouchState.SCALING;
                    } else if (i == CONTROL_ROTATE) {
                        touchState = TouchState.ROTATING;
                    }
                    handleControlClick(i);
                    return true;
                }
            }

            // 1.5. 检查是否点击了绑定按钮
            int bindingClicked = checkBindingButtonClick(touchX, touchY);
            if (bindingClicked >= 0) {
                handleBindingButtonClick(bindingClicked);
                return true;
            }
        }

        // 2. 检查是否点击了贴纸（从上层往下查找）
        Sticker clickedSticker = getStickerAtPoint(touchX, touchY);
        if (clickedSticker != null) {
            // 点击了贴纸，选中它
            if (selectedSticker != clickedSticker) {
                selectedSticker = clickedSticker;
                if (interactionListener != null) {
                    interactionListener.onStickerSelected(selectedSticker);
                }
            }
            // 进入拖动状态
            touchState = TouchState.DRAGGING;
            invalidate();
            return true;
        } else {
            // 点击空白处，取消选中
            if (selectedSticker != null) {
                selectedSticker = null;
                touchState = TouchState.IDLE;
                if (interactionListener != null) {
                    interactionListener.onStickerDeselected();
                }
                invalidate();
            }
        }

        return true;
    }
    
    private boolean handleMove(float touchX, float touchY) {
        if (selectedSticker == null) {
            return false;
        }
        
        float dx = touchX - lastTouchX;
        float dy = touchY - lastTouchY;
        
        switch (touchState) {
            case DRAGGING:
                // 移动贴纸
                selectedSticker.setX(selectedSticker.getX() + dx);
                selectedSticker.setY(selectedSticker.getY() + dy);
                break;
                
            case SCALING:
                // 缩放贴纸
                float[] center = selectedSticker.getCenter();
                float lastDist = distance(lastTouchX, lastTouchY, center[0], center[1]);
                float currentDist = distance(touchX, touchY, center[0], center[1]);
                if (lastDist > 0) {
                    float scale = selectedSticker.getScale() * (currentDist / lastDist);
                    selectedSticker.setScale(scale);
                }
                break;
                
            case ROTATING:
                // 旋转贴纸
                float[] center2 = selectedSticker.getCenter();
                float lastAngle = angle(lastTouchX, lastTouchY, center2[0], center2[1]);
                float currentAngle = angle(touchX, touchY, center2[0], center2[1]);
                selectedSticker.setRotation(selectedSticker.getRotation() + (currentAngle - lastAngle));
                break;
        }
        
        lastTouchX = touchX;
        lastTouchY = touchY;
        invalidate();
        return true;
    }
    
    private boolean handleUp() {
        touchState = TouchState.IDLE;
        // 不要取消选中状态，保持选中贴纸的显示
        return true;
    }
    
    /**
     * 处理控件点击
     */
    private void handleControlClick(int controlType) {
        if (selectedSticker == null) {
            return;
        }

        switch (controlType) {
            case CONTROL_DELETE:
                // 删除
                if (interactionListener != null) {
                    interactionListener.onStickerDeleted(selectedSticker);
                }
                break;

            case CONTROL_COPY:
                // 复制
                if (interactionListener != null) {
                    interactionListener.onStickerCopied(selectedSticker);
                }
                break;
        }

        // 缩放和旋转在MOVE中处理
        if (controlType == CONTROL_SCALE) {
            touchState = TouchState.SCALING;
        } else if (controlType == CONTROL_ROTATE) {
            touchState = TouchState.ROTATING;
        }
    }

    /**
     * 检查是否点击了绑定按钮
     * @return 点击的绑定按钮类型，-1表示未点击
     */
    private int checkBindingButtonClick(float touchX, float touchY) {
        if (selectedSticker == null) {
            return -1;
        }

        RectF bounds = selectedSticker.getBounds();

        // 计算四边中点位置（考虑旋转）
        float[][] positions = new float[4][2];
        positions[BINDING_LEFT] = new float[]{bounds.left, bounds.centerY()};
        positions[BINDING_TOP] = new float[]{bounds.centerX(), bounds.top};
        positions[BINDING_RIGHT] = new float[]{bounds.right, bounds.centerY()};
        positions[BINDING_BOTTOM] = new float[]{bounds.centerX(), bounds.bottom};

        // 如果有旋转，需要计算旋转后的位置
        if (selectedSticker.getRotation() != 0) {
            float[] center = selectedSticker.getCenter();
            double radians = Math.toRadians(selectedSticker.getRotation());
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);

            for (int i = 0; i < 4; i++) {
                float dx = positions[i][0] - center[0];
                float dy = positions[i][1] - center[1];
                positions[i][0] = center[0] + dx * cos - dy * sin;
                positions[i][1] = center[1] + dx * sin + dy * cos;
            }
        }

        // 检查是否点击了某个绑定按钮
        for (int i = 0; i < 4; i++) {
            float dx = touchX - positions[i][0];
            float dy = touchY - positions[i][1];
            if (Math.sqrt(dx * dx + dy * dy) <= bindingButtonRadius * 1.5f) { // 增大触摸区域
                return i;
            }
        }

        return -1;
    }

    /**
     * 处理绑定按钮点击
     */
    private void handleBindingButtonClick(int bindingType) {
        if (selectedSticker == null) {
            return;
        }

        boolean[] bindings = {
            selectedSticker.isBindLeft(),
            selectedSticker.isBindTop(),
            selectedSticker.isBindRight(),
            selectedSticker.isBindBottom()
        };

        // 切换绑定状态
        boolean newState = !bindings[bindingType];

        if (interactionListener != null) {
            interactionListener.onBindingChanged(selectedSticker, bindingType, newState);
        }

        invalidate();
    }
    
    /**
     * 获取指定位置的贴纸（从上层往下查找）
     */
    private Sticker getStickerAtPoint(float x, float y) {
        for (int i = stickers.size() - 1; i >= 0; i--) {
            Sticker sticker = stickers.get(i);
            if (sticker.containsPoint(x, y)) {
                return sticker;
            }
        }
        return null;
    }
    
    /**
     * 计算两点距离
     */
    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 计算角度
     */
    private float angle(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 清理位图缓存
        for (Bitmap bitmap : bitmapCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmapCache.clear();
    }
}