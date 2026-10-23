package local.pokewilds.standalone;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/** Persisted runtime choices used by the display and guest process. */
public final class RuntimeOptions {
    public static final String[] GRAPHICS_VALUES = {"native", "angle-vulkan", "angle-gl", "software"};
    public static final String[] GRAPHICS_LABELS = {
        "Native GPU (recommended)", "Vulkan compatibility", "OpenGL compatibility", "Software (slow diagnostics)"
    };
    public static final int[] VIEWPORT_WIDTHS = {0, 480, 640, 960};
    public static final int[] VIEWPORT_HEIGHTS = {0, 432, 576, 864};

    private static final String PREFS = "runtime";
    private static final String GRAPHICS = "graphics";
    private static final String WIDTH = "viewportWidth";
    private static final String HEIGHT = "viewportHeight";

    public final String graphics;
    public final boolean autoViewport;
    public final int width;
    public final int height;

    private RuntimeOptions(String graphics, int width, int height, boolean autoViewport) {
        this.autoViewport = autoViewport;
        this.graphics = graphics;
        this.width = width;
        this.height = height;
    }

    public static RuntimeOptions read(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String graphics = preferences.getString(GRAPHICS, BuildConfig.RUNTIME_GRAPHICS);
        if (!isGraphics(graphics)) graphics = BuildConfig.RUNTIME_GRAPHICS;
        if (!isGraphics(graphics)) graphics = "native";
        int width = preferences.getInt(WIDTH, VIEWPORT_WIDTHS[0]);
        int height = preferences.getInt(HEIGHT, VIEWPORT_HEIGHTS[0]);
        if (!isViewport(width, height)) {
            width = VIEWPORT_WIDTHS[0];
            height = VIEWPORT_HEIGHTS[0];
        }
        boolean automatic = width == 0 && height == 0;
        if (automatic) {
            android.graphics.Point size = new android.graphics.Point();
            context.getSystemService(android.view.WindowManager.class).getDefaultDisplay().getRealSize(size);
            int[] viewport = ViewportSize.auto(Math.max(size.x, size.y), Math.min(size.x, size.y));
            width = viewport[0]; height = viewport[1];
        }
        return new RuntimeOptions(graphics, width, height, automatic);
    }

    public static void save(Context context, String graphics, int width, int height) {
        if (!isGraphics(graphics)) throw new IllegalArgumentException("Unsupported graphics profile");
        if (!isViewport(width, height)) throw new IllegalArgumentException("Unsupported viewport");
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(GRAPHICS, graphics).putInt(WIDTH, width).putInt(HEIGHT, height).apply();
        RuntimeOptions resolved = read(context);
        applyDisplaySize(context, resolved.width, resolved.height);
    }

    static void applyDisplaySize(Context context, int width, int height) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean("adjustResolution", false)
            .putString("displayResolutionMode", "custom")
            .putString("displayResolutionCustom", width + "x" + height).apply();
    }

    public static boolean isGraphics(String value) {
        if (value == null) return false;
        for (String candidate : GRAPHICS_VALUES) if (candidate.equals(value)) return true;
        return false;
    }

    public static boolean isViewport(int width, int height) {
        for (int i = 0; i < VIEWPORT_WIDTHS.length; i++) {
            if (VIEWPORT_WIDTHS[i] == width && VIEWPORT_HEIGHTS[i] == height) return true;
        }
        return false;
    }

    public static int graphicsIndex(String value) {
        for (int i = 0; i < GRAPHICS_VALUES.length; i++) if (GRAPHICS_VALUES[i].equals(value)) return i;
        return 0;
    }

    public static int viewportIndex(int width, int height) {
        for (int i = 0; i < VIEWPORT_WIDTHS.length; i++) {
            if (VIEWPORT_WIDTHS[i] == width && VIEWPORT_HEIGHTS[i] == height) return i;
        }
        return 0;
    }

    public static int viewportIndex(RuntimeOptions options) {
        return options.autoViewport ? 0 : viewportIndex(options.width, options.height);
    }

    public static String viewportLabel(int index) {
        if (index == 0) return "Auto — match screen";
        return VIEWPORT_WIDTHS[index] + " × " + VIEWPORT_HEIGHTS[index];
    }
}
