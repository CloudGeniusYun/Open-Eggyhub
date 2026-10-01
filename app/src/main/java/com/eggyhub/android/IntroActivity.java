package com.eggyhub.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class IntroActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button btnStart;
    private List<Integer> images = Arrays.asList(R.drawable.intro_1, R.drawable.intro_2, R.drawable.intro_3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnStart = findViewById(R.id.btnStart);

        IntroAdapter adapter = new IntroAdapter(images);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == images.size() - 1) {
                    btnStart.setVisibility(View.VISIBLE);
                    tabLayout.setVisibility(View.GONE);
                } else {
                    btnStart.setVisibility(View.GONE);
                    tabLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        btnStart.setOnClickListener(v -> {
            // 标记已经展示过引导页
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("intro_shown", true).apply();
            
            // 跳转到 SplashActivity 重新进入正常的登录逻辑
            Intent intent = new Intent(IntroActivity.this, SplashActivity.class);
            startActivity(intent);
            finish();
        });
    }

    static class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.ViewHolder> {
        private final List<Integer> images;

        IntroAdapter(List<Integer> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.imageView.setImageResource(images.get(position));
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.introImage);
            }
        }
    }
}