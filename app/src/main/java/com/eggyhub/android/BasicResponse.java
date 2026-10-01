package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class BasicResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
