package com.eggyhub.android.log;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "logs")
public class LogEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long timestamp;
    public String level;
    public String tag;
    public String message;

    public LogEntity(long timestamp, String level, String tag, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.tag = tag;
        this.message = message;
    }
}
