package com.bumbumapps.mynotes.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bumbumapps.mynotes.organizer.OrganizerDatabase;
import com.bumbumapps.mynotes.organizer.Task;

import java.util.concurrent.Executors;

/** Shows the notification for a task whose reminder time has come. */
public class TaskReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final int id = intent.getIntExtra(TaskReminders.EXTRA_TASK_ID, -1);
        final PendingResult result = goAsync();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Task t = OrganizerDatabase.get(app).dao().getTask(id);
                if (t != null && !t.done && t.reminderAt > 0) {
                    TaskReminders.show(app, t);
                    t.reminderAt = 0;
                    OrganizerDatabase.get(app).dao().saveTask(t);
                }
            } catch (RuntimeException ignored) {
                // Nothing sensible to do from a background alarm.
            } finally {
                result.finish();
            }
        });
    }
}
