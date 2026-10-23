package local.pokewilds.standalone;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.*;

/** Foreground acquisition worker shared by network and local-file sources. */
public final class GameAcquisitionService extends Service {
    static final String DOWNLOAD = "local.pokewilds.standalone.DOWNLOAD_GAME";
    static final String LOCAL_ZIP = "local.pokewilds.standalone.LOCAL_GAME_ZIP";
    static final String LOCAL_FOLDER = "local.pokewilds.standalone.LOCAL_GAME_FOLDER";
    static final String CANCEL = "local.pokewilds.standalone.CANCEL_GAME_ACQUISITION";
    static volatile String status = "Game files are needed";
    static volatile boolean active;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile Future<?> job;
    private volatile long generation;

    @Override public void onCreate() {
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel("acquisition", "PokeWilds game download", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) { stopSelfResult(startId); return START_NOT_STICKY; }
        long requested = ++generation;
        Future<?> prior = job;
        if (prior != null) prior.cancel(true);
        if (CANCEL.equals(action)) {
            status = "Game file transfer cancelled"; active = false;
            stopForeground(true); stopSelfResult(startId);
            return START_NOT_STICKY;
        }
        if (!DOWNLOAD.equals(action) && !LOCAL_ZIP.equals(action) && !LOCAL_FOLDER.equals(action)) {
            stopSelfResult(startId); return START_NOT_STICKY;
        }
        Intent cancel = new Intent(this, GameAcquisitionService.class).setAction(CANCEL);
        Notification note = new Notification.Builder(this, "acquisition")
            .setContentTitle("Preparing PokeWilds")
            .setContentText("Verifying game files")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, LauncherActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(new Notification.Action.Builder(null, "Cancel",
                PendingIntent.getService(this, 1, cancel, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)).build())
            .build();
        startForeground(101, note);
        status = DOWNLOAD.equals(action) ? "Starting game download…" : "Reading selected game files…";
        active = true;
        Uri sourceUri = intent.getData();
        GameInstaller.Source source = DOWNLOAD.equals(action) ? GameInstaller.Source.DOWNLOAD
            : LOCAL_ZIP.equals(action) ? GameInstaller.Source.ZIP : GameInstaller.Source.FOLDER;
        job = worker.submit(() -> acquire(source, sourceUri, requested, startId));
        return START_NOT_STICKY;
    }
    private void acquire(GameInstaller.Source source, Uri uri, long requested, int startId) {
        boolean locked = false;
        try {
            RuntimeService.DATA_LOCK.acquire(); locked = true;
            GameInstaller.install(this, source, uri, value -> {
                if (generation == requested) status = value;
            }, () -> generation != requested || Thread.currentThread().isInterrupted());
            if (generation == requested) status = "Game files ready";
        } catch (InterruptedException cancelled) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            if (generation == requested) {
                status = "Game file setup failed: " + error.getMessage();
                try (PrintWriter log = new PrintWriter(new FileOutputStream(new File(getFilesDir(), "acquisition.log"), true))) {
                    error.printStackTrace(log);
                } catch (Exception ignored) { }
            }
        } finally {
            if (locked) RuntimeService.DATA_LOCK.release();
            if (generation == requested) {
                active = false;
                stopForeground(true);
                stopSelfResult(startId);
            }
        }
    }
    @Override public void onDestroy() {
        generation++;
        Future<?> prior = job;
        if (prior != null) prior.cancel(true);
        worker.shutdownNow();
        active = false;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
