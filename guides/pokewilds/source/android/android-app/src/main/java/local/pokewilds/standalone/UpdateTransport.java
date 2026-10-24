package local.pokewilds.standalone;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/** Bounded HTTPS transfers. Cancellation never promotes a partial package. */
final class UpdateTransport {
    interface Progress { void changed(long count, long total); }
    private volatile boolean cancelled;
    private volatile HttpURLConnection connection;
    boolean isCancelled() { return cancelled; }

    void cancel() {
        cancelled = true;
        HttpURLConnection active = connection;
        if (active != null) active.disconnect();
    }

    private void requireActive() throws IOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("cancelled");
    }

    private HttpURLConnection open(String address) throws IOException {
        for (int hop = 0; hop < 4; hop++) {
            requireActive();
            UpdateManifest.requireTrustedUrl(address);
            URL url = new URL(address);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            connection = c;
            c.setInstanceFollowRedirects(false); c.setUseCaches(false);
            c.setConnectTimeout(15000); c.setReadTimeout(20000);
            c.setRequestProperty("Accept-Encoding", "identity");
            c.setRequestProperty("Cache-Control", "no-cache");
            c.setRequestProperty("User-Agent", "PokeWildsNeo-Updater");
            try {
                int code = c.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String next = c.getHeaderField("Location");
                    if (next == null) throw new IOException("address");
                    address = new URL(url, next).toString(); c.disconnect(); continue;
                }
                if (code != 200) throw new IOException("HTTP " + code);
                requireActive();
                return c;
            } catch (IOException error) { c.disconnect(); throw error; }
        }
        throw new IOException("address");
    }

    String manifest() throws IOException {
        HttpURLConnection c = open(UpdateManifest.ENDPOINT);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                requireActive();
                if (out.size() + n > 65536) throw new IOException("manifest");
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally { c.disconnect(); connection = null; }
    }

    File download(UpdateManifest release, File directory, Progress progress) throws Exception {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("storage");
        File ready = new File(directory, "PokeWilds-Neo-" + release.versionName + ".apk");
        File[] old = directory.listFiles((dir, name) -> name.matches("PokeWilds-Neo-[0-9.]+\\.apk") && !name.equals(ready.getName()));
        if (old != null) for (File stale : old) Files.deleteIfExists(stale.toPath());
        if (ready.isFile()) {
            try { verifyDigest(ready, release); return ready; }
            catch (IOException invalid) { requireActive(); if (!ready.delete()) throw new IOException("storage"); }
        }
        if (directory.getUsableSpace() < release.size + 16L * 1024 * 1024) throw new IOException("storage");
        File part = new File(directory, "update.part");
        try {
            HttpURLConnection c = open(release.apkUrl);
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                long declared = c.getContentLengthLong();
                if (declared != -1 && declared != release.size) throw new IOException("size");
                copyVerified(in, out, release, () -> cancelled || Thread.currentThread().isInterrupted(), progress);
            } finally { c.disconnect(); connection = null; }
            requireActive();
            Files.move(part.toPath(), ready.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return ready;
        } finally { Files.deleteIfExists(part.toPath()); }
    }

    static void copyVerified(InputStream in, OutputStream out, UpdateManifest release,
                             java.util.function.BooleanSupplier cancelled, Progress progress) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[65536]; long count = 0, last = 0; int n;
        while ((n = in.read(buffer)) != -1) {
            if (cancelled.getAsBoolean()) throw new IOException("cancelled");
            count += n;
            if (count > release.size) throw new IOException("size");
            out.write(buffer, 0, n); digest.update(buffer, 0, n);
            long now = System.nanoTime();
            if (now - last > 250000000L) { progress.changed(count, release.size); last = now; }
        }
        if (cancelled.getAsBoolean()) throw new IOException("cancelled");
        if (count != release.size || !hex(digest.digest()).equals(release.sha256)) throw new IOException("digest");
        progress.changed(count, release.size);
    }

    void verifyDigest(File file, UpdateManifest release) throws Exception {
        if (file.length() != release.size) throw new IOException("size");
        try (InputStream in = new FileInputStream(file)) {
            copyVerified(in, new OutputStream() {
                @Override public void write(int b) { }
                @Override public void write(byte[] b, int offset, int length) { }
            }, release, () -> cancelled || Thread.currentThread().isInterrupted(), (n, total) -> { });
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }
}
