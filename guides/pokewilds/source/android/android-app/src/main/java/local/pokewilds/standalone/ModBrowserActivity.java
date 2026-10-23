package local.pokewilds.standalone;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Browses app-private mods without exposing the game's files to other apps. */
public final class ModBrowserActivity extends Activity {
    private final ExecutorService files = Executors.newSingleThreadExecutor();
    private final List<ModManager.Entry> entries = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private Path directory = Paths.get("");
    private TextView pathView, statusView;
    private Button upButton, removeButton;
    private ListView listView;
    private BaseAdapter adapter;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 32, 24, 24);
        TextView title = new TextView(this);
        title.setText("Installed mod files"); title.setTextSize(24);
        layout.addView(title);
        TextView help = new TextView(this);
        help.setText("Open folders to find mod files. Select files or whole folders to remove. Different imports can share folders, so check their contents before removing a folder. Export mods ZIP from App settings first if you want a backup. Quit the game before making changes.");
        layout.addView(help);
        pathView = new TextView(this); pathView.setTextSize(18); layout.addView(pathView);
        upButton = new Button(this); upButton.setText("Up one folder");
        upButton.setOnClickListener(v -> openParent()); layout.addView(upButton);
        listView = new ListView(this);
        adapter = new ModAdapter(); listView.setAdapter(adapter);
        layout.addView(listView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        removeButton = new Button(this);
        removeButton.setOnClickListener(v -> confirmRemoval()); layout.addView(removeButton);
        statusView = new TextView(this); layout.addView(statusView);
        setContentView(layout);
        updateControls();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!busy) refreshEntries(null);
    }

    @Override public void onBackPressed() {
        if (!directory.toString().isEmpty() && !busy) openParent();
        else super.onBackPressed();
    }

    private void openParent() {
        if (busy || directory.toString().isEmpty()) return;
        directory = directory.getParent();
        if (directory == null) directory = Paths.get("");
        refreshEntries(null);
    }

    private void openFolder(String name) {
        if (busy) return;
        directory = directory.resolve(name);
        refreshEntries(null);
    }

    private void refreshEntries(String success) {
        busy = true;
        statusView.setText("Loading mod files…");
        updateControls();
        Path requested = directory;
        files.execute(() -> {
            List<ModManager.Entry> found = null;
            String error = null;
            if (RuntimeService.active) error = "Quit the game before managing mods.";
            else if (GameAcquisitionService.active) error = "Finish or cancel game setup before managing mods.";
            else if (!RuntimeService.DATA_LOCK.tryAcquire()) error = "Wait for game setup or another file transfer.";
            else try {
                found = ModManager.list(new File(getFilesDir(), "game").toPath(), requested);
            } catch (Exception failure) {
                error = "Could not list mods: " + failure.getMessage();
            } finally { RuntimeService.DATA_LOCK.release(); }
            List<ModManager.Entry> result = found;
            String message = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                if (message == null) {
                    entries.clear(); entries.addAll(result);
                    selected.clear(); adapter.notifyDataSetChanged();
                    statusView.setText(success != null ? success :
                        (entries.isEmpty() ? "No mod files in this folder." : "Select the files or folders you want to remove."));
                } else statusView.setText(message);
                updateControls();
            });
        });
    }

    private void confirmRemoval() {
        if (busy || selected.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (ModManager.Entry entry : entries) if (selected.contains(entry.name)) names.add(entry.name);
        StringBuilder detail = new StringBuilder("Remove these files or folders from mods/?\n\n");
        for (int i = 0; i < Math.min(names.size(), 6); i++) detail.append("• ").append(names.get(i)).append('\n');
        if (names.size() > 6) detail.append("…and ").append(names.size() - 6).append(" more\n");
        detail.append("\nFolders include all their contents. This cannot be undone.");
        new AlertDialog.Builder(this).setTitle("Remove selected mod files?").setMessage(detail.toString())
            .setNegativeButton("Keep files", null)
            .setPositiveButton("Remove", (dialog, which) -> remove(names)).show();
    }

    private void remove(List<String> names) {
        busy = true;
        statusView.setText("Removing selected mod files…");
        updateControls();
        Path requested = directory;
        files.execute(() -> {
            String error = null;
            if (RuntimeService.active) error = "Quit the game before managing mods.";
            else if (GameAcquisitionService.active) error = "Finish or cancel game setup before managing mods.";
            else if (!RuntimeService.DATA_LOCK.tryAcquire()) error = "Wait for game setup or another file transfer.";
            else try {
                ModManager.removeSelected(new File(getFilesDir(), "game").toPath(), requested, names);
            } catch (Exception failure) {
                error = "Could not remove mods: " + failure.getMessage();
            } finally { RuntimeService.DATA_LOCK.release(); }
            String message = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                if (message == null) refreshEntries("Selected mod files removed. Restart the game to apply changes.");
                else { statusView.setText(message); updateControls(); }
            });
        });
    }

    private void updateControls() {
        if (pathView == null) return;
        pathView.setText("mods/" + (directory.toString().isEmpty() ? "" : directory + "/"));
        upButton.setEnabled(!busy && !directory.toString().isEmpty());
        listView.setEnabled(!busy);
        removeButton.setText("Remove selected (" + selected.size() + ")");
        removeButton.setEnabled(!busy && !selected.isEmpty());
    }

    @Override protected void onDestroy() {
        files.shutdown();
        super.onDestroy();
    }

    private final class ModAdapter extends BaseAdapter {
        @Override public int getCount() { return entries.size(); }
        @Override public Object getItem(int position) { return entries.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout row;
            Button open;
            CheckBox check;
            if (recycled instanceof LinearLayout) {
                row = (LinearLayout) recycled;
                open = (Button) row.getChildAt(0);
                check = (CheckBox) row.getChildAt(1);
            } else {
                row = new LinearLayout(ModBrowserActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                open = new Button(ModBrowserActivity.this);
                row.addView(open, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                check = new CheckBox(ModBrowserActivity.this);
                row.addView(check);
            }
            ModManager.Entry entry = entries.get(position);
            open.setText((entry.directory ? "📁 " : entry.link ? "↗ " : "") + entry.name);
            open.setContentDescription(entry.directory ? "Open folder " + entry.name : "Select file " + entry.name);
            CheckBox selection = check;
            open.setOnClickListener(v -> {
                if (entry.directory) openFolder(entry.name);
                else selection.setChecked(!selection.isChecked());
            });
            check.setOnCheckedChangeListener(null);
            check.setChecked(selected.contains(entry.name));
            check.setContentDescription("Select " + entry.name + " for removal");
            check.setOnCheckedChangeListener((button, checked) -> {
                if (checked) selected.add(entry.name);
                else selected.remove(entry.name);
                updateControls();
            });
            return row;
        }
    }
}
