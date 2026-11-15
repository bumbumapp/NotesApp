package com.bumbumapps.mynotes.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.organizer.OrganizerDatabase;
import com.bumbumapps.mynotes.organizer.Task;

import java.util.concurrent.Executors;

/** Alarms are lost on reboot or force-stop: put every pending reminder back. */
public class BootReceiver extends BroadcastReceiver {

    /** Idempotent; call off the main thread. */
    public static void rescheduleAll(Context app) {
        try {
            for (Note n : NotesDatabase.getNotesDatabase(app).noteDao().getNotesWithReminder()) {
                ReminderScheduler.schedule(app, n);
            }
            for (Task t : OrganizerDatabase.get(app).dao().getTasksWithReminder()) {
                TaskReminders.schedule(app, t);
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final PendingResult result = goAsync();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                rescheduleAll(app);
            } catch (RuntimeException ignored) {
            } finally {
                result.finish();
            }
        });
    }
}
