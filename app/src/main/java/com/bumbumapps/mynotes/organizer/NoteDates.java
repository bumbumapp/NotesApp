package com.bumbumapps.mynotes.organizer;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Notes store their creation time as display text ("Friday, 13 November 2026 14:05"); this turns it back
 * into a day for the calendar. Unparseable text simply yields 0 (the note is not shown on a day). Unit tested.
 */
public final class NoteDates {
    private NoteDates() {
    }

    private static final String[] PATTERNS = {
            "EEEE, dd MMMM yyyy HH:mm", "EEEE, dd MMMM yyyy h:mm a"};

    public static int dayOf(String text, TimeZone tz) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        Locale[] locales = {Locale.getDefault(), Locale.ENGLISH};
        for (Locale l : locales) {
            for (String p : PATTERNS) {
                SimpleDateFormat f = new SimpleDateFormat(p, l);
                f.setTimeZone(tz);
                f.setLenient(false);
                ParsePosition pos = new ParsePosition(0);
                Date d = f.parse(text.trim(), pos);
                if (d != null && pos.getIndex() == text.trim().length()) {
                    return DayKey.of(d.getTime(), tz);
                }
            }
        }
        return 0;
    }
}
