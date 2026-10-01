package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("data")
    private Data data;
    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class Data {
        @SerializedName("avatar")
        private String avatar;
        @SerializedName("contact")
        private String contact;
        @SerializedName("description")
        private String description;
        @SerializedName("eggyid")
        private String eggyid;

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getEggyid() { return eggyid; }
        public void setEggyid(String eggyid) { this.eggyid = eggyid; }
    }
}
