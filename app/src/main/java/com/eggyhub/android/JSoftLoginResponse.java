package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class JSoftLoginResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("code")
    private String code;
    @SerializedName("message")
    private String message;
    @SerializedName("user")
    private User user;

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public User getUser() { return user; }

    public static class User {
        @SerializedName("email")
        private String email;
        @SerializedName("id")
        private int id;
        @SerializedName("user_id")
        private String userId;
        @SerializedName("username")
        private String username;

        public String getEmail() { return email; }
        public int getId() { return id; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
    }
}
