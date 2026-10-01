package com.eggyhub.android;

import com.eggyhub.android.log.AppLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eggyhub.android.R;
import com.eggyhub.android.CategoryItem;

import java.util.List;

/**
 * 分类按钮适配器
 * 用于在RecyclerView中展示分类按钮，并处理选中状态和点击事件
 */
public class CategoryButtonAdapter extends RecyclerView.Adapter<CategoryButtonAdapter.CategoryButtonViewHolder> {

    /**
     * 分类数据列表
     */
    private List<CategoryItem> categoryList;
    /**
     * 点击事件监听器
     */
    private OnItemClickListener listener;
    /**
     * 当前选中的位置
     */
    private int selectedPosition = 0; // 初始化为默认选中位置
    /**
     * 日志标签
     */
    private static final String TAG = "CategoryButtonAdapter";

    /**
     * 点击事件监听器接口
     */
    public interface OnItemClickListener {
        /**
         * 当分类项被点击时调用
         * @param category 被点击的分类
         * @param position 点击的位置
         */
        void onItemClick(CategoryItem category, int position);
    }

    /**
     * 构造函数
     * @param categoryList 分类数据列表
     * @param listener 点击事件监听器
     */
    public CategoryButtonAdapter(List<CategoryItem> categoryList, OnItemClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    /**
     * 更新数据
     * @param newCategoryList 新的分类数据列表
     */
    public void updateData(List<CategoryItem> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        AppLogger.d(TAG, "Updating data with " + newCategoryList.size() + " items.");
        notifyDataSetChanged(); // 通知适配器数据已更改
    }

    /**
     * 创建ViewHolder
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 创建的CategoryButtonViewHolder实例
     */
    @NonNull
    @Override
    public CategoryButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载分类按钮布局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_button, parent, false);
        return new CategoryButtonViewHolder(view);
    }

    /**
     * 绑定数据到ViewHolder
     * @param holder ViewHolder实例
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull CategoryButtonViewHolder holder, int position) {
        CategoryItem category = categoryList.get(position);
        AppLogger.d(TAG, "Binding category: " + category.getName() + " at position: " + position + ", isSelected: " + (position == selectedPosition));
        // 绑定数据并设置选中状态
        holder.bind(category, listener, position == selectedPosition);
    }

    /**
     * 获取item数量
     * @return 分类列表大小
     */
    @Override
    public int getItemCount() {
        AppLogger.d(TAG, "getItemCount: " + categoryList.size());
        return categoryList.size();
    }

    /**
     * 设置选中的位置
     * @param position 要选中的位置
     */
    public void setSelectedPosition(int position) {
        int oldSelectedPosition = this.selectedPosition;
        this.selectedPosition = position;
        // 通知旧位置和新位置的数据已更改
        notifyItemChanged(oldSelectedPosition);
        notifyItemChanged(selectedPosition);
    }

    /**
     * 分类按钮视图持有者
     * 用于持有分类按钮的视图组件
     */
    static class CategoryButtonViewHolder extends RecyclerView.ViewHolder {
        /**
         * 分类按钮TextView
         */
        TextView btnCategory;

        /**
         * 构造函数
         * @param itemView 项视图
         */
        public CategoryButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            btnCategory = itemView.findViewById(R.id.btn_category);
        }

        /**
         * 绑定数据到视图
         * @param category 分类数据
         * @param listener 点击事件监听器
         * @param isSelected 是否选中
         */
        public void bind(final CategoryItem category, final OnItemClickListener listener, boolean isSelected) {
            // 设置分类名称
            btnCategory.setText(category.getName());
            // 根据选中状态设置不同的背景和文字颜色
            if (isSelected) {
                btnCategory.setBackgroundResource(R.drawable.category_selected_background);
                btnCategory.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
            } else {
                btnCategory.setBackgroundResource(R.drawable.category_normal_background);
                btnCategory.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
            }
            // 设置点击事件
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(category, getAdapterPosition());
                }
            });
        }
    }
}