package local.pokewilds.standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Known value types from PokeWilds 0.8.11's settings loader. */
final class GameSettingOptions {
    static final class Choice {
        final String label;
        final String value;

        Choice(String label, String value) {
            this.label = label;
            this.value = value;
        }

        @Override public String toString() { return label; }
    }

    static final class Definition {
        final String label;
        final String hint;
        final List<Choice> choices;
        final boolean decimal;

        Definition(String label, String hint, List<Choice> choices, boolean decimal) {
            this.label = label;
            this.hint = hint;
            this.choices = choices;
            this.decimal = decimal;
        }
    }

    private static final List<Choice> BOOLEAN = choices(
        new Choice("Off", "false"), new Choice("On", "true"));
    private static final Map<String, Definition> DEFINITIONS = definitions();

    private GameSettingOptions() {}

    static Definition forKey(String key) { return DEFINITIONS.get(key); }

    static String validatedValue(String key, String value) {
        String trimmed = value == null ? "" : value.trim();
        if ("gamepadDeadZone".equals(key)) {
            double parsed = number(key, trimmed);
            if (parsed < 0.0 || parsed > 1.0) {
                throw new IllegalArgumentException("Gamepad dead zone must be between 0 and 1.");
            }
        } else if ("zoom".equals(key)) {
            double parsed = number(key, trimmed);
            if (parsed <= 0.0) throw new IllegalArgumentException("Zoom must be greater than 0.");
        }
        return trimmed;
    }

    private static double number(String key, String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be a number.");
        }
    }

    private static Map<String, Definition> definitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        addBoolean(values, "muteMusic", "Mute music");
        addBoolean(values, "specPhysSplitEnabled", "Special/physical split");
        addBoolean(values, "photosensitiveMode", "Photosensitive mode");
        addBoolean(values, "battleAnims", "Battle animations");
        values.put("textSpeed", new Definition("Text speed", null, choices(
            new Choice("Slow", "slow"),
            new Choice("Medium", "mid"),
            new Choice("Fast", "fast"),
            new Choice("Instant", "inst")), false));
        values.put("gamepadDeadZone", new Definition("Gamepad dead zone",
            "Number from 0 to 1 (default 0.3)", Collections.emptyList(), true));
        values.put("zoom", new Definition("Zoom", "Positive number (default 1.0)",
            Collections.emptyList(), true));
        addBoolean(values, "drawReflections", "Draw reflections");
        addBoolean(values, "drawOverlays", "Draw overlays");
        addBoolean(values, "frameBuffersEnabled", "Frame buffers");
        return Collections.unmodifiableMap(values);
    }

    private static void addBoolean(Map<String, Definition> values, String key, String label) {
        values.put(key, new Definition(label, null, BOOLEAN, false));
    }

    private static List<Choice> choices(Choice... values) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values)));
    }
}
