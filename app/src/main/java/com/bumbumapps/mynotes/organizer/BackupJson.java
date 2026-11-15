package com.bumbumapps.mynotes.organizer;

import com.bumbumapps.mynotes.entities.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Everything the app stores, as one JSON document, and the merge rules used when restoring it. Unit tested. */
public final class BackupJson {
    private BackupJson() {
    }

    public static final String MARKER = "bumbumapps.notes.backup";
    public static final int VERSION = 1;

    public static final class Snapshot {
        public final List<Note> notes = new ArrayList<>();
        public final List<Task> tasks = new ArrayList<>();
        public final List<Habit> habits = new ArrayList<>();
        public final List<HabitLog> logs = new ArrayList<>();
        public final List<JournalEntry> journal = new ArrayList<>();
    }

    private static String s(String v) {
        return v == null ? "" : v;
    }

    public static String toJson(Snapshot snap) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", MARKER);
        root.put("version", VERSION);
        JSONArray notes = new JSONArray();
        for (Note n : snap.notes) {
            JSONObject o = new JSONObject();
            o.put("title", s(n.getTitle()));
            o.put("dateTime", s(n.getDateTime()));
            o.put("subtitle", s(n.getSubtitle()));
            o.put("text", s(n.getNoteText()));
            o.put("color", s(n.getColor()));
            o.put("webLink", s(n.getWebLink()));
            o.put("pinned", n.getPinned());
            o.put("category", s(n.getCategory()));
            o.put("noteType", n.getNoteType());
            notes.put(o);
        }
        root.put("notes", notes);
        JSONArray tasks = new JSONArray();
        for (Task t : snap.tasks) {
            JSONObject o = new JSONObject();
            o.put("title", s(t.title));
            o.put("dueDay", t.dueDay);
            o.put("priority", t.priority);
            o.put("done", t.done);
            o.put("createdAt", t.createdAt);
            tasks.put(o);
        }
        root.put("tasks", tasks);
        JSONArray habits = new JSONArray();
        for (Habit h : snap.habits) {
            JSONObject o = new JSONObject();
            o.put("id", h.id);
            o.put("name", s(h.name));
            o.put("icon", s(h.icon));
            o.put("color", h.color);
            o.put("startDay", h.startDay);
            habits.put(o);
        }
        root.put("habits", habits);
        JSONArray logs = new JSONArray();
        for (HabitLog l : snap.logs) {
            JSONObject o = new JSONObject();
            o.put("habitId", l.habitId);
            o.put("day", l.day);
            logs.put(o);
        }
        root.put("habitLogs", logs);
        JSONArray journal = new JSONArray();
        for (JournalEntry e : snap.journal) {
            JSONObject o = new JSONObject();
            o.put("day", e.day);
            o.put("mood", e.mood);
            o.put("text", s(e.text));
            o.put("updatedAt", e.updatedAt);
            journal.put(o);
        }
        root.put("journal", journal);
        return root.toString(2);
    }

    /** @throws JSONException when the text is not a backup written by this app */
    public static Snapshot fromJson(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!MARKER.equals(root.optString("app")) || root.optInt("version", 0) < 1) {
            throw new JSONException("not a Notes backup");
        }
        Snapshot snap = new Snapshot();
        JSONArray notes = root.optJSONArray("notes");
        for (int i = 0; notes != null && i < notes.length(); i++) {
            JSONObject o = notes.getJSONObject(i);
            Note n = new Note();
            n.setTitle(o.optString("title"));
            n.setDateTime(o.optString("dateTime"));
            n.setSubtitle(o.optString("subtitle"));
            n.setNoteText(o.optString("text"));
            n.setColor(o.optString("color", "#333333"));
            n.setWebLink(o.optString("webLink"));
            n.setPinned(o.optInt("pinned"));
            String cat = o.optString("category");
            n.setCategory(cat.isEmpty() ? null : cat);
            n.setNoteType(o.optInt("noteType"));
            snap.notes.add(n);
        }
        JSONArray tasks = root.optJSONArray("tasks");
        for (int i = 0; tasks != null && i < tasks.length(); i++) {
            JSONObject o = tasks.getJSONObject(i);
            Task t = new Task();
            t.title = o.optString("title");
            t.dueDay = o.optInt("dueDay");
            t.priority = Math.max(Task.LOW, Math.min(Task.HIGH, o.optInt("priority", Task.NORMAL)));
            t.done = o.optBoolean("done");
            t.createdAt = o.optLong("createdAt");
            if (!t.title.trim().isEmpty()) {
                snap.tasks.add(t);
            }
        }
        JSONArray habits = root.optJSONArray("habits");
        for (int i = 0; habits != null && i < habits.length(); i++) {
            JSONObject o = habits.getJSONObject(i);
            Habit h = new Habit();
            h.id = o.optInt("id");
            h.name = o.optString("name");
            h.icon = o.optString("icon", h.icon);
            h.color = o.optInt("color");
            h.startDay = o.optInt("startDay");
            if (!h.name.trim().isEmpty()) {
                snap.habits.add(h);
            }
        }
        JSONArray logs = root.optJSONArray("habitLogs");
        for (int i = 0; logs != null && i < logs.length(); i++) {
            JSONObject o = logs.getJSONObject(i);
            int day = o.optInt("day");
            if (DayKey.isValid(day)) {
                snap.logs.add(new HabitLog(o.optInt("habitId"), day));
            }
        }
        JSONArray journal = root.optJSONArray("journal");
        for (int i = 0; journal != null && i < journal.length(); i++) {
            JSONObject o = journal.getJSONObject(i);
            JournalEntry e = new JournalEntry();
            e.day = o.optInt("day");
            e.mood = Moods.clamp(o.optInt("mood", 3));
            e.text = o.optString("text");
            e.updatedAt = o.optLong("updatedAt");
            if (DayKey.isValid(e.day)) {
                snap.journal.add(e);
            }
        }
        return snap;
    }

    public static String noteKey(Note n) {
        return s(n.getTitle()) + "\u0001" + s(n.getDateTime()) + "\u0001" + s(n.getNoteText());
    }

    public static String taskKey(Task t) {
        return s(t.title) + "\u0001" + t.dueDay;
    }

    /** Items of {@code incoming} whose key is not in {@code existing}; duplicates inside incoming are dropped too. */
    public static <T> List<T> missing(List<T> existing, List<T> incoming, KeyOf<T> key) {
        Set<String> seen = new HashSet<>();
        for (T t : existing) {
            seen.add(key.of(t));
        }
        List<T> out = new ArrayList<>();
        for (T t : incoming) {
            if (seen.add(key.of(t))) {
                out.add(t);
            }
        }
        return out;
    }

    public interface KeyOf<T> {
        String of(T t);
    }
}
