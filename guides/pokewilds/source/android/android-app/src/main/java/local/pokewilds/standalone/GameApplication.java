package local.pokewilds.standalone;

import android.preference.PreferenceManager;
import com.termux.x11.LorieApp;

public final class GameApplication extends LorieApp {
    @Override public void onCreate() {
        // Initialize before LorieApp attaches its preference readers.
        RuntimeOptions options = RuntimeOptions.read(this);
        RuntimeOptions.applyDisplaySize(this, options.width, options.height);
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean("showAdditionalKbd", false)
            .putBoolean("fullscreen", true).apply();
        super.onCreate();
        NeoRearDisplay.recover(this);
    }
}
