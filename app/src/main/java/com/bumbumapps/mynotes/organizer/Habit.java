package com.bumbumapps.mynotes.organizer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/** A habit the user wants to repeat every day. */
@Entity(tableName = "habits")
public class Habit implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name = "";

    /** Emoji shown in the habit's icon square. */
    @ColumnInfo(name = "icon")
    public String icon = "\uD83C\uDF31";

    /** Index into {@link HabitColors}. */
    @ColumnInfo(name = "color")
    public int color;

    /** Day (yyyyMMdd) the habit was created; days before it never count as missed. */
    @ColumnInfo(name = "start_day")
    public int startDay;
}
