package com.bumbumapps.mynotes.organizer;

import java.util.List;
import java.util.Set;

/** Percent of habits done on each of the last seven days. Unit tested. */
public final class WeekChart {
    private WeekChart() {
    }

    /** @return 7 values 0..100, oldest day first, last entry is {@code today} */
    public static int[] weekPercent(List<Habit> habits, List<HabitLog> logs, int today) {
        int[] out = new int[7];
        for (int i = 0; i < 7; i++) {
            int day = DayKey.addDays(today, i - 6);
            int active = 0, done = 0;
            for (Habit h : habits) {
                if (h.startDay != 0 && h.startDay > day) {
                    continue;
                }
                active++;
                for (HabitLog l : logs) {
                    if (l.habitId == h.id && l.day == day) {
                        done++;
                        break;
                    }
                }
            }
            out[i] = active == 0 ? 0 : Math.round(100f * done / active);
        }
        return out;
    }

    public static int doneOn(List<Habit> habits, Set<Integer> doneIds) {
        int n = 0;
        for (Habit h : habits) {
            if (doneIds.contains(h.id)) {
                n++;
            }
        }
        return n;
    }
}
