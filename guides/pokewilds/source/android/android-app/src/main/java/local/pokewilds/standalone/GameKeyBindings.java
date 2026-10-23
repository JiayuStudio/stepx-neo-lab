package local.pokewilds.standalone;

import android.content.Context;
import android.view.KeyEvent;

import java.io.File;
import java.util.Locale;

/** Resolves Android/X11 keys from the game's current keyboard-* settings. */
final class GameKeyBindings {
    final int a, b, left, right, up, down, start, shoulderLeft, shoulderRight;

    private GameKeyBindings(int a, int b, int left, int right, int up, int down,
                            int start, int shoulderLeft, int shoulderRight) {
        this.a = a; this.b = b; this.left = left; this.right = right;
        this.up = up; this.down = down; this.start = start;
        this.shoulderLeft = shoulderLeft; this.shoulderRight = shoulderRight;
    }

    static GameKeyBindings defaults() {
        return new GameKeyBindings(KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V);
    }

    static GameKeyBindings read(Context context) {
        GameKeyBindings result = defaults();
        File file = new File(new File(context.getFilesDir(), "game"), GameSettings.FILE_NAME);
        try {
            int a=result.a,b=result.b,left=result.left,right=result.right,up=result.up,down=result.down;
            int start=result.start,l=result.shoulderLeft,r=result.shoulderRight;
            for (GameSettings.Entry entry : GameSettings.read(file.toPath()).entries()) {
                String key = entry.key.toLowerCase(Locale.US);
                if (key.equals("keyboard-a")) a = keyCode(entry.value, a);
                else if (key.equals("keyboard-b")) b = keyCode(entry.value, b);
                else if (key.equals("keyboard-left")) left = keyCode(entry.value, left);
                else if (key.equals("keyboard-right")) right = keyCode(entry.value, right);
                else if (key.equals("keyboard-up")) up = keyCode(entry.value, up);
                else if (key.equals("keyboard-down")) down = keyCode(entry.value, down);
                else if (key.equals("keyboard-start")) start = keyCode(entry.value, start);
                else if (key.equals("keyboard-l")) l = keyCode(entry.value, l);
                else if (key.equals("keyboard-r")) r = keyCode(entry.value, r);
            }
            return new GameKeyBindings(a,b,left,right,up,down,start,l,r);
        } catch (Exception ignored) { return result; }
    }

    static int keyCode(String configured, int fallback) {
        if (configured == null) return fallback;
        String name = configured.trim().toUpperCase(Locale.US).replace(' ', '_');
        if (name.length() == 1 && name.charAt(0) >= 'A' && name.charAt(0) <= 'Z')
            return KeyEvent.KEYCODE_A + name.charAt(0) - 'A';
        if (name.length() == 1 && name.charAt(0) >= '0' && name.charAt(0) <= '9')
            return KeyEvent.KEYCODE_0 + name.charAt(0) - '0';
        switch (name) {
            case "LEFT": return KeyEvent.KEYCODE_DPAD_LEFT;
            case "RIGHT": return KeyEvent.KEYCODE_DPAD_RIGHT;
            case "UP": return KeyEvent.KEYCODE_DPAD_UP;
            case "DOWN": return KeyEvent.KEYCODE_DPAD_DOWN;
            case "ENTER": case "RETURN": return KeyEvent.KEYCODE_ENTER;
            case "SPACE": return KeyEvent.KEYCODE_SPACE;
            case "ESC": case "ESCAPE": return KeyEvent.KEYCODE_ESCAPE;
            case "TAB": return KeyEvent.KEYCODE_TAB;
            case "BACKSPACE": return KeyEvent.KEYCODE_DEL;
            case "SHIFT_LEFT": return KeyEvent.KEYCODE_SHIFT_LEFT;
            case "SHIFT_RIGHT": return KeyEvent.KEYCODE_SHIFT_RIGHT;
            case "CONTROL_LEFT": case "CTRL_LEFT": return KeyEvent.KEYCODE_CTRL_LEFT;
            case "CONTROL_RIGHT": case "CTRL_RIGHT": return KeyEvent.KEYCODE_CTRL_RIGHT;
            default: return fallback;
        }
    }
}
