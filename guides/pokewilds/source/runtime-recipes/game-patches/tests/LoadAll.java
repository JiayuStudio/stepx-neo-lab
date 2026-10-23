package local.pokewilds.bugfix;
import java.util.*;
import java.util.zip.*;
public class LoadAll {
    public static void main(String[] a) throws Exception {
        ClassLoader cl = LoadAll.class.getClassLoader();
        int n = 0, verr = 0, other = 0;
        try (ZipFile z = new ZipFile(a[0])) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                String nm = e.nextElement().getName();
                if (!nm.startsWith("com/pkmngen/game/") || !nm.endsWith(".class")) continue;
                String cn = nm.substring(0, nm.length() - 6).replace('/', '.');
                try { Class.forName(cn, true, cl); n++; }
                catch (VerifyError ve) { verr++; System.out.println("VERIFY ERROR " + cn + ": " + ve.getMessage().split("\n")[0]); }
                catch (Throwable t) { other++; n++; }   // static init needing GL/Gdx: verification already passed
            }
        }
        System.out.println("loaded " + n + " classes, verify errors: " + verr + ", init-failures(ignored): " + other);
        // reflection names used by Hooks exist in the real game
        Class<?> game = Class.forName("com.pkmngen.game.Game", false, cl), map = Class.forName("com.pkmngen.game.PkmnMap", false, cl), pk = Class.forName("com.pkmngen.game.Pokemon", false, cl);
        Hooks.open(game, "staticGame"); Hooks.open(game, "map"); Hooks.open(map, "tiles"); Hooks.open(map, "interiorTiles");
        Hooks.open(pk, "mapTiles"); Hooks.open(pk, "interiorIndex");
        System.out.println("reflection targets (Game.staticGame, Game.map, PkmnMap.tiles/interiorTiles, Pokemon.mapTiles/interiorIndex) all found");
    }
}
