package com.bumbumapps.mynotes.organizer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/** One diary entry per day (yyyyMMdd) with a mood from 1 (awful) to 5 (great). */
@Entity(tableName = "journal")
public class JournalEntry implements Serializable {
    @PrimaryKey
    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "mood")
    public int mood = 3;

    @ColumnInfo(name = "text")
    public String text = "";

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
