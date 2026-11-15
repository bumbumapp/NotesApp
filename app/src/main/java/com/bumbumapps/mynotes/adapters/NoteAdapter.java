package com.bumbumapps.mynotes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.logic.Checklist;
import com.bumbumapps.mynotes.listeners.NotesListener;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> notes;
    private NotesListener notesListener;
    private Timer timer;
    private List<Note> noteSource;

    public NoteAdapter(Context context,List<Note> notes, NotesListener notesListener) {
        this.context = context;
        this.notes = notes;
        this.notesListener = notesListener;
        this.noteSource = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(itemView);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        final Note note = notes.get(position);
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notesListener.onNoteClicked(notes.get(position), position);
            }
        });
        holder.layoutNote.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                notesListener.OnNoteLongClickListener(notes.get(position),position);
               return true;
            }
        });
        holder.item_new.setVisibility(position == 0 && note.getPinned() == 0 ? View.VISIBLE : View.GONE);

        holder.textTitle.setText(note.getTitle());
        boolean checklist = note.getNoteType() == Note.TYPE_CHECKLIST;
        String preview = checklist
                ? Checklist.preview(Checklist.parse(note.getNoteText()), 5)
                : note.getSubtitle();
        if (preview == null || preview.trim().isEmpty()){
            holder.textSubtitle.setVisibility(View.GONE);
        }else {
            holder.textSubtitle.setVisibility(View.VISIBLE);
            holder.textSubtitle.setText(preview);
        }
        StringBuilder badges = new StringBuilder();
        if (note.getPinned() == 1) {
            badges.append(context.getString(R.string.badge_pinned));
        }
        if (checklist) {
            java.util.List<Checklist.Item> items = Checklist.parse(note.getNoteText());
            appendBadge(badges, context.getString(R.string.badge_progress,
                    Checklist.doneCount(items), Checklist.count(items)));
        }
        if (note.getCategory() != null && !note.getCategory().trim().isEmpty()) {
            appendBadge(badges, note.getCategory().trim());
        }
        if (note.getReminderAt() > 0) {
            appendBadge(badges, context.getString(R.string.badge_reminder,
                    android.text.format.DateUtils.formatDateTime(context, note.getReminderAt(),
                            android.text.format.DateUtils.FORMAT_SHOW_DATE
                                    | android.text.format.DateUtils.FORMAT_SHOW_TIME
                                    | android.text.format.DateUtils.FORMAT_ABBREV_MONTH)));
        }
        holder.textBadges.setVisibility(badges.length() == 0 ? View.GONE : View.VISIBLE);
        holder.textBadges.setText(badges);
        holder.textDateTime.setText(note.getDateTime());

        GradientDrawable gradientDrawable = (GradientDrawable) holder.layoutNote.getBackground();
        if (note.getColor() != null){
            switch (note.getColor()){
                case "#333333":
                    if (Setting.Dark_Mode){
                        gradientDrawable.setColor(Color.parseColor("#ECECEC"));
                        holder.textTitle.setTextColor(Color.parseColor("#161616"));
                        holder.textSubtitle.setTextColor(Color.parseColor("#6D6D6D"));
                        holder.textDateTime.setTextColor(Color.parseColor("#6D6D6D"));
                    }else {
                        // Bento light: default notes are white cards with dark text.
                        gradientDrawable.setColor(Color.WHITE);
                        holder.textTitle.setTextColor(Color.parseColor("#0E1222"));
                        holder.textSubtitle.setTextColor(Color.parseColor("#5B6275"));
                        holder.textDateTime.setTextColor(Color.parseColor("#5B6275"));
                    }
                    break;
                default:
                    gradientDrawable.setColor(Color.parseColor(note.getColor()));
                    break;
            }
        }else {
            if (Setting.Dark_Mode){
                gradientDrawable.setColor(Color.parseColor("#ECECEC"));
                holder.textTitle.setTextColor(Color.parseColor("#161616"));
                holder.textSubtitle.setTextColor(Color.parseColor("#6D6D6D"));
                holder.textDateTime.setTextColor(Color.parseColor("#6D6D6D"));
            }else {
                gradientDrawable.setColor(Color.WHITE);
                holder.textTitle.setTextColor(Color.parseColor("#0E1222"));
                holder.textSubtitle.setTextColor(Color.parseColor("#5B6275"));
                holder.textDateTime.setTextColor(Color.parseColor("#5B6275"));
            }
        }

        // Badges follow the title colour so "Pinned" stays readable on every note colour.
        holder.textBadges.setTextColor(holder.textTitle.getCurrentTextColor());
        if (note.getImagePath() != null){
            holder.imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
            holder.imageNote.setVisibility(View.VISIBLE);
        }else {
            holder.imageNote.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class  NoteViewHolder extends RecyclerView.ViewHolder{

        TextView textTitle, textSubtitle, textDateTime, item_new, textBadges;
        RelativeLayout layoutNote;
        RoundedImageView imageNote;


        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            item_new = itemView.findViewById(R.id.item_new);
            textBadges = itemView.findViewById(R.id.item_badges);
            textTitle  = itemView.findViewById(R.id.item_textTitlem);
            textSubtitle  = itemView.findViewById(R.id.item_textSubTitle);
            textDateTime  = itemView.findViewById(R.id.item_textDateTime);
            layoutNote  = itemView.findViewById(R.id.item_layoutNote);
            imageNote  = itemView.findViewById(R.id.item_imageNote);
        }

    }

    private static void appendBadge(StringBuilder sb, String badge) {
        if (sb.length() > 0) {
            sb.append("  \u2022  ");
        }
        sb.append(badge);
    }

    /** Replaces the shown list (already filtered and sorted by NoteFilter). */
    @SuppressLint("NotifyDataSetChanged")
    public void setNotes(List<Note> shown) {
        this.notes = new ArrayList<>(shown);
        this.noteSource = this.notes;
        notifyDataSetChanged();
    }

    public void searchNote(final  String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()) {
                    notes = noteSource;
                }else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : noteSource) {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())){
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        },500);
    }

    public void cancelTimer() {
        if (timer != null){
            timer.cancel();
        }
    }

}
