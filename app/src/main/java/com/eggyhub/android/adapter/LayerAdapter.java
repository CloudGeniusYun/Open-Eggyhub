package com.eggyhub.android.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eggyhub.android.R;
import com.eggyhub.android.theme.Sticker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图层列表Adapter
 */
public class LayerAdapter extends RecyclerView.Adapter<LayerAdapter.LayerViewHolder> {

    private List<Sticker> stickers;
    private OnLayerActionListener listener;
    private int selectedPosition = -1;

    public interface OnLayerActionListener {
        void onLayerSelected(int position, Sticker sticker);
        void onMoveToTop(int position);
        void onMoveToBottom(int position);
        void onClipToRoundedCornersChanged(int stickerIndex, boolean clipToRoundedCorners);
    }

    public LayerAdapter(List<Sticker> stickers, OnLayerActionListener listener) {
        this.stickers = stickers != null ? stickers : new ArrayList<>();
        this.listener = listener;
    }

    /**
     * 将列表position转换为实际贴纸索引
     * 列表显示：顶层在上（position 0），底层在下（position n-1）
     * 实际数组：底层在前（index 0），顶层在后（index n-1）
     */
    private int listPositionToStickerIndex(int listPosition) {
        return stickers.size() - 1 - listPosition;
    }

    /**
     * 将实际贴纸索引转换为列表position
     */
    private int stickerIndexToListPosition(int stickerIndex) {
        return stickers.size() - 1 - stickerIndex;
    }

    @NonNull
    @Override
    public LayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_layer, parent, false);
        return new LayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LayerViewHolder holder, int position) {
        int stickerIndex = listPositionToStickerIndex(position);
        Sticker sticker = stickers.get(stickerIndex);

        // 设置预览图
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(sticker.getImagePath());
            if (bitmap != null) {
                holder.ivPreview.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            holder.ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 设置图层名称（顶层在最上面）
        holder.tvLayerName.setText("图层 " + (position + 1) + (position == 0 ? " (顶层)" : position == stickers.size() - 1 ? " (底层)" : ""));

        // 设置图层信息
        String info = String.format("缩放: %.0f%% | 旋转: %.0f°", 
            sticker.getScale() * 100, 
            sticker.getRotation());
        holder.tvLayerInfo.setText(info);

        // 设置选中状态背景
        holder.itemView.setBackgroundColor(position == selectedPosition ? 
            0x330000FF : android.graphics.Color.TRANSPARENT);

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLayerSelected(stickerIndex, sticker);
            }
            setSelectedPosition(position);
        });

        // 操作按钮
        holder.btnMoveToTop.setOnClickListener(v -> {
            if (listener != null) {
                // 置顶：将图层移到最上层（列表position = 0）
                listener.onMoveToTop(stickerIndex);
            }
        });

        holder.btnMoveToBottom.setOnClickListener(v -> {
            if (listener != null) {
                // 置底：将图层移到最下层（列表position = size-1）
                listener.onMoveToBottom(stickerIndex);
            }
        });

        // 拖动手柄（用于拖动排序）
        holder.ivDragHandle.setVisibility(View.VISIBLE);
        // 注意：实际拖动由ItemTouchHelper处理，这里只是视觉提示

        // 圆角裁剪设置按钮
        updateClipButtonState(holder.btnClipToRoundedCorners, sticker.isClipToRoundedCorners());
        holder.btnClipToRoundedCorners.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClipToRoundedCornersChanged(stickerIndex, !sticker.isClipToRoundedCorners());
            }
        });
    }

    /**
     * 更新裁剪按钮的视觉状态
     */
    private void updateClipButtonState(ImageButton button, boolean isClipEnabled) {
        if (isClipEnabled) {
            button.setBackgroundColor(0xFF4CAF50); // 绿色背景表示启用裁剪
            button.setColorFilter(0xFFFFFFFF); // 白色图标
        } else {
            button.setBackgroundColor(0x00000000); // 透明背景
            button.setColorFilter(0xFF888888); // 灰色图标
        }
    }

    @Override
    public int getItemCount() {
        return stickers != null ? stickers.size() : 0;
    }

    public void updateData(List<Sticker> newStickers) {
        this.stickers = newStickers != null ? newStickers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }

    static class LayerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPreview;
        TextView tvLayerName;
        TextView tvLayerInfo;
        ImageButton btnMoveToTop;
        ImageButton btnMoveToBottom;
        ImageView ivDragHandle;
        ImageButton btnClipToRoundedCorners;

        public LayerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreview = itemView.findViewById(R.id.ivPreview);
            tvLayerName = itemView.findViewById(R.id.tvLayerName);
            tvLayerInfo = itemView.findViewById(R.id.tvLayerInfo);
            btnMoveToTop = itemView.findViewById(R.id.btnMoveToTop);
            btnMoveToBottom = itemView.findViewById(R.id.btnMoveToBottom);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
            btnClipToRoundedCorners = itemView.findViewById(R.id.btnClipToRoundedCorners);
        }
    }
}