/*
                         _oo0oo_
                        o8888888o
                        88" . "88
                        (| -_- |)
                        0\  =  /0
                      ___/`---'\___
                    .' \\|     |// '.
                   / \\|||  :  |||// \
                  / _||||| -:- |||||- \
                 |   | \\\  - /// |   |
                 | \_|  ''\---/''  |_/ |
                 \  .-\__  '-'  ___/-. /
               ___'. .'  /--.--\  `. .'___
            ."" '<  `.___\_<|>_/___.' >' "".
           | | :  `- \`.;`\ _ /`;.`/ - ` : | |
           \  \ `_.   \_ __\ /__ _/   .-` /  /
       =====`-.____`.___ \_____/___.-`___.-'=====
                         `=---='

    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 
  佛祖保佑  Eggyhub稳定运行  不要出现bug
*/
package com.eggyhub.android;

import android.app.Application;
import android.content.SharedPreferences;
import com.eggyhub.android.log.AppLogger;
import com.eggyhub.android.log.CrashHandler;
import com.eggyhub.android.utils.SignatureUtils;
import com.eggyhub.android.utils.IntegrityChecker;
import com.eggyhub.android.utils.OkHttpClientFactory;
import android.util.Log;
import android.widget.Toast;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class EggyApp extends Application {
    public static boolean isRestricted = false;
    private static boolean signatureVerified = false;
    private static long lastVerifyTime = 0;
    private static final long VERIFY_INTERVAL = 5 * 60 * 1000;
    private static boolean securityCheckCompleted = false;
    private static final long USER_DATA_CACHE_DURATION = 60 * 60 * 1000; // 1 hour

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化 OkHttpClient 工厂
        OkHttpClientFactory.init(this);

        registerActivityLifecycleCallbacks(new SecurityInterceptor());

        // 初始化完整性检查器
        IntegrityChecker.initialize(this);

        // 执行安全检查
        performSecurityCheck();

        AppLogger.init(this);
        
        // 初始化安全存储管理器
        SecureStorageManager.init(this);
        
        CrashHandler.getInstance().init(this);
        
        AppLogger.i("EggyApp", "Application Created, Global Logger & CrashHandler Initialized");
        
        // 启动时获取用户数据（1 小时内不重复获取）
        fetchUserDataIfNeeded();
    }
    
    /**
     * 应用启动时获取用户数据（1 小时缓存）
     */
    private void fetchUserDataIfNeeded() {
        new Thread(() -> {
            try {
                SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                int userId = preferences.getInt("id", -1);
                
                if (userId == -1) {
                    AppLogger.d("EggyApp", "用户未登录，跳过数据获取");
                    return;
                }
                
                // 检查缓存时间
                long lastFetchTime = preferences.getLong("lastFetchTime", 0);
                long currentTime = System.currentTimeMillis();
                boolean shouldFetch = (currentTime - lastFetchTime) >= USER_DATA_CACHE_DURATION;
                
                if (shouldFetch) {
                    fetchUserData(userId);
                } else {
                    AppLogger.d("EggyApp", "使用缓存数据，距离上次刷新：" + (currentTime - lastFetchTime) / 1000 + "秒");
                }
            } catch (Exception e) {
                AppLogger.e("EggyApp", "获取用户数据异常：" + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 获取用户数据
     */
    private void fetchUserData(int userId) {
        try {
            // 获取创作者数据
            fetchCreatorData(userId);
            
            // 获取用户详细信息
            fetchUserProfile(userId);
            
            // 更新缓存时间
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            preferences.edit().putLong("lastFetchTime", System.currentTimeMillis()).apply();
        } catch (Exception e) {
            AppLogger.e("EggyApp", "获取用户数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取创作者数据
     */
    private void fetchCreatorData(int userId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/creator/data?id=" + userId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseData);
                
                if (jsonResponse.optInt("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    int publishedGifts = data.optInt("published_gifts", 0);
                    int totalLikes = data.optInt("total_likes", 0);
                    int contributedCodes = data.optInt("contributed_codes", 0);
                    
                    SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("cache_published_gifts", String.valueOf(publishedGifts));
                    editor.putString("cache_total_likes", String.valueOf(totalLikes));
                    editor.putString("cache_contributed_codes", String.valueOf(contributedCodes));
                    editor.apply();
                    
                    AppLogger.d("EggyApp", "创作者数据已更新：发布=" + publishedGifts + ", 获赞=" + totalLikes + ", 贡献=" + contributedCodes);
                }
            }
        } catch (Exception e) {
            AppLogger.e("EggyApp", "获取创作者数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户详细信息
     */
    private void fetchUserProfile(int userId) {
        OkHttpClient client = OkHttpClientFactory.getSharedClient();
        String url = "https://eggyhub.top/api/users/profile?id=" + userId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseData);
                
                if (jsonResponse.optInt("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    String avatar = data.optString("avatar", "");
                    String description = data.optString("description", "");
                    String eggyid = data.optString("eggyid", "");
                    
                    SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("avatar", avatar);
                    editor.putString("description", description);
                    editor.putString("eggyid", eggyid);
                    editor.apply();
                    
                    AppLogger.d("EggyApp", "用户资料已更新");
                }
            }
        } catch (Exception e) {
            AppLogger.e("EggyApp", "获取用户资料失败：" + e.getMessage());
        }
    }
    
    /**
     * 执行安全检查
    
    private void performSecurityCheck() {
        new Thread(() -> {
            try {
                // 1. 先检查是否有有效的授权码
                android.content.SharedPreferences securityPrefs = getSharedPreferences("eggy_security", MODE_PRIVATE);
                String savedAuthCode = securityPrefs.getString("auth_code", "");
                boolean hasValidAuth = false;
                
                if (!savedAuthCode.isEmpty()) {
                    String deviceId = SignatureUtils.getDeviceID(this);
                    hasValidAuth = SignatureUtils.verifyLicenseNative(this, deviceId, savedAuthCode);
                    AppLogger.d("EggyApp", "Auth code verification result: " + hasValidAuth);
                }
                
                // 2. 如果有有效的授权码，直接跳过安全检查
                if (hasValidAuth) {
                    AppLogger.d("EggyApp", "Valid auth code found, skipping security checks");
                    isRestricted = false;
                    securityCheckCompleted = true;
                    return;
                }
                
                // 3. 没有有效的授权码，执行安全检查
                // 检查签名完整性
                boolean signatureValid = verifySignature(this);
                AppLogger.d("EggyApp", "Signature verification result: " + signatureValid);
                
                // 检查文件完整性
                boolean integrityValid = IntegrityChecker.verifyAll(this);
                AppLogger.d("EggyApp", "File integrity verification result: " + integrityValid);
                
                // 如果任何检查失败，设置为受限模式
                if (!signatureValid || !integrityValid) {
                    AppLogger.e("EggyApp", "Security check failed, setting restricted mode");
                    isRestricted = true;
                } else {
                    isRestricted = false;
                }
                
                // 安全检查完成
                securityCheckCompleted = true;
                AppLogger.i("EggyApp", "Security check completed");
            } catch (Exception e) {
                AppLogger.e("EggyApp", "Security check failed: " + e.getMessage());
                isRestricted = true;
                securityCheckCompleted = true;
            }
        }).start();
    }*/
    
    /**
     * 检查安全检查是否已完成
     
    public static boolean isSecurityCheckCompleted() {
        return securityCheckCompleted;
    }
    
    public static boolean verifySignature(android.content.Context context) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVerifyTime > VERIFY_INTERVAL) {
            signatureVerified = SignatureUtils.checkSignatureNative(context);
            lastVerifyTime = currentTime;
        }
        
        return signatureVerified;
    }*/
}
