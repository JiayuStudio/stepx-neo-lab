package local.pokewilds.standalone;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ModManagerTest {
    @Test public void zipImportOverlaysExistingModsAndPreservesOtherFiles() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-import-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/pokemon/pikachu/front.png"), "original");
            write(game.resolve("mods/README.txt"), "keep");
            byte[] zip = archive(Map.of(
                "mods/pokemon/pikachu/front.png", "replacement",
                "mods/music/wild_battle.ogg", "new music"));

            ModManager.importZip(new ByteArrayInputStream(zip), game);

            assertEquals("replacement", read(game.resolve("mods/pokemon/pikachu/front.png")));
            assertEquals("new music", read(game.resolve("mods/music/wild_battle.ogg")));
            assertEquals("keep", read(game.resolve("mods/README.txt")));
            assertFalse(Files.exists(game.resolve(".mods-import.preparing")));
            assertFalse(Files.exists(game.resolve(".mods.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void directFolderImportUsesContentsAsModsRoot() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-folder-test");
        try {
            Path game = game(root), stage = game.resolve(".mods-import.preparing");
            Path incoming = stage.resolve("incoming");
            Files.createDirectories(incoming);
            write(incoming.resolve("player/red/walking.png"), "sprite");

            ModManager.installPrepared(incoming, game, stage);

            assertEquals("sprite", read(game.resolve("mods/player/red/walking.png")));
            assertFalse(Files.exists(game.resolve("mods/incoming")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void wrappedModPackImportsMainModsWithoutOptionalVariants() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-wrapped-test");
        try {
            Path game = game(root);
            byte[] zip = archive(Map.of(
                "Mod Pack/mods/pokemon/pikachu/front.png", "main sprite",
                "Mod Pack/!OPTIONAL MINI MODS/variant/mods/pokemon/pikachu/front.png", "optional sprite",
                "Mod Pack/notes.txt", "instructions"));

            ModManager.importZip(new ByteArrayInputStream(zip), game);

            assertEquals("main sprite", read(game.resolve("mods/pokemon/pikachu/front.png")));
            assertFalse(Files.exists(game.resolve("mods/Mod Pack")));
            assertFalse(Files.exists(game.resolve("mods/!OPTIONAL MINI MODS")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void modPackWithMoreThanTenThousandFilesImports() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-large-test");
        try {
            Path game = game(root);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                for (int i = 0; i < 10001; i++) {
                    zip.putNextEntry(new ZipEntry(String.format("Mod Pack/mods/items/item-%05d.txt", i)));
                    zip.write('x');
                    zip.closeEntry();
                }
            }

            ModManager.importZip(new ByteArrayInputStream(output.toByteArray()), game);

            assertEquals("x", read(game.resolve("mods/items/item-10000.txt")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void unsafeZipLeavesInstalledModsUntouched() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-invalid-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/README.txt"), "installed");
            try {
                ModManager.importZip(new ByteArrayInputStream(archive(Map.of("../outside.txt", "bad"))), game);
                fail("Accepted an escaping ZIP entry");
            } catch (IOException expected) { /* rejected */ }
            assertEquals("installed", read(game.resolve("mods/README.txt")));
            assertFalse(Files.exists(root.resolve("outside.txt")));
            assertFalse(Files.exists(game.resolve(".mods-import.preparing")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void conflictingDirectoryLeavesInstalledModsUntouched() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-conflict-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/player"), "existing file");
            byte[] zip = archive(Map.of("mods/player/red/walking.png", "new sprite"));
            try {
                ModManager.importZip(new ByteArrayInputStream(zip), game);
                fail("Accepted a file/directory conflict");
            } catch (IOException expected) { /* rejected without activating the stage */ }
            assertEquals("existing file", read(game.resolve("mods/player")));
            assertFalse(Files.exists(game.resolve(".mods.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void interruptedSwapRecoversOldOrKeepsNewMods() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-recovery-test");
        try {
            Path game = game(root);
            write(game.resolve(".mods.previous/old.txt"), "old");
            write(game.resolve(".mods-import.preparing/incoming/new.txt"), "unfinished");
            ModManager.recover(game);
            assertEquals("old", read(game.resolve("mods/old.txt")));
            assertFalse(Files.exists(game.resolve(".mods-import.preparing")));

            write(game.resolve(".mods.previous/old.txt"), "backup");
            write(game.resolve("mods/new.txt"), "new");
            ModManager.recover(game);
            assertEquals("new", read(game.resolve("mods/new.txt")));
            assertFalse(Files.exists(game.resolve(".mods.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void exportContainsModsPrefix() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-export-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/pokemon/pikachu/front.png"), "sprite");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ModManager.exportZip(game, output);
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
                ZipEntry entry = zip.getNextEntry();
                assertNotNull(entry);
                assertEquals("mods/pokemon/pikachu/front.png", entry.getName());
                assertEquals("sprite", new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                assertNull(zip.getNextEntry());
            }
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void browseAndRemoveOneFilePreservesOtherMods() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-remove-file-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/pokemon/pikachu/front.png"), "sprite");
            write(game.resolve("mods/pokemon/pikachu/back.png"), "back sprite");
            write(game.resolve("mods/music/wild.ogg"), "music");

            List<ModManager.Entry> top = ModManager.list(game, Paths.get(""));
            assertEquals(2, top.size());
            assertTrue(top.stream().allMatch(entry -> entry.directory));
            List<ModManager.Entry> sprites = ModManager.list(game, Paths.get("pokemon/pikachu"));
            assertEquals(2, sprites.size());
            assertEquals("back.png", sprites.get(0).name);
            assertEquals("front.png", sprites.get(1).name);

            ModManager.removeSelected(game, Paths.get("pokemon/pikachu"), List.of("front.png"));

            assertFalse(Files.exists(game.resolve("mods/pokemon/pikachu/front.png")));
            assertEquals("back sprite", read(game.resolve("mods/pokemon/pikachu/back.png")));
            assertEquals("music", read(game.resolve("mods/music/wild.ogg")));
            assertFalse(Files.exists(game.resolve(".mods-import.preparing")));
            assertFalse(Files.exists(game.resolve(".mods.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void removingOneFolderPreservesItsSibling() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-remove-folder-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/pokemon/pikachu/front.png"), "sprite");
            write(game.resolve("mods/music/wild.ogg"), "music");

            ModManager.removeSelected(game, Paths.get(""), List.of("pokemon"));

            assertFalse(Files.exists(game.resolve("mods/pokemon")));
            assertEquals("music", read(game.resolve("mods/music/wild.ogg")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void invalidRemovalCannotEscapeModsOrChangeFiles() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-remove-invalid-test");
        try {
            Path game = game(root);
            write(game.resolve("mods/keep.txt"), "keep");
            write(game.resolve("save.txt"), "save");
            for (Path directory : List.of(Paths.get(".."), Paths.get("missing"))) {
                try {
                    ModManager.removeSelected(game, directory, List.of("save.txt"));
                    fail("Accepted an invalid mods directory");
                } catch (IOException expected) { /* Existing files remain intact. */ }
            }
            try {
                ModManager.removeSelected(game, Paths.get(""), List.of("../save.txt"));
                fail("Accepted an escaping file name");
            } catch (IOException expected) { /* Existing files remain intact. */ }
            assertEquals("keep", read(game.resolve("mods/keep.txt")));
            assertEquals("save", read(game.resolve("save.txt")));
            assertFalse(Files.exists(game.resolve(".mods.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void removingLinkDoesNotFollowItsTarget() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-remove-link-test");
        try {
            Path game = game(root);
            write(game.resolve("save.txt"), "outside");
            Files.createDirectories(game.resolve("mods"));
            Files.createSymbolicLink(game.resolve("mods/link"), game.resolve("save.txt"));
            write(game.resolve("mods/keep.txt"), "keep");

            ModManager.removeSelected(game, Paths.get(""), List.of("link"));

            assertFalse(Files.exists(game.resolve("mods/link"), LinkOption.NOFOLLOW_LINKS));
            assertEquals("outside", read(game.resolve("save.txt")));
            assertEquals("keep", read(game.resolve("mods/keep.txt")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void removalRecoversInterruptedPriorSwap() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-remove-recover-test");
        try {
            Path game = game(root);
            write(game.resolve(".mods.previous/remove.txt"), "old");
            write(game.resolve(".mods.previous/keep.txt"), "keep");

            ModManager.removeSelected(game, Paths.get(""), List.of("remove.txt"));

            assertFalse(Files.exists(game.resolve("mods/remove.txt")));
            assertEquals("keep", read(game.resolve("mods/keep.txt")));
            assertFalse(Files.exists(game.resolve(".mods.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void emptyModsListIsSafeButRemovalRequiresExistingFolder() throws Exception {
        Path root = Files.createTempDirectory("mod-manager-remove-empty-test");
        try {
            Path game = game(root);
            assertTrue(ModManager.list(game, Paths.get("")).isEmpty());
            try {
                ModManager.list(game, Paths.get(".."));
                fail("Accepted an escaping browse path");
            } catch (IOException expected) { /* Paths outside mods are rejected. */ }
            try {
                ModManager.removeSelected(game, Paths.get(""), List.of("missing.txt"));
                fail("Accepted a removal without mods");
            } catch (IOException expected) { /* Nothing to remove. */ }
        } finally { SafeTar.deleteTree(root); }
    }

    private static Path game(Path root) throws IOException {
        Path game = root.resolve("game");
        Files.createDirectories(game);
        write(game.resolve("pokewilds.jar"), "game");
        return game;
    }

    private static void write(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] archive(Map<String, String> files) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
