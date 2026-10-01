package com.eggyhub.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import com.eggyhub.android.log.AppLogger;

/**
 * 应用图标管理器
 * 用于动态切换应用桌面图标
 */
public class IconManager {

    private static final String TAG = "IconManager";

    // 对应 AndroidManifest.xml 中的 activity-alias 名称
    public static final String ICON_DEFAULT = "com.eggyhub.android.LauncherDefault";
    public static final String ICON_NEW_YEAR = "com.eggyhub.android.LauncherNewYear";
    public static final String ICON_WHITE = "com.eggyhub.android.LauncherWhite";
    public static final String ICON_DARK = "com.eggyhub.android.LauncherDark";
    public static final String ICON_SPONSOR1 = "com.eggyhub.android.LauncherSponsor1";
    public static final String ICON_SPONSOR2 = "com.eggyhub.android.LauncherSponsor2";
    public static final String ICON_SPONSOR3 = "com.eggyhub.android.LauncherSponsor3";
    public static final String ICON_SPONSOR4 = "com.eggyhub.android.LauncherSponsor4";

    /**
     * 切换应用图标
     * 采用“先禁用旧图标，再启用新图标”的顺序
     * @param context 上下文
     * @param targetAliasName 目标别名的全类名
     */
    public static void switchIcon(Context context, String targetAliasName) {
        PackageManager pm = context.getPackageManager();
        String[] allAliases = {
            ICON_DEFAULT, ICON_NEW_YEAR, ICON_WHITE, ICON_DARK,
            ICON_SPONSOR1, ICON_SPONSOR2, ICON_SPONSOR3, ICON_SPONSOR4
        };

        // 1. 先禁用所有其他图标（隐藏旧的）
        for (String alias : allAliases) {
            if (!alias.equals(targetAliasName)) {
                pm.setComponentEnabledSetting(
                        new ComponentName(context, alias),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
                AppLogger.d(TAG, "Disabled old icon alias: " + alias);
            }
        }

        // 2. 再启用目标图标（显示新的）
        pm.setComponentEnabledSetting(
                new ComponentName(context, targetAliasName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
        AppLogger.d(TAG, "Enabled target icon alias: " + targetAliasName);
        
        // 注意：切换后系统 Launcher 可能会有几秒钟的刷新延迟
    }

    /**
     * 获取当前启用的图标别名
     */
    public static String getCurrentIcon(Context context) {
        PackageManager pm = context.getPackageManager();
        String[] allAliases = {
            ICON_DEFAULT, ICON_NEW_YEAR, ICON_WHITE, ICON_DARK,
            ICON_SPONSOR1, ICON_SPONSOR2, ICON_SPONSOR3, ICON_SPONSOR4
        };
        
        for (String alias : allAliases) {
            try {
                if (pm.getComponentEnabledSetting(new ComponentName(context, alias)) 
                        == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return alias;
                }
            } catch (Exception e) {
                // 忽略不存在的组件
            }
        }
        return ICON_DEFAULT;
    }
}
