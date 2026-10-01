package com.eggyhub.android.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eggyhub.android.R;
import com.eggyhub.android.adapter.LayerAdapter;
import com.eggyhub.android.theme.Sticker;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图层管理BottomSheet
 */
public class LayerManagerBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvLayers;
    private TextView tvLayerCount;
    private TextView tvEmptyState;

    private List<Sticker> stickers = new ArrayList<>();
    private LayerAdapter adapter;
    private OnLayerActionListener listener;

    public interface OnLayerActionListener {
        void onLayerSelected(int position, Sticker sticker);
        void onLayerMoved(int fromPosition, int toPosition);
        void onMoveToTop(int position);
        void onMoveToBottom(int position);
        void onClipToRoundedCornersChanged(int stickerIndex, boolean clipToRoundedCorners);
    }

    public static LayerManagerBottomSheet newInstance(List<Sticker> stickers, OnLayerActionListener listener) {
        LayerManagerBottomSheet bottomSheet = new LayerManagerBottomSheet();
        bottomSheet.stickers = stickers != null ? stickers : new ArrayList<>();
        bottomSheet.listener = listener;
        return bottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layer_manager, container, false);

        initViews(view);
        setupRecyclerView();

        return view;
    }

    private void initViews(View view) {
        rvLayers = view.findViewById(R.id.rvLayers);
        tvLayerCount = view.findViewById(R.id.tvLayerCount);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerView() {
        adapter = new LayerAdapter(stickers, new LayerAdapter.OnLayerActionListener() {
            @Override
            public void onLayerSelected(int position, Sticker sticker) {
                if (listener != null) {
                    listener.onLayerSelected(position, sticker);
                }
            }

            @Override
            public void onMoveToTop(int position) {
                if (listener != null) {
                    listener.onMoveToTop(position);
                }
                updateLayerCount();
            }

            @Override
            public void onMoveToBottom(int position) {
                if (listener != null) {
                    listener.onMoveToBottom(position);
                }
                updateLayerCount();
            }

            @Override
            public void onClipToRoundedCornersChanged(int stickerIndex, boolean clipToRoundedCorners) {
                if (listener != null) {
                    listener.onClipToRoundedCornersChanged(stickerIndex, clipToRoundedCorners);
                }
            }
        });

        rvLayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLayers.setAdapter(adapter);

        // 拖拽排序功能
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN,
                0
            ) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    int fromListPosition = viewHolder.getAdapterPosition();
                    int toListPosition = target.getAdapterPosition();

                    // 将列表position转换为实际贴纸索引
                    int fromStickerIndex = stickers.size() - 1 - fromListPosition;
                    int toStickerIndex = stickers.size() - 1 - toListPosition;

                    // 交换贴纸位置
                    Collections.swap(stickers, fromStickerIndex, toStickerIndex);

                    // 更新图层索引
                    for (int i = 0; i < stickers.size(); i++) {
                        stickers.get(i).setLayerIndex(i);
                    }

                    // 更新列表显示（注意：这里使用列表position）
                    adapter.notifyItemMoved(fromListPosition, toListPosition);

                    if (listener != null) {
                        listener.onLayerMoved(fromStickerIndex, toStickerIndex);
                    }

                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // 不支持滑动删除
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    // 启用长按拖动
                    return true;
                }
            }
        );

        itemTouchHelper.attachToRecyclerView(rvLayers);

        updateLayerCount();
    }

    private void updateLayerCount() {
        int count = stickers.size();
        tvLayerCount.setText(count + "个图层");
        tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        rvLayers.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    public void updateStickers(List<Sticker> newStickers) {
        this.stickers = newStickers != null ? newStickers : new ArrayList<>();
        if (adapter != null) {
            adapter.updateData(this.stickers);
        }
        updateLayerCount();
    }
}