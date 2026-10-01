package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() { return message; }
}
