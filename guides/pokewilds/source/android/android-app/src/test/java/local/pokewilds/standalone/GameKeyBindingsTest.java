package local.pokewilds.standalone;

import android.view.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GameKeyBindingsTest {
    @Test public void mapsReleaseSettingNamesToAndroidKeys() {
        assertEquals(KeyEvent.KEYCODE_Z, GameKeyBindings.keyCode("Z", -1));
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, GameKeyBindings.keyCode("Left", -1));
        assertEquals(KeyEvent.KEYCODE_ENTER, GameKeyBindings.keyCode("Enter", -1));
    }

    @Test public void keepsFallbackForUnknownNames() {
        assertEquals(42, GameKeyBindings.keyCode("not-a-key", 42));
    }
}
