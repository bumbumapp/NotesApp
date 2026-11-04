package com.bumbumapps.mynotes.logic;

import java.util.ArrayList;
import java.util.List;

/** Checklist items stored one per line in the note text: "[ ] milk" / "[x] eggs". Pure Java, unit tested. */
public final class Checklist {

    public static final class Item {
        public String text;
        public boolean done;

        public Item(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }

    private Checklist() {
    }

    public static List<Item> parse(String stored) {
        List<Item> items = new ArrayList<>();
        if (stored == null) {
            return items;
        }
        for (String line : stored.split("\n", -1)) {
            if (line.startsWith("[x] ") || line.startsWith("[X] ")) {
                items.add(new Item(line.substring(4), true));
            } else if (line.startsWith("[ ] ")) {
                items.add(new Item(line.substring(4), false));
            } else if (!line.trim().isEmpty()) {
                items.add(new Item(line.trim(), false));
            }
        }
        return items;
    }

    /** Blank items are dropped; line breaks inside an item become spaces. */
    public static String serialize(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item i : items) {
            String t = i.text == null ? "" : i.text.replace('\n', ' ').trim();
            if (t.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(i.done ? "[x] " : "[ ] ").append(t);
        }
        return sb.toString();
    }

    public static int doneCount(List<Item> items) {
        int n = 0;
        for (Item i : items) {
            if (i.done && i.text != null && !i.text.trim().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public static int count(List<Item> items) {
        int n = 0;
        for (Item i : items) {
            if (i.text != null && !i.text.trim().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Moves finished items to the end, keeping the relative order inside both groups. */
    public static void doneToBottom(List<Item> items) {
        List<Item> open = new ArrayList<>();
        List<Item> done = new ArrayList<>();
        for (Item i : items) {
            (i.done ? done : open).add(i);
        }
        items.clear();
        items.addAll(open);
        items.addAll(done);
    }

    public static void removeDone(List<Item> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).done) {
                items.remove(i);
            }
        }
    }

    /** Card preview: the first {@code max} items as "☐ milk" / "☑ eggs" lines. */
    public static String preview(List<Item> items, int max) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Item i : items) {
            if (i.text == null || i.text.trim().isEmpty()) {
                continue;
            }
            if (shown == max) {
                sb.append("\n…");
                break;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(i.done ? "☑ " : "☐ ").append(i.text.trim());
            shown++;
        }
        return sb.toString();
    }
}
