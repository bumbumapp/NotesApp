You are a senior mobile product designer. Read design/PROMPT_41_V1.md (the brief), the V1 mockups in design/mockups/v1/ and design/REVIEW_41_V1.md. Produce an improved V2 set in design/mockups/v2/ (same file names: home.html, tasks.html, habits.html, journal.html, calendar.html, home_dark.html, tasks_dark.html) plus two new files: home_de.html (German labels: "Aufgaben", "Gewohnheiten", "Tagebuch", "Kalender", "3 offen heute", "2 von 4 erledigt", "5 Tage in Folge", "November", greeting "Guten Abend", title "Notizen") and home_ar.html (Arabic, dir="rtl", layout mirrored: "المهام", "العادات", "اليوميات", "التقويم"). Keep the screen as a 412px wide block centred (margin:auto) so it renders in a 500px headless window. Write nothing else.

FIX these defects:
1. Back arrow colour must be the text colour (var(--text)), never browser-link blue. Draw it as an SVG with stroke currentColor; the drawn arrow glyph starts exactly 20px from the left edge.
2. Right-edge symmetry: the settings icon button (home), the month-next arrow (calendar) and every right-aligned element must end exactly 20px from the right edge (same as the left margin). No negative margins that push glyphs past the margin.
3. Tasks: rows 72px tall max (title 15px/600, due chip right under it, 12px gap), priority shown as a small coloured flag/dot placed right after the due chip inside the text block, not alone at the far right edge; row separators inset 20px. Keep sections "Overdue" and "Today" labels.
4. Calendar: agenda rows with 14px titles and 12px secondary text (not smaller), time column never clipped; month arrows as 44px tap targets whose glyph aligns to the 20px margins; week starts Monday.
5. Journal: label the mood strip with the real weekday letters ending on today, highlight today.
6. Habits: add a tiny legend under the chart ("Amber = partly done, green = all done").
7. Home: floating buttons must not hide text of the last note card (add 90px bottom padding to the notes grid so content scrolls clear of them).
Keep everything else (colours, Bento look, fonts, dark palette) as in V1. Type scale 11/12/14/15/16/20/28/32 only.
