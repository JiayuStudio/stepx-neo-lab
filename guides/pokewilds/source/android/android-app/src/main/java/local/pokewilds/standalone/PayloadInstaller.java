package local.pokewilds.standalone;

import android.content.Context;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Properties;

final class PayloadInstaller {
    static File install(Context context, java.util.function.BooleanSupplier cancelled) throws Exception {
        Properties manifest = new Properties();
        try (InputStream in = context.getAssets().open("payload.properties")) { manifest.load(in); }
        String hash = manifest.getProperty("sha256", "");
        if (!hash.matches("[0-9a-f]{64}")) throw new IOException("Invalid bundled payload manifest");
        File base = new File(context.getFilesDir(), "runtime"); base.mkdirs();
        File ready = new File(base, hash);
        if (new File(ready, ".ready").isFile()) {
            removeObsolete(base, ready);
            return ready;
        }
        long expanded = Long.parseLong(manifest.getProperty("expandedBytes"));
        if (expanded <= 0 || expanded > 8L*1024*1024*1024) throw new IOException("Invalid payload size");
        long gameBytes = Long.parseLong(manifest.getProperty("gameBytes", "0"));
        if (gameBytes != 0) throw new IOException("Bundled runtime must not contain game files");
        // Reclaim an interrupted attempt before checking the space needed to retry.
        File stage = new File(base, ".preparing");
        SafeTar.deleteTree(stage.toPath());
        // Include inode/block overhead measured on the emulator's app storage.
        if (base.getUsableSpace() < expanded + 384L*1024*1024) throw new IOException("Not enough space to prepare runtime files");
        stage.mkdirs();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            RuntimeService.status = "Verifying bundled files…";
            // Verify first, without an extra on-disk copy of the large archive.
            try (InputStream raw = context.getAssets().open("runtime.bin"); DigestInputStream input = new DigestInputStream(raw, digest)) {
                byte[] buffer = new byte[65536];
                while (input.read(buffer) != -1) {
                    if (cancelled.getAsBoolean()) throw new InterruptedIOException("Preparation cancelled");
                }
            }
            StringBuilder actual = new StringBuilder(); for (byte b : digest.digest()) actual.append(String.format("%02x", b & 255));
            if (!hash.contentEquals(actual)) throw new IOException("Bundled payload checksum mismatch");
            try (InputStream input = context.getAssets().open("runtime.bin")) {
                final int[] lastPercent = {-1};
                SafeTar.extract(input, stage.toPath(), expanded, 250000, bytes -> {
                    if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException("Preparation cancelled");
                    int percent = (int)(bytes * 100 / expanded);
                    if (percent != lastPercent[0]) {
                        lastPercent[0] = percent;
                        RuntimeService.status = "Preparing bundled files: " + percent + "%";
                    }
                });
            }
            Files.write(new File(stage, ".ready").toPath(), hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            if (ready.exists()) SafeTar.deleteTree(ready.toPath());
            if (!stage.renameTo(ready)) throw new IOException("Cannot activate prepared runtime");
            removeObsolete(base, ready);
            return ready;
        } catch (Exception e) { SafeTar.deleteTree(stage.toPath()); throw e; }
    }
    private static void removeObsolete(File base, File active) throws IOException {
        File[] versions = base.listFiles();
        if (versions == null) return;
        for (File version : versions) {
            if (!version.equals(active) && version.getName().matches("[0-9a-f]{64}"))
                SafeTar.deleteTree(version.toPath());
        }
    }
}
