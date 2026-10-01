package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class OAuthInitResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("state")
    private String state;
    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public String getState() { return state; }
    public String getMessage() { return message; }
}
