package com.bumbumapps.mynotes.organizer;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;

import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Write or edit the journal entry of one day. Saves when you leave, like the checklist editor. */
public class JournalEditActivity extends AppCompatActivity {

    public static final String EXTRA_DAY = "day";
    private static final String STATE_MOOD = "mood";
    private static final String STATE_LOADED = "loaded";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final TextView[] moodViews = new TextView[5];
    private int day;
    private int mood = 4;
    private boolean existing;
    private boolean loaded;
    private boolean finishing;
    private EditText text;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(Setting.Dark_Mode ? R.style.AppTheme2 : R.style.AppTheme);
        super.onCreate(state);
        setContentView(R.layout.activity_journal_edit);
        day = getIntent().getIntExtra(EXTRA_DAY, DayKey.today());
        if (!DayKey.isValid(day)) {
            day = DayKey.today();
        }
        text = findViewById(R.id.entryText);
        TextView title = findViewById(R.id.screenTitle);
        title.setTextSize(20);
        title.setText(DateUtils.formatDateTime(this, DayKey.toCalendar(day, TimeZone.getDefault()).getTimeInMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        for (int i = 0; i < 5; i++) {
            final int level = i + 1;
            moodViews[i] = findViewById(getResources().getIdentifier("mood" + level, "id", getPackageName()));
            moodViews[i].setText(Moods.face(level));
            moodViews[i].setContentDescription(getString(R.string.journal_mood_n, level));
            moodViews[i].setOnClickListener(v -> {
                mood = level;
                paintMood();
            });
        }
        if (state != null) {
            mood = state.getInt(STATE_MOOD, mood);
            loaded = state.getBoolean(STATE_LOADED, false);
        }
        paintMood();
        findViewById(R.id.btnBack).setOnClickListener(v -> saveAndFinish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveAndFinish());
        findViewById(R.id.btnDelete).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage(R.string.journal_delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    finishing = true;
                    io.execute(() -> {
                        OrganizerDatabase.get(getApplicationContext()).dao().deleteJournalEntry(day);
                        runOnUiThread(this::finish);
                    });
                }).show());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAndFinish();
            }
        });
        if (!loaded) {
            io.execute(() -> {
                final JournalEntry e = OrganizerDatabase.get(getApplicationContext()).dao().getJournalEntry(day);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    loaded = true;
                    if (e != null) {
                        existing = true;
                        mood = Moods.clamp(e.mood);
                        text.setText(e.text);
                        text.setSelection(text.getText().length());
                        paintMood();
                    }
                });
            });
        } else {
            existing = true;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_MOOD, mood);
        out.putBoolean(STATE_LOADED, loaded);
    }

    private void paintMood() {
        for (int i = 0; i < 5; i++) {
            boolean sel = mood == i + 1;
            moodViews[i].setBackgroundResource(sel ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            moodViews[i].setAlpha(sel ? 1f : 0.85f);
        }
    }

    private void saveAndFinish() {
        if (finishing) {
            return;
        }
        finishing = true;
        final String body = text.getText().toString().trim();
        if (body.isEmpty() && !existing) {
            finish();
            return;
        }
        final JournalEntry e = new JournalEntry();
        e.day = day;
        e.mood = mood;
        e.text = body;
        e.updatedAt = System.currentTimeMillis();
        io.execute(() -> {
            OrganizerDatabase.get(getApplicationContext()).dao().saveJournalEntry(e);
            runOnUiThread(this::finish);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
