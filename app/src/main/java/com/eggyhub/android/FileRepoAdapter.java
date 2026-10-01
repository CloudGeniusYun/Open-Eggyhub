package com.eggyhub.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.eggyhub.android.log.AppLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.util.List;

/**
 * 仓库列表适配器
 * 用于展示仓库列表数据
 */
public class FileRepoAdapter extends RecyclerView.Adapter<FileRepoAdapter.RepoViewHolder> {
    SharedPreferences preferences;
    private String accessToken;
    private Context mContext;
    private List<RepoItem> mRepoList;
    private static final int TIMEOUT = 10; // 超时时间(秒)
    private OkHttpClient mOkHttpClient;
    private Handler mMainHandler;
    private Call mLikeCall;
    private OnItemClickListener mListener;
    

    /**
     * 函数式接口，用于item点击回调
     */
    public interface OnItemClickListener {
        void onItemClick(int repoId, String repoName, String description);
    }

    /**
     * 构造函数
     * @param context 上下文
     * @param repoList 仓库列表数据
     * @param listener 仓库项点击监听器
     */
    public FileRepoAdapter(Context context, List<RepoItem> repoList, OnItemClickListener listener) {        this.mContext = context;
        this.mRepoList = repoList;
        this.mListener = listener;
        // 初始化 SharedPreferences 并获取 accessToken
        this.preferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        this.accessToken = SecureStorageManager.getAccessToken();
        AppLogger.d("FileRepoAdapter", "获取到的 accessToken: " + (accessToken != null ? accessToken : "null"));
        this.mOkHttpClient = OkHttpClientFactory.createCustomClient(TIMEOUT, TIMEOUT, TIMEOUT);
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public RepoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.itemrepo, parent, false);
        return new RepoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RepoViewHolder holder, int position) {
        RepoItem repoItem = mRepoList.get(position);

        // 设置仓库信息
        holder.textViewRepoName.setText("仓库名: " + repoItem.getName());
        holder.textViewRepoDescription.setText("描述: " + (repoItem.getDescription() != null ? repoItem.getDescription() : "无"));
        holder.textViewRepoLikes.setText("点赞: " + repoItem.getLikes());

        // 确保点赞按钮可见
        // 确保点赞布局可见
        holder.dianzanLayout.setVisibility(View.VISIBLE);
        holder.dianzanLayout.setEnabled(true);

        // 设置点赞布局点击事件
        holder.dianzanLayout.setOnClickListener(v -> {
            // 点击后暂时禁用按钮，防止重复点击
            holder.dianzanLayout.setEnabled(false);
            likeRepo(repoItem.getId(), holder);
        });

        // 取消Java中的图片按钮高度设置，直接在xml中设置图片宽度和高度都为20dp

        // 设置item点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(repoItem.getId(), repoItem.getName(), repoItem.getDescription());
            }
        });
    }

    /**
     * 点赞仓库
     * @param repoId 仓库ID
     * @param holder 视图持有者
     */
    private void likeRepo(int repoId, RepoViewHolder holder) {
        // 取消之前的点赞请求
        if (mLikeCall != null && !mLikeCall.isCanceled()) {
            mLikeCall.cancel();
            AppLogger.d("FileRepoAdapter", "取消之前的点赞请求");
        }

        String url = "https://eggyhub.top/api/repos/like?id=" + repoId;
        AppLogger.d("FileRepoAdapter", "点赞请求URL: " + url);

        // 创建请求构建器
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .tag("FileRepoAdapter");

        // 如果accessToken不为null，则添加Authorization头
        if (accessToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            AppLogger.d("FileRepoAdapter", "添加Authorization头: Bearer " + accessToken);
        } else {
            AppLogger.w("FileRepoAdapter", "accessToken为null，未添加Authorization头");
        }

        Request request = requestBuilder.build();

        mLikeCall = mOkHttpClient.newCall(request);
        mLikeCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    AppLogger.d("FileRepoAdapter", "点赞请求已取消");
                    return;
                }

                // 在主线程显示错误信息
                mMainHandler.post(() -> {
                    Toast.makeText(mContext, "点赞失败，请重试", Toast.LENGTH_SHORT).show();
                    // 恢复按钮可点击状态
                    holder.dianzanLayout.setEnabled(true);
                });
                AppLogger.e("FileRepoAdapter", "点赞失败: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) {
                    AppLogger.d("FileRepoAdapter", "点赞请求已取消");
                    if (response.body() != null) {
                        response.body().close();
                    }
                    return;
                }

                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String responseData = responseBody != null ? responseBody.string() : "";
                        AppLogger.d("FileRepoAdapter", "点赞成功，响应数据: " + responseData);

                        // 在主线程更新UI
                        mMainHandler.post(() -> {
                            // 更新点赞数
                            int position = holder.getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                RepoItem repoItem = mRepoList.get(position);
                                int currentLikes = repoItem.getLikes();
                                repoItem.setLikes(currentLikes + 1);
                                AppLogger.d("FileRepoAdapter", "更新点赞数: " + currentLikes + " -> " + repoItem.getLikes());
                                holder.textViewRepoLikes.setText("点赞: " + repoItem.getLikes());
                                Toast.makeText(mContext, "点赞成功", Toast.LENGTH_SHORT).show();
                            }
                            // 恢复按钮可点击状态
                            holder.dianzanLayout.setEnabled(true);
                        });
                    } else {
                        // 在主线程显示错误信息
                        mMainHandler.post(() -> {
                            Toast.makeText(mContext, "已经点过了", Toast.LENGTH_SHORT).show();
                            // 恢复按钮可点击状态
                            holder.dianzanLayout.setEnabled(true);
                        });
                        AppLogger.e("FileRepoAdapter", "点赞失败，状态码: " + response.code());
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRepoList != null ? mRepoList.size() : 0;
    }

    /**
     * 更新数据列表
     * @param repoList 新的仓库列表数据
     */
    public void updateData(List<RepoItem> repoList) {
        this.mRepoList = repoList;
        notifyDataSetChanged();
    }

    /**
     * 视图持有者类
     */
    static class RepoViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRepoName;
        TextView textViewRepoDescription;
        TextView textViewRepoLikes;
        LinearLayout dianzanLayout;
        ImageView dianzanIcon;
        TextView dianzanText;

        public RepoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRepoName = itemView.findViewById(R.id.reponame);
            textViewRepoDescription = itemView.findViewById(R.id.repods);
            textViewRepoLikes = itemView.findViewById(R.id.repozan);
            dianzanLayout = itemView.findViewById(R.id.repodianzan_layout);
            dianzanIcon = itemView.findViewById(R.id.dianzan_icon);
            dianzanText = itemView.findViewById(R.id.repodianzan);
        }
    }
}