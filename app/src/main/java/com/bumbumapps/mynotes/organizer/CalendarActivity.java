package com.bumbumapps.mynotes.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Month view: dots for tasks, habit check-ins, journal entries and notes; tap a day to see its agenda. */
public class CalendarActivity extends AppCompatActivity {

    private static final String STATE_MONTH = "month";
    private static final String STATE_DAY = "day";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private MonthGridView grid;
    private int month;
    private int selected;
    private List<Task> tasks = new ArrayList<>();
    private List<Habit> habits = new ArrayList<>();
    private List<HabitLog> logs = new ArrayList<>();
    private List<JournalEntry> journal = new ArrayList<>();
    private List<Note> notes = new ArrayList<>();
    private final Map<Integer, Integer> noteDays = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(Setting.Dark_Mode ? R.style.AppTheme2 : R.style.AppTheme);
        super.onCreate(state);
        setContentView(R.layout.activity_calendar);
        int today = DayKey.today();
        selected = state != null ? state.getInt(STATE_DAY, today) : today;
        month = state != null ? state.getInt(STATE_MONTH, today) : today;
        grid = findViewById(R.id.monthGrid);
        grid.setListener(day -> {
            selected = day;
            render();
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.monthPrev).setOnClickListener(v -> shiftMonth(-1));
        findViewById(R.id.monthNext).setOnClickListener(v -> shiftMonth(1));
        findViewById(R.id.agendaAdd).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setItems(new String[]{getString(R.string.calendar_new_task), getString(R.string.calendar_new_journal)},
                        (d, which) -> {
                            if (which == 0) {
                                startActivity(new Intent(this, TasksActivity.class)
                                        .putExtra(TasksActivity.EXTRA_DUE_DAY, selected));
                            } else {
                                startActivity(new Intent(this, JournalEditActivity.class)
                                        .putExtra(JournalEditActivity.EXTRA_DAY, selected));
                            }
                        }).show());
        buildLegend();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_MONTH, month);
        out.putInt(STATE_DAY, selected);
    }

    private void shiftMonth(int delta) {
        month = DayKey.addMonths(month, delta);
        // keep the selected day inside the visible month
        int sel = DayKey.of(DayKey.year(month), DayKey.month(month), 1);
        int today = DayKey.today();
        if (DayKey.year(today) == DayKey.year(month) && DayKey.month(today) == DayKey.month(month)) {
            sel = today;
        }
        selected = sel;
        render();
    }

    private void buildLegend() {
        LinearLayout legend = findViewById(R.id.legend);
        String[] names = {getString(R.string.legend_task), getString(R.string.legend_habit),
                getString(R.string.legend_journal), getString(R.string.legend_note)};
        for (int i = 0; i < 4; i++) {
            TextView t = new TextView(this);
            t.setText(names[i]);
            t.setTextSize(11);
            t.setSingleLine(true);
            t.setTextColor(Ui.themeColor(this, R.attr.Titel_2));
            t.setTypeface(Ui.font(this, R.font.jakarta_medium));
            t.setGravity(Gravity.CENTER_VERTICAL);
            int size = Ui.dp(this, 6);
            android.graphics.drawable.GradientDrawable dot = Ui.circle(MonthGridView.MARK_COLORS[i], 0, 0);
            dot.setSize(size, size);
            t.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
            t.setCompoundDrawablePadding(Ui.dp(this, 5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(this, 24));
            lp.setMarginStart(Ui.dp(this, i == 0 ? 0 : 12));
            t.setLayoutParams(lp);
            legend.addView(t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        io.execute(() -> {
            OrganizerDao dao = OrganizerDatabase.get(getApplicationContext()).dao();
            final List<Task> t = dao.getTasks();
            final List<Habit> h = dao.getHabits();
            final List<HabitLog> l = dao.getHabitLogs();
            final List<JournalEntry> j = dao.getJournal();
            final List<Note> n = NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao().getAllNotes();
            final Map<Integer, Integer> nd = new HashMap<>();
            for (Note note : n) {
                int d = NoteDates.dayOf(note.getDateTime(), TimeZone.getDefault());
                if (d != 0) {
                    nd.put(note.getId(), d);
                }
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                tasks = t;
                habits = h;
                logs = l;
                journal = j;
                notes = n;
                noteDays.clear();
                noteDays.putAll(nd);
                render();
            });
        });
    }

    private Map<Integer, Integer> marks() {
        Map<Integer, Integer> m = new HashMap<>();
        for (Task t : tasks) {
            if (t.dueDay != 0) {
                m.put(t.dueDay, (m.containsKey(t.dueDay) ? m.get(t.dueDay) : 0) | MonthGridView.MARK_TASK);
            }
        }
        for (HabitLog l : logs) {
            m.put(l.day, (m.containsKey(l.day) ? m.get(l.day) : 0) | MonthGridView.MARK_HABIT);
        }
        for (JournalEntry e : journal) {
            m.put(e.day, (m.containsKey(e.day) ? m.get(e.day) : 0) | MonthGridView.MARK_JOURNAL);
        }
        for (int d : noteDays.values()) {
            m.put(d, (m.containsKey(d) ? m.get(d) : 0) | MonthGridView.MARK_NOTE);
        }
        return m;
    }

    private void render() {
        long monthMillis = DayKey.toCalendar(month, TimeZone.getDefault()).getTimeInMillis();
        ((TextView) findViewById(R.id.monthTitle)).setText(DateUtils.formatDateTime(this, monthMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_SHOW_YEAR));
        grid.setState(month, selected, marks());
        long selMillis = DayKey.toCalendar(selected, TimeZone.getDefault()).getTimeInMillis();
        ((TextView) findViewById(R.id.agendaTitle)).setText(DateUtils.formatDateTime(this, selMillis,
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        LinearLayout list = findViewById(R.id.agendaList);
        list.removeAllViews();
        int count = 0;
        for (final Task t : tasks) {
            if (t.dueDay == selected) {
                addRow(list, count++, R.drawable.ic_mod_tasks, R.drawable.bg_icon_amber, 0xFFE8A21F, t.title,
                        getString(t.done ? R.string.task_status_done : R.string.task_status_open),
                        t.reminderAt > 0 ? Ui.timeLabel(this, t.reminderAt) : "",
                        v -> startActivity(new Intent(this, TasksActivity.class)));
            }
        }
        List<String> doneNames = new ArrayList<>();
        for (Habit h : habits) {
            for (HabitLog l : logs) {
                if (l.habitId == h.id && l.day == selected) {
                    doneNames.add(h.name);
                    break;
                }
            }
        }
        if (!doneNames.isEmpty()) {
            addRow(list, count++, R.drawable.ic_mod_habits, R.drawable.bg_icon_green, 0xFF2FBF71,
                    getString(R.string.mod_habits), android.text.TextUtils.join(" · ", doneNames),
                    doneNames.size() + "/" + habits.size(), v -> startActivity(new Intent(this, HabitsActivity.class)));
        }
        for (final JournalEntry e : journal) {
            if (e.day == selected) {
                String p = Moods.preview(e.text, 90);
                addRow(list, count++, R.drawable.ic_mod_journal, R.drawable.bg_icon_violet, 0xFF8B5CF6,
                        getString(R.string.mod_journal), p.isEmpty() ? getString(R.string.journal_no_text) : p,
                        Moods.face(e.mood), v -> startActivity(new Intent(this, JournalEditActivity.class)
                                .putExtra(JournalEditActivity.EXTRA_DAY, e.day)));
            }
        }
        for (final Note n : notes) {
            Integer d = noteDays.get(n.getId());
            if (d != null && d == selected) {
                String title = n.getTitle() == null || n.getTitle().trim().isEmpty()
                        ? getString(R.string.reminder_untitled) : n.getTitle();
                addRow(list, count++, R.drawable.ic_checklist, R.drawable.bg_icon_blue, 0xFF4C8DFF, title,
                        getString(n.getNoteType() == Note.TYPE_CHECKLIST ? R.string.legend_checklist : R.string.legend_note),
                        "", v -> OpenNote.open(this, n));
            }
        }
        findViewById(R.id.agendaList).setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.emptyView).setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void addRow(LinearLayout list, int index, int icon, int bg, int tint, String kind, String text,
                        String meta, View.OnClickListener click) {
        if (index > 0) {
            list.addView(Ui.divider(this, 0));
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_agenda_row, list, false);
        ImageView iv = v.findViewById(R.id.agendaIcon);
        iv.setImageResource(icon);
        iv.setBackgroundResource(bg);
        iv.setColorFilter(tint);
        ((TextView) v.findViewById(R.id.agendaKind)).setText(kind);
        ((TextView) v.findViewById(R.id.agendaText)).setText(text);
        TextView m = v.findViewById(R.id.agendaMeta);
        m.setText(meta);
        m.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
        v.setOnClickListener(click);
        list.addView(v);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
