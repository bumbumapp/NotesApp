package com.bumbumapps.mynotes.organizer;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Tasks, habits and journal live in their own database so the notes database stays untouched. */
@Database(entities = {Task.class, Habit.class, HabitLog.class, JournalEntry.class}, version = 1, exportSchema = false)
public abstract class OrganizerDatabase extends RoomDatabase {

    private static OrganizerDatabase instance;

    public static synchronized OrganizerDatabase get(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    OrganizerDatabase.class, "organizer_db").build();
        }
        return instance;
    }

    public abstract OrganizerDao dao();
}
