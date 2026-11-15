package com.bumbumapps.mynotes.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Daily diary: one entry per day with a mood, a streak and a mood strip for the last week. */
public class JournalActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private List<JournalEntry> entries = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(Setting.Dark_Mode ? R.style.AppTheme2 : R.style.AppTheme);
        super.onCreate(state);
        setContentView(R.layout.activity_journal);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.writeToday).setOnClickListener(v -> openEntry(DayKey.today()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        io.execute(() -> {
            final List<JournalEntry> e = OrganizerDatabase.get(getApplicationContext()).dao().getJournal();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                entries = e;
                render();
            });
        });
    }

    private void openEntry(int day) {
        startActivity(new Intent(this, JournalEditActivity.class).putExtra(JournalEditActivity.EXTRA_DAY, day));
    }

    private void render() {
        final int today = DayKey.today();
        Set<Integer> days = new HashSet<>();
        boolean hasToday = false;
        for (JournalEntry e : entries) {
            days.add(e.day);
            hasToday |= e.day == today;
        }
        int streak = Streaks.current(days, today);
        ((TextView) findViewById(R.id.streakTitle)).setText(getString(R.string.journal_streak, streak));
        ((TextView) findViewById(R.id.streakSub)).setText(getString(R.string.journal_sub, entries.size()));
        ((TextView) findViewById(R.id.writeToday)).setText(hasToday ? R.string.journal_edit_today : R.string.journal_write_today);

        LinearLayout strip = findViewById(R.id.moodStrip);
        strip.removeAllViews();
        int[] week = Moods.lastWeek(entries, today);
        SimpleDateFormat narrow = new SimpleDateFormat("EEEEE", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView face = new TextView(this);
            face.setTextSize(22);
            face.setGravity(Gravity.CENTER);
            if (week[i] == 0) {
                face.setText("·");
                face.setTextColor(0xFF7A4B05);
            } else {
                face.setText(Moods.face(week[i]));
            }
            TextView label = new TextView(this);
            label.setTextSize(11);
            label.setTextColor(0xFF4F3C17);
            label.setText(narrow.format(DayKey.toCalendar(DayKey.addDays(today, i - 6), TimeZone.getDefault()).getTime()));
            label.setTypeface(Ui.font(this, i == 6 ? R.font.jakarta_extrabold : R.font.jakarta_medium));
            label.setPadding(0, Ui.dp(this, 6), 0, 0);
            col.addView(face);
            col.addView(label);
            strip.addView(col);
        }

        LinearLayout list = findViewById(R.id.entryList);
        list.removeAllViews();
        findViewById(R.id.emptyView).setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        SimpleDateFormat wd = new SimpleDateFormat("EEE", Locale.getDefault());
        for (final JournalEntry e : entries) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_journal_entry, null);
            ((TextView) v.findViewById(R.id.entryDay)).setText(String.valueOf(DayKey.day(e.day)));
            ((TextView) v.findViewById(R.id.entryWeekday)).setText(
                    wd.format(DayKey.toCalendar(e.day, TimeZone.getDefault()).getTime()).toUpperCase(Locale.getDefault()));
            String p = Moods.preview(e.text, 140);
            ((TextView) v.findViewById(R.id.entryPreview)).setText(p.isEmpty() ? getString(R.string.journal_no_text) : p);
            ((TextView) v.findViewById(R.id.entryMood)).setText(Moods.face(e.mood));
            v.setOnClickListener(x -> openEntry(e.day));
            list.addView(v);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
