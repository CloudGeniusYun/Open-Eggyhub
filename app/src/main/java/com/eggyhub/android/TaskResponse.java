package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TaskResponse {
    @SerializedName("tasks")
    private List<TaskData> tasks;

    public List<TaskData> getTasks() {
        return tasks;
    }

    public static class TaskData {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("description")
        private String description;
        @SerializedName("max_times")
        private int maxTimes;
        @SerializedName("refresh")
        private String refresh;
        @SerializedName("reward")
        private int reward;
        @SerializedName("user")
        private UserProgress user;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getMaxTimes() { return maxTimes; }
        public String getRefresh() { return refresh; }
        public int getReward() { return reward; }
        public UserProgress getUser() { return user; }
    }

    public static class UserProgress {
        @SerializedName("claimed")
        private int claimed;
        @SerializedName("completed")
        private int completed;

        public int getClaimed() { return claimed; }
        public int getCompleted() { return completed; }
    }
}
