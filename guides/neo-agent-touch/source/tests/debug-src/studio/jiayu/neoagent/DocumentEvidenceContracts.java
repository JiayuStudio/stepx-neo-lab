package studio.jiayu.neoagent;
import android.app.Activity;import android.os.*;import android.view.inputmethod.SurroundingText;import org.json.*;import java.io.*;
/** Debug-only: native proof transitions on synthetic bodies, zero model or third-party mutations. */
final class DocumentEvidenceContracts {
 static int count;static void check(boolean ok,String label)throws Exception{if(!ok)throw new Exception(label);count++;}
 static final String BODY="租赁合同\n甲方：【出租方】\n乙方：【承租方】\n租金每月5日前支付；押金【金额】\n签署：【日期】";
 static SurroundingText read(String body,int offset){return new SurroundingText(body,body.length(),body.length(),offset);}
 static DocumentRuntime completedDoc(ControlService owner){DocumentRuntime d=new DocumentRuntime(owner);d.created=d.permitted=d.attempted=true;d.name="测试租赁合同_证据夹具";d.lastWrite=BODY;d.writes=2;d.reopen.wrote(2);d.reopen.listObserved();d.reopen.openRequested();d.pendingReopen=true;d.acceptReadback(read(BODY,0));d.saved=true;return d;}
 static void start(Activity activity){new Thread(()->{JSONObject result=new JSONObject();ControlService owner=ControlService.instance;String task=owner==null?null:owner.task,allowed=owner==null?null:owner.allowed;TaskRequest request=owner==null?null:owner.activeRequest;count=0;try{
  if(owner==null||owner.busy())throw new Exception("Need idle connected service");
  owner.task="新建并修改测试租赁合同，修改为每月5日前支付";owner.allowed="cn.wps.moffice_eng";owner.activeRequest=new TaskRequest(owner.allowed,owner.task,"",false);
  DocumentRuntime d=completedDoc(owner);check(d.completed(),"baseline full proof");check(d.opens==1&&d.reopen.read==2,"reopen bound to current revision");
  d.acceptReadback(read(BODY.replace("5日前","10日前"),0));check(!d.completed(),"changed body must invalidate finish");check(!d.bodyVerified&&!d.saved&&!d.reopened&&!d.editVerified&&!d.persistedEdit,"dependent proof cleared");check(d.evidence.isEmpty()&&d.reopen.read==0,"stale read text/revision cleared");check(d.proof().getString("readback_state").equals("mismatch"),"mismatch exposed without document text");
  d.acceptReadback(read(BODY,0));check(d.bodyVerified&&!d.completed(),"same editor body alone cannot re-prove saved file");check(!d.reopen.currentRead(),"fresh file reopen still required");
  d.reopen.listObserved();check(d.reopen.openRequested(),"new reopen ticket");d.pendingReopen=true;d.acceptReadback(read(BODY,0));d.saved=true;check(d.completed(),"explicit re-open exact body recovers");check(d.opens==2,"reopen counted once per verified ticket");d.acceptReadback(read(BODY,0));check(d.opens==2,"repeated reads do not count as reopen");
  d.acceptReadback(null);check(!d.completed()&&!d.bodyVerified,"null read cannot finish");check(d.reopen.currentRead(),"temporary absence preserves historical reopen without authorizing finish");d.acceptReadback(read(BODY,0));check(d.completed(),"fresh matching read recovers temporary absence");
  d.acceptReadback(read(BODY,3));check(!d.completed()&&d.readbackState.equals("partial"),"nonzero offset not full body");d.acceptReadback(read(BODY,-1));check(!d.completed()&&d.readbackState.equals("unavailable"),"unknown offset not full body");d.acceptReadback(read(BODY,0));check(d.completed(),"zero offset restores full read");
  d.acceptReadback(read(BODY+"追加条款",0));check(!d.completed(),"suffix contradiction invalidates completion");
  d=completedDoc(owner);d.acceptReadback(read(BODY.replace("签署", "其他"),0));check(!d.completed(),"missing required contract section rejected");
  d=completedDoc(owner);check(!d.verifyCompletion(),"finish cannot reuse historical success when native editor is unavailable");check(!d.completed()&&d.readbackState.equals("unavailable"),"fresh finish check invalidated cached body");
  d=completedDoc(owner);owner.activeRequest=new TaskRequest(owner.allowed,"只把租金修改为每月10日，其他段落保持不变","",false);d.validateReplacement(BODY.replace("5日前","10日前"));check(true,"actual runtime replacement validator accepts exact requested change");boolean rejected=false;try{d.validateReplacement(BODY.replace("5日前","10日前").replace("押金", "保证金"));}catch(ControlService.LocalBoundaryException expected){rejected=true;}check(rejected,"runtime validator rejects unrequested same-paragraph edit");owner.activeRequest=new TaskRequest(owner.allowed,owner.task,"",false);
  d=completedDoc(owner);d.reopen.wrote(3);d.writes=3;check(!d.completed(),"new revision requires own reopen");
  owner.task="在WPS新建一个名为“Neo适配检查”的文档，自己写三段介绍手机内Agent工作原理的文字并保存。";owner.activeRequest=new TaskRequest(owner.allowed,owner.task,"",false);
  String article="手机内Agent先获取授权应用的画面与系统控件。\n模型根据用户目标选择下一步动作。\n手机执行动作并重新观察，核对目标是否达成。";
  d=new DocumentRuntime(owner);d.created=d.permitted=d.attempted=true;d.lastWrite=article;d.writes=1;d.reopen.wrote(1);d.reopen.listObserved();check(d.reopen.openRequested(),"general document reopen ticket");d.pendingReopen=true;d.acceptReadback(read(article,0));d.saved=true;d.name=d.expectedName();
  check(d.completed(),"non-contract exact readback can complete only with full own-file proof");check(d.expectedName().equals("Neo适配检查"),"runtime derives name from original request");
  d.acceptReadback(read(article+"额外段落",0));check(!d.completed()&&!d.bodyVerified&&d.readbackState.equals("mismatch"),"general document mismatch cannot pass");
  d=completedDoc(owner);d.name="Neo适配检查";d.candidateName=d.name;d.saveAt=123;d.recoveryNeedsFile=true;d.noteNameConflict("文件已存在，是否替换");
  check(d.name.isEmpty()&&d.candidateName.isEmpty(),"collision loses attempted old-file identity");check(!d.saved&&!d.reopened&&!d.persistedEdit&&!d.pendingReopen&&!d.recoveryNeedsFile,"collision cannot reopen old file or retain saved proof");check(d.saveAt==0&&!d.reopen.currentRead(),"collision clears save and reopen revision");check(!DocumentNameRules.matches("Neo适配检查",d.expectedName()),"collision requires a new task suffix");check(d.expectedName().startsWith("Neo适配检查_"),"new name retains requested base");String collisionName=d.expectedName();d.noteNameConflict("文件已存在，是否替换");check(d.expectedName().equals(collisionName),"same visible conflict keeps name stable");d.name=collisionName;d.noteNameConflict("文件已存在，是否替换");check(!d.expectedName().equals(collisionName),"second actual save collision obtains a new nonce");
  String unique=d.expectedName();DocumentRuntime restored=new DocumentRuntime(owner);restored.restore(d.checkpoint());check(restored.expectedName().equals(unique),"safe generated name stable across checkpoint");check(!restored.permitted&&!restored.recoveryNeedsFile,"collision checkpoint cannot reopen old file");

  result.put("passed",true).put("checks",count).put("model_calls",0).put("third_party_actions",0).put("scope","real Android SurroundingText objects and DocumentRuntime transitions; not a WPS business run");
 }catch(Exception e){try{result.put("passed",false).put("checks",count).put("error",e.getClass().getSimpleName()+": "+e.getMessage());}catch(Exception ignored){}}
 finally{if(owner!=null){owner.task=task;owner.allowed=allowed;owner.activeRequest=request;}try(FileOutputStream out=activity.openFileOutput("document-evidence-contracts.json",0)){out.write(result.toString(2).getBytes("UTF-8"));}catch(Exception ignored){}activity.runOnUiThread(activity::finish);}
 }).start();}
}
