// SPDX-License-Identifier: MIT
package local.pokewilds.bugfix;

/** One-shot controller/touch confirmation for a specific in-game text editor. */
public final class ControllerConfirm {
    private static final long LIFETIME_NANOS = 1_000_000_000L;
    private static Object requestedEditor;
    private static long expiresAt;

    private ControllerConfirm() { }

    /** Request a controller confirmation for the currently active editor. */
    public static synchronized void request(Object editor) {
        if (editor == null) return;
        requestedEditor = editor;
        expiresAt = System.nanoTime() + LIFETIME_NANOS;
    }

    /** Accept stock Enter input or consume a recent controller request for this editor only. */
    public static synchronized boolean consume(boolean enterPressed, Object editor) {
        if (enterPressed) {
            if (requestedEditor == editor) clear();
            return true;
        }
        if (requestedEditor == null) return false;
        if (System.nanoTime() - expiresAt >= 0) {
            clear();
            return false;
        }
        if (requestedEditor != editor) return false;
        clear();
        return true;
    }

    private static void clear() {
        requestedEditor = null;
        expiresAt = 0L;
    }
}
