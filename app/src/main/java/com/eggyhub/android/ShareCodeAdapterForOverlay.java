package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.annotations.SerializedName;
import com.eggyhub.android.log.AppLogger;

import java.io.Serializable;
import java.util.List;

public class ShareCodeAdapterForOverlay extends RecyclerView.Adapter<ShareCodeAdapterForOverlay.ShareCodeViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ShareCodeItem item);
    }

    private List<ShareCodeItem> shareCodeList;
    private OnItemClickListener listener;
    private int fontColor; // New member variable for font color

    public ShareCodeAdapterForOverlay(List<ShareCodeItem> shareCodeList, OnItemClickListener listener, int fontColor) {
        this.shareCodeList = shareCodeList;
        this.listener = listener;
        this.fontColor = fontColor;
    }

    @NonNull
    @Override
    public ShareCodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.share_code_item, parent, false);
        return new ShareCodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareCodeViewHolder holder, int position) {
        ShareCodeItem item = shareCodeList.get(position);
        holder.textViewName.setText(item.getName());
        holder.textViewName.setTextColor(fontColor);
        holder.textViewDescription.setText(truncateText(item.getDescription(), 30));
        holder.textViewDescription.setTextColor(fontColor);
        holder.textViewShareCodeStock.setText("库存: " + item.getStock()); // Set stock text
        holder.textViewShareCodeStock.setTextColor(fontColor);
        AppLogger.d("ShareCodeAdapter", "Loading image from: " + item.getImageUrl());
        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_launcher_background) // 占位符
                .error(R.drawable.ic_launcher_background) // 错误图片
                .into(holder.imageViewShareCode);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    @Override
    public int getItemCount() {
        return shareCodeList.size();
    }

    static class ShareCodeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDescription;
        TextView textViewShareCodeStock; // Add TextView for stock
        ImageView imageViewShareCode;

        public ShareCodeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewShareCodeName);
            textViewDescription = itemView.findViewById(R.id.textViewShareCodeDescription);
            textViewShareCodeStock = itemView.findViewById(R.id.textViewShareCodeStock); // Initialize stock TextView
            imageViewShareCode = itemView.findViewById(R.id.imageViewShareCode);
        }
    }

    // Data class for ShareCodeItem
    public static class ShareCodeItem implements Serializable {
        private String name;
        private String description;
        @SerializedName("cover")
        private String imageUrl;
        private String id;
        @SerializedName("stock")
        private int stock; // Add stock field
        private int hosting_status; // Add hosting_status field

        public ShareCodeItem(String name, String description, String imageUrl, String id, int stock) {
            this.name = name;
            this.description = description;
            this.imageUrl = imageUrl;
            this.id = id;
            this.stock = stock;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
        public String getId() { return id; }
        public int getStock() { return stock; }
        public void setStock(int stock) { this.stock = stock; } // Add setter for stock
        public int getHosting_status() { return hosting_status; }
    }
}
