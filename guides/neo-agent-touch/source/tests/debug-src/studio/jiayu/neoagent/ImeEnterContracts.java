package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.text.InputType;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.OutputStream;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual owned TextView accessibility actions vs InputConnection actions; no business lease. */
final class ImeEnterContracts {
 interface Job<T> { T run() throws Exception; }
 final Activity activity;
 final JSONObject report = new JSONObject();
 final JSONArray passed = new JSONArray(), cases = new JSONArray(), callbacks = new JSONArray();
 LinearLayout layout;
 EditText editor;
 TextView focusSink;
 ControlService service;
 String originalRun;
 long originalGeneration;
 boolean originalRunning, originalPaused;
 int checks, searchCallbacks;
 boolean nativeConnectionTested;

 ImeEnterContracts(Activity activity) { this.activity = activity; }
 static void start(Activity activity) {
  activity.runOnUiThread(() -> new ImeEnterContracts(activity).show());
 }
 <T> T ui(Job<T> work) throws Exception {
  FutureTask<T> task = new FutureTask<>(() -> {
   if (activity.isFinishing() || activity.isDestroyed()) throw new IllegalStateException("Owned activity closed");
   return work.run();
  });
  activity.runOnUiThread(task);
  return task.get(5, TimeUnit.SECONDS);
 }
 void yes(boolean value, String label) throws Exception {
  if (!value) throw new IllegalStateException(label);
  checks++; passed.put(label);
 }
 void show() {
  try {
   if ((activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
    throw new IllegalStateException("Debug fixture only");
   service = ControlService.instance;
   if (service != null) synchronized (service) {
    originalRun = service.runId; originalGeneration = service.generation;
    originalRunning = service.running; originalPaused = service.paused;
    if (service.busy()) throw new IllegalStateException("Runtime not idle; fixture did not change it");
   }
   activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
   layout = new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
   TextView heading = new TextView(activity);
   heading.setText("自有编辑器对照：节点 IME_ENTER、焦点与明确 SEARCH 回调");
   layout.addView(heading);
   editor = new EditText(activity);
   editor.setSingleLine(true); editor.setInputType(InputType.TYPE_CLASS_TEXT);
   editor.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
   editor.setShowSoftInputOnFocus(false);
   editor.setText("公开合成搜索甲"); editor.setSelection(editor.length());
   editor.setOnEditorActionListener((view, action, event) -> {
    try { callbacks.put(new JSONObject().put("action", action).put("key_event_present", event != null)); }
    catch (Exception impossible) { throw new IllegalStateException(impossible); }
    if (action == EditorInfo.IME_ACTION_SEARCH) searchCallbacks++;
    // Consume every code for isolation: do not let code 0 synthesize extra Enter events.
    return true;
   });
   layout.addView(editor);
   focusSink = new TextView(activity); focusSink.setText("自有焦点对照区");
   focusSink.setFocusable(true); focusSink.setFocusableInTouchMode(true); layout.addView(focusSink);
   activity.setContentView(layout);
   new Thread(this::exercise, "ime-enter-owned-editor").start();
  } catch (Throwable failure) { complete(failure); }
 }
 void waitForWindow() throws Exception {
  long deadline = SystemClock.elapsedRealtime() + 2000;
  while (SystemClock.elapsedRealtime() < deadline) {
   if (ui(() -> activity.hasWindowFocus() && editor.isAttachedToWindow() && editor.isShown())) return;
   Thread.sleep(40);
  }
  throw new IllegalStateException("Owned editor window not ready");
 }
 boolean exposesEnter() {
  AccessibilityNodeInfo info = editor.createAccessibilityNodeInfo();
  try { return info.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER); }
  finally { info.recycle(); }
 }
 void sample(String name, boolean result, int beforeCallbacks, int beforeSearch) throws Exception {
  JSONObject value = new JSONObject().put("case", name).put("native_return", result)
   .put("focused", editor.isFocused()).put("ime_options_action", editor.getImeOptions() & EditorInfo.IME_MASK_ACTION)
   .put("custom_action_id", editor.getImeActionId()).put("listener_callbacks", callbacks.length() - beforeCallbacks)
   .put("search_callbacks", searchCallbacks - beforeSearch);
  if (callbacks.length() > beforeCallbacks) value.put("last_callback", callbacks.getJSONObject(callbacks.length() - 1));
  cases.put(value);
 }
 void exercise() {
  Throwable failure = null;
  try {
   waitForWindow();
   ui(() -> {
    yes(editor.requestFocus() && editor.isFocused(), "owned editor receives input focus");
    yes(editor.getImeActionId() == 0, "default custom actionId is zero");
    yes((editor.getImeOptions() & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_SEARCH,
      "imeOptions independently advertises SEARCH");
    yes(exposesEnter(), "focused editable node exposes ACTION_IME_ENTER");
    int before = callbacks.length(), searches = searchCallbacks;
    boolean returned = editor.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId(), null);
    sample("focused_node_default_action_id", returned, before, searches);
    yes(returned, "focused ACTION_IME_ENTER returns true");
    yes(callbacks.length() == before + 1 && callbacks.getJSONObject(before).getInt("action") == 0,
      "focused node action invokes listener with custom actionId zero");
    yes(searchCallbacks == searches, "true node receipt does not imply SEARCH callback");

    yes(focusSink.requestFocus() && !editor.isFocused(), "owned editor loses focus");
    yes(!exposesEnter(), "unfocused standard node no longer advertises IME_ENTER");
    before = callbacks.length(); searches = searchCallbacks;
    returned = editor.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId(), null);
    sample("unfocused_node_forced_action", returned, before, searches);
    yes(returned, "direct unfocused IME_ENTER may still return true");
    yes(callbacks.length() == before && searchCallbacks == searches,
      "unfocused true receipt invokes no editor listener");

    yes(editor.requestFocus() && editor.isFocused(), "owned editor regains focus");
    EditorInfo attributes = new EditorInfo(); InputConnection connection = editor.onCreateInputConnection(attributes);
    yes(connection != null && (attributes.imeOptions & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_SEARCH
      && attributes.actionId == 0, "real owned InputConnection keeps SEARCH options and distinct zero actionId");
    before = callbacks.length(); searches = searchCallbacks;
    returned = connection.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
    nativeConnectionTested = true;
    sample("input_connection_explicit_search", returned, before, searches);
    yes(returned, "owned InputConnection accepts explicit SEARCH");
    yes(callbacks.length() == before + 1 && searchCallbacks == searches + 1
      && callbacks.getJSONObject(before).getInt("action") == EditorInfo.IME_ACTION_SEARCH,
      "explicit SEARCH reaches one real editor listener with action three");

    editor.setImeActionLabel("搜索", EditorInfo.IME_ACTION_SEARCH);
    before = callbacks.length(); searches = searchCallbacks;
    returned = editor.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId(), null);
    sample("focused_node_explicit_custom_search_id", returned, before, searches);
    yes(returned && callbacks.length() == before + 1 && searchCallbacks == searches + 1,
      "node action reaches SEARCH only when its custom actionId is explicitly SEARCH");
    yes("公开合成搜索甲".equals(editor.getText().toString()), "all comparisons preserve owned query text");
    report.put("success", true);
    return true;
   });
  } catch (Throwable error) { failure = error; }
  finally { complete(failure); }
 }
 boolean runtimeUnchanged() {
  if (ControlService.instance != service) return false;
  if (service == null) return true;
  synchronized (service) {
   return java.util.Objects.equals(originalRun, service.runId) && originalGeneration == service.generation
    && originalRunning == service.running && originalPaused == service.paused;
  }
 }
 void complete(Throwable failure) {
  boolean cleanup = false;
  try {
   cleanup = ui(() -> {
    if (editor != null) { editor.setOnEditorActionListener(null); editor.clearFocus(); }
    if (layout != null) layout.removeAllViews();
    return (layout == null || layout.getChildCount() == 0) && runtimeUnchanged();
   });
  } catch (Throwable cleanupFailure) {
   if (failure == null) failure = cleanupFailure;
  }
  try {
   if (failure != null) report.put("failure_type", failure.getClass().getSimpleName())
    .put("failure", failure.getMessage() == null ? "" : failure.getMessage());
   report.put("success", failure == null && cleanup && report.optBoolean("success"))
    .put("checks", checks).put("passed", passed).put("cases", cases).put("callbacks", callbacks)
    .put("cleanup_verified", cleanup).put("sampledAt", System.currentTimeMillis())
    .put("listener_consumes_all_action_codes_for_isolation", true)
    .put("actual_edit_text_accessibility_action_tested", cases.length() > 0)
    .put("actual_owned_input_connection_tested", nativeConnectionTested)
    .put("accessibility_input_connection_tested", false)
    .put("production_node_refresh_race_reproduced", false)
    .put("qq_music_acceptance_tested", false).put("search_result_page_verified", false)
    .put("business_run_or_lease_modified", false).put("production_preferences_modified", false)
    .put("network_calls", 0).put("third_party_actions", 0).put("permission_changes", 0);
   try (OutputStream output = activity.openFileOutput("ime-enter-contracts.json", Activity.MODE_PRIVATE)) {
    output.write(report.toString(2).getBytes("UTF-8"));
   }
  } catch (Exception ignored) { }
  finally { activity.runOnUiThread(activity::finish); }
 }
}
