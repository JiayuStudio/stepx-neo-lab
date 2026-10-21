package android.content;
import java.util.HashMap;
import java.util.Map;
public class Context {
    public static final int MODE_PRIVATE = 0;
    private final Map<String, String> values = new HashMap<>();
    private final SharedPreferences prefs = new SharedPreferences() {
        public String getString(String key, String fallback) { return values.containsKey(key) ? values.get(key) : fallback; }
        public Editor edit() {
            return new Editor() {
                private final Map<String, String> changes = new HashMap<>();
                public Editor putString(String key, String value) { changes.put(key, value); return this; }
                public Editor remove(String key) { changes.put(key, null); return this; }
                public boolean commit() {
                    for (Map.Entry<String, String> change : changes.entrySet()) {
                        if (change.getValue() == null) values.remove(change.getKey());
                        else values.put(change.getKey(), change.getValue());
                    }
                    return true;
                }
            };
        }
    };
    public Context getApplicationContext() { return this; }
    public SharedPreferences getSharedPreferences(String name, int mode) { return prefs; }
}
