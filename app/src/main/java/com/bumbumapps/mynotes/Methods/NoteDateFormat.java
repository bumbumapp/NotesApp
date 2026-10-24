package com.bumbumapps.mynotes.Methods;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Timestamp shown on a note: 24-hour or 12-hour clock following the device setting. */
public final class NoteDateFormat {
    private NoteDateFormat() {
    }

    /** "HH:mm a" mixed both clocks and showed e.g. "15:48 PM". */
    public static String pattern(boolean is24Hour) {
        return is24Hour ? "EEEE, dd MMMM yyyy HH:mm" : "EEEE, dd MMMM yyyy h:mm a";
    }

    public static String format(Date date, boolean is24Hour, Locale locale) {
        return new SimpleDateFormat(pattern(is24Hour), locale).format(date);
    }

    public static String now(Context context) {
        return format(new Date(), android.text.format.DateFormat.is24HourFormat(context), Locale.getDefault());
    }
}
