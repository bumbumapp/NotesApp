package com.bumbumapps.mynotes.organizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bumbumapps.mynotes.entities.Note;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class OrganizerLogicTest {

    private static Set<Integer> days(Integer... d) {
        return new HashSet<>(Arrays.asList(d));
    }

    private static Task task(String title, int due, boolean done, int pr) {
        Task t = new Task();
        t.title = title;
        t.dueDay = due;
        t.done = done;
        t.priority = pr;
        return t;
    }

    @Test
    public void dayKeyMathCrossesMonthAndYear() {
        assertEquals(20261101, DayKey.addDays(20261031, 1));
        assertEquals(20270101, DayKey.addDays(20261231, 1));
        assertEquals(20240229, DayKey.addDays(20240228, 1));
        assertEquals(20261031, DayKey.addDays(20261101, -1));
        assertEquals(20270201, DayKey.addMonths(20261115, 3));
        assertEquals(20251101, DayKey.addMonths(20260131, -2));
        assertTrue(DayKey.isValid(20240229));
        assertFalse(DayKey.isValid(20230229));
        assertFalse(DayKey.isValid(20261301));
        assertFalse(DayKey.isValid(0));
        assertEquals(2026, DayKey.year(20261116));
        assertEquals(10, DayKey.month(20261116));
        assertEquals(16, DayKey.day(20261116));
    }

    @Test
    public void currentStreakCountsBackFromTodayOrYesterday() {
        assertEquals(0, Streaks.current(days(), 20261116));
        assertEquals(3, Streaks.current(days(20261114, 20261115, 20261116), 20261116));
        // today not done yet: yesterday's run still counts
        assertEquals(2, Streaks.current(days(20261114, 20261115), 20261116));
        // a full missed day resets it
        assertEquals(0, Streaks.current(days(20261113, 20261114), 20261116));
        assertEquals(4, Streaks.current(days(20261029, 20261030, 20261031, 20261101), 20261101));
    }

    @Test
    public void bestStreakFindsLongestRun() {
        assertEquals(0, Streaks.best(new ArrayList<Integer>()));
        assertEquals(1, Streaks.best(Arrays.asList(20261110)));
        assertEquals(3, Streaks.best(Arrays.asList(20261101, 20261102, 20261103, 20261105, 20261106)));
        assertEquals(4, Streaks.best(Arrays.asList(20261230, 20261231, 20270101, 20270102, 20260101)));
    }

    @Test
    public void taskTabsSplitTodayUpcomingDone() {
        int today = 20261116;
        Task overdue = task("a", 20261110, false, Task.NORMAL);
        Task due = task("b", today, false, Task.HIGH);
        Task future = task("c", 20261120, false, Task.LOW);
        Task undated = task("d", 0, false, Task.NORMAL);
        Task done = task("e", today, true, Task.NORMAL);
        List<Task> all = Arrays.asList(overdue, due, future, undated, done);
        assertEquals(Arrays.asList(overdue, due), TaskLogic.filter(all, TaskLogic.TODAY, today));
        assertEquals(Arrays.asList(future, undated), TaskLogic.filter(all, TaskLogic.UPCOMING, today));
        assertEquals(4, TaskLogic.filter(all, TaskLogic.ALL, today).size());
        assertEquals(Arrays.asList(done), TaskLogic.filter(all, TaskLogic.DONE, today));
        assertTrue(TaskLogic.isOverdue(overdue, today));
        assertFalse(TaskLogic.isOverdue(due, today));
        assertFalse(TaskLogic.isOverdue(done, today));
        assertEquals(2, TaskLogic.openToday(all, today));
        assertEquals(4, TaskLogic.openCount(all));
    }

    @Test
    public void weekPercentCountsOnlyHabitsThatAlreadyExisted() {
        int today = 20261116;
        Habit a = new Habit();
        a.id = 1;
        a.startDay = 20261101;
        Habit b = new Habit();
        b.id = 2;
        b.startDay = 20261115; // created yesterday
        List<HabitLog> logs = Arrays.asList(
                new HabitLog(1, today), new HabitLog(2, today), new HabitLog(1, 20261115),
                new HabitLog(1, 20261112));
        int[] p = WeekChart.weekPercent(Arrays.asList(a, b), logs, today);
        assertEquals(7, p.length);
        assertEquals(100, p[6]); // both done today
        assertEquals(50, p[5]);  // yesterday: a done, b not
        assertEquals(100, p[2]); // 14th..: only a existed and was done on the 12th
        assertEquals(0, p[4]);   // 14th: a not done
        int[] none = WeekChart.weekPercent(new ArrayList<Habit>(), new ArrayList<HabitLog>(), today);
        assertEquals(0, none[6]);
    }

    @Test
    public void moodsClampAndPreview() {
        assertEquals(1, Moods.clamp(-3));
        assertEquals(5, Moods.clamp(99));
        assertEquals(5, Moods.FACES.length);
        assertEquals("a b c", Moods.preview("  a\n b\t c ", 20));
        assertEquals("abc…", Moods.preview("abcdefgh", 4));
        assertEquals("", Moods.preview(null, 10));
        JournalEntry e = new JournalEntry();
        e.day = 20261115;
        e.mood = 2;
        int[] w = Moods.lastWeek(Arrays.asList(e), 20261116);
        assertEquals(2, w[5]);
        assertEquals(0, w[6]);
    }

    @Test
    public void noteDatesParseBothClockFormats() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        java.util.Locale old = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        try {
            assertEquals(20261113, NoteDates.dayOf("Friday, 13 November 2026 14:05", tz));
            assertEquals(20261113, NoteDates.dayOf("Friday, 13 November 2026 2:05 PM", tz));
            assertEquals(0, NoteDates.dayOf("garbage", tz));
            assertEquals(0, NoteDates.dayOf("", tz));
            assertEquals(0, NoteDates.dayOf(null, tz));
        } finally {
            java.util.Locale.setDefault(old);
        }
    }

    @Test
    public void backupRoundTripKeepsEverything() throws JSONException {
        BackupJson.Snapshot s = new BackupJson.Snapshot();
        Note n = new Note();
        n.setTitle("Plan \"A\"");
        n.setDateTime("Friday, 13 November 2026 14:05");
        n.setSubtitle("sub");
        n.setNoteText("line1\nline2 é中");
        n.setColor("#fdbe3b");
        n.setPinned(1);
        n.setCategory("Work");
        n.setNoteType(Note.TYPE_CHECKLIST);
        s.notes.add(n);
        s.tasks.add(task("Buy milk", 20261120, true, Task.HIGH));
        Habit h = new Habit();
        h.id = 7;
        h.name = "Read";
        h.icon = "📖";
        h.color = 2;
        h.startDay = 20261101;
        s.habits.add(h);
        s.logs.add(new HabitLog(7, 20261115));
        JournalEntry j = new JournalEntry();
        j.day = 20261115;
        j.mood = 5;
        j.text = "great day";
        s.journal.add(j);

        BackupJson.Snapshot back = BackupJson.fromJson(BackupJson.toJson(s));
        assertEquals(1, back.notes.size());
        Note bn = back.notes.get(0);
        assertEquals("Plan \"A\"", bn.getTitle());
        assertEquals("line1\nline2 é中", bn.getNoteText());
        assertEquals("Work", bn.getCategory());
        assertEquals(1, bn.getPinned());
        assertEquals(Note.TYPE_CHECKLIST, bn.getNoteType());
        assertEquals("Buy milk", back.tasks.get(0).title);
        assertTrue(back.tasks.get(0).done);
        assertEquals(Task.HIGH, back.tasks.get(0).priority);
        assertEquals("Read", back.habits.get(0).name);
        assertEquals(7, back.habits.get(0).id);
        assertEquals(20261115, back.logs.get(0).day);
        assertEquals(5, back.journal.get(0).mood);
        assertEquals("great day", back.journal.get(0).text);
    }

    @Test(expected = JSONException.class)
    public void backupRejectsForeignJson() throws JSONException {
        BackupJson.fromJson("{\"hello\":1}");
    }

    @Test(expected = JSONException.class)
    public void backupRejectsGarbage() throws JSONException {
        BackupJson.fromJson("not json at all");
    }

    @Test
    public void backupDropsInvalidItemsAndKeepsTolerance() throws JSONException {
        String json = "{\"app\":\"" + BackupJson.MARKER + "\",\"version\":1,"
                + "\"tasks\":[{\"title\":\"  \"},{\"title\":\"ok\",\"priority\":99}],"
                + "\"habitLogs\":[{\"habitId\":1,\"day\":20261399},{\"habitId\":1,\"day\":20261101}],"
                + "\"journal\":[{\"day\":0,\"mood\":9},{\"day\":20261101,\"mood\":9}]}";
        BackupJson.Snapshot s = BackupJson.fromJson(json);
        assertEquals(1, s.tasks.size());
        assertEquals(Task.HIGH, s.tasks.get(0).priority);
        assertEquals(1, s.logs.size());
        assertEquals(1, s.journal.size());
        assertEquals(5, s.journal.get(0).mood);
        assertEquals(0, s.notes.size());
    }

    @Test
    public void mergeAddsOnlyMissingItems() {
        List<Task> have = Arrays.asList(task("a", 1, false, 1), task("b", 0, false, 1));
        List<Task> in = Arrays.asList(task("a", 1, true, 2), task("c", 0, false, 1), task("c", 0, false, 1));
        List<Task> add = BackupJson.missing(have, in, BackupJson::taskKey);
        assertEquals(1, add.size());
        assertEquals("c", add.get(0).title);
        assertNull(null);
    }
}
