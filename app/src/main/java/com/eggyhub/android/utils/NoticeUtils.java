package com.eggyhub.android.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公告工具类
 */
public class NoticeUtils {

    // ===================== 缓存正则表达式（性能优化） =====================
    private static final Pattern COLOR_PATTERN = Pattern.compile("#c([0-9a-fA-F]{6})");
    private static final Pattern LINK_PATTERN = Pattern.compile("#f\\(([^)]*)\\)(.*?)#l");

    /**
     * 将游戏内图片路径转换为drawable资源ID
     */
    public static String pathToDrawableName(String path) {
        if (path == null || path.isEmpty()) return null;
        String[] parts = path.split("/");
        if (parts.length > 0) {
            String fileName = parts[parts.length - 1];
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) return fileName.substring(0, dotIndex);
            return fileName;
        }
        return null;
    }

    /**
     * 解析颜色代码
     */
    public static int parseColorCode(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return Color.BLACK;
        try {
            String colorStr = colorCode;
            if (colorStr.startsWith("#c")) colorStr = "#" + colorStr.substring(2);
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            e.printStackTrace();
            return Color.BLACK;
        }
    }

    /**
     * 检查文本是否包含图片标记
     */
    public static boolean containsImage(String text) {
        return text != null && text.contains("#image#");
    }

    /**
     * 提取图片路径
     */
    public static String extractImagePath(String text) {
        if (text == null || !text.contains("#image#")) return null;
        String[] parts = text.split("#image#");
        if (parts.length > 1) return parts[1].trim();
        return null;
    }

    /**
     * 提取纯文本（移除图片标记）
     */
    public static String extractText(String text) {
        if (text == null) return "";
        return text.split("#image#")[0].trim();
    }

    /**
     * 解析带颜色标记的文本（原方法，保持不变）
     * 优化：使用缓存的 Pattern
     */
    public static List<TextPart> parseColoredText(String text) {
        List<TextPart> parts = new ArrayList<>();
        if (text == null || text.isEmpty()) return parts;

        // ★ 使用缓存的 Pattern
        Matcher matcher = COLOR_PATTERN.matcher(text);
        int lastEnd = 0;
        int currentColor = Color.BLACK;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String content = text.substring(lastEnd, matcher.start()).trim();
                if (!content.isEmpty()) parts.add(new TextPart(content, currentColor));
            }
            String colorCode = matcher.group(1);
            currentColor = parseColorCode("#" + colorCode);
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            String content = text.substring(lastEnd).trim();
            if (!content.isEmpty()) parts.add(new TextPart(content, currentColor));
        }
        return parts;
    }

    // ===================== 链接替换工具（保留原有颜色方案） =====================

    /**
     * 在已有的 SpannableStringBuilder 中替换 #f(...)...#l 为可点击链接
     * @param builder 已包含颜色样式的 SpannableStringBuilder
     * @param originalText 原始字符串（用于定位标记位置）
     * @param context 上下文
     * @param defaultColor 默认颜色（当链接未指定颜色时使用）
     */
    public static void replaceLinks(SpannableStringBuilder builder, String originalText, Context context, int defaultColor) {
        String fullText = builder.toString();
        // ★ 使用缓存的 Pattern
        Matcher matcher = LINK_PATTERN.matcher(fullText);
        List<LinkReplacement> replacements = new ArrayList<>();

        while (matcher.find()) {
            String params = matcher.group(1);
            String urlText = matcher.group(2);
            int start = matcher.start();
            int end = matcher.end();

            // 解析颜色参数
            String colorStr = extractParam(params, "c");
            int color;
            try {
                color = Color.parseColor("#" + colorStr);
            } catch (Exception e) {
                color = defaultColor;
            }
            replacements.add(new LinkReplacement(start, end, urlText, color));
        }

        // 从后往前替换，避免位置偏移
        for (int i = replacements.size() - 1; i >= 0; i--) {
            LinkReplacement r = replacements.get(i);
            SpannableString linkSpan = new SpannableString(r.text);
            linkSpan.setSpan(new ForegroundColorSpan(r.color), 0, r.text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            linkSpan.setSpan(new UnderlineSpan(), 0, r.text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            linkSpan.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(r.text));
                    context.startActivity(intent);
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(r.color);
                    ds.setUnderlineText(true);
                }
            }, 0, r.text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.replace(r.start, r.end, linkSpan);
        }
    }

    /**
     * 从参数字符串中提取指定键的值（格式：key:value|...）
     * 优化：使用 split 替代 Pattern，避免每次编译
     */
    private static String extractParam(String params, String key) {
        String[] pairs = params.split("\\|");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    /**
     * 内部类：链接替换信息
     */
    private static class LinkReplacement {
        int start, end;
        String text;
        int color;
        LinkReplacement(int start, int end, String text, int color) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.color = color;
        }
    }

    // ===================== 原有内部类 TextPart，保持不变 =====================

    /**
     * 文本部分（包含文本和颜色）
     */
    public static class TextPart {
        private String text;
        private int color;

        public TextPart(String text, int color) {
            this.text = text;
            this.color = color;
        }

        public String getText() { return text; }
        public int getColor() { return color; }
    }
}