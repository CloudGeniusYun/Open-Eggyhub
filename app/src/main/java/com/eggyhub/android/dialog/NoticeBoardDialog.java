package com.eggyhub.android.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.eggyhub.android.R;
import com.eggyhub.android.model.NoticeItem;
import com.eggyhub.android.utils.NoticeDataManager;
import com.eggyhub.android.utils.NoticeUtils;
import com.eggyhub.android.view.StrokeTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoticeBoardDialog extends Dialog {

    private static final String TAG = "NoticeBoardDialog";

    // ★ 设计尺寸（与 XML 中 dialogContainer 的尺寸一致）
    private static final float DESIGN_WIDTH_DP = 770f;
    private static final float DESIGN_HEIGHT_DP = 440f;

    private LinearLayout contentContainer;
    private FrameLayout dialogContainer;
    private FrameLayout outerContainer;
    private LinearLayout titlesContainer;
    private NestedScrollView contentScrollView;
    private ImageView scrollbarThumb;
    private LinearLayout scrollbarParent;
    private float thumbHeightPx = 0;

    private Typeface titleFont;
    private Typeface contentFont;

    private int[] titleItemIds = {
            R.id.titleItem1, R.id.titleItem2, R.id.titleItem3,
            R.id.titleItem4, R.id.titleItem5
    };
    private int[] titleBgIds = {
            R.id.titleBg1, R.id.titleBg2, R.id.titleBg3,
            R.id.titleBg4, R.id.titleBg5
    };
    private int[] titleContainerIds = {
            R.id.titleContainer1, R.id.titleContainer2, R.id.titleContainer3,
            R.id.titleContainer4, R.id.titleContainer5
    };

    private int[] titleLineIds = {
            R.id.line1, R.id.line2, R.id.line3,
            R.id.line4, R.id.line5
    };

    private List<NoticeItem> noticeItems = new ArrayList<>();
    private List<View> categoryViews = new ArrayList<>();

    private int currentSelectedIndex = 0;
    private boolean isManualScrolling = false;
    private int currentTextColor = Color.BLACK;

    // ★ 补偿加权比例
    private float textCompensationWeight = 0.9f;
    private float marginCompensationWeight = 1.2f;

    // ★ 保存原始值
    private Map<View, int[]> originalPaddings = new HashMap<>();
    private Map<View, int[]> originalMargins = new HashMap<>();
    private Map<TextView, Float> originalTextSizes = new HashMap<>();

    public NoticeBoardDialog(@NonNull Context context) {
        super(context);
    }

    /**
     * 设置字号补偿加权比例
     */
    public void setTextCompensationWeight(float weight) {
        this.textCompensationWeight = weight;
        if (isShowing()) {
            applyScaleAndCompensation();
        }
    }

    /**
     * 设置边距补偿加权比例
     */
    public void setMarginCompensationWeight(float weight) {
        this.marginCompensationWeight = weight;
        if (isShowing()) {
            applyScaleAndCompensation();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_notice_board);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0f);
        }

        initViews();

        findViewById(R.id.dialogContainer).getRootView().setOnClickListener(v -> {
            if (v.getId() != R.id.dialogContainer) {
                dismiss();
            }
        });

        loadNoticeData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        outerContainer.post(this::applyScaleAndCompensation);
    }

    // ========================================================================
    // ★ 缩放 + 补偿核心逻辑
    // ========================================================================

    private void applyScaleAndCompensation() {
        if (outerContainer == null) return;
        Context context = getContext();
        if (context == null) return;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        float designWidthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, DESIGN_WIDTH_DP, dm);
        float designHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, DESIGN_HEIGHT_DP, dm);

        // 目标占比（可调整整体大小）
        float targetWidthRatio = 1.0f;
        float targetHeightRatio = 1.0f;

        float scaleX = (screenWidth * targetWidthRatio) / designWidthPx;
        float scaleY = (screenHeight * targetHeightRatio) / designHeightPx;
        float scale = Math.min(scaleX, scaleY);
        scale = Math.max(scale, 0.5f);
        scale = Math.min(scale, 1.8f);

        Log.d(TAG, "Scale=" + scale + ", textWeight=" + textCompensationWeight +
                ", marginWeight=" + marginCompensationWeight);

        // 应用缩放
        outerContainer.setPivotX(outerContainer.getWidth() / 2f);
        outerContainer.setPivotY(outerContainer.getHeight() / 2f);
        outerContainer.setScaleX(scale);
        outerContainer.setScaleY(scale);

        // ★ 1. 补偿文字大小（先放大再缩放）
        compensateTextSize(scale);

        // ★ 2. 补偿边距
        compensateMarginsAndPaddings(scale);

        // ★ 3. 补偿描边（固定，不加权）
        compensateStroke(scale);
    }

    // ========================================================================
    // ★ 文字大小补偿
    // ========================================================================

    private void saveOriginalTextSizes(View view) {
        if (view == null) return;
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (!originalTextSizes.containsKey(tv)) {
                originalTextSizes.put(tv, tv.getTextSize());
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                saveOriginalTextSizes(group.getChildAt(i));
            }
        }
    }

    private void compensateTextSize(float scale) {
        if (originalTextSizes.isEmpty()) {
            saveOriginalTextSizes(dialogContainer);
        }

        float factor = (1f / scale) * textCompensationWeight;

        for (Map.Entry<TextView, Float> entry : originalTextSizes.entrySet()) {
            TextView tv = entry.getKey();
            float originalSize = entry.getValue();
            if (tv.getParent() != null) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize * factor);
            }
        }
    }

    // ========================================================================
    // ★ 边距补偿
    // ========================================================================

    private void saveOriginalMarginsAndPaddings(View view) {
        if (view == null) return;

        int[] padding = new int[]{
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        };
        originalPaddings.put(view, padding);

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            int[] margin = new int[]{
                    mlp.leftMargin,
                    mlp.topMargin,
                    mlp.rightMargin,
                    mlp.bottomMargin
            };
            originalMargins.put(view, margin);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                saveOriginalMarginsAndPaddings(group.getChildAt(i));
            }
        }
    }

    private void compensateMarginsAndPaddings(float scale) {
        float factor = (1f / scale) * marginCompensationWeight;

        for (Map.Entry<View, int[]> entry : originalPaddings.entrySet()) {
            View view = entry.getKey();
            int[] orig = entry.getValue();
            view.setPadding(
                    (int) (orig[0] * factor),
                    (int) (orig[1] * factor),
                    (int) (orig[2] * factor),
                    (int) (orig[3] * factor)
            );
        }

        for (Map.Entry<View, int[]> entry : originalMargins.entrySet()) {
            View view = entry.getKey();
            int[] orig = entry.getValue();
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.leftMargin = (int) (orig[0] * factor);
                mlp.topMargin = (int) (orig[1] * factor);
                mlp.rightMargin = (int) (orig[2] * factor);
                mlp.bottomMargin = (int) (orig[3] * factor);
                view.setLayoutParams(mlp);
            }
        }
    }

    // ========================================================================
    // ★ 描边补偿（固定 1/scale，不加权）
    // ========================================================================

    private void compensateStroke(float scale) {
        float factor = 1f / scale;
        traverseAndSetStrokeFactor(dialogContainer, factor);
    }

    private void traverseAndSetStrokeFactor(View view, float factor) {
        if (view instanceof StrokeTextView) {
            ((StrokeTextView) view).setStrokeCompensationFactor(factor);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseAndSetStrokeFactor(group.getChildAt(i), factor);
            }
        }
    }

    // ========================================================================
    // ★ 原有初始化与业务逻辑
    // ========================================================================

    private void initViews() {
        loadFonts();

        contentContainer = findViewById(R.id.contentContainer);
        dialogContainer = findViewById(R.id.dialogContainer);
        outerContainer = findViewById(R.id.outerContainer);
        titlesContainer = findViewById(R.id.titlesContainer);
        contentScrollView = findViewById(R.id.contentScrollView);
        scrollbarThumb = findViewById(R.id.scrollbarThumb);

        // ========== 滚动条初始化 ==========
        if (scrollbarThumb != null) {
            ViewGroup parent = (ViewGroup) scrollbarThumb.getParent();
            if (parent instanceof LinearLayout) {
                scrollbarParent = (LinearLayout) parent;
                scrollbarParent.setGravity(Gravity.TOP);
                scrollbarParent.setPadding(0, 0, 0, 0);
                scrollbarParent.setClipChildren(false);
                scrollbarParent.setClipToPadding(false);
            }

            ViewGroup.LayoutParams lp = scrollbarThumb.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                float density = getContext().getResources().getDisplayMetrics().density;
                llp.width = (int) (4 * density);
                llp.height = (int) (21.7f * density);
                llp.topMargin = 0;
                llp.bottomMargin = 0;
                llp.leftMargin = 0;
                llp.rightMargin = 0;
                llp.gravity = Gravity.TOP;
                scrollbarThumb.setLayoutParams(llp);
                thumbHeightPx = llp.height;
            }

            scrollbarThumb.setY(0);
            scrollbarThumb.setVisibility(View.VISIBLE);
        }

        applyFonts();

        ImageButton btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        // ★ 保存原始边距
        saveOriginalMarginsAndPaddings(dialogContainer);

        initCategoryButtons();
        setupScrollListener();
        updateButtonStyle(0);

        contentScrollView.post(() -> updateScrollbarPosition(0));
    }

    private void updateScrollbarPosition(int scrollY) {
        if (scrollbarThumb == null || contentScrollView == null || contentContainer == null || scrollbarParent == null) {
            return;
        }

        int contentHeight = contentContainer.getHeight();
        int scrollViewHeight = contentScrollView.getHeight();
        int maxScroll = contentHeight - scrollViewHeight;
        if (maxScroll <= 0) {
            scrollbarThumb.setVisibility(View.VISIBLE);
            scrollbarThumb.setY(0);
            return;
        }

        float ratio = (float) scrollY / maxScroll;
        int parentHeight = scrollbarParent.getHeight() - scrollbarParent.getPaddingTop() - scrollbarParent.getPaddingBottom();
        float available = parentHeight - thumbHeightPx - 10;
        float y = ratio * available;
        if (y < 0) y = 0;
        if (y > available) y = available;
        y += scrollbarParent.getPaddingTop();

        scrollbarThumb.setY(y);
        scrollbarThumb.setVisibility(View.VISIBLE);
    }

    private void loadFonts() {
        try {
            titleFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/hkxzyt_w9_gb.ttf");
            contentFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/huawenyuantibold.ttf");
        } catch (Exception e) {
            e.printStackTrace();
            titleFont = Typeface.DEFAULT;
            contentFont = Typeface.DEFAULT;
        }
    }

    private void applyFonts() {
        StrokeTextView mainTitleView = findViewById(R.id.mainTitleView);
        if (mainTitleView != null) mainTitleView.setTypeface(titleFont);
        for (int id : titleItemIds) {
            TextView tv = findViewById(id);
            if (tv != null) tv.setTypeface(titleFont);
        }
    }

    private void initCategoryButtons() {
        for (int i = 0; i < titleItemIds.length; i++) {
            final int index = i;
            TextView tv = findViewById(titleItemIds[i]);
            if (tv != null) {
                tv.setOnClickListener(v -> {
                    isManualScrolling = true;
                    updateButtonStyle(index);
                    scrollToCategory(index);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> isManualScrolling = false, 500);
                });
            }
        }
    }

    private void updateButtonStyle(int selectedIndex) {
        currentSelectedIndex = selectedIndex;
        for (int i = 0; i < titleItemIds.length; i++) {
            TextView tv = findViewById(titleItemIds[i]);
            if (tv == null) continue;
            tv.setPadding(62, 15, tv.getPaddingRight(), tv.getPaddingBottom());
            FrameLayout container = findViewById(titleContainerIds[i]);
            if (container != null) container.setMinimumHeight(62);
            ImageView bg = findViewById(titleBgIds[i]);
            if (i == selectedIndex) {
                if (bg != null) bg.setVisibility(View.VISIBLE);
                tv.setTextSize(13f);
                tv.setGravity(Gravity.CENTER);
            } else {
                if (bg != null) bg.setVisibility(View.GONE);
                tv.setTextSize(10f);
                tv.setGravity(Gravity.CENTER);
            }
        }
    }

    private void scrollToCategory(int categoryIndex) {
        if (contentScrollView == null || categoryIndex < 0 || categoryIndex >= categoryViews.size()) return;
        View target = categoryViews.get(categoryIndex);
        if (target == null) return;
        contentScrollView.post(() -> {
            int offset = target.getTop() - contentContainer.getPaddingTop();
            contentScrollView.smoothScrollTo(0, Math.max(0, offset));
        });
    }

    private void setupScrollListener() {
        if (contentScrollView == null) return;
        contentScrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            updateScrollbarPosition(scrollY);
            if (isManualScrolling) return;

            int currentIndex = 0;
            int[] svLoc = new int[2];
            v.getLocationOnScreen(svLoc);
            int top = svLoc[1];
            for (int i = categoryViews.size() - 1; i >= 0; i--) {
                View view = categoryViews.get(i);
                if (view != null) {
                    int[] viewLoc = new int[2];
                    view.getLocationOnScreen(viewLoc);
                    if (viewLoc[1] <= top + 50) {
                        currentIndex = i;
                        break;
                    }
                }
            }
            if (currentIndex != currentSelectedIndex) {
                currentSelectedIndex = currentIndex;
                updateButtonStyle(currentIndex);
            }
        });
    }

    // ===== 数据加载 =====
    private void loadNoticeData() {
        NoticeDataManager dataManager = NoticeDataManager.getInstance();
        new Thread(() -> {
            dataManager.waitForLoad();
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (contentContainer != null) {
                        contentContainer.removeAllViews();
                        contentContainer.requestLayout();
                        contentContainer.invalidate();
                    }
                    if (dataManager.isLoaded()) {
                        noticeItems = dataManager.getCachedNoticeItems();
                        displayNoticeContent();
                        updateCategoryTitles();
                    } else {
                        TextView errorText = new TextView(getContext());
                        errorText.setText("加载公告失败，请检查网络连接");
                        errorText.setTextColor(Color.RED);
                        errorText.setTextSize(16);
                        errorText.setPadding(32, 32, 32, 32);
                        if (contentContainer != null) {
                            contentContainer.addView(errorText);
                        }
                    }
                });
            }
        }).start();
    }

    private void updateCategoryTitles() {
        String[] defaultTitles = {"维护公告", "玩法更新", "活动更新", "乐园更新", "调整优化"};
        int categoryCount = noticeItems.size() - 1;
        if (categoryCount > 5) categoryCount = 5;

        for (int i = 0; i < titleItemIds.length; i++) {
            TextView tv = findViewById(titleItemIds[i]);
            ImageView line = null;
            if (i < titleLineIds.length) {
                line = findViewById(titleLineIds[i]);
            }

            if (i < categoryCount) {
                tv.setVisibility(View.VISIBLE);
                int dataIndex = i + 1;
                if (dataIndex < noticeItems.size()) {
                    tv.setText(noticeItems.get(dataIndex).getTitle());
                } else if (i < defaultTitles.length) {
                    tv.setText(defaultTitles[i]);
                }

                if (line != null) {
                    if (i < categoryCount - 1) {
                        line.setVisibility(View.VISIBLE);
                    } else {
                        line.setVisibility(View.GONE);
                    }
                }
            } else {
                tv.setVisibility(View.GONE);
                if (line != null) {
                    line.setVisibility(View.GONE);
                }
            }
        }
    }

    private void displayNoticeContent() {
        if (contentContainer == null || noticeItems.isEmpty()) return;
        if (contentContainer.getChildCount() > 0) {
            contentContainer.removeAllViews();
            contentContainer.requestLayout();
        }
        categoryViews.clear();

        if (!noticeItems.isEmpty()) {
            NoticeItem first = noticeItems.get(0);
            StrokeTextView mainTitle = findViewById(R.id.mainTitleView);
            if (mainTitle != null) mainTitle.setText(first.getTitle());
            for (String content : first.getContent()) {
                if (content == null || content.trim().isEmpty()) continue;
                if (NoticeUtils.containsImage(content)) addSubtitle(content);
                else addContentText(content);
            }
        }

        for (int i = 1; i < noticeItems.size(); i++) {
            NoticeItem item = noticeItems.get(i);
            addCategoryTitle(item, i - 1);
            for (String content : item.getContent()) {
                if (content == null || content.trim().isEmpty()) continue;
                if (NoticeUtils.containsImage(content)) addSubtitle(content);
                else addContentText(content);
            }
        }
    }

    private void addCategoryTitle(NoticeItem item, int index) {
        FrameLayout container = new FrameLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = index == 0 ? 2 : 26;
        params.bottomMargin = 8;
        container.setLayoutParams(params);

        String bgPath = item.getTitleBgPath();
        if (bgPath != null && !bgPath.isEmpty()) {
            String drawableName = NoticeUtils.pathToDrawableName(bgPath);
            if (drawableName != null) {
                ImageView bgImg = new ImageView(getContext());
                FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                bgImg.setLayoutParams(bgParams);
                bgImg.setAdjustViewBounds(true);
                bgImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int resId = getContext().getResources().getIdentifier(drawableName, "drawable", getContext().getPackageName());
                if (resId != 0) bgImg.setImageResource(resId);
                container.addView(bgImg);
            }
        }

        StrokeTextView titleView = new StrokeTextView(getContext());
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        titleView.setLayoutParams(textParams);
        titleView.setText(item.getTitle());
        titleView.setTextColor(Color.parseColor("#ef5a09"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 27);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(titleFont);
        titleView.setStrokeColor(Color.WHITE);
        titleView.setStrokeWidth(8);
        container.addView(titleView);
        contentContainer.addView(container);
        categoryViews.add(container);
    }

    private void addSubtitle(String content) {
        FrameLayout container = new FrameLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 12;
        params.bottomMargin = 0;
        container.setLayoutParams(params);

        String imagePath = NoticeUtils.extractImagePath(content);
        String drawableName = NoticeUtils.pathToDrawableName(imagePath);
        if (drawableName != null) {
            ImageView bgImg = new ImageView(getContext());
            FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    (int) (27 * getContext().getResources().getDisplayMetrics().density));
            bgParams.gravity = Gravity.START;
            bgParams.leftMargin = 5;
            bgImg.setLayoutParams(bgParams);
            bgImg.setAdjustViewBounds(true);
            bgImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int resId = getContext().getResources().getIdentifier(drawableName, "drawable", getContext().getPackageName());
            if (resId != 0) bgImg.setImageResource(resId);
            container.addView(bgImg);
        }

        String text = NoticeUtils.extractText(content);
        StrokeTextView subtitleView = new StrokeTextView(getContext());
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        subtitleView.setLayoutParams(textParams);
        subtitleView.setText(text);
        subtitleView.setTextColor(Color.WHITE);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        subtitleView.setGravity(Gravity.CENTER_VERTICAL);
        subtitleView.setPadding(25, 9, 10, 8);
        subtitleView.setTypeface(contentFont);
        subtitleView.setStrokeColor(Color.parseColor("#ff6825"));
        subtitleView.setStrokeWidth(5);
        container.addView(subtitleView);
        contentContainer.addView(container);
    }

    private void addContentText(String content) {
        String processedContent = content.replaceAll("([。，、！？；：”’])[^<>]*>", "\u200B$1")
                .replaceAll("<", "\u200B<")
                .replaceAll(">", "\u200B>");

        TextView textView = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 8;
        params.bottomMargin = 20;
        params.leftMargin = 15;
        params.rightMargin = 31;
        textView.setLayoutParams(params);

        List<NoticeUtils.TextPart> parts = NoticeUtils.parseColoredText(processedContent);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (!parts.isEmpty()) {
            for (NoticeUtils.TextPart part : parts) {
                SpannableString sp = new SpannableString(part.getText());
                if (part.getColor() != Color.BLACK) currentTextColor = part.getColor();
                sp.setSpan(new ForegroundColorSpan(currentTextColor), 0, sp.length(), 0);
                builder.append(sp);
            }
        } else {
            SpannableString sp = new SpannableString(processedContent);
            sp.setSpan(new ForegroundColorSpan(currentTextColor), 0, sp.length(), 0);
            builder.append(sp);
        }

        NoticeUtils.replaceLinks(builder, processedContent, getContext(), currentTextColor);

        textView.setText(builder);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.95f);
        textView.setLineSpacing(5, 1);
        textView.setTypeface(contentFont);
        contentContainer.addView(textView);
    }

    // ========================================================================
    // ★ 确保原始终字号在内容动态添加后也被保存
    // ========================================================================

    @Override
    public void show() {
        super.show();
        // 确保动态添加的 TextView 的字号也被保存
        // 实际上，在 applyScaleAndCompensation 中会统一保存和补偿
    }
}