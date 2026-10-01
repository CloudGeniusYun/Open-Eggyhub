package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UpdateNameRequest {
    @SerializedName("new_name")
    private String newName;

    public UpdateNameRequest(String newName) {
        this.newName = newName;
    }

    public String getNewName() { return newName; }
}
