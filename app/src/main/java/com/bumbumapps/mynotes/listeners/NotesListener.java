package com.bumbumapps.mynotes.listeners;

import com.bumbumapps.mynotes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
    void OnNoteLongClickListener(Note note,int position);
}
