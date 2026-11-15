package com.bumbumapps.mynotes.organizer;

import java.util.ArrayList;
import java.util.List;

/** Which tab a task shows in and how a list is ordered. Unit tested. */
public final class TaskLogic {
    private TaskLogic() {
    }

    public static final int TODAY = 0;
    public static final int UPCOMING = 1;
    public static final int ALL = 2;
    public static final int DONE = 3;

    public static boolean isOverdue(Task t, int today) {
        return !t.done && t.dueDay != 0 && t.dueDay < today;
    }

    public static boolean matches(Task t, int tab, int today) {
        switch (tab) {
            case TODAY:
                // Overdue tasks stay visible in Today until they are done or rescheduled.
                return !t.done && t.dueDay != 0 && t.dueDay <= today;
            case UPCOMING:
                return !t.done && (t.dueDay > today || t.dueDay == 0);
            case DONE:
                return t.done;
            default:
                return !t.done;
        }
    }

    public static List<Task> filter(List<Task> all, int tab, int today) {
        List<Task> out = new ArrayList<>();
        for (Task t : all) {
            if (matches(t, tab, today)) {
                out.add(t);
            }
        }
        return out;
    }

    public static int openToday(List<Task> all, int today) {
        return filter(all, TODAY, today).size();
    }

    public static int openCount(List<Task> all) {
        int n = 0;
        for (Task t : all) {
            if (!t.done) {
                n++;
            }
        }
        return n;
    }
}
