package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UpdateDescriptionRequest {
    @SerializedName("ndes")
    private String description;

    public UpdateDescriptionRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
