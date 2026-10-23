package local.pokewilds.standalone;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

public class FloorSaveCompatibilityTest {
    private static void map(Path path, boolean exact) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            zip.putNextEntry(new ZipEntry("data.json"));
            zip.write("{}".getBytes());
            zip.closeEntry();
            if (exact) {
                zip.putNextEntry(new ZipEntry("floor-pokemon.v1"));
                zip.write("1\n0\n".getBytes());
                zip.closeEntry();
            }
        }
    }

    @Test public void blocksOnlyWorldsWithExactFloorEntry() throws Exception {
        Path game = Files.createTempDirectory("floor-save-test");
        try {
            Path old = Files.createDirectory(game.resolve("old.sav"));
            map(old.resolve("map0.json.zip"), false);
            assertNull(FloorSaveCompatibility.firstEnhancedWorld(game.toFile()));
            Path enhanced = Files.createDirectory(game.resolve("enhanced.sav"));
            map(enhanced.resolve("map0.json.zip"), true);
            assertEquals("enhanced.sav", FloorSaveCompatibility.firstEnhancedWorld(game.toFile()));
        } finally {
            java.io.File[] worlds = game.toFile().listFiles();
            if (worlds != null) for (java.io.File world : worlds) {
                java.io.File[] files = world.listFiles();
                if (files != null) for (java.io.File file : files) file.delete();
                world.delete();
            }
            Files.deleteIfExists(game);
        }
    }

    @Test public void refusesUnreadableMapArchive() throws Exception {
        Path game = Files.createTempDirectory("floor-save-corrupt-test");
        Path world = Files.createDirectory(game.resolve("corrupt.sav"));
        Path map = world.resolve("map0.json.zip");
        try {
            Files.write(map, new byte[]{1, 2, 3});
            try {
                FloorSaveCompatibility.firstEnhancedWorld(game.toFile());
                fail("An unreadable map ZIP must not bypass the startup guard");
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().contains("corrupt.sav/map0.json.zip"));
            }
        } finally {
            Files.deleteIfExists(map);
            Files.deleteIfExists(world);
            Files.deleteIfExists(game);
        }
    }
}
