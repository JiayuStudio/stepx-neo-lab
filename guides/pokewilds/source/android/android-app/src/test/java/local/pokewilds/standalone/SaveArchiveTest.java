package local.pokewilds.standalone;

import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.zip.*;

public class SaveArchiveTest {
    @Test public void importsNativeWorldZipWithoutAppManifest() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            SaveArchive.importFrom(new ByteArrayInputStream(nativeWorldZip()), game);
            assertTrue(Files.isRegularFile(game.resolve("world.sav/game.json.zip")));
            assertTrue(Files.isRegularFile(game.resolve("world.sav/map001.json.zip")));
            assertTrue(Files.isRegularFile(game.resolve("world.sav/spawnplayer001.json.zip")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void importsSelectedWorldFolderFiles() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            java.util.Map<String, SaveArchive.InputOpener> files = new java.util.LinkedHashMap<>();
            files.put("game.json.zip", () -> new ByteArrayInputStream(jsonSaveBytes("game")));
            files.put("map001.json.zip", () -> new ByteArrayInputStream(jsonSaveBytes("map")));
            files.put("spawnplayer001.json.zip", () -> new ByteArrayInputStream(jsonSaveBytes("spawn")));
            files.put("map001.png", () -> new ByteArrayInputStream(bytes("image")));
            SaveArchive.importWorldFiles("world.sav", files, game);
            assertEquals("image", text(game.resolve("world.sav/map001.png")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void folderImportCollisionKeepsExistingWorldAndRemovesTemporaryZip() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            createWorld(game.resolve("world.sav"));
            byte[] oldMap = Files.readAllBytes(game.resolve("world.sav/map001.json.zip"));
            java.util.Map<String, SaveArchive.InputOpener> files = new java.util.LinkedHashMap<>();
            files.put("game.json.zip", () -> new ByteArrayInputStream(jsonSaveBytes("other")));
            files.put("map001.json.zip", () -> new ByteArrayInputStream(jsonSaveBytes("other")));
            files.put("spawnplayer001.json.zip", () -> new ByteArrayInputStream(jsonSaveBytes("other")));
            try {
                SaveArchive.importWorldFiles("world.sav", files, game);
                fail("world collision");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("Already exists"));
            }
            assertArrayEquals(oldMap, Files.readAllBytes(game.resolve("world.sav/map001.json.zip")));
            assertFalse(Files.exists(root.resolve("save-folder-import.preparing.zip")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsManifestlessArchiveWithMultipleWorlds() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                putWorld(zip, "first.sav");
                putWorld(zip, "second.sav");
            }
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(bytes.toByteArray()), root.resolve("game"));
                fail("multiple worlds");
            } catch (IOException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("one .sav world folder"));
            }
            assertFalse(Files.exists(root.resolve("game/first.sav")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void roundTripPreservesWorldSettingsAndExcludesJar() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("source");
            createWorld(game.resolve("world.sav"));
            Files.write(game.resolve("settings.txt"), "source-settings".getBytes(StandardCharsets.UTF_8));
            Files.createDirectories(game.resolve("mods"));
            Files.write(game.resolve("mods/bundled.mod"), bytes("bundled"));
            Files.write(game.resolve("mods/new.mod"), bytes("new"));
            Files.createDirectories(game.resolve("mods/nested"));
            Files.write(game.resolve("mods/nested/child.mod"), bytes("child"));
            Files.write(game.resolve("pokewilds.jar"), new byte[]{7});
            byte[] mapSave = Files.readAllBytes(game.resolve("world.sav/map001.json.zip"));
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            SaveArchive.exportTo(game, data);

            Path restored = root.resolve("restored");
            Files.createDirectories(restored);
            Files.write(restored.resolve("settings.txt"), "existing-settings".getBytes(StandardCharsets.UTF_8));
            Files.createDirectories(restored.resolve("mods"));
            Files.write(restored.resolve("mods/bundled.mod"), bytes("bundled"));
            SaveArchive.importFrom(new ByteArrayInputStream(data.toByteArray()), restored);
            assertArrayEquals(mapSave, Files.readAllBytes(restored.resolve("world.sav/map001.json.zip")));
            assertEquals("existing-settings", new String(Files.readAllBytes(restored.resolve("settings.txt")), StandardCharsets.UTF_8));
            assertEquals("bundled", text(restored.resolve("mods/bundled.mod")));
            assertEquals("new", text(restored.resolve("mods/new.mod")));
            assertEquals("child", text(restored.resolve("mods/nested/child.mod")));
            Path fresh = root.resolve("fresh");
            SaveArchive.importFrom(new ByteArrayInputStream(data.toByteArray()), fresh);
            assertEquals("new", text(fresh.resolve("mods/new.mod")));
            assertEquals("child", text(fresh.resolve("mods/nested/child.mod")));
            assertFalse(Files.exists(restored.resolve("pokewilds.jar")));
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(data.toByteArray()), restored);
                fail("collision");
            } catch (IOException expected) { }
            assertArrayEquals(mapSave, Files.readAllBytes(restored.resolve("world.sav/map001.json.zip")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsDifferingModWithoutPartialWorldImport() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectories(game.resolve("mods"));
            Files.write(game.resolve("mods/conflict.mod"), bytes("installed"));
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(archiveWithMods("conflict.mod", "archive")), game);
                fail("mod conflict");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("mods/conflict.mod"));
            }
            assertEquals("installed", text(game.resolve("mods/conflict.mod")));
            assertFalse(Files.exists(game.resolve("world.sav")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsExistingModSymlinkWithoutWritingThroughIt() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Path outside = root.resolve("outside.mod");
            Files.createDirectories(game.resolve("mods"));
            Files.write(outside, bytes("outside"));
            try {
                Files.createSymbolicLink(game.resolve("mods/escape.mod"), outside);
            } catch (UnsupportedOperationException | IOException unavailable) {
                Assume.assumeNoException(unavailable);
            }
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(archiveWithMods("escape.mod", "archive")), game);
                fail("symlink");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("symlink"));
            }
            assertEquals("outside", text(outside));
            assertFalse(Files.exists(game.resolve("world.sav")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsUnsupportedManifestWithoutChangingTarget() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectories(game);
            Files.write(game.resolve("settings.txt"), "keep".getBytes(StandardCharsets.UTF_8));
            byte[] archive = archiveWithManifest("{\"schema\":2,\"game_version\":\"0.8.11\"}", true);
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(archive), game);
                fail("unsupported manifest");
            } catch (IOException expected) { }
            assertEquals("keep", new String(Files.readAllBytes(game.resolve("settings.txt")), StandardCharsets.UTF_8));
            assertFalse(Files.exists(game.resolve("world.sav")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsEmptyWorldWithoutChangingTarget() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectories(game);
            Files.write(game.resolve("settings.txt"), "keep".getBytes(StandardCharsets.UTF_8));
            byte[] archive = archiveWithManifest("{\"schema\":1,\"game_version\":\"0.8.11\"}", false);
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(archive), game);
                fail("empty world");
            } catch (IOException expected) { }
            assertEquals("keep", new String(Files.readAllBytes(game.resolve("settings.txt")), StandardCharsets.UTF_8));
            assertFalse(Files.exists(game.resolve("empty.sav")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsTrailingOrDuplicateManifestWithoutChangingTarget() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectories(game);
            Files.write(game.resolve("settings.txt"), "keep".getBytes(StandardCharsets.UTF_8));
            String[] manifests = {
                "{\"schema\":1,\"game_version\":\"0.8.11\"} trailing",
                "{\"schema\":1,\"schema\":1,\"game_version\":\"0.8.11\"}"
            };
            for (String manifest : manifests) {
                try {
                    SaveArchive.importFrom(new ByteArrayInputStream(archiveWithManifest(manifest, true)), game);
                    fail("malformed manifest");
                } catch (IOException expected) { }
                assertEquals("keep", new String(Files.readAllBytes(game.resolve("settings.txt")), StandardCharsets.UTF_8));
                assertFalse(Files.exists(game.resolve("world.sav")));
            }
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void traversalRejectedWithoutChangingData() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream z = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                putEntry(z, "pokewilds-save-manifest.json", "{\"schema\":1,\"game_version\":\"0.8.11\"}");
                putEntry(z, "world.sav/../../escape", "1");
            }
            try {
                SaveArchive.importFrom(new ByteArrayInputStream(bytes.toByteArray()), root.resolve("game"));
                fail("traversal");
            } catch (IOException expected) { }
            assertFalse(Files.exists(root.resolve("escape")));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void rejectsBackupDirectoriesAndNestedEntries() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            for (String entry : new String[]{"backup.sav.zip/", "backup.sav.zip/member"}) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                    putEntry(zip, "pokewilds-save-manifest.json", "{\"schema\":1,\"game_version\":\"0.8.11\"}");
                    putEntry(zip, entry, entry.endsWith("/") ? null : "not-a-backup");
                }
                Path game = root.resolve(entry.endsWith("/") ? "directory-game" : "nested-game");
                try {
                    SaveArchive.importFrom(new ByteArrayInputStream(bytes.toByteArray()), game);
                    fail("backup path accepted: " + entry);
                } catch (IOException expected) {
                    assertTrue(expected.getMessage().contains("backup"));
                }
                assertFalse(Files.exists(game));
            }
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void recoversInterruptedActivationWithoutRemovingExistingData() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectories(game.resolve("mods"));
            Files.write(game.resolve("mods/bundled.mod"), bytes("bundled"));
            Path stage = game.resolveSibling("save-import.preparing");
            Files.createDirectories(stage.resolve("mods"));
            Files.write(stage.resolve("mods/new.mod"), bytes("new"));
            Files.createDirectories(stage.resolve("world.sav"));
            Files.write(stage.resolve("world.sav/game.json.zip"), bytes("world"));
            Files.move(stage.resolve("mods/new.mod"), game.resolve("mods/new.mod"));
            Files.move(stage.resolve("world.sav"), game.resolve("world.sav"));
            writeJournal(stage.resolve(SaveArchive.ACTIVATION_JOURNAL), false,
                new String[]{"mods/new.mod"}, new String[0], new String[]{"world.sav"});

            SaveArchive.recoverInterruptedImport(game);

            assertEquals("bundled", text(game.resolve("mods/bundled.mod")));
            assertFalse(Files.exists(game.resolve("mods/new.mod")));
            assertFalse(Files.exists(game.resolve("world.sav")));
            assertFalse(Files.exists(stage));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void committedActivationSurvivesJournalCleanup() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Files.createDirectories(game.resolve("world.sav"));
            Files.write(game.resolve("world.sav/game.json.zip"), bytes("world"));
            Path stage = game.resolveSibling("save-import.preparing");
            Files.createDirectories(stage);
            writeJournal(stage.resolve(SaveArchive.ACTIVATION_JOURNAL), true,
                new String[0], new String[0], new String[]{"world.sav"});

            SaveArchive.recoverInterruptedImport(game);

            assertEquals("world", text(game.resolve("world.sav/game.json.zip")));
            assertFalse(Files.exists(stage));
        } finally { SafeTar.deleteTree(root); }
    }

    @Test public void discardsExtractionStageWithoutActivationJournal() throws Exception {
        Path root = Files.createTempDirectory("save-test");
        try {
            Path game = root.resolve("game");
            Path stage = game.resolveSibling("save-import.preparing");
            Files.createDirectories(stage.resolve("world.sav"));
            Files.write(stage.resolve("world.sav/partial.bin"), bytes("partial"));
            Files.write(stage.resolve(SaveArchive.ACTIVATION_JOURNAL + ".tmp"), bytes("partial journal"));

            SaveArchive.recoverInterruptedImport(game);

            assertFalse(Files.exists(stage));
            assertFalse(Files.exists(game));
        } finally { SafeTar.deleteTree(root); }
    }

    private static void createWorld(Path world) throws IOException {
        Files.createDirectories(world);
        writeJsonSave(world.resolve("game.json.zip"), "game");
        writeJsonSave(world.resolve("map001.json.zip"), "map");
        writeJsonSave(world.resolve("spawnplayer001.json.zip"), "spawn");
        Files.write(world.resolve("map001.png"), new byte[]{4, 5});
    }

    private static void writeJsonSave(Path output, String data) throws IOException {
        try (OutputStream stream = Files.newOutputStream(output);
             ZipOutputStream zip = new ZipOutputStream(stream, StandardCharsets.UTF_8)) {
            putEntry(zip, "data.json", data);
        }
    }

    private static String text(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static void writeJournal(Path path, boolean committed, String[] files,
                                     String[] directories, String[] trees) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             DataOutputStream output = new DataOutputStream(java.nio.channels.Channels.newOutputStream(channel))) {
            output.writeInt(SaveArchive.JOURNAL_MAGIC);
            output.writeInt(SaveArchive.JOURNAL_VERSION);
            output.writeByte(committed ? 1 : 0);
            writeJournalPaths(output, files);
            writeJournalPaths(output, directories);
            writeJournalPaths(output, trees);
            output.flush();
        }
    }

    private static void writeJournalPaths(DataOutputStream output, String[] paths) throws IOException {
        output.writeInt(paths.length);
        for (String path : paths) output.writeUTF(path);
    }

    private static byte[] archiveWithMods(String modPath, String modData) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            putEntry(zip, "pokewilds-save-manifest.json", "{\"schema\":1,\"game_version\":\"0.8.11\"}");
            putEntry(zip, "world.sav/", (byte[]) null);
            putEntry(zip, "world.sav/game.json.zip", jsonSaveBytes("game"));
            putEntry(zip, "world.sav/map001.json.zip", jsonSaveBytes("map"));
            putEntry(zip, "world.sav/spawnplayer001.json.zip", jsonSaveBytes("spawn"));
            putEntry(zip, "mods/", (byte[]) null);
            putEntry(zip, "mods/" + modPath, modData);
        }
        return bytes.toByteArray();
    }

    private static byte[] archiveWithManifest(String manifest, boolean validWorld) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            putEntry(zip, "pokewilds-save-manifest.json", manifest);
            if (validWorld) {
                putEntry(zip, "world.sav/", (byte[]) null);
                putEntry(zip, "world.sav/game.json.zip", jsonSaveBytes("game"));
                putEntry(zip, "world.sav/map001.json.zip", jsonSaveBytes("map"));
                putEntry(zip, "world.sav/spawnplayer001.json.zip", jsonSaveBytes("spawn"));
            } else {
                putEntry(zip, "empty.sav/", (byte[]) null);
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] jsonSaveBytes(String data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            putEntry(zip, "data.json", data);
        }
        return bytes.toByteArray();
    }

    private static byte[] nativeWorldZip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            putWorld(zip, "world.sav");
        }
        return bytes.toByteArray();
    }

    private static void putWorld(ZipOutputStream zip, String name) throws IOException {
        putEntry(zip, name + "/", (byte[]) null);
        putEntry(zip, name + "/game.json.zip", jsonSaveBytes("game"));
        putEntry(zip, name + "/map001.json.zip", jsonSaveBytes("map"));
        putEntry(zip, name + "/spawnplayer001.json.zip", jsonSaveBytes("spawn"));
    }

    private static void putEntry(ZipOutputStream zip, String name, String data) throws IOException {
        putEntry(zip, name, data == null ? null : data.getBytes(StandardCharsets.UTF_8));
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        if (data != null) zip.write(data);
        zip.closeEntry();
    }
}
