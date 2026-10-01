package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NameFrameAdapter extends RecyclerView.Adapter<NameFrameAdapter.NameFrameViewHolder> {

    private final List<NameFrameItem> nameFrameList;
    private int selectedPosition = -1;
    private OnItemClickListener onItemClickListener;
    private String userRole = "user";

    public interface OnItemClickListener {
        void onItemClick(int position, String frameName);
    }

    public NameFrameAdapter(List<NameFrameItem> nameFrameList) {
        this.nameFrameList = nameFrameList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public String getSelectedFrameName() {
        if (selectedPosition >= 0 && selectedPosition < nameFrameList.size()) {
            return nameFrameList.get(selectedPosition).getFrameName();
        }
        return null;
    }

    @NonNull
    @Override
    public NameFrameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_name_frame, parent, false);
        return new NameFrameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NameFrameViewHolder holder, int position) {
        NameFrameItem item = nameFrameList.get(position);
        
        // 检查是否为空白框
        boolean isNullFrame = "name_frame_null".equals(item.getFrameName());
        
        if (isNullFrame) {
            // 空白框显示灰色禁止图标
            int nullFrameResourceId = holder.itemView.getContext().getResources().getIdentifier(
                "name_frame_null_round", "drawable", holder.itemView.getContext().getPackageName());
            
            if (nullFrameResourceId != 0) {
                holder.imageViewNameFrame.setImageResource(nullFrameResourceId);
                holder.imageViewNameFrame.setBackgroundResource(0);
            } else {
                // 如果图标不存在，使用透明图片作为备用
                holder.imageViewNameFrame.setImageResource(android.R.color.transparent);
                holder.imageViewNameFrame.setBackgroundResource(0);
            }
            
            // 使用ImageView的contentDescription来显示提示文字
            holder.imageViewNameFrame.setContentDescription("不使用昵称框");
        } else {
            // 设置昵称框图片
            int resourceId = holder.itemView.getContext().getResources().getIdentifier(
                item.getThumbnailName(), "drawable", holder.itemView.getContext().getPackageName());
            
            if (resourceId != 0) {
                holder.imageViewNameFrame.setImageResource(resourceId);
                holder.imageViewNameFrame.setBackgroundResource(0);
            }
        }

        // 检查是否为pro昵称框且用户不是管理员
        boolean isProFrame = item.getFrameName().equals("name_frame_pro");
        boolean isAdmin = "admin".equals(userRole);
        boolean isEnabled = !isProFrame || isAdmin;

        // 设置选中状态
        holder.imageViewSelected.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        // 设置禁用状态
        if (!isEnabled) {
            holder.imageViewNameFrame.setAlpha(0.5f);
            holder.itemView.setEnabled(false);
            holder.imageViewSelected.setVisibility(View.GONE);
        } else {
            holder.imageViewNameFrame.setAlpha(1.0f);
            holder.itemView.setEnabled(true);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!isEnabled) {
                // 非管理员点击pro昵称框时显示提示
                android.widget.Toast.makeText(
                    holder.itemView.getContext(), 
                    "pro昵称框为管理员专属", 
                    android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            int previousPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            
            // 强制重新计算布局，避免网格重叠
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, item.getFrameName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return nameFrameList.size();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        if (previousPosition >= 0) {
            notifyItemChanged(previousPosition);
        }
        if (selectedPosition >= 0) {
            notifyItemChanged(selectedPosition);
        }
    }

    static class NameFrameViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewNameFrame;
        ImageView imageViewSelected;

        public NameFrameViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewNameFrame = itemView.findViewById(R.id.imageViewNameFrame);
            imageViewSelected = itemView.findViewById(R.id.imageViewSelected);
        }
    }
}