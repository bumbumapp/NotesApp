package com.bumbumapps.mynotes.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import androidx.annotation.NonNull;

import com.bumbumapps.mynotes.NoteDao.NoteDao;
import com.bumbumapps.mynotes.entities.Note;

@Database(entities = Note.class, version = 2, exportSchema = false)
public abstract class NotesDatabase extends RoomDatabase {

    protected static NotesDatabase notesDatabase;

    public static synchronized NotesDatabase getNotesDatabase(Context context){
        if(notesDatabase == null){
            notesDatabase = Room.databaseBuilder(
                    context,
                    NotesDatabase.class,
                    "notes_db"
            ).addMigrations(MIGRATION_1_2).build();
        }
        return notesDatabase;
    }

    /** Adds pinning, categories, reminders and checklist type; existing notes keep their data. */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN category TEXT");
            db.execSQL("ALTER TABLE notes ADD COLUMN reminder_at INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN note_type INTEGER NOT NULL DEFAULT 0");
        }
    };

    public abstract NoteDao noteDao();
}
