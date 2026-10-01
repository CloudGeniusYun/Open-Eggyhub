package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MailAdapter extends RecyclerView.Adapter<MailAdapter.MailViewHolder> {

    private List<MailItem> mailList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MailItem mailItem);
    }

    public MailAdapter(List<MailItem> mailList, OnItemClickListener listener) {
        this.mailList = mailList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mail, parent, false);
        return new MailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MailViewHolder holder, int position) {
        MailItem mailItem = mailList.get(position);
        holder.titleTextView.setText(mailItem.getTitle() != null ? mailItem.getTitle() : "");
        
        String content = mailItem.getContent();
        if (content != null) {
            String displayContent = content.length() > 30 ?
                                    content.substring(0, 30) + "..." : content;
            holder.contentTextView.setText(displayContent);
        } else {
            holder.contentTextView.setText("");
        }

        holder.fromTextView.setText("来自: " + (mailItem.getFrom() != null ? mailItem.getFrom() : ""));
        holder.toTextView.setText("送至: " + (mailItem.getTo() != null ? mailItem.getTo() : ""));
        holder.createdAtTextView.setText(mailItem.getCreatedAt() != null ? mailItem.getCreatedAt() : "");

        // Set image based on imageRes
        String imageRes = mailItem.getImageRes();
        if (imageRes != null && !imageRes.isEmpty()) {
            int imageResId = holder.itemView.getContext().getResources().getIdentifier(
                    imageRes, "drawable", holder.itemView.getContext().getPackageName());
            if (imageResId != 0) {
                holder.imageView.setImageResource(imageResId);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(mailItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mailList.size();
    }

    static class MailViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView fromTextView;
        TextView toTextView;
        TextView createdAtTextView;
        ImageView imageView;

        public MailViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.mailTitleTextView);
            contentTextView = itemView.findViewById(R.id.mailContentTextView);
            fromTextView = itemView.findViewById(R.id.mailFromTextView);
            toTextView = itemView.findViewById(R.id.mailToTextView);
            createdAtTextView = itemView.findViewById(R.id.mailCreatedAtTextView);
            imageView = itemView.findViewById(R.id.mailImageView);
        }
    }
}