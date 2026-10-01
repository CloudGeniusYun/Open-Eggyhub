package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class TaskItem {
    @SerializedName("id")
    private int taskId;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("reward")
    private int reward;
    @SerializedName("status")
    private String status;
    
    // Note: canClaim is logic-derived in original code, but if it comes from JSON or needs to be serialized, we might need it.
    // Looking at TaskActivity.java: 
    // boolean canClaim = false;
    // if (completed >= maxTimes) ... canClaim = (claimed < completed);
    // So canClaim is NOT directly from JSON. It is calculated.
    // However, if we want to use Gson to parse the raw JSON, we should match the JSON structure.
    // The JSON has "tasks" array. Each task has: description, id, max_times, name, refresh, reward, user (object).
    // The "user" object has: claimed, completed.
    // So TaskItem structure in JSON is different from this flat TaskItem class.
    // I need to be careful here. 
    // The original manual parsing extracts fields from "user" object and flattens them into TaskItem.
    // If I use Gson, I should either:
    // 1. Create a nested structure matching JSON (TaskResponse -> List<TaskJsonItem> -> UserProgress) and then map to TaskItem.
    // 2. Or modify TaskItem to match JSON structure (not easy because JSON is nested).
    // 3. Or use a custom deserializer (too complex).
    
    // Let's create a new model class that matches the JSON structure exactly, say TaskResponseItem, 
    // and then convert it to TaskItem for the UI, OR update TaskItem to match JSON if possible.
    // But TaskItem is used in Adapter, so changing it might break UI code.
    // Better approach: Create a temporary DTO (Data Transfer Object) class for JSON parsing, 
    // then convert to TaskItem.
    
    // Wait, let me just update TaskItem to have the fields from JSON, and handle the nested "user" object via a static inner class or separate class.
    // Actually, looking at TaskActivity.java again:
    // It manually parses "user" object.
    // I should create a `TaskDto` class that matches the JSON, and then a mapper method.
    
    // Let's hold on updating TaskItem.java directly with @SerializedName if the structure doesn't match.
    // The structure:
    // {
    //   "id": ...,
    //   "name": ...,
    //   "user": { "claimed": ..., "completed": ... }
    // }
    // TaskItem has: taskId, name, claimed, current_count (completed).
    
    // So I will create `TaskDto.java` instead.
    
    private boolean canClaim;
    @SerializedName("max_times")
    private int total_count;
    @SerializedName("completed") // This is from user object in JSON
    private int current_count;
    @SerializedName("claimed") // This is from user object in JSON
    private int claimed;
    @SerializedName("refresh")
    private String refresh;

    public TaskItem() {}

    public TaskItem(int taskId, String name, String description, int reward, String status, boolean canClaim, int total_count, int current_count, int claimed, String refresh) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.reward = reward;
        this.status = status;
        this.canClaim = canClaim;
        this.total_count = total_count;
        this.current_count = current_count;
        this.claimed = claimed;
        this.refresh = refresh;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getReward() {
        return reward;
    }

    public String getStatus() {
        return status;
    }

    public boolean canClaim() {
        return canClaim;
    }

    public void setCanClaim(boolean canClaim) {
        this.canClaim = canClaim;
    }

    public int getTotal_count() {
        return total_count;
    }

    public int getCurrent_count() {
        return current_count;
    }

    public int getClaimed() {
        return claimed;
    }

    public String getRefresh() {
        return refresh;
    }
    
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setReward(int reward) { this.reward = reward; }
    public void setStatus(String status) { this.status = status; }
    public void setTotal_count(int total_count) { this.total_count = total_count; }
    public void setCurrent_count(int current_count) { this.current_count = current_count; }
    public void setClaimed(int claimed) { this.claimed = claimed; }
    public void setRefresh(String refresh) { this.refresh = refresh; }
}
