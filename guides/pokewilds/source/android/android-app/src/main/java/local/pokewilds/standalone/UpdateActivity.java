package local.pokewilds.standalone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-initiated, same-source app updates. Game data is never part of an update transfer. */
public final class UpdateActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;
    private UpdateTransport transport;
    private boolean busy;
    private UpdateManifest release;
    private UpdateManifest checkedRelease;
    private File ready;
    private TextView status, notes;
    private Button check, download, install, cancel;
    private ProgressBar progress;
    private long installedCode;
    private String installedName;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("检查更新");
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::leave);
        try {
            PackageInfo current = getPackageManager().getPackageInfo(getPackageName(), 0);
            installedCode = code(current); installedName = current.versionName;
        } catch (PackageManager.NameNotFoundException impossible) { finish(); return; }
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(20), dp(20), dp(20), dp(24));
        TextView heading = new TextView(this); heading.setText("宝可梦荒野 · 检查更新"); heading.setTextSize(25); page.addView(heading);
        TextView current = new TextView(this);
        current.setText("当前版本 " + installedName + "\n更新来源：StepX Neo 研究所\n安装包由研究所 VPS 提供，更新会保留本机存档。");
        current.setTextSize(16); current.setPadding(0, dp(12), 0, dp(16)); page.addView(current);
        status = new TextView(this); status.setTextSize(17); page.addView(status);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setVisibility(View.GONE); page.addView(progress);
        notes = new TextView(this); notes.setTextSize(16); notes.setPadding(0, dp(12), 0, dp(12)); page.addView(notes);
        check = button(page, "重新检查", this::check);
        download = button(page, "下载更新", this::confirmDownload);
        install = button(page, "安装已下载的更新", this::prepareInstall);
        cancel = button(page, "取消下载 / 检查", () -> {
            if (transport != null) transport.cancel();
            cancel.setEnabled(false); status.setText("正在取消…");
        });
        button(page, "查看更新说明", () -> {
            String url = "https://github.com/JiayuStudio/stepx-neo-lab/" + (checkedRelease == null
                    ? "releases" : "releases/tag/pokewilds-neo-v" + checkedRelease.versionName);
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (android.content.ActivityNotFoundException error) { status.setText("没有可用浏览器，请稍后重试。"); }
        });
        button(page, "返回", this::leave);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll); scroll.requestApplyInsets();
        buttons(); check();
    }

    private Button button(LinearLayout page, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setTextSize(16); button.setMinHeight(dp(50));
        button.setOnClickListener(v -> action.run()); page.addView(button); return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static long code(PackageInfo info) { return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode; }
    private void ui(Runnable action) { runOnUiThread(() -> { if (!destroyed && !isFinishing()) action.run(); }); }
    private void buttons() {
        check.setEnabled(!busy);
        download.setVisibility(release != null && ready == null ? View.VISIBLE : View.GONE); download.setEnabled(!busy);
        install.setVisibility(ready != null ? View.VISIBLE : View.GONE); install.setEnabled(!busy);
        cancel.setVisibility(busy ? View.VISIBLE : View.GONE); cancel.setEnabled(busy);
        if (!busy) progress.setVisibility(View.GONE);
    }
    private UpdateTransport begin(String text) {
        busy = true; transport = new UpdateTransport(); status.setText(text); buttons(); return transport;
    }
    private void done() { ui(() -> { busy = false; buttons(); }); }

    private void check() {
        if (busy) return;
        release = null; checkedRelease = null; ready = null; notes.setText("");
        UpdateTransport transfer = begin("正在检查研究所最新版本…");
        worker.execute(() -> {
            try {
                JSONObject json = new JSONObject(transfer.manifest()).getJSONObject("items").getJSONObject("pokewilds");
                JSONObject identity = json.getJSONObject("apk_manifest");
                if (!"vps".equals(json.getString("hosting"))
                        || !json.getString("version").equals(identity.getString("version_name"))
                        || !json.getString("verified_sha256").matches("[0-9a-f]{64}")
                        || !json.getString("sha256").equals("sha256:" + json.getString("verified_sha256"))
                        || json.getLong("size") != json.getLong("verified_size")) throw new IOException("manifest");
                UpdateManifest found = new UpdateManifest(1, identity.getString("package"),
                        identity.getLong("version_code"), identity.getString("version_name"), json.optInt("min_sdk", 26),
                        json.getString("url"), json.getLong("verified_size"), json.getString("verified_sha256"),
                        json.optString("release_notes", "更新内容请点“查看更新说明”。"));
                ui(() -> {
                    if (transfer.isCancelled()) { status.setText("已取消，可重新检查。"); return; }
                    checkedRelease = found;
                    notes.setText("版本 " + found.versionName + " 更新说明\n\n" + found.notes);
                    if (found.minSdk > Build.VERSION.SDK_INT) {
                        status.setText("最新版本 " + found.versionName + " 需要更高版本的 Android，当前设备暂不支持。");
                    } else if (found.newerThan(installedCode)) {
                        release = found;
                        status.setText("发现新版本 " + found.versionName + "\n安装包 " + size(found.size) + "，建议使用 Wi-Fi 下载。");
                    } else status.setText(found.versionCode == installedCode
                            ? "已是最新版本 " + installedName : "当前版本 " + installedName + " 高于发布版本，无需更新。");
                });
            } catch (Exception error) { failure(error); }
            finally { done(); }
        });
    }

    private void confirmDownload() {
        if (busy || release == null) return;
        UpdateManifest chosen = release;
        new AlertDialog.Builder(this).setTitle("下载 " + chosen.versionName)
                .setMessage("安装包约 " + size(chosen.size) + "。下载后校验完整性和应用签名，安装时仍需你在 Android 中确认。")
                .setPositiveButton("开始下载", (d, w) -> download(chosen)).setNegativeButton("取消", null).show();
    }

    private void download(UpdateManifest chosen) {
        if (busy) return;
        UpdateTransport transfer = begin("正在准备下载…");
        progress.setProgress(0); progress.setVisibility(View.VISIBLE);
        worker.execute(() -> {
            File candidate = null;
            try {
                candidate = transfer.download(chosen, new File(getCacheDir(), "updates"), (count, total) -> ui(() -> {
                    progress.setProgress((int) (count * 100 / total));
                    status.setText("下载 " + chosen.versionName + " · " + count * 100 / total + "%\n" + size(count) + " / " + size(total));
                }));
                verifyPackage(candidate, chosen);
                File verified = candidate;
                ui(() -> {
                    if (transfer.isCancelled()) { status.setText("已取消，可重新下载。"); return; }
                    ready = verified; status.setText("已下载 " + chosen.versionName + "\n完整性、包名、版本与签名核对通过，可以安装。");
                });
            } catch (Exception error) {
                if (candidate != null) candidate.delete();
                failure(error);
            } finally { done(); }
        });
    }

    private void prepareInstall() {
        if (busy || ready == null || release == null) return;
        if (!mayInstall()) return;
        File file = ready; UpdateManifest chosen = release;
        UpdateTransport transfer = begin("正在复核安装包…");
        worker.execute(() -> {
            try {
                transfer.verifyDigest(file, chosen); verifyPackage(file, chosen);
                ui(() -> { if (!transfer.isCancelled() && mayInstall()) launchInstaller(file); });
            } catch (Exception error) { file.delete(); ui(() -> ready = null); failure(error); }
            finally { done(); }
        });
    }

    private boolean mayInstall() {
        if (RuntimeService.active || GameAcquisitionService.active) {
            status.setText("请先保存并退出游戏、结束游戏文件安装，再回来安装更新。已下载的安装包会保留。"); return false;
        }
        return true;
    }

    private void verifyPackage(File file, UpdateManifest chosen) throws Exception {
        com.android.apksig.ApkVerifier.Result result = new com.android.apksig.ApkVerifier.Builder(file)
                .setMinCheckedPlatformVersion(Build.VERSION.SDK_INT).setMaxCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .build().verify();
        if (!result.isVerified()) throw new IOException("signature");
        PackageManager pm = getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo apk = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        PackageInfo current = pm.getPackageInfo(getPackageName(), flags);
        if (apk == null || !getPackageName().equals(apk.packageName)) throw new IOException("package");
        if (code(apk) != chosen.versionCode || !chosen.versionName.equals(apk.versionName)
                || code(apk) <= code(current)) throw new IOException("version");
        if (!signers(apk).equals(signers(current))) throw new IOException("signature");
    }

    private static Set<String> signers(PackageInfo info) throws Exception {
        Signature[] signatures = Build.VERSION.SDK_INT >= 28
                ? (info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners()) : info.signatures;
        if (signatures == null || signatures.length == 0) throw new IOException("signature");
        Set<String> result = new HashSet<>();
        for (Signature signature : signatures) result.add(UpdateTransport.hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())));
        return result;
    }

    private void launchInstaller(File file) {
        try {
            if (!getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(this).setTitle("允许安装游戏更新")
                        .setMessage("请在系统设置中允许“宝可梦荒野”安装应用，返回后再点“安装已下载的更新”。")
                        .setPositiveButton("打开系统设置", (d, w) -> {
                            try { startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))); }
                            catch (android.content.ActivityNotFoundException error) { status.setText("无法打开系统设置，请在 Android 设置中允许此应用安装更新。"); }
                        }).setNegativeButton("取消", null).show(); return;
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".updates", file);
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("游戏更新", uri));
            startActivity(intent);
            status.setText("已交给 Android 安装器。取消安装后仍可再次点击安装。");
        } catch (Exception error) { status.setText("无法打开系统安装器，请稍后重试。"); }
    }

    private static String size(long bytes) { return String.format(Locale.CHINA, "%.1f MB", bytes / (1024.0 * 1024.0)); }
    private void failure(Exception error) {
        String reason = transport != null && transport.isCancelled() ? "cancelled" : error.getMessage();
        String text = "cancelled".equals(reason) ? "已取消，可重新检查或下载。"
                : "storage".equals(reason) ? "存储空间不足或无法写入，请清理空间后重试。"
                : "HTTP 404".equals(reason) ? "暂未找到更新信息，请稍后重试。"
                : error instanceof java.net.SocketTimeoutException ? "网络连接超时，请检查网络后重试。"
                : "address".equals(reason) || "manifest".equals(reason) || error instanceof org.json.JSONException
                        ? "更新信息暂时无效，请稍后重试。"
                : "digest".equals(reason) || "size".equals(reason) || "signature".equals(reason)
                        || "package".equals(reason) || "version".equals(reason)
                        ? "安装包校验未通过，已停止安装，请重新下载。"
                : "更新未完成，请检查网络和存储空间后重试。";
        ui(() -> status.setText(text));
    }

    @Override public void onBackPressed() { leave(); }
    private void leave() {
        if (!busy) { finish(); return; }
        new AlertDialog.Builder(this).setTitle("停止本次更新？").setMessage("离开将取消当前检查或下载，游戏存档不受影响。")
                .setPositiveButton("停止并返回", (d, w) -> finish()).setNegativeButton("继续等待", null).show();
    }
    @Override protected void onDestroy() {
        destroyed = true; if (transport != null) transport.cancel(); worker.shutdownNow(); super.onDestroy();
    }
}
