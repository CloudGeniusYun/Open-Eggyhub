package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class DeleteAccountRequest {
    @SerializedName("password")
    private String password;

    public DeleteAccountRequest(String password) {
        this.password = password;
    }

    public String getPassword() { return password; }
}
