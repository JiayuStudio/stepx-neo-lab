package studio.jiayu.neoagent;
import android.app.Activity;import org.json.*;import java.util.*;import java.io.*;
/** Installed-app routing and per-app original goals; no model calls or business actions. */
final class AdaptiveRoutingContracts {
 static int n;static void yes(boolean b,String name)throws Exception{if(!b)throw new Exception(name);n++;}
 static JSONObject step(String pkg,String goal)throws Exception{return new JSONObject().put("app",pkg).put("goal",goal);}
 static void rejectPlan(Activity a,JSONArray steps,String text,String reason)throws Exception{boolean rejected=false;try{new TaskWorkflow(a,new JSONObject().put("tasks",steps),text);}catch(Exception expected){rejected=true;}yes(rejected,reason);}
 static void run(Activity a){JSONObject out=new JSONObject();n=0;try{
  String text="先打开企业微信查看工作台，不发送消息；然后打开微信搜一搜测试，不关注；最后打开QQ音乐搜索测试歌曲，不购买。";
  JSONArray list=new JSONArray().put(step("com.tencent.wework","查看工作台，不发送消息")).put(step("com.tencent.mm","搜一搜测试，不关注")).put(step("com.tencent.qqmusic","搜索测试歌曲，不购买"));
  TaskWorkflow w=new TaskWorkflow(a,new JSONObject().put("tasks",list),text);yes(w.steps.size()==3,"three selected apps resolved");
  yes(w.steps.get(0).pkg.equals("com.tencent.wework"),"work account not personal WeChat");yes(w.steps.get(1).pkg.equals("com.tencent.mm"),"personal WeChat preserved");
  yes(w.steps.get(0).original.contains("工作台")&&!w.steps.get(0).original.contains("搜一搜"),"work app isolated");yes(w.steps.get(1).original.contains("搜一搜")&&!w.steps.get(1).original.contains("工作台")&&!w.steps.get(1).original.contains("歌曲"),"WeChat isolated");yes(w.steps.get(2).original.contains("歌曲")&&!w.steps.get(2).original.contains("工作台"),"music isolated");
  boolean rejected=false;try{new TaskWorkflow(a,new JSONObject().put("tasks",new JSONArray().put(list.getJSONObject(0)).put(list.getJSONObject(2))),text);}catch(Exception expected){rejected=true;}yes(rejected,"omitted selected app rejected");
  rejectPlan(a,new JSONArray().put(list.getJSONObject(2)).put(list.getJSONObject(1)).put(list.getJSONObject(0)),text,"reversed app order rejected");
  text="企业微信查看工作台微信搜一搜测试QQ音乐搜索测试歌曲";
  w=new TaskWorkflow(a,new JSONObject().put("tasks",list),text);yes(w.steps.size()==3,"ASR without punctuation keeps all apps");
  rejectPlan(a,new JSONArray().put(list.getJSONObject(0)).put(list.getJSONObject(2)),text,"ASR missing middle app rejected");
  text="先召开企业微信查看工作台然后召开开高德地图查看上海人民广场";
  JSONArray decodedSteps=new JSONArray().put(step("com.tencent.wework","查看工作台")).put(step("com.autonavi.minimap","查看上海人民广场"));
  w=new TaskWorkflow(a,new JSONObject().put("tasks",decodedSteps),text);yes(w.steps.size()==2,"observed ASR launch error lost an installed app");
  yes(w.steps.get(0).original.contains("工作台")&&!w.steps.get(0).original.contains("人民广场")&&w.steps.get(1).original.contains("人民广场")&&!w.steps.get(1).original.contains("工作台"),"observed ASR clauses not isolated");
  rejectPlan(a,new JSONArray().put(decodedSteps.getJSONObject(1)),text,"observed ASR omitted first app accepted");
  rejectPlan(a,new JSONArray().put(decodedSteps.getJSONObject(1)).put(decodedSteps.getJSONObject(0)),text,"observed ASR reversed apps accepted");
  text="先打开微信搜索测试然后打开企业微信查看工作台最后回到微信搜索地图教程";
  JSONArray revisit=new JSONArray().put(step("com.tencent.mm","搜索测试")).put(step("com.tencent.wework","查看工作台")).put(step("com.tencent.mm","搜索地图教程"));
  w=new TaskWorkflow(a,new JSONObject().put("tasks",revisit),text);yes(w.steps.size()==3&&w.steps.get(0).pkg.equals(w.steps.get(2).pkg),"explicit app revisit retained");
  rejectPlan(a,new JSONArray().put(step("com.tencent.mm","搜索测试并搜索地图教程")).put(step("com.tencent.wework","查看工作台")),text,"missing app revisit rejected");
  text="先在微信搜索《北京天气》，然后在微信搜索《上海天气》";
  rejectPlan(a,new JSONArray().put(step("com.tencent.mm","搜索上海天气后搜索北京天气")),text,"same-app distinct objects reversed");
  rejectPlan(a,new JSONArray().put(step("com.tencent.mm","搜索北京天气")),text,"same-app second object omitted");
  w=new TaskWorkflow(a,new JSONObject().put("tasks",new JSONArray().put(step("com.tencent.mm","搜索北京天气")).put(step("com.tencent.mm","搜索上海天气"))),text);yes(w.steps.size()==2,"independent same-app searches not merged");
  String fresh="新建完整租赁合同模板文档，由你自行起草正文，租金支付条款初稿写为每月10日前支付，未知资料用占位符，保存文件名为测试租赁合同_顺序固定，保存后重新打开并核对内容。停止条件：文件已保存并重新打开核对完成。";
  String edit="打开本次新建的测试租赁合同_顺序固定文件，仅将租金支付条款由每月10日前支付修改为每月5日前支付，其他段落保持不变，保存后从文件列表重新打开并核对修改结果。只编辑本次新建文件，不签署、不发送。停止条件：保存并从文件列表重新打开核对完成。";
  text="先打开微信搜索测试，然后在WPS新建一份租赁合同，初稿每月10日支付并保存重新打开；再只把本次文件改为每月5日支付，其他条款不变，保存重新打开核对。";
  JSONArray splitDocument=new JSONArray().put(step("com.tencent.mm","搜索测试")).put(step("cn.wps.moffice_eng",fresh)).put(step("cn.wps.moffice_eng",edit));
  w=new TaskWorkflow(a,new JSONObject().put("tasks",splitDocument),text);yes(w.steps.size()==2,"owned WPS creation/edit retains one document stage");String merged=w.steps.get(1).goal;
  yes(merged.indexOf(fresh.replace("停止条件","阶段完成条件"))<merged.indexOf(edit),"document order and both readback conditions retained");
  yes(merged.contains("10日")&&merged.contains("5日")&&merged.contains("其他段落保持不变")&&merged.contains("不签署、不发送"),"document content scope retained");
  yes(splitDocument.length()==3&&splitDocument.getJSONObject(1).getString("goal").equals(fresh),"normalization mutated planner JSON");
  TaskWorkflow restored=TaskWorkflow.restore(a,w.json());yes(restored.steps.size()==2&&restored.index==0&&restored.current().state.equals("paused"),"normalized checkpoint indices preserved");
  for(TaskSpec item:w.steps)yes(!item.verified&&item.state.equals("pending"),"plan validation fabricated completion");
  JSONArray separateDocuments=new JSONArray().put(step("cn.wps.moffice_eng",fresh)).put(step("cn.wps.moffice_eng",edit.replace("顺序固定","另一份")));
  w=new TaskWorkflow(a,new JSONObject().put("tasks",separateDocuments),"在WPS新建一份合同，再打开另一份合同修改支付日");yes(w.steps.size()==2,"different WPS files not merged");
  text="先打开高德地图查公共交通，不打车；然后打开百度网盘搜索测试，不下载。";w=new TaskWorkflow(a,new JSONObject().put("tasks",new JSONArray().put(step("com.autonavi.minimap","查公共交通")).put(step("com.baidu.netdisk","搜索测试"))),text);
  yes(w.steps.size()==2,"map and disk covered");yes(w.steps.get(0).original.contains("公共交通")&&!w.steps.get(0).original.contains("下载"),"map isolated");yes(w.steps.get(1).original.contains("不下载")&&!w.steps.get(1).original.contains("打车"),"disk isolated");
  String gameOriginal="先在微信中找到并短时试玩官方羊了个羊小游戏，约一分钟结束，不需要通关。不要把入口或教程页面说成实际游戏通关；然后在WPS写一份租赁合同，最后打开QQ音乐搜索测试歌曲。";
  JSONArray gameAndOther=new JSONArray().put(step("com.tencent.mm","在微信短时试玩羊了个羊小游戏")).put(step("cn.wps.moffice_eng","新建租赁合同文档")).put(step("com.tencent.qqmusic","搜索测试歌曲"));
  TaskWorkflow gameWorkflow=new TaskWorkflow(a,new JSONObject().put("tasks",gameAndOther),gameOriginal);Config basic=new Config();basic.maxSteps=24;
  yes(gameWorkflow.steps.get(0).config(basic,24).maxSteps==200,"explicit play stage budget lost due later tutorial disclaimer");
  yes(gameWorkflow.steps.get(1).config(basic,24).maxSteps==40,"document stage inherited another app's game budget");
  yes(gameWorkflow.steps.get(2).config(basic,24).maxSteps==24,"ordinary music stage inherited game budget");
  TaskWorkflow tutorial=new TaskWorkflow(a,new JSONObject().put("tasks",new JSONArray().put(step("com.tencent.mm","搜索羊了个羊通关攻略"))),"在微信搜索羊了个羊小游戏通关攻略");yes(tutorial.current().config(basic,24).maxSteps==24,"tutorial search incorrectly received game budget");
  Config c=Config.load(a);for(String pkg:AdaptiveAppApproval.APPS){yes(new AppAccess(a).allowed(pkg),"app selection saved");yes(new RoutineAccess(a).allows(c,Collections.singletonList(pkg)),"recipient scope saved");}
  String original="打开QQ音乐，搜索许嵩的《有何不可》，播放免费版本";TaskRequest music=new TaskRequest("com.tencent.qqmusic",original,"",false);for(String query:new String[]{"许嵩 有何不可","有何不可许嵩"})yes(SearchQueryPolicy.allowed(original,original,music.target,"com.tencent.qqmusic",query),"native derived music query rejected");TaskRequest disk=new TaskRequest("com.baidu.netdisk","找到搜索入口，搜索关键词 测试，看到结果后停止","",false);yes(disk.target.equals("测试"),"native file query captured interface noun");
  out.put("passed",true).put("checks",n).put("apps",5).put("missing_app_rejected",true).put("app_order_preserved",true).put("ASR_unpunctuated_coverage",true).put("legal_app_revisits",true).put("owned_document_continuation",true).put("original_clause_isolation",true).put("model_calls",0).put("business_actions",0);
 }catch(Exception e){try{out.put("passed",false).put("checks",n).put("error",e.getClass().getSimpleName()+": "+e.getMessage());}catch(Exception ignored){}}
 try(FileOutputStream f=a.openFileOutput("adaptive-routing-contracts.json",0)){out.put("sampledAt",System.currentTimeMillis());f.write(out.toString(2).getBytes("UTF-8"));}catch(Exception ignored){}
 }
}
