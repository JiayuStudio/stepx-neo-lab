package local.pokewilds.standalone;

/** Low-resolution game canvas matched to the available display area. */
final class ViewportSize {
    private ViewportSize() { }

    /** Integer-scaled Auto canvas for the zoom patch. */
    static int[] autoZoom(int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) return new int[]{480, 432};
        int upscale = screenWidth >= 960 && screenHeight >= 864 ? 2 : 1;
        int width = (screenWidth & ~1) / upscale;
        int height = (screenHeight & ~1) / upscale;
        // The desktop menus require at least a 10:9 canvas. On portrait and
        // square displays, center a shorter game picture instead of rendering
        // into a tall canvas whose menu extends beyond its usable width.
        height = Math.min(height, (width * 432 / 480) & ~1);
        if (width < 480 || height < 432) return auto(screenWidth, screenHeight);
        return new int[]{width, height};
    }

    static int[] auto(int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) return new int[]{480, 432};
        // Preserve the desktop menu's minimum canvas. A 16:9 screen becomes
        // 768x432, rather than rendering at the panel's full native resolution.
        // The unmodified desktop menu needs at least the original 10:9
        // aspect ratio. Narrow/portrait windows retain that canvas and fit it.
        if ((double) screenWidth / screenHeight < 480.0 / 432) return new int[]{480, 432};
        double scale = 432.0 / screenHeight;
        int width = Math.max(480, (int) Math.round(screenWidth * scale / 8.0) * 8);
        int height = Math.max(432, (int) Math.round(screenHeight * scale));
        return new int[]{width, height};
    }
}
