package com.eggyhub.android;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class MapDetailDialog extends DialogFragment {

    private static final String ARG_MAP_ITEM = "map_item";

    public static MapDetailDialog newInstance(MapItem mapItem) {
        MapDetailDialog fragment = new MapDetailDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MAP_ITEM, mapItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_map_detail, container, false);

        ImageView mapImageView = view.findViewById(R.id.dialogMapImageView);
        TextView mapNameTextView = view.findViewById(R.id.dialogMapNameTextView);
        TextView ownerNameTextView = view.findViewById(R.id.dialogOwnerNameTextView);
        TextView mapIntroTextView = view.findViewById(R.id.dialogMapIntroTextView);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);
        MaterialButton jumpButton = view.findViewById(R.id.jumpButton);

        if (getArguments() != null) {
            MapItem mapItem = getArguments().getParcelable(ARG_MAP_ITEM);
            if (mapItem != null) {
                Glide.with(this)
                        .load(mapItem.getImageUrl())
                        .placeholder(android.R.color.darker_gray)
                        .into(mapImageView);
                mapNameTextView.setText(mapItem.getName());
                ownerNameTextView.setText("作者：" + mapItem.getOwnerName());
                mapIntroTextView.setText("地图描述：" + mapItem.getIntro());

                cancelButton.setOnClickListener(v -> dismiss());
                jumpButton.setOnClickListener(v -> {
                    // Perform the original action: copy map code and launch intent
                    ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("mapcode", mapItem.getMapCode());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Mapcode 已复制: " + mapItem.getMapCode(), Toast.LENGTH_SHORT).show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("ntes://game.mobile/party"));
                            if (intent.resolveActivity(v.getContext().getPackageManager()) != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                v.getContext().startActivity(intent);
                            } else {
                                Toast.makeText(v.getContext(), "无可响应应用", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(v.getContext(), "打开应用失败", Toast.LENGTH_SHORT).show();
                        }
                    }, 2000);
                    dismiss();
                });
            }
        }

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        // 设置对话框窗口背景为透明，避免圆角后面的白色背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // 设置对话框宽度为屏幕宽度的80%
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
        
        return dialog;
    }
}