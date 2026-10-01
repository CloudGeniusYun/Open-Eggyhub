package com.eggyhub.android;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;

import com.eggyhub.android.dialog.StickerEditorDialog;
import com.eggyhub.android.theme.CardViewTheme;

/**
 * 主题设置页面
 */
public class CardViewStyleSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardview_style_settings);

        // 设置标题为"主题"
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("主题");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        // 初始化视图
    }

    private void setupListeners() {
        // 分享码列表卡片 - 点击打开编辑器
        CardView cardShareCode = findViewById(R.id.cardShareCode);
        cardShareCode.setOnClickListener(v -> {
            openStickerEditor();
        });
    }

    private void openStickerEditor() {
        CardViewTheme currentTheme = themeViewModel.getCardViewTheme().getValue();
        if (currentTheme == null) {
            currentTheme = new CardViewTheme();
        }

        StickerEditorDialog dialog = StickerEditorDialog.newInstance(currentTheme, themeViewModel);
        dialog.setOnThemeSavedListener(theme -> {
            Toast.makeText(this, "主题已保存并应用", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "StickerEditorDialog");
    }
}