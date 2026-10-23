package local.pokewilds.standalone;

import android.app.*;
import android.content.Intent;
import android.os.IBinder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;

/** Owns all processes for one game session; no commands are accepted from intents. */
public final class RuntimeService extends Service {
    public static final String QUIT = "local.pokewilds.standalone.QUIT";
    public static final String STOP = "local.pokewilds.standalone.STOP";
    static volatile RuntimeService instance;
    static volatile String status = "Ready";
    static volatile boolean running;
    static volatile boolean active;
    static volatile boolean displayFocused;
    static volatile boolean displayReady;
    static volatile boolean surfaceReady;
    static volatile boolean saveDialogVisible;
    static volatile int[] requestedViewport;
    static final Semaphore DATA_LOCK = new Semaphore(1);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Process> processes = Collections.synchronizedList(new ArrayList<>());
    private Future<?> session;
    private volatile File runtime;
    private File tmp, log, nativeDir;
    private RuntimeOptions options;
    private volatile boolean stopping;
    private volatile boolean forcedStop;

    @Override public void onCreate() {
        super.onCreate();instance=this;
        tmp = new File(getFilesDir(), "shared-tmp"); tmp.mkdirs();
        log = new File(getFilesDir(), "session.log");
        nativeDir = new File(getApplicationInfo().nativeLibraryDir);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("game", "PokeWilds session", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        Intent quit = new Intent(this, RuntimeService.class).setAction(QUIT);
        Notification note = new Notification.Builder(this, "game").setContentTitle("PokeWilds")
            .setContentText("Game session active").setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, LauncherActivity.class)
                .putExtra(LauncherActivity.MANAGE_SAVES_EXTRA, true), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(new Notification.Action.Builder(null, "Quit", PendingIntent.getService(this, 1, quit, PendingIntent.FLAG_IMMUTABLE)).build()).build();
        startForeground(100, note);
        if (intent != null && STOP.equals(intent.getAction())) {
            forcedStop = true; stopping = true; status = "Stopped without saving"; cleanup();
            if (!active) stopSelf();
        } else if (intent != null && QUIT.equals(intent.getAction())) {
            if (running) new Thread(this::requestQuit, "game-quit").start();
            else if (active) { stopping = true; status = "Cancelling startup…"; cleanup(); }
            else stopSelf();
        } else if (session == null || session.isDone()) {
            if (!DATA_LOCK.tryAcquire()) { status = "Save transfer in progress; try again when it finishes."; stopForeground(true); stopSelf(); return START_NOT_STICKY; }
            stopping = false; forcedStop = false; active = true; requestedViewport = null;
            session = worker.submit(this::runSession);
        }
        return START_NOT_STICKY;
    }
    private void runSession() {
        try {
            for(String transientName:new String[]{"neo-dialog.request","neo-dialog.response","neo-dialog.tmp","neo-dialog.answer.tmp"})new File(getFilesDir(),"game/"+transientName).delete();
            options = RuntimeOptions.read(this);
            PatchOptions patches = PatchOptions.read(this);
            try (PrintWriter out = new PrintWriter(log)) {
                out.println("PokeWilds 0.8.11; target SDK " + getApplicationInfo().targetSdkVersion
                    + "; graphics=" + options.graphics + "; viewport=" + options.width + "x" + options.height);
                out.println("Enabled game patches: " + (patches.enabledNames().isEmpty() ? "none" : String.join(", ", patches.enabledNames())));
            }
            if (!GameInstaller.isInstalled(this)) throw new IOException("Game files are not installed; open the launcher to download or select them");
            status = "Preparing bundled runtime files…";
            runtime = PayloadInstaller.install(this, () -> stopping || Thread.currentThread().isInterrupted());
            if (stopping) throw new InterruptedException("Startup cancelled");
            File game = new File(getFilesDir(), "game");
            SaveArchive.recoverInterruptedImport(game.toPath());
            File[] recovery=game.listFiles((dir,name)->name.endsWith(".neo-backup"));
            if(recovery!=null) for(File old:recovery) {File dest=new File(game,old.getName().replace(".neo-backup",".sav"));if(!dest.exists()) java.nio.file.Files.move(old.toPath(),dest.toPath());}
            ModManager.recover(game.toPath());
            if (!patches.floors) {
                String enhancedWorld = FloorSaveCompatibility.firstEnhancedWorld(game);
                if (enhancedWorld != null) throw new IOException(enhancedWorld + " needs Separate floor occupancy. Enable that patch to protect overlapping Pokémon, or export and remove the world before playing without it.");
            }
            for (String name : new String[]{"libproot.so", "libproot-loader.so", "libvirgl_test_server_android.so", "libpulseaudio.so"}) {
                if (!new File(nativeDir, name).isFile()) throw new IOException("Build is missing native runtime component: " + name);
            }
            SafeTar.deleteTree(tmp.toPath());
            if (!tmp.mkdirs()) throw new IOException("Cannot prepare session sockets");
            if (!new File(tmp, "hidden-input").mkdir()) throw new IOException("Cannot isolate game input devices");
            if (patches.anyEnabled()) stagePatchAgent();
            if (patches.controlsEnabled()) stageControlAgent();
            status = "Starting display…";
            Process x11 = start(Arrays.asList("/system/bin/app_process", "/", "com.termux.x11.CmdEntryPoint", ":0", "-ac", "-nolisten", "tcp"));
            waitForSocket(new File(tmp, ".X11-unix/X0"), x11);
            displayReady = true;
            status = "Starting GPU and audio…";
            if (!options.graphics.equals("software")) {
                List<String> gpuArgs = new ArrayList<>(Arrays.asList(new File(nativeDir, "libvirgl_test_server_android.so").toString(), "--no-fork", "--socket-path", new File(tmp, ".virgl_test").toString()));
                if (!options.graphics.equals("native")) gpuArgs.add("--" + options.graphics);
                Process gpu = start(gpuArgs);
                waitForSocket(new File(tmp, ".virgl_test"), gpu);
            }
            // Use a private UNIX socket shared with the guest, never a public audio port.
            // PRoot's guest/host credentials prevent Pulse's shared-memory
            // negotiation from agreeing. Keep audio on the private socket.
            Process pulse = start(Arrays.asList(new File(nativeDir, "libpulseaudio.so").toString(), "--daemonize=no", "--exit-idle-time=-1", "--disable-shm=yes", "--enable-memfd=no", "--use-pid-file=no", "--dl-search-path=" + new File(runtime, "host/lib/pulseaudio/modules"), "-n", "--load=module-native-protocol-unix socket=" + new File(tmp, "pulse-native") + " auth-anonymous=1", "--load=module-sles-sink"));
            waitForSocket(new File(tmp, "pulse-native"), pulse);
            // Attach the Android surface before GLFW chooses its initial size.
            // Opening it after the game starts clips the desktop menu.
            // Permission dialogs and Home can suspend the Activity. Count only
            // foreground time toward a broken-surface timeout, not user time.
            for (int i=0; i<120 && !surfaceReady && !stopping;) {
                if (!x11.isAlive()) throw new IOException("Display server stopped");
                if (displayFocused) i++;
                Thread.sleep(250);
            }
            if (!surfaceReady) throw new IOException("Display surface did not connect");
            status = "Starting PokeWilds…";
            List<String> gameCommand = new ArrayList<>(Arrays.asList("/usr/bin/java", "-Dorg.lwjgl.system.allocator=system", "-Dorg.lwjgl.glfw.window.fullscreen=true"));
            if (patches.anyEnabled()) {
                gameCommand.add("-javaagent:/tmp/bugfix.jar");
                gameCommand.addAll(patches.jvmArguments());
            }
            if (patches.controlsEnabled()) {
                gameCommand.add("-javaagent:/tmp/control-patches.jar");
                gameCommand.add("-Dcontrols.radial=" + patches.radial);
                gameCommand.add("-Dcontrols.zoom=" + patches.zoom);
                gameCommand.add("-Dcontrols.map=" + patches.map);
                gameCommand.add("-Dcontrols.prompts=" + patches.prompts);
                gameCommand.add("-Dcontrols.nativePixels=" + (patches.zoom && options.autoViewport));
            }
            gameCommand.addAll(Arrays.asList("-jar", "/game/pokewilds.jar"));
            Process gameProcess = start(guest(gameCommand));
            // Do not report ready just because Java exists: require the game's X11 window.
            boolean window = false;
            for (int i = 0; i < 60 && gameProcess.isAlive() && !stopping; i++) {
                Process probe = start(guest(Arrays.asList("/usr/bin/xdotool", "search", "--name", "^PokeWilds$")));
                if (probe.waitFor(2, TimeUnit.SECONDS) && probe.exitValue() == 0) { window = true; break; }
                probe.destroy(); Thread.sleep(500);
            }
            if (!window) throw new IOException("Game did not open a window; see startup log");
            int[] viewport = requestedViewport;
            if (viewport == null) viewport = new int[]{options.width, options.height};
            resizeGame(viewport);
            running = true; status = "Running";
            while (!gameProcess.waitFor(500, TimeUnit.MILLISECONDS)) {
                int[] next = requestedViewport;
                if (next != null && (next[0] != viewport[0] || next[1] != viewport[1]) && !stopping) {
                    try {
                        resizeGame(next);
                    } catch (IOException e) {
                        // A display change must not discard a running world's progress.
                        try (PrintWriter out = new PrintWriter(new FileOutputStream(log, true))) {
                            out.println("Viewport resize failed: " + e.getMessage());
                        }
                    }
                    viewport = next;
                }
            }
            int code = gameProcess.exitValue();
            status = forcedStop ? "Stopped without saving" : code == 0 ? "Game closed" : "Game exited with code " + code + ". See startup log.";
        } catch (Exception e) {
            status = forcedStop ? "Stopped without saving" : stopping ? "Startup cancelled" : "Unable to start: " + e.getMessage();
            try (PrintWriter out = new PrintWriter(new FileOutputStream(log, true))) { e.printStackTrace(out); } catch (IOException ignored) { }
        } finally {
            running = false; displayReady = false; surfaceReady = false; displayFocused = false;
            saveDialogVisible = false; active = false;
            cleanup(); DATA_LOCK.release(); stopForeground(true); stopSelf();
        }
    }
    private void stagePatchAgent() throws IOException {
        File agent = new File(tmp, "bugfix.jar");
        try (InputStream source = getAssets().open("bugfix.jar")) {
            Files.copy(source, agent.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try (JarFile jar = new JarFile(agent)) {
            java.util.jar.Manifest manifest = jar.getManifest();
            if (manifest == null || !"local.pokewilds.bugfix.BugFixAgent".equals(
                    manifest.getMainAttributes().getValue("Premain-Class")))
                throw new IOException("Bundled game patch agent has no expected entry point");
        }
    }
    private void stageControlAgent() throws IOException {
        File agent = new File(tmp, "control-patches.jar");
        try (InputStream source = getAssets().open("control-patches.jar")) {
            Files.copy(source, agent.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try (JarFile jar = new JarFile(agent)) {
            java.util.jar.Manifest manifest = jar.getManifest();
            if (manifest == null || !"com.pkmngen.game.OdinAgent".equals(
                    manifest.getMainAttributes().getValue("Premain-Class")))
                throw new IOException("Bundled controls agent has no expected entry point");
        }
        File controls = new File(tmp, "odin-controls");
        if (!controls.isDirectory() && !controls.mkdirs()) throw new IOException("Cannot prepare controls bridge");
        Files.write(new File(controls, "commands").toPath(), "RESET 0;\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.deleteIfExists(new File(controls, "state").toPath());
    }
    private void resizeGame(int[] size) throws IOException, InterruptedException {
        try (PrintWriter out = new PrintWriter(new FileOutputStream(log, true))) {
            out.println("Game viewport: " + size[0] + "x" + size[1]);
        }
        Process resize = start(guest(Arrays.asList("/bin/sh", "-c",
            "w=$(xdotool search --name '^PokeWilds$' | head -n1); "
            + "xdotool windowsize \"$w\" " + size[0] + " " + size[1]
            + " windowmove \"$w\" 0 0")));
        if (!resize.waitFor(5, TimeUnit.SECONDS)) {
            resize.destroy();
            throw new IOException("Game window resize timed out");
        }
        if (resize.exitValue() != 0) throw new IOException("Unable to resize game window");
    }
    private Process start(List<String> args) throws IOException {
        if (stopping) throw new IOException("Session stopped");
        ProcessBuilder b = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        Map<String,String> env = b.environment();
        env.put("HOME", getFilesDir().toString()); env.put("TMPDIR", tmp.toString()); env.put("XDG_RUNTIME_DIR", tmp.toString());
        env.put("CLASSPATH", getApplicationInfo().sourceDir);
        env.put("NEO_NATIVE_DIR",nativeDir.toString());
        env.put("POKEWILDS_NATIVE_DIR", nativeDir.toString());
        env.put("XKB_CONFIG_ROOT", new File(runtime, "rootfs/usr/share/X11/xkb").toString());
        String host = new File(runtime, "host").toString();
        if (args.get(0).equals(new File(nativeDir, "libvirgl_test_server_android.so").toString())) b.directory(new File(host));
        // app_process must use Android's framework libraries, not Termux's
        // namespace shims (notably libbinder_ndk/libandroid).
        env.put("LD_LIBRARY_PATH", args.get(0).equals("/system/bin/app_process") ? nativeDir.toString() :
            nativeDir + ":" + host + "/lib:" + host + "/lib/pulseaudio:" + host + "/opt/virglrenderer-android/lib:" + host + "/opt/angle-android/" + (options.graphics.equals("angle-vulkan") ? "vulkan" : "gl"));
        env.put("PREFIX", host);
        env.put("PULSE_CONFIG_PATH", host + "/etc/pulse");
        env.put("PULSE_RUNTIME_PATH", new File(getFilesDir(), "pulse").toString());
        env.put("PULSE_STATE_PATH", new File(getFilesDir(), "pulse-state").toString());
        env.put("PULSE_DLPATH", host + "/lib/pulseaudio/modules");
        env.put("PROOT_TMP_DIR", tmp.toString()); env.put("PROOT_LOADER", new File(nativeDir, "libproot-loader.so").toString());
        Process p = b.start(); processes.add(p); return p;
    }
    private List<String> guest(List<String> command) {
        // Hide raw controller devices from the guest. Android translates gamepad
        // input into X11 keys only while the game has focus, so a modal menu
        // cannot also move the character behind it.
        List<String> args = new ArrayList<>(Arrays.asList(new File(nativeDir, "libproot.so").toString(), "--kill-on-exit", "-0", "-r", new File(runtime, "rootfs").toString(), "-b", "/dev", "-b", new File(tmp, "hidden-input") + ":/dev/input", "-b", "/proc", "-b", "/sys", "-b", tmp + ":/tmp", "-b", new File(getFilesDir(), "game") + ":/game", "-w", "/game", "/usr/bin/env", "-i", "HOME=/root", "PATH=/usr/local/bin:/usr/bin:/bin", "TMPDIR=/tmp", "DISPLAY=:0", "XDG_RUNTIME_DIR=/tmp", "PULSE_SERVER=unix:/tmp/pulse-native", "ALSOFT_DRIVERS=pulse", "ALSOFT_LOGLEVEL=" + (BuildConfig.DEBUG ? "3" : "1"), "GALLIUM_DRIVER=" + (options.graphics.equals("software") ? "llvmpipe" : "virpipe"), "__GLX_VENDOR_LIBRARY_NAME=mesa", "MESA_GL_VERSION_OVERRIDE=3.3", "LANG=C.UTF-8"));
        args.addAll(command); return args;
    }
    private void waitForSocket(File socket, Process owner) throws Exception {
        for (int i=0; i<100; i++) {
            if (!owner.isAlive()) throw new IOException("Runtime server exited before readiness: " + socket.getName());
            try { if (android.system.OsConstants.S_ISSOCK(android.system.Os.stat(socket.toString()).st_mode)) return; }
            catch (android.system.ErrnoException notReady) { /* wait for the server to bind */ }
            Thread.sleep(100);
        }
        throw new IOException("Runtime server timed out: " + socket.getName());
    }
    private void requestQuit() {
        try {
            status = "Waiting for the game's save / quit dialog…";
            // WM_DELETE_WINDOW preserves the desktop game's own save/cancel handling.
            Process close = start(guest(Arrays.asList("/bin/sh", "-c", "w=$(xdotool search --name '^PokeWilds$' | head -n1); test -n \"$w\" && /usr/local/bin/pokewilds-close \"$w\"")));
            if (!close.waitFor(5, TimeUnit.SECONDS) || close.exitValue() != 0) {
                close.destroy();
                throw new IOException("Could not open the game's quit dialog");
            }
            saveDialogVisible = true;
            long showDeadline = android.os.SystemClock.elapsedRealtime() + 5000;
            boolean sawDialog = false;
            while (running && !stopping) {
                Process probe = start(guest(Arrays.asList("/bin/sh", "-c",
                    "/usr/bin/xdotool search --name '^WARNING$' >/dev/null 2>&1")));
                boolean visible = probe.waitFor(2, TimeUnit.SECONDS) && probe.exitValue() == 0;
                if (probe.isAlive()) probe.destroy();
                processes.remove(probe);
                if (visible) sawDialog = true;
                else if (sawDialog || android.os.SystemClock.elapsedRealtime() >= showDeadline) break;
                Thread.sleep(350);
            }
        } catch (Exception e) { status = "Quit request failed: " + e.getMessage(); }
        finally { saveDialogVisible = false; }
    }
    private void cleanup() {
        synchronized (processes) {
            for (int i=processes.size()-1; i>=0; i--) {
                Process p=processes.get(i);
                if (p.isAlive()) {
                    p.destroy();
                    try { if (!p.waitFor(500, TimeUnit.MILLISECONDS)) p.destroyForcibly(); }
                    catch (InterruptedException interrupted) { p.destroyForcibly(); Thread.currentThread().interrupt(); }
                }
            }
            processes.clear();
        }
    }
    @Override public void onDestroy() { stopping = true; cleanup(); worker.shutdownNow(); super.onDestroy();instance=null; }
    @Override public IBinder onBind(Intent intent) { return null; }
}
