package com.bumbumapps.mynotes.logic;

import java.util.Calendar;
import java.util.TimeZone;

/** Quick reminder times for the picker. All take "now" and a time zone so they are testable. Unit tested. */
public final class ReminderPresets {

    private ReminderPresets() {
    }

    public static long inOneHour(long now) {
        return now + 60L * 60 * 1000;
    }

    /** 20:00 today, or 0 when that is less than 15 minutes away or already past. */
    public static long thisEvening(long now, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, 20);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() - now >= 15L * 60 * 1000 ? c.getTimeInMillis() : 0;
    }

    public static long tomorrowMorning(long now, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** A reminder must be at least a few seconds ahead to ring. */
    public static boolean isFuture(long when, long now) {
        return when > now + 5_000L;
    }
}
