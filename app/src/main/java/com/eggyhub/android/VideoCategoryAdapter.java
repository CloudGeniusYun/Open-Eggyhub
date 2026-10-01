package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.eggyhub.android.R;

/**
 * 视频分类适配器
 * 用于RecyclerView展示视频分类列表，并处理点击事件和选中状态
 */
public class VideoCategoryAdapter extends RecyclerView.Adapter<VideoCategoryAdapter.VideoCategoryViewHolder> {

    /**
     * 分类项点击事件监听器
     */
    private OnItemClickListener listener;
    /**
     * 当前选中的分类位置
     * 默认选中第0项
     */
    private int selectedPosition = 0; // Default selected position

    /**
     * 分类项点击事件回调接口
     */
    public interface OnItemClickListener {
        /**
         * 分类项被点击时调用
         * @param position 被点击的分类项位置
         */
        void onItemClick(int position);
    }

    /**
     * 设置分类项点击事件监听器
     * @param listener 监听器实例
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 视频分类数据列表
     */
    private final List<VideoCategory> videoCategoryList;

    /**
     * 构造函数
     * @param videoCategoryList 视频分类数据列表
     */
    public VideoCategoryAdapter(List<VideoCategory> videoCategoryList) {
        this.videoCategoryList = videoCategoryList;
    }

    /**
     * 创建视图持有者
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 视频分类视图持有者
     */
    @NonNull
    @Override
    public VideoCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_category, parent, false);
        return new VideoCategoryViewHolder(view);
    }

    /**
     * 绑定数据到视图持有者
     * @param holder 视图持有者
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull VideoCategoryViewHolder holder, int position) {
        VideoCategory category = videoCategoryList.get(position);
        // 设置分类名称
        holder.textViewCategoryName.setText(category.getName());

        // 根据是否选中设置不同的背景
        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.category_selected_background); // 设置选中背景
        } else {
            holder.itemView.setBackgroundResource(R.drawable.category_normal_background); // 设置普通背景
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int previousSelectedPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                // 通知适配器前一个选中项和当前选中项的状态变化
                notifyItemChanged(previousSelectedPosition);
                notifyItemChanged(selectedPosition);
                // 触发点击事件回调
                listener.onItemClick(selectedPosition);
            }
        });
    }

    /**
     * 获取分类列表项数量
     * @return 列表项数量
     */
    @Override
    public int getItemCount() {
        return videoCategoryList.size();
    }

    /**
     * 视频分类视图持有者
     * 绑定分类列表项布局中的UI组件
     */
    public static class VideoCategoryViewHolder extends RecyclerView.ViewHolder {
        /**
         * 分类名称文本视图
         */
        TextView textViewCategoryName;

        /**
         * 构造函数
         * @param itemView 列表项视图
         */
        public VideoCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
        }
    }
}