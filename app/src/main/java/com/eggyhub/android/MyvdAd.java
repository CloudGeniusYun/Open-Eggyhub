package com.eggyhub.android;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * 视频列表适配器
 * 用于RecyclerView展示视频列表数据，并处理点击事件
 */
public class MyvdAd extends RecyclerView.Adapter<MyvdAd.VideoViewHolder> {
    Context mAppContext;



    Context context;

    /**
     * 视频数据列表
     */
    private  List<Myvditem> videoList;
    /**
     * 视频项点击事件监听器
     */





    /**
     * 视频项点击事件回调接口
     */
    public class OnItemClickListener {
        /**
         * 视频项被点击时调用
         * @param item 被点击的视频项数据
         */
        void onItemClick(Myvditem item){

        };
    }

    /**
     * 设置视频项点击事件监听器
     * @param listener 监听器实例
     */


    /**
     * 构造函数

     */
    public MyvdAd(List<Myvditem> videoList,Context AppContext) {
        this.videoList = videoList;
        this.mAppContext =AppContext.getApplicationContext();
    }

    /**
     * 创建视图持有者
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 视频视图持有者
     */
    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_list, parent, false);
        return new VideoViewHolder(view);
    }

    /**
     * 绑定数据到视图持有者
     * @param holder 视图持有者
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Myvditem myvditem = videoList.get(position);
        // 设置视频名称
        holder.textViewVideoName.setText(myvditem.getName());
        // 使用Glide加载视频封面图片
        Glide.with(holder.imageViewCover.getContext()).load(myvditem.getCover()).into(holder.imageViewCover);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {


            Intent intent = new Intent(mAppContext, MangerVideoActivity.class);
            intent.putExtra("name",myvditem.getName());
            intent.putExtra("id",myvditem.getId());
            intent.putExtra("cover",myvditem.getCover());
            intent.putExtra("bv",myvditem.getLink());
            intent.putExtra("ds",myvditem.getDescription());
            intent.putExtra("gr",myvditem.getGr());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mAppContext.startActivity(intent);

        });
    }

    /**
     * 获取视频列表项数量
     * @return 列表项数量
     */
    @Override
    public int getItemCount() {
        return videoList.size();
    }

    /**
     * 更新视频列表数据
     * @param newVideoList 新的视频数据列表
     */
    public void updateVideoList(List<Myvditem> newVideoList) {
        this.videoList.clear();
        this.videoList.addAll(newVideoList);
        notifyDataSetChanged(); // 通知适配器数据已更改
    }

    /**
     * 视频视图持有者
     * 绑定视频列表项布局中的UI组件
     */
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        /**
         * 视频封面图片视图
         */
        ImageView imageViewCover;
        /**
         * 视频名称文本视图
         */
        TextView textViewVideoName;

        /**
         * 构造函数
         * @param itemView 列表项视图
         */
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCover = itemView.findViewById(R.id.imageViewCover);
            textViewVideoName = itemView.findViewById(R.id.textViewVideoName);
        }
    }
}