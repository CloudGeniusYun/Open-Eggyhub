package com.eggyhub.android;

import com.eggyhub.android.log.AppLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.WindowManager;
import android.content.DialogInterface;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.eggyhub.android.BasicResponse;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.bumptech.glide.Glide;
import com.eggyhub.android.ShareCodeItem;
import com.eggyhub.android.theme.CardViewTheme;
import com.eggyhub.android.theme.ThemeViewModel;

import java.io.IOException;
import java.util.List;

/**
 * 分享码适配器
 * 用于RecyclerView展示分享码列表，并处理点赞和领取事件
 */
public class ShareCodeAdapter extends RecyclerView.Adapter<ShareCodeAdapter.ShareCodeViewHolder> {

    /**
     * 分享码数据列表
     */
    private List<ShareCodeItem> shareCodeList;
    /**
     * 点赞点击事件监听器
     */
    private OnLikeClickListener likeClickListener;
    /**
     * 领取点击事件监听器
     */
    private OnClaimClickListener claimClickListener;
    
    /**
     * 当前分类ID（用于id=9的特殊样式）
     */
    private int currentCategoryId = 1;

    /**
     * 当前主题（用于应用CardView样式）
     */
    private CardViewTheme currentTheme;

    /**
     * 点赞点击事件回调接口
     */
    public interface OnLikeClickListener {
        /**
         * 点赞被点击时调用
         * @param shareCodeId 分享码ID
         */
        void onLikeClick(int shareCodeId);
    }

    /**
     * 领取点击事件回调接口
     */
    public interface OnClaimClickListener {
        /**
         * 领取按钮被点击时调用
         * @param item 分享码对象
         */
        void onClaimClick(ShareCodeItem item);
    }

    /**
     * 构造函数
     * @param shareCodeList 分享码数据列表
     * @param likeClickListener 点赞点击事件监听器
     * @param claimClickListener 领取点击事件监听器
     */
    public ShareCodeAdapter(List<ShareCodeItem> shareCodeList, OnLikeClickListener likeClickListener, OnClaimClickListener claimClickListener) {
        this.shareCodeList = shareCodeList;
        this.likeClickListener = likeClickListener;
        this.claimClickListener = claimClickListener;
    }

    /**
     * 设置当前主题
     * @param theme CardView主题
     */
    public void setCardViewTheme(CardViewTheme theme) {
        this.currentTheme = theme;
    }

    /**
     * 设置当前分类ID
     * @param categoryId 分类ID
     */
    public void setCurrentCategoryId(int categoryId) {
        this.currentCategoryId = categoryId;
        notifyDataSetChanged(); // 更新所有item的样式
    }

    /**
     * 创建视图持有者
     * @param parent 父视图组
     * @param viewType 视图类型
     * @return 分享码视图持有者
     */
    @NonNull
    @Override
    public ShareCodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_code, parent, false);
        return new ShareCodeViewHolder(view);
    }

    /**
     * 绑定数据到视图持有者
     * @param holder 视图持有者
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull ShareCodeViewHolder holder, int position) {
        ShareCodeItem item = shareCodeList.get(position);

        // 延迟应用CardView主题，确保视图已布局
        holder.itemView.post(() -> applyCardViewTheme(holder));

        // 使用Glide加载封面图
        Glide.with(holder.itemView.getContext()).load(item.getCover()).into(holder.ivCover);
        // 设置分享码名称
        holder.tvShareCodeName.setText(item.getName());
        // 设置提供者
        holder.tvProvider.setText(item.getProvider());
        // 设置描述
        holder.tvDescription.setText(item.getDescription());
        holder.tvDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDescriptionDialog(v.getContext(), item.getDescription());
            }
        });
        // 设置库存
        holder.tvStock.setText(String.valueOf(item.getStock()) + "件");
        // 设置点赞数
        holder.tvLikes.setText(String.valueOf(item.getLikes()));

        // 设置价格逻辑
        if (item.getValue() == 50) {
            holder.ivPriceIcon.setImageResource(R.drawable.kuai);
            holder.tvPriceValue.setText(" 1");
        } else {
            holder.ivPriceIcon.setImageResource(R.drawable.suipian);
            holder.tvPriceValue.setText(" "+String.valueOf(item.getValue()));
        }

        // 设置领取按钮点击事件
        holder.btnClaim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (claimClickListener != null) {
                    claimClickListener.onClaimClick(item);
                }
            }
        });

        // 设置点赞图标点击事件
        holder.ivLikesIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (likeClickListener != null) {
                    likeClickListener.onLikeClick(item.getId());
                }
            }
        });

        // 根据库存设置提醒补货按钮的可见性 及 领取按钮状态
        if (item.getStock() <= 0) {
            holder.llRestockNotice.setVisibility(View.VISIBLE);
            // 设置提醒补货点击事件
            holder.llRestockNotice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRestockNotice(item.getId(), v);
                }
            });

            // 库存为0，禁用领取按钮并变灰
            holder.btnClaim.setEnabled(false);
            holder.btnClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        } else {
            holder.llRestockNotice.setVisibility(View.GONE);

            // 库存不为0，启用领取按钮并恢复颜色
            holder.btnClaim.setEnabled(true);
            holder.btnClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E90FF")));
        }
        
        // 根据分类ID设置特殊的渐变背景和紫色主题（仅id=9）
        if (currentCategoryId == 9) {
            // id=9分类：白色 → 浅紫色渐变背景
            GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, // 从上到下渐变
                new int[]{
                    Color.parseColor("#FFFFFF"), // 白色
                    Color.parseColor("#E6D5F2")  // 浅紫色
                }
            );
            gradientDrawable.setCornerRadius(8f); // 圆角8dp
            holder.itemView.setBackground(gradientDrawable);
            
            // 1. 领取按钮：紫色渐变背景
            // 先禁用MaterialButton的backgroundTint，否则渐变会被覆盖
            androidx.core.view.ViewCompat.setBackgroundTintList(holder.btnClaim, null);
            
            // 创建紫色渐变背景
            GradientDrawable claimButtonGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, // 从左到右渐变
                new int[]{
                    Color.parseColor("#9b59b6"), // 浅紫色
                    Color.parseColor("#8e44ad"), // 中紫色
                    Color.parseColor("#6c3483")  // 深紫色
                }
            );
            
            // 设置圆角（胶囊状：圆角半径为高度的一半）
            claimButtonGradient.setCornerRadius(100f); // 大圆角值，确保胶囊状
            
            // 库存为0时，按钮禁用且变灰
            if (item.getStock() <= 0) {
                claimButtonGradient.setColor(Color.GRAY); // 禁用时灰色
            }
            
            // 设置背景
            holder.btnClaim.setBackground(claimButtonGradient);
            
            // 2. 货币消耗数字：紫色文字
            holder.tvPriceValue.setTextColor(Color.parseColor("#9b59b6"));
            
            // 3. 点赞图标：紫色填充
            Drawable likeIcon = holder.ivLikesIcon.getDrawable();
            if (likeIcon != null) {
                Drawable wrappedDrawable = DrawableCompat.wrap(likeIcon.mutate());
                DrawableCompat.setTint(wrappedDrawable, Color.parseColor("#9b59b6"));
                holder.ivLikesIcon.setImageDrawable(wrappedDrawable);
            }
            
            // 4. 补货图标：紫色填充（llRestockNotice中的ImageView）
            if (holder.llRestockNotice.getChildCount() > 0) {
                ImageView restockIcon = (ImageView) holder.llRestockNotice.getChildAt(0);
                Drawable restockDrawable = restockIcon.getDrawable();
                if (restockDrawable != null) {
                    Drawable wrappedRestockDrawable = DrawableCompat.wrap(restockDrawable.mutate());
                    DrawableCompat.setTint(wrappedRestockDrawable, Color.parseColor("#9b59b6"));
                    restockIcon.setImageDrawable(wrappedRestockDrawable);
                }
            }
        } else {
            // 其他分类：恢复默认背景和颜色
            holder.itemView.setBackgroundResource(R.drawable.item_share_code_background);
            
            // 恢复领取按钮默认蓝色背景
            if (item.getStock() > 0) {
                holder.btnClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E90FF")));
            }
            
            // 恢复货币消耗数字默认蓝色
            holder.tvPriceValue.setTextColor(android.graphics.Color.parseColor("#1E90FF"));
            
            // 恢复点赞图标默认颜色
            Drawable likeIcon = holder.ivLikesIcon.getDrawable();
            if (likeIcon != null) {
                Drawable wrappedDrawable = DrawableCompat.wrap(likeIcon.mutate());
                DrawableCompat.setTintList(wrappedDrawable, null); // 清除tint，恢复原色
                holder.ivLikesIcon.setImageDrawable(wrappedDrawable);
            }
            
            // 恢复补货图标默认颜色
            if (holder.llRestockNotice.getChildCount() > 0) {
                ImageView restockIcon = (ImageView) holder.llRestockNotice.getChildAt(0);
                Drawable restockDrawable = restockIcon.getDrawable();
                if (restockDrawable != null) {
                    Drawable wrappedRestockDrawable = DrawableCompat.wrap(restockDrawable.mutate());
                    DrawableCompat.setTintList(wrappedRestockDrawable, null); // 清除tint，恢复原色
                    restockIcon.setImageDrawable(wrappedRestockDrawable);
                }
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

    private void showDescriptionDialog(Context context, String description) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("详情：");
        builder.setMessage(description);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置弹窗的固定比例
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8); // 宽度为屏幕的80%
        lp.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.4); // 高度为屏幕的40%
        dialog.getWindow().setAttributes(lp);
        AppLogger.d("ShareCodeAdapter", "显示分享码详情弹窗");
    }

    private void sendRestockNotice(int id, View view) {
        Context context = view.getContext();
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/mails/notice?id=" + id;

        String accessToken = SecureStorageManager.getAccessToken();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                view.post(() -> Toast.makeText(view.getContext(), "提醒补货请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        try {
                            String bodyString = responseBody.string();
                            Gson gson = new Gson();
                            BasicResponse responseModel = gson.fromJson(bodyString, BasicResponse.class);
                            String message = responseModel != null ? responseModel.getMessage() : "";
                            view.post(() -> Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            view.post(() -> Toast.makeText(view.getContext(), "解析响应失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        view.post(() -> Toast.makeText(view.getContext(), "提醒补货请求失败: " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    /**
     * 应用CardView主题
     */
    private void applyCardViewTheme(ShareCodeViewHolder holder) {
        // id=9的分类不参与自定义主题（已有特殊渐变背景）
        if (currentCategoryId == 9) {
            if (holder.stickerView != null) {
                holder.stickerView.setVisibility(android.view.View.GONE);
            }
            return;
        }

        if (currentTheme == null || !currentTheme.isEnabled()) {
            android.util.Log.d("ShareCodeAdapter", "applyCardViewTheme: currentTheme is null or disabled");
            if (holder.stickerView != null) {
                holder.stickerView.setVisibility(android.view.View.GONE);
            }
            return;
        }

        if (!(holder.itemView instanceof MaterialCardView)) {
            android.util.Log.d("ShareCodeAdapter", "applyCardViewTheme: itemView is not MaterialCardView");
            return;
        }

        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        
        // 检查CardView尺寸是否已准备好
        if (cardView.getWidth() == 0 || cardView.getHeight() == 0) {
            android.util.Log.d("ShareCodeAdapter", "applyCardViewTheme: CardView size is 0, waiting for layout");
            cardView.post(() -> applyCardViewTheme(holder));
            return;
        }
        
        android.util.Log.d("ShareCodeAdapter", "applyCardViewTheme: Applying theme with " + currentTheme.getStickers().size() + " stickers");

        // 保持默认阴影效果（4dp）
        float defaultElevation = 4f * holder.itemView.getContext().getResources().getDisplayMetrics().density;
        cardView.setCardElevation(defaultElevation);
        
        // 设置背景色为白色
        cardView.setCardBackgroundColor(android.graphics.Color.WHITE);

        // 应用贴纸
        if (holder.stickerView != null) {
            if (!currentTheme.getStickers().isEmpty()) {
                holder.stickerView.setStickers(currentTheme.getStickers());
                holder.stickerView.setCornerRadius(currentTheme.getCornerRadius());
                holder.stickerView.setVisibility(android.view.View.VISIBLE);
                android.util.Log.d("ShareCodeAdapter", "applyCardViewTheme: Stickers applied");
            } else {
                holder.stickerView.setVisibility(android.view.View.GONE);
            }
        }

        // 应用圆角到CardView
        cardView.setRadius(currentTheme.getCornerRadius() *
            holder.itemView.getContext().getResources().getDisplayMetrics().density);

        // 应用边框设置
        if (currentTheme.getBorderWidth() > 0 && currentTheme.getBorderColor() != 0) {
            cardView.setStrokeWidth((int)(currentTheme.getBorderWidth() *
                holder.itemView.getContext().getResources().getDisplayMetrics().density));
            cardView.setStrokeColor(currentTheme.getBorderColor());
            android.util.Log.d("ShareCodeAdapter", "applyCardViewTheme: Border applied - width=" + currentTheme.getBorderWidth() + "dp");
        } else {
            // 无边框
            cardView.setStrokeWidth(0);
        }
    }

    /**
     * 分享码视图持有者
     * 绑定分享码列表项布局中的UI组件
     */
    static class ShareCodeViewHolder extends RecyclerView.ViewHolder {
        /**
         * 贴纸显示View
         */
        com.eggyhub.android.views.StickerView stickerView;
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
        LinearLayout llRestockNotice;
        ImageView ivPriceIcon;
        TextView tvPriceValue;

        /**
         * 构造函数
         * @param itemView 列表项视图
         */
        public ShareCodeViewHolder(@NonNull View itemView) {
            super(itemView);
            stickerView = itemView.findViewById(R.id.stickerView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvShareCodeName = itemView.findViewById(R.id.tv_share_code_name);
            tvProvider = itemView.findViewById(R.id.tv_provider);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvStock = itemView.findViewById(R.id.tv_stock);
            tvLikes = itemView.findViewById(R.id.tv_likes);
            btnClaim = itemView.findViewById(R.id.btn_claim);
            ivLikesIcon = itemView.findViewById(R.id.iv_likes_icon);
            llRestockNotice = itemView.findViewById(R.id.ll_restock_notice);
            ivPriceIcon = itemView.findViewById(R.id.iv_price_icon);
            tvPriceValue = itemView.findViewById(R.id.tv_price_value);
        }
    }
}