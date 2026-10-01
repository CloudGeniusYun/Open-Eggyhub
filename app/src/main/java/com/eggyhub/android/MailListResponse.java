package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MailListResponse {
    @SerializedName("data")
    private List<MailData> data;

    public List<MailData> getData() {
        return data;
    }
}
