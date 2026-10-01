package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UpdateNameResponse {
    @SerializedName("new_username")
    private String newUsername;
    @SerializedName("message")
    private String message;
    @SerializedName("status")
    private String status;

    public String getNewUsername() { return newUsername; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
}
