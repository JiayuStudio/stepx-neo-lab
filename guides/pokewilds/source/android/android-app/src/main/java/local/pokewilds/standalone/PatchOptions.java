package local.pokewilds.standalone;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** User-selected built-in game fixes, passed to the guest JVM at startup. */
final class PatchOptions {
    static final String SPRITES = "sprites";
    static final String HOOH = "hooh";
    static final String FLOORS = "floors";
    static final String EGGS = "eggs";
    static final String RADIAL = "radial";
    static final String ZOOM = "zoom";
    static final String MAP = "map";
    static final String PROMPTS = "prompts";

    private static final String PREFERENCES = "patch-options";
    private static final String KEY_PREFIX = "bugfix.";

    final boolean sprites;
    final boolean hooh;
    final boolean floors;
    final boolean eggs;
    final boolean radial;
    final boolean zoom;
    final boolean map;
    final boolean prompts;

    PatchOptions(boolean sprites, boolean hooh, boolean floors, boolean eggs) {
        this(sprites, hooh, floors, eggs, false, false);
    }

    PatchOptions(boolean sprites, boolean hooh, boolean floors, boolean eggs, boolean radial, boolean zoom) {
        this(sprites, hooh, floors, eggs, radial, zoom, false, false);
    }

    PatchOptions(boolean sprites, boolean hooh, boolean floors, boolean eggs, boolean radial, boolean zoom, boolean map, boolean prompts) {
        this.map = map;
        this.prompts = prompts;
        this.sprites = sprites;
        this.hooh = hooh;
        this.floors = floors;
        this.eggs = eggs;
        this.radial = radial;
        this.zoom = zoom;
    }

    static PatchOptions read(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return new PatchOptions(
            preferences.getBoolean(KEY_PREFIX + SPRITES, true),
            preferences.getBoolean(KEY_PREFIX + HOOH, true),
            preferences.getBoolean(KEY_PREFIX + FLOORS, true),
            preferences.getBoolean(KEY_PREFIX + EGGS, true),
            preferences.getBoolean(KEY_PREFIX + RADIAL, true),
            preferences.getBoolean(KEY_PREFIX + ZOOM, true),
            preferences.getBoolean(KEY_PREFIX + MAP, true),
            preferences.getBoolean(KEY_PREFIX + PROMPTS, false));
    }

    static void save(Context context, String patch, boolean enabled) {
        if (!SPRITES.equals(patch) && !HOOH.equals(patch) && !FLOORS.equals(patch) && !EGGS.equals(patch)
            && !RADIAL.equals(patch) && !ZOOM.equals(patch) && !MAP.equals(patch) && !PROMPTS.equals(patch)) {
            throw new IllegalArgumentException("Unknown patch: " + patch);
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PREFIX + patch, enabled).apply();
    }

    boolean isEnabled(String patch) {
        if (SPRITES.equals(patch)) return sprites;
        if (HOOH.equals(patch)) return hooh;
        if (FLOORS.equals(patch)) return floors;
        if (EGGS.equals(patch)) return eggs;
        if (RADIAL.equals(patch)) return radial;
        if (ZOOM.equals(patch)) return zoom;
        if (MAP.equals(patch)) return map;
        if (PROMPTS.equals(patch)) return prompts;
        throw new IllegalArgumentException("Unknown patch: " + patch);
    }

    boolean anyEnabled() { return sprites || hooh || floors || eggs || prompts; }
    boolean controlsEnabled() { return radial || zoom || map || prompts; }

    List<String> jvmArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add("-Dbugfix.sprites=" + sprites);
        arguments.add("-Dbugfix.hooh=" + hooh);
        arguments.add("-Dbugfix.floors=" + floors);
        arguments.add("-Dbugfix.eggs=" + eggs);
        arguments.add("-Dbugfix.prompts=" + prompts);
        return Collections.unmodifiableList(arguments);
    }

    List<String> enabledNames() {
        List<String> names = new ArrayList<>();
        if (sprites) names.add("Directional sprites");
        if (hooh) names.add("Ho-Oh");
        if (floors) names.add("Separate floor occupancy");
        if (eggs) names.add("Egg floor saving");
        if (radial) names.add("Field move wheel");
        if (zoom) names.add("Shoulder zoom");
        if (map) names.add("Select map shortcut");
        if (prompts) names.add("Controller button prompts");
        return Collections.unmodifiableList(names);
    }
}
