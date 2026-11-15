package com.bumbumapps.mynotes.organizer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/** A to-do item with an optional due day (yyyyMMdd), priority and reminder. */
@Entity(tableName = "tasks")
public class Task implements Serializable {
    public static final int LOW = 0;
    public static final int NORMAL = 1;
    public static final int HIGH = 2;

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title = "";

    /** Due day as yyyyMMdd, 0 when the task has no date. */
    @ColumnInfo(name = "due_day")
    public int dueDay;

    @ColumnInfo(name = "priority")
    public int priority = NORMAL;

    @ColumnInfo(name = "done")
    public boolean done;

    /** Epoch millis of the reminder, 0 when none. */
    @ColumnInfo(name = "reminder_at")
    public long reminderAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
