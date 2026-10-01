package com.eggyhub.android;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.eggyhub.android.MailListResponse;
import com.eggyhub.android.MailData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.eggyhub.android.utils.OkHttpClientFactory;

import android.app.AlertDialog;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

/**
 * 邮箱Activity
 * 用于展示和处理用户邮件
 */
public class MailActivity extends BaseActivity implements MailAdapter.OnItemClickListener {
    // 日志标签
    private static final String TAG = "MailActivity";
    // 超时时间(秒)
    private static final int TIMEOUT = 10;
    // API基础URL
    private static final String BASE_URL = "https://eggyhub.top/api";
    // 邮件列表API路径
    private static final String MAIL_LIST_URL = BASE_URL + "/mails?page=1&per_page=50";

    // 成员变量
    private RecyclerView mMailRecyclerView;     // 邮件列表RecyclerView
    private MailAdapter mMailAdapter;           // 邮件列表适配器
    private List<MailItem> mMailList;           // 邮件数据列表
    private OkHttpClient mOkHttpClient;         // OkHttp客户端
    private Call mMailCall;                     // 邮件数据请求
    private ImageButton mBackButton;            // 返回按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail);

        initViews();
        initData();
        setupListeners();
        fetchMails();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        mBackButton = findViewById(R.id.back_button);
        mMailRecyclerView = findViewById(R.id.mail_recycler_view);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 初始化OkHttpClient（使用工厂类，自动添加代理拦截器）
        mOkHttpClient = OkHttpClientFactory.createCustomClient(TIMEOUT, TIMEOUT, TIMEOUT);

        // 初始化邮件列表和适配器
        mMailList = new ArrayList<>();
        mMailAdapter = new MailAdapter(mMailList, this);

        // 初始化RecyclerView
        mMailRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMailRecyclerView.setAdapter(mMailAdapter);
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 设置返回按钮点击事件
        mBackButton.setOnClickListener(v -> onBackPressed());
    }

    /**
     * 获取邮件数据
     */
    private void fetchMails() {
        // 获取本地存储的 accessToken
        String accessToken = SecureStorageManager.getAccessToken();

        Request.Builder requestBuilder = new Request.Builder()
                .url(MAIL_LIST_URL)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken);

        Request request = requestBuilder.build();

        mMailCall = mOkHttpClient.newCall(request);
        mMailCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) {
                    return;
                }
                runOnUiThread(() -> Toast.makeText(MailActivity.this, "网络异常: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (call.isCanceled()) {
                        return;
                    }

                    if (response.isSuccessful() && responseBody != null) {
                        final String responseData = responseBody.string();
                        runOnUiThread(() -> {
                            try {
                                parseMailResponse(responseData);
                            } catch (Exception e) {
                                Toast.makeText(MailActivity.this, "数据解析错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(MailActivity.this, "请求失败: " + response.code(), Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    /**
     * 解析邮件响应数据
     * @param responseData 响应数据字符串
     */
    private void parseMailResponse(String responseData) {
        Gson gson = new Gson();
        MailListResponse response = gson.fromJson(responseData, MailListResponse.class);
        if (response == null) return;
        List<MailData> dataList = response.getData();
        List<MailItem> newMailList = new ArrayList<>();

        if (dataList == null) return;

        // 获取本地用户名
        String localUsername = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", "");
        if (localUsername == null) localUsername = "";

        for (MailData mailData : dataList) {
            String title = mailData.getTitle();
            String content = mailData.getContent();
            String createdAt = mailData.getCreatedAt();
            boolean isSent = mailData.isSent();
            int senderId = mailData.getSenderId();
            int targetId = mailData.getTargetId();
            int status = mailData.getStatus();

            // 根据isSent和status设置from/to和图片
            String from = "";
            String to = "";
            String imageRes = ""; // Placeholder for image resource name

            if (isSent) {
                from = localUsername;
                to = "用户" + targetId;
                imageRes = "send"; // send.webp
            } else {
                to = localUsername;
                from = "用户" + senderId;
                if (status == 2) {
                    imageRes = "unread"; // unread.webp
                } else if (status == 1) {
                    imageRes = "readed"; // readed.webp
                }
            }

            // 转换内容为中文
            String chineseContent = convertUnicodeToChinese(content);

            // 格式化时间
            String formattedCreatedAt = createdAt != null ? createdAt.replace("T", " ") : "";

            newMailList.add(new MailItem(title, chineseContent, from, to, formattedCreatedAt, imageRes));
        }

        updateMailList(newMailList);
    }

    /**
     * 更新邮件列表
     * @param newMailList 新的邮件列表
     */
    private void updateMailList(List<MailItem> newMailList) {
        mMailList.clear();
        mMailList.addAll(newMailList);
        mMailAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(MailItem mailItem) {
        showMailDetailDialog(mailItem);
    }

    /**
     * 显示邮件详情对话框
     * @param mailItem 邮件项
     */
    private void showMailDetailDialog(MailItem mailItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mailItem.getTitle());

        // 创建LinearLayout来容纳内容
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        // 发件人TextView
        TextView fromTextView = new TextView(this);
        fromTextView.setText("发件人: " + mailItem.getFrom());
        fromTextView.setTextSize(16);
        layout.addView(fromTextView);

        // 收件人TextView
        TextView toTextView = new TextView(this);
        toTextView.setText("收件人: " + mailItem.getTo());
        toTextView.setTextSize(16);
        layout.addView(toTextView);

        // 时间TextView
        TextView timeTextView = new TextView(this);
        timeTextView.setText("时间: " + mailItem.getCreatedAt());
        timeTextView.setTextSize(16);
        layout.addView(timeTextView);

        // 内容TextView
        TextView contentTextView = new TextView(this);
        contentTextView.setText("\n内容:\n" + mailItem.getContent());
        contentTextView.setTextSize(16);
        layout.addView(contentTextView);

        // 添加ScrollView以支持长内容滚动
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("关闭", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置对话框宽度匹配父容器
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
    }

    /**
     * 将Unicode转义序列转换为中文字符
     * @param unicode Unicode字符串
     * @return 转换后的中文字符串
     */
    private String convertUnicodeToChinese(String unicode) {
        if (unicode == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = -1;
        int pos = 0;

        while ((i = unicode.indexOf("\\u", pos)) != -1) {
            sb.append(unicode.substring(pos, i));
            if (i + 5 < unicode.length()) {
                pos = i + 6;
                String hex = unicode.substring(i + 2, i + 6);
                try {
                    char ch = (char) Integer.parseInt(hex, 16);
                    sb.append(ch);
                } catch (NumberFormatException e) {
                    // 如果不是有效的Unicode转义，直接追加原始序列
                    sb.append(unicode.substring(i, pos));
                }
            } else {
                // 如果序列不完整，追加字符串的其余部分
                sb.append(unicode.substring(i));
                pos = unicode.length();
            }
        }
        // 追加最后一个Unicode序列之后的字符串部分
        sb.append(unicode.substring(pos, unicode.length()));
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消网络请求
        if (mMailCall != null && !mMailCall.isCanceled()) {
            mMailCall.cancel();
        }
    }
}