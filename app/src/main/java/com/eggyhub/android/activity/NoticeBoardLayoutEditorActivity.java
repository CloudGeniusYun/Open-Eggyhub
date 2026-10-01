package com.eggyhub.android.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eggyhub.android.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

/**
 * 公告板布局编辑器 - 支持拖动和缩放
 */
public class NoticeBoardLayoutEditorActivity extends Activity {

    private FrameLayout editorCanvas;
    private LinearLayout controlsPanel;
    private EditText etWidth, etHeight, etX, etY;

    private DraggableScalableView dialogContainer;
    private DraggableScalableView titlesContainer;
    private DraggableScalableView contentContainer;
    private DraggableScalableView selectedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建主布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        mainLayout.setBackgroundColor(Color.WHITE);

        // 左侧：编辑画布
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        ));
        scrollView.setBackgroundColor(Color.parseColor("#F5F5F5"));

        editorCanvas = new FrameLayout(this);
        editorCanvas.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        scrollView.addView(editorCanvas);

        mainLayout.addView(scrollView);

        // 右侧：控制面板
        createControlsPanel(mainLayout);

        setContentView(mainLayout);

        // 创建可拖动和缩放的组件
        createDraggableComponents();

        // 添加导出按钮
        Button btnExport = new Button(this);
        btnExport.setText("导出布局数据");
        btnExport.setOnClickListener(v -> exportLayoutData());
        controlsPanel.addView(btnExport);
    }

    /**
     * 创建控制面板
     */
    private void createControlsPanel(LinearLayout parent) {
        ScrollView controlScroll = new ScrollView(this);
        controlScroll.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        ));

        controlsPanel = new LinearLayout(this);
        controlsPanel.setOrientation(LinearLayout.VERTICAL);
        controlsPanel.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        controlsPanel.setPadding(16, 16, 16, 16);
        controlsPanel.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // 标题
        TextView title = new TextView(this);
        title.setText("布局编辑器");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        controlsPanel.addView(title);

        // 说明
        TextView instruction = new TextView(this);
        instruction.setText("操作说明:\n- 点击组件选中\n- 拖动移动位置\n- 双指缩放大小");
        instruction.setTextSize(12);
        instruction.setTextColor(Color.GRAY);
        instruction.setPadding(0, 0, 0, 16);
        controlsPanel.addView(instruction);

        // 组件选择
        TextView tvSelect = new TextView(this);
        tvSelect.setText("选择组件:");
        tvSelect.setTextSize(14);
        controlsPanel.addView(tvSelect);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,
            new String[]{"弹窗容器", "标题区域", "内容区域"});
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectComponent(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        controlsPanel.addView(spinner);

        // 位置和尺寸
        TextView tvPos = new TextView(this);
        tvPos.setText("位置和尺寸:");
        tvPos.setTextSize(14);
        tvPos.setPadding(0, 16, 0, 8);
        controlsPanel.addView(tvPos);

        LinearLayout posLayout = new LinearLayout(this);
        posLayout.setOrientation(LinearLayout.HORIZONTAL);
        posLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        etX = new EditText(this);
        etX.setHint("X");
        etX.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etX.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        posLayout.addView(etX);

        etY = new EditText(this);
        etY.setHint("Y");
        etY.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etY.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        posLayout.addView(etY);

        controlsPanel.addView(posLayout);

        LinearLayout sizeLayout = new LinearLayout(this);
        sizeLayout.setOrientation(LinearLayout.HORIZONTAL);
        sizeLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        etWidth = new EditText(this);
        etWidth.setHint("宽");
        etWidth.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etWidth.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        sizeLayout.addView(etWidth);

        etHeight = new EditText(this);
        etHeight.setHint("高");
        etHeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etHeight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        sizeLayout.addView(etHeight);

        controlsPanel.addView(sizeLayout);

        // 应用按钮
        Button btnApply = new Button(this);
        btnApply.setText("应用数值");
        btnApply.setOnClickListener(v -> applyValues());
        controlsPanel.addView(btnApply);

        controlScroll.addView(controlsPanel);
        parent.addView(controlScroll);
    }

    /**
     * 创建可拖动和缩放的组件
     */
    private void createDraggableComponents() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        // 弹窗容器（绿色）
        dialogContainer = new DraggableScalableView(this);
        dialogContainer.setLayoutParams(new FrameLayout.LayoutParams(560, 700));
        dialogContainer.setX((screenWidth - 560) / 2f);
        dialogContainer.setY(50);
        dialogContainer.setBackgroundColor(Color.parseColor("#4CAF50"));
        dialogContainer.setTag("dialogContainer");
        dialogContainer.setOnViewSelectedListener(view -> {
            selectedView = view;
            updateControlValues();
        });
        editorCanvas.addView(dialogContainer);

        // 标题区域（蓝色，带分隔线和按钮背景）
        titlesContainer = new DraggableScalableView(this);
        titlesContainer.setLayoutParams(new FrameLayout.LayoutParams(100, 600));
        titlesContainer.setX(dialogContainer.getX() + 20);
        titlesContainer.setY(dialogContainer.getY() + 80);
        titlesContainer.setBackgroundColor(Color.parseColor("#2196F3"));
        titlesContainer.setTag("titlesContainer");
        titlesContainer.setOnViewSelectedListener(view -> {
            selectedView = view;
            updateControlValues();
        });

        // 添加标题内容示例
        LinearLayout titlesContent = new LinearLayout(this);
        titlesContent.setOrientation(LinearLayout.VERTICAL);
        titlesContent.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titlesContent.setPadding(8, 8, 8, 8);

        String[] titles = {"维护公告", "玩法更新", "活动更新", "乐园更新", "系统更新"};
        for (String title : titles) {
            // 标题按钮背景
            ImageView btnBg = new ImageView(this);
            btnBg.setImageResource(R.drawable.notice_btn_tab);
            btnBg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            btnBg.setAdjustViewBounds(true);

            FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            bgParams.setMargins(0, 4, 0, 4);
            btnBg.setLayoutParams(bgParams);

            FrameLayout btnFrame = new FrameLayout(this);
            btnFrame.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            btnFrame.addView(btnBg);

            TextView titleText = new TextView(this);
            titleText.setText(title);
            titleText.setTextColor(Color.BLACK);
            titleText.setTextSize(12);
            titleText.setGravity(Gravity.CENTER);
            titleText.setPadding(8, 8, 8, 8);
            btnFrame.addView(titleText);

            titlesContent.addView(btnFrame);

            // 分隔线
            ImageView divider = new ImageView(this);
            divider.setImageResource(R.drawable.img_notice_line);
            divider.setScaleType(ImageView.ScaleType.FIT_XY);
            divider.setAdjustViewBounds(true);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4
            );
            dividerParams.setMargins(0, 4, 0, 4);
            divider.setLayoutParams(dividerParams);
            titlesContent.addView(divider);
        }

        titlesContainer.setContentView(titlesContent);
        editorCanvas.addView(titlesContainer);

        // 内容区域（紫色，带滚动条）
        contentContainer = new DraggableScalableView(this);
        contentContainer.setLayoutParams(new FrameLayout.LayoutParams(400, 600));
        contentContainer.setX(dialogContainer.getX() + 140);
        contentContainer.setY(dialogContainer.getY() + 80);
        contentContainer.setBackgroundColor(Color.parseColor("#9C27B0"));
        contentContainer.setTag("contentContainer");
        contentContainer.setOnViewSelectedListener(view -> {
            selectedView = view;
            updateControlValues();
        });

        // 添加内容示例
        ScrollView contentScrollView = new ScrollView(this);
        contentScrollView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setPadding(8, 8, 8, 8);

        for (int i = 0; i < 5; i++) {
            TextView sectionTitle = new TextView(this);
            sectionTitle.setText(titles[i]);
            sectionTitle.setTextSize(16);
            sectionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            sectionTitle.setTextColor(Color.BLACK);
            sectionTitle.setPadding(0, 16, 0, 8);
            contentLayout.addView(sectionTitle);

            TextView sectionContent = new TextView(this);
            sectionContent.setText("这是 " + titles[i] + " 的内容。这里是示例文本，实际内容会从JSON数据中加载。");
            sectionContent.setTextSize(13);
            sectionContent.setTextColor(Color.DKGRAY);
            sectionContent.setLineSpacing(4f, 1f);
            sectionContent.setPadding(0, 0, 0, 16);
            contentLayout.addView(sectionContent);
        }

        contentScrollView.addView(contentLayout);
        contentContainer.setContentView(contentScrollView);
        editorCanvas.addView(contentContainer);
    }

    /**
     * 选择组件
     */
    private void selectComponent(int position) {
        switch (position) {
            case 0:
                selectedView = dialogContainer;
                break;
            case 1:
                selectedView = titlesContainer;
                break;
            case 2:
                selectedView = contentContainer;
                break;
        }
        if (selectedView != null) {
            selectedView.setSelected(true);
            updateControlValues();
        }
    }

    /**
     * 更新控制面板中的数值
     */
    private void updateControlValues() {
        if (selectedView != null) {
            etX.setText(String.valueOf((int) selectedView.getX()));
            etY.setText(String.valueOf((int) selectedView.getY()));
            etWidth.setText(String.valueOf(selectedView.getWidth()));
            etHeight.setText(String.valueOf(selectedView.getHeight()));
        }
    }

    /**
     * 应用数值
     */
    private void applyValues() {
        if (selectedView != null) {
            try {
                float x = Float.parseFloat(etX.getText().toString());
                float y = Float.parseFloat(etY.getText().toString());
                int width = Integer.parseInt(etWidth.getText().toString());
                int height = Integer.parseInt(etHeight.getText().toString());

                selectedView.setX(x);
                selectedView.setY(y);
                selectedView.setScale(width / (float) selectedView.getWidth(), height / (float) selectedView.getHeight());

                Toast.makeText(this, "已应用数值", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "请输入有效数值", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 导出布局数据
     */
    private void exportLayoutData() {
        try {
            JSONObject layoutData = new JSONObject();

            // 弹窗容器
            JSONObject dialogData = new JSONObject();
            dialogData.put("x", (int) dialogContainer.getX());
            dialogData.put("y", (int) dialogContainer.getY());
            dialogData.put("width", dialogContainer.getWidth());
            dialogData.put("height", dialogContainer.getHeight());
            layoutData.put("dialogContainer", dialogData);

            // 标题区域
            JSONObject titlesData = new JSONObject();
            titlesData.put("x", (int) titlesContainer.getX());
            titlesData.put("y", (int) titlesContainer.getY());
            titlesData.put("width", titlesContainer.getWidth());
            titlesData.put("height", titlesContainer.getHeight());
            layoutData.put("titlesContainer", titlesData);

            // 内容区域
            JSONObject contentData = new JSONObject();
            contentData.put("x", (int) contentContainer.getX());
            contentData.put("y", (int) contentContainer.getY());
            contentData.put("width", contentContainer.getWidth());
            contentData.put("height", contentContainer.getHeight());
            layoutData.put("contentContainer", contentData);

            // 保存到文件
            File file = new File(getExternalFilesDir(null), "notice_board_layout.json");
            FileWriter writer = new FileWriter(file);
            writer.write(layoutData.toString());
            writer.close();

            // 生成XML代码
            String xmlCode = generateXmlCode();

            // 显示对话框
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("布局数据已导出");
            builder.setMessage("文件: " + file.getAbsolutePath() + "\n\n生成的XML:\n\n" + xmlCode);
            builder.setPositiveButton("复制XML", (dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("layout_xml", xmlCode);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("关闭", null);
            builder.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 生成XML代码
     */
    private String generateXmlCode() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        xml.append("    android:layout_width=\"match_parent\"\n");
        xml.append("    android:layout_height=\"match_parent\"\n");
        xml.append("    android:background=\"#80000000\">\n\n");

        // 弹窗容器
        xml.append("    <FrameLayout\n");
        xml.append("        android:id=\"@+id/dialogContainer\"\n");
        xml.append("        android:layout_width=\"").append(dialogContainer.getWidth()).append("dp\"\n");
        xml.append("        android:layout_height=\"").append(dialogContainer.getHeight()).append("dp\"\n");
        xml.append("        android:layout_gravity=\"center\">\n\n");

        // 背景图片
        xml.append("        <ImageView\n");
        xml.append("            android:layout_width=\"match_parent\"\n");
        xml.append("            android:layout_height=\"match_parent\"\n");
        xml.append("            android:scaleType=\"fitCenter\"\n");
        xml.append("            android:src=\"@drawable/img_notice_bg\" />\n\n");

        // 标题区域
        xml.append("        <!-- 标题区域 -->\n");
        xml.append("        <LinearLayout\n");
        xml.append("            android:id=\"@+id/titlesContainer\"\n");
        xml.append("            android:layout_width=\"").append(titlesContainer.getWidth()).append("dp\"\n");
        xml.append("            android:layout_height=\"").append(titlesContainer.getHeight()).append("dp\"\n");
        xml.append("            android:layout_marginStart=\"").append((int) (titlesContainer.getX() - dialogContainer.getX())).append("dp\"\n");
        xml.append("            android:layout_marginTop=\"").append((int) (titlesContainer.getY() - dialogContainer.getY())).append("dp\"\n");
        xml.append("            android:orientation=\"vertical\"\n");
        xml.append("            android:padding=\"8dp\">\n");
        xml.append("        </LinearLayout>\n\n");

        // 内容区域
        xml.append("        <!-- 内容区域 -->\n");
        xml.append("        <ScrollView\n");
        xml.append("            android:id=\"@+id/contentScrollView\"\n");
        xml.append("            android:layout_width=\"").append(contentContainer.getWidth()).append("dp\"\n");
        xml.append("            android:layout_height=\"").append(contentContainer.getHeight()).append("dp\"\n");
        xml.append("            android:layout_marginStart=\"").append((int) (contentContainer.getX() - dialogContainer.getX())).append("dp\"\n");
        xml.append("            android:layout_marginTop=\"").append((int) (contentContainer.getY() - dialogContainer.getY())).append("dp\"\n");
        xml.append("            android:scrollbars=\"vertical\"\n");
        xml.append("            android:padding=\"12dp\">\n");
        xml.append("        </ScrollView>\n");

        xml.append("    </FrameLayout>\n");
        xml.append("</FrameLayout>\n");

        return xml.toString();
    }

    /**
     * 可拖动和缩放的自定义View
     */
    private class DraggableScalableView extends FrameLayout {
        private View contentView;
        private ScaleGestureDetector scaleDetector;
        private PointF lastTouchPoint = new PointF();
        private float scaleFactor = 1.0f;
        private Matrix matrix = new Matrix();
        private OnViewSelectedListener selectedListener;

        public DraggableScalableView(Context context) {
            super(context);
            scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            setBackgroundColor(Color.TRANSPARENT);
        }

        public void setContentView(View view) {
            if (contentView != null) {
                removeView(contentView);
            }
            contentView = view;
            addView(view);
        }

        public void setOnViewSelectedListener(OnViewSelectedListener listener) {
            this.selectedListener = listener;
        }

        public void setScale(float scaleX, float scaleY) {
            scaleFactor = Math.min(scaleX, scaleY);
            applyScale();
        }

        private void applyScale() {
            if (contentView != null) {
                contentView.setScaleX(scaleFactor);
                contentView.setScaleY(scaleFactor);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchPoint.set(event.getX(), event.getY());
                    if (selectedListener != null) {
                        selectedListener.onViewSelected(this);
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - lastTouchPoint.x;
                    float dy = event.getY() - lastTouchPoint.y;

                    setX(getX() + dx);
                    setY(getY() + dy);

                    lastTouchPoint.set(event.getX(), event.getY());
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
                applyScale();
                return true;
            }
        }
    }

    private interface OnViewSelectedListener {
        void onViewSelected(DraggableScalableView view);
    }
}