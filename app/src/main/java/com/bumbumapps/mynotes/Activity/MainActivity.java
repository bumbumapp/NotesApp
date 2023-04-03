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
import android.widget.EditText;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bumbumapps.mynotes.Activity.Note.CreateNoteActivity;
import com.bumbumapps.mynotes.Activity.Setting.SettingActivity;
import com.bumbumapps.mynotes.Methods.Methods;
import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.adapters.NoteAdapter;
import com.bumbumapps.mynotes.database.DeleteDatabase;
import com.bumbumapps.mynotes.database.NotesDatabase;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.listeners.InterAdListener;
import com.bumbumapps.mynotes.listeners.NotesListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

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
    private int noteClickedPostion = -1;
    AlertDialog dialogDeletNote;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
        relativeLayout=findViewById(R.id.realtiv);
        adView=findViewById(R.id.adView);
        loadbannerads();
        scheduleInterstitial();
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
                noteClickedPostion = position;
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                intent.putExtra("isViemOrUpdate", true);
                intent.putExtra("note", note);
                startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
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
        noteAdapter = new NoteAdapter(this, noteList, new NotesListener() {
            @Override
            public void onNoteClicked(Note note, int position) {
                methods.showInter(position, note, "");
            }

            @Override
            public void OnNoteLongClickListener(Note note, int position) {

                    if (dialogDeletNote == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        View view = LayoutInflater.from(MainActivity.this).inflate(
                                R.layout.layout_delete_note_move, (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
                        );
                        builder.setView(view);
                        dialogDeletNote = builder.create();
                        if (dialogDeletNote.getWindow() != null){
                            dialogDeletNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                        }
                        view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                @SuppressLint("StaticFieldLeak")
                                class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();
                                        deleteSaveNote(note);
                                    }

                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao()
                                                .deletNote(note);

                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        try {
                                            Intent refresh = new Intent(MainActivity.this, MainActivity.class);
                                            startActivity(refresh);//Start the same Activity
                                            finish(); //finish Activity.

                                     //   finish();
                                        }catch (Exception e){
                                           Log.d("Tag","Error"+e.getMessage());
                                        }

                                    }
                                }




                                    new DeleteNoteTask().execute();


                            }
                        });

                        view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialogDeletNote.dismiss();
                            }
                        });
                    }

                    dialogDeletNote.show();
                }

        });

        notesRecyclerView.setAdapter(noteAdapter);
        getNotes(REQUST_CODE_SHOW_NOTES, false);


        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                noteAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0){
                    noteAdapter.searchNote(editable.toString());
                }

            }
        });

//        LinearLayout adView = findViewById(R.id.adView_main);
//        methods.showBannerAd(adView);

    }

    private void deleteSaveNote(Note note) {
    @SuppressLint("StaticFieldLeak")
    class  DeleteSaveNoteTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            DeleteDatabase.getNotesDatabase(getApplicationContext()).noteDao().insertNote(note);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }
    new DeleteSaveNoteTask().execute();


    }
    private void getNotes(final int requestCode, final  boolean isNoteDeleted){

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>>{
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getNotesDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if (requestCode == REQUST_CODE_SHOW_NOTES){
                    noteList.addAll(notes);
                    noteAdapter.notifyDataSetChanged();
                }else if (requestCode == REQUEST_CODE_ADD_NOTE){
                    noteList.add(0, notes.get(0));
                    noteAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                }else if (requestCode == REQUEST_CODE_UPDATE_NOTE){
                    noteList.remove(noteClickedPostion);
                    if (isNoteDeleted){
                        noteAdapter.notifyItemRemoved(noteClickedPostion);
                    }else {
                        noteList.add(noteClickedPostion, notes.get(noteClickedPostion));
                        noteAdapter.notifyItemChanged(noteClickedPostion);
                    }
                }
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
            if (data != null){
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false));
            }
        }
    }




    private void setUpInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-8444865753152507/8521044739", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i("TAG", "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i("TAG", loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }
    private void scheduleInterstitial() {
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        setUpInterstitialAd();
                    }
                });

            }
        }, 1, 3, TimeUnit.MINUTES);

    }
    private void loadbannerads() {
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public void OnClick(View view) {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    startActivityForResult(
                            new Intent(MainActivity.this, CreateNoteActivity.class),
                            REQUEST_CODE_ADD_NOTE
                    );
                    Log.d("TAG", "The ad was dismissed.");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                    Log.d("TAG", "The ad failed to show.");
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    mInterstitialAd = null;
                    Log.d("TAG", "The ad was shown.");
                }
            });
        }
        else{
            startActivityForResult(
                    new Intent(getApplicationContext(), CreateNoteActivity.class),
                    REQUEST_CODE_ADD_NOTE
            );
        }

    }
}