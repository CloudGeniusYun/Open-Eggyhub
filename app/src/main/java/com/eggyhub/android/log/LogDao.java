package com.eggyhub.android.log;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import android.database.Cursor;

@Dao
public interface LogDao {
    @Insert
    void insert(LogEntity log);

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    Cursor getAllLogsCursor();

    @Query("DELETE FROM logs")
    int deleteAllLogs();

    @Query("DELETE FROM logs WHERE id = :id")
    int deleteLogById(int id);
}
