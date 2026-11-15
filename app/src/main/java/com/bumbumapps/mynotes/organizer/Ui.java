package com.bumbumapps.mynotes.organizer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumbumapps.mynotes.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Calendar;
import java.util.TimeZone;

/** Small view helpers shared by the organizer screens. */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int themeColor(Context c, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    /** A filter chip (same look as the home screen chips). */
    public static TextView chip(Context c, CharSequence text, boolean selected, View.OnClickListener click) {
        TextView chip = new TextView(c);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setTypeface(ResourcesCompat.getFont(c, R.font.jakarta_bold));
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
        chip.setTextColor(selected ? ContextCompat.getColor(c, R.color.dark_color) : themeColor(c, R.attr.Titel_1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 40));
        lp.setMarginEnd(dp(c, 8));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(click);
        return chip;
    }

    /** Pill background for chips that sit on a sheet (same colour as the sheet, so they need their own fill). */
    public static android.graphics.drawable.Drawable sheetChipBg(Context c, boolean selected) {
        return roundRect(selected ? ContextCompat.getColor(c, R.color.teal_200) : themeColor(c, R.attr.BackgroundColor),
                dp(c, 20));
    }

    public static TextView sectionLabel(Context c, CharSequence text, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(12);
        t.setTypeface(ResourcesCompat.getFont(c, R.font.jakarta_extrabold));
        t.setTextColor(color);
        t.setPadding(dp(c, 20), dp(c, 16), dp(c, 20), dp(c, 2));
        return t;
    }

    public static View divider(Context c, int insetDp) {
        View v = new View(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMarginStart(dp(c, insetDp));
        lp.setMarginEnd(dp(c, insetDp));
        v.setLayoutParams(lp);
        v.setBackgroundColor(themeColor(c, R.attr.Boder));
        return v;
    }

    public static GradientDrawable roundRect(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    public static GradientDrawable circle(int fill, int strokeColor, int strokePx) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        if (strokePx > 0) {
            g.setStroke(strokePx, strokeColor);
        }
        return g;
    }

    /** Opens a bottom sheet that rises above the keyboard and has a transparent surround. */
    public static BottomSheetDialog sheet(Activity a, View content) {
        BottomSheetDialog d = new BottomSheetDialog(a);
        d.setContentView(content);
        d.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        d.getBehavior().setSkipCollapsed(true);
        if (d.getWindow() != null) {
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        View parent = (View) content.getParent();
        if (parent != null) {
            parent.setBackgroundColor(Color.TRANSPARENT);
        }
        d.show();
        return d;
    }

    /** "Today", "Tomorrow", "Yesterday" or a short date such as "Nov 18". */
    public static String dayLabel(Context c, int day, int today) {
        if (day == today) {
            return c.getString(R.string.day_today);
        }
        if (day == DayKey.addDays(today, 1)) {
            return c.getString(R.string.day_tomorrow);
        }
        if (day == DayKey.addDays(today, -1)) {
            return c.getString(R.string.day_yesterday);
        }
        Calendar cal = DayKey.toCalendar(day, TimeZone.getDefault());
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
        if (DayKey.year(day) != DayKey.year(today)) {
            flags |= DateUtils.FORMAT_SHOW_YEAR;
        } else {
            flags |= DateUtils.FORMAT_NO_YEAR;
        }
        return DateUtils.formatDateTime(c, cal.getTimeInMillis(), flags);
    }

    public static String timeLabel(Context c, long millis) {
        return DateUtils.formatDateTime(c, millis, DateUtils.FORMAT_SHOW_TIME);
    }

    public static Typeface font(Context c, int res) {
        return ResourcesCompat.getFont(c, res);
    }
}
