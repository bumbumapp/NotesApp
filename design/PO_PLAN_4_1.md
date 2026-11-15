# NotesApp - product plan for Huawei rule 4.1 (round 2, 2026-11-16)

Reviewer feedback: "Your app offers only one simple content - Notes". Huawei phones ship a built-in Notes app,
so "Notes: Notepad with Lock" must clearly be more: a personal organizer.

## What exists (after e9118f5, v1.5.0)
Rich-text notes (colors, photo, link), checklist notes, pin, categories + filter chips, per-note reminders
(inexact alarms, POST_NOTIFICATIONS asked only when a reminder is set), share as text, PIN/fingerprint app lock,
recycle bin, dark mode, search, TXT/image export. Home = a plain list of notes. Nothing on the home screen says
"organizer"; all notes features are notes features.

## Competitors in the category (Google Keep, Microsoft To Do, Samsung Notes, Journey, Habitica, Loop Habit Tracker, Day One)
Users of "notes + organizer" apps expect: dated to-do lists, a day/month calendar, a daily journal with mood,
habit streaks, backup. Huawei's own Notes already does notes/checklists, so the differentiation must be separate
content types with their own screens.

## New modules (all offline, no new permissions, no new data collection)
1. Tasks - to-do list with due date, priority (low/normal/high), optional reminder; views Today / Upcoming / All / Done;
   overdue highlighted; quick-add bar; complete/uncomplete, edit, delete. Reminders reuse the app's inexact alarm
   channel; notification permission requested only when a task reminder is set.
2. Habit tracker - daily habits with colour, check-in for today, current streak and best streak, a 7-day dot strip
   per habit and a "this week" bar chart of completion; edit/delete habits.
3. Journal - one diary entry per day with mood (5 levels) and free text; current streak, total entries, a "mood this
   week" strip; list newest first; edit/delete; "write today" button.
4. Calendar - month grid with dots for notes (by creation day), tasks (by due day), journal entries and habit
   check-ins; tap a day to see everything on it and jump into it; "+" to add a task/journal for the chosen day.
5. Backup and restore (Settings) - export every note, task, habit and journal entry to one JSON file with the system
   file picker (SAF, no storage permission) and restore from it; export of a note as TXT already exists.

## Home screen
Bento "Organizer" grid right under the header: four tiles (Tasks, Habits, Journal, Calendar), each with a pastel icon
square, title and a live status line ("3 open today", "2 of 4 done", "5 day streak", month name). The notes list,
search and filter chips follow below; the whole header scrolls with the list so notes keep the screen.
A reviewer sees 4 organizer modules + notes + lock within seconds.

## Out of scope
Home-screen widget (needs more QA surface), cloud sync (would add data collection), exact alarms.

## Quality bar
Release (R8) hms build tested on emulator, rotation/back/empty states/notification denied, 2x monkey 3000 events,
unit tests for streaks, task grouping, mood stats, calendar parsing, backup JSON round trip.
