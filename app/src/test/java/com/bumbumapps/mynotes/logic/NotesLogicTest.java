package com.bumbumapps.mynotes.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bumbumapps.mynotes.entities.Note;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

public class NotesLogicTest {

    private static Note note(int id, String title, String text, int pinned, String cat, int type, long rem) {
        Note n = new Note();
        n.setId(id);
        n.setTitle(title);
        n.setSubtitle("");
        n.setNoteText(text);
        n.setPinned(pinned);
        n.setCategory(cat);
        n.setNoteType(type);
        n.setReminderAt(rem);
        return n;
    }

    @Test
    public void checklistRoundTripKeepsStateAndDropsBlanks() {
        List<Checklist.Item> items = new ArrayList<>();
        items.add(new Checklist.Item("milk", false));
        items.add(new Checklist.Item("   ", false));
        items.add(new Checklist.Item("eggs\nand bacon", true));
        String stored = Checklist.serialize(items);
        assertEquals("[ ] milk\n[x] eggs and bacon", stored);
        List<Checklist.Item> back = Checklist.parse(stored);
        assertEquals(2, back.size());
        assertTrue(back.get(1).done);
        assertEquals(1, Checklist.doneCount(back));
        assertEquals(2, Checklist.count(back));
    }

    @Test
    public void checklistParseToleratesPlainLinesAndNull() {
        assertEquals(0, Checklist.parse(null).size());
        List<Checklist.Item> p = Checklist.parse("buy bread\n[X] call");
        assertEquals(2, p.size());
        assertFalse(p.get(0).done);
        assertTrue(p.get(1).done);
    }

    @Test
    public void checklistDoneToBottomAndRemoveDone() {
        List<Checklist.Item> items = Checklist.parse("[x] a\n[ ] b\n[x] c\n[ ] d");
        Checklist.doneToBottom(items);
        assertEquals("b", items.get(0).text);
        assertEquals("d", items.get(1).text);
        assertTrue(items.get(2).done);
        Checklist.removeDone(items);
        assertEquals(2, items.size());
    }

    @Test
    public void checklistPreviewLimitsLines() {
        List<Checklist.Item> items = Checklist.parse("[x] a\n[ ] b\n[ ] c");
        assertEquals("☑ a\n☐ b\n…", Checklist.preview(items, 2));
        assertEquals("☑ a\n☐ b\n☐ c", Checklist.preview(items, 5));
    }

    @Test
    public void filterPinnedFirstThenNewest() {
        List<Note> all = Arrays.asList(note(1, "old", "x", 0, null, 0, 0), note(2, "new", "x", 0, null, 0, 0),
                note(3, "pin", "x", 1, null, 0, 0));
        List<Note> r = NoteFilter.apply(all, "", NoteFilter.ALL);
        assertEquals(3, r.get(0).getId());
        assertEquals(2, r.get(1).getId());
        assertEquals(1, r.get(2).getId());
    }

    @Test
    public void filterByCategoryChecklistReminderAndQuery() {
        List<Note> all = Arrays.asList(note(1, "Trip", "pack bags", 0, "Travel", 0, 0),
                note(2, "Shop", "[ ] milk", 0, null, 1, 5L), note(3, "Idea", "app", 1, "work", 0, 0));
        assertEquals(1, NoteFilter.apply(all, "", "travel").size());
        assertEquals(2, NoteFilter.apply(all, "", NoteFilter.CHECKLISTS).get(0).getId());
        assertEquals(1, NoteFilter.apply(all, "", NoteFilter.REMINDERS).size());
        assertEquals(1, NoteFilter.apply(all, "", NoteFilter.PINNED).size());
        assertEquals(1, NoteFilter.apply(all, "MILK", NoteFilter.ALL).size());
        assertEquals(0, NoteFilter.apply(all, "milk", "work").size());
    }

    @Test
    public void categoriesAreDistinctSortedAndCleaned() {
        List<Note> all = Arrays.asList(note(1, "a", "", 0, "work", 0, 0), note(2, "b", "", 0, "Work", 0, 0),
                note(3, "c", "", 0, " Home ", 0, 0), note(4, "d", "", 0, "", 0, 0));
        assertEquals(Arrays.asList("Home", "work"), NoteFilter.categories(all));
        assertEquals("a b", NoteFilter.cleanCategory("a\n\n  b"));
        assertEquals(24, NoteFilter.cleanCategory("abcdefghijklmnopqrstuvwxyz0123456789").length());
    }

    @Test
    public void copyMetaKeepsOrganisingFields() {
        Note src = note(1, "t", "x", 1, "work", 1, 99L);
        Note dst = new Note();
        dst.copyMetaFrom(src);
        assertEquals(1, dst.getPinned());
        assertEquals("work", dst.getCategory());
        assertEquals(99L, dst.getReminderAt());
        assertEquals(Note.TYPE_CHECKLIST, dst.getNoteType());
    }

    @Test
    public void reminderPresets() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar c = Calendar.getInstance(utc);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 5, 10, 0, 0);
        long morning = c.getTimeInMillis();
        c.set(2026, Calendar.NOVEMBER, 5, 19, 50, 0);
        long lateEvening = c.getTimeInMillis();
        assertEquals(morning + 3_600_000L, ReminderPresets.inOneHour(morning));
        c.set(2026, Calendar.NOVEMBER, 5, 20, 0, 0);
        assertEquals(c.getTimeInMillis(), ReminderPresets.thisEvening(morning, utc));
        assertEquals(0, ReminderPresets.thisEvening(lateEvening, utc)); // under 15 minutes away
        c.set(2026, Calendar.NOVEMBER, 6, 9, 0, 0);
        assertEquals(c.getTimeInMillis(), ReminderPresets.tomorrowMorning(morning, utc));
        assertTrue(ReminderPresets.isFuture(morning + 60_000, morning));
        assertFalse(ReminderPresets.isFuture(morning + 1_000, morning));
    }
}
