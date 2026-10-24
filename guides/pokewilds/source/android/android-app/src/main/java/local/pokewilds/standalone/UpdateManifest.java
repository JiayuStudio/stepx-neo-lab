package local.pokewilds.standalone;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/** Validated, immutable release information. Does not infer updates from prose. */
final class UpdateManifest {
    static final String PACKAGE = "studio.jiayu.pokewilds.neo";
    static final String HOST = "stepxneo.jiayustudio.dpdns.org";
    static final String ENDPOINT = "https://" + HOST + "/apk-catalog.json";
    static final long MAX_APK_BYTES = 512L * 1024 * 1024;
    final long versionCode, size;
    final int minSdk;
    final String versionName, apkUrl, sha256, notes;

    UpdateManifest(int schema, String packageName, long code, String version, int sdk,
                   String url, long bytes, String hash, String description) throws IOException {
        if (schema != 1 || !PACKAGE.equals(packageName) || code <= 0 || code > Integer.MAX_VALUE
                || version == null || !version.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")
                || sdk < 26 || sdk > 100 || bytes <= 0 || bytes > MAX_APK_BYTES
                || hash == null || !hash.matches("[0-9a-fA-F]{64}")
                || description == null || description.length() > 12000) throw new IOException("manifest");
        requireTrustedUrl(url);
        String path = URI.create(url).getRawPath();
        if (!path.matches("/downloads/files/pokewilds/[1-9][0-9]*-" + hash.toLowerCase(Locale.ROOT) + "\\.apk")) throw new IOException("address");
        versionCode = code; versionName = version; minSdk = sdk; apkUrl = url;
        size = bytes; sha256 = hash.toLowerCase(Locale.ROOT); notes = description;
    }

    static void requireTrustedUrl(String address) throws IOException {
        try {
            URI uri = new URI(address);
            if (!"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost())
                    || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getRawPath() == null || !(uri.getRawPath().equals("/apk-catalog.json")
                        || uri.getRawPath().startsWith("/downloads/files/pokewilds/"))
                    || uri.getRawPath().contains("%") || uri.getRawPath().contains("\\")
                    || !uri.normalize().equals(uri)) throw new IOException("address");
        } catch (java.net.URISyntaxException | IllegalArgumentException | NullPointerException error) {
            throw new IOException("address");
        }
    }

    boolean newerThan(long installedCode) { return versionCode > installedCode; }
}
