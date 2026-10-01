package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

/**
 * 视频数据模型类
 * 用于存储视频的相关信息，如名称、封面、播放链接等
 */
public class VideoItem {
    /**
     * 视频封面图片URL
     */
    @SerializedName("cover")
    private String cover;
    /**
     * 视频描述
     */
    @SerializedName("description")
    private String description;
    /**
     * 视频ID
     */
    @SerializedName("id")
    private int id;
    /**
     * 视频播放链接
     */
    @SerializedName("link")
    private String link;
    /**
     * 视频名称
     */
    @SerializedName("name")
    private String name;
    /**
     * 视频提供商
     */
    @SerializedName("provider")
    private String provider;
    /**
     * 视频状态
     * 0: 正常
     * 1: 下架
     * 2: 审核中
     */
    @SerializedName("status")
    private int status;

    /**
     * 获取视频封面图片URL
     * @return 视频封面图片URL
     */
    public String getCover() {
        return cover;
    }

    /**
     * 设置视频封面图片URL
     * @param cover 视频封面图片URL
     */
    public void setCover(String cover) {
        this.cover = cover;
    }

    /**
     * 获取视频描述
     * @return 视频描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置视频描述
     * @param description 视频描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取视频ID
     * @return 视频ID
     */
    public int getId() {
        return id;
    }

    /**
     * 设置视频ID
     * @param id 视频ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 获取视频播放链接
     * @return 视频播放链接
     */
    public String getLink() {
        return link;
    }

    /**
     * 设置视频播放链接
     * @param link 视频播放链接
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * 获取视频名称
     * @return 视频名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置视频名称
     * @param name 视频名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取视频提供商
     * @return 视频提供商
     */
    public String getProvider() {
        return provider;
    }

    /**
     * 设置视频提供商
     * @param provider 视频提供商
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * 获取视频状态
     * @return 视频状态
     * 0: 正常
     * 1: 下架
     * 2: 审核中
     */
    public int getStatus() {
        return status;
    }

    /**
     * 设置视频状态
     * @param status 视频状态
     * 0: 正常
     * 1: 下架
     * 2: 审核中
     */
    public void setStatus(int status) {
        this.status = status;
    }
}