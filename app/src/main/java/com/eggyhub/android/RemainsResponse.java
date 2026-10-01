package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class RemainsResponse {
    @SerializedName("message")
    private int message;

    public int getMessage() {
        return message;
    }

    public void setMessage(int message) {
        this.message = message;
    }
}
