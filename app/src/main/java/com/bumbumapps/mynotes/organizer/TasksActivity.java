package com.bumbumapps.mynotes.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.logic.ReminderPresets;
import com.bumbumapps.mynotes.reminder.ReminderUi;
import com.bumbumapps.mynotes.reminder.TaskReminders;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** To-do list: quick add, due dates, priorities, reminders; Today / Upcoming / All / Done views. */
public class TasksActivity extends AppCompatActivity {

    public static final String EXTRA_DUE_DAY = "due_day";
    private static final String STATE_TAB = "tab";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private List<Task> all = new ArrayList<>();
    private int tab = TaskLogic.TODAY;
    private boolean tabChosen;
    private int quickDue; // 0 = no date
    private boolean quickDueTouched;
    private EditText quickAdd;
    private BottomSheetDialog sheet;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        setTheme(Setting.Dark_Mode ? R.style.AppTheme2 : R.style.AppTheme);
        super.onCreate(state);
        setContentView(R.layout.activity_tasks);
        if (state != null) {
            tab = state.getInt(STATE_TAB, tab);
            tabChosen = true;
        }
        quickAdd = findViewById(R.id.quickAdd);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.quickSend).setOnClickListener(v -> addQuick());
        findViewById(R.id.quickDue).setOnClickListener(v -> pickQuickDue());
        findViewById(R.id.quickDueLabel).setOnClickListener(v -> {
            quickDue = 0;
            quickDueTouched = true;
            updateQuickLabel();
        });
        quickAdd.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addQuick();
                return true;
            }
            return false;
        });
        findViewById(R.id.clearDone).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage(R.string.task_clear_done_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> io.execute(() -> {
                    OrganizerDatabase.get(getApplicationContext()).dao().deleteDoneTasks();
                    load();
                })).show());
        int due = getIntent().getIntExtra(EXTRA_DUE_DAY, 0);
        if (due != 0 && DayKey.isValid(due)) {
            quickDue = due;
            quickDueTouched = true;
            tab = TaskLogic.ALL;
            tabChosen = true;
            quickAdd.requestFocus();
        }
        updateQuickLabel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_TAB, tab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        io.execute(() -> {
            final List<Task> tasks = OrganizerDatabase.get(getApplicationContext()).dao().getTasks();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                all = tasks;
                if (!tabChosen) {
                    tabChosen = true;
                    int today = DayKey.today();
                    if (TaskLogic.openToday(all, today) == 0 && TaskLogic.openCount(all) > 0) {
                        tab = TaskLogic.ALL;
                    }
                }
                render();
            });
        });
    }

    private void updateQuickLabel() {
        TextView l = findViewById(R.id.quickDueLabel);
        if (quickDue == 0) {
            l.setVisibility(View.GONE);
        } else {
            l.setVisibility(View.VISIBLE);
            l.setText(getString(R.string.task_due_chip_clear, Ui.dayLabel(this, quickDue, DayKey.today())));
        }
    }

    private void pickQuickDue() {
        final int today = DayKey.today();
        final String[] labels = {getString(R.string.day_today), getString(R.string.day_tomorrow),
                getString(R.string.task_pick_date), getString(R.string.task_no_date)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.task_due_date)
                .setItems(labels, (d, which) -> {
                    quickDueTouched = true;
                    if (which == 0) {
                        quickDue = today;
                        updateQuickLabel();
                    } else if (which == 1) {
                        quickDue = DayKey.addDays(today, 1);
                        updateQuickLabel();
                    } else if (which == 2) {
                        pickDate(quickDue == 0 ? today : quickDue, day -> {
                            quickDue = day;
                            updateQuickLabel();
                        });
                    } else {
                        quickDue = 0;
                        updateQuickLabel();
                    }
                })
                .show();
    }

    interface DayCallback {
        void onDay(int day);
    }

    private void pickDate(int initial, final DayCallback cb) {
        new DatePickerDialog(this, (v, y, m, d) -> cb.onDay(DayKey.of(y, m, d)),
                DayKey.year(initial), DayKey.month(initial), DayKey.day(initial)).show();
    }

    private void addQuick() {
        String title = quickAdd.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.task_empty_title, Toast.LENGTH_SHORT).show();
            return;
        }
        final Task t = new Task();
        t.title = title;
        t.createdAt = System.currentTimeMillis();
        t.dueDay = quickDueTouched ? quickDue : (tab == TaskLogic.TODAY ? DayKey.today() : 0);
        quickAdd.setText("");
        quickDue = 0;
        quickDueTouched = false;
        updateQuickLabel();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(quickAdd.getWindowToken(), 0);
        }
        io.execute(() -> {
            OrganizerDatabase.get(getApplicationContext()).dao().saveTask(t);
            load();
        });
    }

    private void render() {
        final int today = DayKey.today();
        int open = TaskLogic.openCount(all);
        ((TextView) findViewById(R.id.screenCount)).setText(getString(R.string.tasks_open_count, open));

        LinearLayout chips = findViewById(R.id.tabChips);
        chips.removeAllViews();
        String[] names = {getString(R.string.tab_today), getString(R.string.tab_upcoming),
                getString(R.string.tab_all), getString(R.string.tab_done)};
        for (int i = 0; i < 4; i++) {
            final int t = i;
            chips.addView(Ui.chip(this, names[i], tab == i, v -> {
                tab = t;
                tabChosen = true;
                render();
            }));
        }

        LinearLayout list = findViewById(R.id.taskList);
        list.removeAllViews();
        List<Task> shown = TaskLogic.filter(all, tab, today);
        TextView empty = findViewById(R.id.emptyView);
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(tab == TaskLogic.DONE ? R.string.tasks_empty_done
                : tab == TaskLogic.TODAY ? R.string.tasks_empty_today : R.string.tasks_empty);
        findViewById(R.id.clearDone).setVisibility(tab == TaskLogic.DONE && !shown.isEmpty() ? View.VISIBLE : View.GONE);
        if (shown.isEmpty()) {
            return;
        }
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(0, 0, 0, Ui.dp(this, 8));
        list.addView(card);
        int red = ContextCompat.getColor(this, R.color.colorDelete);
        int muted = Ui.themeColor(this, R.attr.Titel_2);
        String section = null;
        boolean firstRow = true;
        for (Task t : shown) {
            String s = tab == TaskLogic.DONE ? null
                    : TaskLogic.isOverdue(t, today) ? getString(R.string.section_overdue)
                    : t.dueDay == today ? getString(R.string.day_today)
                    : t.dueDay > today ? getString(R.string.section_upcoming)
                    : getString(R.string.section_no_date);
            if (s != null && !s.equals(section) && (tab == TaskLogic.ALL || tab == TaskLogic.TODAY)) {
                section = s;
                card.addView(Ui.sectionLabel(this, s, s.equals(getString(R.string.section_overdue)) ? red : muted));
                firstRow = true;
            }
            if (!firstRow) {
                card.addView(Ui.divider(this, 20));
            }
            firstRow = false;
            card.addView(row(t, today));
        }
    }

    private View row(final Task t, int today) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_task, null);
        TextView title = v.findViewById(R.id.taskTitle);
        title.setText(t.title);
        ImageView check = v.findViewById(R.id.taskCheckIcon);
        check.setBackgroundResource(t.done ? R.drawable.bg_circle_done : R.drawable.bg_circle_check);
        check.setImageResource(t.done ? R.drawable.ic_check_bold : 0);
        check.setColorFilter(0xFFFFFFFF);
        if (t.done) {
            title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            title.setTextColor(Ui.themeColor(this, R.attr.Titel_2));
        }
        TextView due = v.findViewById(R.id.taskDue);
        boolean overdue = TaskLogic.isOverdue(t, today);
        if (t.dueDay == 0 && t.reminderAt == 0) {
            due.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder();
            if (overdue) {
                sb.append(getString(R.string.section_overdue)).append(" · ");
            }
            if (t.dueDay != 0) {
                sb.append(Ui.dayLabel(this, t.dueDay, today));
            }
            if (t.reminderAt > 0) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append("⏰ ").append(Ui.timeLabel(this, t.reminderAt));
            }
            due.setText(sb.toString());
            if (overdue) {
                due.setTextColor(0xFFC5303A);
                due.setBackground(Ui.roundRect(0x1AFF4842, Ui.dp(this, 8)));
            }
        }
        TextView pr = v.findViewById(R.id.taskPriority);
        if (t.priority == Task.HIGH && !t.done) {
            pr.setText("● " + getString(R.string.priority_high));
            pr.setTextColor(0xFFE5484D);
        } else if (t.priority == Task.LOW && !t.done) {
            pr.setText("● " + getString(R.string.priority_low));
            pr.setTextColor(Ui.themeColor(this, R.attr.Titel_2));
        } else {
            pr.setVisibility(View.GONE);
        }
        if (due.getVisibility() == View.GONE && pr.getVisibility() == View.GONE) {
            v.findViewById(R.id.taskMeta).setVisibility(View.GONE);
        }
        v.findViewById(R.id.taskCheck).setOnClickListener(x -> toggle(t));
        v.setOnClickListener(x -> edit(t));
        return v;
    }

    private void toggle(final Task t) {
        t.done = !t.done;
        io.execute(() -> {
            OrganizerDatabase.get(getApplicationContext()).dao().saveTask(t);
            if (t.done) {
                TaskReminders.cancel(getApplicationContext(), t.id);
            } else {
                TaskReminders.schedule(getApplicationContext(), t);
            }
            load();
        });
    }

    private void edit(final Task original) {
        final Task t = new Task();
        t.id = original.id;
        t.title = original.title;
        t.dueDay = original.dueDay;
        t.priority = original.priority;
        t.done = original.done;
        t.reminderAt = original.reminderAt;
        t.createdAt = original.createdAt;
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
        final EditText input = content.findViewById(R.id.taskTitleInput);
        input.setText(t.title);
        input.setSelection(input.getText().length());
        final TextView dueBtn = content.findViewById(R.id.taskDueButton);
        final TextView remBtn = content.findViewById(R.id.taskReminderButton);
        final LinearLayout prRow = content.findViewById(R.id.priorityRow);
        final Runnable refresh = () -> {
            dueBtn.setText(t.dueDay == 0 ? getString(R.string.task_no_date) : Ui.dayLabel(this, t.dueDay, DayKey.today()));
            remBtn.setText(t.reminderAt > 0
                    ? android.text.format.DateUtils.formatDateTime(this, t.reminderAt,
                    android.text.format.DateUtils.FORMAT_SHOW_DATE | android.text.format.DateUtils.FORMAT_SHOW_TIME
                            | android.text.format.DateUtils.FORMAT_ABBREV_MONTH)
                    : getString(R.string.task_no_reminder));
            prRow.removeAllViews();
            String[] names = {getString(R.string.priority_low), getString(R.string.priority_normal), getString(R.string.priority_high)};
            for (int i = 0; i < 3; i++) {
                final int p = i;
                TextView c = Ui.chip(this, names[i], t.priority == i, null);
                c.setBackground(Ui.sheetChipBg(this, t.priority == i));
                c.setOnClickListener(x -> {
                    t.priority = p;
                    for (int k = 0; k < 3; k++) {
                        TextView o = (TextView) prRow.getChildAt(k);
                        boolean sel = k == p;
                        o.setBackground(Ui.sheetChipBg(this, sel));
                        o.setTextColor(sel ? ContextCompat.getColor(this, R.color.dark_color) : Ui.themeColor(this, R.attr.Titel_1));
                    }
                });
                prRow.addView(c);
            }
        };
        refresh.run();
        dueBtn.setOnClickListener(v -> {
            final int today = DayKey.today();
            final String[] labels = {getString(R.string.day_today), getString(R.string.day_tomorrow),
                    getString(R.string.task_pick_date), getString(R.string.task_no_date)};
            new AlertDialog.Builder(this).setItems(labels, (d, which) -> {
                if (which == 0) {
                    t.dueDay = today;
                } else if (which == 1) {
                    t.dueDay = DayKey.addDays(today, 1);
                } else if (which == 3) {
                    t.dueDay = 0;
                } else {
                    pickDate(t.dueDay == 0 ? today : t.dueDay, day -> {
                        t.dueDay = day;
                        refresh.run();
                    });
                    return;
                }
                refresh.run();
            }).show();
        });
        remBtn.setOnClickListener(v -> chooseReminder(t, refresh));
        content.findViewById(R.id.taskDelete).setOnClickListener(v -> {
            if (sheet != null) {
                sheet.dismiss();
            }
            io.execute(() -> {
                OrganizerDatabase.get(getApplicationContext()).dao().deleteTask(t.id);
                TaskReminders.cancel(getApplicationContext(), t.id);
                load();
            });
        });
        content.findViewById(R.id.taskSave).setOnClickListener(v -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, R.string.task_empty_title, Toast.LENGTH_SHORT).show();
                return;
            }
            t.title = title;
            if (sheet != null) {
                sheet.dismiss();
            }
            io.execute(() -> {
                OrganizerDatabase.get(getApplicationContext()).dao().saveTask(t);
                TaskReminders.schedule(getApplicationContext(), t);
                load();
            });
            if (t.reminderAt > System.currentTimeMillis() && t.reminderAt != original.reminderAt) {
                ReminderUi.ensureNotifications(this);
            }
        });
        sheet = Ui.sheet(this, content);
    }

    private void chooseReminder(final Task t, final Runnable refresh) {
        final long now = System.currentTimeMillis();
        final TimeZone tz = TimeZone.getDefault();
        final List<String> labels = new ArrayList<>();
        final List<Long> times = new ArrayList<>(); // -1 pick, -2 none
        labels.add(getString(R.string.reminder_in_hour));
        times.add(ReminderPresets.inOneHour(now));
        if (t.dueDay != 0) {
            Calendar c = DayKey.toCalendar(t.dueDay, tz);
            c.set(Calendar.HOUR_OF_DAY, 9);
            c.set(Calendar.MINUTE, 0);
            if (c.getTimeInMillis() > now + 60_000L) {
                labels.add(getString(R.string.task_reminder_due_morning));
                times.add(c.getTimeInMillis());
            }
        }
        labels.add(getString(R.string.reminder_pick));
        times.add(-1L);
        if (t.reminderAt > 0) {
            labels.add(getString(R.string.action_reminder_remove));
            times.add(-2L);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.reminder_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    long v = times.get(which);
                    if (v == -2L) {
                        t.reminderAt = 0;
                        refresh.run();
                    } else if (v == -1L) {
                        final Calendar c = Calendar.getInstance();
                        c.add(Calendar.HOUR_OF_DAY, 1);
                        new DatePickerDialog(this, (dv, y, m, day) -> new TimePickerDialog(this, (tv, h, min) -> {
                            Calendar ch = Calendar.getInstance();
                            ch.set(y, m, day, h, min, 0);
                            ch.set(Calendar.MILLISECOND, 0);
                            if (!ReminderPresets.isFuture(ch.getTimeInMillis(), System.currentTimeMillis())) {
                                Toast.makeText(this, R.string.reminder_past, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            t.reminderAt = ch.getTimeInMillis();
                            refresh.run();
                        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                                android.text.format.DateFormat.is24HourFormat(this)).show(),
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
                    } else {
                        t.reminderAt = v;
                        refresh.run();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ReminderUi.onPermissionResult(this, requestCode, grantResults);
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
