package studio.jiayu.neoagent;
import android.app.Activity;import org.json.*;import java.io.*;
/** Dedicated emulator only. Synthetic goals; zero provider calls or third-party actions. */
final class CheckpointContracts {
 static void seed(Activity a,ControlService s)throws Exception{
  if(!android.os.Build.HARDWARE.equals("ranchu")&&!android.os.Build.HARDWARE.equals("goldfish"))throw new Exception("Emulator fixture only");
  if(s==null||s.busy())throw new Exception("Runtime not idle");Config c=new Config();c.endpoint="https://example.invalid";c.key="LOCAL_CHECKPOINT_FIXTURE";c.model="fixture";c.save(a);new RoutineAccess(a).approve(c,java.util.Collections.singletonList(a.getPackageName()));
  s.allowed=a.getPackageName();s.task="恢复合成目标，只观察，不重复旧动作";s.ownerSession="fixture-session";s.runId="fixture-run";s.activeRequest=new TaskRequest(s.allowed,s.task,"",false);s.runConfig=c;s.startedAt=System.currentTimeMillis();s.activeMillis=1000;s.running=false;s.paused=true;s.generation=s.ownership.start();s.generation=s.ownership.pause();s.nextStep=3;s.pendingDispatch=false;s.log("合成任务已暂停");
 }
 static void contracts(Activity a,ControlService s)throws Exception{
  int passed=0;JSONObject result=new JSONObject();Vault v=new Vault(a);
  try{
   seed(a,s);String raw=v.get(TaskCheckpoint.KEY);JSONObject o=new JSONObject(raw);
   yes(!raw.contains("LOCAL_CHECKPOINT_FIXTURE")&&!raw.contains("\"key\""));passed++;
   String disk=a.getSharedPreferences("vault",0).getString(TaskCheckpoint.KEY,"");yes(!disk.contains(s.task)&&!disk.contains("fixture-session"));passed++;
   long old=s.generation;s.stop("fixture stop");yes(!v.has(TaskCheckpoint.KEY)&&!s.ownership.accepts(old));passed++;
   v.put(TaskCheckpoint.KEY,raw);yes(TaskCheckpoint.restore(s)&&s.paused&&!s.running&&s.currentObservation==null&&s.nodes.isEmpty());passed++;
   yes(s.nextStep==3&&s.runId.equals("fixture-run")&&s.ownerSession.equals("fixture-session"));passed++;
   s.stop("fixture reset");JSONObject changed=new JSONObject(raw);changed.getJSONObject("config").put("endpoint","https://different.invalid");v.put(TaskCheckpoint.KEY,changed.toString());boolean rejected=false;try{TaskCheckpoint.restore(s);}catch(Exception expected){rejected=true;}yes(rejected&&!s.busy());passed++;
   changed=new JSONObject(raw).put("pending",true);v.put(TaskCheckpoint.KEY,changed.toString());yes(TaskCheckpoint.restore(s)&&!s.recoveryHold.isEmpty()&&!s.resumeFromUser());passed++;
   s.stop("fixture reset");changed=new JSONObject(raw).put("at",System.currentTimeMillis()-86400001L);v.put(TaskCheckpoint.KEY,changed.toString());yes(!TaskCheckpoint.restore(s)&&!v.has(TaskCheckpoint.KEY));passed++;
   String play="在本地演示找到并短时试玩羊了个羊小游戏，进入棋盘操作几次即可，不需要通关。不要把入口或教程页面说成游戏通关。";
   changed=new JSONObject(raw).put("original",play).put("goal","本地游玩预算配置回归").put("step",25).put("active",12345).put("calls",24).put("actions",11);changed.getJSONObject("config").put("maxSteps",24);v.put(TaskCheckpoint.KEY,changed.toString());
   yes(TaskCheckpoint.restore(s)&&s.paused&&!s.running&&s.runConfig.maxSteps==200&&s.nextStep==25&&s.modelCalls==24&&s.actions==11&&s.activeMillis==12345&&s.executionLimit()==1800000L);passed++;
   s.runConfig.maxSteps=24;s.reapplyOriginalGameBudget();yes(s.runConfig.maxSteps==200&&s.nextStep==25&&s.activeMillis==12345&&s.modelCalls==24&&s.actions==11);passed++;
   s.stop("fixture game budget reset");changed.put("active",1800000L);v.put(TaskCheckpoint.KEY,changed.toString());yes(TaskCheckpoint.restore(s)&&s.runConfig.maxSteps==200&&!s.resumeFromUser()&&s.activeMillis==1800000L);passed++;
   s.stop("fixture tutorial budget reset");changed.put("original","搜索羊了个羊小游戏通关攻略").put("goal","搜索教程，不游玩").put("active",12345);v.put(TaskCheckpoint.KEY,changed.toString());yes(TaskCheckpoint.restore(s)&&s.runConfig.maxSteps==24&&s.executionLimit()==600000L&&s.nextStep==25&&!s.resumeFromUser());passed++;
   s.reapplyOriginalGameBudget();yes(s.runConfig.maxSteps==24&&s.nextStep==25&&s.activeMillis==12345);passed++;s.stop("fixture budget checks finished");
   DocumentRuntime d=s.document;d.created=d.permitted=d.attempted=d.bodyVerified=d.saved=d.reopened=d.persistedEdit=d.editVerified=true;d.name="测试租赁合同_恢复合成";d.lastWrite="合成正文";d.writes=2;d.opens=2;d.reopen.wrote(2);d.reopen.listObserved();d.reopen.openRequested();d.reopen.bodyRead();JSONObject doc=d.checkpoint();d.restore(doc);
   yes(d.created&&d.attempted&&d.name.equals("测试租赁合同_恢复合成")&&d.lastWrite.equals("合成正文")&&d.writes==2);passed++;
   yes(!d.permitted&&!d.bodyVerified&&!d.saved&&!d.reopened&&!d.persistedEdit&&!d.editVerified);passed++;
   yes(d.recoveryNeedsFile&&!d.reopen.currentRead()&&!d.reopen.listed&&!d.reopen.pending&&d.tapAt==0);passed++;
   s.touchPoints.reset();int[] point=s.touchPoints.pick(0,0,5,5,2,2,2);long[] used=s.touchPoints.snapshot();s.touchPoints.reset();s.touchPoints.restore(used);boolean unique=true;for(int i=0;i<22;i++){int[] next=s.touchPoints.pick(0,0,5,5,2,2,2);if(next[0]==point[0]&&next[1]==point[1])unique=false;}yes(unique);passed++;
   s.stop("fixture finished");yes(!TaskCheckpoint.restore(s));passed++;
   result.put("success",true);
  }catch(Throwable failure){result.put("success",false).put("failure",failure.getMessage());}
  finally{s.stop("检查点合成回归结束");s.document.reset();result.put("passed",passed).put("real_network_calls",0).put("third_party_actions",0).put("sampledAt",System.currentTimeMillis());try(FileOutputStream out=a.openFileOutput("checkpoint-contracts.json",0)){out.write(result.toString(2).getBytes("UTF-8"));}}
 }
 static void yes(boolean ok)throws Exception{if(!ok)throw new Exception("Checkpoint invariant failed");}
 static void report(Activity a,ControlService s)throws Exception{
  JSONObject r=new JSONObject().put("paused",s!=null&&s.paused).put("running",s!=null&&s.running).put("same_goal",s!=null&&s.task.equals("恢复合成目标，只观察，不重复旧动作")).put("same_session",s!=null&&s.ownerSession.equals("fixture-session")).put("same_run",s!=null&&s.runId.equals("fixture-run")).put("nextStep",s==null?0:s.nextStep).put("no_old_observation",s!=null&&s.currentObservation==null&&s.nodes.isEmpty()).put("model_calls",s==null?-1:s.modelCalls).put("actions",s==null?-1:s.actions).put("real_network_calls",0);
  String stored=new Vault(a).get(TaskCheckpoint.KEY);r.put("no_credential_fields",!stored.contains("LOCAL_CHECKPOINT_FIXTURE")&&!stored.contains("\"key\""));
  try(FileOutputStream out=a.openFileOutput("checkpoint-process.json",0)){out.write(r.toString(2).getBytes("UTF-8"));}
 }
}
