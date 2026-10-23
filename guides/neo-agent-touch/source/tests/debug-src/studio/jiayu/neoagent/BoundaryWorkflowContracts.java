package studio.jiayu.neoagent;
import android.app.Activity;import org.json.*;import java.io.*;
/** Pure native queue serialization/restore; no app launch, gesture or model request. */
final class BoundaryWorkflowContracts {
 static int checks;static void yes(boolean x,String s)throws Exception{if(!x)throw new Exception(s);checks++;}
 static void rejected(Activity a,JSONObject state,String label)throws Exception{boolean rejected=false;try{TaskWorkflow.restore(a,state);}catch(Exception expected){rejected=true;}yes(rejected,label);}
 static void run(Activity a){JSONObject report=new JSONObject();checks=0;try{
  String original="先在美团查看电影，尝试到订单核对页；如果必须锁座，停在选座页，不锁座、不确认选座。再在本地演示搜索下一项";
  JSONArray tasks=new JSONArray().put(new JSONObject().put("app","美团").put("goal","查看电影场次，若必须锁座则停在选座页")).put(new JSONObject().put("app","Neo Agent Touch 本地演示").put("goal","在本地演示中搜索下一项").put("target","下一项"));
  TaskWorkflow w=new TaskWorkflow(a,new JSONObject().put("tasks",tasks),original);yes(w.steps.get(0).checkout,"movie preview scope");
  TaskSpec first=w.steps.get(0);first.state="partial";first.verified=false;first.result="已到选座页，订单核对未完成";first.proof=new JSONObject().put("kind","allowed_movie_boundary").put("original_signature",PurchaseFacts.digest(first.original)).put("native_stop_verified",true).put("full_order_review_verified",false);w.index=1;
  JSONObject checkpoint=w.json();TaskWorkflow restored=TaskWorkflow.restore(a,checkpoint);yes(restored.index==1,"next stage retained");yes(restored.steps.get(0).state.equals("partial"),"partial state retained");yes(!restored.steps.get(0).verified,"not upgraded to full success");yes(restored.current().state.equals("paused"),"restore requires fresh continuation");yes(restored.results().contains("核对未完成"),"incomplete result visible");
  JSONObject bad=new JSONObject(checkpoint.toString());bad.getJSONArray("tasks").getJSONObject(0).put("proof",new JSONObject());rejected(a,bad,"missing native boundary proof accepted");
  bad=new JSONObject(checkpoint.toString());bad.getJSONArray("tasks").getJSONObject(0).getJSONObject("proof").put("full_order_review_verified",true);rejected(a,bad,"contradictory order success accepted");
  bad=new JSONObject(checkpoint.toString());bad.getJSONArray("tasks").getJSONObject(0).put("verified",true);rejected(a,bad,"partial upgraded to verified");
  bad=new JSONObject(checkpoint.toString());bad.put("original","在美团看电影到订单核对页，不能停在选座页；再打开本地演示");rejected(a,bad,"changed user scope reuses stop permission");
  bad=new JSONObject(checkpoint.toString());bad.put("original",original.replace("查看电影","查看明天的电影"));rejected(a,bad,"old stop proof reused for a different date");
  report.put("passed",true).put("checks",checks).put("model_calls",0).put("third_party_actions",0).put("checkpoint_restored_stage",1).put("partial_counts_as_order_review",false);
 }catch(Exception e){try{report.put("passed",false).put("checks",checks).put("error",e.getClass().getSimpleName()+": "+e.getMessage());}catch(Exception ignored){}}try(FileOutputStream f=a.openFileOutput("boundary-workflow-contracts.json",0)){f.write(report.toString(2).getBytes("UTF-8"));}catch(Exception ignored){}}
}
