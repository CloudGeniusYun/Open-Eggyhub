package com.eggyhub.android.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eggyhub.android.R;
import com.eggyhub.android.theme.CardViewTheme;
import com.eggyhub.android.theme.Sticker;
import com.eggyhub.android.theme.ThemeViewModel;
import com.eggyhub.android.views.StickerEditorView;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * 贴纸编辑器弹窗
 */
public class StickerEditorDialog extends DialogFragment {
    private static final String TAG = "StickerEditorDialog";
    
    private StickerEditorView stickerEditorView;
    private TextView tvStickerCount;
    private MaterialButton btnAddSticker, btnLayerManager, btnSave, btnCancel, btnRestoreDefault, btnBorderSettings;
    
    private CardViewTheme currentTheme;
    private ThemeViewModel themeViewModel;
    private Sticker selectedSticker = null; // 当前选中的贴纸
    
    // 图片选择器
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    public interface OnThemeSavedListener {
        void onThemeSaved(CardViewTheme theme);
    }
    private OnThemeSavedListener themeSavedListener;
    
    public static StickerEditorDialog newInstance(CardViewTheme theme, ThemeViewModel viewModel) {
        StickerEditorDialog dialog = new StickerEditorDialog();
        dialog.currentTheme = theme;
        dialog.themeViewModel = viewModel;
        return dialog;
    }
    
    public void setOnThemeSavedListener(OnThemeSavedListener listener) {
        this.themeSavedListener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        // 初始化图片选择器
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        addStickerFromUri(imageUri);
                    }
                }
            }
        );
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_sticker_editor, container, false);
        
        initViews(view);
        setupListeners();
        loadCurrentTheme();
        
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 设置全屏
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                // 点击外部不关闭
                setCancelable(false);
            }
        }
    }
    
    private void initViews(View view) {
        stickerEditorView = view.findViewById(R.id.stickerEditorView);
        tvStickerCount = view.findViewById(R.id.tvStickerCount);
        btnAddSticker = view.findViewById(R.id.btnAddSticker);
        btnLayerManager = view.findViewById(R.id.btnLayerManager);
        btnBorderSettings = view.findViewById(R.id.btnBorderSettings);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnRestoreDefault = view.findViewById(R.id.btnRestoreDefault);
    }
    
    private void setupListeners() {
        btnAddSticker.setOnClickListener(v -> openImagePicker());
        btnLayerManager.setOnClickListener(v -> openLayerManager());
        btnBorderSettings.setOnClickListener(v -> openBorderSettings());
        btnSave.setOnClickListener(v -> saveTheme());
        btnCancel.setOnClickListener(v -> dismiss());
        btnRestoreDefault.setOnClickListener(v -> showRestoreDefaultConfirmation());

        stickerEditorView.setOnStickerInteractionListener(new StickerEditorView.OnStickerInteractionListener() {
            @Override
            public void onStickerDeleted(Sticker sticker) {
                handleDeleteSticker(sticker);
            }

            @Override
            public void onStickerCopied(Sticker sticker) {
                handleCopySticker(sticker);
            }

            @Override
            public void onStickerSelected(Sticker sticker) {
                updateStickerCount();
            }

            @Override
            public void onStickerDeselected() {
                updateStickerCount();
            }

            @Override
            public void onBindingChanged(Sticker sticker, int bindingType, boolean enabled) {
                // 更新贴纸的绑定状态
                switch (bindingType) {
                    case 0: // BINDING_LEFT
                        sticker.setBindLeft(enabled);
                        break;
                    case 1: // BINDING_TOP
                        sticker.setBindTop(enabled);
                        break;
                    case 2: // BINDING_RIGHT
                        sticker.setBindRight(enabled);
                        break;
                    case 3: // BINDING_BOTTOM
                        sticker.setBindBottom(enabled);
                        break;
                }

                Log.d(TAG, "Binding changed for sticker: " + sticker.getId() + ", type=" + bindingType + ", enabled=" + enabled);
            }
        });
    }
    
    private void loadCurrentTheme() {
        if (currentTheme != null) {
            stickerEditorView.setStickers(currentTheme.getStickers());
            updateStickerCount();
            
            // 设置预览区域（CardView大小）
            stickerEditorView.post(() -> {
                int viewWidth = stickerEditorView.getWidth();
                int viewHeight = stickerEditorView.getHeight();
                float cornerRadius = currentTheme.getCornerRadius();
                
                // 预览区为CardView的实际尺寸（居中显示）
                float previewWidth = viewWidth - 32;
                float previewHeight = 130 * getResources().getDisplayMetrics().density;
                float left = 16;
                float top = (viewHeight - previewHeight) / 2;
                float right = left + previewWidth;
                float bottom = top + previewHeight;
                
                stickerEditorView.setPreviewRect(left, top, right, bottom, cornerRadius);
            });
        }
    }
    
    private void openImagePicker() {
        // 检查贴纸数量限制
        if (currentTheme != null && currentTheme.getMaxStickers() > 0) {
            int currentCount = currentTheme.getStickers().size();
            if (currentCount >= currentTheme.getMaxStickers()) {
                Toast.makeText(requireContext(), 
                    "已达到最大贴纸数量: " + currentTheme.getMaxStickers(), 
                    Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "选择贴纸图片"));
    }
    
    private LayerManagerBottomSheet layerManagerBottomSheet;

    private void openLayerManager() {
        if (layerManagerBottomSheet == null) {
            layerManagerBottomSheet = LayerManagerBottomSheet.newInstance(
                currentTheme.getStickers(),
                new LayerManagerBottomSheet.OnLayerActionListener() {
                    @Override
                    public void onLayerSelected(int position, Sticker sticker) {
                        // 选中贴纸
                        if (selectedSticker != null && selectedSticker.getId().equals(sticker.getId())) {
                            selectedSticker = null;
                            stickerEditorView.setStickers(currentTheme.getStickers());
                        } else {
                            selectedSticker = sticker;
                            stickerEditorView.setStickers(currentTheme.getStickers());
                        }
                        updateStickerCount();
                    }

                    @Override
                    public void onLayerMoved(int fromPosition, int toPosition) {
                        // 图层顺序已改变，更新显示
                        stickerEditorView.setStickers(currentTheme.getStickers());
                    }

                    @Override
                    public void onMoveToTop(int position) {
                        currentTheme.moveLayerToTop(currentTheme.getStickers().get(position).getId());
                        stickerEditorView.setStickers(currentTheme.getStickers());
                        layerManagerBottomSheet.updateStickers(currentTheme.getStickers());
                    }

                    @Override
                    public void onMoveToBottom(int position) {
                        currentTheme.moveLayerToBottom(currentTheme.getStickers().get(position).getId());
                        stickerEditorView.setStickers(currentTheme.getStickers());
                        layerManagerBottomSheet.updateStickers(currentTheme.getStickers());
                    }

                    @Override
                    public void onClipToRoundedCornersChanged(int stickerIndex, boolean clipToRoundedCorners) {
                        // 显示确认弹窗
                        showClipConfirmationDialog(stickerIndex, clipToRoundedCorners);
                    }
                }
            );
        } else {
            layerManagerBottomSheet.updateStickers(currentTheme.getStickers());
        }
        
        layerManagerBottomSheet.show(getChildFragmentManager(), "LayerManagerBottomSheet");
    }

    private void openBorderSettings() {
        // 创建边框设置弹窗
        BorderSettingsDialog dialog = BorderSettingsDialog.newInstance(currentTheme);
        dialog.setOnBorderSettingsListener(theme -> {
            // 更新当前主题
            currentTheme = theme;
            Toast.makeText(requireContext(), "边框设置已保存", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "BorderSettingsDialog");
    }
    
    private void addStickerFromUri(Uri imageUri) {
        try {
            // 1. 将图片保存到应用私有目录
            String fileName = "sticker_" + System.currentTimeMillis() + ".png";
            File destFile = new File(requireContext().getFilesDir(), "themes/" + fileName);
            destFile.getParentFile().mkdirs();
            
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                // 保存到文件
                FileOutputStream outputStream = new FileOutputStream(destFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                
                // 2. 创建贴纸对象
                float x = 50f + (float) Math.random() * 200f; // 随机X位置
                float y = 50f + (float) Math.random() * 200f; // 随机Y位置
                float width = bitmap.getWidth();
                float height = bitmap.getHeight();
                
                // 如果图片太大，通过scale来缩小显示，而不是改变width/height
                float initialScale = 1.0f;
                float maxWidth = stickerEditorView.getWidth() * 0.5f;
                float maxHeight = stickerEditorView.getHeight() * 0.5f;
                if (width > maxWidth || height > maxHeight) {
                    initialScale = Math.min(maxWidth / width, maxHeight / height);
                }
                
                Sticker sticker = new Sticker(destFile.getAbsolutePath(), x, y, width, height);
                sticker.setScale(initialScale);
                
                // 3. 添加到编辑器
                stickerEditorView.addSticker(sticker);
                currentTheme.addSticker(sticker);
                updateStickerCount();
                
                Log.d(TAG, "Added sticker: " + sticker.getId() + " from " + destFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add sticker", e);
            Toast.makeText(requireContext(), "添加贴纸失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleDeleteSticker(Sticker sticker) {
        stickerEditorView.removeSticker(sticker.getId());
        currentTheme.removeSticker(sticker.getId());
        updateStickerCount();
        
        Toast.makeText(requireContext(), "贴纸已删除", Toast.LENGTH_SHORT).show();
    }
    
    private void handleCopySticker(Sticker sticker) {
        try {
            // 复制贴纸文件
            File originalFile = new File(sticker.getImagePath());
            String newFileName = "sticker_" + System.currentTimeMillis() + ".png";
            File newFile = new File(requireContext().getFilesDir(), "themes/" + newFileName);
            
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
            FileOutputStream outputStream = new FileOutputStream(newFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
            
            // 创建新贴纸（位置偏移）
            Sticker newSticker = new Sticker(
                newFile.getAbsolutePath(),
                sticker.getX() + 50,
                sticker.getY() + 50,
                sticker.getWidth(),
                sticker.getHeight()
            );
            newSticker.setScale(sticker.getScale());
            newSticker.setRotation(sticker.getRotation());
            newSticker.setAlpha(sticker.getAlpha());
            newSticker.setClipToRoundedCorners(sticker.isClipToRoundedCorners());
            
            stickerEditorView.addSticker(newSticker);
            currentTheme.addSticker(newSticker);
            updateStickerCount();
            
            Toast.makeText(requireContext(), "贴纸已复制", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy sticker", e);
            Toast.makeText(requireContext(), "复制贴纸失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveTheme() {
        if (currentTheme == null) {
            currentTheme = new CardViewTheme();
        }
        
        // 获取编辑后的贴纸列表
        List<Sticker> stickers = stickerEditorView.getStickers();
        currentTheme.setStickers(stickers);
        currentTheme.setEnabled(true);
        
        // 保存到ViewModel
        if (themeViewModel != null) {
            themeViewModel.updateCardViewTheme(currentTheme);
        }
        
        // 回调
        if (themeSavedListener != null) {
            themeSavedListener.onThemeSaved(currentTheme);
        }
        
        Toast.makeText(requireContext(), "主题已保存", Toast.LENGTH_SHORT).show();
        dismiss();
    }
    
    private void updateStickerCount() {
        int count = stickerEditorView.getStickers().size();
        tvStickerCount.setText(count + "个贴纸");
    }

    /**
     * 显示恢复默认确认对话框
     */
    private void showRestoreDefaultConfirmation() {
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("恢复默认")
            .setMessage("确定要清除所有贴纸和自定义设置,恢复到默认样式吗?")
            .setPositiveButton("确定", (dialog, which) -> {
                restoreDefaultTheme();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 恢复默认主题
     */
    private void restoreDefaultTheme() {
        // 创建一个空的默认主题
        CardViewTheme defaultTheme = new CardViewTheme();
        defaultTheme.setEnabled(false);
        defaultTheme.setCornerRadius(8f);
        defaultTheme.setBorderColor(0x00000000);
        defaultTheme.setBorderWidth(0f);
        defaultTheme.getStickers().clear();

        // 更新当前主题
        currentTheme = defaultTheme;

        // 清空编辑器中的贴纸
        stickerEditorView.setStickers(new java.util.ArrayList<>());

        // 更新ViewModel
        if (themeViewModel != null) {
            themeViewModel.updateCardViewTheme(defaultTheme);
        }

        updateStickerCount();

        Toast.makeText(requireContext(), "已恢复默认样式", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Theme restored to default");
    }

    /**
     * 显示圆角裁剪设置确认对话框
     */
    private void showClipConfirmationDialog(int stickerIndex, boolean clipToRoundedCorners) {
        Sticker sticker = currentTheme.getStickers().get(stickerIndex);
        String message = clipToRoundedCorners ?
            "启用圆角裁剪后,贴纸将被裁剪以适应CardView的圆角。\n\n确定要启用吗?" :
            "禁用圆角裁剪后,贴纸将完整显示,可能会超出CardView的圆角边界。\n\n确定要禁用吗?";

        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("圆角裁剪设置")
            .setMessage(message)
            .setPositiveButton("确定", (dialog, which) -> {
                // 更新贴纸的裁剪设置
                sticker.setClipToRoundedCorners(clipToRoundedCorners);

                // 更新编辑器显示
                stickerEditorView.setStickers(currentTheme.getStickers());

                // 更新图层列表
                if (layerManagerBottomSheet != null) {
                    layerManagerBottomSheet.updateStickers(currentTheme.getStickers());
                }

                Log.d(TAG, "Clip to rounded corners changed for sticker " + stickerIndex + ": " + clipToRoundedCorners);
            })
            .setNegativeButton("取消", null)
            .show();
    }
}