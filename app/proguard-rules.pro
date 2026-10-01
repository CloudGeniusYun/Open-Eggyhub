# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# =============================================================================
# 1. Android 系统组件与基础配置
# =============================================================================

# 如果使用 WebView 与 JS 交互，请取消注释并填写接口类
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# 保留行号信息，方便 Crash 堆栈定位（生产环境可选移除）
-keepattributes SourceFile,LineNumberTable

# 开启极致混淆属性：保留泛型签名、异常、内部类结构等
# Signature: 对 Gson 等泛型解析至关重要
# RuntimeVisibleAnnotations: Gson 运行时读取 @SerializedName 必需
-keepattributes Signature,Exceptions,InnerClasses,AnnotationDefault,EnclosingMethod,RuntimeVisibleAnnotations

# 混淆策略配置
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# 移除 Log 日志（生产环境安全建议）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# =============================================================================
# 2. 第三方库通用规则
# =============================================================================

# --- Gson ---
# 保留 Gson 序列化所需的字段注解
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# 保留 TypeToken，防止泛型擦除导致解析失败
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Retrofit (通常会自动包含，但手动添加更保险) ---
# 保留 API 接口定义，防止方法名被混淆导致请求失败
-keep interface com.eggyhub.android.api.** { *; }
# 保留 Retrofit 注解
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# --- Glide (如果有用到) ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# =============================================================================
# 3. 项目特定规则 (Project Specific)
# =============================================================================

# --- Native (JNI) ---
# 保留包含 native 方法的类，防止 UnsatisfiedLinkError
-keepclasseswithmembernames class * {
    native <methods>;
}
# 显式保留 SignatureUtils
-keep class com.eggyhub.android.utils.SignatureUtils { *; }

# --- Data Models (Bean/Entity) ---
# 保留数据模型类，防止 Gson 解析失败
# 1. 位于 model 包下的所有类
-keep class com.eggyhub.android.model.** { *; }
# 2. 根包下以 Item/Entity/Response/Request 结尾的类
-keep class com.eggyhub.android.*Item { *; }
-keep class com.eggyhub.android.*Entity { *; }
-keep class com.eggyhub.android.*Response { *; }
-keep class com.eggyhub.android.*Response$* { *; }
-keep class com.eggyhub.android.*Request { *; }
-keep class com.eggyhub.android.*Request$* { *; }
-keep class com.eggyhub.android.*Data { *; }

# 3. 其他特定的数据模型类
-keep class com.eggyhub.android.LoginCredentials { *; }
-keep class com.eggyhub.android.VideoCategory { *; }
-keep class com.eggyhub.android.UpdateInfo { *; }
# 3. 特殊的内部类数据模型
-keep class com.eggyhub.android.ShareCodeAdapterForOverlay$ShareCodeItem { *; }
-keep class com.eggyhub.android.MangerActivity$FileResponse { *; }
-keep class com.eggyhub.android.MangerActivity$FileData { *; }
-keep class com.eggyhub.android.MangerActivity$RepoData { *; }

# 5. 保留 BiliApiService 及其返回类型
-keep interface com.eggyhub.android.api.BiliApiService { *; }
-keep class com.eggyhub.android.model.bili.** { *; }

# 6. 保留 Retrofit 的 Call 和 Callback
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# --- 四大组件 (Manifest 中注册的类) ---
# 尽管 R8 通常会自动处理，但为了安全起见，显式保留
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# --- 资源引用 ---
# 保持 R 文件中的静态字段，防止资源 ID 找不到
-keepclassmembers class **.R$* {
    public static <fields>;
}

# =============================================================================
# 8. GSYVideoPlayer 混淆规则
# =============================================================================
-keep class com.shuyu.gsyvideoplayer.video.** { *; }
-keep class com.shuyu.gsyvideoplayer.video.base.** { *; }
-keep class com.shuyu.gsyvideoplayer.utils.** { *; }
-keep class tv.danmaku.ijk.media.player.** { *; }
-keep interface com.shuyu.gsyvideoplayer.listener.** { *; }
-keep class com.shuyu.gsyvideoplayer.builder.** { *; }
-keep class com.shuyu.gsyvideoplayer.** { *; }
-dontwarn tv.danmaku.ijk.media.player.**
-dontwarn com.shuyu.gsyvideoplayer.**