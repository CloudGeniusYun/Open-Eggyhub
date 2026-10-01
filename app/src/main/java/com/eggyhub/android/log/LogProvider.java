package com.eggyhub.android.log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LogProvider extends ContentProvider {
    public static final String AUTHORITY = "com.eggyhub.android.logprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/logs");

    private LogDao logDao;

    @Override
    public boolean onCreate() {
        logDao = LogDatabase.getDatabase(getContext()).logDao();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor = logDao.getAllLogsCursor();
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "vnd.android.cursor.dir/" + AUTHORITY + ".logs";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values != null) {
            LogEntity entity = new LogEntity(
                values.getAsLong("timestamp"),
                values.getAsString("level"),
                values.getAsString("tag"),
                values.getAsString("message")
            );
            logDao.insert(entity);
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return uri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int count;
        if (selection != null && selection.equals("id=?") && selectionArgs != null && selectionArgs.length > 0) {
            // 删除单条日志
            int id = Integer.parseInt(selectionArgs[0]);
            count = logDao.deleteLogById(id);
        } else {
            // 删除全部日志
            count = logDao.deleteAllLogs();
        }
        
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
