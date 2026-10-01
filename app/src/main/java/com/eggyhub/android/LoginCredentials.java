package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class LoginCredentials {
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;
    @SerializedName("timestamp")
    private double timestamp;

    public LoginCredentials(String email, String password, double timestamp) {
        this.email = email;
        this.password = password;
        this.timestamp = timestamp;
    }
}
