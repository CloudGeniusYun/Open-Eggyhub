package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("username")
    private String username;
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;
    @SerializedName("invite")
    private String inviteCode;

    public RegisterRequest(String username, String email, String password, String inviteCode) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.inviteCode = inviteCode;
    }
}
