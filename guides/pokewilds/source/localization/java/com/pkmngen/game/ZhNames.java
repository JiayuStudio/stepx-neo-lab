package com.pkmngen.game;

import com.badlogic.gdx.Gdx;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Properties;

/** Names for display contexts whose English identifiers have multiple meanings. */
public final class ZhNames {
    private ZhNames() {}

    private static Properties read(String bundle) {
        Properties values = new Properties();
        String path = "localization/" + bundle + "_zh_CN.properties";
        try {
            // readString decodes UTF-8 before Java properties parsing.
            values.load(new StringReader(Gdx.files.internal(path).readString("UTF-8")));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Chinese names: " + path, error);
        }
        return values;
    }

    private static final class Types {
        static final Properties VALUES = read("type");
    }
    private static final class FieldMoves {
        static final Properties VALUES = read("fieldmove");
    }
    private static final class Species {
        static final Properties VALUES = read("pokemondisplayname");
    }

    private static String key(String source) {
        return source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
    }

    /** Resolve an elemental type, including the unknown type of Curse. */
    public static String typeName(String source) {
        if (source == null) return "";
        String name = key(source);
        if (name.equals("curse_t")) name = "typeless";
        return Types.VALUES.getProperty(name, source);
    }

    /** Resolve a move category separately from the general STATUS menu label. */
    public static String categoryName(String source) {
        if (source == null) return "";
        switch (key(source)) {
            case "physical": return "物理";
            case "special": return "特殊";
            case "status": return "变化";
            default: return source;
        }
    }

    /** Party context: REPel is a field action, rather than the spray item. */
    public static String fieldMoveName(String source) {
        if (source == null) return "";
        String translated = FieldMoves.VALUES.getProperty(key(source));
        return translated != null ? translated : ZhText.translate(source);
    }

    private static String speciesKey(String source) {
        switch (key(source)) {
            // These names occur in release code and in older mod resources.
            case "aexeggutor": return "exeggutor_alolan";
            case "araichu": return "raichu_alolan";
            case "amarowak": return "marowak_alolan";
            case "gcorsola": return "corsola_galarian";
            case "gyamask": return "yamask_galarian";
            case "gdarumaka": return "darumaka_galarian";
            case "gdarmanitan": return "darmanitan_galarian";
            case "gdarmanitanzen": return "darmanitan_galarian_zenmode";
            case "darmanitanzen": return "darmanitan_zenmode";
            default: return key(source);
        }
    }

    /** Read a display name without changing Pokemon fields or serialized data. */
    public static String displayNickname(Pokemon pokemon) {
        if (pokemon == null) return "";
        String nickname = pokemon.nickname;
        if (pokemon.isEgg) return Species.VALUES.getProperty("egg", "蛋");
        if (pokemon.isGhost && "ghost".equalsIgnoreCase(nickname))
            return Species.VALUES.getProperty("ghost", "幽灵");
        if (pokemon.specie == null) return nickname == null ? "" : nickname;

        String defaultName = pokemon.getNickname();
        // A custom nickname remains the player's exact text. Nidoran and forms
        // require the species identifier, since getNickname collapses them.
        if (nickname != null && !nickname.equalsIgnoreCase(defaultName))
            return "\uE000"+nickname+"\uE001";
        String translated = Species.VALUES.getProperty(speciesKey(pokemon.specie.name));
        if (translated != null) return translated;
        // Unknown mod species keep their supplied names; Raitora has no official name.
        return nickname == null ? defaultName : nickname;
    }
}
