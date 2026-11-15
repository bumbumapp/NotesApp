package com.bumbumapps.mynotes.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.organizer.Task;
import com.bumbumapps.mynotes.organizer.TasksActivity;

/** Task reminders: one inexact alarm per task, shown on the same notification channel as note reminders. */
public final class TaskReminders {

    public static final String EXTRA_TASK_ID = "reminder_task_id";
    private static final int BASE = 1_000_000;

    private TaskReminders() {
    }

    private static PendingIntent intentFor(Context c, int taskId, int flags) {
        Intent i = new Intent(c, TaskReminderReceiver.class)
                .setAction("com.bumbumapps.mynotes.TASK_REMINDER")
                .putExtra(EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(c, BASE + taskId, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c, Task t) {
        if (t.done || t.reminderAt <= System.currentTimeMillis()) {
            cancel(c, t.id);
            return;
        }
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.reminderAt,
                    intentFor(c, t.id, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    public static void cancel(Context c, int taskId) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(intentFor(c, taskId, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    static void show(Context c, Task t) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(ReminderReceiver.CHANNEL,
                    c.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(c, TasksActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, BASE + t.id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, ReminderReceiver.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(c.getString(R.string.task_reminder_title))
                .setContentText(t.title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(t.title))
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(BASE + t.id, b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS denied: the reminder is silently dropped, the task stays in the list.
        }
    }
}
