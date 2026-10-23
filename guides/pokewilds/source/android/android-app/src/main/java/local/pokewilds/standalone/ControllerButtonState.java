package local.pokewilds.standalone;

import java.util.HashMap;
import java.util.Map;

/** Keeps each press on its original route until release, including across context changes. */
final class ControllerButtonState {
    interface Release { void run(boolean canceled); }
    interface Press { Release run(); }
    interface KeySink { void send(int key, boolean down); }

    private final Map<String, Release> presses = new HashMap<>();
    private final java.util.Set<String> handledButtons = new java.util.HashSet<>();
    private final Map<Integer, Integer> heldKeys = new HashMap<>();
    private final KeySink sink;

    ControllerButtonState(KeySink sink) { this.sink = sink; }

    boolean route(String owner, int key, boolean down, int repeat, boolean canceled, Press press) {
        String id = owner + ":" + key;
        if (!down) {
            Release release = presses.remove(id);
            if (release == null) return handledButtons.contains(id);
            release.run(canceled);
            return true;
        }
        if (presses.containsKey(id)) return true;
        if (repeat != 0) return handledButtons.contains(id);
        Release release = press.run();
        if (release == null) { handledButtons.remove(id); return false; }
        handledButtons.add(id);
        presses.put(id, release);
        return true;
    }

    Release holdKey(int key) {
        int count = heldKeys.getOrDefault(key, 0);
        heldKeys.put(key, count + 1);
        if (count == 0) sink.send(key, true);
        return canceled -> {
            int remaining = heldKeys.get(key) - 1;
            if (remaining == 0) { heldKeys.remove(key); sink.send(key, false); }
            else heldKeys.put(key, remaining);
        };
    }

    void cancelOwner(String owner) {
        for (String id : new java.util.ArrayList<>(presses.keySet())) {
            if (id.startsWith(owner + ":")) presses.remove(id).run(true);
        }
    }

    void cancelAll() {
        Map<String, Release> active = new HashMap<>(presses);
        presses.clear();
        for (Release release : active.values()) release.run(true);
    }
}
