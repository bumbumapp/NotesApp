package com.bumbumapps.mynotes.organizer;

import java.util.List;

/** Journal mood helpers. Unit tested. */
public final class Moods {
    private Moods() {
    }

    /** Level 1 (awful) to 5 (great). */
    public static final String[] FACES = {"😞", "😕", "😐", "🙂", "😄"};

    public static String face(int mood) {
        return FACES[clamp(mood) - 1];
    }

    public static int clamp(int mood) {
        return Math.max(1, Math.min(5, mood));
    }

    /** Mood of each of the last seven days (oldest first), 0 where there is no entry. */
    public static int[] lastWeek(List<JournalEntry> entries, int today) {
        int[] out = new int[7];
        for (int i = 0; i < 7; i++) {
            int day = DayKey.addDays(today, i - 6);
            for (JournalEntry e : entries) {
                if (e.day == day) {
                    out[i] = clamp(e.mood);
                    break;
                }
            }
        }
        return out;
    }

    public static String preview(String text, int max) {
        if (text == null) {
            return "";
        }
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() <= max ? s : s.substring(0, max - 1).trim() + "…";
    }
}
