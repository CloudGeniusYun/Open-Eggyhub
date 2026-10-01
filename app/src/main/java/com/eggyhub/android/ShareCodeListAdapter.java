package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ShareCodeListAdapter extends RecyclerView.Adapter<ShareCodeListAdapter.ViewHolder> {

    private List<DisplayShareCodeItem> shareCodeList;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int id, String code);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public ShareCodeListAdapter(List<DisplayShareCodeItem> shareCodeList) {
        this.shareCodeList = shareCodeList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_share_code, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisplayShareCodeItem item = shareCodeList.get(position);
        holder.textViewShareCode.setText(item.getCode());
        holder.buttonDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(item.getId(), item.getCode());
            }
        });
    }

    @Override
    public int getItemCount() {
        return shareCodeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewShareCode;
        TextView buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewShareCode = itemView.findViewById(R.id.textViewShareCode);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }

    public void updateList(List<DisplayShareCodeItem> newList) {
        this.shareCodeList = newList;
        notifyDataSetChanged();
    }
}