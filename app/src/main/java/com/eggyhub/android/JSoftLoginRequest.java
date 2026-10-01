package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class JSoftLoginRequest {
    @SerializedName("user_id")
    private String userId;
    @SerializedName("password")
    private String password;
    @SerializedName("state")
    private String state;

    public JSoftLoginRequest(String userId, String password, String state) {
        this.userId = userId;
        this.password = password;
        this.state = state;
    }
}
