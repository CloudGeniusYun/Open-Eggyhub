package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("user")
    private User user;
    @SerializedName("message")
    private String message;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class User {
        @SerializedName("username")
        private String username;
        @SerializedName("id")
        private int id;
        @SerializedName("email")
        private String email;
        @SerializedName("role")
        private String role;
        @SerializedName("sponser")
        private String sponser;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getSponser() { return sponser; }
        public void setSponser(String sponser) { this.sponser = sponser; }
    }
}
