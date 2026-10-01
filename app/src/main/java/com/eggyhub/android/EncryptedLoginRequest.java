package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class EncryptedLoginRequest {
    @SerializedName("auth")
    private String auth;

    public EncryptedLoginRequest(String auth) {
        this.auth = auth;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
}
