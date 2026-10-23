package studio.jiayu.neoagent;
import org.json.*;
/** Encrypted local journal. Never stores credentials, screenshots, nodes or input connections. */
final class TaskCheckpoint {
 static final String KEY="task-checkpoint-v1";
 static void save(ControlService s)throws Exception{
  Vault v=new Vault(s);if(!s.busy()||s.runConfig==null){v.remove(KEY);return;}
  v.put(KEY,snapshot(s).toString());
 }
 static JSONObject snapshot(ControlService s)throws Exception{
  Config c=s.runConfig;
  JSONObject o=new JSONObject().put("version",1).put("at",System.currentTimeMillis()).put("app",s.allowed).put("goal",s.task).put("run",s.runId).put("session",s.ownerSession)
   .put("config",c.json()).put("checkout",s.checkout).put("original",s.activeRequest==null?s.task:s.activeRequest.original).put("target",s.activeRequest==null?"":s.activeRequest.target)
   .put("step",s.nextStep).put("active",s.activeElapsed()).put("started",s.startedAt).put("calls",s.modelCalls).put("shots",s.screenshots).put("actions",s.actions)
   .put("working_memory",new JSONObject(s.workingMemory.snapshot())).put("trace",s.trace.toString()).put("pending",s.pendingDispatch).put("document",s.document.checkpoint()).put("workflow",s.workflow==null?JSONObject.NULL:s.workflow.json());
  JSONArray touches=new JSONArray();for(long point:s.touchPoints.snapshot())touches.put(point);o.put("used_touches",touches);
  return o;
 }
 static boolean restore(ControlService s)throws Exception{
  if(s.busy())return false;VoiceGoalTransfer.recover(s);Vault v=new Vault(s);String raw=v.get(KEY);if(raw.isEmpty())return false;JSONObject o=new JSONObject(raw);
  long age=System.currentTimeMillis()-o.getLong("at");if(o.getInt("version")!=1||age<0||age>86400000L){v.remove(KEY);return false;}
  Config c=Config.load(s);JSONObject stored=o.getJSONObject("config");
  if(!c.endpoint.equals(stored.getString("endpoint"))||!c.model.equals(stored.getString("model"))||!c.protocol.equals(stored.getString("protocol"))||!c.vision||!stored.getBoolean("vision"))throw new Exception("模型配置已变化，未恢复旧任务");
  c.validate();String pkg=o.getString("app");s.requireDataGrant(c,pkg);c.maxSteps=GameplayIntent.steps(o.getString("original"),stored.getInt("maxSteps"));c.checkout=o.getBoolean("checkout");
  TaskWorkflow workflow=o.optJSONObject("workflow")==null?null:TaskWorkflow.restore(s,o.getJSONObject("workflow"));if(workflow!=null){for(String app:workflow.packages())s.requireDataGrant(c,app);if(!workflow.current().pkg.equals(pkg)||!workflow.current().goal.equals(o.getString("goal"))||!workflow.current().original.equals(o.getString("original"))||workflow.current().checkout!=c.checkout)throw new Exception("任务队列进度不一致");}s.workflow=workflow;
  s.allowed=pkg;s.task=o.getString("goal");s.runId=o.getString("run");s.ownerSession=o.optString("session");s.activeRequest=new TaskRequest(pkg,o.getString("original"),o.optString("target"),c.checkout);s.checkout=s.activeRequest.checkout;
  s.nextStep=Math.max(1,o.getInt("step"));s.activeMillis=Math.max(0,o.getLong("active"));s.startedAt=o.getLong("started");s.modelCalls=o.getInt("calls");s.screenshots=o.getInt("shots");s.actions=o.getInt("actions");s.trace=new StringBuilder(o.optString("trace"));
  JSONObject memo=o.optJSONObject("working_memory");s.workingMemory.clear();if(memo!=null)try{s.workingMemory.restore(memo.optString("summary"),memo.optString("source_observation"),memo.optString("proposed_action"),memo.optBoolean("action_dispatched"));}catch(IllegalArgumentException invalid){s.workingMemory.clear();}s.visualScenes.clear();s.document.restore(o.getJSONObject("document"));JSONArray touches=o.optJSONArray("used_touches");if(touches!=null){if(touches.length()>4096)throw new Exception("Invalid touch history");long[] points=new long[touches.length()];for(int i=0;i<points.length;i++)points[i]=touches.getLong(i);s.touchPoints.restore(points);}s.pendingDispatch=o.optBoolean("pending");
  s.recoveryHold=s.pendingDispatch&&!s.purchase.pendingForRun()&&!(DocumentScope.applies(pkg)&&s.document.created&&!s.document.name.isEmpty())&&!GameplayIntent.play(s.activeRequest.original)?"上次动作回执不完整，不能自动重试；请核对应用后结束本轮，重新发起目标":DocumentScope.applies(pkg)&&s.document.created&&s.document.name.isEmpty()?"未命名文档的保存状态不明；请先手动保存核对，再结束本轮重新发起":"";
  s.runConfig=c;s.reapplyOriginalGameBudget();s.currentObservation=null;s.nodes.clear();s.running=false;s.paused=true;s.ownership.start();s.generation=s.ownership.pause();s.runtimeHealth.phase("paused");
  s.log("已恢复原目标 · 点击交还 Agent 后重新观察"+(s.recoveryHold.isEmpty()?"":" · "+s.recoveryHold));s.showOverlay();return true;
 }
}
