package local.pokewilds.standalone;

import org.junit.Test;
import java.io.*;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class UpdatePolicyTest {
    private UpdateManifest release(byte[] bytes) throws Exception {
        String hash = UpdateTransport.hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return new UpdateManifest(1, UpdateManifest.PACKAGE, 816, "0.8.16", 26,
                "https://" + UpdateManifest.HOST + "/downloads/files/pokewilds/123-" + hash + ".apk", bytes.length, hash, "更新说明");
    }
    @Test public void comparesNumericVersionCodesAndNeverOffersDowngrade() throws Exception {
        UpdateManifest release = release(new byte[]{1});
        assertTrue(release.newerThan(815)); assertFalse(release.newerThan(816)); assertFalse(release.newerThan(817));
    }
    @Test public void rejectsUntrustedAndAmbiguousAddresses() throws Exception {
        String base = "https://" + UpdateManifest.HOST;
        String[] bad = {"http://" + UpdateManifest.HOST + "/downloads/pokewilds/update.json",
                base + ".example.org/downloads/pokewilds/update.json", "https://user@" + UpdateManifest.HOST + "/downloads/pokewilds/update.json",
                base + ":8443/downloads/pokewilds/update.json", base + "/downloads/pokewilds/../other.apk",
                base + "/downloads/pokewilds/%2e%2e/other.apk", base + "/downloads/pokewilds/update.json?q=1",
                base + "/downloads/pokewilds/update.json#fragment", base + "/other/update.json", "file:///data/update.json"};
        for (String url : bad) {
            assertThrows(url, IOException.class, () -> UpdateManifest.requireTrustedUrl(url));
        }
        UpdateManifest.requireTrustedUrl(UpdateManifest.ENDPOINT);
    }
    @Test public void rejectsWrongPackageOversizedPackageAndMismatchedDigestPath() throws Exception {
        UpdateManifest good = release(new byte[]{1});
        assertThrows(IOException.class, () -> new UpdateManifest(1, "another.app", 816, "0.8.16", 26,
                good.apkUrl, good.size, good.sha256, ""));
        assertThrows(IOException.class, () -> new UpdateManifest(1, UpdateManifest.PACKAGE, 816, "0.8.16", 26,
                good.apkUrl, UpdateManifest.MAX_APK_BYTES + 1, good.sha256, ""));
        assertThrows(IOException.class, () -> new UpdateManifest(1, UpdateManifest.PACKAGE, 816, "0.8.16", 26,
                good.apkUrl.replace(good.sha256, "0".repeat(64)), good.size, good.sha256, ""));
        assertThrows(IOException.class, () -> new UpdateManifest(1, UpdateManifest.PACKAGE, 0, "0.8.16", 26,
                good.apkUrl, good.size, good.sha256, ""));
        assertThrows(IOException.class, () -> new UpdateManifest(1, UpdateManifest.PACKAGE, 816, "0.8.16", 26,
                good.apkUrl, good.size, "invalid", ""));
    }
    @Test public void acceptsOnlyExactBytesAndDigest() throws Exception {
        byte[] bytes = new byte[]{1,2,3,4}; UpdateManifest release = release(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UpdateTransport.copyVerified(new ByteArrayInputStream(bytes), out, release, () -> false, (n,t) -> {});
        assertArrayEquals(bytes, out.toByteArray());
        for (byte[] invalid : new byte[][]{{1,2,3}, {1,2,3,4,5}, {1,2,3,5}})
            assertThrows(IOException.class, () -> UpdateTransport.copyVerified(new ByteArrayInputStream(invalid),
                    new ByteArrayOutputStream(), release, () -> false, (n,t) -> {}));
    }
    @Test public void cancellationBeforeAndDuringTransferPreventsSuccess() throws Exception {
        byte[] bytes = new byte[65537]; UpdateManifest release = release(bytes);
        assertThrows(IOException.class, () -> UpdateTransport.copyVerified(new ByteArrayInputStream(bytes),
                new ByteArrayOutputStream(), release, () -> true, (n,t) -> {}));
        AtomicBoolean cancel = new AtomicBoolean();
        assertThrows(IOException.class, () -> UpdateTransport.copyVerified(new ByteArrayInputStream(bytes),
                new ByteArrayOutputStream(), release, cancel::get, (n,t) -> cancel.set(true)));
    }
    @Test public void failedDownloadCannotReuseCorruptedCachedPackage() throws Exception {
        byte[] bytes = new byte[]{1,2,3}; UpdateManifest release = release(bytes);
        File cache = File.createTempFile("update-cache", ".apk");
        try {
            try (FileOutputStream out = new FileOutputStream(cache)) { out.write(new byte[]{1,2,4}); }
            assertThrows(IOException.class, () -> new UpdateTransport().verifyDigest(cache, release));
        } finally { cache.delete(); }
    }
}
