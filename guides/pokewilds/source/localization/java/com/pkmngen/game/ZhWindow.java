package com.pkmngen.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.system.MemoryStack;

/** Window resizing in logical macOS points; no texture filters or FBO behavior change. */
public final class ZhWindow {
    public static final float CANVAS_WIDTH = 160f;
    public static final float CANVAS_HEIGHT = 144f;
    public static final int DEFAULT_WIDTH = 720;
    public static final int DEFAULT_HEIGHT = 648;
    public static final int MIN_WIDTH = 320;
    public static final int MIN_HEIGHT = 288;
    private static final boolean STEAM_DECK = Boolean.getBoolean("pokewilds.steamdeck");
    private static final Field VIEWPORT = viewportField();
    private static int restoreWidth = DEFAULT_WIDTH;
    private static int restoreHeight = DEFAULT_HEIGHT;
    private static int restoreX;
    private static int restoreY;
    private static final ConcurrentLinkedQueue<Integer> SHORTCUTS = new ConcurrentLinkedQueue<>();
    private static long shortcutWindow;
    private static GLFWKeyCallback shortcutCallback;
    private static GLFWKeyCallback previousKeyCallback;
    private static ShortcutListener shortcutListener;

    private ZhWindow() {}

    private static Field viewportField() {
        try {
            Field field = Game.class.getDeclaredField("viewport");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static ScreenViewport viewport(Game game) {
        try {
            return (ScreenViewport) VIEWPORT.get(game);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access game viewport", e);
        }
    }

    /** Inject before createConfig returns, after upstream setWindowedMode. */
    public static void configure(Lwjgl3ApplicationConfiguration config) {
        config.setHdpiMode(HdpiMode.Logical);
        config.setResizable(true);
        if (STEAM_DECK) {
            config.setWindowSizeLimits(640, 400, -1, -1);
            config.setWindowedMode(1280, 800);
            config.setDecorated(false);
            return;
        }
        config.setWindowSizeLimits(MIN_WIDTH, MIN_HEIGHT, -1, -1);
        int[] workarea = primaryWorkarea();
        float scale = Math.min(DEFAULT_WIDTH / CANVAS_WIDTH,
            Math.min((workarea[2] - 48f) / CANVAS_WIDTH,
                     (workarea[3] - 80f) / CANVAS_HEIGHT));
        scale = Math.max(2f, scale);
        int width = Math.round(CANVAS_WIDTH * scale);
        int height = Math.round(CANVAS_HEIGHT * scale);
        config.setWindowedMode(width, height);
        config.setWindowPosition(workarea[0] + Math.max(0, (workarea[2] - width) / 2),
                                 workarea[1] + Math.max(0, (workarea[3] - height) / 2));
    }

    /** Inject at Game.resize entry before ScreenViewport.update. */
    public static void beforeResize(Game game, int width, int height) {
        if (width <= 0 || height <= 0 || game.cam == null || !isDesktop()) return;
        ScreenViewport viewport = viewport(game);
        if (viewport == null) return;
        float scale = fitScale(width, height);
        viewport.setUnitsPerPixel(1f / scale);
        game.scale = Math.max(1, Math.round(scale));
    }

    /** Inject before Game.resize returns, correcting narrow/portrait and odd dimensions. */
    public static void afterResize(Game game, int width, int height) {
        if (width <= 0 || height <= 0 || game.cam == null || !isDesktop()) return;
        ScreenViewport viewport = viewport(game);
        if (viewport == null) return;
        float unitsPerPixel = 1f / fitScale(width, height);
        viewport.setUnitsPerPixel(unitsPerPixel);
        // Use exact logical dimensions. Viewport applies HDPI conversion for Retina GL.
        viewport.update(width, height, false);
        float worldWidth = width * unitsPerPixel;
        float worldHeight = height * unitsPerPixel;
        // Keep the whole 160x144 UI centered. Extra space shows more of the world.
        game.uiBatch.getProjectionMatrix().setToOrtho2D(
            (CANVAS_WIDTH - worldWidth) / 2f,
            (CANVAS_HEIGHT - worldHeight) / 2f,
            worldWidth, worldHeight);
        game.currScreen.set(width, height);
    }

    public static float fitScale(int width, int height) {
        float fit = Math.min(width / CANVAS_WIDTH, height / CANVAS_HEIGHT);
        return STEAM_DECK ? Math.max(1f, (float) Math.floor(fit)) : fit;
    }

    /** Replace currScreen.x / 6f in Game.render's map-edge checks with this call. */
    public static float halfWidth(Game game) {
        return game.cam.viewportWidth * game.cam.zoom / 2f;
    }

    /** Replace currScreen.y / 6f in Game.render's map-edge checks with this call. */
    public static float halfHeight(Game game) {
        return game.cam.viewportHeight * game.cam.zoom / 2f;
    }

    /** Inject once at render entry. Resize commands run here, outside native input callbacks. */
    public static void tick(Game game) {
        if (!(Gdx.graphics instanceof Lwjgl3Graphics)) return;
        Lwjgl3Window window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        installShortcutCallback(window.getWindowHandle());
        Integer direction;
        while ((direction = pollShortcutDirection()) != null) {
            if (!Gdx.graphics.isFullscreen()) resizeByStep(window, direction);
        }
    }

    private static void installShortcutCallback(long handle) {
        if (handle == shortcutWindow && shortcutCallback != null) return;
        dispose(null);
        ShortcutListener listener = new ShortcutListener();
        GLFWKeyCallback callback = GLFWKeyCallback.create(listener);
        GLFWKeyCallback original;
        try {
            original = GLFW.glfwSetKeyCallback(handle, callback);
        } catch (RuntimeException | Error failure) {
            callback.free();
            throw failure;
        }
        listener.previous = original;
        previousKeyCallback = original;
        shortcutListener = listener;
        shortcutCallback = callback;
        shortcutWindow = handle;
    }

    /** The previous LWJGL listener receives every press/release/repeat without alteration. */
    static final class ShortcutListener implements GLFWKeyCallbackI {
        volatile GLFWKeyCallbackI previous;

        @Override public void invoke(long window, int key, int scancode, int action, int mods) {
            int direction = shortcutDirection(key, action, mods);
            if (direction != 0) SHORTCUTS.add(direction);
            GLFWKeyCallbackI original = previous;
            if (original != null) original.invoke(window, key, scancode, action, mods);
        }
    }

    static int shortcutDirection(int key, int action, int mods) {
        // The event modifier bits survive even if all keys were released before render.
        if (action != GLFW.GLFW_PRESS || (mods & GLFW.GLFW_MOD_SUPER) == 0) return 0;
        if (key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) return 1;
        if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) return -1;
        return 0;
    }

    static Integer pollShortcutDirection() {
        return SHORTCUTS.poll();
    }

    /** Inject at Game.dispose entry, before LWJGL frees its original key callback/window. */
    public static void dispose(Game game) {
        if (shortcutCallback != null) {
            GLFWKeyCallback displaced = GLFW.glfwSetKeyCallback(shortcutWindow, previousKeyCallback);
            if (displaced != null && displaced.address() != shortcutCallback.address()) {
                // Preserve a callback another component installed after ours.
                GLFW.glfwSetKeyCallback(shortcutWindow, displaced);
            }
            shortcutCallback.free();
        }
        previousKeyCallback = null;
        shortcutListener = null;
        shortcutCallback = null;
        shortcutWindow = 0L;
        SHORTCUTS.clear();
    }

    /** Preserve short input taps for the patched gameplay InputProcessor. */
    public static boolean keyDown(int key) {
        return Gdx.input.isKeyPressed(key) || Gdx.input.isKeyJustPressed(key);
    }

    /** Replace private Game.toggleFullScreen body with this static call and return. */
    public static void toggleFullscreen(Game game) {
        if (!(Gdx.graphics instanceof Lwjgl3Graphics)) return;
        Lwjgl3Graphics graphics = (Lwjgl3Graphics) Gdx.graphics;
        Lwjgl3Window window = graphics.getWindow();
        if (STEAM_DECK) {
            // Gamescope owns the display mode. F11 restores the native game window only.
            graphics.setWindowedMode(1280, 800);
            graphics.setUndecorated(true);
            return;
        }
        if (graphics.isFullscreen()) {
            if (graphics.setWindowedMode(restoreWidth, restoreHeight)) {
                graphics.setUndecorated(false);
                graphics.setResizable(true);
                window.setPosition(restoreX, restoreY);
            }
        } else {
            restoreWidth = graphics.getLogicalWidth();
            restoreHeight = graphics.getLogicalHeight();
            restoreX = window.getPositionX();
            restoreY = window.getPositionY();
            Graphics.DisplayMode mode = graphics.getDisplayMode(graphics.getMonitor());
            graphics.setFullscreenMode(mode);
        }
    }

    private static boolean isDesktop() {
        return Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.Desktop;
    }

    private static void resizeByStep(Lwjgl3Window window, int direction) {
        Lwjgl3Graphics graphics = (Lwjgl3Graphics) Gdx.graphics;
        int[] workarea = monitorWorkarea(graphics);
        int[] size = windowSizeForStep(graphics.getLogicalWidth(), graphics.getLogicalHeight(),
                                     workarea[2], workarea[3], direction);
        int width = size[0];
        int height = size[1];
        int x = window.getPositionX();
        int y = window.getPositionY();
        if (graphics.setWindowedMode(width, height)) {
            window.setPosition(Math.max(workarea[0], Math.min(x, workarea[0] + workarea[2] - width)),
                               Math.max(workarea[1], Math.min(y, workarea[1] + workarea[3] - height - 32)));
        }
    }

    static int[] windowSizeForStep(int width, int height, int workWidth, int workHeight, int direction) {
        float currentScale = fitScale(width, height);
        float maxScale = Math.min((workWidth - 48f) / CANVAS_WIDTH,
                                 (workHeight - 80f) / CANVAS_HEIGHT);
        float newScale = Math.max(2f, Math.min(currentScale + direction * 0.5f, maxScale));
        return new int[] {Math.round(CANVAS_WIDTH * newScale), Math.round(CANVAS_HEIGHT * newScale)};
    }

    private static int[] primaryWorkarea() {
        // createConfig follows getScale(), which initializes GLFW through getDisplayMode().
        return workarea(GLFW.glfwGetPrimaryMonitor());
    }

    private static int[] monitorWorkarea(Lwjgl3Graphics graphics) {
        // Monitor workareas and GLFW window dimensions share logical point units on macOS.
        Graphics.Monitor monitor = graphics.getMonitor();
        long nativeMonitor = GLFW.glfwGetPrimaryMonitor();
        org.lwjgl.PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors != null) {
            for (int i = 0; i < monitors.limit(); ++i) {
                long candidate = monitors.get(i);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer x = stack.mallocInt(1), y = stack.mallocInt(1);
                    GLFW.glfwGetMonitorPos(candidate, x, y);
                    if (x.get(0) == monitor.virtualX && y.get(0) == monitor.virtualY) {
                        nativeMonitor = candidate;
                        break;
                    }
                }
            }
        }
        return workarea(nativeMonitor);
    }

    private static int[] workarea(long monitor) {
        if (monitor == 0L) return new int[] {0, 0, 1440, 900};
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1), y = stack.mallocInt(1);
            IntBuffer width = stack.mallocInt(1), height = stack.mallocInt(1);
            GLFW.glfwGetMonitorWorkarea(monitor, x, y, width, height);
            if (width.get(0) <= 0 || height.get(0) <= 0)
                return new int[] {0, 0, 1440, 900};
            return new int[] {x.get(0), y.get(0), width.get(0), height.get(0)};
        }
    }
}
