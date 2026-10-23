package local.pokewilds.standalone;

import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GameInstallerTest {
    @Test public void extractsOnlyGameDistributionAndSkipsSaveFolders() throws Exception {
        Path root = Files.createTempDirectory("game-installer-test");
        try {
            Path zip = archive(root, Map.of(
                "game/pokewilds.jar", "jar",
                "game/README.txt", "readme",
                "game/world.sav/game.json", "save"));
            Path stage = root.resolve("stage"); Files.createDirectory(stage);
            GameInstaller.extractArchive(zip, stage, "game", ignored -> {}, () -> false);
            assertEquals("jar", new String(Files.readAllBytes(stage.resolve("pokewilds.jar")), StandardCharsets.UTF_8));
            assertFalse(Files.exists(stage.resolve("world.sav")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsTraversalAndUnexpectedRoots() throws Exception {
        for (String entry : new String[]{"game/../outside", "other/pokewilds.jar", "game/a/../../outside"}) {
            Path root = Files.createTempDirectory("game-installer-invalid-test");
            try {
                Path zip = archive(root, Map.of(entry, "bad"));
                Path stage = root.resolve("stage"); Files.createDirectory(stage);
                try {
                    GameInstaller.extractArchive(zip, stage, "game", ignored -> {}, () -> false);
                    fail("Accepted " + entry);
                } catch (IOException expected) { /* rejected */ }
                assertFalse(Files.exists(root.resolve("outside")));
            } finally { SafeTar.deleteTree(root); }
        }
    }

    @Test public void cancellationLeavesNoExtractedFile() throws Exception {
        Path root = Files.createTempDirectory("game-installer-cancel-test");
        try {
            Path zip = archive(root, Map.of("game/pokewilds.jar", "jar"));
            Path stage = root.resolve("stage"); Files.createDirectory(stage);
            try {
                GameInstaller.extractArchive(zip, stage, "game", ignored -> {}, () -> true);
                fail("Expected cancellation");
            } catch (IOException expected) { /* caller removes stage */ }
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void extractsPinnedReleaseWhenBuildCacheIsAvailable() throws Exception {
        Path archive = Paths.get("../runtime/cache/game-v0.8.11");
        Assume.assumeTrue(Files.isRegularFile(archive));
        Path root = Files.createTempDirectory("official-game-installer-test");
        try {
            GameInstaller.extractArchive(archive, root, "pokewilds-v0.8.11-otherplatforms",
                ignored -> {}, () -> false);
            assertTrue(Files.size(root.resolve("pokewilds.jar")) > 100_000_000);
            assertTrue(Files.isRegularFile(root.resolve("README.txt")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void activationPreservesUserSettingsModsAndWorlds() throws Exception {
        Path root = Files.createTempDirectory("game-installer-activation-test");
        try {
            Path old = root.resolve("game"), stage = root.resolve("game.preparing");
            Files.createDirectories(old.resolve("mods"));
            Files.createDirectories(old.resolve("first.sav"));
            Files.createDirectories(stage.resolve("mods"));
            Files.write(old.resolve("settings.txt"), "custom".getBytes(StandardCharsets.UTF_8));
            Files.write(old.resolve("mods/custom.mod"), "mod".getBytes(StandardCharsets.UTF_8));
            Files.write(old.resolve("first.sav/game.json"), "world".getBytes(StandardCharsets.UTF_8));
            Files.write(stage.resolve("pokewilds.jar"), "new jar".getBytes(StandardCharsets.UTF_8));
            Files.write(stage.resolve("settings.txt"), "default".getBytes(StandardCharsets.UTF_8));
            GameInstaller.preserveUserData(old, stage);
            GameInstaller.activate(old, stage);
            assertEquals("custom", text(old.resolve("settings.txt")));
            assertEquals("mod", text(old.resolve("mods/custom.mod")));
            assertEquals("world", text(old.resolve("first.sav/game.json")));
            assertEquals("new jar", text(old.resolve("pokewilds.jar")));
            assertFalse(Files.exists(root.resolve("game.previous")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void interruptedActivationRestoresPreviousGame() throws Exception {
        Path root = Files.createTempDirectory("game-installer-recovery-test");
        try {
            Path previous = root.resolve("game.previous");
            Files.createDirectory(previous);
            Files.write(previous.resolve("pokewilds.jar"), "old".getBytes(StandardCharsets.UTF_8));
            Path game = root.resolve("game");
            GameInstaller.recover(game);
            assertEquals("old", text(game.resolve("pokewilds.jar")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void restartRemovesInterruptedDownloadWithoutDeletingGame() throws Exception {
        Path root = Files.createTempDirectory("game-installer-cleanup-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectory(game);
            Files.write(game.resolve("pokewilds.jar"), "installed".getBytes(StandardCharsets.UTF_8));
            Files.write(root.resolve("game-download.part"), "partial ZIP".getBytes(StandardCharsets.UTF_8));
            Path stage = root.resolve("game.preparing");
            Files.createDirectory(stage);
            Files.write(stage.resolve("pokewilds.jar"), "partial game".getBytes(StandardCharsets.UTF_8));

            GameInstaller.discardInterruptedFiles(root);

            assertFalse(Files.exists(root.resolve("game-download.part")));
            assertFalse(Files.exists(stage));
            assertEquals("installed", text(game.resolve("pokewilds.jar")));
        } finally { SafeTar.deleteTree(root); }
    }

    private static String text(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path archive(Path root, Map<String, String> entries) throws IOException {
        Path zip = root.resolve("game.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, String> item : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(item.getKey()));
                out.write(item.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return zip;
    }
}
