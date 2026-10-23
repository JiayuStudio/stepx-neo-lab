package studio.jiayu.neoagent;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONObject;

/** A new spoken goal cannot discard its paused predecessor before durable commit. */
final class VoiceGoalTransfer {
 static final String JOURNAL="voice-goal-transfer-v1";
 interface Storage {String read(String key)throws Exception;void write(String key,String value)throws Exception;void remove(String key)throws Exception;}
 final ControlService owner;
 Storage storage;
 private ControlService.VoiceHold current;
 VoiceGoalTransfer(ControlService service){owner=service;storage=new Storage(){
  public String read(String key)throws Exception{return new Vault(owner).get(key);}
  public void write(String key,String value)throws Exception{new Vault(owner).put(key,value);}
  public void remove(String key)throws Exception{new Vault(owner).remove(key);}
 };}
 private void mainThread()throws Exception{if(Looper.myLooper()!=Looper.getMainLooper())throw new Exception("语音目标交接需要主线程");}
 void invalidate(){current=null;}
 ControlService.VoiceHold hold(String session,String request)throws Exception{
  mainThread();if(owner!=ControlService.instance||!owner.connected)throw new Exception("无障碍服务未连接，旧目标保留");
  if(request==null||request.isEmpty()||session==null||session.isEmpty())throw new Exception("语音请求已失效，旧目标保留");
  if(!owner.busy()){current=null;return null;}
  if(!session.equals(owner.ownerSession))throw new Exception("另一对话的目标仍在执行，请先在原对话接管");
  if(owner.running)owner.pauseForUser();
  if(!owner.paused||owner.running||owner.runConfig==null)throw new Exception("旧目标未能暂停，语音目标未替换");
  String checkpoint=TaskCheckpoint.snapshot(owner).toString();
  writeReadback(TaskCheckpoint.KEY,checkpoint);
  current=new ControlService.VoiceHold(owner,request,session,owner.runId,owner.generation,checkpoint);
  return current;
 }
 boolean held(ControlService.VoiceHold hold){try{return hold!=null&&current==hold&&hold.owner==owner&&owner==ControlService.instance&&owner.connected
  &&owner.paused&&!owner.running&&owner.busy()&&owner.generation==hold.generation&&hold.run.equals(owner.runId)
  &&hold.session.equals(owner.ownerSession)&&hold.checkpoint.equals(storage.read(TaskCheckpoint.KEY));}catch(Exception unreadable){return false;}}
 private void writeReadback(String key,String value)throws Exception{storage.write(key,value);if(!value.equals(storage.read(key)))throw new Exception("目标交接记录未能完整读回，旧目标保留");}
 private void clearJournal()throws Exception{storage.remove(JOURNAL);if(!storage.read(JOURNAL).isEmpty())throw new Exception("目标交接记录清理未核验");}
 boolean replace(ControlService.VoiceHold hold,Config config,TaskWorkflow plan,String session)throws Exception{
  mainThread();if(!held(hold)||!hold.session.equals(session))throw new Exception("语音对应的旧目标已变化，未替换当前目标");
  config.validate();if(!config.vision||plan==null||plan.index!=0||plan.steps.isEmpty()||plan.steps.size()>6||plan.baseSteps<1||plan.baseSteps>40)throw new Exception("新目标计划无效，旧目标保留");
  if(owner.worker.isShutdown())throw new Exception("控制服务正在关闭，旧目标保留");
  for(String pkg:plan.packages())owner.requireDataGrant(config,pkg);
  TaskSpec first=plan.current();Config stage=first.config(config,plan.baseSteps);
  if(DocumentScope.applies(first.pkg)||PurchasePolicy.grant(first.original,first.pkg).order||(AppCatalog.isCommerce(first.pkg)&&ShoppingIntent.preview(first.original)))stage.maxSteps=Math.max(stage.maxSteps,40);
  stage.maxSteps=GameplayIntent.steps(first.original,stage.maxSteps);stage.validate();
  // The independent purchase ledger is preserved. A new voice intent does not cancel or
  // replay an issued order; its normal per-order/run checks still govern any financial tool.
  if(!held(hold))throw new Exception("旧目标已变化，语音替换取消");
  String run=UUID.randomUUID().toString();long started=System.currentTimeMillis();
  JSONObject workflow=plan.json();workflow.getJSONArray("tasks").getJSONObject(0).put("state","running");
  String after=new JSONObject().put("version",1).put("at",started).put("app",first.pkg).put("goal",first.goal).put("run",run).put("session",session)
   .put("config",stage.json()).put("checkout",first.checkout).put("original",first.original).put("target",first.target).put("step",1).put("active",0).put("started",started)
   .put("calls",0).put("shots",0).put("actions",0).put("working_memory",new JSONObject(new TaskMemory().snapshot())).put("trace","语音新目标已准备\n")
   .put("pending",false).put("document",new DocumentRuntime(owner).checkpoint()).put("workflow",workflow).put("used_touches",new JSONArray()).toString();
  JSONObject journal=new JSONObject().put("version",1).put("phase","prepared").put("before",hold.checkpoint).put("after",after);
  String prepared=journal.toString();
  writeReadback(JOURNAL,prepared); // The active predecessor has not changed yet.
  RuntimeSnapshot before=new RuntimeSnapshot(owner);
  CompletableFuture<Boolean> gate=new CompletableFuture<>();boolean staged=false,committed=false;
  try{
   if(!held(hold))throw new Exception("旧目标在准备期间变化，未替换");
   owner.generation=owner.ownership.replacePaused(hold.generation);staged=true;
   owner.allowed=first.pkg;owner.task=first.goal;owner.runId=run;owner.ownerSession=session;owner.activeRequest=new TaskRequest(first.pkg,first.original,first.target,first.checkout,first.sharedComparison);
   owner.checkout=owner.activeRequest.checkout;owner.runConfig=stage;owner.workflow=plan;owner.running=true;owner.paused=false;owner.nextStep=1;owner.semanticBacks=0;
   owner.activeMillis=0;owner.activeSince=SystemClock.elapsedRealtime();owner.startedAt=started;owner.currentObservation=null;owner.pendingDispatch=false;owner.recoveryHold="";
   owner.modelCalls=owner.screenshots=owner.actions=owner.launchRecoveries=0;owner.outcomeDetail="";owner.trace=new StringBuilder("语音新目标已准备\n");
   owner.lastDispatchObservation="";owner.lastEffect="none";owner.lastProposal=new JSONObject();owner.lastBoundary=new JSONObject();owner.lastFailure=new JSONObject();owner.lastKeyboardRecovery=new JSONObject();
   owner.awaitingDecision=false;owner.decisionInvalidated=false;owner.document=new DocumentRuntime(owner);owner.purchase=new PurchaseRuntime(owner);
   owner.voiceWorkerScheduler.schedule(stage,first.pkg,first.goal,owner.generation,gate);
   writeReadback(TaskCheckpoint.KEY,after);
   writeReadback(JOURNAL,journal.put("phase","committed").toString());
   clearJournal();
   // All operations below are local resets. The scheduled worker is still behind the gate.
   owner.deviceMcp.reset();owner.rejectedActions.clear();owner.visualScenes.clear();owner.workingMemory.clear();owner.touchPoints.reset();owner.nodes.clear();first.state="running";
   current=null;committed=true;owner.log("已采用新语音目标 · 目标 1/"+plan.steps.size()+" · "+first.app);owner.showOverlay();gate.complete(true);return true;
  }catch(Exception failure){
   gate.complete(false);
   if(!committed){
    if(staged){if(owner.ownership.state()==RunOwnership.State.RUNNING)owner.generation=owner.ownership.pause();before.restore(owner);}
    current=null;
    // A failed rollback retains the prepared journal so startup can restore the predecessor.
    boolean restored=false;
    try{writeReadback(JOURNAL,prepared);writeReadback(TaskCheckpoint.KEY,hold.checkpoint);clearJournal();restored=true;}catch(Exception unavailable){/* Fail closed, with encrypted recovery evidence retained. */}
    owner.runtimeHealth.phase("paused");owner.showOverlay();
    if(!restored)throw new Exception("新目标未开始，原目标仍暂停；交接存储恢复尚待核验");
   }
   throw failure;
  }
 }
 /** A prepared, never-dispatched transfer is rolled back before ordinary checkpoint restore. */
 static void recover(Context context)throws Exception{
  Vault vault=new Vault(context);String raw=vault.get(JOURNAL);if(raw.isEmpty())return;
  JSONObject journal=new JSONObject(raw);if(journal.getInt("version")!=1)throw new Exception("语音交接记录版本无效");
  String phase=journal.getString("phase"),before=journal.getString("before"),after=journal.getString("after");
  if(!phase.equals("prepared")&&!phase.equals("committed"))throw new Exception("语音交接阶段无效");
  // No worker is released until the journal has been cleared and read back, so either
  // surviving journal phase means an interrupted transfer, not a completed new goal.
  if(before.length()>131072||after.length()>131072||new JSONObject(before).getInt("version")!=1||new JSONObject(after).getInt("version")!=1)throw new Exception("语音交接内容无效");
  vault.put(TaskCheckpoint.KEY,before);if(!before.equals(vault.get(TaskCheckpoint.KEY)))throw new Exception("原目标恢复读回失败");
  vault.remove(JOURNAL);if(!vault.get(JOURNAL).isEmpty())throw new Exception("语音交接恢复记录未清理");
 }
 /** Exact mutable task references for rollback; ownership epochs are deliberately never restored. */
 static final class RuntimeSnapshot {
  final String allowed,task,run,session,status,detail,recovery,lastObservation,lastEffect;
  final boolean running,paused,checkout,pending,awaiting,invalidated;final int step,backs,calls,shots,actions,recoveries;
  final long started,activeSince,activeMillis;final Config config;final TaskWorkflow workflow;final TaskRequest request;final ControlService.Observation observation;
  final DocumentRuntime document;final PurchaseRuntime purchase;final StringBuilder trace;final JSONObject proposal,boundary,failure,keyboard;
  RuntimeSnapshot(ControlService s){allowed=s.allowed;task=s.task;run=s.runId;session=s.ownerSession;status=s.status;detail=s.outcomeDetail;recovery=s.recoveryHold;lastObservation=s.lastDispatchObservation;lastEffect=s.lastEffect;
   running=s.running;paused=s.paused;checkout=s.checkout;pending=s.pendingDispatch;awaiting=s.awaitingDecision;invalidated=s.decisionInvalidated;step=s.nextStep;backs=s.semanticBacks;calls=s.modelCalls;shots=s.screenshots;actions=s.actions;recoveries=s.launchRecoveries;
   started=s.startedAt;activeSince=s.activeSince;activeMillis=s.activeMillis;config=s.runConfig;workflow=s.workflow;request=s.activeRequest;observation=s.currentObservation;document=s.document;purchase=s.purchase;trace=s.trace;proposal=s.lastProposal;boundary=s.lastBoundary;failure=s.lastFailure;keyboard=s.lastKeyboardRecovery;}
  void restore(ControlService s){s.allowed=allowed;s.task=task;s.runId=run;s.ownerSession=session;s.status=status;s.outcomeDetail=detail;s.recoveryHold=recovery;s.lastDispatchObservation=lastObservation;s.lastEffect=lastEffect;
   s.running=running;s.paused=paused;s.checkout=checkout;s.pendingDispatch=pending;s.awaitingDecision=awaiting;s.decisionInvalidated=invalidated;s.nextStep=step;s.semanticBacks=backs;s.modelCalls=calls;s.screenshots=shots;s.actions=actions;s.launchRecoveries=recoveries;
   s.startedAt=started;s.activeSince=activeSince;s.activeMillis=activeMillis;s.runConfig=config;s.workflow=workflow;s.activeRequest=request;s.currentObservation=observation;s.document=document;s.purchase=purchase;s.trace=trace;s.lastProposal=proposal;s.lastBoundary=boundary;s.lastFailure=failure;s.lastKeyboardRecovery=keyboard;}
 }
}
