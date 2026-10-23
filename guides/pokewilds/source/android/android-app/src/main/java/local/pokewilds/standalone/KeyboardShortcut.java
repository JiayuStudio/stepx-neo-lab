package local.pokewilds.standalone;

import android.content.Context;
import android.view.KeyEvent;

/** Physical gamepad button used to show or hide the Android keyboard. */
final class KeyboardShortcut {
    static final int RIGHT_TRIGGER = 0;
    static final int RIGHT_STICK = 6;
    static final int OFF = 7;
    // Persisted IDs are independent of the order of the choices in the UI.
    static final int[] CHOICES = {RIGHT_TRIGGER, 2, 3, 5, RIGHT_STICK, OFF};
    static final String[] LABELS = {"Right trigger (R2)", "X", "Y", "Left stick click", "Right stick click", "Off"};
    private static final String PREFS = "keyboard_shortcut";
    private static final String KEY = "button";

    static int read(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int choice = preferences.getInt(KEY, RIGHT_STICK);
        int normalized = normalize(choice);
        if (choice != normalized) preferences.edit().putInt(KEY, normalized).apply();
        return normalized;
    }

    static void save(Context context, int choice) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY, normalize(choice)).apply();
    }

    static boolean matchesKey(int choice, int keyCode) {
        switch (choice) {
            case RIGHT_TRIGGER: return keyCode == KeyEvent.KEYCODE_BUTTON_R2;
            case 2: return keyCode == KeyEvent.KEYCODE_BUTTON_X;
            case 3: return keyCode == KeyEvent.KEYCODE_BUTTON_Y;
            case 5: return keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL;
            case RIGHT_STICK: return keyCode == KeyEvent.KEYCODE_BUTTON_THUMBR;
            default: return false;
        }
    }

    static int normalize(int choice) {
        for (int allowed : CHOICES) if (choice == allowed) return choice;
        return RIGHT_STICK;
    }

    static int indexOf(int choice) {
        int normalized = normalize(choice);
        for (int i = 0; i < CHOICES.length; i++) if (CHOICES[i] == normalized) return i;
        throw new IllegalStateException("Missing default keyboard shortcut");
    }

    static int touchChoice(int choice) { return normalize(choice) == OFF ? OFF : RIGHT_TRIGGER; }
}
