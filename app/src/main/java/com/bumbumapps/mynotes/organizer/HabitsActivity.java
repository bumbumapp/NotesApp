package com.bumbumapps.mynotes.organizer;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Daily habits: check in today, streaks, a 7-day dot strip per habit and a weekly completion chart. */
public class HabitsActivity extends AppCompatActivity {

    static final String[] ICONS = {"💧", "📖", "🏃", "🧘", "😴",
            "🥗", "💊", "✍️", "🎯", "🦷", "🎸", "🌱"};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private List<Habit> habits = new ArrayList<>();
    private List<HabitLog> logs = new ArrayList<>();
    private BottomSheetDialog sheet;
    private String pickedIcon = ICONS[0];
    private int pickedColor;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(Setting.Dark_Mode ? R.style.AppTheme2 : R.style.AppTheme);
        super.onCreate(state);
        setContentView(R.layout.activity_habits);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView add = findViewById(R.id.addHabit);
        add.setTextColor(Setting.Dark_Mode ? 0xFFF5B83D : 0xFF9A6408);
        add.setOnClickListener(v -> edit(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        io.execute(() -> {
            OrganizerDao dao = OrganizerDatabase.get(getApplicationContext()).dao();
            final List<Habit> h = dao.getHabits();
            final List<HabitLog> l = dao.getHabitLogs();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                habits = h;
                logs = l;
                render();
            });
        });
    }

    private void render() {
        final int today = DayKey.today();
        Map<Integer, Set<Integer>> byHabit = new HashMap<>();
        for (HabitLog l : logs) {
            Set<Integer> s = byHabit.get(l.habitId);
            if (s == null) {
                s = new HashSet<>();
                byHabit.put(l.habitId, s);
            }
            s.add(l.day);
        }
        int doneToday = 0, best = 0;
        for (Habit h : habits) {
            Set<Integer> s = byHabit.containsKey(h.id) ? byHabit.get(h.id) : new HashSet<Integer>();
            if (s.contains(today)) {
                doneToday++;
            }
            best = Math.max(best, Streaks.best(s));
        }
        ((TextView) findViewById(R.id.screenCount)).setText(habits.isEmpty() ? ""
                : getString(R.string.habits_done_count, doneToday, habits.size()));

        // weekly chart
        int[] pct = WeekChart.weekPercent(habits, logs, today);
        String[] labels = new String[7];
        SimpleDateFormat narrow = new SimpleDateFormat("EEEEE", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            labels[i] = narrow.format(DayKey.toCalendar(DayKey.addDays(today, i - 6), TimeZone.getDefault()).getTime());
        }
        ((WeekBarsView) findViewById(R.id.weekBars)).setData(pct, labels);
        long from = DayKey.toCalendar(DayKey.addDays(today, -6), TimeZone.getDefault()).getTimeInMillis();
        long to = DayKey.toCalendar(today, TimeZone.getDefault()).getTimeInMillis();
        ((TextView) findViewById(R.id.weekRange)).setText(DateUtils.formatDateRange(this, from, to,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR));
        ((TextView) findViewById(R.id.weekLegend)).setText(best > 0
                ? getString(R.string.habits_legend) + "  ·  " + getString(R.string.habits_best, best)
                : getString(R.string.habits_legend));

        LinearLayout list = findViewById(R.id.habitList);
        list.removeAllViews();
        findViewById(R.id.emptyView).setVisibility(habits.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.weekCard).setVisibility(habits.isEmpty() ? View.GONE : View.VISIBLE);
        for (final Habit h : habits) {
            Set<Integer> s = byHabit.containsKey(h.id) ? byHabit.get(h.id) : new HashSet<Integer>();
            list.addView(row(h, s, today));
        }
    }

    private View row(final Habit h, final Set<Integer> days, final int today) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_habit, null);
        TextView icon = v.findViewById(R.id.habitIcon);
        icon.setText(h.icon);
        icon.setBackground(Ui.roundRect(HabitColors.soft(h.color), Ui.dp(this, 12)));
        if (Setting.Dark_Mode) {
            icon.setBackground(Ui.roundRect((HabitColors.main(h.color) & 0x00FFFFFF) | 0x33000000, Ui.dp(this, 12)));
        }
        ((TextView) v.findViewById(R.id.habitName)).setText(h.name);
        ((TextView) v.findViewById(R.id.habitStats)).setText(getString(R.string.habit_stats,
                Streaks.current(days, today), Streaks.best(days)));
        LinearLayout dots = v.findViewById(R.id.habitDots);
        int size = Ui.dp(this, 16);
        for (int i = 0; i < 7; i++) {
            int day = DayKey.addDays(today, i - 6);
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(Ui.dp(this, 7));
            dot.setLayoutParams(lp);
            boolean done = days.contains(day);
            dot.setBackground(Ui.circle(done ? HabitColors.main(h.color) : 0x00000000,
                    done ? HabitColors.main(h.color) : Ui.themeColor(this, R.attr.Boder),
                    done ? 0 : Ui.dp(this, 1.5f)));
            if (day == today && !done) {
                dot.setBackground(Ui.circle(0x00000000, 0xFFF5B83D, Ui.dp(this, 1.5f)));
            }
            dots.addView(dot);
        }
        ImageView check = v.findViewById(R.id.habitCheckIcon);
        final boolean doneToday = days.contains(today);
        check.setBackground(doneToday ? Ui.circle(HabitColors.main(h.color), 0, 0)
                : Ui.circle(0x00000000, Ui.themeColor(this, R.attr.Titel_2), Ui.dp(this, 1.5f)));
        check.setImageResource(R.drawable.ic_check_bold);
        check.setColorFilter(doneToday ? 0xFFFFFFFF : Ui.themeColor(this, R.attr.Titel_2));
        v.findViewById(R.id.habitCheck).setContentDescription(getString(doneToday
                ? R.string.habit_uncheck_today : R.string.habit_check_today));
        v.findViewById(R.id.habitCheck).setOnClickListener(x -> io.execute(() -> {
            OrganizerDao dao = OrganizerDatabase.get(getApplicationContext()).dao();
            if (doneToday) {
                dao.deleteHabitLog(h.id, today);
            } else {
                dao.saveHabitLog(new HabitLog(h.id, today));
            }
            load();
        }));
        v.setOnClickListener(x -> edit(h));
        return v;
    }

    private void edit(@Nullable final Habit existing) {
        final Habit h = new Habit();
        if (existing != null) {
            h.id = existing.id;
            h.name = existing.name;
            h.icon = existing.icon;
            h.color = existing.color;
            h.startDay = existing.startDay;
        } else {
            h.color = habits.size() % HabitColors.MAIN.length;
            h.icon = ICONS[habits.size() % ICONS.length];
            h.startDay = DayKey.today();
        }
        pickedIcon = h.icon;
        pickedColor = h.color;
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_habit, null);
        ((TextView) content.findViewById(R.id.sheetTitle)).setText(existing == null ? R.string.habit_add : R.string.habit_edit);
        final EditText input = content.findViewById(R.id.habitNameInput);
        input.setText(h.name);
        input.setSelection(input.getText().length());
        final LinearLayout iconRow = content.findViewById(R.id.iconRow);
        final LinearLayout colorRow = content.findViewById(R.id.colorRow);
        final Runnable refresh = () -> {
            iconRow.removeAllViews();
            for (final String ic : ICONS) {
                TextView t = new TextView(this);
                t.setText(ic);
                t.setTextSize(22);
                t.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48));
                lp.setMarginEnd(Ui.dp(this, 8));
                t.setLayoutParams(lp);
                t.setBackground(Ui.sheetChipBg(this, ic.equals(pickedIcon)));
                t.setOnClickListener(x -> {
                    pickedIcon = ic;
                    for (int k = 0; k < iconRow.getChildCount(); k++) {
                        iconRow.getChildAt(k).setBackground(Ui.sheetChipBg(this, ICONS[k].equals(pickedIcon)));
                    }
                });
                iconRow.addView(t);
            }
            colorRow.removeAllViews();
            for (int i = 0; i < HabitColors.MAIN.length; i++) {
                final int ci = i;
                View c = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1);
                lp.setMarginEnd(i == HabitColors.MAIN.length - 1 ? 0 : Ui.dp(this, 8));
                c.setLayoutParams(lp);
                c.setBackground(Ui.circle(HabitColors.main(i), ci == pickedColor
                        ? Ui.themeColor(this, R.attr.Titel_1) : 0, Ui.dp(this, ci == pickedColor ? 3 : 0)));
                c.setOnClickListener(x -> {
                    pickedColor = ci;
                    for (int k = 0; k < colorRow.getChildCount(); k++) {
                        colorRow.getChildAt(k).setBackground(Ui.circle(HabitColors.main(k), k == pickedColor
                                ? Ui.themeColor(this, R.attr.Titel_1) : 0, Ui.dp(this, k == pickedColor ? 3 : 0)));
                    }
                });
                colorRow.addView(c);
            }
        };
        refresh.run();
        View del = content.findViewById(R.id.habitDelete);
        del.setVisibility(existing == null ? View.GONE : View.VISIBLE);
        del.setOnClickListener(v -> {
            if (sheet != null) {
                sheet.dismiss();
            }
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setMessage(R.string.habit_delete_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (d, w) -> io.execute(() -> {
                        OrganizerDao dao = OrganizerDatabase.get(getApplicationContext()).dao();
                        dao.deleteHabitLogs(existing.id);
                        dao.deleteHabit(existing.id);
                        load();
                    })).show();
        });
        content.findViewById(R.id.habitSave).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.habit_empty_name, Toast.LENGTH_SHORT).show();
                return;
            }
            h.name = name;
            h.icon = pickedIcon;
            h.color = pickedColor;
            if (sheet != null) {
                sheet.dismiss();
            }
            io.execute(() -> {
                OrganizerDatabase.get(getApplicationContext()).dao().saveHabit(h);
                load();
            });
        });
        sheet = Ui.sheet(this, content);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sheet != null && sheet.isShowing()) {
            sheet.dismiss();
        }
        io.shutdown();
    }
}
