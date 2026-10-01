package com.eggyhub.android;

/**
 * 文件数据模型类
 * 用于存储文件的相关信息
 */
public class FileItem {
    private int fileId;
    private String fileSize;
    private int fileSizeKb;
    private String fileType;
    private String originalName;
    private int status;
    private String uploadTime;
    private int repoId;

    /**
     * 构造函数
     */
    public FileItem(int fileId, String fileSize, int fileSizeKb, String fileType, 
                   String originalName, int status, String uploadTime, int repoId) {
        this.fileId = fileId;
        this.fileSize = fileSize;
        this.fileSizeKb = fileSizeKb;
        this.fileType = fileType;
        this.originalName = originalName;
        this.status = status;
        this.uploadTime = uploadTime;
        this.repoId = repoId;
    }

    /**
     * 从FileData对象创建FileItem
     */
    public static FileItem fromFileData(MangerActivity.FileData fileData, int repoId) {
        return new FileItem(
            fileData.getFileId(),
            fileData.getFileSize(),
            fileData.getFileSizeKb(),
            fileData.getFileType(),
            fileData.getOriginalName(),
            fileData.getStatus(),
            fileData.getUploadTime(),
            repoId
        );
    }
    public static FileItem fromFileData(FileActivity.FileData fileData, int repoId) {
        return new FileItem(
                fileData.getFileId(),
                fileData.getFileSize(),
                fileData.getFileSizeKb(),
                fileData.getFileType(),
                fileData.getOriginalName(),
                fileData.getStatus(),
                fileData.getUploadTime(),
                repoId
        );
    }

    // Getter 和 Setter 方法
    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public int getFileSizeKb() {
        return fileSizeKb;
    }

    public void setFileSizeKb(int fileSizeKb) {
        this.fileSizeKb = fileSizeKb;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public int getRepoId() {
        return repoId;
    }

    public void setRepoId(int repoId) {
        this.repoId = repoId;
    }

    /**
     * 获取文件类型对应的图标资源
     * @return 图标资源ID
     */
    public int getFileTypeIcon() {
        if (fileType == null) {
            return R.drawable.ic_file;
        }
        switch (fileType.toLowerCase()) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return R.drawable.ic_image;
            case "mp3":
            case "wav":
                return R.drawable.ic_audio;
            case "txt":
                return R.drawable.ic_text;
            case "lua":
            case "json":
            case "md":
                return R.drawable.ic_code;
            default:
                return R.drawable.ic_file;
        }
    }

    /**
     * 获取下载链接
     * @return 下载链接
     */
    public String getDownloadUrl() {
        return "api/repos/" + repoId + "/" + originalName + "?download=true";
    }

    /**
     * 获取预览链接
     * @return 预览链接
     */
    public String getPreviewUrl() {
        return "api/repos/" + repoId + "/" + originalName;
    }
}