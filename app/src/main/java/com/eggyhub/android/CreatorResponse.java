package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class CreatorResponse {
    @SerializedName("username")
    private String username;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("published_gifts")
    private int publishedGifts;
    @SerializedName("total_likes")
    private String totalLikes;
    @SerializedName("contributed_codes")
    private int contributedCodes;
    @SerializedName("claimed_codes")
    private int claimedCodes;
    @SerializedName("top_claimed_gift")
    private String topClaimedGift;

    public String getUsername() { return username; }
    public int getUserId() { return userId; }
    public int getPublishedGifts() { return publishedGifts; }
    public String getTotalLikes() { return totalLikes; }
    public int getContributedCodes() { return contributedCodes; }
    public int getClaimedCodes() { return claimedCodes; }
    public String getTopClaimedGift() { return topClaimedGift; }
}
