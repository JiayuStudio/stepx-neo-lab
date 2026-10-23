package local.pokewilds.standalone;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Copies current 0.8.11 JSON-in-zip save data; it never imports game files or another app. */
public final class SaveArchive {
    private static final long LIMIT = 2L * 1024 * 1024 * 1024;
    private static final int MANIFEST_LIMIT = 16 * 1024;
    private static final String MANIFEST = "pokewilds-save-manifest.json";
    private static final int MANIFEST_SCHEMA = 1;
    private static final String GAME_VERSION = "0.8.11";
    private static final String PREPARING = "save-import.preparing";
    private static final String FOLDER_ARCHIVE = "save-folder-import.preparing.zip";
    static final String ACTIVATION_JOURNAL = ".activation-journal";
    static final int JOURNAL_MAGIC = 0x504B534A;
    static final int JOURNAL_VERSION = 1;
    private static final int JOURNAL_LIMIT = 1024 * 1024;
    private static final int JOURNAL_ENTRY_LIMIT = 100000;

    private SaveArchive() {}

    @FunctionalInterface
    interface InputOpener {
        InputStream open() throws IOException;
    }

    /** Imports the direct files of a selected PokeWilds .sav folder. */
    static void importWorldFiles(String worldName, Map<String, InputOpener> files, Path game) throws IOException {
        if (!safeName(worldName) || !worldName.endsWith(".sav") || worldName.endsWith(".sav.zip")) {
            throw new IOException("Choose a PokeWilds .sav world folder");
        }
        if (files.isEmpty()) throw new IOException("Selected world folder is empty");
        Path archive = game.resolveSibling(FOLDER_ARCHIVE);
        Files.deleteIfExists(archive);
        try {
            try (ZipOutputStream zip = new ZipOutputStream(
                    Files.newOutputStream(archive, StandardOpenOption.CREATE_NEW), StandardCharsets.UTF_8)) {
                int count = 0;
                long size = 0;
                byte[] buffer = new byte[65536];
                for (Map.Entry<String, InputOpener> file : files.entrySet()) {
                    String name = file.getKey();
                    if (!safeName(name) || ++count > 100000) throw new IOException("Invalid world folder file name or count");
                    zip.putNextEntry(new ZipEntry(worldName + "/" + name));
                    try (InputStream input = file.getValue().open()) {
                        if (input == null) throw new IOException("Could not read world file: " + name);
                        int n;
                        while ((n = input.read(buffer)) != -1) {
                            size += n;
                            if (size > LIMIT) throw new IOException("World folder is too large");
                            zip.write(buffer, 0, n);
                        }
                    }
                    zip.closeEntry();
                }
            }
            try (InputStream input = Files.newInputStream(archive)) {
                importFrom(input, game);
            }
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    static void discardInterruptedFolderArchive(Path game) throws IOException {
        Files.deleteIfExists(game.resolveSibling(FOLDER_ARCHIVE));
    }

    private static boolean safeName(String name) {
        return name != null && !name.isEmpty() && !name.equals(".") && !name.equals("..")
            && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf('\0') < 0;
    }

    private static boolean allowed(String name) {
        String top = name.split("/", 2)[0];
        return top.equals("settings.txt") || top.equals("mods") || top.endsWith(".sav") || top.endsWith(".sav.zip");
    }

    public static void exportTo(Path game, OutputStream destination) throws IOException {
        recoverInterruptedImport(game);
        validateExportRoot(game);
        try (ZipOutputStream zip = new ZipOutputStream(destination, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(manifestBytes());
            zip.closeEntry();
            Files.walkFileTree(game, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path p, java.nio.file.attribute.BasicFileAttributes a) {
                    return p.equals(game) || allowed(game.relativize(p).toString()) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                }
                public FileVisitResult visitFile(Path p, java.nio.file.attribute.BasicFileAttributes a) throws IOException {
                    String name = game.relativize(p).toString().replace(File.separatorChar, '/');
                    if (allowed(name) && a.isRegularFile()) {
                        zip.putNextEntry(new ZipEntry(name));
                        Files.copy(p, zip);
                        zip.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public static void importFrom(InputStream source, Path game) throws IOException {
        recoverInterruptedImport(game);
        Path stage = game.resolveSibling(PREPARING);
        SafeTar.deleteTree(stage);
        Files.createDirectories(stage);
        Set<String> tops = new LinkedHashSet<>();
        Set<Path> seen = new HashSet<>();
        long size = 0;
        int count = 0;
        boolean manifestSeen = false;
        boolean preserveStage = false;
        try {
            try (ZipInputStream zip = new ZipInputStream(source, StandardCharsets.UTF_8)) {
                ZipEntry e;
                byte[] buffer = new byte[65536];
                while ((e = zip.getNextEntry()) != null) {
                    if (++count > 100000) throw new IOException("Too many save archive entries");
                    String name = e.getName();
                    if (MANIFEST.equals(name)) {
                        if (manifestSeen || e.isDirectory()) throw new IOException("Invalid save archive manifest");
                        validateManifest(readBounded(zip, MANIFEST_LIMIT));
                        manifestSeen = true;
                        zip.closeEntry();
                        continue;
                    }
                    Path relative;
                    try { relative = Paths.get(name); }
                    catch (InvalidPathException error) { throw new IOException("Unsupported save archive path: " + name, error); }
                    Path out = stage.resolve(relative).normalize();
                    if (relative.isAbsolute() || name.contains("\\") || !out.startsWith(stage) || !allowed(name)) {
                        if ("data.json".equals(name)) {
                            throw new IOException("Choose the complete .sav folder or its ZIP, not one JSON ZIP file");
                        }
                        throw new IOException("Unsupported save archive path: " + name);
                    }
                    if (relative.getNameCount() == 0) throw new IOException("Empty save archive path");
                    String top = relative.getName(0).toString();
                    if (!out.startsWith(stage.resolve(top))) {
                        throw new IOException("Unsupported save archive path: " + name);
                    }
                    if (top.endsWith(".sav.zip") && (e.isDirectory() || relative.getNameCount() != 1)) {
                        throw new IOException("Invalid world backup entry: " + name);
                    }
                    if (top.equals("settings.txt") && (e.isDirectory() || relative.getNameCount() != 1)) {
                        throw new IOException("Invalid settings entry");
                    }
                    if (!seen.add(out)) throw new IOException("Duplicate save archive path");
                    tops.add(top);
                    if (e.isDirectory()) {
                        Files.createDirectories(out);
                        zip.closeEntry();
                        continue;
                    }
                    if (out.getParent() == null) throw new IOException("Invalid save archive path: " + name);
                    Files.createDirectories(out.getParent());
                    try (OutputStream output = Files.newOutputStream(out, StandardOpenOption.CREATE_NEW)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            size += n;
                            if (size > LIMIT) throw new IOException("Save archive is too large");
                            output.write(buffer, 0, n);
                        }
                    }
                    zip.closeEntry();
                }
            }
            if (!manifestSeen && (tops.size() != 1 || !tops.iterator().next().endsWith(".sav")
                    || tops.iterator().next().endsWith(".sav.zip"))) {
                throw new IOException("A ZIP without an app manifest must contain one .sav world folder");
            }
            if (tops.isEmpty()) throw new IOException("Archive contains no saves or settings");
            for (String top : tops) {
                if (top.endsWith(".sav") && !top.endsWith(".sav.zip")) validateWorldFolder(stage.resolve(top), top);
                if (top.endsWith(".sav.zip") && !Files.isRegularFile(stage.resolve(top), LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("World backup is not a file: " + top);
                }
            }
            ensureGameDirectory(game);
            // Preserve existing controls/settings; importing a world must not reset them.
            if (Files.isSymbolicLink(game.resolve("settings.txt"))) {
                throw new IOException("Unsafe existing settings symlink: settings.txt");
            }
            if (Files.exists(game.resolve("settings.txt"), LinkOption.NOFOLLOW_LINKS)) {
                tops.remove("settings.txt");
                Files.deleteIfExists(stage.resolve("settings.txt"));
            }
            if (tops.isEmpty()) throw new IOException("No new worlds or mods in archive; existing settings were preserved");
            for (String top : tops) {
                if (top.equals("mods")) {
                    validateExistingMods(game.resolve("mods"));
                } else if (existsNoFollow(game.resolve(top))) {
                    throw new IOException("Already exists: " + top + ". Import into a fresh game data directory or rename the world in the archive.");
                }
            }
            ModMergePlan modPlan = new ModMergePlan();
            if (tops.contains("mods")) collectModPlan(stage.resolve("mods"), game.resolve("mods"), modPlan);
            ActivationJournal journal = new ActivationJournal();
            for (String top : tops) {
                if (top.equals("mods")) {
                    if (modPlan.createRoot) journal.directories.add(Paths.get("mods"));
                    for (Path relative : modPlan.files) journal.files.add(Paths.get("mods").resolve(relative));
                    for (Path relative : modPlan.directories) journal.directories.add(Paths.get("mods").resolve(relative));
                } else if (top.endsWith(".sav") && !top.endsWith(".sav.zip")) {
                    journal.trees.add(Paths.get(top));
                } else {
                    journal.files.add(Paths.get(top));
                }
            }
            writeJournal(stage.resolve(ACTIVATION_JOURNAL), journal, false);
            try {
                for (String top : tops) {
                    if (top.equals("mods")) {
                        applyModPlan(stage.resolve(top), game.resolve(top), modPlan);
                    } else {
                        Files.move(stage.resolve(top), game.resolve(top));
                    }
                }
                writeJournal(stage.resolve(ACTIVATION_JOURNAL), journal, true);
            } catch (IOException error) {
                if (!rollbackJournal(game, journal, error)) preserveStage = true;
                throw error;
            }
        } finally {
            if (!preserveStage) SafeTar.deleteTree(stage);
        }
    }

    private static void ensureGameDirectory(Path game) throws IOException {
        if (Files.isSymbolicLink(game)) throw new IOException("Unsafe game directory symlink");
        if (Files.exists(game, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(game, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Game save path is not a directory");
            }
        } else {
            Files.createDirectories(game);
        }
    }

    private static boolean existsNoFollow(Path path) {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path);
    }

    private static final class ModMergePlan {
        private final List<Path> files = new ArrayList<>();
        private final List<Path> directories = new ArrayList<>();
        private boolean createRoot;
    }

    private static final class ActivationJournal {
        private boolean committed;
        private final List<Path> files = new ArrayList<>();
        private final List<Path> directories = new ArrayList<>();
        private final List<Path> trees = new ArrayList<>();
    }

    private static void validateExistingMods(Path mods) throws IOException {
        if (!existsNoFollow(mods)) return;
        if (Files.isSymbolicLink(mods) || !Files.isDirectory(mods, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Unsafe existing mods path: mods");
        }
        Files.walkFileTree(mods, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path path, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isSymbolicLink(path)) throw unsafeModsPath(mods, path);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path path, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isSymbolicLink(path)) throw unsafeModsPath(mods, path);
                if (!attrs.isRegularFile()) throw new IOException("Invalid existing mods entry: " + mods.relativize(path));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path path, IOException error) throws IOException {
                throw new IOException("Cannot inspect existing mods path: " + mods.relativize(path), error);
            }
        });
    }

    private static IOException unsafeModsPath(Path mods, Path path) {
        return new IOException("Unsafe existing mods symlink: mods/" + mods.relativize(path));
    }

    private static void collectModPlan(Path source, Path target, ModMergePlan plan) throws IOException {
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Invalid mods archive entry");
        }
        if (!existsNoFollow(target)) {
            plan.createRoot = true;
        } else if (Files.isSymbolicLink(target) || !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Mods path is not a directory: mods");
        }
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path path, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                if (path.equals(source)) return FileVisitResult.CONTINUE;
                Path destination = target.resolve(source.relativize(path));
                if (!existsNoFollow(destination)) {
                    plan.directories.add(source.relativize(path));
                } else {
                    ensureModDirectory(destination, source.relativize(path).toString());
                }
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path path, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isSymbolicLink(path) || !attrs.isRegularFile()) {
                    throw new IOException("Invalid mods archive entry: mods/" + source.relativize(path));
                }
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                ensureModParentForPlan(destination.getParent(), relative, target, plan);
                if (existsNoFollow(destination)) {
                    if (Files.isSymbolicLink(destination)) {
                        throw new IOException("Unsafe existing mods symlink: mods/" + relative);
                    }
                    if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Mod conflict at mods/" + relative + ": existing entry is not a file");
                    }
                    if (!sameFileContent(path, destination)) {
                        throw new IOException("Mod conflict at mods/" + relative + ": existing file differs from imported file");
                    }
                    return FileVisitResult.CONTINUE;
                }
                plan.files.add(relative);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFileFailed(Path path, IOException error) throws IOException {
                throw new IOException("Cannot read mods archive entry: mods/" + source.relativize(path), error);
            }
        });
    }

    private static void applyModPlan(Path source, Path target, ModMergePlan plan) throws IOException {
        if (plan.createRoot) {
            if (existsNoFollow(target)) throw new IOException("Mods path appeared during import: mods");
            Files.createDirectory(target);
        } else if (Files.isSymbolicLink(target) || !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Mods path is not a directory: mods");
        }
        for (Path relative : plan.directories) {
            Path destination = target.resolve(relative);
            if (existsNoFollow(destination)) {
                throw new IOException("Mod path appeared during import: mods/" + relative);
            }
            Files.createDirectory(destination);
        }
        for (Path relative : plan.files) {
            Path sourceFile = source.resolve(relative);
            Path destination = target.resolve(relative);
            ensureModParent(destination.getParent(), relative);
            if (existsNoFollow(destination)) {
                throw new IOException("Mod path appeared during import: mods/" + relative);
            }
            Files.move(sourceFile, destination);
        }
    }

    private static void ensureModParentForPlan(Path parent, Path relative, Path target,
                                               ModMergePlan plan) throws IOException {
        if (parent == null) throw new IOException("Invalid mods archive path");
        if (!existsNoFollow(parent)) {
            Path parentRelative = target.relativize(parent);
            if (plan.createRoot && parent.equals(target)) return;
            if (plan.directories.contains(parentRelative)) return;
        }
        ensureModParent(parent, relative);
    }

    private static void ensureModDirectory(Path directory, String relative) throws IOException {
        if (!existsNoFollow(directory)) {
            return;
        }
        if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Mod conflict at mods/" + relative + ": existing entry is not a directory");
        }
    }

    private static void ensureModParent(Path parent, Path relative) throws IOException {
        if (parent == null) throw new IOException("Invalid mods archive path");
        // walkFileTree visits every archive directory before its files, so this
        // only needs to verify that an existing parent was not replaced by a link.
        if (Files.isSymbolicLink(parent)) {
            throw new IOException("Unsafe existing mods symlink: mods/" + relative);
        }
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Mod conflict at mods/" + relative + ": parent is not a directory");
        }
    }

    private static boolean sameFileContent(Path first, Path second) throws IOException {
        if (Files.size(first) != Files.size(second)) return false;
        try (InputStream a = Files.newInputStream(first); InputStream b = Files.newInputStream(second)) {
            byte[] left = new byte[65536];
            byte[] right = new byte[65536];
            int n;
            while ((n = a.read(left)) != -1) {
                int offset = 0;
                while (offset < n) {
                    int m = b.read(right, offset, n - offset);
                    if (m == -1) return false;
                    for (int i = 0; i < m; i++) {
                        if (left[offset + i] != right[offset + i]) return false;
                    }
                    offset += m;
                }
            }
            return b.read() == -1;
        }
    }

    /** Removes an interrupted activation, or just its staging tree after commit. */
    public static void recoverInterruptedImport(Path game) throws IOException {
        Path stage = game.resolveSibling(PREPARING);
        if (!Files.exists(stage, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(stage) || !Files.isDirectory(stage, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Unsafe interrupted import staging path");
        }
        Path journalPath = stage.resolve(ACTIVATION_JOURNAL);
        if (Files.isSymbolicLink(journalPath)) {
            throw new IOException("Interrupted save import journal is missing or unsafe");
        }
        if (!Files.exists(journalPath, LinkOption.NOFOLLOW_LINKS)) {
            // The importer writes this journal before its first activation move.
            // No journal therefore means extraction stopped before activation.
            SafeTar.deleteTree(stage);
            return;
        }
        if (!Files.isRegularFile(journalPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Interrupted save import journal is missing or unsafe");
        }
        ActivationJournal journal = readJournal(journalPath);
        validateJournal(game, journal);
        if (journal.committed) {
            SafeTar.deleteTree(stage);
            return;
        }
        IOException rollbackError = new IOException("Interrupted save import was rolled back");
        if (!rollbackJournal(game, journal, rollbackError)) {
            throw new IOException("Interrupted save import rollback failed; journal retained", rollbackError);
        }
        SafeTar.deleteTree(stage);
    }

    private static void writeJournal(Path path, ActivationJournal journal, boolean committed) throws IOException {
        int entries = journal.files.size() + journal.directories.size() + journal.trees.size();
        if (entries > JOURNAL_ENTRY_LIMIT) throw new IOException("Too many save import activation entries");
        Path temporary = path.resolveSibling(path.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temporary);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             DataOutputStream output = new DataOutputStream(Channels.newOutputStream(channel))) {
            output.writeInt(JOURNAL_MAGIC);
            output.writeInt(JOURNAL_VERSION);
            output.writeByte(committed ? 1 : 0);
            writeJournalPaths(output, journal.files);
            writeJournalPaths(output, journal.directories);
            writeJournalPaths(output, journal.trees);
            output.flush();
            channel.force(true);
        }
        if (Files.size(temporary) > JOURNAL_LIMIT) throw new IOException("Save import journal is too large");
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeJournalPaths(DataOutputStream output, List<Path> paths) throws IOException {
        output.writeInt(paths.size());
        for (Path path : paths) {
            String name = path.toString().replace(File.separatorChar, '/');
            if (name.length() == 0 || name.length() > 65535) throw new IOException("Invalid save import journal path");
            output.writeUTF(name);
        }
    }

    private static ActivationJournal readJournal(Path path) throws IOException {
        if (Files.size(path) > JOURNAL_LIMIT) throw new IOException("Save import journal is too large");
        ActivationJournal journal = new ActivationJournal();
        try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
            if (input.readInt() != JOURNAL_MAGIC || input.readInt() != JOURNAL_VERSION) {
                throw new IOException("Unsupported save import journal");
            }
            int state = input.readUnsignedByte();
            if (state != 0 && state != 1) throw new IOException("Malformed save import journal");
            journal.committed = state == 1;
            readJournalPaths(input, journal.files);
            readJournalPaths(input, journal.directories);
            readJournalPaths(input, journal.trees);
            if (input.read() != -1) throw new IOException("Malformed save import journal");
        } catch (EOFException | UTFDataFormatException error) {
            throw new IOException("Malformed save import journal", error);
        }
        return journal;
    }

    private static void readJournalPaths(DataInputStream input, List<Path> paths) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > JOURNAL_ENTRY_LIMIT || count > JOURNAL_LIMIT / 8) {
            throw new IOException("Invalid save import journal entry count");
        }
        for (int i = 0; i < count; i++) {
            try { paths.add(Paths.get(input.readUTF())); }
            catch (InvalidPathException error) { throw new IOException("Malformed save import journal", error); }
        }
    }

    private static void validateJournal(Path game, ActivationJournal journal) throws IOException {
        ensureGameDirectory(game);
        Set<String> seen = new HashSet<>();
        for (Path path : journal.files) validateJournalPath(game, path, 0, seen);
        for (Path path : journal.directories) validateJournalPath(game, path, 1, seen);
        for (Path path : journal.trees) validateJournalPath(game, path, 2, seen);
    }

    private static Path validateJournalPath(Path game, Path relative, int kind, Set<String> seen) throws IOException {
        String text = relative.toString();
        if (relative.isAbsolute() || relative.getNameCount() == 0 || text.contains("\\")
            || !relative.normalize().equals(relative) || !seen.add(text)) {
            throw new IOException("Unsafe save import journal path: " + text);
        }
        String top = relative.getName(0).toString();
        boolean valid;
        if (kind == 0) {
            valid = (top.equals("settings.txt") && relative.getNameCount() == 1)
                || (top.equals("mods") && relative.getNameCount() > 1)
                || (top.endsWith(".sav.zip") && relative.getNameCount() == 1);
        } else if (kind == 1) {
            valid = top.equals("mods");
        } else {
            valid = top.endsWith(".sav") && !top.endsWith(".sav.zip") && relative.getNameCount() == 1;
        }
        if (!valid) throw new IOException("Unsafe save import journal path: " + text);
        Path root = game.toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new IOException("Unsafe save import journal path: " + text);
        ensureNoSymlinkAncestors(root, target);
        if (Files.isSymbolicLink(target)) throw new IOException("Unsafe save import journal symlink: " + text);
        return target;
    }

    private static void ensureNoSymlinkAncestors(Path root, Path target) throws IOException {
        if (Files.isSymbolicLink(root)) throw new IOException("Unsafe game directory symlink");
        Path current = root;
        for (Path part : root.relativize(target)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) throw new IOException("Unsafe save import journal symlink: " + root.relativize(current));
        }
    }

    private static boolean rollbackJournal(Path game, ActivationJournal journal, IOException error) {
        boolean success = true;
        for (int i = journal.files.size() - 1; i >= 0; i--) {
            try { deleteJournalFile(game.resolve(journal.files.get(i)).normalize()); }
            catch (IOException rollback) { error.addSuppressed(rollback); success = false; }
        }
        for (int i = journal.trees.size() - 1; i >= 0; i--) {
            try { deleteJournalTree(game.resolve(journal.trees.get(i)).normalize()); }
            catch (IOException rollback) { error.addSuppressed(rollback); success = false; }
        }
        for (int i = journal.directories.size() - 1; i >= 0; i--) {
            try { deleteJournalDirectory(game.resolve(journal.directories.get(i)).normalize()); }
            catch (IOException rollback) { error.addSuppressed(rollback); success = false; }
        }
        return success;
    }

    private static void deleteJournalFile(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Unsafe interrupted import file: " + path);
        }
        Files.delete(path);
    }

    private static void deleteJournalDirectory(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(path) || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Unsafe interrupted import directory: " + path);
        }
        Files.delete(path);
    }

    private static void deleteJournalTree(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(path) || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Unsafe interrupted import world: " + path);
        }
        rejectSymlinks(path);
        SafeTar.deleteTree(path);
    }

    private static void rejectSymlinks(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path path, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isSymbolicLink(path)) throw new IOException("Unsafe interrupted import symlink: " + path);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path path, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isSymbolicLink(path)) throw new IOException("Unsafe interrupted import symlink: " + path);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void validateExportRoot(Path game) throws IOException {
        if (!Files.isDirectory(game, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Game save directory is missing");
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(game)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (name.endsWith(".sav") && !name.endsWith(".sav.zip")) {
                    if (!Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) throw new IOException("World is not a directory: " + name);
                    validateWorldFolder(entry, name);
                } else if (name.endsWith(".sav.zip") && !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("World backup is not a file: " + name);
                }
            }
        }
    }

    private static void validateWorldFolder(Path world, String name) throws IOException {
        if (!Files.isDirectory(world, LinkOption.NOFOLLOW_LINKS)) throw new IOException("World is not a directory: " + name);
        // PkmnMap.saveToFileNew in v0.8.11 writes these JSON-in-zip members.
        boolean gameData = false;
        boolean mapData = false;
        boolean spawnData = false;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(world)) {
            for (Path entry : entries) {
                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("World contains a non-file entry: " + name + "/" + entry.getFileName());
                }
                String file = entry.getFileName().toString();
                if (file.equals("game.json.zip")) {
                    validateJsonSave(entry);
                    gameData = true;
                } else if (file.startsWith("map") && file.endsWith(".json.zip")) {
                    validateJsonSave(entry);
                    mapData = true;
                } else if (file.startsWith("spawn") && file.endsWith(".json.zip")) {
                    validateJsonSave(entry);
                    spawnData = true;
                }
            }
        }
        if (!gameData || !mapData || !spawnData) {
            throw new IOException("World is empty or missing game/map/spawn JSON saves: " + name);
        }
    }

    private static void validateJsonSave(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry data = zip.getEntry("data.json");
            if (data == null || data.isDirectory() || data.getSize() == 0) {
                throw new IOException("Save archive is missing data.json: " + file.getFileName());
            }
            try (InputStream input = zip.getInputStream(data)) {
                int first;
                do { first = input.read(); }
                while (first != -1 && Character.isWhitespace((char) first));
                if (first == -1) throw new IOException("Save archive has empty data.json: " + file.getFileName());
            }
        } catch (ZipException error) {
            throw new IOException("Malformed save archive: " + file.getFileName(), error);
        }
    }

    private static byte[] manifestBytes() {
        return ("{\"schema\":1,\"game_version\":\"" + GAME_VERSION + "\"}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static void validateManifest(byte[] bytes) throws IOException {
        ManifestParser parser = new ManifestParser(new String(bytes, StandardCharsets.UTF_8));
        parser.skipWhitespace();
        parser.expect('{');
        parser.skipWhitespace();
        boolean schemaSeen = false;
        boolean gameVersionSeen = false;
        int schema = 0;
        String gameVersion = null;
        for (int field = 0; field < 2; field++) {
            String key = parser.string();
            parser.skipWhitespace();
            parser.expect(':');
            parser.skipWhitespace();
            if ("schema".equals(key)) {
                if (schemaSeen) throw parser.malformed();
                schemaSeen = true;
                schema = parser.number();
            } else if ("game_version".equals(key)) {
                if (gameVersionSeen) throw parser.malformed();
                gameVersionSeen = true;
                gameVersion = parser.string();
            } else {
                throw parser.malformed();
            }
            parser.skipWhitespace();
            if (field == 0) parser.expect(',');
        }
        parser.skipWhitespace();
        parser.expect('}');
        parser.skipWhitespace();
        if (!schemaSeen || !gameVersionSeen || !parser.atEnd()) throw parser.malformed();
        if (schema != MANIFEST_SCHEMA || !GAME_VERSION.equals(gameVersion)) {
            throw new IOException("Unsupported save archive version");
        }
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = input.read(buffer)) != -1) {
            if (output.size() + n > limit) throw new IOException("Save archive manifest is too large");
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    private static final class ManifestParser {
        private final String text;
        private int position;

        ManifestParser(String text) { this.text = text; }

        void skipWhitespace() {
            while (position < text.length()) {
                char c = text.charAt(position);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
                position++;
            }
        }

        void expect(char expected) throws IOException {
            if (position >= text.length() || text.charAt(position++) != expected) throw malformed();
        }

        String string() throws IOException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < text.length()) {
                char c = text.charAt(position++);
                if (c == '"') return value.toString();
                if (c == '\\' || c < 0x20) throw malformed();
                value.append(c);
            }
            throw malformed();
        }

        int number() throws IOException {
            int start = position;
            while (position < text.length()) {
                char c = text.charAt(position);
                if (c < '0' || c > '9') break;
                position++;
            }
            if (start == position || (position - start > 1 && text.charAt(start) == '0')) throw malformed();
            try { return Integer.parseInt(text.substring(start, position)); }
            catch (NumberFormatException error) { throw new IOException("Malformed save archive manifest", error); }
        }

        boolean atEnd() { return position == text.length(); }

        IOException malformed() { return new IOException("Malformed save archive manifest"); }
    }
}
