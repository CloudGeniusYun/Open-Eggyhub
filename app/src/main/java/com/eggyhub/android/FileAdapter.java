package com.eggyhub.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * 文件列表适配器
 * 用于展示文件列表数据
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private Context mContext;
    private List<FileItem> mFileList;
    private OnFileActionListener mListener;

    /**
     * 构造函数
     * @param context 上下文
     * @param fileList 文件列表数据
     */
    public FileAdapter(Context context, List<FileItem> fileList) {
        this.mContext = context;
        this.mFileList = fileList;
    }

    /**
     * 设置文件操作监听器
     * @param listener 监听器
     */
    public void setOnFileActionListener(OnFileActionListener listener) {
        this.mListener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = mFileList.get(position);

        // 设置文件信息
        holder.textViewFileName.setText(fileItem.getOriginalName());
        holder.textViewFileSize.setText("文件大小: " + fileItem.getFileSize());
        holder.textViewUploadTime.setText("上传时间: " + fileItem.getUploadTime());

        // 判断文件类型，如果是图片则加载图片预览，否则显示文件类型图标
        String fileType = fileItem.getFileType();
        if (fileType != null && (fileType.equalsIgnoreCase("jpg") || fileType.equalsIgnoreCase("jpeg") || 
            fileType.equalsIgnoreCase("png") || fileType.equalsIgnoreCase("gif"))) {
            // 使用Glide加载图片预览，添加完整URL前缀
            String previewUrl = "https://eggyhub.top/" + fileItem.getPreviewUrl();
            Glide.with(mContext)
                 .load(previewUrl)
                 .placeholder(fileItem.getFileTypeIcon()) // 加载中显示文件类型图标
                 .error(fileItem.getFileTypeIcon())       // 加载失败显示文件类型图标
                 .into(holder.imageViewFileType);
        } else {
            // 非图片类型，显示文件类型图标
            holder.imageViewFileType.setImageResource(fileItem.getFileTypeIcon());
        }

        // 设置预览按钮点击事件
        holder.buttonPreview.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPreviewClick(fileItem);
            }
        });

        // 设置下载按钮点击事件
        holder.buttonDownload.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onDownloadClick(fileItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFileList != null ? mFileList.size() : 0;
    }

    /**
     * 更新数据列表
     * @param fileList 新的文件列表数据
     */
    public void updateData(List<FileItem> fileList) {
        this.mFileList = fileList;
        notifyDataSetChanged();
    }

    /**
     * 视图持有者类
     */
    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewFileType;
        TextView textViewFileName;
        TextView textViewFileSize;
        TextView textViewUploadTime;
        Button buttonPreview;
        Button buttonDownload;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewFileType = itemView.findViewById(R.id.imageViewFileType);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
            textViewFileSize = itemView.findViewById(R.id.textViewFileSize);
            textViewUploadTime = itemView.findViewById(R.id.textViewUploadTime);
            buttonPreview = itemView.findViewById(R.id.buttonPreview);
            buttonDownload = itemView.findViewById(R.id.buttonDownload);
        }
    }

    /**
     * 文件操作监听器接口
     */
    public interface OnFileActionListener {
        void onPreviewClick(FileItem fileItem);
        void onDownloadClick(FileItem fileItem);
    }
}