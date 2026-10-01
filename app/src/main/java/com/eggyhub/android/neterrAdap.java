package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class neterrAdap extends RecyclerView.Adapter<neterrAdap.errViewHolder>{
    private String str;

    public neterrAdap(String str) {
        this.str =str;
    }

    @NonNull
    @Override
    public errViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View V = LayoutInflater.from(parent.getContext()).inflate(R.layout.network_err_layout,parent, false);
        return new errViewHolder(V);
    }

    @Override
    public void onBindViewHolder(@NonNull errViewHolder holder, int position) {
        holder.tx.setText(this.str);

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class errViewHolder extends RecyclerView.ViewHolder {
        TextView tx;

        public errViewHolder(@NonNull View itemView) {
            super(itemView);

            this.tx = itemView.findViewById(R.id.neterrtext);
        }
    }
}
