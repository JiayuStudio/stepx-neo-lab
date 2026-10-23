package local.pokewilds.standalone;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Acquires a pinned game distribution without putting it in the APK. */
final class GameInstaller {
    enum Source { DOWNLOAD, ZIP, FOLDER }
    static final long MAX_ARCHIVE = 256L * 1024 * 1024;
    static final long MAX_EXPANDED = 512L * 1024 * 1024;
    static final int MAX_ENTRIES = 2000;
    private static final String READY = ".game-source-ready";
    private static volatile String rejectedJarIdentity;

    static final class Spec {
        final String version, url, archiveHash, archiveMember, jarHash;
        final List<String> requiredFiles;
        Spec(String version, String url, String archiveHash, String archiveMember,
             String jarHash, List<String> requiredFiles) {
            this.version = version; this.url = url; this.archiveHash = archiveHash;
            this.archiveMember = archiveMember; this.jarHash = jarHash;
            this.requiredFiles = requiredFiles;
        }
        String revision() { return version + ":" + jarHash; }
    }

    private GameInstaller() {}

    static Spec spec(Context context) throws Exception {
        byte[] data;
        try (InputStream in = context.getAssets().open("game-source.json")) { data = readLimited(in, 16384); }
        JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));
        if (json.getInt("schema") != 1) throw new IOException("Unsupported game source metadata");
        JSONArray required = json.getJSONArray("requiredFiles");
        List<String> files = new ArrayList<>();
        for (int i = 0; i < required.length(); i++) files.add(required.getString(i));
        Spec s = new Spec(json.getString("version"), json.getString("url"), json.getString("sha256"),
            json.getString("archiveMember"), json.getString("jarSha256"), files);
        if (!s.url.startsWith("https://") || !s.archiveHash.matches("[0-9a-f]{64}")
            || !s.jarHash.matches("[0-9a-f]{64}") || !validName(s.archiveMember)
            || !files.contains("pokewilds.jar") || files.stream().anyMatch(f -> !validRelative(f)))
            throw new IOException("Invalid game source metadata");
        return s;
    }

    /** Recognizes a previous bundled install after checking its official JAR once. */
    static boolean isInstalled(Context context) {
        try {
            Spec s = spec(context);
            Path game = context.getFilesDir().toPath().resolve("game");
            recover(game);
            Path jar = game.resolve("pokewilds.jar");
            if (!Files.isRegularFile(jar, LinkOption.NOFOLLOW_LINKS)) return false;
            Path marker = game.resolve(READY);
            if (Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)
                && markerValue(jar, s).equals(new String(Files.readAllBytes(marker), StandardCharsets.US_ASCII))) return true;
            String identity = jar.toString() + ":" + Files.size(jar) + ":" + Files.getLastModifiedTime(jar).toMillis();
            if (identity.equals(rejectedJarIdentity)) return false;
            if (!s.jarHash.equals(sha256(jar, () -> false))) {
                rejectedJarIdentity = identity;
                return false;
            }
            writeMarker(game, s);
            return true;
        } catch (Exception error) { return false; }
    }

    static void install(Context context, Source source, Uri uri, Consumer<String> progress,
                        BooleanSupplier cancelled) throws Exception {
        Spec s = spec(context);
        Path game = context.getFilesDir().toPath().resolve("game");
        recover(game);
        SaveArchive.recoverInterruptedImport(game);
        ModManager.recover(game);
        if (isInstalled(context)) { progress.accept("Game files ready"); return; }
        Path stage = game.resolveSibling("game.preparing");
        Path archive = game.resolveSibling("game-download.part");
        SafeTar.deleteTree(stage);
        Files.deleteIfExists(archive);
        // The selected ZIP is copied before extraction; the folder path only needs a stage.
        long needed = source == Source.FOLDER ? 220L * 1024 * 1024 : 350L * 1024 * 1024;
        if (context.getFilesDir().getUsableSpace() < needed)
            throw new IOException("Not enough free space to prepare game files");
        try {
            if (source == Source.FOLDER) {
                if (uri == null) throw new IOException("No game folder selected");
                Files.createDirectories(stage);
                copyFolder(context, uri, stage, progress, cancelled);
            } else {
                if (source == Source.DOWNLOAD) { try (InputStream in = context.getAssets().open("neo-game.zip")) { copyArchive(in, archive, progress, cancelled); } }
                else {
                    if (uri == null) throw new IOException("No game ZIP selected");
                    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                        if (input == null) throw new IOException("Could not open game ZIP");
                        copyArchive(input, archive, progress, cancelled);
                    }
                }
                if (!s.archiveHash.equals(sha256(archive, cancelled))) throw new IOException("Game ZIP checksum mismatch; choose the official v" + s.version + " archive");
                Files.createDirectories(stage);
                extractArchive(archive, stage, s.archiveMember, progress, cancelled);
                // Extraction has finished; release the private ZIP copy before activation.
                Files.delete(archive);
            }
            for (String required : s.requiredFiles)
                if (!Files.isRegularFile(stage.resolve(required), LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Game files are missing " + required);
            if (!s.jarHash.equals(sha256(stage.resolve("pokewilds.jar"), cancelled)))
                throw new IOException("Game JAR checksum mismatch; choose the official v" + s.version + " files");
            if (cancelled.getAsBoolean()) throw new InterruptedIOException("Game installation cancelled");
            progress.accept("Installing verified game files…");
            preserveUserData(game, stage);
            writeMarker(stage, s);
            activate(game, stage);
            progress.accept("Game files ready");
        } finally {
            try { Files.deleteIfExists(archive); }
            finally { SafeTar.deleteTree(stage); }
        }
    }

    /** Remove files left by a process kill, only while the game data lock is held. */
    static void discardInterruptedFiles(Path files) throws IOException {
        Path archive = files.resolve("game-download.part");
        try { Files.deleteIfExists(archive); }
        finally { SafeTar.deleteTree(files.resolve("game.preparing")); }
    }

    private static void download(Spec s, Path archive, Consumer<String> progress,
                                 BooleanSupplier cancelled) throws Exception {
        URL url = new URL(s.url);
        for (int redirect = 0; redirect < 5; redirect++) {
            if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IOException("Game download must use HTTPS");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "PokeWilds-Android/0.4");
            try {
                int code = connection.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Game download redirect has no location");
                    url = new URL(url, location);
                    continue;
                }
                if (code != 200) throw new IOException("Game download returned HTTP " + code);
                long length = connection.getContentLengthLong();
                if (length > MAX_ARCHIVE) throw new IOException("Game ZIP is too large");
                try (InputStream input = connection.getInputStream()) { copyArchive(input, archive, progress, cancelled); }
                return;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many game download redirects");
    }

    private static void copyArchive(InputStream input, Path archive, Consumer<String> progress,
                                    BooleanSupplier cancelled) throws IOException {
        byte[] buffer = new byte[65536]; long bytes = 0; long last = 0;
        try (OutputStream output = Files.newOutputStream(archive, StandardOpenOption.CREATE_NEW)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (cancelled.getAsBoolean()) throw new InterruptedIOException("Game download cancelled");
                bytes += count;
                if (bytes > MAX_ARCHIVE) throw new IOException("Game ZIP is too large");
                output.write(buffer, 0, count);
                if (bytes - last >= 4L * 1024 * 1024) {
                    progress.accept("Receiving game ZIP: " + (bytes / (1024 * 1024)) + " MB"); last = bytes;
                }
            }
        }
        if (bytes == 0) throw new IOException("Game ZIP is empty");
    }

    static void extractArchive(Path archive, Path stage, String member, Consumer<String> progress,
                               BooleanSupplier cancelled) throws IOException {
        Set<Path> seen = new HashSet<>();
        long expanded = 0; int entries = 0;
        try (ZipArchiveInputStream zip = new ZipArchiveInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipArchiveEntry entry; byte[] buffer = new byte[65536];
            while ((entry = zip.getNextZipEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IOException("Game ZIP has too many entries");
                String name = entry.getName();
                if (name.equals(member) || name.equals(member + "/")) continue;
                if (!name.startsWith(member + "/")) throw new IOException("Unexpected game ZIP path: " + name);
                String relative = name.substring(member.length() + 1);
                if (!validRelative(relative)) throw new IOException("Unsafe game ZIP path: " + name);
                if (entry.isUnixSymlink()) throw new IOException("Game ZIP contains a link");
                if (isSavePath(relative)) continue;
                Path out = stage.resolve(relative).normalize();
                if (!out.startsWith(stage) || !seen.add(out)) throw new IOException("Duplicate or unsafe game ZIP path: " + name);
                if (entry.isDirectory()) { Files.createDirectories(out); continue; }
                Files.createDirectories(out.getParent());
                try (OutputStream output = Files.newOutputStream(out, StandardOpenOption.CREATE_NEW)) {
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        if (cancelled.getAsBoolean()) throw new InterruptedIOException("Game installation cancelled");
                        expanded += count;
                        if (expanded > MAX_EXPANDED) throw new IOException("Game files exceed size limit");
                        output.write(buffer, 0, count);
                    }
                }
            }
        }
        progress.accept("Verified game files extracted");
    }

    private static void copyFolder(Context context, Uri tree, Path stage, Consumer<String> progress,
                                   BooleanSupplier cancelled) throws IOException {
        String id = DocumentsContract.getTreeDocumentId(tree);
        int[] count = {0}; long[] bytes = {0};
        copyChildren(context, tree, id, stage, stage, count, bytes, progress, cancelled);
    }

    private static void copyChildren(Context context, Uri tree, String parentId, Path root, Path output,
                                     int[] count, long[] bytes, Consumer<String> progress,
                                     BooleanSupplier cancelled) throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
        try (Cursor cursor = context.getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) throw new IOException("Could not list selected game folder");
            while (cursor.moveToNext()) {
                if (cancelled.getAsBoolean()) throw new InterruptedIOException("Game installation cancelled");
                if (++count[0] > MAX_ENTRIES) throw new IOException("Game folder has too many entries");
                String childId = cursor.getString(0), name = cursor.getString(1), mime = cursor.getString(2);
                if (!validName(name)) throw new IOException("Unsafe game file name");
                Path destination = output.resolve(name);
                if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Duplicate game file name: " + name);
                if (isSavePath(name) && output.equals(root)) continue;
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    Files.createDirectory(destination);
                    copyChildren(context, tree, childId, root, destination, count, bytes, progress, cancelled);
                } else {
                    Uri document = DocumentsContract.buildDocumentUriUsingTree(tree, childId);
                    try (InputStream input = context.getContentResolver().openInputStream(document);
                         OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                        if (input == null) throw new IOException("Could not read game file: " + name);
                        byte[] buffer = new byte[65536]; int n;
                        while ((n = input.read(buffer)) != -1) {
                            if (cancelled.getAsBoolean()) throw new InterruptedIOException("Game installation cancelled");
                            bytes[0] += n;
                            if (bytes[0] > MAX_EXPANDED) throw new IOException("Game folder exceeds size limit");
                            out.write(buffer, 0, n);
                        }
                    }
                    if (bytes[0] % (4L * 1024 * 1024) < 65536)
                        progress.accept("Copying game folder: " + (bytes[0] / (1024 * 1024)) + " MB");
                }
            }
        } catch (SecurityException e) { throw new IOException("Game folder access was lost", e); }
    }

    private static boolean isSavePath(String relative) {
        String top = relative.split("/", 2)[0];
        return top.endsWith(".sav") || top.endsWith(".sav.zip");
    }
    private static boolean validName(String name) {
        return name != null && !name.isEmpty() && !name.equals(".") && !name.equals("..")
            && name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf('\0') < 0;
    }
    private static boolean validRelative(String path) {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0) return false;
        String[] parts = path.split("/", -1);
        for (int i = 0; i < parts.length; i++)
            if (!validName(parts[i]) && !(i == parts.length - 1 && parts[i].isEmpty())) return false;
        return true;
    }

    static void preserveUserData(Path old, Path stage) throws IOException {
        if (!Files.isDirectory(old, LinkOption.NOFOLLOW_LINKS)) return;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(old)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (name.equals("settings.txt") || name.equals("mods") || isSavePath(name))
                    copyUserPath(entry, stage.resolve(name));
            }
        }
    }
    private static void copyUserPath(Path from, Path to) throws IOException {
        if (Files.isSymbolicLink(from)) throw new IOException("Existing user data contains a link");
        if (Files.isDirectory(from, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(to);
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(from)) {
                for (Path entry : entries) copyUserPath(entry, to.resolve(entry.getFileName()));
            }
        } else if (Files.isRegularFile(from, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void activate(Path game, Path stage) throws IOException {
        Path previous = game.resolveSibling("game.previous");
        SafeTar.deleteTree(previous);
        boolean hadGame = Files.exists(game, LinkOption.NOFOLLOW_LINKS);
        if (hadGame) Files.move(game, previous);
        try { Files.move(stage, game); }
        catch (IOException error) {
            if (hadGame && !Files.exists(game, LinkOption.NOFOLLOW_LINKS)) Files.move(previous, game);
            throw error;
        }
        SafeTar.deleteTree(previous);
    }
    static void recover(Path game) throws IOException {
        Path previous = game.resolveSibling("game.previous");
        if (!Files.exists(game, LinkOption.NOFOLLOW_LINKS) && Files.exists(previous, LinkOption.NOFOLLOW_LINKS))
            Files.move(previous, game);
    }
    private static void writeMarker(Path game, Spec s) throws IOException {
        Path temporary = game.resolve(READY + ".preparing");
        Files.write(temporary, markerValue(game.resolve("pokewilds.jar"), s).getBytes(StandardCharsets.US_ASCII));
        Files.move(temporary, game.resolve(READY), StandardCopyOption.REPLACE_EXISTING);
    }
    private static String markerValue(Path jar, Spec s) throws IOException {
        return s.revision() + ":" + Files.size(jar) + ":" + Files.getLastModifiedTime(jar).toMillis();
    }
    private static String sha256(Path path, BooleanSupplier cancelled) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new DigestInputStream(Files.newInputStream(path), digest)) {
            byte[] buffer = new byte[65536];
            while (in.read(buffer) != -1) if (cancelled.getAsBoolean()) throw new InterruptedIOException("Game installation cancelled");
        }
        StringBuilder text = new StringBuilder();
        for (byte b : digest.digest()) text.append(String.format(Locale.ROOT, "%02x", b & 255));
        return text.toString();
    }
    private static byte[] readLimited(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
        while ((count = in.read(buffer)) != -1) {
            if (out.size() + count > limit) throw new IOException("Game source metadata is too large");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }
}
