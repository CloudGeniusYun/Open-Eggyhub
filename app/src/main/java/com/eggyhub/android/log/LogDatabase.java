package com.eggyhub.android.log;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LogEntity.class}, version = 1, exportSchema = false)
public abstract class LogDatabase extends RoomDatabase {
    public abstract LogDao logDao();
    private static volatile LogDatabase INSTANCE;

    public static LogDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (LogDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LogDatabase.class, "app_logs_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
