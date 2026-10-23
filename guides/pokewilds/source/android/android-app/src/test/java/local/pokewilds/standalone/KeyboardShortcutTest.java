package local.pokewilds.standalone;

import android.view.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyboardShortcutTest {
    @Test public void removedChoicesMigrateToDefaultWithoutReindexingSavedChoices() {
        assertEquals(KeyboardShortcut.RIGHT_STICK, KeyboardShortcut.normalize(1));
        assertEquals(KeyboardShortcut.RIGHT_STICK, KeyboardShortcut.normalize(4));
        for (int choice : new int[]{0, 2, 3, 5, 6, 7}) {
            assertEquals(choice, KeyboardShortcut.normalize(choice));
            assertEquals(choice, KeyboardShortcut.CHOICES[KeyboardShortcut.indexOf(choice)]);
        }
    }

    @Test public void absentTouchButtonsFallBackToR2ButOffStaysOff() {
        for (int choice : new int[]{0, 1, 2, 3, 4, 5, 6})
            assertEquals(KeyboardShortcut.RIGHT_TRIGGER, KeyboardShortcut.touchChoice(choice));
        assertEquals(KeyboardShortcut.OFF, KeyboardShortcut.touchChoice(KeyboardShortcut.OFF));
    }

    @Test public void selectAndL2NeverToggleKeyboard() {
        for (int choice : KeyboardShortcut.CHOICES) {
            assertFalse(KeyboardShortcut.matchesKey(choice, KeyEvent.KEYCODE_BUTTON_SELECT));
            assertFalse(KeyboardShortcut.matchesKey(choice, KeyEvent.KEYCODE_BUTTON_L2));
        }
        assertTrue(KeyboardShortcut.matchesKey(KeyboardShortcut.RIGHT_TRIGGER, KeyEvent.KEYCODE_BUTTON_R2));
        assertTrue(KeyboardShortcut.matchesKey(KeyboardShortcut.RIGHT_STICK, KeyEvent.KEYCODE_BUTTON_THUMBR));
    }
}
