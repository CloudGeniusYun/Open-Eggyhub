package com.eggyhub.android;

/**
 * 分类数据模型
 * 用于存储分类的ID和名称信息
 */
public class CategoryItem {
    /**
     * 分类ID
     */
    private int id;
    /**
     * 分类名称
     */
    private String name;

    /**
     * 构造函数
     * @param id 分类ID
     * @param name 分类名称
     */
    public CategoryItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 获取分类ID
     * @return 分类ID
     */
    public int getId() {
        return id;
    }

    /**
     * 获取分类名称
     * @return 分类名称
     */
    public String getName() {
        return name;
    }
}