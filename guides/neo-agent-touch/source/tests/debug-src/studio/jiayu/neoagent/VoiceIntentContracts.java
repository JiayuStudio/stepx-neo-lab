package studio.jiayu.neoagent;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Main callbacks and durable production transfer; synthetic owners/planner and gated scheduler, no mic/API. */
final class VoiceIntentContracts {
 final UiQaActivity a;final ControlService s;final JSONObject report=new JSONObject();final JSONArray passed=new JSONArray(),cleanupFailures=new JSONArray();
 final ArrayList<VoicePlanningContracts.Gate> gates=new ArrayList<>();final ArrayList<Thread> workers=new ArrayList<>();
 final HashSet<String> ownedRuns=new HashSet<>();
 final AtomicInteger dispatched=new AtomicInteger(),scheduled=new AtomicInteger(),lateStarts=new AtomicInteger();final AtomicReference<Throwable> gateFailure=new AtomicReference<>();
 MainActivity.PlanningSource oldPlanner;MainActivity.TaskStarter oldStarter;ControlService.VoiceWorkerScheduler oldScheduler;VoiceGoalTransfer.Storage oldStorage;VoiceGoalTransfer.RuntimeSnapshot before;ModelClient oldModel;
 SavedFiles files;Helpers helpers;String oldRun="",stage="preconditions";boolean captured,mutated,complete,recoveryTested,terminalTested;int checks;long oldGeneration;final long started=SystemClock.elapsedRealtime();
 static void run(UiQaActivity activity){new Thread(()->new VoiceIntentContracts(activity).run(),"voice-intent-contracts").start();}
 VoiceIntentContracts(UiQaActivity activity){a=activity;s=ControlService.instance;}
 <T>T ui(Callable<T> callable)throws Exception{CompletableFuture<T> done=new CompletableFuture<>();a.runOnUiThread(()->{try{done.complete(callable.call());}catch(Throwable failure){done.completeExceptionally(failure);}});return done.get(5,TimeUnit.SECONDS);}
 void yes(boolean condition,String label)throws Exception{if(!condition)throw new IllegalStateException(label);passed.put(label);checks++;}
 VoicePlanningContracts.Gate gate(boolean fail)throws Exception{VoicePlanningContracts.Gate gate=new VoicePlanningContracts.Gate(fail);gates.add(gate);ui(()->{a.planningSource=gate;return true;});return gate;}
 void finalText(VoicePlanningContracts.Gate gate)throws Exception{ui(()->{a.voiceOwnerSession=a.sessionId;a.voiceResult("打开Neo Agent Touch 本地演示搜索测试",true);return true;});yes(gate.entered.await(3,TimeUnit.SECONDS),"final_enters_original_planner");workers.add(ui(()->a.planningWorker));}
 void drain(VoicePlanningContracts.Gate gate)throws Exception{Thread worker=ui(()->a.planningWorker);gate.release.countDown();worker.join(3500);yes(!worker.isAlive(),"local_planner_drained");ui(()->true);}
 String begin()throws Exception{return ui(()->{String request=a.beginVoiceRequest();a.prepareVoiceIntent(request);return request;});}
 boolean predecessor()throws Exception{return ui(()->{if(!oldRun.equals(s.runId)||!s.paused||s.running||!s.busy())return false;JSONObject checkpoint=new JSONObject(new Vault(a).get(TaskCheckpoint.KEY));return oldRun.equals(checkpoint.optString("run"))&&"本机保留目标".equals(checkpoint.optString("goal"))&&a.sessionId.equals(checkpoint.optString("session"));});}
 TaskWorkflow plan()throws Exception{TaskWorkflow plan=new TaskWorkflow(a,VoicePlanningContracts.task(),"打开Neo Agent Touch 本地演示搜索测试");plan.baseSteps=24;return plan;}
 boolean reject(ControlService.VoiceHold hold,Config config,TaskWorkflow plan)throws Exception{try{return !ui(()->s.replaceHeldVoice(hold,config,plan,a.sessionId));}catch(Exception expected){return true;}}
 void recoveryPhases()throws Exception{
  Vault vault=new Vault(a);String prior=vault.get(TaskCheckpoint.KEY);String candidate=new JSONObject(prior).put("run","fixture-unreleased-candidate").put("goal","未派发的公开候选").toString();
  for(String phase:new String[]{"prepared","committed"}){
   vault.put(TaskCheckpoint.KEY,candidate);vault.put(VoiceGoalTransfer.JOURNAL,new JSONObject().put("version",1).put("phase",phase).put("before",prior).put("after",candidate).toString());
   yes(candidate.equals(vault.get(TaskCheckpoint.KEY))&&phase.equals(new JSONObject(vault.get(VoiceGoalTransfer.JOURNAL)).getString("phase")),phase+"_surviving_journal_written_and_read_back");
   ui(()->{VoiceGoalTransfer.recover(a);return true;});
   yes(prior.equals(vault.get(TaskCheckpoint.KEY))&&vault.get(VoiceGoalTransfer.JOURNAL).isEmpty()&&predecessor(),phase+"_same_process_recovery_restores_before_and_clears_journal");
  }
  recoveryTested=true;
 }
 void lateTerminalCalls()throws Exception{
  final String[] current=ui(()->new String[]{s.runId,s.outcomeDetail,s.status,a.getSharedPreferences("vault",0).getString("last-outcome","")});
  AtomicInteger rejected=new AtomicInteger();AtomicReference<Throwable> unexpected=new AtomicReference<>();
  Thread delayed=new Thread(()->{
   try{s.commitTerminalDetail(oldGeneration,"旧执行器迟到的公开完成说明");}catch(Exception stale){if("任务已停止".equals(stale.getMessage()))rejected.incrementAndGet();else unexpected.set(stale);}
   try{s.commitAsk(oldGeneration,"旧执行器迟到的公开接管说明");}catch(Exception stale){if("任务已停止".equals(stale.getMessage()))rejected.incrementAndGet();else unexpected.set(stale);}
  },"voice-old-terminal-callback");workers.add(delayed);delayed.start();delayed.join(3500);
  yes(!delayed.isAlive()&&rejected.get()==2&&unexpected.get()==null,"old_generation_terminal_and_ask_helpers_rejected");
  yes(ui(()->s.running&&!s.paused&&current[0].equals(s.runId)&&current[1].equals(s.outcomeDetail)&&current[2].equals(s.status)&&current[3].equals(a.getSharedPreferences("vault",0).getString("last-outcome",""))),"late_old_terminal_callbacks_leave_new_run_and_outcome_unchanged");
  terminalTested=true;
 }
 void run(){try{
  yes((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"debug_only");
  // This owned debug Activity ends after the fixture. Never deliver its synthetic outcome via refresh.
  ui(()->{a.handler.removeCallbacks(a.refresh);return true;});
  yes(s!=null&&s.connected&&!s.busy()&&!s.running&&!s.paused,"original_runtime_idle");
  yes(ui(()->a.resumed&&a.loaded&&"intent".equals(a.voiceMode())&&!a.voiceUiTest()),"fresh_isolated_intent_ui");yes(new AppAccess(a).configured(),"existing_app_configuration");
  yes(!s.purchase.pendingForRun(),"no_pending_transaction");yes(new Vault(a).get(VoiceGoalTransfer.JOURNAL).isEmpty(),"no_existing_transfer_journal");
  files=new SavedFiles(a);ui(()->{before=new VoiceGoalTransfer.RuntimeSnapshot(s);oldModel=s.model;helpers=new Helpers(s);oldPlanner=a.planningSource;oldStarter=a.taskStarter;oldScheduler=s.voiceWorkerScheduler;oldStorage=s.voiceGoals.storage;return true;});captured=true;
  Config current=Config.load(a);current.validate();new RoutineAccess(a).approve(current,Collections.singletonList(a.getPackageName()));
  ui(()->{a.taskStarter=(activity,service)->{lateStarts.incrementAndGet();return false;};return true;});
  stage="old_planner_interruption";VoicePlanningContracts.Gate stale=gate(false);ui(()->{a.sendMessage("打开Neo Agent Touch 本地演示搜索测试");return true;});yes(stale.entered.await(3,TimeUnit.SECONDS),"old_planner_entered");Thread staleWorker=ui(()->a.planningWorker);workers.add(staleWorker);long oldTurn=ui(()->a.turn);String superseding=begin();
  yes(ui(()->a.turn>oldTurn&&!a.planning&&a.pendingToken.isEmpty()),"microphone_intent_revokes_old_planner_synchronously");int messages=ui(()->a.chat().getJSONArray("messages").length());stale.release.countDown();staleWorker.join(3500);ui(()->true);
  yes(ui(()->!staleWorker.isAlive()&&lateStarts.get()==0&&a.pendingToken.isEmpty()&&a.chat().getJSONArray("messages").length()==messages),"late_old_plan_cannot_present_or_start");ui(()->{a.cancelVoiceRequest(superseding);a.taskStarter=oldStarter;return true;});
  stage="owned_running_predecessor";oldRun=UUID.randomUUID().toString();ownedRuns.add(oldRun);Config local=new Config();local.endpoint="https://voice-intent-fixture.invalid/v1";local.model="fixture";local.key="LOCAL_FIXTURE";
  new RoutineAccess(a).approve(local,Collections.singletonList(a.getPackageName()));
  ui(()->{mutated=true;s.model=new ModelClient();s.runId=oldRun;s.ownerSession=a.sessionId;s.allowed=a.getPackageName();s.task="本机保留目标";s.runConfig=local;s.activeRequest=new TaskRequest(s.allowed,s.task,"",false);s.workflow=null;s.document=new DocumentRuntime(s);s.purchase=new PurchaseRuntime(s);s.trace=new StringBuilder();s.outcomeDetail="";s.pendingDispatch=false;s.recoveryHold="";s.startedAt=System.currentTimeMillis();s.activeMillis=0;s.activeSince=SystemClock.elapsedRealtime();s.running=true;s.paused=false;s.generation=s.ownership.start();oldGeneration=s.generation;s.runtimeHealth.phase("starting");TaskCheckpoint.save(s);return true;});
  stage="planner_failure_preserves_predecessor";begin();yes(predecessor(),"real_hold_pauses_and_journals_predecessor");VoicePlanningContracts.Gate failure=gate(true);finalText(failure);drain(failure);yes(predecessor(),"planner_failure_keeps_paused_checkpoint");
  stage="cancel_preserves_predecessor";String cancel=begin();VoicePlanningContracts.Gate cancelled=gate(false);finalText(cancelled);ui(()->{a.cancelVoiceRequest(cancel);return true;});drain(cancelled);yes(predecessor()&&scheduled.get()==0,"cancelled_new_plan_keeps_old_checkpoint");
  stage="draft_preserves_predecessor";begin();long draftTurn=ui(()->a.turn);ui(()->{a.voiceOwnerSession=a.sessionId;a.voiceResult("公开草稿",false);return true;});yes(ui(()->a.turn==draftTurn&&!a.planning&&a.voiceIntent.ticket.phase()==VoiceIntentRules.Phase.DRAFT)&&predecessor(),"draft_does_not_replace_or_resume");
  stage="same_process_journal_recovery";recoveryPhases();
  stage="scope_and_hold_guards";begin();ControlService.VoiceHold staleHold=ui(()->a.voiceIntent.hold);Config denied=new Config();denied.endpoint="https://unapproved-voice-fixture.invalid/v1";denied.model="fixture";denied.key="LOCAL_FIXTURE";TaskWorkflow scopePlan=plan();yes(reject(staleHold,denied,scopePlan)&&predecessor(),"unapproved_scope_rejected_before_mutation");begin();yes(reject(staleHold,current,plan())&&predecessor(),"superseded_hold_cannot_replace");
  stage="scheduler_rollback";ControlService.VoiceHold rejectionHold=ui(()->a.voiceIntent.hold);long beforeEpoch=ui(()->s.generation);ui(()->{s.voiceWorkerScheduler=(cfg,pkg,goal,g,commit)->{ownedRuns.add(s.runId);commit.thenAccept(accepted->{if(Boolean.TRUE.equals(accepted))dispatched.incrementAndGet();});throw new IOException("fixture_scheduler_rejected");};return true;});
  yes(reject(rejectionHold,current,plan()),"scheduler_rejection_reported");yes(predecessor()&&ui(()->s.generation>beforeEpoch)&&dispatched.get()==0&&new Vault(a).get(VoiceGoalTransfer.JOURNAL).isEmpty(),"scheduler_failure_rolls_back_checkpoint_without_dispatch");
  stage="prepared_journal_failure";begin();ControlService.VoiceHold storageHold=ui(()->a.voiceIntent.hold);ui(()->{s.voiceGoals.storage=new VoiceGoalTransfer.Storage(){public String read(String key)throws Exception{return oldStorage.read(key);}public void write(String key,String value)throws Exception{if(VoiceGoalTransfer.JOURNAL.equals(key))throw new IOException("fixture_journal_rejected");oldStorage.write(key,value);}public void remove(String key)throws Exception{oldStorage.remove(key);}};return true;});
  try{yes(reject(storageHold,current,plan())&&predecessor(),"prepared_journal_failure_keeps_old_goal");}finally{ui(()->{s.voiceGoals.storage=oldStorage;return true;});}
  stage="successful_original_chat_transfer";ui(()->{s.voiceWorkerScheduler=(cfg,pkg,goal,g,commit)->{ownedRuns.add(s.runId);scheduled.incrementAndGet();commit.thenAccept(accepted->{if(!Boolean.TRUE.equals(accepted))return;try{JSONObject stored=new JSONObject(new Vault(a).get(TaskCheckpoint.KEY));if(!s.running||!s.ownership.accepts(g)||oldRun.equals(s.runId)||!s.runId.equals(stored.getString("run"))||!new Vault(a).get(VoiceGoalTransfer.JOURNAL).isEmpty())throw new IllegalStateException("gate_opened_before_durable_commit");dispatched.incrementAndGet();}catch(Throwable bad){gateFailure.set(bad);}});};return true;});
  String accepted=begin();VoicePlanningContracts.Gate success=gate(false);finalText(success);drain(success);
  yes(ui(()->s.running&&!s.paused&&!oldRun.equals(s.runId)&&s.runId.equals(a.voiceTaskRun)&&a.pendingToken.isEmpty()&&a.voiceIntent.ticket.phase()==VoiceIntentRules.Phase.COMMITTED),"original_chat_commits_exact_new_run");
  yes(dispatched.get()==1&&scheduled.get()==1&&gateFailure.get()==null,"scheduler_gate_opens_once_after_durable_commit");
  stage="late_old_terminal_callbacks";lateTerminalCalls();
  long committedTurn=ui(()->a.turn);ui(()->{a.voiceOwnerSession=a.sessionId;a.voiceResult("重复公开回调",true);return true;});yes(ui(()->a.turn==committedTurn)&&dispatched.get()==1,"duplicate_final_cannot_start_again");
  ui(()->{a.cancelVoiceRequest(cancel);return true;});yes(ui(()->s.running&&accepted.equals(a.voiceTaskRequest)),"old_cancel_preserves_committed_successor");complete=true;stage="contracts_passed";
 }catch(Throwable failure){try{report.put("failure_type",failure.getClass().getSimpleName()).put("failure_stage",stage);}catch(Exception ignored){}}
 finally{cleanup();}}
 void cleanup(){boolean runtimeRestored=!mutated,persistentRestored=files==null,helpersRestored=helpers==null;try{ui(()->{++a.turn;a.planning=false;a.planner.cancel();a.cancelVoice();a.clearPendingPlan();if(a.voiceIntent!=null)a.voiceIntents.abandon(a.voiceIntent.ticket);return true;});}catch(Throwable failure){cleanupFailures.put("revoke_callbacks");}
  for(VoicePlanningContracts.Gate gate:gates)gate.release.countDown();for(Thread worker:workers)try{worker.join(3500);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();cleanupFailures.put("planner_join");}
  if(captured){
   try{ui(()->{s.voiceWorkerScheduler=oldScheduler;s.voiceGoals.storage=oldStorage;a.planningSource=oldPlanner;a.taskStarter=oldStarter;return true;});}catch(Throwable failure){cleanupFailures.put("restore_seams");}
   try{ui(()->{if(mutated&&s==ControlService.instance&&ownedRuns.contains(s.runId)&&a.sessionId.equals(s.ownerSession)&&s.busy())s.stop("语音目标事务本机检查结束");return true;});}catch(Throwable failure){cleanupFailures.put("owned_stop");}
   try{runtimeRestored=ui(()->{if(s!=ControlService.instance||s.busy()||mutated&&!ownedRuns.contains(s.runId))return false;if(mutated){s.generation=s.ownership.end();before.restore(s);s.model=oldModel;s.runtimeHealth.phase("idle");}return !s.running&&!s.paused&&!s.busy()&&s.runId.equals(before.run)&&s.ownerSession.equals(before.session)&&s.document==before.document&&s.purchase==before.purchase&&s.workflow==before.workflow&&s.runConfig==before.config&&s.model==oldModel;});}catch(Throwable failure){cleanupFailures.put("runtime_restore");}
   if(runtimeRestored)try{helpersRestored=ui(()->{if(!s.busy()){helpers.restore(s);return helpers.matches(s);}return false;});}catch(Throwable failure){cleanupFailures.put("helpers_restore");}
  }
  if(files!=null){try{files.restoreGrant(a);}catch(Throwable failure){cleanupFailures.put("grant_restore");}if(runtimeRestored){try{files.restoreJournal(a);}catch(Throwable failure){cleanupFailures.put("journal_restore");}try{files.restoreReport(a);}catch(Throwable failure){cleanupFailures.put("report_restore");}try{files.removeTestKeys(a);}catch(Throwable failure){cleanupFailures.put("test_namespace_cleanup");}}try{persistentRestored=files.matches(a);}catch(Throwable failure){cleanupFailures.put("persistent_readback");}}
  try{boolean clean=runtimeRestored&&persistentRestored&&helpersRestored&&cleanupFailures.length()==0;report.put("success",complete&&clean).put("stage",stage).put("checks",checks).put("passed",passed).put("cleanup_failures",cleanupFailures).put("cleanup_verified",clean).put("runtime_restored",runtimeRestored).put("persistent_state_restored",persistentRestored).put("helper_state_restored",helpersRestored).put("ownership_epoch_rewound",false)
   .put("source","synthetic final -> production MainActivity planner callbacks -> durable VoiceGoalTransfer -> gated scheduler stub").put("synthetic_runtime_owner",true).put("planner_stubbed",true).put("scheduler_stubbed",true).put("business_worker_executed",false).put("native_commit_gate_count",dispatched.get()).put("same_process_recovery",recoveryTested).put("process_restart_tested",false).put("late_old_generation_terminal_helpers_tested",terminalTested).put("old_worker_thread_simulated",true).put("network_calls",0).put("third_party_actions",0).put("microphone_used",false).put("microphone_permission_requested",false).put("asr_accuracy_tested",false).put("production_preferences_used_for_config_only",true).put("private_content_recorded",false).put("sampledAt",System.currentTimeMillis()).put("elapsed_ms",SystemClock.elapsedRealtime()-started);try(FileOutputStream output=a.openFileOutput("voice-intent-contracts.json",0)){output.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.runOnUiThread(a::finish);
 }
 static final class SavedFiles {
  final String[] keys={RoutineAccess.KEY,TaskCheckpoint.KEY,VoiceGoalTransfer.JOURNAL,"last-outcome"};final String[] values=new String[keys.length];final boolean[] exists=new boolean[keys.length];final byte[] run;
  SavedFiles(UiQaActivity a)throws Exception{SharedPreferences prefs=a.getSharedPreferences("vault",0);for(int i=0;i<keys.length;i++){exists[i]=prefs.contains(keys[i]);values[i]=prefs.getString(keys[i],"");}File file=new File(a.getFilesDir(),"last-run.json");run=file.isFile()?Files.readAllBytes(file.toPath()):null;}
  void keys(UiQaActivity a,int from,int to)throws Exception{SharedPreferences.Editor edit=a.getSharedPreferences("vault",0).edit();for(int i=from;i<to;i++){if(exists[i])edit.putString(keys[i],values[i]);else edit.remove(keys[i]);}if(!edit.commit())throw new IOException("fixture_restore_failed");}
  void restoreGrant(UiQaActivity a)throws Exception{keys(a,0,1);}
  void restoreJournal(UiQaActivity a)throws Exception{keys(a,1,keys.length);}
  void restoreReport(UiQaActivity a)throws Exception{File file=new File(a.getFilesDir(),"last-run.json");if(run==null)Files.deleteIfExists(file.toPath());else Files.write(file.toPath(),run);}
  void removeTestKeys(UiQaActivity a){new Vault(a).remove("test-voice-chats");new Vault(a).remove("test-voice-user-memory");}
  boolean matches(UiQaActivity a)throws Exception{SharedPreferences prefs=a.getSharedPreferences("vault",0);for(int i=0;i<keys.length;i++)if(exists[i]!=prefs.contains(keys[i])||exists[i]&&!values[i].equals(prefs.getString(keys[i],"")))return false;File file=new File(a.getFilesDir(),"last-run.json");return run==null?!file.exists():file.isFile()&&Arrays.equals(run,Files.readAllBytes(file.toPath()));}
 }
 /** Only debug memory backup; raw cache entries are never serialized or emitted. */
 static final class Helpers {
  final Map<String,Object> memory;final long[] touches;final ArrayDeque<RejectedActions.Entry> rejected;final LinkedHashMap<Integer,AccessibilityNodeInfo> nodes;final ArrayDeque<?> scenes;final int sequence,requests,calls;final String tool;final boolean initialized,negotiated;final Field recentField,sequenceField;
  Helpers(ControlService s)throws Exception{memory=new LinkedHashMap<>(s.workingMemory.snapshot());touches=s.touchPoints.snapshot();rejected=new ArrayDeque<>(s.rejectedActions.entries);nodes=new LinkedHashMap<>(s.nodes);recentField=PixelScene.class.getDeclaredField("recent");sequenceField=PixelScene.class.getDeclaredField("sequence");recentField.setAccessible(true);sequenceField.setAccessible(true);scenes=new ArrayDeque<>((ArrayDeque<?>)recentField.get(s.visualScenes));sequence=sequenceField.getInt(s.visualScenes);requests=s.deviceMcp.requests;calls=s.deviceMcp.toolCalls;tool=s.deviceMcp.lastTool;initialized=s.deviceMcp.initialized;negotiated=s.deviceMcp.negotiated;}
  @SuppressWarnings({"unchecked","rawtypes"}) void restore(ControlService s)throws Exception{s.workingMemory.restore((String)memory.get("summary"),(String)memory.get("source_observation"),(String)memory.get("proposed_action"),(Boolean)memory.get("action_dispatched"));s.touchPoints.restore(touches);s.rejectedActions.entries.clear();s.rejectedActions.entries.addAll(rejected);s.nodes.clear();s.nodes.putAll(nodes);ArrayDeque target=(ArrayDeque)recentField.get(s.visualScenes);target.clear();target.addAll(scenes);sequenceField.setInt(s.visualScenes,sequence);s.deviceMcp.requests=requests;s.deviceMcp.toolCalls=calls;s.deviceMcp.lastTool=tool;s.deviceMcp.initialized=initialized;s.deviceMcp.negotiated=negotiated;}
  boolean matches(ControlService s)throws Exception{long[] current=s.touchPoints.snapshot(),original=touches.clone();Arrays.sort(current);Arrays.sort(original);return memory.equals(s.workingMemory.snapshot())&&Arrays.equals(current,original)&&new ArrayList<>(s.rejectedActions.entries).equals(new ArrayList<>(rejected))&&s.nodes.equals(nodes)&&sequenceField.getInt(s.visualScenes)==sequence&&new ArrayList<>((ArrayDeque<?>)recentField.get(s.visualScenes)).equals(new ArrayList<>(scenes))&&s.deviceMcp.requests==requests&&s.deviceMcp.toolCalls==calls&&s.deviceMcp.lastTool.equals(tool)&&s.deviceMcp.initialized==initialized&&s.deviceMcp.negotiated==negotiated;}
 }
}
