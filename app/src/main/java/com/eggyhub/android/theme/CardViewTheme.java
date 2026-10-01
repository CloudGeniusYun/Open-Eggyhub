package com.eggyhub.android.theme;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CardView样式配置
 */
public class CardViewTheme implements Serializable {
    private List<Sticker> stickers = new ArrayList<>();
    private float cornerRadius = 8f;
    private boolean enabled = false;
    private int maxStickers = 0; // 0表示不限制

    // 边框设置
    private int borderColor = 0x00000000; // 边框颜色(默认透明,无边框)
    private float borderWidth = 0f; // 边框宽度(dp)

    // 预览尺寸(用于坐标转换)
    private float previewWidth = 0f;
    private float previewHeight = 0f;

    public List<Sticker> getStickers() {
        return stickers;
    }

    public void setStickers(List<Sticker> stickers) {
        this.stickers = stickers != null ? stickers : new ArrayList<>();
        updateLayerIndices();
    }

    public void addSticker(Sticker sticker) {
        sticker.setLayerIndex(stickers.size());
        this.stickers.add(sticker);
    }

    public void removeSticker(String stickerId) {
        for (int i = 0; i < stickers.size(); i++) {
            if (stickers.get(i).getId().equals(stickerId)) {
                stickers.remove(i);
                updateLayerIndices();
                break;
            }
        }
    }

    public Sticker getStickerById(String stickerId) {
        for (Sticker sticker : stickers) {
            if (sticker.getId().equals(stickerId)) {
                return sticker;
            }
        }
        return null;
    }

    /**
     * 移动贴纸图层向上
     * @param stickerId 贴纸ID
     */
    public void moveLayerUp(String stickerId) {
        Sticker sticker = getStickerById(stickerId);
        if (sticker != null) {
            int currentIndex = sticker.getLayerIndex();
            if (currentIndex < stickers.size() - 1) {
                // 交换位置
                for (Sticker s : stickers) {
                    if (s.getLayerIndex() == currentIndex + 1) {
                        s.setLayerIndex(currentIndex);
                        break;
                    }
                }
                sticker.setLayerIndex(currentIndex + 1);
                sortStickersByLayer();
            }
        }
    }

    /**
     * 移动贴纸图层向下
     * @param stickerId 贴纸ID
     */
    public void moveLayerDown(String stickerId) {
        Sticker sticker = getStickerById(stickerId);
        if (sticker != null) {
            int currentIndex = sticker.getLayerIndex();
            if (currentIndex > 0) {
                // 交换位置
                for (Sticker s : stickers) {
                    if (s.getLayerIndex() == currentIndex - 1) {
                        s.setLayerIndex(currentIndex);
                        break;
                    }
                }
                sticker.setLayerIndex(currentIndex - 1);
                sortStickersByLayer();
            }
        }
    }

    /**
     * 将贴纸移到最上层
     * @param stickerId 贴纸ID
     */
    public void moveLayerToTop(String stickerId) {
        Sticker sticker = getStickerById(stickerId);
        if (sticker != null && stickers.size() > 1) {
            int maxLayer = stickers.size() - 1;
            int currentLayer = sticker.getLayerIndex();
            
            // 将该贴纸之上的所有贴纸下移
            for (Sticker s : stickers) {
                if (s.getLayerIndex() > currentLayer) {
                    s.setLayerIndex(s.getLayerIndex() - 1);
                }
            }
            sticker.setLayerIndex(maxLayer);
            sortStickersByLayer();
        }
    }

    /**
     * 将贴纸移到最下层
     * @param stickerId 贴纸ID
     */
    public void moveLayerToBottom(String stickerId) {
        Sticker sticker = getStickerById(stickerId);
        if (sticker != null && stickers.size() > 1) {
            int currentLayer = sticker.getLayerIndex();
            
            // 将该贴纸之下的所有贴纸上移
            for (Sticker s : stickers) {
                if (s.getLayerIndex() < currentLayer) {
                    s.setLayerIndex(s.getLayerIndex() + 1);
                }
            }
            sticker.setLayerIndex(0);
            sortStickersByLayer();
        }
    }

    /**
     * 按图层顺序排序贴纸
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
     * 更新所有贴纸的图层索引
     */
    private void updateLayerIndices() {
        for (int i = 0; i < stickers.size(); i++) {
            stickers.get(i).setLayerIndex(i);
        }
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxStickers() {
        return maxStickers;
    }

    public void setMaxStickers(int maxStickers) {
        this.maxStickers = maxStickers;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    public float getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(float previewWidth) {
        this.previewWidth = previewWidth;
    }

    public float getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(float previewHeight) {
        this.previewHeight = previewHeight;
    }

    @Override
    public String toString() {
        return "CardViewTheme{" +
                "stickers=" + stickers.size() +
                ", cornerRadius=" + cornerRadius +
                ", enabled=" + enabled +
                ", maxStickers=" + maxStickers +
                ", borderColor=" + Integer.toHexString(borderColor) +
                ", borderWidth=" + borderWidth +
                ", previewSize=" + previewWidth + "x" + previewHeight +
                '}';
    }
}