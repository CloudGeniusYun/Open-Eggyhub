package com.eggyhub.android.player;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.eggyhub.android.log.AppLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * 动态加载视频播放器 dex 的管理器
 * 支持手动导入 dex APK 或从服务器下载
 */
public class DexLoader {

    private static final String TAG = "DexLoader";
    private static final String DEX_FILE_NAME = "video_player.dex.apk";
    private static final String NATIVE_LIB_DIR = "video_native_libs";
    private static final String PREF_NAME = "video_dex_prefs";
    private static final String KEY_DEX_VERSION = "dex_version";
    private static final int CURRENT_DEX_VERSION = 14;
    private static final String ENGINE_CLASS_NAME = "com.eggyhub.android.player.DexPlayerEngineImpl";

    private static IVideoPlayerEngine engineInstance = null;
    private static boolean initialized = false;
    private static Resources dexResources = null;
    private static Context dexContext = null;

    /**
     * 检查 dex APK 是否已导入
     */
    public static boolean isDexAvailable(Context context) {
        File dexFile = new File(context.getFilesDir(), DEX_FILE_NAME);
        return dexFile.exists() && dexFile.length() > 0;
    }

    /**
     * 从 Uri 导入 dex APK（用于文件选择器）
     */
    public static boolean importDexApk(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                AppLogger.e(TAG, "无法打开文件: " + uri);
                return false;
            }

            File dexFile = new File(context.getFilesDir(), DEX_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(dexFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.close();
            is.close();

            // 清理旧的 native 库目录，强制重新提取
            File nativeLibParent = new File(context.getFilesDir(), NATIVE_LIB_DIR);
            if (nativeLibParent.exists()) {
                deleteDirectory(nativeLibParent);
            }

            // 重置版本号，强制重新加载
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_DEX_VERSION, 0).apply();

            // 清理已加载的引擎实例
            engineInstance = null;
            initialized = false;

            AppLogger.i(TAG, "dex APK 导入成功: " + dexFile.getAbsolutePath() + ", 大小: " + dexFile.length());
            return true;

        } catch (Exception e) {
            AppLogger.e(TAG, "导入 dex APK 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从 InputStream 导入 dex APK（用于服务器下载）
     */
    public static boolean importDexApk(Context context, InputStream is) {
        try {
            File dexFile = new File(context.getFilesDir(), DEX_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(dexFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.close();
            is.close();

            // 清理旧的 native 库目录
            File nativeLibParent = new File(context.getFilesDir(), NATIVE_LIB_DIR);
            if (nativeLibParent.exists()) {
                deleteDirectory(nativeLibParent);
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_DEX_VERSION, 0).apply();

            engineInstance = null;
            initialized = false;

            AppLogger.i(TAG, "dex APK 导入成功，大小: " + dexFile.length());
            return true;

        } catch (Exception e) {
            AppLogger.e(TAG, "导入 dex APK 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取播放器引擎实例
     * 整个过程：复制 dex APK → 提取 so 库 → 创建 DexClassLoader → 加载引擎类
     */
    public static synchronized IVideoPlayerEngine getEngine(Context context) {
        if (engineInstance != null) {
            return engineInstance;
        }

        try {
            // 1. 确保 dex APK 已复制到内部存储
            File dexFile = ensureDexFile(context);
            if (dexFile == null) {
                AppLogger.e(TAG, "无法获取 dex 文件");
                return null;
            }

            // 2. 提取 native 库（.so 文件）
            File nativeLibDir = extractNativeLibs(context, dexFile);
            // DexClassLoader 需要具体的 ABI 目录，而不是父目录
            String libraryPath = null;
            if (nativeLibDir != null) {
                String abi = getPreferredAbi();
                if (abi != null) {
                    File abiDir = new File(nativeLibDir, abi);
                    if (abiDir.exists()) {
                        libraryPath = abiDir.getAbsolutePath();
                        AppLogger.d(TAG, "native 库路径: " + libraryPath);
                    }
                }
            }

            // 3. 创建 DexClassLoader
            File optimizedDir = context.getDir("dex_opt", Context.MODE_PRIVATE);

            DexClassLoader dexClassLoader = new DexClassLoader(
                    dexFile.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    libraryPath,
                    context.getClassLoader()
            );

            // 4. 创建能访问 dex APK 资源的 Resources
            dexResources = createDexResources(context, dexFile);
            dexContext = new DexContextWrapper(context, dexResources, dexClassLoader);

            // 5. 加载引擎实现类
            Class<?> engineClass = dexClassLoader.loadClass(ENGINE_CLASS_NAME);
            Constructor<?> constructor = engineClass.getConstructor();
            engineInstance = (IVideoPlayerEngine) constructor.newInstance();

            // 6. 通过反射调用 setDexContext（使用 dex ClassLoader 的 Context 类）
            try {
                Class<?> contextClass = dexClassLoader.loadClass("android.content.Context");
                Method setDexContextMethod = engineClass.getMethod("setDexContext", contextClass);
                setDexContextMethod.invoke(engineInstance, dexContext);
                AppLogger.d(TAG, "setDexContext 调用成功");
            } catch (NoSuchMethodException e) {
                AppLogger.w(TAG, "引擎没有 setDexContext 方法");
            } catch (Exception e) {
                AppLogger.w(TAG, "setDexContext 调用失败: " + e.getMessage());
            }

            AppLogger.i(TAG, "视频播放器引擎加载成功");
            initialized = true;
            return engineInstance;

        } catch (Exception e) {
            AppLogger.e(TAG, "加载视频播放器引擎失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取能访问 dex 资源的 Context
     */
    public static Context getDexContext() {
        return dexContext;
    }

    /**
     * 创建能访问 dex APK 资源的 Resources 对象
     * 只添加 dex APK 的资源路径（避免与主应用资源 ID 冲突）
     */
    private static Resources createDexResources(Context context, File dexFile) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            
            // 只添加 dex APK（GSYVideoPlayer 只需要它自己的资源）
            int cookie = (int) addAssetPath.invoke(assetManager, dexFile.getAbsolutePath());
            AppLogger.d(TAG, "addAssetPath cookie: " + cookie);
            
            if (cookie == 0) {
                AppLogger.e(TAG, "addAssetPath 失败，返回 cookie=0");
                return context.getResources();
            }

            Resources baseResources = context.getResources();
            return new Resources(
                    assetManager,
                    baseResources.getDisplayMetrics(),
                    baseResources.getConfiguration()
            );
        } catch (Exception e) {
            AppLogger.e(TAG, "创建 dex Resources 失败: " + e.getMessage());
            return context.getResources();
        }
    }

    /**
     * Context 包装器，使用 dex APK 的 Resources
     */
    private static class DexContextWrapper extends ContextWrapper {
        private final Resources dexResources;
        private final ClassLoader dexClassLoader;
        private LayoutInflater dexLayoutInflater;
        private Resources.Theme dexTheme;

        DexContextWrapper(Context base, Resources resources, ClassLoader classLoader) {
            super(base);
            this.dexResources = resources;
            this.dexClassLoader = classLoader;
            AppLogger.d(TAG, "DexContextWrapper 创建，Resources: " + resources);
        }

        @Override
        public Resources getResources() {
            return dexResources;
        }

        @Override
        public AssetManager getAssets() {
            return dexResources.getAssets();
        }

        @Override
        public Resources.Theme getTheme() {
            if (dexTheme == null) {
                dexTheme = dexResources.newTheme();
                // 使用一个简单的主题
                dexTheme.applyStyle(android.R.style.Theme_DeviceDefault_NoActionBar, true);
            }
            return dexTheme;
        }

        @Override
        public Object getSystemService(String name) {
            AppLogger.d(TAG, "getSystemService 被调用: " + name);
            if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
                if (dexLayoutInflater == null) {
                    AppLogger.d(TAG, "创建 dex LayoutInflater...");
                    LayoutInflater baseInflater = LayoutInflater.from(getBaseContext());
                    dexLayoutInflater = baseInflater.cloneInContext(this);
                    
                    // 通过反射强制设置 Factory2，绕过 AppCompat 的 View 转换
                    try {
                        // 先重置 mFactorySet
                        java.lang.reflect.Field factorySetField = LayoutInflater.class.getDeclaredField("mFactorySet");
                        factorySetField.setAccessible(true);
                        factorySetField.set(dexLayoutInflater, false);
                        AppLogger.d(TAG, "mFactorySet 重置成功");
                        
                        // 设置自定义 Factory2
                        dexLayoutInflater.setFactory2(new LayoutInflater.Factory2() {
                            @Override
                            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                                AppLogger.d(TAG, "Factory2 onCreateView: " + name);
                                return onCreateView(name, context, attrs);
                            }

                            @Override
                            public View onCreateView(String name, Context context, AttributeSet attrs) {
                                try {
                                    if (name.indexOf('.') == -1) {
                                        // 内置 View（SeekBar, TextView 等）
                                        AppLogger.d(TAG, "创建内置 View: " + name);
                                        return dexLayoutInflater.createView(name, "android.widget.", attrs);
                                    } else {
                                        // 自定义 View - 使用 dex ClassLoader 加载
                                        AppLogger.d(TAG, "创建自定义 View: " + name);
                                        Class<?> viewClass = dexClassLoader.loadClass(name);
                                        Constructor<?> constructor = viewClass.getConstructor(Context.class, AttributeSet.class);
                                        return (View) constructor.newInstance(context, attrs);
                                    }
                                } catch (Exception e) {
                                    AppLogger.e(TAG, "createView 失败: " + name + ", " + e.getMessage());
                                    return null;
                                }
                            }
                        });
                        AppLogger.d(TAG, "Factory2 设置成功");
                    } catch (Exception e) {
                        AppLogger.e(TAG, "设置 Factory2 失败: " + e.getMessage());
                    }
                }
                AppLogger.d(TAG, "返回 dex LayoutInflater");
                return dexLayoutInflater;
            }
            AppLogger.d(TAG, "返回 super.getSystemService");
            return super.getSystemService(name);
        }
    }

    /**
     * 确保 dex APK 文件存在于内部存储
     */
    private static File ensureDexFile(Context context) {
        File dexFile = new File(context.getFilesDir(), DEX_FILE_NAME);

        if (!dexFile.exists() || dexFile.length() == 0) {
            AppLogger.e(TAG, "dex APK 未导入，请先导入播放器模块");
            return null;
        }

        AppLogger.d(TAG, "dex APK 已存在: " + dexFile.getAbsolutePath() + ", 大小: " + dexFile.length());
        return dexFile;
    }

    /**
     * 从 dex APK 中提取 native 库（.so 文件）
     */
    private static File extractNativeLibs(Context context, File dexApkFile) {
        String abi = getPreferredAbi();
        if (abi == null) {
            AppLogger.e(TAG, "无法获取设备 ABI");
            return null;
        }

        File nativeLibDir = new File(context.getFilesDir(), NATIVE_LIB_DIR + "/" + abi);
        
        // 检查版本，如果版本更新了，清理旧的 native 库目录
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int savedVersion = prefs.getInt(KEY_DEX_VERSION, 0);
        if (nativeLibDir.exists() && savedVersion >= CURRENT_DEX_VERSION) {
            AppLogger.d(TAG, "native 库目录已存在，版本匹配: " + nativeLibDir.getAbsolutePath());
            return nativeLibDir.getParentFile();
        }

        // 版本不匹配或目录不存在，清理并重新提取
        if (nativeLibDir.exists()) {
            AppLogger.d(TAG, "清理旧的 native 库目录");
            deleteDirectory(nativeLibDir.getParentFile());
        }

        AppLogger.d(TAG, "开始提取 native 库，ABI: " + abi);

        try {
            if (!nativeLibDir.mkdirs()) return null;

            try (ZipFile zipFile = new ZipFile(dexApkFile)) {
                String prefix = "lib/" + abi + "/";
                boolean foundAny = false;

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    if (entryName.startsWith(prefix) && entryName.endsWith(".so")) {
                        String soFileName = entryName.substring(prefix.length());
                        File soFile = new File(nativeLibDir, soFileName);

                        try (InputStream is = zipFile.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(soFile)) {
                            byte[] buffer = new byte[8192];
                            int length;
                            while ((length = is.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                        soFile.setExecutable(true);
                        foundAny = true;
                        AppLogger.d(TAG, "提取 so: " + soFileName);
                    }
                }

                if (!foundAny) {
                    nativeLibDir.delete();
                    // 尝试 armeabi-v7a 作为备选
                    if (!abi.equals("armeabi-v7a")) {
                        return extractNativeLibsForAbi(context, dexApkFile, "armeabi-v7a");
                    }
                    return null;
                }
            }

            return nativeLibDir.getParentFile();

        } catch (IOException e) {
            AppLogger.e(TAG, "提取 native 库失败: " + e.getMessage());
            return null;
        }
    }

    private static File extractNativeLibsForAbi(Context context, File dexApkFile, String abi) {
        File nativeLibDir = new File(context.getFilesDir(), NATIVE_LIB_DIR + "/" + abi);
        if (nativeLibDir.exists()) return nativeLibDir.getParentFile();

        try {
            if (!nativeLibDir.mkdirs()) return null;
            try (ZipFile zipFile = new ZipFile(dexApkFile)) {
                String prefix = "lib/" + abi + "/";
                boolean found = false;
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(prefix) && entry.getName().endsWith(".so")) {
                        String soFileName = entry.getName().substring(prefix.length());
                        File soFile = new File(nativeLibDir, soFileName);
                        try (InputStream is = zipFile.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(soFile)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                        }
                        soFile.setExecutable(true);
                        found = true;
                    }
                }
                if (!found) {
                    nativeLibDir.delete();
                    return null;
                }
            }
            return nativeLibDir.getParentFile();
        } catch (IOException e) {
            return null;
        }
    }

    private static String getPreferredAbi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String[] abis = Build.SUPPORTED_ABIS;
            if (abis != null && abis.length > 0) {
                return abis[0];
            }
        }
        return Build.CPU_ABI;
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    public static boolean isLoaded() {
        return initialized && engineInstance != null;
    }

    public static synchronized void reset() {
        if (engineInstance != null) {
            try {
                engineInstance.destroy();
            } catch (Exception ignored) {}
            engineInstance = null;
        }
        initialized = false;
    }
}