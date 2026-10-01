package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

/**
 * 仓库数据模型类
 * 用于存储从API获取的仓库信息
 */
public class RepoItem {
    private int id;
    private String name;
    private String description;
    private int likes;
    @SerializedName("file_count")
    private int fileCount;
    private String username;

    /**
     * 获取仓库ID
     * @return 仓库ID
     */
    public int getId() {
        return id;
    }

    /**
     * 设置仓库ID
     * @param id 仓库ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 获取仓库名称
     * @return 仓库名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置仓库名称
     * @param name 仓库名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取仓库描述
     * @return 仓库描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置仓库描述
     * @param description 仓库描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取点赞数
     * @return 点赞数
     */
    public int getLikes() {
        return likes;
    }

    /**
     * 设置点赞数
     * @param likes 点赞数
     */
    public void setLikes(int likes) {
        this.likes = likes;
    }

    /**
     * 获取文件数量
     * @return 文件数量
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * 设置文件数量
     * @param fileCount 文件数量
     */
    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }
}