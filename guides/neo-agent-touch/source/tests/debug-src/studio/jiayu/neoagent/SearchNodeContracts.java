package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/** Owned native nodes plus SearchNodeRules only. Never changes ControlService task/owner/config. */
final class SearchNodeContracts {
 interface Job<T>{T run()throws Exception;}
 final Activity activity;final Handler main=new Handler(Looper.getMainLooper());
 final JSONArray passed=new JSONArray();final JSONObject report=new JSONObject();
 final AtomicInteger searches=new AtomicInteger(),listenerCalls=new AtomicInteger(),nonSearchCalls=new AtomicInteger();final AtomicBoolean completing=new AtomicBoolean();
 final JSONArray callbackActions=new JSONArray();final JSONObject submitDiagnostic=new JSONObject();
 volatile String step="bootstrap";long callbackWaitMs;
 static final java.util.Set<String> SAFE_CODES=new java.util.HashSet<>(java.util.Arrays.asList(
  "assertion_failed","debug_only","runtime_not_idle","owned_fixture_lease_changed","stale_owned_ui","owned_fixture_closed",
  "owned_view_not_focused","owned_editor_not_foreground","exact_owned_node_mismatch","owned_text_changed",
  "owned_node_changed_at_dispatch","owned_native_write_unconfirmed","node_ime_action_not_advertised",
  "node_ime_native_return_false","node_ime_search_callback_timeout","node_ime_search_duplicate_callback",
  "ui_timeout","ui_cancelled","interrupted","unclassified"));
 static final class FixtureFailure extends Exception{final String code;FixtureFailure(String code){super("owned_fixture_failed");this.code=SAFE_CODES.contains(code)?code:"unclassified";}}
 static String failureCode(Throwable failure){Throwable cause=failure;for(int i=0;i<6&&cause!=null;i++){
  if(cause instanceof FixtureFailure)return ((FixtureFailure)cause).code;
  if(cause instanceof java.util.concurrent.TimeoutException)return "ui_timeout";
  if(cause instanceof java.util.concurrent.CancellationException)return "ui_cancelled";
  if(cause instanceof InterruptedException)return "interrupted";
  if(cause.getCause()==cause)break;cause=cause.getCause();
 }return "unclassified";}
 final Object uiLock=new Object();final ArrayList<FutureTask<?>> queued=new ArrayList<>();
 volatile boolean closed;volatile long uiEpoch=1;Runnable delayedStart;Thread worker;
 EditText first,second;ControlService service;long connection,generation;String run="";
 int checks;boolean nativeWriteAccepted;
 SearchNodeContracts(Activity activity){this.activity=activity;}
 static void start(Activity activity){activity.runOnUiThread(()->new SearchNodeContracts(activity).show());}
 void yes(boolean value,String label)throws Exception{if(!value)throw new FixtureFailure("assertion_failed");checks++;passed.put(label);}
 boolean baseline(){return service!=null&&service==ControlService.instance&&service.ownsConnection()&&service.connectionToken()==connection&&service.generation==generation&&run.equals(service.runId)&&!service.busy();}
 void guard()throws Exception{
  if(closed||activity.isFinishing()||activity.isDestroyed()||!baseline()||!activity.hasWindowFocus()||first==null||second==null||!first.isAttachedToWindow()||!second.isAttachedToWindow()||!first.hasWindowFocus()||!second.hasWindowFocus())throw new FixtureFailure("owned_fixture_lease_changed");
 }
 <T>T ui(Job<T> job)throws Exception{
  final long ticket=uiEpoch;
  FutureTask<T> task=new FutureTask<>(()->{if(ticket!=uiEpoch)throw new FixtureFailure("stale_owned_ui");guard();return job.run();});
  synchronized(uiLock){if(closed||ticket!=uiEpoch)throw new FixtureFailure("owned_fixture_closed");queued.add(task);main.post(task);}
  try{return task.get(4,TimeUnit.SECONDS);}catch(Exception failure){
   task.cancel(false);main.removeCallbacks(task);synchronized(uiLock){closed=true;uiEpoch++;}throw failure;
  }finally{synchronized(uiLock){queued.remove(task);}}
 }
 void show(){try{
  if((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)throw new FixtureFailure("debug_only");
  service=ControlService.instance;if(service==null||!service.ownsConnection()||service.busy())throw new FixtureFailure("runtime_not_idle");
  connection=service.connectionToken();generation=service.generation;run=service.runId;
  LinearLayout box=new LinearLayout(activity);box.setOrientation(1);TextView title=new TextView(activity);title.setText("自有搜索节点：真实写入、未提交拦截及禁止重放");box.addView(title);
  first=field();second=field();box.addView(first);box.addView(second);activity.setContentView(box);first.requestFocus();
  delayedStart=()->{if(closed)return;worker=new Thread(this::exercise,"search-node-owned-fixture");worker.start();};main.postDelayed(delayedStart,600);
 }catch(Throwable failure){complete(failure);}}
 EditText field(){EditText field=new EditText(activity);field.setSingleLine(true);field.setInputType(android.text.InputType.TYPE_CLASS_TEXT);field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);field.setHint("搜索公开演示内容");field.setText("原有公开查询");field.setSelection(field.length());field.setOnEditorActionListener((view,id,event)->{listenerCalls.incrementAndGet();if(id==EditorInfo.IME_ACTION_SEARCH)searches.incrementAndGet();else nonSearchCalls.incrementAndGet();try{callbackActions.put(new JSONObject().put("action",id>=0&&id<=7?id:-1).put("key_event_present",event!=null));}catch(Exception ignored){}return true;});return field;}
 AccessibilityNodeInfo node(EditText expected)throws Exception{
  guard();if(!expected.isFocused())throw new FixtureFailure("owned_view_not_focused");
  AccessibilityNodeInfo root=service.getRootInActiveWindow();if(root==null||!activity.getPackageName().contentEquals(root.getPackageName()))throw new FixtureFailure("owned_editor_not_foreground");
  AccessibilityNodeInfo focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT),owned=expected.createAccessibilityNodeInfo();
  if(focused==null||owned==null||!owned.equals(focused)||!focused.isEditable()||!focused.isFocused()||focused.isPassword())throw new FixtureFailure("exact_owned_node_mismatch");return focused;
 }
 final class Port implements SearchNodeRules.Port {
  final EditText field;int calls;boolean hide;
  Port(EditText field){this.field=field;}
  SearchInputRules.Sample raw()throws Exception{AccessibilityNodeInfo current=node(field);return new SearchInputRules.Sample(current.getText()==null?"":current.getText().toString(),0,current.getTextSelectionStart(),current.getTextSelectionEnd());}
  public SearchInputRules.Sample read()throws Exception{return ui(()->hide&&calls>0?null:raw());}
  public void set(SearchInputRules.Sample expected,String value,Runnable dispatching)throws Exception{ui(()->{
   AccessibilityNodeInfo current=node(field);if(!expected.same(raw()))throw new FixtureFailure("owned_text_changed");
   Bundle arguments=new Bundle();arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
   guard();if(!current.equals(node(field)))throw new FixtureFailure("owned_node_changed_at_dispatch");
   dispatching.run();calls++;if(!current.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arguments))throw new FixtureFailure("owned_native_write_unconfirmed");nativeWriteAccepted=true;return true;
  });}
  public void pause(int ms)throws Exception{Thread.sleep(ms);if(closed)throw new FixtureFailure("owned_fixture_closed");}
 }
 void submitContext(AccessibilityNodeInfo current)throws Exception{
  submitDiagnostic.put("action_advertised",current.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER))
   .put("view_ime_action",first.getImeOptions()&EditorInfo.IME_MASK_ACTION).put("view_custom_action_id",first.getImeActionId()>=0&&first.getImeActionId()<=7?first.getImeActionId():-1)
   .put("view_no_enter_action",(first.getImeOptions()&EditorInfo.IME_FLAG_NO_ENTER_ACTION)!=0).put("dispatch_attempted",false).put("native_return_known",false);
  String selected="unavailable";try{String value=Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);selected=value!=null&&value.toLowerCase(java.util.Locale.ROOT).contains("sogou")?"sogou":value!=null&&value.startsWith(activity.getPackageName()+"/")?"agent":"other_or_unknown";}catch(RuntimeException denied){}
  boolean keyboardVisible=false,rootAvailable=false,windowsSampled=false;String visible="none";
  try{for(AccessibilityWindowInfo window:service.getWindows())if(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){keyboardVisible=true;AccessibilityNodeInfo root=window.getRoot();if(root!=null){rootAvailable=true;String pkg=String.valueOf(root.getPackageName()).toLowerCase(java.util.Locale.ROOT);visible=pkg.contains("sogou")?"sogou":activity.getPackageName().equals(pkg)?"agent":"other_or_unknown";}else visible="unknown";}windowsSampled=true;}catch(RuntimeException unavailable){visible="unavailable";}
  submitDiagnostic.put("selected_ime_category",selected).put("ime_windows_sampled",windowsSampled).put("keyboard_visible",keyboardVisible).put("keyboard_root_available",rootAvailable).put("visible_ime_category",visible);
 }
 void awaitSearchCallback()throws Exception{
  long start=SystemClock.elapsedRealtime(),deadline=start+1800;
  try{while(SystemClock.elapsedRealtime()<deadline){
   int count=ui(()->{node(first);return searches.get();});long now=SystemClock.elapsedRealtime();
   if(count>1)throw new FixtureFailure("node_ime_search_duplicate_callback");
   if(count==1&&now<=deadline)return;
   if(now>=deadline)break;Thread.sleep(Math.min(40,deadline-now));
  }
  throw new FixtureFailure("node_ime_search_callback_timeout");
  }finally{callbackWaitMs=SystemClock.elapsedRealtime()-start;}
 }
 void exercise(){Throwable failed=null;try{
  step="native_node_identity";
  AccessibilityNodeInfo original=ui(()->node(first)),fresh=ui(()->node(first));yes(original.equals(fresh),"real Android wrappers identify exact owned EditText");
  SearchNodeRules.Session sessions=new SearchNodeRules.Session();yes(sessions.begin("owned-node",1),"synthetic protocol owner begins");Object editor=new Object(),key=sessions.focus("owned-node",1,original,editor,true);yes(sessions.focus("owned-node",1,fresh,editor,true)==key,"refreshed wrapper keeps receipt key");
  SearchFinishRules finish=new SearchFinishRules();finish.begin("owned-node",1);SearchInputRules.Pending pending=new SearchInputRules.Pending();Port port=new Port(first);
  step="type_and_exact_readback";
  yes(SearchNodeRules.replace(port,pending,finish,key,"owned-node",1,"公开搜索甲")==SearchInputRules.Result.VERIFIED,"real ACTION_SET_TEXT verified by native readback");
  yes(port.calls==1&&ui(()->first.getText().toString().equals("公开搜索甲")),"exactly one owned native write");
  yes(finish.beforeFinish("owned-node",1,key,"公开搜索甲",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"type then finish rejected without submit");
  step="already_present_no_write";
  yes(SearchNodeRules.replace(port,pending,finish,key,"owned-node",1,"公开搜索甲")==SearchInputRules.Result.ALREADY_PRESENT&&port.calls==1,"already-present text makes no new write");
  SearchSubmitRules.State state=finish.submissionState("owned-node",1,key);
  step="native_submit_preflight";
  ui(()->{SearchInputRules.Sample now=port.raw();AccessibilityNodeInfo current=node(first);submitContext(current);
   if(!current.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER))throw new FixtureFailure("node_ime_action_not_advertised");
   guard();if(!current.equals(node(first)))throw new FixtureFailure("owned_node_changed_at_dispatch");
   state.reserve(key,"owned-node",now.text);finish.submitted("owned-node",1,key,now.text);step="native_submit_dispatch";submitDiagnostic.put("dispatch_attempted",true);
   boolean accepted=current.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());submitDiagnostic.put("native_return_known",true).put("native_accepted",accepted);
   if(!accepted)throw new FixtureFailure("node_ime_native_return_false");return true;
  });
  step="native_submit_listener_wait";awaitSearchCallback();
  yes(searches.get()==1,"real ACTION_IME_ENTER reaches owned SEARCH listener once");
  step="submit_receipt_and_dedupe";
  yes(finish.beforeFinish("owned-node",1,key,"公开搜索甲",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"submit attempt clears only missing-submit blocker");
  boolean refused=false;try{state.reserve(key,"owned-node","公开搜索甲");}catch(SearchSubmitRules.Rejected expected){refused=true;}yes(refused&&searches.get()==1,"same write cycle cannot reserve duplicate submit");
  step="a_b_a_actual_write";
  yes(SearchNodeRules.replace(port,pending,finish,key,"owned-node",1,"公开搜索乙")==SearchInputRules.Result.VERIFIED,"new actual B write starts new attempt");
  yes(SearchNodeRules.replace(port,pending,finish,key,"owned-node",1,"公开搜索甲")==SearchInputRules.Result.VERIFIED,"A-B-A actual write accepted");
  SearchSubmitRules.State returned=finish.submissionState("owned-node",1,key);yes(returned!=state,"A-B-A does not reuse A reservation");returned.reserve(key,"owned-node","公开搜索甲");
  step="unverified_write_not_replayed";
  Port lost=new Port(first);lost.hide=true;yes(SearchNodeRules.replace(lost,pending,finish,key,"owned-node",1,"公开搜索丙")==SearchInputRules.Result.DISPATCHED_UNVERIFIED,"hidden readback remains unverified");
  yes(finish.beforeFinish("owned-node",1,key,null,true,false,true,false)==SearchFinishRules.Decision.WAIT_FOR_READBACK,"unverified write blocks finish");
  refused=false;try{SearchNodeRules.replace(lost,pending,finish,key,"owned-node",1,"公开搜索丙");}catch(SearchInputRules.Rejected expected){refused=true;}yes(refused&&lost.calls==1,"unknown native receipt not replayed");
  step="second_field_session";
  ui(()->{second.requestFocus();return true;});Thread.sleep(100);AccessibilityNodeInfo other=ui(()->node(second));Object secondKey=sessions.focus("owned-node",1,other,editor,true);yes(secondKey!=key&&!sessions.accepts("owned-node",1,key,other,editor),"second exact owned field rejects first field session");
  step="old_epoch";
  yes(sessions.begin("owned-node",2),"next synthetic epoch begins");Object newKey=sessions.focus("owned-node",2,other,editor,true);yes(sessions.focus("owned-node",1,original,editor,true)==null&&sessions.accepts("owned-node",2,newKey,other,editor),"old callback cannot change new epoch");
  step="no_enter_action";
  EditorInfo options=ui(()->{second.setImeOptions(EditorInfo.IME_ACTION_SEARCH|EditorInfo.IME_FLAG_NO_ENTER_ACTION);EditorInfo value=new EditorInfo();second.onCreateInputConnection(value);return value;});
  yes((options.imeOptions&EditorInfo.IME_MASK_ACTION)==EditorInfo.IME_ACTION_SEARCH&&!SearchInputRules.canSubmit(options.imeOptions),"real editor NO_ENTER_ACTION is not advertised as submittable");
 }catch(Throwable failure){failed=failure;}finally{complete(failed);}}
 void complete(Throwable failure){
  if(!completing.compareAndSet(false,true))return;
  synchronized(uiLock){closed=true;uiEpoch++;if(delayedStart!=null)main.removeCallbacks(delayedStart);for(FutureTask<?> task:queued){task.cancel(false);main.removeCallbacks(task);}queued.clear();}
  final Throwable result=failure;
  main.post(()->{
   boolean cleanup=true;
   try{if(!activity.isDestroyed()&&activity.hasWindowFocus()&&first!=null&&first.isAttachedToWindow()){
    if(first.getWindowToken()!=null)((InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(first.getWindowToken(),0);
    first.clearFocus();if(second!=null&&second.isAttachedToWindow())second.clearFocus();
   }}catch(RuntimeException denied){cleanup=false;}
   try{report.put("success",result==null&&cleanup).put("checks",checks).put("passed",passed).put("cleanup",cleanup).put("sampledAt",System.currentTimeMillis()).put("network_calls",0).put("third_party_actions",0).put("permission_changes",0).put("input_method_selection_changes",0).put("runtime_fields_modified",false).put("baseline_connection_unchanged",baseline()).put("production_node_write_protocol",true).put("real_accessibility_set_text_accepted",nativeWriteAccepted).put("synthetic_protocol_owner",true).put("production_setPublicText_tested",false).put("production_control_run_and_bridge_owner_tested",false).put("sogou_runtime_tested",false).put("result_page_or_playback_verified",false);report.put("last_step",step).put("submit_diagnostic",submitDiagnostic.put("listener_callbacks",listenerCalls.get()).put("search_callbacks",searches.get()).put("non_search_callbacks",nonSearchCalls.get()).put("callback_actions",callbackActions).put("callback_wait_ms",callbackWaitMs).put("callback_wait_bound_ms",1800));if(result!=null)report.put("failure_kind",result.getClass().getSimpleName()).put("failure_code",failureCode(result));try(OutputStream stream=activity.openFileOutput("search-node-contracts.json",0)){stream.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}if(!activity.isDestroyed())activity.finish();
  });
 }
}
