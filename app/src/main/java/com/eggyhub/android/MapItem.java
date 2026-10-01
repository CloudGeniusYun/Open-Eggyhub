package com.eggyhub.android;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

/**
 * 地图项数据模型类
 * 实现了Parcelable接口，支持跨组件数据传递
 * 存储地图相关信息，如名称、编码、介绍、图片等
 */
public class MapItem implements Parcelable {
    /** 地图项唯一标识符 */
    @SerializedName("id")
    private String id;
    /** 地图名称 */
    @SerializedName("name")
    private String name;
    /** 地图编码 */
    @SerializedName("mapCode")
    private String mapCode;
    /** 地图介绍 */
    @SerializedName("intro")
    private String intro;
    /** 地图图片URL */
    @SerializedName("imageUrl")
    private String imageUrl;
    /** 地图创建时间戳 */
    @SerializedName("time")
    private long time;
    /** 地图作者ID */
    @SerializedName("ownerId")
    private String ownerId;
    /** 地图作者名称 */
    @SerializedName("ownerName")
    private String ownerName;
    /** 地图作者头像URL */
    @SerializedName("ownerHead")
    private String ownerHead;
    /** 使用该地图所需的金币数 */
    @SerializedName("coin")
    private int coin;
    /** 地图游玩次数 */
    @SerializedName("playNum")
    private int playNum;
    /** 地图热度值 */
    @SerializedName("hotNum")
    private int hotNum;
    /** 地图分享次数 */
    @SerializedName("shareNum")
    private int shareNum;
    /** 地图投票次数 */
    @SerializedName("voteNum")
    private int voteNum;
    /** VIP用户贡献的热度值 */
    @SerializedName("vipHotNum")
    private int vipHotNum;
    /** 是否使用VIP特权 */
    @SerializedName("useVip")
    private boolean useVip;

    public MapItem() {}

    /**
     * 构造函数
     * @param id 地图项唯一标识符
     * @param name 地图名称
     * @param mapCode 地图编码
     * @param intro 地图介绍
     * @param imageUrl 地图图片URL
     * @param time 地图创建时间戳
     * @param ownerId 地图作者ID
     * @param ownerName 地图作者名称
     * @param ownerHead 地图作者头像URL
     * @param coin 使用该地图所需的金币数
     * @param playNum 地图游玩次数
     * @param hotNum 地图热度值
     * @param shareNum 地图分享次数
     * @param voteNum 地图投票次数
     * @param vipHotNum VIP用户贡献的热度值
     * @param useVip 是否使用VIP特权
     */
    public MapItem(String id, String name, String mapCode, String intro, String imageUrl, long time, String ownerId, String ownerName, String ownerHead, int coin, int playNum, int hotNum, int shareNum, int voteNum, int vipHotNum, boolean useVip) {
        this.id = id;
        this.name = name;
        this.mapCode = mapCode;
        this.intro = intro;
        this.imageUrl = imageUrl;
        this.time = time;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerHead = ownerHead;
        this.coin = coin;
        this.playNum = playNum;
        this.hotNum = hotNum;
        this.shareNum = shareNum;
        this.voteNum = voteNum;
        this.vipHotNum = vipHotNum;
        this.useVip = useVip;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMapCode() { return mapCode; }
    public String getIntro() { return intro; }
    public String getImageUrl() { return imageUrl; }
    public long getTime() { return time; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerHead() { return ownerHead; }
    public int getCoin() { return coin; }
    public int getPlayNum() { return playNum; }
    public int getHotNum() { return hotNum; }
    public int getShareNum() { return shareNum; }
    public int getVoteNum() { return voteNum; }
    public int getVipHotNum() { return vipHotNum; }
    public boolean isUseVip() { return useVip; }

    protected MapItem(Parcel in) {
        id = in.readString();
        name = in.readString();
        mapCode = in.readString();
        intro = in.readString();
        imageUrl = in.readString();
        time = in.readLong();
        ownerId = in.readString();
        ownerName = in.readString();
        ownerHead = in.readString();
        coin = in.readInt();
        playNum = in.readInt();
        hotNum = in.readInt();
        shareNum = in.readInt();
        voteNum = in.readInt();
        vipHotNum = in.readInt();
        useVip = in.readByte() != 0;
    }

    public static final Creator<MapItem> CREATOR = new Creator<MapItem>() {
        @Override
        public MapItem createFromParcel(Parcel in) {
            return new MapItem(in);
        }

        @Override
        public MapItem[] newArray(int size) {
            return new MapItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(mapCode);
        dest.writeString(intro);
        dest.writeString(imageUrl);
        dest.writeLong(time);
        dest.writeString(ownerId);
        dest.writeString(ownerName);
        dest.writeString(ownerHead);
        dest.writeInt(coin);
        dest.writeInt(playNum);
        dest.writeInt(hotNum);
        dest.writeInt(shareNum);
        dest.writeInt(voteNum);
        dest.writeInt(vipHotNum);
        dest.writeByte((byte) (useVip ? 1 : 0));
    }

    // Getter方法

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setMapCode(String mapCode) { this.mapCode = mapCode; }
    public void setIntro(String intro) { this.intro = intro; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTime(long time) { this.time = time; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setOwnerHead(String ownerHead) { this.ownerHead = ownerHead; }
    public void setCoin(int coin) { this.coin = coin; }
    public void setPlayNum(int playNum) { this.playNum = playNum; }
    public void setHotNum(int hotNum) { this.hotNum = hotNum; }
    public void setShareNum(int shareNum) { this.shareNum = shareNum; }
    public void setVoteNum(int voteNum) { this.voteNum = voteNum; }
    public void setVipHotNum(int vipHotNum) { this.vipHotNum = vipHotNum; }
    public void setUseVip(boolean useVip) { this.useVip = useVip; }
}
