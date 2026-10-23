package local.pokewilds.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TouchInputStateTest {
    @Test public void fingerCanLeaveDpadAndReturnWithoutLifting() {
        List<String> events = new ArrayList<>();
        TouchInputState state = new TouchInputState((key, down) ->
            events.add(key + (down ? " down" : " up")));

        assertTrue(state.down(7, 19));
        state.move(7, TouchInputState.NONE);
        assertTrue(state.hasCapturedPointers());
        state.move(7, 19);
        state.up(7);

        assertEquals(Arrays.asList("19 down", "19 up", "19 down", "19 up"), events);
        assertFalse(state.hasCapturedPointers());
    }

    @Test public void unrelatedInputSourcesAreNotControllers() {
        int gamepad = 0x00000401;
        int joystick = 0x01000010;
        int keyboard = 0x00000101;
        int touchscreen = 0x00001002;

        assertFalse(TouchInputState.isControllerSource(keyboard, gamepad, joystick));
        assertFalse(TouchInputState.isControllerSource(touchscreen, gamepad, joystick));
        assertTrue(TouchInputState.isControllerSource(gamepad, gamepad, joystick));
        assertTrue(TouchInputState.isControllerSource(joystick, gamepad, joystick));
    }

    @Test public void returningFingerDoesNotReleaseAnotherFingersButton() {
        List<String> events = new ArrayList<>();
        TouchInputState state = new TouchInputState((key, down) ->
            events.add(key + (down ? " down" : " up")));

        state.down(1, 19);
        state.down(2, 20);
        state.move(1, TouchInputState.NONE);
        state.move(1, 19);
        state.up(1);
        state.up(2);

        assertEquals(Arrays.asList("19 down", "20 down", "19 up", "19 down", "19 up", "20 up"), events);
    }
}
