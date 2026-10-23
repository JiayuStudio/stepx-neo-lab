package com.pkmngen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;

/** Battle screenshot compatibility for logical-point windows and Retina framebuffers. */
public final class ZhFrameBuffer {
    private ZhFrameBuffer() {}

    /**
     * Exact ScreenUtils.getFrameBufferPixmap(IIII) replacement, scoped to five battle sites.
     * Read the centered 160x144 canvas in physical pixels; return the caller's legacy scale.
     * No row flip: upstream effects treat glReadPixels row zero as battle y=0.
     */
    public static Pixmap capture(int x, int y, int width, int height) {
        int logicalWidth = Gdx.graphics.getWidth();
        int logicalHeight = Gdx.graphics.getHeight();
        int framebufferWidth = Gdx.graphics.getBackBufferWidth();
        int framebufferHeight = Gdx.graphics.getBackBufferHeight();
        int[] bounds = captureBounds(logicalWidth, logicalHeight, framebufferWidth, framebufferHeight);
        int[] size = outputSize(x, y, width, height);
        Game game = Game.staticGame;
        if (game != null) {
            if (game.mapBatch != null && game.mapBatch.isDrawing()) game.mapBatch.flush();
            if (game.uiBatch != null && game.uiBatch.isDrawing()) game.uiBatch.flush();
        }
        Pixmap source = ScreenUtils.getFrameBufferPixmap(bounds[0], bounds[1], bounds[2], bounds[3]);
        if (source.getWidth() == size[0] && source.getHeight() == size[1]) return source;
        Pixmap output = new Pixmap(size[0], size[1], Pixmap.Format.RGBA8888);
        output.setBlending(Pixmap.Blending.None);
        output.setFilter(Pixmap.Filter.NearestNeighbour);
        try {
            output.drawPixmap(source, 0, 0, source.getWidth(), source.getHeight(),
                              0, 0, size[0], size[1]);
            return output;
        } catch (RuntimeException | Error failure) {
            output.dispose();
            throw failure;
        } finally {
            source.dispose();
        }
    }

    /** Pure geometry, shared with the headless native-pixmap acceptance checks. */
    public static int[] captureBounds(int logicalWidth, int logicalHeight,
                                      int framebufferWidth, int framebufferHeight) {
        if (logicalWidth <= 0 || logicalHeight <= 0 || framebufferWidth <= 0 || framebufferHeight <= 0)
            throw new IllegalArgumentException("Framebuffer dimensions must be positive");
        double fit = ZhWindow.fitScale(logicalWidth, logicalHeight);
        double canvasWidth = 160.0 * fit;
        double canvasHeight = 144.0 * fit;
        double logicalLeft = (logicalWidth - canvasWidth) / 2.0;
        double logicalBottom = (logicalHeight - canvasHeight) / 2.0;
        double densityX = framebufferWidth / (double) logicalWidth;
        double densityY = framebufferHeight / (double) logicalHeight;
        int left = Math.max(0, (int) Math.round(logicalLeft * densityX));
        int bottom = Math.max(0, (int) Math.round(logicalBottom * densityY));
        int right = Math.min(framebufferWidth, (int) Math.round((logicalLeft + canvasWidth) * densityX));
        int top = Math.min(framebufferHeight, (int) Math.round((logicalBottom + canvasHeight) * densityY));
        return new int[] {left, bottom, Math.max(1, right - left), Math.max(1, top - bottom)};
    }

    public static int[] outputSize(int x, int y, int width, int height) {
        // Slash, ShadowClaw and Lick explicitly sample i*3,j*3 from 480x432.
        if (x == 0 && y == 0 && width == 480 && height == 432) return new int[] {480, 432};
        // Psychic and LoadAndPlayAnimation sample i*heightM,j*heightM.
        // In portrait mode width can exceed the window; that is a virtual legacy buffer.
        if (height <= 0) throw new IllegalArgumentException("Screenshot height must be positive");
        float heightM = height / 144f;
        int outputWidth = (int) Math.ceil(160.0 * heightM);
        int outputHeight = (int) Math.ceil(144.0 * heightM);
        // Avoid float rounding truncating the subsequent newPixmap to 159x143.
        while ((int) (outputWidth / heightM) < 160) ++outputWidth;
        while ((int) (outputHeight / heightM) < 144) ++outputHeight;
        return new int[] {outputWidth, outputHeight};
    }
}
