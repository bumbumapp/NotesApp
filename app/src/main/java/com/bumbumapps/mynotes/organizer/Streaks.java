package com.bumbumapps.mynotes.organizer;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** Streak maths shared by habits and the journal. Unit tested. */
public final class Streaks {
    private Streaks() {
    }

    /**
     * Consecutive days ending today; if today is not done yet the streak that ends yesterday still counts
     * (the day is not over), so it only resets after a full missed day.
     */
    public static int current(Set<Integer> days, int today) {
        int d = days.contains(today) ? today : DayKey.addDays(today, -1);
        int n = 0;
        while (days.contains(d)) {
            n++;
            d = DayKey.addDays(d, -1);
        }
        return n;
    }

    public static int best(Collection<Integer> days) {
        TreeSet<Integer> sorted = new TreeSet<>(days);
        int best = 0, run = 0;
        Integer prev = null;
        for (int d : sorted) {
            run = prev != null && DayKey.addDays(prev, 1) == d ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }
}
