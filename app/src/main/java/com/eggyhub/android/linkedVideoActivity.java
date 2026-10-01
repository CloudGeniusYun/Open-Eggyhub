package com.eggyhub.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import com.eggyhub.android.log.AppLogger;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.eggyhub.android.VideoUploadInfo;
import com.eggyhub.android.AvatarUploadResponse;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.eggyhub.android.utils.OkHttpClientFactory;

/**{"name":","cover":"","description":"111","stock":1,"link":"BV1omhGzoE2k"}
 * 关联视频活动
 * 用于上传视频信息和封面图片
 */
public class linkedVideoActivity extends BaseActivity {
    TextView upphoto;
    SharedPreferences preferences;
    String accessToken;// 获取访问令牌
    private static final int PICK_IMAGE_REQUEST = 1;
    String fileName;
    Button fabu;
    EditText name;
    EditText bv;
    EditText ds;
    String sname;
    String sbv;
    String sds;
    String cover;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linked_video);
        upphoto = findViewById(R.id.clickupdateText);
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        accessToken = SecureStorageManager.getAccessToken();
        name = findViewById(R.id.Videoname);
        bv = findViewById(R.id.Videobv);
        ds = findViewById(R.id.Videods);


        // 初始化返回按钮
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前Activity，返回上一页
            }
        });

        LinearLayout videoupdataLayout = findViewById(R.id.Videoupdata);
         fabu = findViewById(R.id.Videofabu);


        videoupdataLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openImageChooser();
            }
        });
        fabu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage(imageUri);

            }
        });

    }

    /**
     * 打开图片选择器
     */
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * 处理图片选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            imageUri = data.getData();
            fileName = getFileName(imageUri);
            upphoto.setText(fileName);


            Toast.makeText(this, "图片已选择", Toast.LENGTH_SHORT).show();


        }
    }

    /**
     * 将图片添加到请求中并上传
     * @param imageUri 图片URI
     */
    private void uploadImage(Uri imageUri) {
        try {
            sname = name.getText().toString();
            sbv = bv.getText().toString();
            sds = ds.getText().toString();
            if (sname.equals("") || sds.equals("") || sbv.equals("") || imageUri ==null){
                Toast.makeText(this, "数据不能为空", Toast.LENGTH_SHORT).show();
            }
            else if(validateBvId(sbv).equals("1")){
            Gson gson = new Gson();
            VideoUploadInfo videoInfo = new VideoUploadInfo(sname, "", sds, 1, sbv);
            String jsonInfo = gson.toJson(videoInfo);

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = getBytes(inputStream);

            OkHttpClient client = OkHttpClientFactory.getSharedClient();



            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", fileName,RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .addFormDataPart(
                            "info",
                            null,
                            RequestBody.create(
                                    MediaType.parse("application/json"),
                                    jsonInfo
                            )
                    )
                    .build();


            Request request = new Request.Builder()
                    .url("https://eggyhub.top/api/videos/sub")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    AppLogger.e("Upload", "上传失败: " + e.getMessage());
                    runOnUiThread(() -> {
                        String errorMsg = "上传失败";
                        if (e.getMessage() != null) {
                            if (e.getMessage().contains("timeout") || e.getMessage().contains("timed out")) {
                                errorMsg = "上传超时，请检查网络连接或尝试压缩图片";
                            } else if (e.getMessage().contains("Connection")) {
                                errorMsg = "网络连接异常，请检查网络设置";
                            }
                        }
                        Toast.makeText(linkedVideoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    try (okhttp3.ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            String jsonString = responseBody.string();

                            try {
                                Gson gson = new Gson();
                                AvatarUploadResponse responseModel = gson.fromJson(jsonString, AvatarUploadResponse.class);
                                cover = responseModel != null ? responseModel.getCover() : null;
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(linkedVideoActivity.this, "解析异常", Toast.LENGTH_SHORT).show());
                            }
                            if(cover!= null){


                            runOnUiThread(() -> {
                                Toast.makeText(linkedVideoActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                                finish();
                            });}
                            else{runOnUiThread(() -> Toast.makeText(linkedVideoActivity.this, "上传失败", Toast.LENGTH_SHORT).show());}


                        } else {
                            runOnUiThread(() -> Toast.makeText(linkedVideoActivity.this, "上传失败", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            });}
            else{
                Toast.makeText(this, validateBvId(sbv), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            AppLogger.e("Upload", "上传异常: " + e.getMessage());
            Toast.makeText(this, "上传异常", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将输入流转换为字节数组
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
     */
    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    /**
     * 从URI获取文件名
     * @param uri 图片URI
     * @return 文件名
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (scheme == null || scheme.equals("content")) {
            String[] proj = {MediaStore.Images.Media.DISPLAY_NAME};
            try (Cursor cursor = getContentResolver().query(uri, proj, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    fileName = cursor.getString(columnIndex);
                }
            }
        } else if (scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        }


        if (fileName == null || fileName.isEmpty()) {
            fileName = "cover_" + System.currentTimeMillis() + ".jpg";
        }

        return fileName;
    }
    public static String validateBvId(String bvId) {
        if (bvId == null || bvId.isEmpty()) {
            return "BV号不能为空";
        }

        // 检查长度
        if (bvId.length() != 12) {
            return "BV号长度必须为12位";
        }

        // 检查前缀
        String prefix = bvId.substring(0, 2);
        if (!prefix.equalsIgnoreCase("BV")) {
            return "BV号必须以BV开头";
        }

        // 检查字符集
        String suffix = bvId.substring(2);
        for (char c : suffix.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return "BV号只能包含字母和数字";
            }
        }

        return "1";
    }


}

