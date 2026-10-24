package local.pokewilds.standalone;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LauncherActivity extends Activity {
    public static final String MANAGE_SAVES_EXTRA = "local.pokewilds.standalone.MANAGE_SAVES";
    public static final String CHECK_UPDATES_EXTRA = "local.pokewilds.standalone.CHECK_UPDATES";
    private static boolean autoDownloadAttempted;
    private final Handler handler = new Handler();
    private TextView statusView;
    private LinearLayout acquisitionPanel;
    private LinearLayout gamePanel;
    private LinearLayout managementPanel;
    private Button downloadButton;
    private Button localZipButton;
    private Button localFolderButton;
    private Button cancelAcquisitionButton;
    private boolean openedGame;
    private boolean managementMode;
    private boolean manuallyStarted;
    private boolean gameStartRequested;
    private Spinner graphicsControl;
    private Spinner viewportControl;
    private Spinner touchControl;
    private CheckBox spritesPatchControl;
    private CheckBox hoohPatchControl;
    private CheckBox floorsPatchControl;
    private CheckBox eggsPatchControl;
    private CheckBox radialPatchControl;
    private CheckBox zoomPatchControl;
    private CheckBox mapPatchControl, promptsPatchControl;
    private final java.util.concurrent.ExecutorService files = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            boolean gameInstalled = GameInstaller.isInstalled(LauncherActivity.this);
            if (!gameInstalled) {
                showAcquisitionPanel();
                statusView.setText(GameAcquisitionService.status);
                boolean acquiring = GameAcquisitionService.active;
                downloadButton.setEnabled(!acquiring);
                localZipButton.setEnabled(true);
                localFolderButton.setEnabled(true);
                cancelAcquisitionButton.setEnabled(acquiring);
                if (!managementMode && "Game files ready".equals(GameAcquisitionService.status)) startGame();
            } else {
                showGamePanel();
                statusView.setText(RuntimeService.status);
                if (!managementMode && !gameStartRequested && !GameAcquisitionService.active) startGame();
            }
            if (openedGame && !RuntimeService.active && "Game closed".equals(RuntimeService.status)) {
                openedGame = false;
                manuallyStarted = false;
                gameStartRequested = false;
                if (!managementMode) { finishAndRemoveTask(); return; }
            }
            if (RuntimeService.displayReady && !openedGame && (!managementMode || manuallyStarted)) {
                openedGame = true;
                manuallyStarted = false;
                startActivity(new Intent(LauncherActivity.this, GameActivity.class));
            }
            updateRuntimeControls();
            handler.postDelayed(this, 500);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null){manuallyStarted=state.getBoolean("neo-start-pending");gameStartRequested=state.getBoolean("neo-game-requested");openedGame=state.getBoolean("neo-game-opened");}
        autoDownloadAttempted = getPreferences(MODE_PRIVATE).getBoolean("auto-download-attempted", false);
        managementMode = isManagementIntent(getIntent());
        if (RuntimeService.DATA_LOCK.tryAcquire()) {
            try {
                GameInstaller.discardInterruptedFiles(getFilesDir().toPath());
                ModManager.recover(getFilesDir().toPath().resolve("game"));
                SaveArchive.recoverInterruptedImport(getFilesDir().toPath().resolve("game"));
                SaveArchive.discardInterruptedFolderArchive(getFilesDir().toPath().resolve("game"));
            } catch (IOException error) {
                GameAcquisitionService.status = "Could not recover app files: " + error.getMessage();
            }
            finally { RuntimeService.DATA_LOCK.release(); }
        }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 32);
        TextView title = new TextView(this); title.setText("宝可梦荒野 · Neo"); title.setTextSize(30); layout.addView(title);
        Button neo = new Button(this); neo.setText("背屏伙伴 · 陪伴与互动"); neo.setOnClickListener(v -> startActivity(new Intent(this, NeoPetActivity.class))); layout.addView(neo);
        Button updates = new Button(this); updates.setText("检查游戏更新 · " + BuildConfig.VERSION_NAME);
        updates.setOnClickListener(v -> startActivity(new Intent(this, UpdateActivity.class))); layout.addView(updates);
        statusView = new TextView(this); statusView.setTextSize(18); statusView.setText(GameAcquisitionService.status); layout.addView(statusView);
        acquisitionPanel = new LinearLayout(this); acquisitionPanel.setOrientation(LinearLayout.VERTICAL);
        TextView acquisitionHelp = new TextView(this);
        acquisitionHelp.setText("首次启动将解压本地中文游戏；无需下载。");
        acquisitionPanel.addView(acquisitionHelp);
        downloadButton = new Button(this); downloadButton.setText("安装内置中文游戏");
        downloadButton.setOnClickListener(v -> startAcquisition(GameAcquisitionService.DOWNLOAD, null));
        acquisitionPanel.addView(downloadButton);
        localZipButton = new Button(this); localZipButton.setText("选择游戏 ZIP");
        localZipButton.setOnClickListener(v -> chooseLocalZip()); acquisitionPanel.addView(localZipButton);
        localFolderButton = new Button(this); localFolderButton.setText("选择游戏目录");
        localFolderButton.setOnClickListener(v -> chooseLocalFolder()); acquisitionPanel.addView(localFolderButton);
        cancelAcquisitionButton = new Button(this); cancelAcquisitionButton.setText("取消安装");
        cancelAcquisitionButton.setOnClickListener(v -> startAcquisition(GameAcquisitionService.CANCEL, null));
        acquisitionPanel.addView(cancelAcquisitionButton);
        TextView earlySavesHeading = new TextView(this); earlySavesHeading.setText("存档管理"); earlySavesHeading.setTextSize(20);
        acquisitionPanel.addView(earlySavesHeading);
        TextView earlySavesHelp = new TextView(this);
        earlySavesHelp.setText("可以先导入存档，再安装游戏。请先完成或取消当前安装。");
        acquisitionPanel.addView(earlySavesHelp);
        Button earlyImport = new Button(this); earlyImport.setText("导入存档 ZIP");
        earlyImport.setOnClickListener(v -> chooseSaveImport());
        acquisitionPanel.addView(earlyImport);
        Button earlyFolderImport = new Button(this); earlyFolderImport.setText("导入 .sav 存档目录");
        earlyFolderImport.setOnClickListener(v -> chooseSaveFolder());
        acquisitionPanel.addView(earlyFolderImport);
        layout.addView(acquisitionPanel);

        gamePanel = new LinearLayout(this); gamePanel.setOrientation(LinearLayout.VERTICAL);
        Button start = new Button(this); start.setText("开始 / 继续游戏");
        start.setOnClickListener(v -> { openedGame = false; manuallyStarted = true; startGame(); }); gamePanel.addView(start);
        Button forceStop = new Button(this); forceStop.setText("异常退出恢复");
        forceStop.setOnClickListener(v -> confirmForceStop()); gamePanel.addView(forceStop);
        Button quit = new Button(this); quit.setText("退出游戏");
        quit.setOnClickListener(v -> openGameForQuit()); gamePanel.addView(quit);
        managementPanel = new LinearLayout(this);
        managementPanel.setOrientation(LinearLayout.VERTICAL);
        addRuntimeControls(managementPanel);
        Button gameSettings = new Button(this); gameSettings.setText("游戏设置");
        gameSettings.setOnClickListener(v -> startActivity(new Intent(this, GameSettingsActivity.class)));
        managementPanel.addView(gameSettings);
        managementPanel.setVisibility(managementMode ? android.view.View.VISIBLE : android.view.View.GONE);
        gamePanel.addView(managementPanel);
        Button logs = new Button(this); logs.setText("查看启动日志");
        logs.setOnClickListener(v -> {
            String text;
            try {
                try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(new java.io.File(getFilesDir(), "session.log"), "r")) {
                    long length = file.length();
                    long from = Math.max(0, length - 12000);
                    file.seek(from);
                    byte[] bytes = new byte[(int) (length - from)];
                    file.readFully(bytes);
                    text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) { text = "No startup log yet."; }
            new android.app.AlertDialog.Builder(this).setTitle("启动日志").setMessage(text).setPositiveButton("关闭", null).show();
        }); gamePanel.addView(logs);
        TextView savesHeading = new TextView(this); savesHeading.setText("存档管理"); savesHeading.setTextSize(20);
        gamePanel.addView(savesHeading);
        TextView savesHelp = new TextView(this);
        savesHelp.setText("请先保存并退出游戏，再导出备份或导入 ZIP、.sav 存档目录。导入不会覆盖已有世界。");
        gamePanel.addView(savesHelp);
        Button export = new Button(this); export.setText("导出存档");
        export.setOnClickListener(v -> {
            if (RuntimeService.active) { RuntimeService.status = "Save and quit the game before exporting."; return; }
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"pokewilds-saves.zip"), 10);
        }); gamePanel.addView(export);
        Button importButton = new Button(this); importButton.setText("导入存档 ZIP");
        importButton.setOnClickListener(v -> chooseSaveImport()); gamePanel.addView(importButton);
        Button importFolder = new Button(this); importFolder.setText("导入 .sav 存档目录");
        importFolder.setOnClickListener(v -> chooseSaveFolder()); gamePanel.addView(importFolder);
        TextView modsHeading = new TextView(this); modsHeading.setText("模组"); modsHeading.setTextSize(20);
        gamePanel.addView(modsHeading);
        TextView modsHelp = new TextView(this);
        modsHelp.setText("请先退出游戏。导入模组会替换同路径文件，建议先导出备份；可在管理页面移除已安装的模组。");
        gamePanel.addView(modsHelp);
        Button importModsZip = new Button(this); importModsZip.setText("导入模组 ZIP");
        importModsZip.setOnClickListener(v -> chooseModsZip()); gamePanel.addView(importModsZip);
        Button importModsFolder = new Button(this); importModsFolder.setText("导入模组目录");
        importModsFolder.setOnClickListener(v -> chooseModsFolder()); gamePanel.addView(importModsFolder);
        Button exportMods = new Button(this); exportMods.setText("导出模组 ZIP");
        exportMods.setOnClickListener(v -> chooseModsExport()); gamePanel.addView(exportMods);
        Button browseMods = new Button(this); browseMods.setText("管理已安装模组");
        browseMods.setOnClickListener(v -> {
            if (RuntimeService.active || GameAcquisitionService.active) {
                setSaveStatus("Quit the game and finish setup before managing mods.");
                return;
            }
            startActivity(new Intent(this, ModBrowserActivity.class));
        }); gamePanel.addView(browseMods);
        addPatchControls(gamePanel);
        layout.addView(gamePanel);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout); setContentView(scroll);applyInsets(scroll);
        if (GameInstaller.isInstalled(this)) {
            showGamePanel();
            if (!managementMode && !GameAcquisitionService.active) startGame();
        } else {
            showAcquisitionPanel();
            maybeStartAutoDownload();
        }
        if (state == null && updateIntent(getIntent())) startActivity(new Intent(this, UpdateActivity.class));
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == 12 || request == 13) {
            android.net.Uri source = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(source,
                    data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (SecurityException ignored) { /* This provider did not offer persistent access. */ }
            startAcquisition(request == 12 ? GameAcquisitionService.LOCAL_ZIP : GameAcquisitionService.LOCAL_FOLDER, source);
            return;
        }
        if (request == 14 || request == 15 || request == 16) {
            if (RuntimeService.active) { setSaveStatus("Quit the game before managing mods."); return; }
            setSaveStatus(request == 16 ? "Exporting mods…" : "Importing mods…");
            android.net.Uri selected = data.getData();
            files.submit(() -> {
                if (!RuntimeService.DATA_LOCK.tryAcquire()) { setSaveStatus("Wait for game setup or quit the game before managing mods."); return; }
                try {
                    java.nio.file.Path game = new java.io.File(getFilesDir(), "game").toPath();
                    if (request == 14) {
                        try (java.io.InputStream input = getContentResolver().openInputStream(selected)) {
                            if (input == null) throw new IOException("Could not open mod ZIP");
                            ModManager.importZip(input, game);
                        }
                    } else if (request == 15) {
                        ModManager.importFolder(this, selected, game);
                    } else {
                        try (java.io.OutputStream output = getContentResolver().openOutputStream(selected)) {
                            if (output == null) throw new IOException("Could not create mod ZIP");
                            ModManager.exportZip(game, output);
                        }
                    }
                    setSaveStatus(request == 16 ? "Mods exported" : "Mods installed; restart the game to load them");
                } catch (Exception error) { setSaveStatus("Mod transfer failed: " + error.getMessage()); }
                finally { RuntimeService.DATA_LOCK.release(); }
            });
            return;
        }
        if (request != 10 && request != 11 && request != 17) return;
        if (RuntimeService.active) { setSaveStatus("Quit the game first."); return; }
        setSaveStatus(request == 10 ? "Exporting saves…" : "Importing saves…");
        Uri selected = data.getData();
        if (request == 17) {
            try {
                getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { /* Provider grants access only for this selection. */ }
        }
        files.submit(() -> {
            if (!RuntimeService.DATA_LOCK.tryAcquire()) { setSaveStatus("Wait for game setup or quit the game before transferring saves."); return; }
            try {
                java.nio.file.Path game = new java.io.File(getFilesDir(), "game").toPath();
                if (request == 10) {
                    try (java.io.OutputStream output = getContentResolver().openOutputStream(selected)) {
                        if (output == null) throw new IOException("Could not create save ZIP");
                        SaveArchive.exportTo(game, output);
                    }
                } else if (request == 17) {
                    importSelectedWorldFolder(selected, game);
                } else {
                    try (java.io.InputStream input = getContentResolver().openInputStream(selected)) {
                        if (input == null) throw new IOException("Could not open save ZIP");
                        SaveArchive.importFrom(input, game);
                    }
                }
                setSaveStatus(request == 10 ? "Saves exported" : "Saves imported");
            } catch (Exception e) { setSaveStatus("Save transfer failed: " + e.getMessage()); }
            finally { RuntimeService.DATA_LOCK.release(); }
        });
    }
    @Override protected void onSaveInstanceState(Bundle state){
        state.putBoolean("neo-start-pending",manuallyStarted);state.putBoolean("neo-game-requested",gameStartRequested);state.putBoolean("neo-game-opened",openedGame);super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { files.shutdown(); super.onDestroy(); }
    private void applyInsets(android.view.View view){
        if(android.os.Build.VERSION.SDK_INT>=35){
            getWindow().setDecorFitsSystemWindows(false);
            view.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return android.view.WindowInsets.CONSUMED;});
            view.requestApplyInsets();
        }
    }
    private void startGame() {
        if (!GameInstaller.isInstalled(this)) return;
        gameStartRequested = true;
        startForegroundService(new Intent(this, RuntimeService.class));
    }

    private void chooseLocalZip() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, 12);
    }

    private void chooseSaveImport() {
        if (RuntimeService.active) { setSaveStatus("Quit the game before importing."); return; }
        if (GameAcquisitionService.active) { setSaveStatus("Cancel or finish game setup before importing saves."); return; }
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip")
            .addCategory(Intent.CATEGORY_OPENABLE), 11);
    }

    private void chooseSaveFolder() {
        if (RuntimeService.active) { setSaveStatus("Quit the game before importing."); return; }
        if (GameAcquisitionService.active) { setSaveStatus("Cancel or finish game setup before importing saves."); return; }
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), 17);
    }

    private void importSelectedWorldFolder(Uri tree, java.nio.file.Path game) throws IOException {
        try {
            String rootId = DocumentsContract.getTreeDocumentId(tree);
            Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId);
            String[] rootColumns = {DocumentsContract.Document.COLUMN_DISPLAY_NAME};
            String worldName;
            try (Cursor cursor = getContentResolver().query(root, rootColumns, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) throw new IOException("Could not read selected world folder");
                worldName = cursor.getString(0);
            }
            Map<String, SaveArchive.InputOpener> files = new LinkedHashMap<>();
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId);
            String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
            try (Cursor cursor = getContentResolver().query(children, columns, null, null, null)) {
                if (cursor == null) throw new IOException("Could not list selected world folder");
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0), name = cursor.getString(1), mime = cursor.getString(2);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                        throw new IOException("Select the .sav world folder itself, containing ZIP and PNG files");
                    }
                    if (files.containsKey(name)) throw new IOException("Duplicate world file: " + name);
                    Uri document = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    files.put(name, () -> getContentResolver().openInputStream(document));
                }
            }
            SaveArchive.importWorldFiles(worldName, files, game);
        } catch (SecurityException error) {
            throw new IOException("World folder access was lost", error);
        }
    }

    private void chooseModsZip() {
        if (RuntimeService.active) { setSaveStatus("Quit the game before managing mods."); return; }
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip")
            .addCategory(Intent.CATEGORY_OPENABLE), 14);
    }

    private void chooseModsFolder() {
        if (RuntimeService.active) { setSaveStatus("Quit the game before managing mods."); return; }
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), 15);
    }

    private void chooseModsExport() {
        if (RuntimeService.active) { setSaveStatus("Quit the game before managing mods."); return; }
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip")
            .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "pokewilds-mods.zip"), 16);
    }

    private void setSaveStatus(String value) {
        RuntimeService.status = value;
        GameAcquisitionService.status = value;
    }

    private void chooseLocalFolder() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, 13);
    }

    private void startAcquisition(String action, android.net.Uri source) {
        if (GameAcquisitionService.DOWNLOAD.equals(action)) markDownloadAttempted();
        Intent intent = new Intent(this, GameAcquisitionService.class).setAction(action);
        if (source != null) intent.setData(source).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (GameAcquisitionService.CANCEL.equals(action)) startService(intent);
        else startForegroundService(intent);
        showAcquisitionPanel();
        if (GameAcquisitionService.CANCEL.equals(action)) statusView.setText("Cancelling game file setup…");
        else statusView.setText(source == null ? "Starting game file setup…" : "Importing local game files…");
    }

    private void showAcquisitionPanel() {
        if (acquisitionPanel != null) acquisitionPanel.setVisibility(android.view.View.VISIBLE);
        if (gamePanel != null) gamePanel.setVisibility(android.view.View.GONE);
    }

    private void markDownloadAttempted() {
        autoDownloadAttempted = true;
        getPreferences(MODE_PRIVATE).edit().putBoolean("auto-download-attempted", true).apply();
    }

    private void maybeStartAutoDownload() {
        if (GameAcquisitionService.active) { markDownloadAttempted(); return; }
        if (autoDownloadAttempted) return;
        if (new java.io.File(getFilesDir(), "game/pokewilds.jar").isFile()) {
            markDownloadAttempted();
            GameAcquisitionService.status = "Existing game files differ from the official release. Choose 安装内置中文游戏 or local files to replace them; saves and settings will be kept.";
            return;
        }
        startAcquisition(GameAcquisitionService.DOWNLOAD, null);
    }

    private void showGamePanel() {
        if (acquisitionPanel != null) acquisitionPanel.setVisibility(android.view.View.GONE);
        if (gamePanel != null) gamePanel.setVisibility(android.view.View.VISIBLE);
    }

    private void addRuntimeControls(LinearLayout layout) {
        RuntimeOptions options = RuntimeOptions.read(this);
        TextView heading = new TextView(this); heading.setText("Runtime settings"); heading.setTextSize(20); layout.addView(heading);
        TextView graphicsLabel = new TextView(this); graphicsLabel.setText("Graphics profile"); layout.addView(graphicsLabel);
        graphicsControl = new Spinner(this);
        graphicsControl.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, RuntimeOptions.GRAPHICS_LABELS));
        graphicsControl.setSelection(RuntimeOptions.graphicsIndex(options.graphics));
        graphicsControl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (RuntimeService.active) {
                    graphicsControl.setSelection(RuntimeOptions.graphicsIndex(RuntimeOptions.read(LauncherActivity.this).graphics));
                    RuntimeService.status = "Quit the game before changing runtime settings.";
                    return;
                }
                RuntimeOptions current = RuntimeOptions.read(LauncherActivity.this);
                RuntimeOptions.save(LauncherActivity.this, RuntimeOptions.GRAPHICS_VALUES[position], current.autoViewport ? 0 : current.width, current.autoViewport ? 0 : current.height);
            }
        });
        layout.addView(graphicsControl);
        TextView viewportLabel = new TextView(this); viewportLabel.setText("Viewport"); layout.addView(viewportLabel);
        viewportControl = new Spinner(this);
        String[] viewports = new String[RuntimeOptions.VIEWPORT_WIDTHS.length];
        for (int i = 0; i < viewports.length; i++) viewports[i] = RuntimeOptions.viewportLabel(i);
        viewportControl.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, viewports));
        viewportControl.setSelection(RuntimeOptions.viewportIndex(options));
        viewportControl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (RuntimeService.active) {
                    RuntimeOptions current = RuntimeOptions.read(LauncherActivity.this);
                    viewportControl.setSelection(RuntimeOptions.viewportIndex(current));
                    RuntimeService.status = "Quit the game before changing runtime settings.";
                    return;
                }
                RuntimeOptions current = RuntimeOptions.read(LauncherActivity.this);
                RuntimeOptions.save(LauncherActivity.this, current.graphics,
                    RuntimeOptions.VIEWPORT_WIDTHS[position], RuntimeOptions.VIEWPORT_HEIGHTS[position]);
            }
        });
        layout.addView(viewportControl);
        TextView touchLabel = new TextView(this); touchLabel.setText("On-screen controls"); layout.addView(touchLabel);
        touchControl = new Spinner(this);
        touchControl.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
            TouchControls.TOUCH_MODE_LABELS));
        touchControl.setSelection(TouchControls.readTouchMode(this));
        touchControl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (RuntimeService.active) {
                    touchControl.setSelection(TouchControls.readTouchMode(LauncherActivity.this));
                    RuntimeService.status = "Quit the game before changing runtime settings.";
                    return;
                }
                TouchControls.saveTouchMode(LauncherActivity.this, position);
            }
        });
        layout.addView(touchControl);
        TextView keyboardLabel = new TextView(this);
        keyboardLabel.setText("Show keyboard with gamepad"); layout.addView(keyboardLabel);
        Spinner keyboardControl = new Spinner(this);
        keyboardControl.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
            KeyboardShortcut.LABELS));
        keyboardControl.setSelection(KeyboardShortcut.indexOf(KeyboardShortcut.read(this)));
        keyboardControl.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                KeyboardShortcut.save(LauncherActivity.this, KeyboardShortcut.CHOICES[position]);
            }
        });
        layout.addView(keyboardControl);
        TextView keyboardHelp = new TextView(this);
        keyboardHelp.setText("On-screen controls use R2 when the selected button is not shown. Off disables the shortcut for both touch and gamepad.");
        layout.addView(keyboardHelp);
        updateRuntimeControls();
    }

    private void updateRuntimeControls() {
        if (graphicsControl == null) return;
        boolean enabled = !RuntimeService.active;
        graphicsControl.setEnabled(enabled);
        viewportControl.setEnabled(enabled);
        touchControl.setEnabled(enabled);
        boolean patchesEnabled = enabled && !GameAcquisitionService.active;
        if (spritesPatchControl != null) spritesPatchControl.setEnabled(patchesEnabled);
        if (hoohPatchControl != null) hoohPatchControl.setEnabled(patchesEnabled);
        if (floorsPatchControl != null) floorsPatchControl.setEnabled(patchesEnabled);
        if (eggsPatchControl != null) eggsPatchControl.setEnabled(patchesEnabled);
        if (radialPatchControl != null) radialPatchControl.setEnabled(patchesEnabled);
        if (zoomPatchControl != null) zoomPatchControl.setEnabled(patchesEnabled);
        if (mapPatchControl != null) mapPatchControl.setEnabled(patchesEnabled);
        if (promptsPatchControl != null) promptsPatchControl.setEnabled(patchesEnabled);
    }

    private void addPatchControls(LinearLayout layout) {
        PatchOptions options = PatchOptions.read(this);
        TextView heading = new TextView(this); heading.setText("Patches"); heading.setTextSize(20); layout.addView(heading);
        TextView help = new TextView(this);
        help.setText("All built-in patches are enabled by default. Changes take effect the next time the game starts.");
        layout.addView(help);
        spritesPatchControl = patchCheckbox("Directional sprites", "Fixes incorrect Cut, Ride, and Build facing directions with some mods.",
            options.sprites, PatchOptions.SPRITES);
        hoohPatchControl = patchCheckbox("Ho-Oh", "Fixes nearby Pokémon freezing when near Ho-Oh.", options.hooh, PatchOptions.HOOH);
        floorsPatchControl = patchCheckbox("Separate floor occupancy", "Allows Pokémon to occupy overlapping tiles on different building floors.",
            options.floors, PatchOptions.FLOORS);
        eggsPatchControl = patchCheckbox("Egg floor saving", "Saves eggs on the floor where they were laid.",
            options.eggs, PatchOptions.EGGS);
        radialPatchControl = patchCheckbox("Field move wheel", "Hold L2, choose a field move by analog stick or touch, then release to use it.",
            options.radial, PatchOptions.RADIAL);
        zoomPatchControl = patchCheckbox("Shoulder zoom", "Use L1/R1 to zoom in the world or the map; BUILD/DIG still cycle materials. Auto Fit uses pixel-aligned scaling for crisp zoom and may use more power.",
            options.zoom, PatchOptions.ZOOM);
        mapPatchControl = patchCheckbox("Select map shortcut", "Press Select to open the map during gameplay. Select or B closes it and returns to gameplay.", options.map, PatchOptions.MAP);
        promptsPatchControl = patchCheckbox("Controller button prompts", "Show A/B, L1/R1, Start, and D-pad in built-in game instructions. Start also confirms nickname and sign text; typing still uses a keyboard.", options.prompts, PatchOptions.PROMPTS);
        addPatchSubheading(layout, "Fixes");
        layout.addView(spritesPatchControl);
        layout.addView(hoohPatchControl);
        layout.addView(eggsPatchControl);
        addPatchSubheading(layout, "Enhancements");
        layout.addView(radialPatchControl);
        layout.addView(zoomPatchControl);
        layout.addView(mapPatchControl);
        layout.addView(promptsPatchControl);
        layout.addView(floorsPatchControl);
        TextView floorWarning = new TextView(this);
        floorWarning.setText("SAVE COMPATIBILITY: When Pokémon share coordinates on different floors, this patch adds exact placements to the world save. Older PokeWilds versions read only a legacy copy, where some Pokémon may be moved or absent. Saving there removes the exact placements. Export a backup and keep this patch on for affected worlds.");
        floorWarning.setPadding(24, 4, 24, 12);
        layout.addView(floorWarning);
        updateRuntimeControls();
    }

    private void addPatchSubheading(LinearLayout layout, String title) {
        TextView subheading = new TextView(this);
        subheading.setText(title);
        subheading.setTextSize(18);
        subheading.setTypeface(null, android.graphics.Typeface.BOLD);
        subheading.setPadding(0, 16, 0, 4);
        layout.addView(subheading);
    }

    private CheckBox patchCheckbox(String label, String description, boolean checked, String patch) {
        CheckBox checkbox = new CheckBox(this);
        android.text.SpannableString text = new android.text.SpannableString(label + " — " + description);
        text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0, label.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        checkbox.setText(text);
        LinearLayout.LayoutParams spacing = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        spacing.bottomMargin = Math.round(10 * getResources().getDisplayMetrics().density);
        checkbox.setLayoutParams(spacing);
        checkbox.setChecked(checked);
        checkbox.setOnCheckedChangeListener((button, value) -> {
            if (PatchOptions.read(LauncherActivity.this).isEnabled(patch) == value) return;
            if (RuntimeService.active || GameAcquisitionService.active) {
                button.setChecked(PatchOptions.read(LauncherActivity.this).isEnabled(patch));
                RuntimeService.status = "Finish setup and quit the game before changing patches.";
                return;
            }
            if (PatchOptions.FLOORS.equals(patch)) {
                if (value) {
                    new android.app.AlertDialog.Builder(this).setTitle("Floor save compatibility")
                        .setMessage("This patch saves exact Pokémon placements when floors overlap. Older PokeWilds versions may show those Pokémon at moved positions or omit them. Saving there removes the exact placements. Export your worlds before using them in an older version.")
                        .setPositiveButton("Enable patch", (dialog, which) -> PatchOptions.save(this, patch, true))
                        .setNegativeButton("Keep off", (dialog, which) -> button.setChecked(false))
                        .setOnCancelListener(dialog -> button.setChecked(false)).show();
                    return;
                }
                String enhanced;
                try { enhanced = FloorSaveCompatibility.firstEnhancedWorld(new java.io.File(getFilesDir(), "game")); }
                catch (IOException e) { enhanced = "a world whose saves could not be inspected"; }
                if (enhanced != null) {
                    new android.app.AlertDialog.Builder(this).setTitle("This save needs the floor patch")
                        .setMessage(enhanced + " contains exact floor placements. The launcher will block game startup while this patch is off. Turn it back on to play that world, or export and remove the world before starting without it.")
                        .setPositiveButton("Turn off for now", (dialog, which) -> PatchOptions.save(this, patch, false))
                        .setNegativeButton("Keep enabled", (dialog, which) -> button.setChecked(true))
                        .setOnCancelListener(dialog -> button.setChecked(true)).show();
                    return;
                }
            }
            PatchOptions.save(LauncherActivity.this, patch, value);
        });
        return checkbox;
    }
    private void openGameForQuit() {
        if (!RuntimeService.active) { RuntimeService.status = "No game is running."; return; }
        if (!RuntimeService.displayReady) { RuntimeService.status = "Game display is not ready yet."; return; }
        openedGame = true;
        startService(new Intent(this, RuntimeService.class).setAction(RuntimeService.QUIT));
        startActivity(new Intent(this, GameActivity.class));
    }
    private void confirmForceStop() {
        if (!RuntimeService.active) { RuntimeService.status = "No game is running."; return; }
        new android.app.AlertDialog.Builder(this).setTitle("Force-stop recovery?")
            .setMessage("Use this only when normal Quit cannot close the game. Unsaved progress may be lost.")
            .setPositiveButton("Force-stop", (d, w) -> {
                RuntimeService.status = "Force-stopping game…";
                startService(new Intent(this, RuntimeService.class).setAction(RuntimeService.STOP));
            })
            .setNegativeButton("Keep playing", null).show();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        managementMode = isManagementIntent(intent);
        managementPanel.setVisibility(managementMode ? android.view.View.VISIBLE : android.view.View.GONE);
        manuallyStarted = false;
        openedGame = false;
        gameStartRequested = false;
        if (!managementMode && GameInstaller.isInstalled(this) && !GameAcquisitionService.active) startGame();
        else if (!GameInstaller.isInstalled(this)) {
            maybeStartAutoDownload();
        }
        if (updateIntent(intent)) startActivity(new Intent(this, UpdateActivity.class));
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }

    private static boolean isManagementIntent(Intent intent) {
        return intent != null && (intent.getBooleanExtra(MANAGE_SAVES_EXTRA, false)
            || "true".equalsIgnoreCase(intent.getStringExtra(MANAGE_SAVES_EXTRA)) || updateIntent(intent));
    }
    private static boolean updateIntent(Intent intent) {
        return intent != null && (intent.getBooleanExtra(CHECK_UPDATES_EXTRA, false)
            || "true".equalsIgnoreCase(intent.getStringExtra(CHECK_UPDATES_EXTRA)));
    }
}
