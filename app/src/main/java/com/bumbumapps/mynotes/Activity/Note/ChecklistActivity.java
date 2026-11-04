package com.bumbumapps.mynotes.Activity.Note;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.logic.Checklist;
import com.bumbumapps.mynotes.reminder.ReminderScheduler;
import com.bumbumapps.mynotes.reminder.ReminderUi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Editor for checklist notes: tick items off, add, remove, set a reminder. Saves when you leave. */
public class ChecklistActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE = "note";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Checklist.Item> items = new ArrayList<>();
    private LinearLayout itemsLayout;
    private EditText titleView;
    private TextView progressView;
    private Note note;
    private boolean saving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(Setting.Dark_Mode ? R.style.AppTheme2 : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist);
        itemsLayout = findViewById(R.id.checklistItems);
        titleView = findViewById(R.id.checklistTitle);
        progressView = findViewById(R.id.checklistProgress);

        note = (Note) getIntent().getSerializableExtra(EXTRA_NOTE);
        if (note != null) {
            titleView.setText(note.getTitle());
            items.addAll(Checklist.parse(note.getNoteText()));
        }
        if (items.isEmpty()) {
            items.add(new Checklist.Item("", false));
        }
        render(note == null ? 0 : -1);

        findViewById(R.id.checklistBack).setOnClickListener(v -> finishAndSave());
        findViewById(R.id.checklistAdd).setOnClickListener(v -> {
            items.add(new Checklist.Item("", false));
            render(items.size() - 1);
        });
        findViewById(R.id.checklistClearDone).setOnClickListener(v -> {
            Checklist.removeDone(items);
            if (items.isEmpty()) {
                items.add(new Checklist.Item("", false));
            }
            render(-1);
        });
        findViewById(R.id.checklistReminder).setOnClickListener(v -> {
            if (Checklist.count(items) == 0) {
                Toast.makeText(this, R.string.checklist_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            // The reminder needs a saved note, so save first.
            persist(false, () -> ReminderUi.show(this, note, null));
        });
    }

    /** Rebuilds the rows; {@code focus} is the row index to put the cursor in, or -1. */
    private void render(int focus) {
        itemsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < items.size(); i++) {
            final Checklist.Item item = items.get(i);
            View row = inflater.inflate(R.layout.item_checklist_row, itemsLayout, false);
            final CheckBox check = row.findViewById(R.id.rowCheck);
            final EditText text = row.findViewById(R.id.rowText);
            text.setText(item.text);
            check.setChecked(item.done);
            strike(text, item.done);
            check.setOnCheckedChangeListener((b, on) -> {
                item.done = on;
                strike(text, on);
                updateProgress();
            });
            text.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable e) {
                    item.text = e.toString();
                    updateProgress();
                }
            });
            final int index = i;
            text.setOnEditorActionListener((v, action, ev) -> {
                items.add(index + 1, new Checklist.Item("", false));
                render(index + 1);
                return true;
            });
            row.findViewById(R.id.rowDelete).setOnClickListener(v -> {
                items.remove(index);
                if (items.isEmpty()) {
                    items.add(new Checklist.Item("", false));
                }
                render(-1);
            });
            itemsLayout.addView(row);
            if (i == focus) {
                text.requestFocus();
                text.post(() -> {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(text, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }
        updateProgress();
    }

    private static void strike(EditText t, boolean done) {
        t.setPaintFlags(done ? t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                : t.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        t.setAlpha(done ? 0.55f : 1f);
    }

    private void updateProgress() {
        progressView.setText(getString(R.string.checklist_progress,
                Checklist.doneCount(items), Checklist.count(items)));
    }

    /** Writes the note to the database; empty checklists are not saved. */
    private void persist(final boolean thenFinish, @Nullable final Runnable afterSave) {
        if (saving) {
            return;
        }
        if (Checklist.count(items) == 0) {
            if (thenFinish) {
                finish();
            }
            return;
        }
        saving = true;
        final Note n = note == null ? new Note() : note;
        n.setNoteType(Note.TYPE_CHECKLIST);
        n.setTitle(titleView.getText().toString().trim());
        n.setSubtitle("");
        n.setNoteText(Checklist.serialize(items));
        n.setDateTime(new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date()));
        if (n.getColor() == null) {
            n.setColor("#333333");
        }
        final android.content.Context app = getApplicationContext();
        io.execute(() -> {
            NotesDatabase db = NotesDatabase.getNotesDatabase(app);
            db.noteDao().insertNote(n);
            if (n.getId() == 0) {
                // New row: find the id Room assigned (newest note).
                List<Note> all = db.noteDao().getAllNotes();
                if (!all.isEmpty()) {
                    n.setId(all.get(0).getId());
                }
            }
            runOnUiThread(() -> {
                note = n;
                saving = false;
                if (afterSave != null) {
                    afterSave.run();
                }
                if (thenFinish) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        });
    }

    private void finishAndSave() {
        persist(true, null);
    }

    @Override
    public void onBackPressed() {
        finishAndSave();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ReminderUi.onPermissionResult(this, requestCode, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
