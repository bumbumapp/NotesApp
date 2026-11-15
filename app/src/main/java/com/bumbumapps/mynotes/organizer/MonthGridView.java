package com.bumbumapps.mynotes.organizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.bumbumapps.mynotes.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Month grid with a weekday header, today / selected markers and coloured dots per day. */
public class MonthGridView extends View {

    public interface Listener {
        void onDay(int dayKey);
    }

    public static final int MARK_TASK = 1;
    public static final int MARK_HABIT = 2;
    public static final int MARK_JOURNAL = 4;
    public static final int MARK_NOTE = 8;
    public static final int[] MARK_COLORS = {0xFFF5B83D, 0xFF2FBF71, 0xFF8B5CF6, 0xFF4C8DFF};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();
    private final float d;
    private int month = DayKey.today();
    private int selected = DayKey.today();
    private int today = DayKey.today();
    private java.util.Map<Integer, Integer> marks = new java.util.HashMap<>();
    private Listener listener;

    public MonthGridView(Context c) {
        this(c, null);
    }

    public MonthGridView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        paint.setTypeface(Ui.font(c, R.font.jakarta_semibold));
        setFocusable(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setState(int monthDay, int selectedDay, java.util.Map<Integer, Integer> marks) {
        this.month = monthDay;
        this.selected = selectedDay;
        this.today = DayKey.today();
        this.marks = marks;
        invalidate();
    }

    private float headerH() {
        return 32 * d;
    }

    private float cellH() {
        return 46 * d;
    }

    @Override
    protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, Math.round(headerH() + cellH() * 6));
    }

    /** Offset (0..6) of the first of the month from the locale's first day of the week. */
    private int leading() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(DayKey.year(month), DayKey.month(month), 1);
        int first = c.getFirstDayOfWeek();
        return Math.floorMod(c.get(Calendar.DAY_OF_WEEK) - first, 7);
    }

    private boolean rtl() {
        return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    private int colOf(int col) {
        return rtl() ? 6 - col : col;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float cw = w / 7f;
        int text = Ui.themeColor(getContext(), R.attr.Titel_1);
        int muted = Ui.themeColor(getContext(), R.attr.Titel_2);
        paint.setTextAlign(Paint.Align.CENTER);
        // weekday header
        Calendar hc = Calendar.getInstance();
        int first = hc.getFirstDayOfWeek();
        SimpleDateFormat narrow = new SimpleDateFormat("EEEEE", Locale.getDefault());
        paint.setTextSize(11 * d);
        paint.setColor(muted);
        for (int col = 0; col < 7; col++) {
            Calendar x = Calendar.getInstance();
            x.set(2023, Calendar.JANUARY, 1); // a Sunday
            x.add(Calendar.DAY_OF_MONTH, Math.floorMod(first - Calendar.SUNDAY + col, 7));
            canvas.drawText(narrow.format(x.getTime()), colOf(col) * cw + cw / 2, headerH() * 0.65f, paint);
        }
        int lead = leading();
        int start = DayKey.addDays(DayKey.of(DayKey.year(month), DayKey.month(month), 1), -lead);
        for (int i = 0; i < 42; i++) {
            int key = DayKey.addDays(start, i);
            int row = i / 7, col = colOf(i % 7);
            float cx = col * cw + cw / 2;
            float top = headerH() + row * cellH();
            float cy = top + 20 * d;
            boolean inMonth = DayKey.month(key) == DayKey.month(month);
            if (key == today) {
                paint.setColor(0xFFF5B83D);
                canvas.drawCircle(cx, cy, 15 * d, paint);
            }
            if (key == selected && inMonth) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.5f * d);
                paint.setColor(key == today ? 0xFF7A4B05 : text);
                canvas.drawCircle(cx, cy, 15 * d, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setTextSize(13 * d);
            paint.setFakeBoldText(key == today || key == selected);
            paint.setColor(key == today ? 0xFF131418 : inMonth ? text : muted);
            paint.setAlpha(inMonth ? 255 : 90);
            String s = String.valueOf(DayKey.day(key));
            paint.getTextBounds(s, 0, s.length(), bounds);
            canvas.drawText(s, cx, cy + bounds.height() / 2f, paint);
            paint.setAlpha(255);
            paint.setFakeBoldText(false);
            if (inMonth) {
                Integer m = marks.get(key);
                if (m != null && m != 0) {
                    int n = Integer.bitCount(m);
                    float gap = 7 * d;
                    float x0 = cx - (n - 1) * gap / 2f;
                    int k = 0;
                    for (int b = 0; b < 4; b++) {
                        if ((m & (1 << b)) != 0) {
                            paint.setColor(MARK_COLORS[b]);
                            canvas.drawCircle(x0 + k * gap, cy + 20 * d, 2.2f * d, paint);
                            k++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP) {
            float y = e.getY() - headerH();
            if (y < 0) {
                return true;
            }
            int row = (int) (y / cellH());
            int col = (int) (e.getX() / (getWidth() / 7f));
            if (row < 0 || row > 5 || col < 0 || col > 6) {
                return true;
            }
            int slot = rtl() ? 6 - col : col;
            int key = DayKey.addDays(DayKey.of(DayKey.year(month), DayKey.month(month), 1), -leading() + row * 7 + slot);
            if (DayKey.month(key) == DayKey.month(month) && listener != null) {
                performClick();
                listener.onDay(key);
            }
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
