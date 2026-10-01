package com.eggyhub.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.eggyhub.android.R;
import java.util.List;

/**
 * 地图适配器
 * 用于RecyclerView展示地图项列表，处理地图数据与视图的绑定
 */
public class MapAdapter extends RecyclerView.Adapter<MapAdapter.MapViewHolder> {

    /** 地图项数据列表 */
    private final List<MapItem> mapList;
    private final List<String> mapCodeCacheList;

    /**
     * 构造函数
     * @param mapList 初始化的地图项数据列表
     */
    public MapAdapter(List<MapItem> mapList, List<String> mapCodeCacheList) {
        this.mapList = mapList;
        this.mapCodeCacheList = mapCodeCacheList;
    }

    /**
     * 更新地图项数据列表
     * @param newMapList 新的地图项数据列表
     */
    public void updateMapList(List<MapItem> newMapList) {
        this.mapList.clear();
        this.mapList.addAll(newMapList);
        notifyDataSetChanged();
    }

    /**
     * 创建地图项视图持有者
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 地图项视图持有者
     */
    @NonNull
    @Override
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_map_card, parent, false);
        return new MapViewHolder(view);
    }

    /**
     * 绑定地图数据到视图
     * @param holder 视图持有者
     * @param position 数据位置
     */
    @Override
    public void onBindViewHolder(@NonNull MapViewHolder holder, int position) {
        MapItem mapItem = mapList.get(position);
        holder.mapNameTextView.setText(mapItem.getName());
        holder.ownerNameTextView.setText("作者：" + mapItem.getOwnerName());
        holder.mapIntroTextView.setText("地图描述：" + mapItem.getIntro());

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(mapItem.getImageUrl())
                .placeholder(android.R.color.darker_gray) // Placeholder for gray area
                .into(holder.mapImageView);

        holder.itemView.setOnClickListener(v -> {
            // 显示地图详情对话框
            if (v.getContext() instanceof MainActivity) {
                MainActivity activity = (MainActivity) v.getContext();
                MapDetailDialog.newInstance(mapItem).show(activity.getSupportFragmentManager(), "MapDetailDialog");
            }
        });
    }

    @Override
    public int getItemCount() {
        return mapList.size();
    }

    public static class MapViewHolder extends RecyclerView.ViewHolder {
        TextView mapNameTextView;
        ImageView mapImageView;
        TextView ownerNameTextView;
        TextView mapIntroTextView;

        public MapViewHolder(@NonNull View itemView) {
            super(itemView);
            mapNameTextView = itemView.findViewById(R.id.mapNameTextView);
            mapImageView = itemView.findViewById(R.id.mapImageView);
            ownerNameTextView = itemView.findViewById(R.id.authorNameTextView);
            mapIntroTextView = itemView.findViewById(R.id.mapDescriptionTextView);
           
        }
    }


}