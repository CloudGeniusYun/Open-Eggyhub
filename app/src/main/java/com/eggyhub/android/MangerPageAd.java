package com.eggyhub.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

public class MangerPageAd extends RecyclerView.Adapter<MangerPageAd.ViewHolder> {
    String con;
    Context mAppContext;


    private List<ArticleItem> articleList;
    private Context context;

    // 构造函数，接收文章数据
    public MangerPageAd(List<ArticleItem> articleList,Context context) {
        this.articleList = articleList != null ? articleList : new java.util.ArrayList<>();
        mAppContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArticleItem article = articleList.get(position);


        holder.textViewTitle.setText(article.getTitle());

        holder.textViewAuthor.setText("id：" + article.getId());

        holder.textViewCategory.setText(article.getDate());

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            // 日志输出点击的文章数据
            AppLogger.d("MangerPageAd", "点击文章: " + article.getTitle());
            AppLogger.d("MangerPageAd", "文章ID: " + article.getId());
            
            // 创建跳转到CreateArticleActivity的Intent
            Intent intent = new Intent(context, CreateArticleActivity.class);
            // 将文章数据原封不动地传递
            Bundle bundle = new Bundle();
            bundle.putInt("articleId", article.getId());
            bundle.putString("articleTitle", article.getTitle());
            bundle.putString("articleDate", article.getDate());
            
            Toast.makeText(mAppContext, "加载中，请稍等", Toast.LENGTH_SHORT).show();
            
            // 异步获取文章内容
            getArticleData(article.getId(), data -> {
                if (data != null && !data.equals("失败")) {
                    bundle.putString("articleContent", data);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(mAppContext, "加载文章内容失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }


    public void updateData(List<ArticleItem> newArticleList) {
        articleList.clear();
        articleList.addAll(newArticleList != null ? newArticleList : new java.util.ArrayList<>());
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView textViewTitle;   // 文章标题TextView
        TextView textViewAuthor;  // 文章作者TextView
        TextView textViewCategory; // 文章分类TextView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化视图组件
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewCategory = itemView.findViewById(R.id.textViewCategory);
        }
    }

    // 定义回调接口
    private interface OnArticleDataCallback {
        void onDataReceived(String data);
    }

    private void getArticleData(int acid, final OnArticleDataCallback callback){
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/article?id="+acid;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 在UI线程执行回调
                ((android.app.Activity)context).runOnUiThread(() -> {
                    callback.onDataReceived("失败");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    final String result;
                    if (response.isSuccessful() && responseBody != null) {
                        result = responseBody.string();
                    } else {
                        result = "失败";
                    }
                    // 在UI线程执行回调
                    ((android.app.Activity)context).runOnUiThread(() -> {
                        callback.onDataReceived(result);
                    });
                }
            }
        });
    }
}
