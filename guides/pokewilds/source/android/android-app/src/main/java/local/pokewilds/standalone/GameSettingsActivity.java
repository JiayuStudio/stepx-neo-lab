package local.pokewilds.standalone;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Android form for the unchanged game's settings.txt values. */
public final class GameSettingsActivity extends Activity {
    private interface ValueControl { String value(); }

    private final Map<String, ValueControl> controls = new LinkedHashMap<>();
    private GameSettings document;
    private File settingsFile;
    private LinearLayout fields;
    private Button save;
    private TextView message;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("PokeWilds settings");
        settingsFile = new File(new File(getFilesDir(), "game"), GameSettings.FILE_NAME);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(32));
        TextView title = new TextView(this);
        title.setText("PokeWilds settings"); title.setTextSize(26); page.addView(title);
        TextView help = new TextView(this);
        help.setText("Changes take effect the next time the game starts. Keybindings, unknown settings, and comments are preserved.");
        help.setPadding(0, dp(8), 0, dp(16)); page.addView(help);
        message = new TextView(this); page.addView(message);
        fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL); page.addView(fields);
        save = new Button(this); save.setText("Save settings"); save.setOnClickListener(v -> save()); page.addView(save);
        ScrollView scroll = new ScrollView(this); scroll.addView(page); setContentView(scroll);
        load();
    }

    private void load() {
        controls.clear(); fields.removeAllViews();
        if (RuntimeService.active) {
            message.setText("Quit the game before changing its settings.");
            save.setEnabled(false);
            return;
        }
        try {
            document = GameSettings.read(settingsFile.toPath());
            for (GameSettings.Entry entry : document.entries()) if (!GameSettings.isKeyBinding(entry.key)) addField(entry);
            if (controls.isEmpty()) message.setText("No editable key=value settings were found.");
            else message.setText(controls.size() + " settings loaded");
            save.setEnabled(!controls.isEmpty());
        } catch (Exception error) {
            document = null; message.setText(error.getMessage()); save.setEnabled(false);
        }
    }

    private void addField(GameSettings.Entry entry) {
        GameSettingOptions.Definition definition = GameSettingOptions.forKey(entry.key);
        TextView label = new TextView(this);
        label.setText(definition == null ? entry.key : definition.label);
        label.setTextSize(16);
        fields.addView(label);

        if (definition != null && !definition.choices.isEmpty()) {
            addChoiceField(entry, definition);
        } else {
            addTextField(entry, definition);
        }
        if (definition != null && definition.hint != null) {
            TextView hint = new TextView(this);
            hint.setText(definition.hint);
            hint.setPadding(0, 0, 0, dp(10));
            fields.addView(hint);
        }
    }

    private void addChoiceField(GameSettings.Entry entry, GameSettingOptions.Definition definition) {
        List<GameSettingOptions.Choice> choices = new ArrayList<>(definition.choices);
        int selected = -1;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).value.equals(entry.value)) { selected = i; break; }
        }
        if (selected < 0) {
            choices.add(0, new GameSettingOptions.Choice("Current: " + entry.value, entry.value));
            selected = 0;
        }
        Spinner spinner = new Spinner(this);
        ArrayAdapter<GameSettingOptions.Choice> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        fields.addView(spinner);
        controls.put(entry.key, () -> ((GameSettingOptions.Choice) spinner.getSelectedItem()).value);
    }

    private void addTextField(GameSettings.Entry entry, GameSettingOptions.Definition definition) {
        EditText value = new EditText(this);
        value.setSingleLine(true);
        value.setText(entry.value);
        if (definition != null && definition.decimal) {
            value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            value.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
        value.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        fields.addView(value);
        controls.put(entry.key, () -> value.getText().toString());
    }

    private void save() {
        if (RuntimeService.active) { message.setText("Quit the game before changing its settings."); return; }
        if (document == null) return;
        try {
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, ValueControl> entry : controls.entrySet()) {
                values.put(entry.getKey(), GameSettingOptions.validatedValue(entry.getKey(), entry.getValue().value()));
            }
            document = document.withValues(values); document.writeAtomically(settingsFile.toPath());
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show(); load();
        } catch (Exception error) { message.setText("Could not save: " + error.getMessage()); }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
