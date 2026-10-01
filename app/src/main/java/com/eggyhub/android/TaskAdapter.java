package com.eggyhub.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<TaskItem> taskItems;

    public TaskAdapter(List<TaskItem> taskItems) {
        this.taskItems = taskItems;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem item = taskItems.get(position);
        holder.textViewName.setText(item.getName());
        holder.textViewDescription.setText(item.getDescription());
        holder.textViewReward.setText(String.valueOf(item.getReward()));
        holder.textViewCount.setText("完成次数: " + item.getCurrent_count() + "/" + item.getTotal_count());
        
        // 格式化刷新周期显示：0 显示“永久”，其他数字加“天”
        String refreshText = item.getRefresh();
        String displayRefresh;
        if ("0".equals(refreshText)) {
            displayRefresh = "永久";
        } else {
            displayRefresh = refreshText + "天";
        }
        holder.textViewRefresh.setText("刷新周期: " + displayRefresh);

        // 设置领取按钮状态
        if (item.canClaim()) { // 如果任务可领取
            holder.buttonClaim.setText("领取奖励");
            holder.buttonClaim.setEnabled(true);
            holder.buttonClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0096FF));
            holder.buttonClaim.setTextColor(Color.WHITE);
            holder.buttonClaim.setOnClickListener(v -> {
                if (holder.itemView.getContext() instanceof TaskActivity) {
                    ((TaskActivity) holder.itemView.getContext()).claimTask(item.getTaskId(), item.getCurrent_count(), item.getClaimed());
                }
            });
        } else if (item.getStatus().equals("已完成") && item.getClaimed() >= item.getCurrent_count()) { // 已完成且已领取
            holder.buttonClaim.setText("已完成");
            holder.buttonClaim.setEnabled(false);
            holder.buttonClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
            holder.buttonClaim.setTextColor(0xFF888888);
            holder.buttonClaim.setOnClickListener(null);
        } else { // 其他不可领取状态（未完成等）
            holder.buttonClaim.setText("未完成"); // 或者根据实际状态显示“进行中”等
            holder.buttonClaim.setEnabled(false);
            holder.buttonClaim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
            holder.buttonClaim.setTextColor(0xFF888888);
            holder.buttonClaim.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return taskItems.size();
    }

    public void updateTasks(List<TaskItem> newTasks) {
        this.taskItems.clear();
        this.taskItems.addAll(newTasks);
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDescription;
        TextView textViewReward;
        TextView textViewStatus;
        TextView textViewCount;
        TextView textViewRefresh;
        MaterialButton buttonClaim;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewTaskName);
            textViewDescription = itemView.findViewById(R.id.textViewTaskDescription);
            textViewReward = itemView.findViewById(R.id.textViewTaskReward);
            textViewStatus = itemView.findViewById(R.id.textViewTaskStatus);
            textViewCount = itemView.findViewById(R.id.textViewTaskCount);
            textViewRefresh = itemView.findViewById(R.id.textViewTaskRefresh);
            buttonClaim = itemView.findViewById(R.id.buttonClaimTask);
        }
    }
}