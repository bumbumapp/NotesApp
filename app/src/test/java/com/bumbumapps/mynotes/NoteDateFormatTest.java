package com.bumbumapps.mynotes;

import static org.junit.Assert.assertEquals;

import com.bumbumapps.mynotes.Methods.NoteDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.junit.Test;

/** Note timestamps showed a 24-hour time with AM/PM, e.g. "15:48 PM". */
public class NoteDateFormatTest {
    private static Date at1548() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 24, 15, 48, 0);
        return c.getTime();
    }

    @Test
    public void twentyFourHourClockHasNoAmPm() {
        assertEquals("Saturday, 24 October 2026 15:48", NoteDateFormat.format(at1548(), true, Locale.US));
    }

    @Test
    public void twelveHourClockUsesAmPm() {
        assertEquals("Saturday, 24 October 2026 3:48 PM", NoteDateFormat.format(at1548(), false, Locale.US));
    }
}
