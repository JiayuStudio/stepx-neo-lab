package local.pokewilds.standalone;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class GameSettingsTest {
    @Test public void changesValuesWithoutLosingCommentsOrUnknownSettings() {
        GameSettings input = GameSettings.parse("# PokeWilds\nkeyboard-a=Z\nfuture-setting=untouched\n\n");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("keyboard-a", "X");
        assertEquals("# PokeWilds\nkeyboard-a=X\nfuture-setting=untouched\n\n", input.withValues(values).serialize());
    }

    @Test public void preservesWindowsNewlines() {
        GameSettings input = GameSettings.parse("muteMusic=false\r\nspecPhysSplitEnabled=true\r\n");
        Map<String, String> values = new LinkedHashMap<>(); values.put("muteMusic", "true");
        assertEquals("muteMusic=true\r\nspecPhysSplitEnabled=true\r\n", input.withValues(values).serialize());
    }

    @Test public void atomicWriteKeepsBackup() throws Exception {
        Path directory = Files.createTempDirectory("game-settings");
        Path file = directory.resolve("settings.txt");
        Files.write(file, "muteMusic=false\n".getBytes(StandardCharsets.UTF_8));
        GameSettings.parse("muteMusic=true\n").writeAtomically(file);
        assertEquals("muteMusic=true\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertEquals("muteMusic=false\n", new String(Files.readAllBytes(directory.resolve("settings.txt.backup")), StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesMalformedAdvancedLines() {
        GameSettings.parse("muteMusic true\n").validateForSave();
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesSpacesAroundSeparator() {
        GameSettings.parse("muteMusic = true\n").validateForSave();
    }

    @Test public void identifiesBindingsWithoutHidingOtherInputSettings() {
        assertTrue(GameSettings.isKeyBinding("keyboard-A"));
        assertTrue(GameSettings.isKeyBinding("gamepad-B"));
        assertFalse(GameSettings.isKeyBinding("gamepadDeadZone"));
        assertFalse(GameSettings.isKeyBinding("muteMusic"));
    }
}
