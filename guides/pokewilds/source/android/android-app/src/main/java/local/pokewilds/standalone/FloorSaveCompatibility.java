package local.pokewilds.standalone;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/** Detects worlds that need the lossless floor loader before starting an unpatched game. */
final class FloorSaveCompatibility {
    private static final String ENTRY = "floor-pokemon.v1";

    private FloorSaveCompatibility() { }

    static String firstEnhancedWorld(File gameDirectory) throws IOException {
        File[] worlds = gameDirectory.listFiles();
        if (worlds == null) {
            if (gameDirectory.isDirectory()) throw new IOException("Cannot inspect installed worlds");
            return null;
        }
        for (File world : worlds) {
            if (!world.isDirectory() || !world.getName().endsWith(".sav")) continue;
            File[] maps = world.listFiles();
            if (maps == null) throw new IOException("Cannot inspect save folder " + world.getName());
            for (File map : maps) {
                if (!map.isFile() || !map.getName().startsWith("map") || !map.getName().endsWith(".json.zip")) continue;
                try (ZipFile zip = new ZipFile(map)) {
                    if (zip.getEntry(ENTRY) != null) return world.getName();
                } catch (IOException e) {
                    throw new IOException("Cannot inspect " + world.getName() + "/" + map.getName(), e);
                }
            }
        }
        return null;
    }
}
