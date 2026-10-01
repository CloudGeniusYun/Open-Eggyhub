package com.eggyhub.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 已领取分享码页面
 * 展示用户已领取的分享码列表，并提供复制功能
 */
public class ClaimedShareCodeActivity extends BaseActivity {

    /**
     * 用于展示已领取分享码列表的RecyclerView
     */
    private RecyclerView recyclerViewClaimedShareCodes;
    /**
     * 分享码列表适配器
     */
    private ClaimedShareCodeAdapter adapter;
    /**
     * 已领取分享码数据列表
     */
    private List<ClaimedShareCodeItem> claimedShareCodeList;

    /**
     * Activity创建时调用的方法
     * 初始化UI组件，设置布局和适配器
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claimed_share_code); // 设置布局文件

        // 设置页面标题
        TextView pageTitle = findViewById(R.id.tv_page_title);
        pageTitle.setText("已领取的分享码");

        // 初始化RecyclerView
        recyclerViewClaimedShareCodes = findViewById(R.id.recyclerViewClaimedShareCodes);
        recyclerViewClaimedShareCodes.setLayoutManager(new LinearLayoutManager(this));

        // 初始化数据列表和适配器
        claimedShareCodeList = new ArrayList<>();
        adapter = new ClaimedShareCodeAdapter(claimedShareCodeList, this);
        recyclerViewClaimedShareCodes.setAdapter(adapter);

        // 获取已领取的分享码数据
        fetchClaimedShareCodes();
    }

    /**
     * 获取已领取的分享码数据
     * 从服务器 API 获取用户已领取的分享码列表
     */
    private void fetchClaimedShareCodes() {
        // 获取用户访问令牌
        String accessToken = SecureStorageManager.getAccessToken();

        // 检查令牌是否存在
        if (accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建OkHttpClient实例
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        // 构建请求
        Request request = new Request.Builder()
                .url("https://eggyhub.top/api/gifts/myclaim")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        // 异步执行请求
        client.newCall(request).enqueue(new Callback() {
            /**
             * 请求失败时调用
             * @param call 请求对象
             * @param e 异常信息
             */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ClaimedShareCodeActivity.this, "获取已领取分享码失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            /**
             * 请求成功时调用
             * @param call 请求对象
             * @param response 响应对象
             * @throws IOException IO异常
             */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        String bodyString = responseBody.string();
                        try {
                            // 解析JSON响应
                            Gson gson = new Gson();
                            Type listType = new TypeToken<ArrayList<ClaimedShareCodeItem>>(){}.getType();
                            List<ClaimedShareCodeItem> newItems = gson.fromJson(bodyString, listType);

                            // 更新UI
                            runOnUiThread(() -> {
                                claimedShareCodeList.clear();
                                if (newItems != null) {
                                    claimedShareCodeList.addAll(newItems);
                                }
                                adapter.notifyDataSetChanged();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(ClaimedShareCodeActivity.this, "解析已领取分享码数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(ClaimedShareCodeActivity.this, "获取已领取分享码失败: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }



    /**
     * 已领取分享码列表适配器
     * 用于RecyclerView的数据展示和交互
     */
    public static class ClaimedShareCodeAdapter extends RecyclerView.Adapter<ClaimedShareCodeAdapter.ViewHolder> {

        /**
         * 分享码数据列表
         */
        private List<ClaimedShareCodeItem> items;
        /**
         * 上下文
         */
        private Context context;

        /**
         * 构造函数
         * @param items 分享码数据列表
         * @param context 上下文
         */
        public ClaimedShareCodeAdapter(List<ClaimedShareCodeItem> items, Context context) {
            this.items = items;
            this.context = context;
        }

        /**
         * 创建视图持有者
         * @param parent 父视图组
         * @param viewType 视图类型
         * @return 视图持有者
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_claimed_share_code, parent, false);
            return new ViewHolder(view);
        }

        /**
         * 绑定数据到视图持有者
         * @param holder 视图持有者
         * @param position 位置
         */
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClaimedShareCodeItem item = items.get(position);
            // 设置分享码名称和值
            holder.tvShareCodeName.setText(item.getName());
            holder.tvShareCodeValue.setText(item.getCode());
            // 设置复制按钮点击事件
            holder.btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Share Code", item.getCode());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "分享码已复制", Toast.LENGTH_SHORT).show();
            });
        }

        /**
         * 获取列表项数量
         * @return 列表项数量
         */
        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * 视图持有者类
         * 绑定列表项布局中的UI组件
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * 分享码名称文本视图
             */
            TextView tvShareCodeName;
            /**
             * 分享码值文本视图
             */
            TextView tvShareCodeValue;
            /**
             * 复制按钮
             */
            Button btnCopy;

            /**
             * 构造函数
             * @param itemView 列表项视图
             */
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvShareCodeName = itemView.findViewById(R.id.tv_share_code_name);
                tvShareCodeValue = itemView.findViewById(R.id.tv_share_code_value);
                btnCopy = itemView.findViewById(R.id.btn_copy);
            }
        }
    }
}