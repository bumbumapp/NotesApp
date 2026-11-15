package com.bumbumapps.mynotes.organizer;

import java.util.Calendar;
import java.util.TimeZone;

/** Days are plain ints yyyyMMdd in the device time zone: easy to store, sort and compare. Unit tested. */
public final class DayKey {
    private DayKey() {
    }

    public static int of(long millis, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(millis);
        return of(c);
    }

    public static int of(Calendar c) {
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }

    public static int today() {
        return of(System.currentTimeMillis(), TimeZone.getDefault());
    }

    public static int of(int year, int month0, int day) {
        return year * 10000 + (month0 + 1) * 100 + day;
    }

    public static int year(int key) {
        return key / 10000;
    }

    /** 0-based month. */
    public static int month(int key) {
        return (key / 100) % 100 - 1;
    }

    public static int day(int key) {
        return key % 100;
    }

    public static Calendar toCalendar(int key, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.clear();
        c.set(year(key), month(key), day(key), 12, 0, 0);
        return c;
    }

    public static int addDays(int key, int delta) {
        Calendar c = toCalendar(key, TimeZone.getDefault());
        c.add(Calendar.DAY_OF_MONTH, delta);
        return of(c);
    }

    public static int addMonths(int key, int delta) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year(key), month(key), 1, 12, 0, 0);
        c.add(Calendar.MONTH, delta);
        return of(c);
    }

    public static boolean isValid(int key) {
        if (key < 10000101 || key > 99991231) {
            return false;
        }
        int m = month(key), d = day(key);
        if (m < 0 || m > 11 || d < 1) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year(key), m, 1);
        return d <= c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}
