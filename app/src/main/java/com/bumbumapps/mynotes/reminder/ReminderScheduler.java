package com.bumbumapps.mynotes.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.bumbumapps.mynotes.entities.Note;

/** Schedules one inexact alarm per note (no exact-alarm permission needed). */
public final class ReminderScheduler {

    private ReminderScheduler() {
    }

    private static PendingIntent intentFor(Context c, int noteId, int flags) {
        Intent i = new Intent(c, ReminderReceiver.class)
                .setAction("com.bumbumapps.mynotes.REMINDER")
                .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId);
        return PendingIntent.getBroadcast(c, noteId, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c, Note note) {
        if (note.getReminderAt() <= System.currentTimeMillis()) {
            cancel(c, note.getId());
            return;
        }
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, note.getReminderAt(),
                intentFor(c, note.getId(), PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void cancel(Context c, int noteId) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(intentFor(c, noteId, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }
}
