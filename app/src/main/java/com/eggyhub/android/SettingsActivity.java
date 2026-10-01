package com.eggyhub.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eggyhub.android.log.AppLogger;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 设置页面
 * 包含应用图标切换、调试日志、关于、反馈等系统级设置
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        
        setupViews();
    }

    private void setupViews() {
        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 切换图标
        findViewById(R.id.cardSwitchIcon).setOnClickListener(v -> showSwitchIconDialog());

        // CardView样式设置
        findViewById(R.id.cardCardViewStyle).setOnClickListener(v -> {
            AppLogger.d(TAG, "Opening CardViewStyleSettingsActivity");
            Intent intent = new Intent(this, CardViewStyleSettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 弹窗设置
        findViewById(R.id.cardOverlaySettings).setOnClickListener(v -> {
            AppLogger.d(TAG, "Opening OverlaySettingsActivity");
            Intent intent = new Intent(this, OverlaySettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 调试日志开关
        setupDebugLogSwitch();

        // 关于
        findViewById(R.id.cardAboutApp).setOnClickListener(v -> {
            AppLogger.d(TAG, "Opening AboutActivity");
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 赞助
        findViewById(R.id.cardSponsor).setOnClickListener(v -> {
            AppLogger.d(TAG, "Opening SponsorActivity");
            Intent intent = new Intent(this, SponsorActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 贡献者名单
        findViewById(R.id.cardGratitude).setOnClickListener(v -> {
            AppLogger.d(TAG, "Opening GratitudeActivity");
            Intent intent = new Intent(this, GratitudeActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupDebugLogSwitch() {
        String debugPackageName = "com.eggyhub.debug";
        boolean isDebugAppInstalled = false;
        try {
            getPackageManager().getPackageInfo(debugPackageName, 0);
            isDebugAppInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            isDebugAppInstalled = false;
        }

        CardView cardDebugLog = findViewById(R.id.cardDebugLog);
        SwitchCompat switchLocalLog = findViewById(R.id.switchLocalLog);

        if (isDebugAppInstalled && cardDebugLog != null && switchLocalLog != null) {
            cardDebugLog.setVisibility(View.VISIBLE);
            
            boolean isLocalLogEnabled = preferences.getBoolean("local_log_enabled", true);
            switchLocalLog.setChecked(isLocalLogEnabled);

            switchLocalLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AppLogger.d(TAG, "Debug log enabled: " + isChecked);
                preferences.edit().putBoolean("local_log_enabled", isChecked).apply();
                Toast.makeText(this, isChecked ? "本地日志存储已开启" : "本地日志存储已关闭", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showSwitchIconDialog() {
        AppLogger.d(TAG, "Showing SwitchIconDialog");
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_switch_icon, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewIcons);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));

        String sponsorValueStr = preferences.getString("sponser", "0");
        boolean isSponsor = false;
        try {
            double sponsorValue = Double.parseDouble(sponsorValueStr);
            isSponsor = sponsorValue > 0.00;
        } catch (Exception e) {
            isSponsor = false;
        }

        String[] names = {"默认图标", "新年图标", "极简（黑）", "极简（白）", "黑金（深）", "黑金（金）", "黑金（深）阴影", "黑金（金）阴影"};
        int[] icons = {
            R.mipmap.ic_launcher, 
            R.mipmap.ic_launcher_newyear, 
            R.mipmap.ic_launcher_white,
            R.mipmap.ic_launcher_dark,
            R.mipmap.ic_launcher_sponsor1, 
            R.mipmap.ic_launcher_sponsor2, 
            R.mipmap.ic_launcher_sponsor3, 
            R.mipmap.ic_launcher_sponsor4
        };
        String[] aliases = {
            IconManager.ICON_DEFAULT, 
            IconManager.ICON_NEW_YEAR, 
            IconManager.ICON_WHITE,
            IconManager.ICON_DARK,
            IconManager.ICON_SPONSOR1, 
            IconManager.ICON_SPONSOR2, 
            IconManager.ICON_SPONSOR3, 
            IconManager.ICON_SPONSOR4
        };
        
        String currentIcon = IconManager.getCurrentIcon(this);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .create();

        boolean finalIsSponsor = isSponsor;
        IconAdapter adapter = new IconAdapter(names, icons, aliases, currentIcon, finalIsSponsor, (targetAlias) -> {
            boolean isExclusive = targetAlias.contains("Sponsor");
            if (isExclusive && !finalIsSponsor) {
                Toast.makeText(this, "该图标为赞助者专属，请前往支持我们~", Toast.LENGTH_SHORT).show();
                return;
            }

            AppLogger.i(TAG, "Switching icon to: " + targetAlias);
            IconManager.switchIcon(this, targetAlias);
            Toast.makeText(this, "图标已切换，稍后生效", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        dialog.show();

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int buttonTextColor = (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLACK;
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonTextColor);
    }

    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private final String[] names;
        private final int[] icons;
        private final String[] aliases;
        private final String currentAlias;
        private final OnIconClickListener listener;
        private final boolean isSponsor;

        public IconAdapter(String[] names, int[] icons, String[] aliases, String currentAlias, boolean isSponsor, OnIconClickListener listener) {
            this.names = names;
            this.icons = icons;
            this.aliases = aliases;
            this.currentAlias = currentAlias;
            this.isSponsor = isSponsor;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.imageIcon.setImageResource(icons[position]);
            String name = names[position];
            boolean isExclusive = aliases[position].contains("Sponsor");
            
            if (isExclusive) {
                SpannableString spannableString = new SpannableString("  " + name);
                Drawable drawable = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.vip);
                if (drawable != null) {
                    int size = (int)(holder.textName.getTextSize() * 1.3);
                    drawable.setBounds(0, 0, size, size);
                    ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
                    spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                holder.textName.setText(spannableString);
            } else {
                holder.textName.setText(name);
            }
            
            holder.radioButton.setChecked(aliases[position].equals(currentAlias));
            holder.itemView.setOnClickListener(v -> listener.onIconClick(aliases[position]));
        }

        @Override
        public int getItemCount() {
            return names.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageIcon;
            TextView textName;
            RadioButton radioButton;

            public ViewHolder(View itemView) {
                super(itemView);
                imageIcon = itemView.findViewById(R.id.imageIconPreview);
                textName = itemView.findViewById(R.id.textViewIconName);
                radioButton = itemView.findViewById(R.id.radioSelected);
            }
        }
    }

    interface OnIconClickListener {
        void onIconClick(String targetAlias);
    }
}