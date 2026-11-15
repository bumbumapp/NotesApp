package com.bumbumapps.mynotes.organizer;

/** The six habit colours: main colour and a pastel for backgrounds. */
public final class HabitColors {
    private HabitColors() {
    }

    public static final int[] MAIN = {
            0xFF2FBF71, 0xFF4C8DFF, 0xFF8B5CF6, 0xFFF5B83D, 0xFFFF6B6B, 0xFF14B8A6};
    public static final int[] SOFT = {
            0xFFDDF6E8, 0xFFE0ECFF, 0xFFECE4FD, 0xFFFDF0D2, 0xFFFFE1E1, 0xFFD7F5F1};

    public static int main(int i) {
        return MAIN[Math.floorMod(i, MAIN.length)];
    }

    public static int soft(int i) {
        return SOFT[Math.floorMod(i, SOFT.length)];
    }
}
