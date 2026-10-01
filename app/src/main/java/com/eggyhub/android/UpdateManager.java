package com.eggyhub.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.eggyhub.android.log.AppLogger;
import android.webkit.URLUtil;
import androidx.core.content.FileProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.eggyhub.android.utils.OkHttpClientFactory;

import com.eggyhub.android.BuildConfig;

import android.content.SharedPreferences;

 public class UpdateManager {
    
    private Context mContext;
    private static final String TAG = "UpdateManager"; // Add TAG for logging
    private static final String API_URL = "https://eggyhub.top/api/upgrade"; // 新增：定义API请求URL
     private static final String DOWNLOAD_URL = "https://eggyhub.top/static/app/Eggyhub.apk"; // 新增：定义APK下载URL

     private static final String PREF_NAME = "update_prefs";
     private static final String KEY_DOWNLOAD_PATH = "download_path";
     private static final String KEY_DOWNLOAD_VERSION = "download_version";

     private final Activity mActivity;
     private final Handler mMainHandler;

     private AlertDialog mUpdateDialog;
     private ProgressBar mProgressBar;
     private TextView mProgressTextView;
     private View mUpdateButton;
     private View mExitButton;
     private View mLayoutButtons;
     private View mLayoutProgress;

     private long mDownloadId = -1;
     private String mDownloadFilePath;
     private DownloadManager mDownloadManager;
     private int mDownloadVersion = -1;
     private int mLatestVersionCode = -1;
     private UpdateCallback mCallback;

     public interface UpdateCallback {
         void onFinish();
     }

     private static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";
     private final BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
             if (mDownloadId == id) {
                 if (mUpdateDialog != null && mUpdateDialog.isShowing()) {
                     queryDownloadProgress();
                 }
             }
         }
     };

     public UpdateManager(Context context) {
         mContext = context;
         mActivity = (Activity) context;
         mMainHandler = new Handler(Looper.getMainLooper());
         loadSavedDownloadInfo();
     }

     private void loadSavedDownloadInfo() {
         SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
         mDownloadFilePath = prefs.getString(KEY_DOWNLOAD_PATH, null);
         mDownloadVersion = prefs.getInt(KEY_DOWNLOAD_VERSION, -1);
     }

     private void clearSavedDownloadInfo() {
         SharedPreferences.Editor editor = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
         editor.remove(KEY_DOWNLOAD_PATH);
         editor.remove(KEY_DOWNLOAD_VERSION);
         editor.apply();
     }

     private void saveDownloadInfo(String filePath, int versionCode) {
         SharedPreferences.Editor editor = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
         editor.putString(KEY_DOWNLOAD_PATH, filePath);
         editor.putInt(KEY_DOWNLOAD_VERSION, versionCode);
         editor.apply();
     }

     public void checkForUpdate(UpdateCallback callback) {
         mCallback = callback;
         OkHttpClient client = OkHttpClientFactory.getSharedClient();
         Request request = new Request.Builder()
                 .url(API_URL) // 使用定义的API_URL
                 .get()
                 .build();

         client.newCall(request).enqueue(new Callback() {
             @Override
             public void onFailure(@NonNull Call call, @NonNull IOException e) {
                 mMainHandler.post(() -> {
                     Toast.makeText(mContext, "检查更新失败", Toast.LENGTH_SHORT).show();
                     if (mCallback != null) {
                         mCallback.onFinish();
                     }
                 });
             }

             @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        if (responseBody == null) return;
                        String responseData = responseBody.string();
                        Gson gson = new Gson();
                        UpdateInfo info = gson.fromJson(responseData, UpdateInfo.class);
                        
                        int currentVersionCode = BuildConfig.VERSION_CODE;
                        boolean isUsed = info.isUsed();
                        int usedVersion = info.getUsedVersion();

                        if (!isUsed) {
                            if (usedVersion == -1 || usedVersion == currentVersionCode) {
                                String notice = info.getNotice();
                                mMainHandler.post(() -> showUnavailableNoticeDialog(notice));
                                return;
                            }
                        }

                        int latestVersionCodeFromApi = info.getLatestVersionCode();
                        String updateMessage = info.getUpdateMessage();
                        boolean forceUpdate = info.isForceUpdate();
                        String downloadUrlFromApi = info.getDownloadUrl();
                        if (downloadUrlFromApi == null || downloadUrlFromApi.isEmpty()) {
                            downloadUrlFromApi = DOWNLOAD_URL;
                        }

                        if (mDownloadFilePath != null && mDownloadVersion >= latestVersionCodeFromApi && mDownloadVersion > currentVersionCode) {
                            File downloadedApk = new File(mDownloadFilePath);
                            if (downloadedApk.exists()) {
                                try {
                                    PackageInfo downloadedPackageInfo = mContext.getPackageManager().getPackageArchiveInfo(mDownloadFilePath, 0);
                                    if (downloadedPackageInfo != null && downloadedPackageInfo.versionCode >= latestVersionCodeFromApi) {
                                        AppLogger.d(TAG, "Found a higher or equal version APK locally downloaded, version: " + downloadedPackageInfo.versionCode);
                                        mLatestVersionCode = downloadedPackageInfo.versionCode;

                                        mMainHandler.post(() -> showUpdateDialog(updateMessage, mDownloadFilePath, forceUpdate));
                                        return;
                                    }

                                } catch (Exception e) {
                                    AppLogger.e(TAG, "Unexpected error checking downloaded APK: " + e.getMessage());
                                    clearSavedDownloadInfo();
                                }
                            } else {
                                AppLogger.d(TAG, "Saved download path points to a non-existent file, clearing info.");
                                clearSavedDownloadInfo();
                            }
                        }

                        if (latestVersionCodeFromApi > currentVersionCode) {
                            AppLogger.d(TAG, "New version available: " + latestVersionCodeFromApi + ", current: " + currentVersionCode);
                            mLatestVersionCode = latestVersionCodeFromApi;
                            String finalDownloadUrl = downloadUrlFromApi;
                            mMainHandler.post(() -> showUpdateDialog(updateMessage, finalDownloadUrl, forceUpdate));
                        } else {
                            mMainHandler.post(() -> {
                                if (mCallback != null) {
                                    mCallback.onFinish();
                                }
                            });
                        }

                    } else {
                        mMainHandler.post(() -> {
                            if (mCallback != null) {
                                mCallback.onFinish();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mMainHandler.post(() -> {
                        if (mCallback != null) {
                            mCallback.onFinish();
                        }
                    });
                }
            }
         });
     }

     public void checkForUpdate() {
         checkForUpdate(null);
     }

     private void showUnavailableNoticeDialog(String notice) {
        if (!(mContext instanceof Activity)) {
            AppLogger.e(TAG, "Context is not an Activity. Cannot show notice dialog.");
            return;
        }
        Activity activity = (Activity) mContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
        View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_update, null);
        builder.setView(dialogView);

        TextView titleTextView = dialogView.findViewById(R.id.tvUpdateTitle);
        TextView messageTextView = dialogView.findViewById(R.id.update_message);
        mUpdateButton = dialogView.findViewById(R.id.btn_update);
        mExitButton = dialogView.findViewById(R.id.btn_exit);
        mLayoutProgress = dialogView.findViewById(R.id.layout_progress);
        mLayoutButtons = dialogView.findViewById(R.id.layout_buttons);

        titleTextView.setText("系统通知");
        messageTextView.setText(notice);
        
        mUpdateButton.setVisibility(View.GONE);
        mLayoutProgress.setVisibility(View.GONE);
        
        if (mExitButton instanceof TextView) {
            ((TextView) mExitButton).setText("退出软件");
        }
        
        mExitButton.setOnClickListener(v -> {
            if (mContext instanceof Activity) {
                ((Activity) mContext).finishAffinity();
            }
            System.exit(0);
        });

        builder.setCancelable(false);
        mUpdateDialog = builder.create();
        
        // 设置背景透明以配合布局中的圆角/间距（如果需要的话，但布局本身有背景色）
        if (mUpdateDialog.getWindow() != null) {
            mUpdateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        mUpdateDialog.show();
    }

    private void showUpdateDialog(String updateMessage, String downloadUrl, boolean forceUpdate) {
        if (!(mContext instanceof Activity)) {
            AppLogger.e(TAG, "Context is not an Activity. Cannot show update dialog.");
            return;
        }
        Activity activity = (Activity) mContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_update, null);
        builder.setView(dialogView);

        TextView titleTextView = dialogView.findViewById(R.id.tvUpdateTitle);
        TextView messageTextView = dialogView.findViewById(R.id.update_message);
        mUpdateButton = dialogView.findViewById(R.id.btn_update);
        mExitButton = dialogView.findViewById(R.id.btn_exit);
        mProgressBar = dialogView.findViewById(R.id.update_progress);
        mProgressTextView = dialogView.findViewById(R.id.update_progress_text);
        mLayoutProgress = dialogView.findViewById(R.id.layout_progress);
        mLayoutButtons = dialogView.findViewById(R.id.layout_buttons);

        titleTextView.setText("发现新版本");
        messageTextView.setText(updateMessage);

        if (mExitButton instanceof TextView) {
            ((TextView) mExitButton).setText(forceUpdate ? "退出软件" : "稍后更新");
        }

        mUpdateButton.setOnClickListener(v -> {
            File localApkFile = new File(downloadUrl);
            if (localApkFile.exists() && downloadUrl.endsWith(".apk")) {
                AppLogger.d(TAG, "Attempting to install locally downloaded APK: " + downloadUrl);
                installApk(downloadUrl);
            } else if (downloadUrl != null && URLUtil.isValidUrl(downloadUrl) && downloadUrl.endsWith(".apk")) {
                AppLogger.d(TAG, "Starting download from URL: " + downloadUrl);
                mLayoutButtons.setVisibility(View.GONE);
                mLayoutProgress.setVisibility(View.VISIBLE);
                startDownload(downloadUrl);
            }
        });

        mExitButton.setOnClickListener(v -> {
            if (forceUpdate) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).finishAffinity();
                }
                System.exit(0);
            } else {
                mUpdateDialog.dismiss();
                if (mCallback != null) {
                    mCallback.onFinish();
                }
            }
        });

        builder.setCancelable(!forceUpdate);

        mUpdateDialog = builder.create();
        
        if (mUpdateDialog.getWindow() != null) {
            mUpdateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        mUpdateDialog.show();
    }

    private void startDownload(String downloadUrl) {
        String fileName = Uri.parse(downloadUrl).getLastPathSegment(); // 使用从URL中获取的文件名
        File apkFile = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        mDownloadFilePath = apkFile.getAbsolutePath();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setMimeType("application/vnd.android.package-archive");
        request.setTitle(mContext.getString(R.string.app_name) + "更新");
        request.setDescription("正在下载新的版本...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(apkFile)); // 使用应用私有目录
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadId = mDownloadManager.enqueue(request);
        AppLogger.d(TAG, "Download started with ID: " + mDownloadId + ", file name: " + fileName + ", path: " + mDownloadFilePath);

        ContextCompat.registerReceiver(mContext, mDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED);
        queryDownloadProgress();
    }

    private void installApk(String filePath) {
        AppLogger.d(TAG, "Calling installApk for path: " + filePath);
        File apkFile = new File(filePath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mContext.getPackageManager().canRequestPackageInstalls()) {
                AppLogger.d(TAG, "Can request package installs, proceeding with install.");
                performInstall(apkFile);
            } else {
                AppLogger.d(TAG, "Cannot request package installs, requesting permissions.");
                requestInstallPermissions(filePath);
            }
        } else {
            AppLogger.d(TAG, "Android version < O, proceeding with legacy install.");
            performInstall(apkFile);
        }
    }

    private void performInstall(File apkFile) {
        AppLogger.d(TAG, "Attempting to install APK: " + apkFile.getAbsolutePath());
        if (!apkFile.exists()) {
            AppLogger.e(TAG, "APK file not found: " + apkFile.getAbsolutePath());
            Toast.makeText(mContext, "安装包不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(mContext, FILE_PROVIDER_AUTHORITY, apkFile);
            AppLogger.d(TAG, "FileProvider URI: " + uri.toString());
        } else {
            uri = Uri.fromFile(apkFile);
            AppLogger.d(TAG, "Legacy URI: " + uri.toString());
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive");

        try {
            if (mContext instanceof Activity) {
                mContext.startActivity(intent);
            } else {
                // 如果上下文不是Activity，可以尝试通过创建一个新的任务栈来启动，但可能不理想
                // 或者直接通过其他方式通知用户进行手动安装
                // 这里我们假设通常是从Activity启动的，否则可能需要更复杂的处理
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
            // If you want to keep the dialog open after starting the install, comment out or remove this line.
            // if (mUpdateDialog != null) {
            //     mUpdateDialog.dismiss();
            // }
        } catch (Exception e) {
            AppLogger.e(TAG, "Error installing APK", e);
            Toast.makeText(mContext, "安装失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mContext.getPackageManager().canRequestPackageInstalls()) {
                    // 再次尝试安装
                    if (mDownloadFilePath != null) {
                        performInstall(new File(mDownloadFilePath));
                    } else {
                        Toast.makeText(mContext, "未找到下载文件路径，请重新检查更新", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mContext, "未授予安装未知应用的权限，无法安装", Toast.LENGTH_SHORT).show();
                    // If you want to keep the dialog open even if permissions are not granted, comment out or remove this line.
                    // if (mUpdateDialog != null) {
                    //     mUpdateDialog.dismiss();
                    // }
                }
            }
        }
    }

    private void requestInstallPermissions(String filePath) {
        mDownloadFilePath = filePath; // 保存当前安装路径，以便在权限授予后继续安装
        Toast.makeText(mContext, "请授予安装未知应用的权限", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
        ((Activity) mContext).startActivityForResult(intent, 1001);
    }

    private void queryDownloadProgress() {
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(mDownloadId);

        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                Cursor cursor = downloadManager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    switch (status) {
                        case DownloadManager.STATUS_RUNNING:
                            long bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            long bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            int progress = 0;
                            if (bytesTotal > 0) {
                                progress = (int) ((bytesDownloaded * 100) / bytesTotal);
                            }
                            final int currentProgress = progress;
                            mMainHandler.post(() -> {
                                mProgressBar.setProgress(currentProgress);
                                mProgressTextView.setText(String.format("%.1f%%", (float) currentProgress));
                            });
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            downloading = false;
                            mMainHandler.post(() -> {
                                mProgressBar.setProgress(100);
                                mProgressTextView.setText("100.0%");
                                AppLogger.d(TAG, "Download successful, file path: " + mDownloadFilePath);
                                if (mDownloadFilePath != null) {
                                    AppLogger.d(TAG, "Calling installApk for path: " + mDownloadFilePath);
                                    installApk(mDownloadFilePath);
                                    SharedPreferences.Editor editor = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
                                    editor.putString(KEY_DOWNLOAD_PATH, mDownloadFilePath);
                                    editor.putInt(KEY_DOWNLOAD_VERSION, mLatestVersionCode);
                                    editor.apply();
                                }
                            });
                            break;
                        case DownloadManager.STATUS_FAILED:
                            downloading = false;
                            mMainHandler.post(() -> {
                                Toast.makeText(mContext, "下载失败", Toast.LENGTH_SHORT).show();
                                if (mLayoutButtons != null) mLayoutButtons.setVisibility(View.VISIBLE);
                                if (mLayoutProgress != null) mLayoutProgress.setVisibility(View.GONE);
                            });
                            break;
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                            break;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        }).start();
    }

    public void unregisterReceiver() {
        try {
            mContext.unregisterReceiver(mDownloadReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }
}