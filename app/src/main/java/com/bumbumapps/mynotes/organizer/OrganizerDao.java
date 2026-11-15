package com.bumbumapps.mynotes.organizer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OrganizerDao {

    // tasks
    @Query("SELECT * FROM tasks ORDER BY done ASC, CASE WHEN due_day = 0 THEN 99999999 ELSE due_day END ASC, priority DESC, id DESC")
    List<Task> getTasks();

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTask(int id);

    @Query("SELECT * FROM tasks WHERE reminder_at > 0 AND done = 0")
    List<Task> getTasksWithReminder();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long saveTask(Task task);

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteTask(int id);

    @Query("DELETE FROM tasks WHERE done = 1")
    void deleteDoneTasks();

    // habits
    @Query("SELECT * FROM habits ORDER BY id ASC")
    List<Habit> getHabits();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long saveHabit(Habit habit);

    @Query("DELETE FROM habits WHERE id = :id")
    void deleteHabit(int id);

    @Query("DELETE FROM habit_logs WHERE habit_id = :id")
    void deleteHabitLogs(int id);

    @Query("SELECT * FROM habit_logs")
    List<HabitLog> getHabitLogs();

    @Query("SELECT * FROM habit_logs WHERE day = :day")
    List<HabitLog> getHabitLogsForDay(int day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveHabitLog(HabitLog log);

    @Query("DELETE FROM habit_logs WHERE habit_id = :habitId AND day = :day")
    void deleteHabitLog(int habitId, int day);

    // journal
    @Query("SELECT * FROM journal ORDER BY day DESC")
    List<JournalEntry> getJournal();

    @Query("SELECT * FROM journal WHERE day = :day")
    JournalEntry getJournalEntry(int day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveJournalEntry(JournalEntry entry);

    @Query("DELETE FROM journal WHERE day = :day")
    void deleteJournalEntry(int day);

    // backup restore
    @Query("DELETE FROM tasks")
    void clearTasks();

    @Query("DELETE FROM habits")
    void clearHabits();

    @Query("DELETE FROM habit_logs")
    void clearHabitLogs();

    @Query("DELETE FROM journal")
    void clearJournal();
}
