package com.eggyhub.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.RouteListingPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MycdAd extends RecyclerView.Adapter<MycdAd.ShareCodeViewHolder>{
    String at;
    Context mAppContext;

    private List<ShareCodeItem> shareCodeList;

    /**
     * 领取点击事件回调接口
     */
    public interface OnClaimClickListener {
        /**
         * 领取按钮被点击时调用
         * @param giftId 礼品ID
         */
        void onClaimClick(int giftId);
    }

    /**
     * 构造函数
     * @param shareCodeList 分享码数据列表


     */
    public MycdAd(List<ShareCodeItem> shareCodeList,String at,Context AppContext) {
        this.mAppContext =AppContext.getApplicationContext();
        this.shareCodeList = shareCodeList;
        this.at = at;


    }

    /**
     * 创建视图持有者
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 分享码视图持有者
     */
    @NonNull
    @Override
    public MycdAd.ShareCodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mycd, parent, false);
        return new MycdAd.ShareCodeViewHolder(view);
    }

    /**
     * 绑定数据到视图持有者
     * @param holder 视图持有者
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull MycdAd.ShareCodeViewHolder holder, int position) {
        ShareCodeItem item = shareCodeList.get(position);

        // 使用Glide加载封面图
        Glide.with(holder.itemView.getContext()).load(item.getCover()).into(holder.ivCover);
        // 设置分享码名称
        holder.tvShareCodeName.setText(item.getName());
        // 设置提供者
        holder.tvProvider.setText(at);
        // 设置描述
        holder.tvDescription.setText(item.getDescription());
        // 设置库存
        holder.tvStock.setText(String.valueOf(item.getStock()) + "件");
        // 设置点赞数
        holder.tvLikes.setText(String.valueOf(item.getLikes()));
        holder.LL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mAppContext, ManagerCodeActivity.class);
                intent.putExtra("name",item.getName());
                intent.putExtra("id",item.getId());
                intent.putExtra("cover",item.getCover());
                intent.putExtra("ds",item.getDescription());
                intent.putExtra("gr",item.getGr());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mAppContext.startActivity(intent);
            }
        });

        if (position == 0) {
            checkTutorialStatus(holder);
        }
    }

    private void checkTutorialStatus(ShareCodeViewHolder holder) {
        Context context = holder.itemView.getContext();
        TutorialManager manager = TutorialManager.getInstance(context);
        if (manager.isTutorialRunning() && TutorialManager.TUTORIAL_SUPPLEMENT_CODE.equals(manager.getCurrentTutorial())) {
            int stepIndex = manager.getStepIndex();
            if (stepIndex == 3 && context instanceof Activity) {
                Activity activity = (Activity) context;
                holder.itemView.post(() -> {
                    if (holder.itemView.isShown()) {
                        GuideHelper.show(activity, holder.itemView, "第四步：点击你要补充分享码的资源进入管理页", true, () -> {
                            manager.nextStep(); // 3 -> 4
                        });
                    }
                });
            }
        }
    }

    /**
     * 获取分享码列表项数量
     * @return 列表项数量
     */
    @Override
    public int getItemCount() {
        return shareCodeList.size();
    }

    /**
     * 分享码视图持有者
     * 绑定分享码列表项布局中的UI组件
     */
    static class ShareCodeViewHolder extends RecyclerView.ViewHolder {
        /**
         * 封面图
         */
        ImageView ivCover;
        /**
         * 分享码名称
         */
        TextView tvShareCodeName;
        /**
         * 提供者
         */
        TextView tvProvider;
        /**
         * 描述
         */
        TextView tvDescription;
        /**
         * 库存
         */
        TextView tvStock;
        /**
         * 点赞数
         */
        TextView tvLikes;
        /**
         * 领取按钮
         */
        Button btnClaim;
        /**
         * 点赞图标
         */
        ImageView ivLikesIcon;
        LinearLayout LL;

        /**
         * 构造函数
         * @param itemView 列表项视图
         */
        public ShareCodeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvShareCodeName = itemView.findViewById(R.id.tv_share_code_name);
            tvProvider = itemView.findViewById(R.id.tv_provider);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvStock = itemView.findViewById(R.id.tv_stock);
            tvLikes = itemView.findViewById(R.id.tv_likes);
            btnClaim = itemView.findViewById(R.id.btn_claim);
            ivLikesIcon = itemView.findViewById(R.id.iv_likes_icon);
            LL = itemView.findViewById(R.id.MyCode);
        }
    }
}
