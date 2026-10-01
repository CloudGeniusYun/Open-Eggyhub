package com.eggyhub.android;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章列表适配器
 * 用于在RecyclerView中展示文章数据，处理数据绑定和点击事件
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    /**
     * 文章数据列表
     */
    private final List<ArticleItem> articleList;

    /**
     * 构造函数
     * @param articleList 包含文章数据的JSON数组
     */
    public ArticleAdapter(List<ArticleItem> articleList) {
        this.articleList = articleList;
    }

    /**
     * 创建ViewHolder
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 创建的ArticleViewHolder实例
     */
    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载文章项布局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    /**
     * 绑定数据到ViewHolder
     * @param holder ViewHolder实例
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        ArticleItem article = articleList.get(position);
        holder.textViewTitle.setText(article.getTitle() != null ? article.getTitle() : "");
        holder.textViewAuthor.setText(article.getAuthor() != null ? article.getAuthor() : "");
        holder.textViewCategory.setText(article.getCategory() != null ? article.getCategory() : "");
        holder.textViewDate.setText(article.getDate() != null ? article.getDate() : "");
    }

    /**
     * 获取item数量
     * @return 文章列表大小
     */
    @Override
    public int getItemCount() {
        return articleList.size();
    }

    /**
     * 文章视图持有者
     * 用于持有文章项的视图组件
     */
    class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;   // 文章标题TextView
        TextView textViewAuthor;  // 文章作者TextView
        TextView textViewCategory; // 文章分类TextView
        TextView textViewDate; // 文章日期TextView

        /**
         * 构造函数
         * @param itemView 项视图
         */
        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化视图组件
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewCategory = itemView.findViewById(R.id.textViewCategory);
            textViewDate = itemView.findViewById(R.id.textViewDate);

            // 设置项点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ArticleItem clickedArticle = articleList.get(position);
                    // 创建跳转到文章详情的Intent
                    Intent intent = new Intent(itemView.getContext(), ArticleDetailActivity.class);
                    intent.putExtra("id", clickedArticle.getId());
                    intent.putExtra("title", clickedArticle.getTitle());
                    intent.putExtra("author", clickedArticle.getAuthor());
                    // 启动文章详情Activity
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }


}