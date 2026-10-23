package com.pkmngen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.pkmngen.game.util.SpriteProxy;
import com.pkmngen.game.util.TextureCache;

/** Native 8x8 CJK sprites from the user's Chinese reference; symbols keep modern fallback. */
public final class ZhReferenceFont {
    private ZhReferenceFont() {}

    /** Invoke after ZhText.install(Game). */
    public static void install(Game game) {
        String chars = Gdx.files.internal("localization/glyphs.txt").readString("UTF-8");
        String coverage = Gdx.files.internal("localization/reference8-glyphs.txt").readString("UTF-8");
        int installed = 0;
        Texture black = TextureCache.get(Gdx.files.internal("localization/reference-cn.png"));
        Texture white = TextureCache.get(Gdx.files.internal("localization/refinverse.png"));
        black.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        white.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        int columns = black.getWidth() / 8;
        for (int i = 0; i < chars.length(); ++i) {
            char c = chars.charAt(i);
            if (coverage.indexOf(c) < 0) continue;
            int index = c - 0x4e00;
            int x = index % columns * 8;
            int y = index / columns * 8;
            SpriteProxy normal = new SpriteProxy(Color.LIGHT_GRAY,black, x, y, 8, 8);
            SpriteProxy inverse = new SpriteProxy(Color.LIGHT_GRAY,white, x, y, 8, 8);
            game.textDict.put(c, normal);
            game.transparentDict.put(c, normal);
            game.textDictInverse.put(c, inverse);
            ++installed;
        }
        System.out.println("Reference native 8x8 Chinese glyphs installed: " + installed + " / " + chars.length());
    }
}
