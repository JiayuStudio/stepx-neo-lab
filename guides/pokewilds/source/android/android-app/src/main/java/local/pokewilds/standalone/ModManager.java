package local.pokewilds.standalone;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Imports game mod files through Android's document picker without exposing app-private storage. */
final class ModManager {
    private static final String STAGE = ".mods-import.preparing";
    private static final String PREVIOUS = ".mods.previous";
    private static final long MAX_BYTES = 512L * 1024 * 1024;
    private static final int MAX_ENTRIES = 50000;

    private ModManager() {}

    static final class Entry {
        final String name;
        final boolean directory;
        final boolean link;
        final long size;

        Entry(String name, BasicFileAttributes attributes) {
            this.name = name;
            directory = attributes.isDirectory();
            link = attributes.isSymbolicLink();
            size = attributes.isRegularFile() ? attributes.size() : 0;
        }
    }

    /** Call while holding RuntimeService.DATA_LOCK before using the mods directory. */
    static void recover(Path game) throws IOException {
        if (!Files.isDirectory(game, LinkOption.NOFOLLOW_LINKS)) return;
        Path mods = game.resolve("mods"), previous = game.resolve(PREVIOUS);
        if (Files.exists(previous, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(mods, LinkOption.NOFOLLOW_LINKS)) SafeTar.deleteTree(previous);
            else Files.move(previous, mods);
        }
        SafeTar.deleteTree(game.resolve(STAGE));
    }

    static void importZip(InputStream source, Path game) throws IOException {
        prepare(game);
        Path stage = game.resolve(STAGE), incoming = stage.resolve("incoming");
        try {
            Files.createDirectories(incoming);
            extractZip(source, incoming);
            installPrepared(incoming, game, stage);
        } finally { SafeTar.deleteTree(stage); }
    }

    static void importFolder(Context context, Uri tree, Path game) throws IOException {
        prepare(game);
        Path stage = game.resolve(STAGE), incoming = stage.resolve("incoming");
        try {
            Files.createDirectories(incoming);
            int[] entries = {0}; long[] bytes = {0};
            copyChildren(context, tree, DocumentsContract.getTreeDocumentId(tree), incoming, entries, bytes);
            installPrepared(incoming, game, stage);
        } catch (SecurityException error) {
            throw new IOException("Mod folder access was lost", error);
        } finally { SafeTar.deleteTree(stage); }
    }

    static void exportZip(Path game, OutputStream destination) throws IOException {
        recover(game);
        Path mods = game.resolve("mods");
        if (!Files.isDirectory(mods, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(mods))
            throw new IOException("No mods folder to export");
        int[] entries = {0}; long[] bytes = {0};
        try (ZipOutputStream zip = new ZipOutputStream(destination, StandardCharsets.UTF_8)) {
            Files.walkFileTree(mods, new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isSymbolicLink(dir)) throw new IOException("Mods folder contains a link");
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile() || Files.isSymbolicLink(file)) throw new IOException("Mods folder contains an unsupported file");
                    checkLimit(entries, bytes, Files.size(file));
                    String name = "mods/" + mods.relativize(file).toString().replace(File.separatorChar, '/');
                    zip.putNextEntry(new ZipEntry(name));
                    Files.copy(file, zip);
                    zip.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /** Browse one directory inside mods; callers hold RuntimeService.DATA_LOCK. */
    static List<Entry> list(Path game, Path relativeDirectory) throws IOException {
        recover(game);
        validateRelativeDirectory(relativeDirectory);
        Path mods = game.resolve("mods");
        if (!Files.exists(mods, LinkOption.NOFOLLOW_LINKS)) return new ArrayList<>();
        Path directory = modsDirectory(mods, relativeDirectory);
        List<Entry> entries = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children)
                entries.add(new Entry(child.getFileName().toString(),
                    Files.readAttributes(child, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)));
        }
        entries.sort(Comparator.comparing((Entry entry) -> !entry.directory)
            .thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    /** Remove selected immediate children with the import swap's recovery guarantees; callers hold DATA_LOCK. */
    static void removeSelected(Path game, Path relativeDirectory, List<String> names) throws IOException {
        recover(game);
        Path mods = game.resolve("mods");
        modsDirectory(mods, relativeDirectory);
        if (names.isEmpty()) throw new IOException("Select at least one mod file or folder");
        Set<Path> skipped = new HashSet<>();
        for (String name : names) {
            if (!safeName(name)) throw new IOException("Invalid mod file name");
            Path relative = relativeDirectory.resolve(name);
            if (!Files.exists(mods.resolve(relative), LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Mod file no longer exists: " + name);
            skipped.add(relative);
        }
        Path stage = game.resolve(STAGE), merged = stage.resolve("merged");
        try {
            Files.createDirectories(stage);
            int[] entries = {0}; long[] bytes = {0};
            copyTree(mods, merged, entries, bytes, false, skipped);
            activate(game, merged);
        } finally { SafeTar.deleteTree(stage); }
    }

    private static Path modsDirectory(Path mods, Path relative) throws IOException {
        validateRelativeDirectory(relative);
        Path directory = mods;
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory))
            throw new IOException("No safe mods folder to browse");
        if (!relative.toString().isEmpty()) for (Path part : relative) {
            directory = directory.resolve(part);
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory))
                throw new IOException("Mod folder is unavailable");
        }
        return directory;
    }

    private static void validateRelativeDirectory(Path relative) throws IOException {
        if (relative.isAbsolute() || !relative.normalize().equals(relative))
            throw new IOException("Invalid mods folder path");
        if (!relative.toString().isEmpty()) for (Path part : relative) {
            if (!safeName(part.toString())) throw new IOException("Invalid mods folder path");
        }
    }

    private static void prepare(Path game) throws IOException {
        recover(game);
        if (!Files.isRegularFile(game.resolve("pokewilds.jar"), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Install the game before importing mods");
        Files.createDirectories(game.resolve(STAGE));
    }

    private static void extractZip(InputStream source, Path incoming) throws IOException {
        int[] entries = {0}; long[] bytes = {0};
        Set<Path> seen = new HashSet<>();
        try (ZipArchiveInputStream zip = new ZipArchiveInputStream(new BufferedInputStream(source))) {
            ZipArchiveEntry entry; byte[] buffer = new byte[65536];
            while ((entry = zip.getNextZipEntry()) != null) {
                if (++entries[0] > MAX_ENTRIES) throw new IOException("Mod ZIP has too many entries");
                String name = entry.getName();
                if (!safePath(name)) throw new IOException("Unsafe mod ZIP path: " + name);
                if (entry.isUnixSymlink()) throw new IOException("Mod ZIP contains a link");
                Path relative = Paths.get(name).normalize();
                Path output = incoming.resolve(relative).normalize();
                if (!output.startsWith(incoming) || !seen.add(output)) throw new IOException("Duplicate mod ZIP path: " + name);
                if (entry.isDirectory()) { Files.createDirectories(output); continue; }
                Files.createDirectories(output.getParent());
                try (OutputStream out = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        bytes[0] += count;
                        if (bytes[0] > MAX_BYTES) throw new IOException("Mod ZIP is too large");
                        out.write(buffer, 0, count);
                    }
                }
            }
        }
    }

    private static void copyChildren(Context context, Uri tree, String parentId, Path output,
                                     int[] entries, long[] bytes) throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
        try (Cursor cursor = context.getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) throw new IOException("Could not list selected mod folder");
            while (cursor.moveToNext()) {
                if (++entries[0] > MAX_ENTRIES) throw new IOException("Mod folder has too many entries");
                String id = cursor.getString(0), name = cursor.getString(1), mime = cursor.getString(2);
                if (!safeName(name)) throw new IOException("Unsafe mod file name");
                Path target = output.resolve(name);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Duplicate mod file: " + name);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    Files.createDirectory(target);
                    copyChildren(context, tree, id, target, entries, bytes);
                } else {
                    Uri document = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    try (InputStream input = context.getContentResolver().openInputStream(document)) {
                        if (input == null) throw new IOException("Could not read mod file: " + name);
                        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                            byte[] buffer = new byte[65536]; int count;
                            while ((count = input.read(buffer)) != -1) {
                                bytes[0] += count;
                                if (bytes[0] > MAX_BYTES) throw new IOException("Mod folder is too large");
                                out.write(buffer, 0, count);
                            }
                        }
                    }
                }
            }
        }
    }

    /** Build a complete merged tree, then swap it into place. */
    static void installPrepared(Path incoming, Path game, Path stage) throws IOException {
        Path source = findModsRoot(incoming);
        Path merged = stage.resolve("merged");
        Files.createDirectories(merged);
        int[] entries = {0}; long[] bytes = {0};
        Path current = game.resolve("mods");
        if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) copyTree(current, merged, entries, bytes, false);
        int before = entries[0];
        copyTree(source, merged, entries, bytes, true);
        if (entries[0] == before) throw new IOException("Selected ZIP or folder contains no mod files");
        activate(game, merged);
    }

    private static Path findModsRoot(Path incoming) throws IOException {
        Path direct = incoming.resolve("mods");
        if (Files.isDirectory(direct, LinkOption.NOFOLLOW_LINKS)) return direct;
        Path nested = null;
        try (DirectoryStream<Path> children = Files.newDirectoryStream(incoming)) {
            for (Path child : children) {
                if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) continue;
                Path candidate = child.resolve("mods");
                if (!Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) continue;
                if (nested != null) throw new IOException("Mod archive has multiple mods folders; choose one folder");
                nested = candidate;
            }
        }
        return nested != null ? nested : incoming;
    }

    private static void copyTree(Path source, Path target, int[] entries, long[] bytes,
                                 boolean replace) throws IOException {
        copyTree(source, target, entries, bytes, replace, java.util.Collections.emptySet());
    }

    private static void copyTree(Path source, Path target, int[] entries, long[] bytes,
                                 boolean replace, Set<Path> skipped) throws IOException {
        if (Files.isSymbolicLink(source) || !Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Mods source is not a folder");
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (skipped.contains(source.relativize(dir))) return FileVisitResult.SKIP_SUBTREE;
                if (Files.isSymbolicLink(dir)) throw new IOException("Mods folder contains a link");
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (skipped.contains(source.relativize(file))) return FileVisitResult.CONTINUE;
                if (!attrs.isRegularFile() || Files.isSymbolicLink(file)) throw new IOException("Mods folder contains an unsupported file");
                if (file.getFileName().toString().equals("pokewilds.jar")) throw new IOException("Choose mod files, not a game archive");
                checkLimit(entries, bytes, Files.size(file));
                Path destination = target.resolve(source.relativize(file));
                if (replace) Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                else Files.copy(file, destination);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void activate(Path game, Path merged) throws IOException {
        Path current = game.resolve("mods"), previous = game.resolve(PREVIOUS);
        boolean hadMods = Files.exists(current, LinkOption.NOFOLLOW_LINKS);
        if (hadMods) Files.move(current, previous);
        try { Files.move(merged, current); }
        catch (IOException error) {
            if (hadMods && !Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                try { Files.move(previous, current); }
                catch (IOException rollback) { error.addSuppressed(rollback); }
            }
            throw error;
        }
        // A process kill after activation leaves both folders; recover() keeps the new one.
        try { SafeTar.deleteTree(previous); } catch (IOException ignored) { /* Retry on next launch. */ }
    }

    private static void checkLimit(int[] entries, long[] bytes, long size) throws IOException {
        if (++entries[0] > MAX_ENTRIES || size < 0 || size > MAX_BYTES - bytes[0])
            throw new IOException("Mods exceed size or file-count limit");
        bytes[0] += size;
    }

    private static boolean safeName(String name) {
        return name != null && !name.isEmpty() && !name.equals(".") && !name.equals("..")
            && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf('\0') < 0;
    }

    private static boolean safePath(String path) {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0) return false;
        String[] parts = path.split("/", -1);
        for (int i = 0; i < parts.length; i++)
            if (!safeName(parts[i]) && !(i == parts.length - 1 && parts[i].isEmpty())) return false;
        return true;
    }
}
