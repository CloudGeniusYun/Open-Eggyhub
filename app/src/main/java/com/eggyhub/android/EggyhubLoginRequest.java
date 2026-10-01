package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class EggyhubLoginRequest {
    @SerializedName("state")
    private String state;
    @SerializedName("code")
    private String code;
    @SerializedName("email")
    private String email;
    @SerializedName("id")
    private int id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("password")
    private String password;
    @SerializedName("username")
    private String username;

    public EggyhubLoginRequest(String state, String code, String email, int id, String userId, String password, String username) {
        this.state = state;
        this.code = code;
        this.email = email;
        this.id = id;
        this.userId = userId;
        this.password = password;
        this.username = username;
    }
}
