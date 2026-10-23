package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real framework IME/connection over owned editors; the task owner is synthetic and model-free. */
final class AgentImeContracts {
 private final Activity activity;
 private final JSONObject report=new JSONObject();
 private final JSONArray passed=new JSONArray();
 private final AtomicInteger searches=new AtomicInteger(),sends=new AtomicInteger();
 private final String fixtureRun="agent-ime-fixture-"+UUID.randomUUID();
 private final long started=SystemClock.elapsedRealtime(),deadline=started+36000;
 private ControlService owner;
 private EditText first,second,send;
 private RuntimeBefore runtime;
 private PersistentBefore persistent;
 private String originalIme="",stage="preconditions";
 private boolean mutated,grantChanged,completed,realConnectionTested;
 private int checks;

 static void start(Activity activity){new Thread(()->new AgentImeContracts(activity).run(),"agent-ime-contracts").start();}
 private AgentImeContracts(Activity activity){this.activity=activity;}
 private interface Probe {boolean ready()throws Exception;}
 private void yes(boolean value,String label)throws Exception{if(!value)throw new IllegalStateException(label);passed.put(label);checks++;}
 private void alive()throws Exception{if(SystemClock.elapsedRealtime()>=deadline)throw new IllegalStateException("fixture_deadline");if(activity.isFinishing()||activity.isDestroyed())throw new IllegalStateException("fixture_activity_closed");if(mutated&&(owner!=ControlService.instance||!fixtureRun.equals(owner.runId)))throw new IllegalStateException("fixture_owner_replaced");}
 private void await(Probe probe,int milliseconds,String label)throws Exception{long until=Math.min(deadline,SystemClock.elapsedRealtime()+milliseconds);while(SystemClock.elapsedRealtime()<until){alive();if(probe.ready()){yes(true,label);return;}Thread.sleep(70);}throw new IllegalStateException(label);}
 private static String canonical(String value){ComponentName id=ComponentName.unflattenFromString(value==null?"":value);return id==null?"":id.flattenToShortString();}
 private String selected(){return canonical(Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD));}
 private boolean enabled(String id){InputMethodManager manager=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);for(InputMethodInfo info:manager.getEnabledInputMethodList())if(id.equals(canonical(info.getId())))return true;return false;}
 private boolean ownRun(){return mutated&&owner==ControlService.instance&&fixtureRun.equals(owner.runId);}
 private void focus(EditText field)throws Exception{owner.ui(()->{alive();owner.runtimeHealth.phase("starting");field.requestFocus();((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);return true;});}
 private AgentInputMethodService.EditorSnapshot editor(long g,int action,long previousEpoch)throws Exception{
  final AgentInputMethodService.EditorSnapshot[] found={null};
  await(()->owner.ui(()->{AgentInputMethodService ime=AgentInputMethodService.instance;if(ime==null||!owner.agentImeLease.held(g))return false;AgentInputMethodService.EditorSnapshot value=ime.snapshot(owner,g);if(!value.connectionReady||value.imeAction!=action||!activity.getPackageName().equals(value.packageName)||value.epoch==previousEpoch)return false;found[0]=value;return true;}),3500,"real_editor_connection_ready");
  return found[0];
 }
 private String read(long g,long epoch)throws Exception{return owner.ui(()->AgentInputMethodService.instance.readText(owner,g,epoch,256));}
 private boolean denied(ControlService.Job<Boolean> operation,String expectedCode)throws Exception{try{owner.ui(operation);return false;}catch(java.io.IOException expected){if(expectedCode.equals(expected.getMessage()))return true;throw expected;}}
 private void restored(String label)throws Exception{await(()->owner.ui(()->originalIme.equals(selected())&&!owner.agentImeLease.status().optBoolean("recovery_record_present")),3500,label);}
 private EditText field(String hint,int action){EditText input=new EditText(activity);input.setSingleLine(true);input.setHint(hint);input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setImeOptions(action|EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);input.setOnEditorActionListener((view,id,event)->{if(id==EditorInfo.IME_ACTION_SEARCH)searches.incrementAndGet();if(id==EditorInfo.IME_ACTION_SEND)sends.incrementAndGet();return true;});return input;}
 private void createUi()throws Exception{owner.ui(()->{activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,80,24,24);layout.setBackgroundColor(Color.rgb(35,34,37));TextView title=new TextView(activity);title.setText("Agent 输入法 · 本机检查\n仅测试当前页面，不连接模型或其他应用。");title.setTextColor(Color.rgb(255,163,116));title.setTextSize(16);layout.addView(title);first=field("本机搜索 A",EditorInfo.IME_ACTION_SEARCH);second=field("本机搜索 B",EditorInfo.IME_ACTION_SEARCH);send=field("SEND 边界",EditorInfo.IME_ACTION_SEND);first.setText("公开测试");second.setText("另一个公开字段");send.setText("禁止发送的测试");for(EditText input:new EditText[]{first,second,send})layout.addView(input,new LinearLayout.LayoutParams(-1,110));layout.setFocusableInTouchMode(true);layout.requestFocus();activity.setContentView(layout);return true;});}
 private long startOwner(Config config)throws Exception{return owner.ui(()->{alive();synchronized(owner){yes(!owner.busy()&&!owner.running&&!owner.paused,"runtime_still_idle");runtime=new RuntimeBefore(owner);mutated=true;owner.workflow=null;owner.activeRequest=new TaskRequest(activity.getPackageName(),"本机输入连接检查","",false);owner.runConfig=config;owner.runId=fixtureRun;owner.ownerSession="";owner.allowed=activity.getPackageName();owner.task="本机输入连接检查";owner.checkout=false;owner.trace=new StringBuilder();owner.status="本机输入连接检查";owner.outcomeDetail="";owner.pendingDispatch=false;owner.recoveryHold="";owner.currentObservation=null;owner.model=new ModelClient();owner.modelCalls=owner.actions=owner.screenshots=owner.launchRecoveries=0;owner.nextStep=1;owner.activeMillis=0;owner.activeSince=SystemClock.elapsedRealtime();owner.startedAt=System.currentTimeMillis();owner.running=true;owner.paused=false;owner.generation=owner.ownership.start();owner.runtimeHealth.phase("starting");return owner.generation;}});}

 private void run(){try{
  yes((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"debug_only");
  if(!AgentImeLease.enabled(activity)){report.put("needs_user_enable",true);stage="needs_user_enable";return;}
  report.put("needs_user_enable",false);
  owner=ControlService.instance;yes(owner!=null&&owner.connected&&!owner.busy()&&!owner.running&&!owner.paused,"runtime_idle");
  yes(!owner.agentImeLease.status().optBoolean("recovery_record_present"),"no_existing_recovery_record");
  originalIme=selected();yes(!originalIme.isEmpty()&&!AgentImeLease.imeId(activity).equals(originalIme)&&enabled(originalIme),"restorable_original_ime_selected");
  persistent=new PersistentBefore(activity);
  Config config=new Config();config.endpoint="https://agent-ime-fixture.invalid/v1";config.model="local-fixture";config.key="LOCAL_FIXTURE";config.vision=true;
  grantChanged=true;new RoutineAccess(activity).approve(config,Collections.singletonList(activity.getPackageName()));
  yes(new RoutineAccess(activity).allows(config,Collections.singletonList(activity.getPackageName())),"owned_local_scope_granted");
  createUi();long g=startOwner(config);stage="first_acquire";focus(first);
  yes(owner.agentImeLease.acquire(g),"production_lease_acquired");
  yes(AgentImeLease.imeId(activity).equals(selected()),"agent_selection_read_back");
  AgentInputMethodService.EditorSnapshot a=editor(g,EditorInfo.IME_ACTION_SEARCH,-1);
  yes("公开测试".equals(read(g,a.epoch)),"real_input_connection_complete_read");realConnectionTested=true;
  yes(owner.ui(()->AgentInputMethodService.instance.replaceText(owner,g,a.epoch,"本机查询")),"production_replace_accepted");
  await(()->"本机查询".equals(read(g,a.epoch)),1500,"replacement_read_back");
  int beforeSearch=searches.get();yes(owner.ui(()->AgentInputMethodService.instance.submitSearch(owner,g,a.epoch)),"production_search_accepted");
  await(()->searches.get()==beforeSearch+1,1500,"real_search_listener_once");yes(sends.get()==0,"no_send_listener");
  stage="cross_field";focus(second);AgentInputMethodService.EditorSnapshot b=editor(g,EditorInfo.IME_ACTION_SEARCH,a.epoch);
  yes(b.epoch!=a.epoch,"framework_editor_epoch_changed");
  yes(denied(()->AgentInputMethodService.instance.replaceText(owner,g,a.epoch,"不得写入"),"agent_ime_editor_changed"),"old_field_replace_rejected");
  yes(denied(()->AgentInputMethodService.instance.submitSearch(owner,g,a.epoch),"agent_ime_editor_changed"),"old_field_search_rejected");
  yes("另一个公开字段".equals(read(g,b.epoch))&&searches.get()==beforeSearch+1,"other_field_untouched");
  stage="takeover_restore";owner.ui(()->{owner.pauseForUser();return true;});
  yes(owner.paused&&!owner.running&&!owner.ownership.accepts(g),"production_takeover_preserves_paused_goal");restored("takeover_original_ime_verified");
  stage="resume_acquire";long resumed=owner.ui(()->{alive();synchronized(owner){yes(owner.paused&&!owner.running,"synthetic_resume_requires_paused_owner");owner.running=true;owner.paused=false;owner.activeSince=SystemClock.elapsedRealtime();owner.generation=owner.ownership.resume();owner.runtimeHealth.phase("starting");return owner.generation;}});
  yes(resumed!=g,"resumed_owner_has_new_epoch");focus(first);yes(owner.agentImeLease.acquire(resumed),"resumed_production_lease_acquired");
  AgentInputMethodService.EditorSnapshot again=editor(resumed,EditorInfo.IME_ACTION_SEARCH,-1);
  yes(denied(()->AgentInputMethodService.instance.replaceText(owner,g,again.epoch,"不得写入"),"agent_ime_lease_unavailable"),"old_owner_generation_rejected");
  stage="send_boundary";focus(send);AgentInputMethodService.EditorSnapshot noSearch=editor(resumed,EditorInfo.IME_ACTION_SEND,again.epoch);int searchCount=searches.get();
  yes(denied(()->AgentInputMethodService.instance.submitSearch(owner,resumed,noSearch.epoch),"agent_ime_search_action_unavailable"),"send_editor_cannot_submit_search");
  yes(searches.get()==searchCount&&sends.get()==0,"send_rejection_has_no_editor_action");restored("rejected_action_original_ime_verified");
  stage="stop_restore";focus(first);yes(owner.agentImeLease.acquire(resumed),"lease_reacquired_before_stop");editor(resumed,EditorInfo.IME_ACTION_SEARCH,-1);
  owner.ui(()->{owner.stop("本机输入法检查结束");return true;});restored("stop_original_ime_verified");
  yes(!owner.busy()&&!owner.running&&!owner.paused,"production_stop_releases_owner");completed=true;stage="contracts_passed";
 }catch(Throwable failure){try{report.put("failure_type",failure.getClass().getSimpleName()).put("failure_stage",stage);}catch(Exception ignored){}}
 finally{finish();}}

 private void finish(){boolean runtimeRestored=!mutated,prefsRestored=!grantChanged,imeRestored=originalIme.isEmpty(),cleanup=true;
  JSONArray cleanupFailures=new JSONArray();
  // Every phase is independent: a platform/binder failure must not skip the saved grant restore.
  if(ownRun())try{yes(owner.ui(()->{if(!ownRun())return false;if(owner.busy()||owner.running||owner.paused)owner.stop("本机输入法检查清理");else owner.agentImeLease.releaseAsync();return true;}),"cleanup_owned_stop_attempted");}catch(Throwable failure){cleanup=false;cleanupFailures.put("owned_stop:"+failure.getClass().getSimpleName());}
  else if(mutated){cleanup=false;cleanupFailures.put("owner_changed");}
  if(!originalIme.isEmpty())try{long until=SystemClock.elapsedRealtime()+4500;while(SystemClock.elapsedRealtime()<until){if(originalIme.equals(selected())&&(owner==null||!owner.agentImeLease.status().optBoolean("recovery_record_present"))){imeRestored=true;break;}Thread.sleep(70);}}catch(Throwable failure){cleanup=false;cleanupFailures.put("ime_readback:"+failure.getClass().getSimpleName());}
  if(first!=null)try{owner.ui(()->{first.clearFocus();second.clearFocus();send.clearFocus();((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(first.getWindowToken(),0);return true;});}catch(Throwable failure){cleanup=false;cleanupFailures.put("editor_close:"+failure.getClass().getSimpleName());}
  // A failed platform restore retains its durable IME recovery ticket, not the temporary grant/task.
  if(runtime!=null)try{runtimeRestored=owner.ui(()->{if(!ownRun()||owner.busy())return false;runtime.restore(owner);return runtime.matches(owner);});}catch(Throwable failure){cleanup=false;cleanupFailures.put("runtime_restore:"+failure.getClass().getSimpleName());}
  if(persistent!=null){
   try{persistent.restoreGrant(activity);}catch(Throwable failure){cleanup=false;cleanupFailures.put("grant_restore:"+failure.getClass().getSimpleName());}
   if(!mutated||runtimeRestored){
    try{persistent.restoreJournal(activity);}catch(Throwable failure){cleanup=false;cleanupFailures.put("journal_restore:"+failure.getClass().getSimpleName());}
    try{persistent.restoreReport(activity);}catch(Throwable failure){cleanup=false;cleanupFailures.put("report_restore:"+failure.getClass().getSimpleName());}
   }
   try{prefsRestored=persistent.matches(activity);}catch(Throwable failure){cleanup=false;cleanupFailures.put("state_readback:"+failure.getClass().getSimpleName());}
  }
  try{
   cleanup&=runtimeRestored&&prefsRestored&&imeRestored;
   report.put("success",completed&&cleanup).put("stage",stage).put("checks",checks).put("passed",passed).put("cleanup_failures",cleanupFailures).put("sampledAt",System.currentTimeMillis()).put("elapsed_ms",SystemClock.elapsedRealtime()-started)
    .put("source","framework_bound_agent_ime_owned_editors").put("synthetic_runtime_owner",true).put("runtime_resume","RunOwnership.resume").put("production_start_tested",false).put("production_resumeFromUser_tested",false).put("business_worker_started",false)
    .put("real_input_connection_tested",realConnectionTested).put("system_ime_enable_requested",false).put("system_settings_written",false).put("network_calls",0).put("third_party_actions",0).put("microphone_used",false).put("editor_contents_recorded",false).put("ime_ids_recorded",false)
    .put("search_listener_calls",searches.get()).put("send_listener_calls",sends.get()).put("runtime_mutated",mutated).put("temporary_grant_written",grantChanged).put("runtime_restored",runtimeRestored).put("persistent_state_restored",prefsRestored).put("original_ime_restore_required",mutated).put("original_ime_restored",imeRestored).put("cleanup_complete",cleanup).put("ownership_epoch_rewound",false).put("cooperative_deadline_ms",45000);
   try(FileOutputStream output=activity.openFileOutput("agent-ime-contracts.json",Context.MODE_PRIVATE)){output.write(report.toString(2).getBytes("UTF-8"));}
  }catch(Exception ignored){}
  activity.runOnUiThread(activity::finish);
 }

 /** Only touched state is retained in memory; no history/configuration/IME identity enters the report. */
 private static final class RuntimeBefore {
  final String allowed,task,run,session,status,detail,recovery;final boolean checkout,pending,awaiting,invalidated;
  final Config config;final TaskWorkflow workflow;final TaskRequest request;final ControlService.Observation observation;final ModelClient model;final StringBuilder trace;
  final int calls,shots,actions,recoveries,next;final long started,activeSince,activeMillis;final String healthPhase;
  RuntimeBefore(ControlService s)throws Exception{allowed=s.allowed;task=s.task;run=s.runId;session=s.ownerSession;status=s.status;detail=s.outcomeDetail;recovery=s.recoveryHold;checkout=s.checkout;pending=s.pendingDispatch;awaiting=s.awaitingDecision;invalidated=s.decisionInvalidated;config=s.runConfig;workflow=s.workflow;request=s.activeRequest;observation=s.currentObservation;model=s.model;trace=s.trace;calls=s.modelCalls;shots=s.screenshots;actions=s.actions;recoveries=s.launchRecoveries;next=s.nextStep;started=s.startedAt;activeSince=s.activeSince;activeMillis=s.activeMillis;healthPhase=s.runtimeHealth.snapshot().optString("phase","idle");}
  void restore(ControlService s){s.allowed=allowed;s.task=task;s.runId=run;s.ownerSession=session;s.status=status;s.outcomeDetail=detail;s.recoveryHold=recovery;s.checkout=checkout;s.pendingDispatch=pending;s.awaitingDecision=awaiting;s.decisionInvalidated=invalidated;s.runConfig=config;s.workflow=workflow;s.activeRequest=request;s.currentObservation=observation;s.model=model;s.trace=trace;s.modelCalls=calls;s.screenshots=shots;s.actions=actions;s.launchRecoveries=recoveries;s.nextStep=next;s.startedAt=started;s.activeSince=activeSince;s.activeMillis=activeMillis;s.running=false;s.paused=false;s.runtimeHealth.phase(healthPhase);}
  boolean matches(ControlService s){return !s.running&&!s.paused&&!s.busy()&&s.allowed.equals(allowed)&&s.task.equals(task)&&s.runId.equals(run)&&s.ownerSession.equals(session)&&s.status.equals(status)&&s.outcomeDetail.equals(detail)&&s.recoveryHold.equals(recovery)&&s.checkout==checkout&&s.pendingDispatch==pending&&s.awaitingDecision==awaiting&&s.decisionInvalidated==invalidated&&s.runConfig==config&&s.workflow==workflow&&s.activeRequest==request&&s.currentObservation==observation&&s.model==model&&s.trace==trace&&s.modelCalls==calls&&s.screenshots==shots&&s.actions==actions&&s.launchRecoveries==recoveries&&s.nextStep==next&&s.startedAt==started&&s.activeSince==activeSince&&s.activeMillis==activeMillis;}
 }
 private static final class PersistentBefore {
  final String[] keys={RoutineAccess.KEY,TaskCheckpoint.KEY,"last-outcome"};final String[] values=new String[keys.length];final boolean[] exists=new boolean[keys.length];final byte[] lastRun;final boolean fileExists;
  PersistentBefore(Context context)throws Exception{SharedPreferences vault=context.getSharedPreferences("vault",0);for(int i=0;i<keys.length;i++){exists[i]=vault.contains(keys[i]);values[i]=vault.getString(keys[i],"");}File file=new File(context.getFilesDir(),"last-run.json");fileExists=file.exists();lastRun=fileExists?bytes(file):null;}
  void restoreGrant(Context context)throws Exception{restoreKeys(context,0,1);}
  void restoreJournal(Context context)throws Exception{restoreKeys(context,1,keys.length);}
  void restoreKeys(Context context,int from,int to)throws Exception{SharedPreferences.Editor edit=context.getSharedPreferences("vault",0).edit();for(int i=from;i<to;i++){if(exists[i])edit.putString(keys[i],values[i]);else edit.remove(keys[i]);}if(!edit.commit())throw new IllegalStateException("fixture_prefs_restore_failed");}
  void restoreReport(Context context)throws Exception{File file=new File(context.getFilesDir(),"last-run.json");if(fileExists){try(FileOutputStream output=new FileOutputStream(file)){output.write(lastRun);}}else if(file.exists()&&!file.delete())throw new IllegalStateException("fixture_report_cleanup_failed");}
  boolean matches(Context context)throws Exception{SharedPreferences vault=context.getSharedPreferences("vault",0);for(int i=0;i<keys.length;i++)if(exists[i]!=vault.contains(keys[i])||exists[i]&&!values[i].equals(vault.getString(keys[i],"")))return false;File file=new File(context.getFilesDir(),"last-run.json");return fileExists==file.exists()&&(!fileExists||Arrays.equals(lastRun,bytes(file)));}
  static byte[] bytes(File file)throws Exception{try(FileInputStream input=new FileInputStream(file);ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int read;while((read=input.read(buffer))!=-1)output.write(buffer,0,read);return output.toByteArray();}}
 }
}
