package com.eggyhub.android;

import com.eggyhub.android.log.AppLogger;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.MediaStore;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.SeekBar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Toast;
import java.io.File;

import com.eggyhub.android.FirstOverlayService;
import com.eggyhub.android.MinimizeIconService;
import com.eggyhub.android.OverlayService;
import com.yalantis.ucrop.UCrop;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class OverlaySettingsActivity extends BaseActivity {

    private static final String LAST_FETCH_TIME_KEY = "last_fetch_time";
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    private SharedPreferences preferences;
    private androidx.appcompat.widget.SwitchCompat switchQuickReplenish;
    private ColorSaturationBrightnessPicker colorSaturationBrightnessPicker;
    private ColorHueSlider colorHueSlider;
    private View colorPreview;
    private EditText editTextRgbCode;
    private TextView textViewOpacityPercentage;
    private SeekBar seekBarOpacity;
    private androidx.cardview.widget.CardView cardShowHostedShareCodes;
    private androidx.appcompat.widget.SwitchCompat switchShowHostedShareCodes;
    private androidx.appcompat.widget.SwitchCompat switchEnableHeightEdit;
    private TextView textViewPreviewRectangle;
    private androidx.cardview.widget.CardView cardMinimizeIcon;

    private ColorSaturationBrightnessPicker colorSaturationBrightnessPickerFont;
    private ColorHueSlider colorHueSliderFont;
    private View colorPreviewFont;
    private EditText editTextRgbCodeFont;

    private static final String PREF_BACKGROUND_COLOR = "pref_background_color";
    private static final String PREF_FONT_COLOR = "pref_font_color";
    private static final String PREF_SHOW_HOSTED_SHARE_CODES = "pref_show_hosted_share_codes";
    private static final String PREF_ENABLE_HEIGHT_EDIT = "pref_enable_height_edit";
    private static final int REQUEST_CODE_PICK_IMAGE = 101; // New request code for image picker

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay_settings);

        // Set up touch listener for non-text box views to hide keyboard.
        findViewById(R.id.root_layout).setOnTouchListener((v, event) -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            v.clearFocus();
            return false;
        });

        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        switchQuickReplenish = findViewById(R.id.switchQuickReplenish);
        colorSaturationBrightnessPicker = findViewById(R.id.colorSaturationBrightnessPicker);
        colorHueSlider = findViewById(R.id.colorHueSlider);

        // Color preview and RGB input
        colorPreview = findViewById(R.id.colorPreview);
        editTextRgbCode = findViewById(R.id.editTextRgbCode);

        // Opacity controls
        textViewOpacityPercentage = findViewById(R.id.textViewOpacityPercentage);
        seekBarOpacity = findViewById(R.id.seekBarOpacity);

        // Hosted Share Codes controls
        cardShowHostedShareCodes = findViewById(R.id.cardShowHostedShareCodes);
        switchShowHostedShareCodes = findViewById(R.id.switchShowHostedShareCodes);

        // Height Edit controls
        switchEnableHeightEdit = findViewById(R.id.switchEnableHeightEdit);
        boolean isHeightEditEnabled = preferences.getBoolean(PREF_ENABLE_HEIGHT_EDIT, false);
        switchEnableHeightEdit.setChecked(isHeightEditEnabled);
        switchEnableHeightEdit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(PREF_ENABLE_HEIGHT_EDIT, isChecked).apply();
        });

        // Preview Rectangle
        textViewPreviewRectangle = findViewById(R.id.textViewPreviewRectangle);

        // Minimize Icon Card
        cardMinimizeIcon = findViewById(R.id.cardMinimizeIcon);
        cardMinimizeIcon.setOnClickListener(v -> {
            // 启动图片选择器
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });

        // 根据登录返回的数据中sponser的值不为0.00来控制cardMinimizeIcon的显示
        SharedPreferences userPrefsForSponsor = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String sponserValueStr = userPrefsForSponsor.getString("sponser", "0"); // 获取sponser值，默认为"0"
        try {
            double sponserValue = Double.parseDouble(sponserValueStr);
            if (sponserValue != 0.00) {
                cardMinimizeIcon.setVisibility(View.VISIBLE);
            } else {
                cardMinimizeIcon.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            AppLogger.e("OverlaySettingsActivity", "Error parsing sponser value: " + sponserValueStr, e);
            cardMinimizeIcon.setVisibility(View.GONE); // 解析失败则隐藏
        }

        // Implement actual admin check logic
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userRole = userPrefs.getString("role", "user"); // Default to "user" if not found
        if ("admin".equals(userRole)) {
            cardShowHostedShareCodes.setVisibility(View.VISIBLE);
        } else {
            cardShowHostedShareCodes.setVisibility(View.GONE);
        }

        // 从SharedPreferences加载开关状态
        boolean isQuickReplenishEnabled = preferences.getBoolean("quick_replenish_enabled", false);
        switchQuickReplenish.setChecked(isQuickReplenishEnabled);

        // Load hosted share codes switch state
        boolean isShowHostedShareCodesEnabled = preferences.getBoolean(PREF_SHOW_HOSTED_SHARE_CODES, false);
        switchShowHostedShareCodes.setChecked(isShowHostedShareCodesEnabled);

        // 从SharedPreferences加载背景颜色，如果不存在则使用默认颜色 (例如，白色)
        int savedBackgroundColor = preferences.getInt(PREF_BACKGROUND_COLOR, Color.BLACK);

        // Load and set opacity
        int savedOpacity = preferences.getInt("pref_background_opacity", 70); // Default to 70%


        float[] hsv = new float[3];
        Color.colorToHSV(savedBackgroundColor, hsv);
        colorHueSlider.setHue(hsv[0]);
        colorSaturationBrightnessPicker.setHue(hsv[0]);
        // Note: Setting saturation and brightness directly is not straightforward with HSVToColor, 
        // as it\'s handled by the picker\'s touch events. We\'ll rely on the picker\'s internal state for now.

        // Update color preview and RGB code initially
        updateColorPreviewAndRgbCode(savedBackgroundColor, savedOpacity);

        colorHueSlider.setOnHueChangedListener(hue -> {
            colorSaturationBrightnessPicker.setHue(hue);
            int currentColor = colorSaturationBrightnessPicker.getColor();
            updateColorPreviewAndRgbCode(currentColor, seekBarOpacity.getProgress());
        });

        colorSaturationBrightnessPicker.setOnColorChangedListener(color -> {
            preferences.edit().putInt(PREF_BACKGROUND_COLOR, color).apply();
            updateColorPreviewAndRgbCode(color, seekBarOpacity.getProgress());
        });

        editTextRgbCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed for this implementation
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hexColor = s.toString();
                if (hexColor.length() == 6) {
                    try {
                        int color = Color.parseColor("#" + hexColor);
                        // Update picker only if the color is different to avoid infinite loop
                        if (color != colorSaturationBrightnessPicker.getColor()) {
                            float[] hsv = new float[3];
                            Color.colorToHSV(color, hsv);
                            colorHueSlider.setHue(hsv[0]);
                            colorSaturationBrightnessPicker.setHue(hsv[0]);
                            // Manually set saturation and brightness from the parsed color
                            colorSaturationBrightnessPicker.setSaturation(hsv[1]);
                            colorSaturationBrightnessPicker.setBrightness(hsv[2]);
                            preferences.edit().putInt(PREF_BACKGROUND_COLOR, color).apply();
                            updateColorPreviewAndRgbCode(color, seekBarOpacity.getProgress());
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid hex color, ignore or show error
                    }
                }
            }
        });

        textViewOpacityPercentage.setText(savedOpacity + "%" );
        seekBarOpacity.setProgress(savedOpacity);

        seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewOpacityPercentage.setText(progress + "%");
                preferences.edit().putInt("pref_background_opacity", progress).apply();
                updateColorPreviewAndRgbCode(colorSaturationBrightnessPicker.getColor(), progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed for this implementation
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed for this implementation
            }
        });

        switchShowHostedShareCodes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(PREF_SHOW_HOSTED_SHARE_CODES, isChecked).apply();
        });

        switchQuickReplenish.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 保存开关状态到SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("quick_replenish_enabled", isChecked);
            editor.apply();
            if (isChecked) {
                if (!Settings.canDrawOverlays(OverlaySettingsActivity.this)) {
                    // 请求 SYSTEM_ALERT_WINDOW 权限
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1001);
                    // 此时不直接启动服务，等待权限回调
                } else {
                    // 权限已授予，启动服务显示悬浮窗
                    startOverlayService();
                }
            } else {
                // 关闭悬浮窗
                stopOverlayService();
            }
        });

        // Font Color controls
        colorSaturationBrightnessPickerFont = findViewById(R.id.colorSaturationBrightnessPickerFont);
        colorHueSliderFont = findViewById(R.id.colorHueSliderFont);
        colorPreviewFont = findViewById(R.id.colorPreviewFont);
        editTextRgbCodeFont = findViewById(R.id.editTextRgbCodeFont);

        // 从SharedPreferences加载字体颜色，如果不存在则使用默认颜色 (例如，白色)
        int savedFontColor = preferences.getInt(PREF_FONT_COLOR, Color.WHITE);

        float[] hsvFont = new float[3];
        Color.colorToHSV(savedFontColor, hsvFont);
        colorHueSliderFont.setHue(hsvFont[0]);
        colorSaturationBrightnessPickerFont.setHue(hsvFont[0]);
        updateFontColorPreviewAndRgbCode(savedFontColor);

        colorHueSliderFont.setOnHueChangedListener(hue -> {
            colorSaturationBrightnessPickerFont.setHue(hue);
            int currentColor = colorSaturationBrightnessPickerFont.getColor();
            updateFontColorPreviewAndRgbCode(currentColor);
        });

        colorSaturationBrightnessPickerFont.setOnColorChangedListener(color -> {
            preferences.edit().putInt(PREF_FONT_COLOR, color).apply();
            updateFontColorPreviewAndRgbCode(color);
        });

        editTextRgbCodeFont.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed for this implementation
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hexColor = s.toString();
                if (hexColor.length() == 6) {
                    try {
                        int color = Color.parseColor("#" + hexColor);
                        if (color != colorSaturationBrightnessPickerFont.getColor()) {
                            float[] hsv = new float[3];
                            Color.colorToHSV(color, hsv);
                            colorHueSliderFont.setHue(hsv[0]);
                            colorSaturationBrightnessPickerFont.setHue(hsv[0]);
                            colorSaturationBrightnessPickerFont.setSaturation(hsv[1]);
                            colorSaturationBrightnessPickerFont.setBrightness(hsv[2]);
                            preferences.edit().putInt(PREF_FONT_COLOR, color).apply();
                            updateFontColorPreviewAndRgbCode(color);
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid hex color, ignore or show error
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 根据服务的实际运行状态更新开关UI
        boolean isFirstOverlayActive = FirstOverlayService.isFirstOverlayActive();
        boolean isMinimizeIconActive = MinimizeIconService.isMinimizeIconActive();
        boolean isOverlayServiceActive = OverlayService.isOverlayServiceActive();

        // 如果任何一个服务正在运行，则开关应为开启状态
        // 否则，如果所有服务都未运行，则开关应为关闭状态
        if (isFirstOverlayActive || isMinimizeIconActive || isOverlayServiceActive) {
            switchQuickReplenish.setChecked(true);
        } else {
            switchQuickReplenish.setChecked(false);
        }

        // 确保SharedPreferences中的状态与UI同步
        preferences.edit().putBoolean("quick_replenish_enabled", switchQuickReplenish.isChecked()).apply();
    }

    private void startOverlayService() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userRole = userPrefs.getString("role", "user");

        if ("admin".equals(userRole)) {
            // 管理员账号，直接刷新数据，不限制刷新时间
            AppLogger.d("OverlaySettingsActivity", "Admin user detected. Forcing refresh and skipping time limit.");
            Intent serviceIntent = new Intent(OverlaySettingsActivity.this, OverlayService.class);
            serviceIntent.setAction("ACTION_REFRESH_SHARE_CODE"); // 发送刷新数据的Action
            serviceIntent.putExtra(PREF_SHOW_HOSTED_SHARE_CODES, preferences.getBoolean(PREF_SHOW_HOSTED_SHARE_CODES, false));
            startService(serviceIntent);
            // 管理员不更新上次获取时间
        } else {
            // 普通用户，检查缓存时间
            AppLogger.d("OverlaySettingsActivity", "Regular user detected. Checking cache time.");
            long lastFetchTime = preferences.getLong(LAST_FETCH_TIME_KEY, 0);
            long currentTime = System.currentTimeMillis();
            AppLogger.d("OverlaySettingsActivity", "lastFetchTime = " + lastFetchTime + ", currentTime = " + currentTime);

            if (lastFetchTime == 0 || (currentTime - lastFetchTime) > CACHE_DURATION) {
                // 缓存过期或首次加载，启动服务并刷新数据
                AppLogger.d("OverlaySettingsActivity", "Cache expired or first load. Refreshing data.");
                Intent serviceIntent = new Intent(OverlaySettingsActivity.this, OverlayService.class);
                serviceIntent.setAction("ACTION_REFRESH_SHARE_CODE"); // 发送刷新数据的Action
                serviceIntent.putExtra(PREF_SHOW_HOSTED_SHARE_CODES, preferences.getBoolean(PREF_SHOW_HOSTED_SHARE_CODES, false));
                startService(serviceIntent);
                // 更新上次获取时间
                preferences.edit().putLong(LAST_FETCH_TIME_KEY, currentTime).apply();
            } else {
                // 缓存未过期，直接启动服务显示悬浮窗（不刷新数据）
                AppLogger.d("OverlaySettingsActivity", "Cache not expired. Showing overlay without refresh.");
                Intent serviceIntent = new Intent(OverlaySettingsActivity.this, OverlayService.class);
                serviceIntent.setAction("ACTION_SHOW_SECOND_OVERLAY");
                serviceIntent.putExtra(PREF_SHOW_HOSTED_SHARE_CODES, preferences.getBoolean(PREF_SHOW_HOSTED_SHARE_CODES, false));
                startService(serviceIntent);
            }
        }
    }

    private void stopOverlayService() {
        // 停止 OverlayService
        Intent serviceIntent = new Intent(OverlaySettingsActivity.this, OverlayService.class);
        stopService(serviceIntent);

        // 停止 FirstOverlayService
        Intent firstOverlayServiceIntent = new Intent(OverlaySettingsActivity.this, FirstOverlayService.class);
        stopService(firstOverlayServiceIntent);

        // 停止 MinimizeIconService
        Intent minimizeIconServiceIntent = new Intent(OverlaySettingsActivity.this, MinimizeIconService.class);
        stopService(minimizeIconServiceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (Settings.canDrawOverlays(this)) {
                // 权限已授予，启动服务显示悬浮窗
                startOverlayService();
            } else {
                // 权限未授予，关闭开关并提示用户
                switchQuickReplenish.setChecked(false);
                preferences.edit().putBoolean("quick_replenish_enabled", false).apply();
                Toast.makeText(this, "需要悬浮窗权限才能开启此功能", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri sourceUri = data.getData();
            if (sourceUri != null) {
                // 创建一个目标URI，用于保存裁剪后的图片
                File croppedImageFile = new File(getCacheDir(), "cropped_image.jpg");
                Uri destinationUri = Uri.fromFile(croppedImageFile);

                // 启动 uCrop 裁剪
                UCrop.of(sourceUri, destinationUri)
                        .withAspectRatio(1, 1) // 固定裁剪比例为 1:1
                        .start(this);
            }
        } else if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    // 这里可以处理裁剪后的图片，例如显示在 ImageView 中或上传
                    Toast.makeText(this, "请重启弹窗以应用效果", Toast.LENGTH_LONG).show();
                    // 保存裁剪后的图片 URI 到 SharedPreferences
                    preferences.edit().putString("minimize_icon_uri", resultUri.toString()).apply();
                } else {
                    Toast.makeText(this, "裁剪后的图片URI为空", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                final Throwable cropError = UCrop.getError(data);
                if (cropError != null) {
                    Toast.makeText(this, "图片裁剪失败: " + cropError.getMessage(), Toast.LENGTH_LONG).show();
                    AppLogger.e("OverlaySettingsActivity", "uCrop error", cropError);
                } else {
                    Toast.makeText(this, "图片裁剪失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updateColorPreviewAndRgbCode(int color, int opacity) {
        // opacity is 0-100, convert to alpha 0-255
        int alpha = (int) (255 * (opacity / 100.0f));
        int colorWithAlpha = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        colorPreview.setBackgroundColor(colorWithAlpha);
        if (textViewPreviewRectangle != null) {
            textViewPreviewRectangle.setBackgroundColor(colorWithAlpha);
        }
        editTextRgbCode.setText(String.format("%06X", (0xFFFFFF & color)));
        // Move cursor to the end of the text
        editTextRgbCode.setSelection(editTextRgbCode.getText().length());
    }

    private void updateFontColorPreviewAndRgbCode(int color) {
        colorPreviewFont.setBackgroundColor(color);
        if (textViewPreviewRectangle != null) {
            textViewPreviewRectangle.setTextColor(color);
        }
        editTextRgbCodeFont.setText(String.format("%06X", (0xFFFFFF & color)));
        editTextRgbCodeFont.setSelection(editTextRgbCodeFont.getText().length());
    }
}