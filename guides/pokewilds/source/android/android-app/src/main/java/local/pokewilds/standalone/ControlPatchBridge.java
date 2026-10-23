package local.pokewilds.standalone;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Optional control commands shared with the game agent; never replaces the existing touch controls. */
final class ControlPatchBridge {
    static final String[] MOVES = {"BUILD", "CUT", "FLY", "SURF", "DIG", "RIDE", "SMASH", "TELEPORT",
        "FLASH", "CHARM", "POWER", "REPEL", "ATTACK", "HEADBUTT", "PAINT"};
    private final boolean radial, zoom, map, prompts;
    private final File directory;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Properties state = new Properties();
    private final Set<String> wheelOwners = new HashSet<>();
    private final Set<String> consumedShoulders = new HashSet<>();
    private final Runnable changed;
    private long lastState, stateModified, lastReadAt, wheelId, lastBeat;
    private boolean stateFresh;
    private int selected = -1;
    private boolean active, axisTrigger;
    private float stickX, stickY, hatX, hatY;

    ControlPatchBridge(Context context, boolean radial, boolean zoom, boolean map, boolean prompts, Runnable changed) {
        this.radial = radial;
        this.zoom = zoom;
        this.map = map;
        this.prompts = prompts;
        this.changed = changed;
        directory = new File(context.getFilesDir(), "shared-tmp/odin-controls");
    }

    void resume() { active = true; handler.removeCallbacks(poll); handler.post(poll); }
    void suspend() {
        active = false;
        handler.removeCallbacks(poll);
        cancelWheel();
        consumedShoulders.clear();
        axisTrigger = false;
    }

    boolean radialEnabled() { return radial; }
    boolean zoomEnabled() { return zoom; }
    boolean wheelShown() { return radial && !wheelOwners.isEmpty(); }
    int selected() { return selected; }
    boolean available(int index) { return Boolean.parseBoolean(state.getProperty("available." + index, "false")); }

    private boolean fresh() { return System.currentTimeMillis() - lastState < 1200; }
    private boolean canOpen() { return fresh() && Boolean.parseBoolean(state.getProperty("canOpen", "false")); }
    private boolean canZoom() { return fresh() && "zoom".equals(state.getProperty("shoulder")); }

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!active) return;
            boolean redraw = false;
            File stateFile = new File(directory, "state");
            long modified = stateFile.lastModified();
            long now = System.currentTimeMillis();
            if (modified != 0 && (modified != stateModified || now - lastReadAt >= 500)) {
                lastReadAt = now;
                try (FileInputStream in = new FileInputStream(stateFile)) {
                    Properties next = new Properties();
                    next.load(in);
                    long stamp = Long.parseLong(next.getProperty("time", "0"));
                    if (stamp >= lastState) {
                        next.remove("time");
                        if (!next.equals(state)) { state.clear(); state.putAll(next); redraw = true; }
                        lastState = stamp;
                        stateModified = modified;
                    }
                } catch (IOException | NumberFormatException ignored) { }
            }
            boolean freshNow = fresh();
            if (freshNow != stateFresh) { stateFresh = freshNow; redraw = true; }
            if (wheelShown()) {
                if (System.currentTimeMillis() - lastBeat > 350) {
                    send("BEAT", wheelId, null);
                    lastBeat = System.currentTimeMillis();
                }
                if (!freshNow) { cancelWheel(); redraw = false; }
            }
            if (redraw) changed.run();
            handler.postDelayed(this, 60);
        }
    };

    private void send(String command, long id, String argument) {
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create control directory");
            try (FileOutputStream out = new FileOutputStream(new File(directory, "commands"), true)) {
                String line = command + " " + id + (argument == null ? "" : " " + argument) + ";\n";
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) { android.util.Log.e("PokeWildsControls", "Control command failed", e); }
    }

    void wheel(String owner, boolean down) {
        if (!radial) return;
        boolean wasOpen = !wheelOwners.isEmpty();
        if (down) {
            if (!wasOpen && !canOpen()) return;
            wheelOwners.add(owner);
        } else wheelOwners.remove(owner);
        boolean isOpen = !wheelOwners.isEmpty();
        if (!wasOpen && isOpen) {
            selected = -1;
            wheelId = System.currentTimeMillis();
            lastBeat = wheelId;
            send("OPEN", wheelId, null);
        } else if (wasOpen && !isOpen) {
            send("CLOSE", wheelId, Integer.toString(selected));
            selected = -1;
        }
        changed.run();
    }

    private void cancelWheel() {
        if (wheelShown()) send("CANCEL", wheelId, null);
        wheelOwners.clear();
        selected = -1;
        changed.run();
    }

    private void cancelWheelOwner(String owner) {
        if (wheelOwners.remove(owner) && wheelOwners.isEmpty()) {
            send("CANCEL", wheelId, null);
            selected = -1;
        }
        changed.run();
    }

    void chooseFromMenu(int index){if(wheelShown()&&index>=0&&index<MOVES.length)selected=index;}

    void select(float x, float y) {
        if (!wheelShown() || x * x + y * y < .35f * .35f) return;
        double angle = Math.atan2(y, x) + Math.PI / 2;
        selected = (int) Math.floor(angle / (2 * Math.PI / MOVES.length) + .5);
        selected = (selected % MOVES.length + MOVES.length) % MOVES.length;
        changed.run();
    }

    void selectTouch(float x, float y, float width, float height) {
        float radius = Math.min(width, height) * .37f;
        float dx = (x - width / 2) / radius, dy = (y - height / 2) / radius;
        if (dx * dx + dy * dy <= 1.12f * 1.12f) select(dx, dy);
    }

    boolean routeKey(KeyEvent event) {
        if (!isController(event.getSource())) return false;
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        String owner = "key:" + event.getDeviceId();
        if (prompts && code == KeyEvent.KEYCODE_BUTTON_START && !RuntimeService.saveDialogVisible) {
            if (down && event.getRepeatCount() == 0 && !event.isCanceled())
                send("START", System.currentTimeMillis(), null);
            return true;
        }
        if (map && code == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (down && event.getRepeatCount() == 0 && !event.isCanceled() && !wheelShown() && fresh())
                send("MAP", System.currentTimeMillis(), null);
            return true;
        }
        if (radial && code == KeyEvent.KEYCODE_BUTTON_L2) {
            if (event.isCanceled()) cancelWheelOwner(owner);
            else if (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP)
                wheel(owner, down);
            return true;
        }
        if (wheelShown() && code >= KeyEvent.KEYCODE_DPAD_UP && code <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (down) {
                if (code == KeyEvent.KEYCODE_DPAD_UP) select(0, -1);
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) select(0, 1);
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) select(-1, 0);
                if (code == KeyEvent.KEYCODE_DPAD_RIGHT) select(1, 0);
            }
            return true;
        }
        if (zoom && (code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1)) {
            String shoulder = owner + ":" + code;
            if (event.getAction() == KeyEvent.ACTION_UP && consumedShoulders.remove(shoulder)) return true;
            if (down && canZoom()) {
                if (consumedShoulders.add(shoulder)) send("ZOOM", code == KeyEvent.KEYCODE_BUTTON_R1 ? 1 : -1, null);
                return true;
            }
        }
        return false;
    }

    boolean routeMotion(MotionEvent event) {
        if (!radial || (event.getSource() & InputDevice.SOURCE_JOYSTICK) == 0
            || event.getAction() != MotionEvent.ACTION_MOVE) return false;
        boolean trigger = Math.max(event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_BRAKE)) > .2f;
        if (trigger != axisTrigger) {
            axisTrigger = trigger;
            wheel("axis:" + event.getDeviceId(), trigger);
        }
        if (!wheelShown()) return false;
        stickX = event.getAxisValue(MotionEvent.AXIS_X);
        stickY = event.getAxisValue(MotionEvent.AXIS_Y);
        hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if (Math.abs(hatX) + Math.abs(hatY) > .3f) select(hatX, hatY);
        else select(stickX, stickY);
        return true;
    }

    private static boolean isController(int source) {
        return (source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            || (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }
}
