package com.bumbumapps.mynotes.organizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.bumbumapps.mynotes.R;

/** Seven vertical bars: how many of today's habits were done on each of the last seven days. */
public class WeekBarsView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[] percents = new int[7];
    private String[] labels = new String[7];
    private float density;

    public WeekBarsView(Context c) {
        this(c, null);
    }

    public WeekBarsView(Context c, AttributeSet a) {
        super(c, a);
        density = c.getResources().getDisplayMetrics().density;
        paint.setTypeface(Ui.font(c, R.font.jakarta_medium));
        for (int i = 0; i < 7; i++) {
            labels[i] = "";
        }
    }

    public void setData(int[] percents, String[] labels) {
        this.percents = percents;
        this.labels = labels;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            sb.append(labels[i]).append(' ').append(percents[i]).append("%. ");
        }
        setContentDescription(sb.toString().trim());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float labelH = 22 * density;
        float barW = 22 * density;
        float trackTop = 6 * density, trackBottom = h - labelH;
        float slot = w / 7f;
        int track = Ui.themeColor(getContext(), R.attr.BackgroundColor);
        int muted = Ui.themeColor(getContext(), R.attr.Titel_2);
        int text = Ui.themeColor(getContext(), R.attr.Titel_1);
        for (int i = 0; i < 7; i++) {
            float cx = slot * i + slot / 2f;
            rect.set(cx - barW / 2, trackTop, cx + barW / 2, trackBottom);
            paint.setColor(track);
            canvas.drawRoundRect(rect, 8 * density, 8 * density, paint);
            int p = Math.max(0, Math.min(100, percents[i]));
            if (p > 0) {
                float top = trackBottom - Math.max(barW * 0.6f, (trackBottom - trackTop) * p / 100f);
                rect.set(cx - barW / 2, top, cx + barW / 2, trackBottom);
                paint.setColor(p == 100 ? 0xFF2FBF71 : 0xFFF5B83D);
                canvas.drawRoundRect(rect, 8 * density, 8 * density, paint);
            }
            paint.setColor(i == 6 ? text : muted);
            paint.setTextSize(11 * density);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(i == 6);
            canvas.drawText(labels[i], cx, h - 5 * density, paint);
            paint.setFakeBoldText(false);
        }
    }
}
