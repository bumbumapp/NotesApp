package com.bumbumapps.mynotes.Activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.bumbumapps.mynotes.Activity.Note.ChecklistActivity;
import com.bumbumapps.mynotes.Activity.Note.CreateNoteActivity;
import com.bumbumapps.mynotes.logic.Checklist;
import com.bumbumapps.mynotes.logic.NoteFilter;
import com.bumbumapps.mynotes.reminder.ReminderScheduler;
import com.bumbumapps.mynotes.reminder.ReminderUi;
import com.bumbumapps.mynotes.Activity.Setting.SettingActivity;
import com.bumbumapps.mynotes.AdsHelper;
import com.bumbumapps.mynotes.Methods.Methods;
import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.adapters.NoteAdapter;
import com.bumbumapps.mynotes.database.DeleteDatabase;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.listeners.InterAdListener;
import com.bumbumapps.mynotes.listeners.NotesListener;

public class MainActivity extends AppCompatActivity{

    private Methods methods;
    public  static  final int REQUEST_CODE_ADD_NOTE = 1;
    public  static  final int REQUEST_CODE_UPDATE_NOTE = 2;
    private static final int REQUST_CODE_SHOW_NOTES = 3;
    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    EditText inputSearch;
    private NoteAdapter noteAdapter;
    private RelativeLayout relativeLayout;
    private ImageButton settingbutton;
    public static final String EXTRA_OPEN_NOTE_ID = "open_note_id";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private String query = "";
    private String filter = NoteFilter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Setting.Dark_Mode ) {
            setTheme(R.style.AppTheme2);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);
        inputSearch = findViewById(R.id.inputSearch);
//        setTitle(R.string.app_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        ((android.widget.TextView) findViewById(R.id.textGreeting)).setText(hour >= 5 && hour < 12
                ? R.string.bento_greeting_morning : hour >= 12 && hour < 18
                ? R.string.bento_greeting_afternoon : R.string.bento_greeting_evening);
        relativeLayout=findViewById(R.id.realtiv);
        loadbannerads();
        if (Setting.Dark_Mode ) {
            findViewById(R.id.layoutSearch).setBackgroundResource(R.drawable.blacksearch_bacground);
            toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.background_Night));
            relativeLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.background_Night));
            setTheme(R.style.AppTheme2);
        }
        methods = new Methods(this);

        methods = new Methods(this, new InterAdListener() {
            @Override
            public void onClick(int position, Note note, String type) {
                openNote(note);
            }

        });






        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        settingbutton=findViewById(R.id.settingsbutton);
        settingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
                finish();
            }
        });
        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, new ArrayList<>(), new NotesListener() {
            @Override
            public void onNoteClicked(Note note, int position) {
                methods.showInter(position, note, "");
            }

            @Override
            public void OnNoteLongClickListener(Note note, int position) {
                showNoteActions(note);
            }
        });

        notesRecyclerView.setAdapter(noteAdapter);

        findViewById(R.id.imageAddChecklist).setOnClickListener(v ->
                startActivity(new Intent(this, ChecklistActivity.class)));

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                query = editable.toString();
                refreshList();
            }
        });
        handleOpenNoteIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenNoteIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    /** A tapped reminder notification opens the note it belongs to. */
    private void handleOpenNoteIntent(Intent intent) {
        final int id = intent == null ? -1 : intent.getIntExtra(EXTRA_OPEN_NOTE_ID, -1);
        if (id < 0) {
            return;
        }
        intent.removeExtra(EXTRA_OPEN_NOTE_ID);
        io.execute(() -> {
            Note n = NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao().getNote(id);
            if (n != null) {
                runOnUiThread(() -> openNote(n));
            }
        });
    }

    private void openNote(Note note) {
        Intent intent;
        if (note.getNoteType() == Note.TYPE_CHECKLIST) {
            intent = new Intent(getApplicationContext(), ChecklistActivity.class);
            intent.putExtra(ChecklistActivity.EXTRA_NOTE, note);
        } else {
            intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            intent.putExtra("isViemOrUpdate", true);
            intent.putExtra("note", note);
        }
        startActivity(intent);
    }

    private void loadNotes() {
        io.execute(() -> {
            final List<Note> notes = NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao().getAllNotes();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                noteList = notes;
                refreshList();
            });
        });
    }

    private void refreshList() {
        List<String> cats = NoteFilter.categories(noteList);
        boolean any = false;
        boolean hasPinned = false, hasChecklist = false, hasReminder = false;
        for (Note n : noteList) {
            hasPinned |= n.getPinned() == 1;
            hasChecklist |= n.getNoteType() == Note.TYPE_CHECKLIST;
            hasReminder |= n.getReminderAt() > 0;
        }
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        keys.add(NoteFilter.ALL);
        labels.add(getString(R.string.filter_all));
        if (hasPinned) {
            keys.add(NoteFilter.PINNED);
            labels.add(getString(R.string.filter_pinned));
        }
        if (hasChecklist) {
            keys.add(NoteFilter.CHECKLISTS);
            labels.add(getString(R.string.filter_checklists));
        }
        if (hasReminder) {
            keys.add(NoteFilter.REMINDERS);
            labels.add(getString(R.string.filter_reminders));
        }
        for (String c : cats) {
            keys.add(c);
            labels.add(c);
        }
        if (!keys.contains(filter)) {
            filter = NoteFilter.ALL;
        }
        LinearLayout chips = findViewById(R.id.filterChips);
        chips.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            TextView chip = new TextView(this);
            boolean sel = key.equals(filter);
            chip.setText(labels.get(i));
            chip.setTextSize(13);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding((int) (14 * d), 0, (int) (14 * d), 0);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setBackgroundResource(sel ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            chip.setTextColor(sel ? ContextCompat.getColor(this, R.color.dark_color) : resolveColor(R.attr.Titel_1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, (int) (40 * d));
            lp.setMarginEnd((int) (8 * d));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                filter = key;
                refreshList();
            });
            chips.addView(chip);
        }
        findViewById(R.id.chipScroll).setVisibility(keys.size() > 1 ? View.VISIBLE : View.GONE);
        List<Note> shown = NoteFilter.apply(noteList, query, filter);
        noteAdapter.setNotes(shown);
        findViewById(R.id.emptyNotes).setVisibility(shown.isEmpty() && !noteList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int resolveColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void showNoteActions(final Note note) {
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();
        labels.add(getString(note.getPinned() == 1 ? R.string.action_unpin : R.string.action_pin));
        actions.add(() -> {
            note.setPinned(note.getPinned() == 1 ? 0 : 1);
            saveMeta(note);
        });
        labels.add(getString(R.string.action_category));
        actions.add(() -> chooseCategory(note));
        labels.add(getString(R.string.action_reminder));
        actions.add(() -> ReminderUi.show(this, note, this::loadNotes));
        labels.add(getString(R.string.action_share));
        actions.add(() -> shareNote(note));
        labels.add(getString(R.string.action_trash));
        actions.add(() -> confirmTrash(note));
        new AlertDialog.Builder(this)
                .setTitle(note.getTitle() == null || note.getTitle().trim().isEmpty()
                        ? getString(R.string.app_name) : note.getTitle())
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void saveMeta(final Note note) {
        io.execute(() -> {
            NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao().insertNote(note);
            runOnUiThread(this::loadNotes);
        });
    }

    private void chooseCategory(final Note note) {
        final List<String> cats = NoteFilter.categories(noteList);
        final List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.category_none));
        labels.addAll(cats);
        labels.add(getString(R.string.category_new));
        new AlertDialog.Builder(this)
                .setTitle(R.string.category_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    if (which == 0) {
                        note.setCategory(null);
                        saveMeta(note);
                    } else if (which == labels.size() - 1) {
                        askNewCategory(note);
                    } else {
                        note.setCategory(cats.get(which - 1));
                        saveMeta(note);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void askNewCategory(final Note note) {
        final EditText input = new EditText(this);
        input.setHint(R.string.category_new_hint);
        input.setSingleLine(true);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        FrameLayout box = new FrameLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(R.string.category_new)
                .setView(box)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String c = NoteFilter.cleanCategory(input.getText().toString());
                    note.setCategory(c.isEmpty() ? null : c);
                    saveMeta(note);
                })
                .show();
    }

    private void shareNote(Note note) {
        String body = note.getNoteType() == Note.TYPE_CHECKLIST
                ? Checklist.preview(Checklist.parse(note.getNoteText()), Integer.MAX_VALUE)
                : (note.getSubtitle() == null ? "" : note.getSubtitle() + "\n\n")
                + android.text.Html.fromHtml(note.getNoteText() == null ? "" : note.getNoteText()).toString().trim();
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, note.getTitle())
                .putExtra(Intent.EXTRA_TEXT, (note.getTitle() == null ? "" : note.getTitle() + "\n\n") + body);
        startActivity(Intent.createChooser(send, getString(R.string.action_share)));
    }

    private void confirmTrash(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.move_to_recycle_bin)
                .setMessage(R.string.are_you_sure_you_want_to_move_to_recycle_bin_this_note)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.move_to_recycle_bin, (d, w) -> io.execute(() -> {
                    Context app = getApplicationContext();
                    ReminderScheduler.cancel(app, note.getId());
                    DeleteDatabase.getNotesDatabase(app).noteDao().insertNote(note);
                    NotesDatabase.getNotesDatabase(app).noteDao().deletNote(note);
                    runOnUiThread(this::loadNotes);
                }))
                .show();
    }

    private void loadbannerads() {
        // UMP consent first; Mobile Ads starts and requests ads only after canRequestAds().
        final android.view.ViewGroup container = findViewById(R.id.adView);
        AdsHelper.gatherConsent(this, () -> {
            AdsHelper.loadBanner(this, container, getString(R.string.banner_id));
            AdsHelper.preloadInterstitial(this, getString(R.string.interstitial_id));
        });
    }

    public void OnClick(View view) {
        // Capped interstitial (AdsHelper: 15 min) before opening the editor, then continue.
        AdsHelper.showInterstitialIfReady(this, getString(R.string.interstitial_id), () -> startActivityForResult(
                new Intent(getApplicationContext(), CreateNoteActivity.class),
                REQUEST_CODE_ADD_NOTE
        ));
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