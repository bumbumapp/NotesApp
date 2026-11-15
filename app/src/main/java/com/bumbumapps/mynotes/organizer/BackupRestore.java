package com.bumbumapps.mynotes.organizer;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.bumbumapps.mynotes.NoteDao.NoteDao;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads and writes the backup file through the system file picker (no storage permission). */
public final class BackupRestore {
    private BackupRestore() {
    }

    public static final class Result {
        public int notes, tasks, habits, journal;
    }

    private static final int MAX_BYTES = 20 * 1024 * 1024;

    /** Must run off the main thread. */
    public static Result export(Context c, Uri uri) throws IOException, JSONException {
        Context app = c.getApplicationContext();
        BackupJson.Snapshot snap = new BackupJson.Snapshot();
        snap.notes.addAll(NotesDatabase.getNotesDatabase(app).noteDao().getAllNotes());
        OrganizerDao dao = OrganizerDatabase.get(app).dao();
        snap.tasks.addAll(dao.getTasks());
        snap.habits.addAll(dao.getHabits());
        snap.logs.addAll(dao.getHabitLogs());
        snap.journal.addAll(dao.getJournal());
        byte[] bytes = BackupJson.toJson(snap).getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = app.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("no output stream");
            }
            out.write(bytes);
        }
        Result r = new Result();
        r.notes = snap.notes.size();
        r.tasks = snap.tasks.size();
        r.habits = snap.habits.size();
        r.journal = snap.journal.size();
        return r;
    }

    /** Adds what is missing; existing notes, tasks, habits and journal days are left as they are. */
    public static Result restore(Context c, Uri uri) throws IOException, JSONException {
        Context app = c.getApplicationContext();
        ContentResolver cr = app.getContentResolver();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) {
                throw new IOException("no input stream");
            }
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) {
                buf.write(chunk, 0, n);
                if (buf.size() > MAX_BYTES) {
                    throw new IOException("file too large");
                }
            }
        }
        BackupJson.Snapshot snap = BackupJson.fromJson(new String(buf.toByteArray(), StandardCharsets.UTF_8));
        Result r = new Result();

        NoteDao noteDao = NotesDatabase.getNotesDatabase(app).noteDao();
        for (Note n : BackupJson.missing(noteDao.getAllNotes(), snap.notes, BackupJson::noteKey)) {
            n.setId(0);
            noteDao.insertNote(n);
            r.notes++;
        }
        OrganizerDao dao = OrganizerDatabase.get(app).dao();
        for (Task t : BackupJson.missing(dao.getTasks(), snap.tasks, BackupJson::taskKey)) {
            t.id = 0;
            dao.saveTask(t);
            r.tasks++;
        }
        // Habits are matched by name; old ids in the file are mapped to the ids on this phone.
        List<Habit> have = dao.getHabits();
        Map<Integer, Integer> idMap = new HashMap<>();
        for (Habit incoming : snap.habits) {
            Habit match = null;
            for (Habit h : have) {
                if (h.name.equalsIgnoreCase(incoming.name)) {
                    match = h;
                    break;
                }
            }
            if (match != null) {
                idMap.put(incoming.id, match.id);
            } else {
                int oldId = incoming.id;
                incoming.id = 0;
                int newId = (int) dao.saveHabit(incoming);
                incoming.id = newId;
                have.add(incoming);
                idMap.put(oldId, newId);
                r.habits++;
            }
        }
        for (HabitLog l : snap.logs) {
            Integer id = idMap.get(l.habitId);
            if (id != null) {
                dao.saveHabitLog(new HabitLog(id, l.day));
            }
        }
        for (JournalEntry e : snap.journal) {
            if (dao.getJournalEntry(e.day) == null) {
                dao.saveJournalEntry(e);
                r.journal++;
            }
        }
        return r;
    }
}
