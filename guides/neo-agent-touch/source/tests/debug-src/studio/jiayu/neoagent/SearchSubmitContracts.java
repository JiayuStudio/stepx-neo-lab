package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Owned Android editors plus production SearchSubmitRules. No business run/lease is acquired.
 * InputConnection.performEditorAction must reach the real EditText listener; this fixture does
 * not create an AccessibilityInputConnection or establish a third-party IME editor session.
 */
final class SearchSubmitContracts {
 interface Work { void run() throws Exception; }
 final Activity activity;
 final JSONObject report = new JSONObject();
 final JSONArray passed = new JSONArray();
 final AtomicInteger firstCallbacks = new AtomicInteger(), secondCallbacks = new AtomicInteger();
 final AtomicInteger unexpectedCallbacks = new AtomicInteger();
 SearchInputContracts.OffsetEditor first, second;
 int checks;
 SearchSubmitContracts(Activity activity) { this.activity = activity; }

 static void start(Activity activity) {
  activity.runOnUiThread(() -> new SearchSubmitContracts(activity).show());
 }
 static void run(Activity activity) { start(activity); }
 void yes(boolean value, String label) throws Exception {
  if (!value) throw new IllegalStateException(label);
  checks++; passed.put(label);
 }
 <T> T ui(SearchInputContracts.Job<T> job) throws Exception {
  try { return SearchInputContracts.ui(activity, job); }
  catch (java.util.concurrent.ExecutionException wrapped) {
   Throwable cause = wrapped.getCause();
   if (cause instanceof Exception) throw (Exception) cause;
   throw wrapped;
  }
 }
 int callbacks() { return firstCallbacks.get() + secondCallbacks.get(); }

 void show() {
  try {
   if ((activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
    throw new IllegalStateException("Debug fixture only");
   ControlService service = ControlService.instance;
   if (service != null && service.busy()) throw new IllegalStateException("Runtime not idle");
   LinearLayout layout = new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
   TextView label = new TextView(activity);
   label.setText("自有原生搜索提交：真实编辑回调、禁止重放与过期请求拒绝"); layout.addView(label);
   first = editor(firstCallbacks); second = editor(secondCallbacks);
   layout.addView(first); layout.addView(second); activity.setContentView(layout);
   new Handler(Looper.getMainLooper()).postDelayed(() ->
     new Thread(this::exercise, "search-submit-owned-editor").start(), 500);
  } catch (Throwable failure) { complete(failure); }
 }
 SearchInputContracts.OffsetEditor editor(AtomicInteger callbackCount) {
  SearchInputContracts.OffsetEditor field = new SearchInputContracts.OffsetEditor(activity);
  field.setHint("公开合成搜索关键词");
  field.setOnEditorActionListener((view, actionId, event) -> {
   if (actionId == EditorInfo.IME_ACTION_SEARCH) { callbackCount.incrementAndGet(); return true; }
   unexpectedCallbacks.incrementAndGet(); return false;
  });
  return field;
 }
 static final class Live {
  SearchInputContracts.OffsetEditor editor; InputConnection connection; String run;
  Live(SearchInputContracts.OffsetEditor editor, InputConnection connection, String run) {
   this.editor = editor; this.connection = connection; this.run = run;
  }
 }
 final class Port implements SearchSubmitRules.Port {
  final Live live; final SearchInputContracts.OffsetEditor editor;
  final InputConnection connection; final String run;
  int dispatches, selections; boolean ignoreSelection, throwAfterDispatch; Runnable beforeSearch;
  Port(Live live) { this.live = live; editor = live.editor; connection = live.connection; run = live.run; }
  void owner() throws SearchSubmitRules.Rejected {
   if (live.editor != editor || live.connection != connection || !run.equals(live.run))
    throw new SearchSubmitRules.Rejected("Owned fixture editor/run changed");
  }
  SearchInputRules.Sample raw() {
   SurroundingText value = connection.getSurroundingText(513, 513, 0);
   return value == null ? null : new SearchInputRules.Sample(value.getText().toString(),
     value.getOffset(), value.getSelectionStart(), value.getSelectionEnd());
  }
  public SearchInputRules.Sample read() throws Exception {
   return ui(() -> { owner(); return raw(); });
  }
  public void selectAll() throws Exception {
   ui(() -> { owner(); selections++;
    if (!ignoreSelection) connection.performContextMenuAction(android.R.id.selectAll);
    return true;
   });
  }
  public void pause(int milliseconds) throws Exception {
   Thread.sleep(milliseconds);
   if (activity.isFinishing() || activity.isDestroyed()) throw new IllegalStateException("Owned fixture closed");
  }
  public void search(SearchInputRules.Sample expected, Runnable dispatching) throws Exception {
   ui(() -> {
    if (beforeSearch != null) { Runnable change = beforeSearch; beforeSearch = null; change.run(); }
    owner();
    if (!expected.same(raw())) throw new SearchSubmitRules.Rejected("Owned fixture text changed");
    // Reserve only after the final owner/sample check. Do not call the listener directly.
    dispatching.run(); dispatches++;
    if (!connection.performEditorAction(EditorInfo.IME_ACTION_SEARCH))
     throw new IllegalStateException("Native editor action not accepted");
    if (throwAfterDispatch) throw new IllegalStateException("Synthetic exception after native dispatch");
    return true;
   });
  }
 }
 void text(SearchInputContracts.OffsetEditor field, String value) throws Exception {
  ui(() -> { field.setText(value); field.setSelection(field.length()); return true; });
 }
 SearchSubmitRules.Validator query(String expected) {
  return value -> { if (!expected.equals(value)) throw new SearchSubmitRules.Rejected("Unexpected fixture query"); };
 }
 String submit(Port port, SearchInputRules.Pending writes, SearchSubmitRules.State state, String text) throws Exception {
  return SearchSubmitRules.submit(port, writes, state, port.editor, port.run, query(text));
 }
 void rejected(Port port, Work work, String label) throws Exception {
  int before = callbacks(), dispatched = port.dispatches; boolean rejected = false;
  try { work.run(); } catch (SearchSubmitRules.Rejected expected) { rejected = true; }
  yes(rejected, label + ": production protocol rejected");
  yes(callbacks() == before && port.dispatches == dispatched, label + ": no native action dispatched");
 }
 void exercise() {
  Throwable failed = null;
  try {
   final String queryA = "公开搜索甲", runA = "owned-submit-run-A";
   InputConnection a = ui(() -> { first.requestFocus(); first.setText(queryA); first.setSelection(first.length());
    return first.onCreateInputConnection(new EditorInfo()); });
   InputConnection b = ui(() -> { second.setText("公开搜索乙"); second.setSelection(second.length());
    return second.onCreateInputConnection(new EditorInfo()); });
   yes(a != null && b != null, "owned native InputConnections available");
   Live live = new Live(first, a, runA); Port original = new Port(live);
   SearchInputRules.Sample sample = original.read();
   yes(sample != null && sample.offset == -1 && sample.readable(), "native unknown-offset sample readable");
   SearchInputRules.Pending writes = new SearchInputRules.Pending(); SearchSubmitRules.State state = new SearchSubmitRules.State();
   String receipt = submit(original, writes, state, queryA);
   yes(firstCallbacks.get() == 1 && secondCallbacks.get() == 0 && original.dispatches == 1,
     "native performEditorAction reaches exactly one real editor callback");
   yes(original.selections == 1, "unknown offset selects and confirms full native text");
   yes(state.dispatched(first, runA, queryA), "receipt bound to editor run and text");
   yes(receipt != null && !receipt.isEmpty(), "production dispatch receipt returned without claiming result page");
   rejected(original, () -> submit(original, writes, state, queryA), "same editor/run/query replay");

   text(first, queryA); Port pendingPort = new Port(live);
   SearchInputRules.Pending pendingWrite = new SearchInputRules.Pending();
   pendingWrite.dispatched(first, runA, "公开搜索尚未读回");
   rejected(pendingPort, () -> submit(pendingPort, pendingWrite, new SearchSubmitRules.State(), queryA),
     "unresolved prior write");

   text(first, queryA); Port unselected = new Port(live); unselected.ignoreSelection = true;
   rejected(unselected, () -> submit(unselected, new SearchInputRules.Pending(), new SearchSubmitRules.State(), queryA),
     "unknown offset without confirmed selection");

   text(first, queryA); Port changedText = new Port(live);
   changedText.beforeSearch = () -> { first.setText("公开搜索已变化"); first.setSelection(first.length()); };
   rejected(changedText, () -> submit(changedText, new SearchInputRules.Pending(), new SearchSubmitRules.State(), queryA),
     "text changes at final dispatch boundary");

   text(first, queryA); Port changedEditor = new Port(live);
   changedEditor.beforeSearch = () -> { live.editor = second; live.connection = b; };
   rejected(changedEditor, () -> submit(changedEditor, new SearchInputRules.Pending(), new SearchSubmitRules.State(), queryA),
     "stale request after editor changes");
   live.editor = first; live.connection = a;

   text(first, queryA); Port changedRun = new Port(live);
   changedRun.beforeSearch = () -> live.run = "owned-submit-run-B";
   rejected(changedRun, () -> submit(changedRun, new SearchInputRules.Pending(), new SearchSubmitRules.State(), queryA),
     "stale request after run changes");
   Port currentRun = new Port(live);
   submit(currentRun, new SearchInputRules.Pending(), state, queryA);
   yes(firstCallbacks.get() == 2 && currentRun.dispatches == 1,
     "new request in current run may legitimately dispatch same text");
   rejected(currentRun, () -> submit(currentRun, new SearchInputRules.Pending(), state, queryA),
     "current run duplicate still rejected");

   String afterQuery = "公开搜索派发后异常"; text(first, afterQuery);
   Port after = new Port(live); after.throwAfterDispatch = true; SearchSubmitRules.State reserved = new SearchSubmitRules.State();
   boolean threw = false; try { submit(after, new SearchInputRules.Pending(), reserved, afterQuery); }
   catch (IllegalStateException expected) { threw = true; }
   yes(threw && firstCallbacks.get() == 3 && after.dispatches == 1,
     "exception after real native action does not erase dispatch");
   after.throwAfterDispatch = false;
   rejected(after, () -> submit(after, new SearchInputRules.Pending(), reserved, afterQuery),
     "exception-after-dispatch replay");
   yes(secondCallbacks.get() == 0 && unexpectedCallbacks.get() == 0,
     "no other editor or non-search callback fired");
   report.put("success", true).put("native_owned_editor_callback_observed", true)
     .put("duplicate_dispatch_prevented", true).put("stale_owner_gate_checked_before_reserve", true);
  } catch (Throwable failure) { failed = failure; }
  finally { complete(failed); }
 }
 void complete(Throwable failed) {
  try {
   if (failed != null) report.put("success", false).put("failure_check", failed.getMessage() == null ? "" : failed.getMessage())
     .put("failure_type", failed.getClass().getSimpleName());
   report.put("checks", checks).put("passed", passed)
     .put("source", "owned Android InputConnection + SearchSubmitRules protocol")
     .put("first_editor_search_callbacks", firstCallbacks.get()).put("second_editor_search_callbacks", secondCallbacks.get())
     .put("native_accessibility_editor_session_tested", false)
     .put("accessibility_input_connection_perform_editor_action_tested", false)
     .put("owner_gate_scope", "fixture Port identity/run rechecks; not a production AccessibilityInputConnection session test")
     .put("search_result_page_verified", false).put("qq_music_acceptance_tested", false)
     .put("business_run_or_lease_modified", false).put("production_preferences_modified", false)
     .put("network_calls", 0).put("third_party_actions", 0).put("permission_changes", 0)
     .put("sampledAt", System.currentTimeMillis());
   try (OutputStream output = activity.openFileOutput("search-submit-contracts.json", 0)) {
    output.write(report.toString(2).getBytes("UTF-8"));
   }
  } catch (Exception ignored) { }
  finally { activity.runOnUiThread(activity::finish); }
 }
}
