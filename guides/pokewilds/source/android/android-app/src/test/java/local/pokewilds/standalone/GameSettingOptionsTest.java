package local.pokewilds.standalone;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GameSettingOptionsTest {
    @Test public void exposesEveryRecognizedTextSpeed() {
        GameSettingOptions.Definition definition = GameSettingOptions.forKey("textSpeed");
        List<String> values = new ArrayList<>();
        for (GameSettingOptions.Choice choice : definition.choices) values.add(choice.value);
        assertArrayEquals(new String[] {"slow", "mid", "fast", "inst"}, values.toArray(new String[0]));
    }

    @Test public void exposesCanonicalBooleanValues() {
        GameSettingOptions.Definition definition = GameSettingOptions.forKey("battleAnims");
        assertEquals("false", definition.choices.get(0).value);
        assertEquals("true", definition.choices.get(1).value);
    }

    @Test public void acceptsUsefulNumericRanges() {
        assertEquals("0.3", GameSettingOptions.validatedValue("gamepadDeadZone", " 0.3 "));
        assertEquals("1.25", GameSettingOptions.validatedValue("zoom", "1.25"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDeadZoneOutsideControllerAxisRange() {
        GameSettingOptions.validatedValue("gamepadDeadZone", "1.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZoomThatTheGameIgnores() {
        GameSettingOptions.validatedValue("zoom", "0");
    }
}
