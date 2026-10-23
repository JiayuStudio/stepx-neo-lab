package local.pokewilds.standalone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Tracks fingers captured by a touch button and the key held by each finger. */
final class TouchInputState {
    interface KeySink { void send(int key, boolean down); }

    static final int NONE = -1;
    private final Map<Integer, Integer> activePointers = new HashMap<>();
    private final Set<Integer> capturedPointers = new HashSet<>();
    private final KeySink sink;

    TouchInputState(KeySink sink) { this.sink = sink; }

    static boolean isControllerSource(int sources, int gamepadSource, int joystickSource) {
        return (sources & gamepadSource) == gamepadSource
            || (sources & joystickSource) == joystickSource;
    }

    boolean down(int pointer, int key) {
        if (key == NONE) return false;
        capturedPointers.add(pointer);
        changePointerKey(pointer, key);
        return true;
    }

    void move(int pointer, int key) {
        if (!capturedPointers.contains(pointer)) return;
        changePointerKey(pointer, key);
    }

    void up(int pointer) {
        capturedPointers.remove(pointer);
        changePointerKey(pointer, NONE);
    }

    boolean isHeld(int key){return activePointers.containsValue(key);}

    boolean hasCapturedPointers() { return !capturedPointers.isEmpty(); }

    void releaseAll() {
        Set<Integer> released = new HashSet<>();
        for (int key : activePointers.values()) if (released.add(key)) sink.send(key, false);
        activePointers.clear();
        capturedPointers.clear();
    }

    private void changePointerKey(int pointer, int nextKey) {
        int oldKey = activePointers.getOrDefault(pointer, NONE);
        if (oldKey == nextKey) return;
        if (oldKey != NONE) {
            activePointers.remove(pointer);
            if (!activePointers.containsValue(oldKey)) sink.send(oldKey, false);
        }
        if (nextKey != NONE) {
            boolean alreadyHeld = activePointers.containsValue(nextKey);
            activePointers.put(pointer, nextKey);
            if (!alreadyHeld) sink.send(nextKey, true);
        }
    }
}
