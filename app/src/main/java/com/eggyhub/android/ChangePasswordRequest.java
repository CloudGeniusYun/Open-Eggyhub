package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {
    @SerializedName("old_password")
    private String oldPassword;
    @SerializedName("new_password")
    private String newPassword;

    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() { return oldPassword; }
    public String getNewPassword() { return newPassword; }
}
