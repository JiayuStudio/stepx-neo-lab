package local.pokewilds.standalone;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.*;

/** Extracts trusted, checksum-verified payloads, still enforcing archive boundaries. */
public final class SafeTar {
    private SafeTar() {}
    public static void extract(InputStream source, Path destination, long byteLimit, int entryLimit) throws IOException {
        extract(source, destination, byteLimit, entryLimit, bytes -> {});
    }
    public static void extract(InputStream source, Path destination, long byteLimit, int entryLimit,
                               java.util.function.LongConsumer progress) throws IOException {
        Files.createDirectories(destination);
        Path root = destination.toAbsolutePath().normalize();
        List<TarArchiveEntry> links = new ArrayList<>();
        Set<Path> files = new HashSet<>();
        long written = 0; int count = 0;
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(source))) {
            TarArchiveEntry entry;
            byte[] buffer = new byte[65536];
            while ((entry = tar.getNextEntry()) != null) {
                if (++count > entryLimit) throw new IOException("Too many archive entries");
                Path out = resolve(root, entry.getName());
                if (entry.isDirectory()) { Files.createDirectories(out); continue; }
                if (!files.add(out)) throw new IOException("Duplicate archive path: " + entry.getName());
                if (entry.isSymbolicLink() || entry.isLink()) { links.add(entry); continue; }
                if (!entry.isFile()) throw new IOException("Unsupported archive entry: " + entry.getName());
                if (entry.getSize() < 0 || entry.getSize() > byteLimit-written) throw new IOException("Payload exceeds size limit");
                Files.createDirectories(out.getParent());
                try (OutputStream output = Files.newOutputStream(out, StandardOpenOption.CREATE_NEW)) {
                    int n;
                    while ((n = tar.read(buffer)) != -1) { written += n; if (written > byteLimit) throw new IOException("Payload exceeds size limit"); output.write(buffer, 0, n); progress.accept(written); }
                }
                if ((entry.getMode() & 0111) != 0 && !out.toFile().setExecutable(true, true)) throw new IOException("Cannot set executable mode");
            }
        }
        // Defer links: no later file write can follow an archive-controlled symlink.
        for (TarArchiveEntry e : links) {
            Path out = resolve(root, e.getName());
            Files.createDirectories(out.getParent());
            String name = e.getLinkName();
            Path target;
            if (e.isLink()) target = resolve(root, name);
            else if (name.startsWith("/")) {
                // Absolute guest links belong to the rootfs, never the Android host.
                Path guestRoot = root.resolve("rootfs");
                if (!out.startsWith(guestRoot)) throw new IOException("Absolute link outside rootfs");
                target = guestRoot.resolve(name.substring(1)).normalize();
                if (!target.startsWith(guestRoot)) throw new IOException("Escaping guest symlink");
            } else target = out.getParent().resolve(name).normalize();
            if (!target.startsWith(root)) throw new IOException("Escaping archive link");
            if (e.isLink()) {
                // Android app SELinux rules can reject hard links. Materialize a copy.
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid hard-link target");
                long size = Files.size(target);
                if (size > byteLimit-written) throw new IOException("Payload exceeds size limit");
                written += size;
                Files.copy(target, out);
                progress.accept(written);
                if (Files.isExecutable(target) && !out.toFile().setExecutable(true, true)) throw new IOException("Cannot set hard-link copy mode");
            } else {
                Path relative = out.getParent().relativize(target);
                Files.createSymbolicLink(out, relative.toString().isEmpty() ? Paths.get(".") : relative);
            }
        }
    }
    private static Path resolve(Path root, String name) throws IOException {
        Path p = Paths.get(name);
        if (p.isAbsolute() || name.indexOf('\0') >= 0) throw new IOException("Invalid archive path");
        Path out = root.resolve(p).normalize();
        if (!out.startsWith(root)) throw new IOException("Archive path traversal");
        return out;
    }
    public static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFileFailed(Path p, IOException error) throws IOException {
                // PRoot can leave mode-000 bind placeholders in its private
                // rootfs. Restore owner access before removing this old tree.
                if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)
                    && p.toFile().setReadable(true, true)
                    && p.toFile().setWritable(true, true)
                    && p.toFile().setExecutable(true, true)) {
                    deleteTree(p);
                    return FileVisitResult.CONTINUE;
                }
                throw error;
            }
            @Override public FileVisitResult visitFile(Path p, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException { Files.delete(p); return FileVisitResult.CONTINUE; }
            @Override public FileVisitResult postVisitDirectory(Path p, IOException error) throws IOException { if (error != null) throw error; Files.delete(p); return FileVisitResult.CONTINUE; }
        });
    }
}
