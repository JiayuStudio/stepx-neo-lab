package local.pokewilds.standalone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Line-preserving access to the unchanged game's settings.txt file. */
final class GameSettings {
    static final String FILE_NAME = "settings.txt";
    private static final int MAX_BYTES = 1024 * 1024;

    static final class Entry {
        final String key;
        final String value;

        Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private final List<String> lines;
    private final String newline;
    private final boolean trailingNewline;

    private GameSettings(List<String> lines, String newline, boolean trailingNewline) {
        this.lines = lines;
        this.newline = newline;
        this.trailingNewline = trailingNewline;
    }

    static GameSettings read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("settings.txt has not been installed yet.");
        long size = Files.size(path);
        if (size > MAX_BYTES) throw new IOException("settings.txt is unexpectedly large.");
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    static GameSettings parse(String text) {
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        boolean trailing = text.endsWith("\n");
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] split = normalized.split("\n", -1);
        int count = trailing ? split.length - 1 : split.length;
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) lines.add(split[i]);
        return new GameSettings(lines, newline, trailing);
    }

    List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (String line : lines) {
            int separator = separator(line);
            if (separator < 1) continue;
            entries.add(new Entry(line.substring(0, separator), line.substring(separator + 1)));
        }
        return Collections.unmodifiableList(entries);
    }

    static boolean isKeyBinding(String key) {
        if (key == null) return false;
        String normalized = key.toLowerCase(java.util.Locale.US);
        return normalized.startsWith("keyboard-") || normalized.startsWith("gamepad-");
    }

    GameSettings withValues(Map<String, String> values) {
        Map<String, String> remaining = new LinkedHashMap<>(values);
        List<String> changed = new ArrayList<>(lines.size() + remaining.size());
        for (String line : lines) {
            int separator = separator(line);
            if (separator < 1) {
                changed.add(line);
                continue;
            }
            String key = line.substring(0, separator);
            String value = remaining.remove(key);
            changed.add(value == null ? line : key + "=" + validateValue(key, value));
        }
        for (Map.Entry<String, String> entry : remaining.entrySet()) {
            String key = validateKey(entry.getKey());
            changed.add(key + "=" + validateValue(key, entry.getValue()));
        }
        return new GameSettings(changed, newline, trailingNewline || !changed.isEmpty());
    }

    String serialize() {
        String text = String.join(newline, lines);
        return trailingNewline && !lines.isEmpty() ? text + newline : text;
    }

    void validateForSave() {
        Map<String, Boolean> keys = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) continue;
            int split = line.indexOf('=');
            if (split < 1) throw new IllegalArgumentException("Line " + (i + 1) + " must use key=value.");
            String key = validateKey(line.substring(0, split));
            validateValue(key, line.substring(split + 1));
            if (keys.put(key, true) != null) throw new IllegalArgumentException("Duplicate setting: " + key);
        }
    }

    void writeAtomically(Path path) throws IOException {
        validateForSave();
        Files.createDirectories(path.getParent());
        Path staged = path.resolveSibling(FILE_NAME + ".preparing");
        Path backup = path.resolveSibling(FILE_NAME + ".backup");
        byte[] contents = serialize().getBytes(StandardCharsets.UTF_8);
        Files.write(staged, contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
        if (Files.isRegularFile(path)) Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(staged, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(staged, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int separator(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) return -1;
        return line.indexOf('=');
    }

    private static String validateKey(String key) {
        if (key == null || key.isEmpty() || !key.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid setting name: " + key);
        }
        return key;
    }

    private static String validateValue(String key, String value) {
        validateKey(key);
        if (value == null) return "";
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Setting values must stay on one line.");
        }
        return value.trim();
    }
}
