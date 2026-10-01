package com.eggyhub.android.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eggyhub.android.R;
import com.eggyhub.android.theme.CardViewTheme;

/**
 * 边框设置弹窗
 */
public class BorderSettingsDialog extends DialogFragment {

    private static final String ARG_THEME = "theme";

    private CardViewTheme currentTheme;
    private int selectedColor = 0x00000000;
    private float selectedWidth = 0f;

    private SeekBar seekBarBorderWidth;
    private TextView tvBorderWidth;
    private View viewCurrentColor;

    public interface OnBorderSettingsListener {
        void onBorderSettingsSaved(CardViewTheme theme);
    }
    private OnBorderSettingsListener listener;

    public static BorderSettingsDialog newInstance(CardViewTheme theme) {
        BorderSettingsDialog dialog = new BorderSettingsDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_THEME, theme);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBorderSettingsListener(OnBorderSettingsListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTheme = (CardViewTheme) getArguments().getSerializable(ARG_THEME);
        }
        if (currentTheme == null) {
            currentTheme = new CardViewTheme();
        }

        selectedColor = currentTheme.getBorderColor();
        selectedWidth = currentTheme.getBorderWidth();
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_border_settings, null);

        initViews(view);
        setupListeners(view);

        builder.setView(view);
        builder.setTitle(null);

        return builder.create();
    }

    private void initViews(View view) {
        seekBarBorderWidth = view.findViewById(R.id.seekBarBorderWidth);
        tvBorderWidth = view.findViewById(R.id.tvBorderWidth);
        viewCurrentColor = view.findViewById(R.id.viewCurrentColor);

        // 设置当前值
        seekBarBorderWidth.setProgress((int) selectedWidth);
        tvBorderWidth.setText(selectedWidth + "dp");
        updateColorPreview();
    }

    private void setupListeners(View view) {
        // 宽度滑块
        seekBarBorderWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedWidth = progress;
                tvBorderWidth.setText(progress + "dp");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 颜色按钮
        View.OnClickListener colorClickListener = v -> {
            int color = 0x00000000;
            if (v.getId() == R.id.btnColorBlack) {
                color = 0xFF000000;
            } else if (v.getId() == R.id.btnColorWhite) {
                color = 0xFFFFFFFF;
            } else if (v.getId() == R.id.btnColorGray) {
                color = 0xFF888888;
            } else if (v.getId() == R.id.btnColorRed) {
                color = 0xFFF44336;
            } else if (v.getId() == R.id.btnColorBlue) {
                color = 0xFF2196F3;
            } else if (v.getId() == R.id.btnColorGreen) {
                color = 0xFF4CAF50;
            } else if (v.getId() == R.id.btnColorYellow) {
                color = 0xFFFFEB3B;
            } else if (v.getId() == R.id.btnColorOrange) {
                color = 0xFFFF9800;
            } else if (v.getId() == R.id.btnColorPurple) {
                color = 0xFF9C27B0;
            } else if (v.getId() == R.id.btnColorTransparent) {
                color = 0x00000000;
            }

            selectedColor = color;
            updateColorPreview();
        };

        // 设置颜色按钮点击监听器
        view.findViewById(R.id.btnColorBlack).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorWhite).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorGray).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorRed).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorBlue).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorGreen).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorYellow).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorOrange).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorPurple).setOnClickListener(colorClickListener);
        view.findViewById(R.id.btnColorTransparent).setOnClickListener(colorClickListener);

        // 保存按钮
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveSettings();
        });

        // 取消按钮
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            dismiss();
        });
    }

    private void updateColorPreview() {
        if (viewCurrentColor != null) {
            if (selectedColor == 0x00000000) {
                viewCurrentColor.setBackgroundColor(0xFFFFFFFF);
                // 添加边框表示透明
                GradientDrawable border = new GradientDrawable();
                border.setColor(0xFFFFFFFF);
                border.setStroke(2, 0xFFCCCCCC);
                viewCurrentColor.setBackground(border);
            } else {
                viewCurrentColor.setBackgroundColor(selectedColor);
            }
        }
    }

    private void saveSettings() {
        currentTheme.setBorderColor(selectedColor);
        currentTheme.setBorderWidth(selectedWidth);

        if (listener != null) {
            listener.onBorderSettingsSaved(currentTheme);
        }

        dismiss();
    }
}