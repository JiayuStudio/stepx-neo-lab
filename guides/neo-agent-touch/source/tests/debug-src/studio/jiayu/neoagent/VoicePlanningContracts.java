package studio.jiayu.neoagent;
import android.content.pm.ApplicationInfo;import android.os.*;import org.json.*;import java.io.*;import java.nio.file.Files;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;

/** Debug-only MainActivity planning callbacks. Local planner seam and synthetic owned lease; no provider or app actions. */
final class VoicePlanningContracts {
 final UiQaActivity a;final ControlService s;final ArrayList<Gate> gates=new ArrayList<>();final AtomicInteger starts=new AtomicInteger();int checks;
 MainActivity.PlanningSource oldSource;MainActivity.TaskStarter oldStarter;String grant,outcome,checkpoint;byte[] runReport;boolean captured;String ownedRun="";
 boolean hadDirect,oldDirect;OfflineVoice previousVoice,fixtureVoice;String oldRun,oldOwner,oldAllowed,oldTask,oldStatus,oldDetail;Config oldRunConfig;TaskWorkflow oldWorkflow;TaskRequest oldRequest;long oldActiveMillis,oldActiveSince;
 VoicePlanningContracts(UiQaActivity activity){a=activity;s=ControlService.instance;}
 static void run(UiQaActivity a){new Thread(()->new VoicePlanningContracts(a).run(),"voice-planning-contracts").start();}
 static final class Gate implements MainActivity.PlanningSource {
  final boolean fail;final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  Gate(boolean failure){fail=failure;}
  public JSONObject plan(ModelClient ignored,Config config,String context)throws Exception{entered.countDown();if(!release.await(8,TimeUnit.SECONDS))throw new Exception("Local planner gate timeout");if(fail)throw new Exception("Local planned failure");return task();}
 }
 static JSONObject task()throws Exception{return new JSONObject().put("kind","task").put("app","Neo Agent Touch 本地演示").put("goal","在本地演示中搜索测试，看到结果后停止").put("target","测试").put("checkout",false);}
 <T>T ui(Callable<T> call)throws Exception{CompletableFuture<T> value=new CompletableFuture<>();a.runOnUiThread(()->{try{value.complete(call.call());}catch(Throwable e){value.completeExceptionally(e);}});return value.get(5,TimeUnit.SECONDS);}
 void yes(boolean value,String why)throws Exception{if(!value)throw new Exception(why);checks++;}
 Gate gate(boolean fail)throws Exception{Gate gate=new Gate(fail);gates.add(gate);ui(()->{a.planningSource=gate;return true;});return gate;}
 String voice(Gate gate)throws Exception{String request=ui(()->{String token=a.beginVoiceRequest();a.voiceOwnerSession=a.sessionId;a.voiceResult("打开Neo Agent Touch 本地演示搜索测试",true);return token;});yes(gate.entered.await(3,TimeUnit.SECONDS),"Voice final did not enter MainActivity planner");yes(ui(()->a.planning&&a.voiceTaskTurn==a.turn&&request.equals(a.voiceTaskRequest)),"Accepted final not linked to its planning turn");return request;}
 Thread worker()throws Exception{return ui(()->a.planningWorker);}
 void drain(Gate gate,Thread worker)throws Exception{gate.release.countDown();worker.join(4000);yes(!worker.isAlive(),"Local planner worker did not return");ui(()->true);}
 void cancel(String request)throws Exception{ui(()->{a.new Bridge().action("voiceCancel",new JSONObject().put("request",request).toString());return true;});}
 void clearPending()throws Exception{ui(()->{a.pendingToken="";a.pendingWorkflow=null;a.pendingConfig=null;a.clearVoiceTask();a.event("clearTask",true);return true;});}
 void syntheticLease(MainActivity activity,ControlService service){
  if(service.busy())throw new IllegalStateException("Runtime unexpectedly occupied");ownedRun=UUID.randomUUID().toString();service.runId=ownedRun;service.ownerSession=activity.sessionId;service.allowed=activity.getPackageName();service.task="本地语音取消所有权检查";service.runConfig=activity.pendingConfig;service.workflow=activity.pendingWorkflow;service.activeRequest=new TaskRequest(service.allowed,"本地合成检查","",false);service.running=true;service.paused=false;service.activeMillis=0;service.activeSince=SystemClock.elapsedRealtime();service.generation=service.ownership.start();
 }
 void stopOwnLease()throws Exception{ui(()->{synchronized(s){if(!ownedRun.isEmpty()&&ownedRun.equals(s.runId)&&s.busy())s.stop("语音规划本地检查结束");}return true;});}
 void run(){JSONObject report=new JSONObject();try{
  yes((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"Debug fixture only");
  yes(s!=null&&s.connected&&!s.busy(),"Runtime not idle");yes(ui(()->a.resumed&&a.loaded&&"plan".equals(a.voiceMode())&&!a.voiceUiTest()),"Fresh isolated planning UI required");
  yes(new AppAccess(a).configured(),"App selection must already be configured");
  Vault vault=new Vault(a);grant=vault.get(RoutineAccess.KEY);outcome=vault.get("last-outcome");checkpoint=vault.get(TaskCheckpoint.KEY);File file=new File(a.getFilesDir(),"last-run.json");runReport=file.isFile()?Files.readAllBytes(file.toPath()):null;
  hadDirect=a.getSharedPreferences("ui",0).contains("voiceDirect");oldDirect=a.getSharedPreferences("ui",0).getBoolean("voiceDirect",false);
  ui(()->{previousVoice=a.voice;if(previousVoice!=null&&previousVoice.active())throw new Exception("Existing capture active");synchronized(s){oldRun=s.runId;oldOwner=s.ownerSession;oldAllowed=s.allowed;oldTask=s.task;oldStatus=s.status;oldDetail=s.outcomeDetail;oldRunConfig=s.runConfig;oldWorkflow=s.workflow;oldRequest=s.activeRequest;oldActiveMillis=s.activeMillis;oldActiveSince=s.activeSince;}return true;});captured=true;
  ui(()->{oldSource=a.planningSource;oldStarter=a.taskStarter;a.taskStarter=(activity,service)->{starts.incrementAndGet();return false;};a.newChat();return true;});
  // Leave automatic confirmation enabled: an unguarded old success would attempt a start.
  new RoutineAccess(a).approve(Config.load(a),Collections.singletonList(a.getPackageName()));
  Gate success=gate(false);String first=voice(success);Thread firstWorker=worker();long submitted=ui(()->a.turn);cancel(first);
  yes(ui(()->!a.planning&&a.turn==submitted+1&&a.pendingToken.isEmpty()),"Explicit final-to-plan cancellation failed");final int successMessages=ui(()->a.chat().getJSONArray("messages").length());drain(success,firstWorker);
  yes(ui(()->starts.get()==0&&a.pendingToken.isEmpty()&&!a.planning&&a.chat().getJSONArray("messages").length()==successMessages),"Late success presented or started a cancelled plan");
  Gate failure=gate(true);String failed=voice(failure);Thread failedWorker=worker();cancel(failed);int messages=ui(()->a.chat().getJSONArray("messages").length());drain(failure,failedWorker);final int failedMessages=messages;
  yes(ui(()->!a.planning&&starts.get()==0&&a.chat().getJSONArray("messages").length()==failedMessages),"Late failure polluted cancelled chat");
  // Pending consent must also be revoked, including an already captured confirm token.
  vault.remove(RoutineAccess.KEY);Gate pending=gate(false);String pendingRequest=voice(pending);Thread pendingWorker=worker();drain(pending,pendingWorker);String oldConfirm=ui(()->a.pendingToken);
  yes(ui(()->!oldConfirm.isEmpty()&&a.pendingWorkflow!=null&&a.pendingWorkflow.steps.size()==1),"Production success did not build and present workflow");cancel(pendingRequest);ui(()->{a.confirmTask(oldConfirm);return true;});
  yes(ui(()->a.pendingToken.isEmpty()&&a.pendingWorkflow==null&&starts.get()==0),"Cancelled pending voice remained confirmable");
  // Implicit microphone/mode changes preserve a business plan; a mismatched explicit token cannot cancel it.
  Gate implicit=gate(false);String implicitRequest=voice(implicit);Thread implicitWorker=worker();long implicitTurn=ui(()->a.turn);cancel(first);
  ui(()->{a.cancelVoice();a.new Bridge().action("voiceMode",new JSONObject().put("direct",false).toString());return true;});
  yes(ui(()->a.planning&&a.turn==implicitTurn&&implicitRequest.equals(a.voiceTaskRequest)),"Implicit audio or stale-token cancellation revoked business planning");cancel(implicitRequest);drain(implicit,implicitWorker);
  // A cancelled old voice response must not cancel or replace a later accepted manual goal.
  Gate old=gate(false);String oldRequest=voice(old);Thread oldWorker=worker();cancel(oldRequest);Gate manual=gate(false);
  ui(()->{a.sendMessage("打开Neo Agent Touch 本地演示搜索测试");return true;});yes(manual.entered.await(3,TimeUnit.SECONDS),"Manual successor did not enter planner");Thread manualWorker=worker();long manualTurn=ui(()->a.turn);cancel(oldRequest);drain(old,oldWorker);
  yes(ui(()->a.planning&&a.turn==manualTurn&&a.voiceTaskRequest.isEmpty()),"Old voice cancellation changed manual successor");drain(manual,manualWorker);yes(ui(()->!a.pendingToken.isEmpty()&&a.voiceTaskRequest.isEmpty()),"Manual successor failed production success path");clearPending();
  // The same stale token also must not cancel a later utterance, even with old failure in flight.
  Gate older=gate(true);String olderRequest=voice(older);Thread olderWorker=worker();cancel(olderRequest);Gate newer=gate(false);String newerRequest=voice(newer);Thread newerWorker=worker();long newerTurn=ui(()->a.turn);cancel(olderRequest);drain(older,olderWorker);
  yes(ui(()->a.planning&&a.turn==newerTurn&&newerRequest.equals(a.voiceTaskRequest)&&newerRequest.equals(a.voiceRequest)),"Old token changed new utterance ownership");drain(newer,newerWorker);cancel(newerRequest);
  // A fresh positive callback passes through real pending/confirm code into an owned synthetic lease.
  new RoutineAccess(a).approve(Config.load(a),Collections.singletonList(a.getPackageName()));
  ui(()->{a.taskStarter=(activity,service)->{starts.incrementAndGet();syntheticLease(activity,service);return true;};return true;});
  Gate launched=gate(false);String launchedRequest=voice(launched);Thread launchedWorker=worker();drain(launched,launchedWorker);long lease=ui(()->s.generation);
  yes(ui(()->s.busy()&&a.pendingToken.isEmpty()&&s.runId.equals(a.voiceTaskRun)&&starts.get()==1),"Started voice run not linked to originating turn");cancel(launchedRequest);
  yes(ui(()->!s.busy()&&!s.running&&!s.paused&&!s.ownership.accepts(lease)),"Explicit cancellation did not end just-started owned lease");
  // Same session, different native run: cancellation may revoke only the exact original run id.
  Gate replaced=gate(false);String replacedRequest=voice(replaced);Thread replacedWorker=worker();drain(replaced,replacedWorker);String replacedRun=ownedRun;stopOwnLease();
  ui(()->{synchronized(s){syntheticLease(a,s);}return true;});String laterRun=ownedRun;cancel(replacedRequest);
  yes(ui(()->s.busy()&&laterRun.equals(s.runId)&&!replacedRun.equals(s.runId)),"Stale voice cancellation stopped a different native run");stopOwnLease();
  // Queued old DOM controls cannot stop or cancel a newer AudioSession. No microphone is opened.
  final String[] captureRequest={""};final long[] captureLease={0};
  ui(()->{fixtureVoice=new OfflineVoice(a,new OfflineVoice.Listener(){public boolean foreground(){return a.resumed;}public void state(JSONObject value){}public void result(String text,boolean direct){}public void error(String message){}});a.voice=fixtureVoice;captureRequest[0]=a.beginVoiceRequest();a.voiceOwnerSession=a.sessionId;captureLease[0]=a.voice.session.begin(true);a.voice.token=captureLease[0];a.voice.session.listening(captureLease[0]);return true;});
  ui(()->{a.new Bridge().action("voiceStop",new JSONObject().put("request",first).toString());return true;});
  yes(ui(()->a.voice.session.state()==VoiceSession.State.LISTENING&&a.voice.session.current(captureLease[0])),"Old stop ended newer capture");cancel(first);
  yes(ui(()->a.voice.session.state()==VoiceSession.State.LISTENING&&captureRequest[0].equals(a.voiceRequest)),"Old cancel ended newer capture");
  ui(()->{a.new Bridge().action("voiceStop",new JSONObject().put("request",captureRequest[0]).toString());return true;});
  yes(ui(()->a.voice.session.state()==VoiceSession.State.FINISHING),"Current stop did not finish current capture");cancel(captureRequest[0]);
  yes(ui(()->!a.voice.active()),"Current cancellation did not revoke capture");
  report.put("success",true).put("checks",checks).put("late_success_rejected",true).put("late_failure_rejected",true).put("pending_confirmation_revoked",true).put("manual_successor_preserved",true).put("voice_successor_preserved",true).put("implicit_audio_cancel_preserves_plan",true).put("just_started_owned_lease_cancelled",true).put("different_run_preserved",true).put("stale_capture_stop_rejected",true).put("stale_capture_cancel_rejected",true);
 }catch(Throwable error){try{report.put("success",false).put("checks",checks).put("failure",error.getMessage()).put("failure_type",error.getClass().getSimpleName());}catch(Exception ignored){}}
 finally{
  for(Gate gate:gates)gate.release.countDown();try{report.put("cleanup_verified",!captured);if(captured){stopOwnLease();boolean idle=ui(()->{++a.turn;a.planning=false;a.pendingToken="";a.planner.cancel();a.clearVoiceTask();a.cancelVoice();if(fixtureVoice!=null)fixtureVoice.close();a.voice=previousVoice;if(oldSource!=null)a.planningSource=oldSource;if(oldStarter!=null)a.taskStarter=oldStarter;synchronized(s){if(s.busy())return false;s.runId=oldRun;s.ownerSession=oldOwner;s.allowed=oldAllowed;s.task=oldTask;s.status=oldStatus;s.outcomeDetail=oldDetail;s.runConfig=oldRunConfig;s.workflow=oldWorkflow;s.activeRequest=oldRequest;s.activeMillis=oldActiveMillis;s.activeSince=oldActiveSince;}return true;});Vault vault=new Vault(a);if(idle){restore(vault,RoutineAccess.KEY,grant);restore(vault,"last-outcome",outcome);restore(vault,TaskCheckpoint.KEY,checkpoint);File file=new File(a.getFilesDir(),"last-run.json");if(runReport==null)Files.deleteIfExists(file.toPath());else Files.write(file.toPath(),runReport);}android.content.SharedPreferences.Editor editor=a.getSharedPreferences("ui",0).edit();if(hadDirect)editor.putBoolean("voiceDirect",oldDirect);else editor.remove("voiceDirect");if(!editor.commit())throw new IOException("Preference restore failed");vault.remove("test-voice-user-memory");vault.remove("test-voice-chats");boolean restored=idle&&grant.equals(vault.get(RoutineAccess.KEY))&&outcome.equals(vault.get("last-outcome"))&&checkpoint.equals(vault.get(TaskCheckpoint.KEY))&&a.getSharedPreferences("ui",0).contains("voiceDirect")==hadDirect&&a.getSharedPreferences("ui",0).getBoolean("voiceDirect",false)==oldDirect&&ui(()->!s.busy()&&a.planningSource==oldSource&&a.taskStarter==oldStarter&&a.voice==previousVoice);File restoredFile=new File(a.getFilesDir(),"last-run.json");restored&=runReport==null?!restoredFile.exists():restoredFile.isFile()&&Arrays.equals(runReport,Files.readAllBytes(restoredFile.toPath()));report.put("original_local_state_restored",restored).put("cleanup_verified",restored);if(!restored)report.put("success",false);}}catch(Exception cleanup){try{report.put("success",false).put("original_local_state_restored",false).put("cleanup_verified",false).put("cleanup_failure_type",cleanup.getClass().getSimpleName());}catch(Exception ignored){}}
  try{report.put("real_network_calls",0).put("third_party_actions",0).put("microphone_used",false).put("fixture_kind","blocked local planning seam; native MainActivity callbacks and synthetic runtime ownership, not a real API or live speech test").put("sampledAt",System.currentTimeMillis());try(FileOutputStream out=a.openFileOutput("voice-planning-contracts.json",0)){out.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.runOnUiThread(a::finish);
 }
 }
 static void restore(Vault vault,String key,String value)throws Exception{if(value==null||value.isEmpty())vault.remove(key);else vault.put(key,value);}
}
