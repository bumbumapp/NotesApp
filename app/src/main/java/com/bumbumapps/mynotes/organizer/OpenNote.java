package com.bumbumapps.mynotes.organizer;

import android.content.Context;
import android.content.Intent;

import com.bumbumapps.mynotes.Activity.Note.ChecklistActivity;
import com.bumbumapps.mynotes.Activity.Note.CreateNoteActivity;
import com.bumbumapps.mynotes.entities.Note;

/** Opens a note in the right editor (text note or checklist). */
public final class OpenNote {
    private OpenNote() {
    }

    public static void open(Context c, Note note) {
        Intent intent;
        if (note.getNoteType() == Note.TYPE_CHECKLIST) {
            intent = new Intent(c, ChecklistActivity.class).putExtra(ChecklistActivity.EXTRA_NOTE, note);
        } else {
            intent = new Intent(c, CreateNoteActivity.class)
                    .putExtra("isViemOrUpdate", true).putExtra("note", note);
        }
        c.startActivity(intent);
    }
}
