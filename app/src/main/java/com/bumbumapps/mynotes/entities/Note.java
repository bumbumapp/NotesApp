package com.bumbumapps.mynotes.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "notes")
public class Note implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "date_time")
    private String dateTime;

    @ColumnInfo(name = "subtitle")
    private String Subtitle;

    @ColumnInfo(name = "note_text")
    private String noteText;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "web_link")
    private String webLink;

    /** 1 when the note is pinned to the top of the list. */
    @ColumnInfo(name = "pinned", defaultValue = "0")
    private int pinned;

    /** Optional user category; null or empty means uncategorised. */
    @ColumnInfo(name = "category")
    private String category;

    /** Reminder time in epoch millis, 0 when none. */
    @ColumnInfo(name = "reminder_at", defaultValue = "0")
    private long reminderAt;

    /** 0 = rich text note, 1 = checklist. */
    @ColumnInfo(name = "note_type", defaultValue = "0")
    private int noteType;

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_CHECKLIST = 1;

    public int getPinned() {
        return pinned;
    }

    public void setPinned(int pinned) {
        this.pinned = pinned;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(long reminderAt) {
        this.reminderAt = reminderAt;
    }

    public int getNoteType() {
        return noteType;
    }

    public void setNoteType(int noteType) {
        this.noteType = noteType;
    }

    /** Editors rebuild a Note from their fields; this keeps the organising data they do not edit. */
    public void copyMetaFrom(Note other) {
        if (other == null) {
            return;
        }
        pinned = other.pinned;
        category = other.category;
        reminderAt = other.reminderAt;
        noteType = other.noteType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSubtitle() {
        return Subtitle;
    }

    public void setSubtitle(String subtitle) {
        Subtitle = subtitle;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }


    @NonNull
    @Override
    public String toString() {
        return title + " : " + dateTime;
    }
}
