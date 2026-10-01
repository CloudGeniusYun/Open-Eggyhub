package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ContentViewHolder> {

    private final List<ContentItem> contentItems;

    public ContentAdapter(List<ContentItem> contentItems) {
        this.contentItems = contentItems;
    }

    @NonNull
    @Override
    public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_content_card, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentViewHolder holder, int position) {
        ContentItem item = contentItems.get(position);
        holder.textViewTitle.setText(item.getTitle());
    }

    @Override
    public int getItemCount() {
        return contentItems.size();
    }

    static class ContentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;

        ContentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewCardTitle);
        }
    }
}