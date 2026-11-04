package com.bumbumapps.mynotes.logic;

import com.bumbumapps.mynotes.entities.Note;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Home list rules: search, category / pinned / checklist filters and pinned-first ordering. Unit tested. */
public final class NoteFilter {

    /** Filter values that are not user categories. */
    public static final String ALL = "\u0000all";
    public static final String PINNED = "\u0000pinned";
    public static final String CHECKLISTS = "\u0000checklists";
    public static final String REMINDERS = "\u0000reminders";

    private NoteFilter() {
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    public static boolean matches(Note n, String filter) {
        if (filter == null || ALL.equals(filter)) {
            return true;
        }
        if (PINNED.equals(filter)) {
            return n.getPinned() == 1;
        }
        if (CHECKLISTS.equals(filter)) {
            return n.getNoteType() == Note.TYPE_CHECKLIST;
        }
        if (REMINDERS.equals(filter)) {
            return n.getReminderAt() > 0;
        }
        return filter.equalsIgnoreCase(n.getCategory() == null ? "" : n.getCategory().trim());
    }

    public static boolean matchesQuery(Note n, String query) {
        String q = lower(query).trim();
        if (q.isEmpty()) {
            return true;
        }
        return lower(n.getTitle()).contains(q) || lower(n.getSubtitle()).contains(q)
                || lower(n.getNoteText()).contains(q) || lower(n.getCategory()).contains(q);
    }

    /** Pinned notes first, then newest (highest id) first. */
    public static List<Note> apply(List<Note> all, String query, String filter) {
        List<Note> out = new ArrayList<>();
        for (Note n : all) {
            if (matches(n, filter) && matchesQuery(n, query)) {
                out.add(n);
            }
        }
        Collections.sort(out, new Comparator<Note>() {
            @Override
            public int compare(Note a, Note b) {
                if (a.getPinned() != b.getPinned()) {
                    return b.getPinned() - a.getPinned();
                }
                return b.getId() - a.getId();
            }
        });
        return out;
    }

    /** Distinct user categories, alphabetical, case-insensitively de-duplicated. */
    public static List<String> categories(List<Note> all) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (Note n : all) {
            String c = n.getCategory() == null ? "" : n.getCategory().trim();
            if (!c.isEmpty() && seen.add(c.toLowerCase(Locale.ROOT))) {
                out.add(c);
            }
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** Strips control characters and caps the length of a typed category name. */
    public static String cleanCategory(String raw) {
        if (raw == null) {
            return "";
        }
        String c = raw.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("\\s+", " ");
        return c.length() > 24 ? c.substring(0, 24).trim() : c;
    }
}
