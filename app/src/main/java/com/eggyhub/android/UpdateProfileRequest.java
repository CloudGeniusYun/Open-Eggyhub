package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {
    @SerializedName("eggyid")
    private String eggyId;
    @SerializedName("description")
    private String description;
    @SerializedName("contact")
    private String contact;

    public UpdateProfileRequest(String eggyId, String description, String contact) {
        this.eggyId = eggyId;
        this.description = description;
        this.contact = contact;
    }

    public String getEggyId() { return eggyId; }
    public String getDescription() { return description; }
    public String getContact() { return contact; }
}
