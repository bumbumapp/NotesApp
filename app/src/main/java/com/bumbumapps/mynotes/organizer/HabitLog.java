package com.bumbumapps.mynotes.organizer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** One check-in: habit {@code habitId} was done on {@code day} (yyyyMMdd). */
@Entity(tableName = "habit_logs", primaryKeys = {"habit_id", "day"})
public class HabitLog {
    @ColumnInfo(name = "habit_id")
    public int habitId;

    @ColumnInfo(name = "day")
    public int day;

    public HabitLog() {
    }

    public HabitLog(int habitId, int day) {
        this.habitId = habitId;
        this.day = day;
    }
}
