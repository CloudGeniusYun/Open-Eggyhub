package com.eggyhub.android;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.eggyhub.android.log.AppLogger;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class ImagePreviewActivity extends AppCompatActivity {
    private static final String TAG = "ImagePreviewActivity";
    private ImageView imageViewPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        // 初始化视图
        imageViewPreview = findViewById(R.id.imageViewPreview);

        // 获取传入的图片URL和认证token
        Intent intent = getIntent();
        if (intent != null) {
            String imageUrl = intent.getStringExtra("image_url");
            String token = intent.getStringExtra("token");

            if (imageUrl != null && !imageUrl.isEmpty()) {
                AppLogger.d(TAG, "预览图片URL: " + imageUrl);
                // 使用Glide加载图片
                loadImageWithGlide(imageUrl, token);
            } else {
                AppLogger.e(TAG, "图片URL为空");
                Toast.makeText(this, "无法预览图片: URL为空", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            AppLogger.e(TAG, "Intent为空");
            Toast.makeText(this, "无法预览图片: 缺少参数", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 使用Glide加载图片
     * @param imageUrl 图片URL
     * @param token 认证token
     */
    private void loadImageWithGlide(String imageUrl, String token) {
        try {
            Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<Drawable> target, boolean isFirstResource) {
                            AppLogger.e(TAG, "图片加载失败: " + e.getMessage());
                            Toast.makeText(ImagePreviewActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, com.bumptech.glide.request.target.Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            AppLogger.d(TAG, "图片加载成功");
                            return false;
                        }
                    })
                    .into(imageViewPreview);
        } catch (Exception e) {
            AppLogger.e(TAG, "加载图片异常: " + e.getMessage());
            Toast.makeText(this, "加载图片异常", Toast.LENGTH_SHORT).show();
        }
    }
}