package local.pokewilds.standalone;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ControllerButtonStateTest {
    @Test public void releaseUsesTheRouteChosenAtPressEvenAfterContextChanges() {
        List<String> events = new ArrayList<>();
        ControllerButtonState state = new ControllerButtonState((key, down) -> events.add(key + ":" + down));
        assertTrue(state.route("touch", 1, true, 0, false, () -> state.holdKey(31)));
        assertTrue(state.route("touch", 1, false, 0, false, () -> { fail("Release must not choose a new action"); return null; }));
        assertEquals(Arrays.asList("31:true", "31:false"), events);
    }

    @Test public void touchAndPhysicalKeysDoNotReleaseEachOther() {
        List<String> events = new ArrayList<>();
        ControllerButtonState state = new ControllerButtonState((key, down) -> events.add(key + ":" + down));
        state.route("touch", 1, true, 0, false, () -> state.holdKey(31));
        state.route("device-8", 1, true, 0, false, () -> state.holdKey(31));
        state.cancelOwner("touch");
        assertEquals(Arrays.asList("31:true"), events);
        state.route("device-8", 1, false, 0, false, () -> null);
        assertEquals(Arrays.asList("31:true", "31:false"), events);
    }

    @Test public void duplicateDispatchAndRepeatsDoNotRepeatAnAction() {
        List<String> events = new ArrayList<>();
        ControllerButtonState state = new ControllerButtonState((key, down) -> { });
        ControllerButtonState.Press press = () -> { events.add("zoom"); return canceled -> events.add("release"); };
        state.route("touch", 1, true, 0, false, press);
        state.route("touch", 1, true, 0, false, press);
        state.route("touch", 1, true, 1, false, press);
        state.route("touch", 1, false, 0, false, press);
        assertEquals(Arrays.asList("zoom", "release"), events);
    }

    @Test public void cancellationDoesNotCommitAnActionOnRelease() {
        List<Boolean> canceled = new ArrayList<>();
        ControllerButtonState state = new ControllerButtonState((key, down) -> { });
        state.route("touch", 1, true, 0, false, () -> canceled::add);
        state.cancelAll();
        assertTrue(state.route("touch", 1, false, 0, false, () -> null));
        assertEquals(Arrays.asList(true), canceled);
    }

    @Test public void separateDevicesEachReceiveTheirOwnAction() {
        List<String> events = new ArrayList<>();
        ControllerButtonState state = new ControllerButtonState((key, down) -> { });
        for (String owner : Arrays.asList("touch", "device-1", "device-2"))
            state.route(owner, 1, true, 0, false, () -> { events.add(owner); return canceled -> { }; });
        assertEquals(Arrays.asList("touch", "device-1", "device-2"), events);
    }
    @Test public void duplicateReleaseIsConsumedWhileAnotherOwnerStillHoldsKey() {
        List<String> events = new ArrayList<>();
        ControllerButtonState state = new ControllerButtonState((key, down) -> events.add(key + ":" + down));
        state.route("device-1", 1, true, 0, false, () -> state.holdKey(31));
        state.route("touch", 1, true, 0, false, () -> state.holdKey(31));
        state.cancelOwner("device-1");
        assertTrue(state.route("device-1", 1, false, 0, false, () -> null));
        assertTrue(state.route("device-1", 1, false, 0, false, () -> null));
        assertEquals(Arrays.asList("31:true"), events);
        assertFalse(state.route("unknown", 1, false, 0, false, () -> null));
        assertFalse(state.route("device-1", 1, true, 0, false, () -> null));
        assertFalse(state.route("device-1", 1, false, 0, false, () -> null));
        state.cancelOwner("touch");
        assertEquals(Arrays.asList("31:true", "31:false"), events);
    }
}
