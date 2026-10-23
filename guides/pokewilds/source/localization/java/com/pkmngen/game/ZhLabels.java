package com.pkmngen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import java.util.HashMap;
import java.util.Map;

/** Runtime overlays for English baked into the original PNGs. Source art is preserved. */
public final class ZhLabels {
    private static Texture white;
    private static final Map<String,Color> backgroundColors = new HashMap<>();
    private static final String[] BAG = {"道具", "球类", "材料", "重要道具", "药品"};

    private static Texture white() {
        if (white == null) {
            Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixel.setColor(Color.WHITE);
            pixel.fill();
            white = new Texture(pixel);
            pixel.dispose();
        }
        return white;
    }

    private static Color statsColor(int index) {
        String path = "menu/stats_screen" + (index + 1) + ".png";
        Color color = backgroundColors.get(path);
        if (color == null) {
            Pixmap image = new Pixmap(Gdx.files.internal(path));
            color = new Color();
            Color.rgba8888ToColor(color, image.getPixel(157, 70));
            image.dispose();
            backgroundColors.put(path, color);
        }
        return color;
    }

    /** Coordinates are the source PNG's top-left pixel coordinates, plus screen offset. */
    private static void patch(Game game, float screenX, float screenY, int x, int top,
                              int width, int height, String text, Color mask, boolean inverse) {
        float bottom = 144 - top - height + screenY;
        game.uiBatch.setColor(mask);
        game.uiBatch.draw(white(), x + screenX, bottom, width, height);
        game.uiBatch.setColor(Color.WHITE);
        drawText(game, text, x + screenX, bottom, inverse);
    }

    private static void drawText(Game game, String text, float x, float y, boolean inverse) {
        for (int i = 0; i < text.length(); ++i) {
            char letter = text.charAt(i);
            Sprite glyph = inverse ? game.textDictInverse.get(letter) : game.textDict.get(letter);
            if (glyph != null) game.uiBatch.draw(glyph, x + 8 * i, y, 8, 8);
        }
    }

    private static void normalBattle(Game game, DrawBattleMenuNormal menu) {
        if (menu.disabled || game.battle.playerLockedMove != -1) return;
        patch(game, 0, 0, 80, 112, 40, 8, "战斗", Color.WHITE, false);
        patch(game, 0, 0, 128, 112, 24, 8, "宝可梦", Color.WHITE, false);
        patch(game, 0, 0, 80, 128, 32, 8, "道具", Color.WHITE, false);
        patch(game, 0, 0, 128, 128, 24, 8, "逃跑", Color.WHITE, false);
    }

    private static void safariBattle(Game game) {
        patch(game, 0, 0, 16, 112, 56, 8, "狩猎球×30", Color.WHITE, false);
        patch(game, 0, 0, 112, 112, 32, 8, "诱饵", Color.WHITE, false);
        patch(game, 0, 0, 16, 128, 80, 8, "扔石头", Color.WHITE, false);
        patch(game, 0, 0, 112, 128, 24, 8, "逃跑", Color.WHITE, false);
    }

    private static void attacks(Game game, DrawAttacksMenu menu) {
        // Learning a new move moves the complete panel up by 48 pixels.
        patch(game, menu.offset.x, menu.offset.y, 8, 72, 40, 8, "属性/", Color.WHITE, false);
    }

    private static void stats(Game game, DrawStatsScreen menu) {
        int page = Math.max(0, Math.min(2, menu.currIndex));
        patch(game, 0, 0, 64, 0, 16, 8, "编号", Color.WHITE, false);
        patch(game, 0, 0, 112, 0, 8, 8, "级", Color.WHITE, false);
        Color color = statsColor(page);
        if (page == 0) {
            patch(game, 0, 0, 80, 72, 80, 8, "经验值", color, false);
            patch(game, 0, 0, 0, 96, 56, 8, "状态/", color, false);
            patch(game, 0, 0, 80, 96, 64, 8, "下一级", color, false);
            patch(game, 0, 0, 0, 112, 40, 8, "属性/", color, false);
            patch(game, 0, 0, 112, 112, 32, 8, "目标等级", color, false);
        } else if (page == 1) {
            patch(game, 0, 0, 0, 64, 32, 8, "道具", color, false);
            patch(game, 0, 0, 0, 80, 32, 8, "招式", color, false);
        } else {
            patch(game, 0, 0, 0, 72, 24, 8, "ID号", color, false);
            patch(game, 0, 0, 0, 96, 32, 8, "初训家/", color, false);
            // Numeric values occupy the next eight-pixel row and remain untouched.
            patch(game, 0, 0, 88, 64, 48, 8, "攻击", color, false);
            patch(game, 0, 0, 88, 80, 56, 8, "防御", color, false);
            patch(game, 0, 0, 88, 96, 64, 8, "特攻", color, false);
            patch(game, 0, 0, 88, 112, 64, 8, "特防", color, false);
            patch(game, 0, 0, 88, 128, 40, 8, "速度", color, false);
        }
    }

    private static void bag(Game game) {
        patch(game, 0, 0, 16, 0, 48, 8, "分类", Color.BLACK, true);
        patch(game, 0, 0, 104, 0, 32, 8, "道具", Color.BLACK, true);
        // bagNames[boxIndex] is a 40x24 region drawn at (0,96). Preserve its border.
        game.uiBatch.setColor(Color.BLACK);
        game.uiBatch.draw(white(), 2, 98, 36, 20);
        game.uiBatch.setColor(Color.WHITE);
        String text = BAG[Math.floorMod(DrawItemMenuGen2.boxIndex, BAG.length)];
        drawText(game, text, (40 - 8 * text.length()) / 2.0f, 104, true);
    }

    /** Inject at the final draw return of each supported screen's step(Game). */
    public static void labels(Game game, Action screen) {
        Color previous = new Color(game.uiBatch.getColor());
        try {
            if (screen instanceof DrawBattleMenuNormal) normalBattle(game, (DrawBattleMenuNormal)screen);
            else if (screen instanceof DrawBattleMenuSafariZone) safariBattle(game);
            else if (screen instanceof DrawAttacksMenu) attacks(game, (DrawAttacksMenu)screen);
            else if (screen instanceof DrawStatsScreen) stats(game, (DrawStatsScreen)screen);
            else if (screen instanceof DrawItemMenuGen2) bag(game);
        } finally {
            game.uiBatch.setColor(previous);
        }
    }

    /** Fallback for an injector that supplies a class name rather than this. */
    public static void labels(Game game, String className) {
        for (Action action : game.actionStackCopy) {
            if (action != null && (action.getClass().getName().equals(className)
                || action.getClass().getSimpleName().equals(className))) {
                labels(game, action);
                return;
            }
        }
    }

    public static void dispose() {
        if (white != null) white.dispose();
        white = null;
        backgroundColors.clear();
    }
}
