package com.bumbumapps.mynotes.NoteDao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import com.bumbumapps.mynotes.entities.Note;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE id = :id")
    Note getNote(int id);

    @Query("SELECT * FROM notes WHERE reminder_at > 0")
    List<Note> getNotesWithReminder();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void  insertNote(Note note);

    @Delete
    void deletNote(Note note);
}
