package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

/**
 * 视频分类类，用于保存分类信息
 */
public class VideoCategory {
    @SerializedName("id")
    private int id; // 分类ID

    @SerializedName("name")
    private String name; // 分类名称

    private boolean isSelected; // 是否选中

    public VideoCategory() {
    }

    /**
     * 构造函数
     * @param id 分类ID
     * @param name 分类名称
     */
    public VideoCategory(int id, String name) {
        this.id = id;
        this.name = name;
        this.isSelected = false;
    }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public int getId() { return id; }
    public String getName() { return name; }
    
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}
