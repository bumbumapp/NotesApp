package com.bumbumapps.mynotes.reminder;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.logic.ReminderPresets;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;

/** The "set reminder" dialog shared by the home list and the checklist editor. */
public final class ReminderUi {

    public static final int REQUEST_NOTIFICATIONS = 77;

    private ReminderUi() {
    }

    /** @param note a note that is already saved (has an id); it is updated in place */
    public static void show(final Activity activity, final Note note, final Runnable onChanged) {
        final long now = System.currentTimeMillis();
        final TimeZone tz = TimeZone.getDefault();
        final List<String> labels = new ArrayList<>();
        final List<Long> times = new ArrayList<>(); // -1 = pick, -2 = remove
        labels.add(activity.getString(R.string.reminder_in_hour));
        times.add(ReminderPresets.inOneHour(now));
        long evening = ReminderPresets.thisEvening(now, tz);
        if (evening > 0) {
            labels.add(activity.getString(R.string.reminder_evening));
            times.add(evening);
        }
        labels.add(activity.getString(R.string.reminder_tomorrow));
        times.add(ReminderPresets.tomorrowMorning(now, tz));
        labels.add(activity.getString(R.string.reminder_pick));
        times.add(-1L);
        if (note.getReminderAt() > 0) {
            labels.add(activity.getString(R.string.action_reminder_remove));
            times.add(-2L);
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.reminder_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    long t = times.get(which);
                    if (t == -1L) {
                        pick(activity, note, onChanged);
                    } else if (t == -2L) {
                        apply(activity, note, 0, onChanged);
                    } else {
                        apply(activity, note, t, onChanged);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void pick(final Activity activity, final Note note, final Runnable onChanged) {
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, 1);
        new DatePickerDialog(activity, (v, y, m, day) ->
                new TimePickerDialog(activity, (tv, h, min) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, day, h, min, 0);
                    chosen.set(Calendar.MILLISECOND, 0);
                    apply(activity, note, chosen.getTimeInMillis(), onChanged);
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                        android.text.format.DateFormat.is24HourFormat(activity)).show(),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static void apply(final Activity activity, final Note note, final long when, final Runnable onChanged) {
        if (when > 0 && !ReminderPresets.isFuture(when, System.currentTimeMillis())) {
            Toast.makeText(activity, R.string.reminder_past, Toast.LENGTH_SHORT).show();
            return;
        }
        note.setReminderAt(when);
        final android.content.Context app = activity.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            NotesDatabase.getNotesDatabase(app).noteDao().insertNote(note);
            if (when > 0) {
                ReminderScheduler.schedule(app, note);
            } else {
                ReminderScheduler.cancel(app, note.getId());
            }
            activity.runOnUiThread(() -> {
                if (activity.isFinishing()) {
                    return;
                }
                if (when > 0) {
                    Toast.makeText(activity, activity.getString(R.string.reminder_set,
                            DateUtils.formatDateTime(activity, when, DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH)),
                            Toast.LENGTH_SHORT).show();
                    ensureNotifications(activity);
                } else {
                    Toast.makeText(activity, R.string.reminder_removed, Toast.LENGTH_SHORT).show();
                }
                if (onChanged != null) {
                    onChanged.run();
                }
            });
        });
    }

    /** Asks for the optional notification permission; a denied answer is explained, never blocking. */
    private static void ensureNotifications(Activity activity) {
        if (NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else {
            Toast.makeText(activity, R.string.reminder_no_notifications, Toast.LENGTH_LONG).show();
        }
    }

    /** Call from onRequestPermissionsResult. */
    public static void onPermissionResult(Activity activity, int requestCode, int[] results) {
        if (requestCode == REQUEST_NOTIFICATIONS && results.length > 0
                && results[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, R.string.reminder_no_notifications, Toast.LENGTH_LONG).show();
        }
    }
}
