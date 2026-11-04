package com.bumbumapps.mynotes.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumbumapps.mynotes.Activity.MainActivity;
import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.logic.Checklist;

import java.util.concurrent.Executors;

/** Shows the reminder notification for a note, then clears the reminder so its badge disappears. */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTE_ID = "reminder_note_id";
    static final String CHANNEL = "note_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final int id = intent.getIntExtra(EXTRA_NOTE_ID, -1);
        final PendingResult result = goAsync();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Note note = NotesDatabase.getNotesDatabase(app).noteDao().getNote(id);
                if (note != null && note.getReminderAt() > 0) {
                    show(app, note);
                    note.setReminderAt(0);
                    NotesDatabase.getNotesDatabase(app).noteDao().insertNote(note);
                }
            } catch (RuntimeException ignored) {
                // Nothing sensible to do from a background alarm.
            } finally {
                result.finish();
            }
        });
    }

    static void show(Context c, Note note) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                    c.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(c, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, note.getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, note.getId(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = note.getTitle() == null || note.getTitle().trim().isEmpty()
                ? c.getString(R.string.reminder_untitled) : note.getTitle();
        String body = note.getNoteType() == Note.TYPE_CHECKLIST
                ? Checklist.preview(Checklist.parse(note.getNoteText()), 4)
                : (note.getSubtitle() == null ? "" : note.getSubtitle());
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(note.getId(), b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS was denied: the reminder is silently dropped.
        }
    }
}
