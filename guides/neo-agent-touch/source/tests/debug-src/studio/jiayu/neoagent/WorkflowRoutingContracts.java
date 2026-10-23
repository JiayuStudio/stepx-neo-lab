package studio.jiayu.neoagent;
import android.app.Activity;import android.content.pm.ApplicationInfo;import java.io.*;import java.util.*;import org.json.*;
/** Construct production workflows from fixed public examples. No planning API or business execution. */
final class WorkflowRoutingContracts {
 interface Job{void run()throws Exception;}
 final Activity activity;int checks;final JSONObject report=new JSONObject();
 WorkflowRoutingContracts(Activity a){activity=a;}
 static void start(Activity a){new Thread(()->new WorkflowRoutingContracts(a).run(),"workflow-routing-contracts").start();}
 void yes(boolean b,String label)throws Exception{report.put("last_check",label);if(!b)throw new Exception(label);checks++;}
 JSONObject step(String pkg,String goal,String source)throws Exception{JSONObject out=new JSONObject().put("app",pkg).put("goal",goal);if(source!=null)out.put("source",source);return out;}
 TaskWorkflow workflow(String user,JSONObject... steps)throws Exception{JSONArray list=new JSONArray();for(JSONObject s:steps)list.put(s);return new TaskWorkflow(activity,new JSONObject().put("tasks",list),user);}
 void rejected(String code,Job job)throws Exception{WorkflowPlanException found=null;try{job.run();}catch(WorkflowPlanException expected){found=expected;}yes(found!=null,"invalid workflow accepted");yes(code.equals(found.code()),"unexpected workflow rejection class");yes(!found.safeFeedback().contains("演唱者甲"),"feedback reflected example text");}
 void run(){RuntimeBefore before=new RuntimeBefore();String selections="";boolean success=false,unchanged=false;try{
  yes((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"debug build required");yes(before.idle,"runtime busy");AppAccess access=new AppAccess(activity);selections=access.saved().toString();
  for(String pkg:Arrays.asList("com.tencent.wework","com.autonavi.minimap","com.tencent.qqmusic"))yes(access.allowed(pkg)&&access.launchable(pkg),"fixture requires existing selected launchable applications");
  String user="先打开企业微信查看工作台用高德地图搜索附近的公园最后又可就说播放演唱者甲的示例歌曲";List<StageSourceRules.Clause> clauses=StageSourceRules.clauses(user,AppTaskScope.spans(activity,user));yes(clauses.size()==3,"unpunctuated implicit stage missing");
  JSONObject work=step("com.tencent.wework","查看工作台后停止",null),map=step("com.autonavi.minimap","搜索附近的公园并展示结果后停止",null),music=step("com.tencent.qqmusic","搜索并播放演唱者甲的歌曲《示例歌曲》，开始播放后停止",clauses.get(2).source);
  rejected("category_conflict",()->workflow(user,work,map,step("com.tencent.wework","搜索并播放歌曲《示例歌曲》，开始播放后停止",null)));
  rejected("source_required",()->workflow(user,work,map,step("com.tencent.qqmusic",music.getString("goal"),null)));
  TaskWorkflow good=workflow(user,work,map,music);yes(good.steps.size()==3,"three stages not retained");TaskSpec third=good.steps.get(2);
  yes(third.pkg.equals("com.tencent.qqmusic")&&third.original.equals(clauses.get(2).source),"implicit music did not retain own exact original");yes(!third.original.contains("工作台")&&!third.original.contains("公园"),"another stage leaked into music permission source");
  for(TaskSpec s:good.steps)yes(!s.verified&&s.state.equals("pending"),"construction created completion evidence");
  TaskWorkflow restored=TaskWorkflow.restore(activity,good.json());yes(restored.steps.get(2).source.equals(third.source)&&restored.steps.get(2).original.equals(third.original),"checkpoint lost exact source binding");
  JSONObject forged=good.json();forged.getJSONArray("tasks").getJSONObject(2).put("source","播放演唱者甲的示例歌曲");rejected("source_invalid",()->TaskWorkflow.restore(activity,forged));
  rejected("source_invalid",()->workflow(user,work,map,new JSONObject(music.toString()).put("source",7)));
  rejected("source_app_mismatch",()->workflow(user,work,map,new JSONObject(music.toString()).put("source",clauses.get(1).source)));
  rejected("source_order",()->workflow(user,music,work,map));
  String negative="不要播放示例歌曲";rejected("source_denied",()->workflow(negative,step("com.tencent.qqmusic","搜索并播放示例歌曲",negative)));
  String deniedApp="不要在QQ音乐播放示例歌曲";rejected("source_denied",()->workflow(deniedApp,step("com.tencent.qqmusic","搜索并播放示例歌曲",deniedApp)));
  String limited="先搜索日用品，只查价格，不买，预算5元，停在结果页";rejected("source_invalid",()->workflow(limited,step("com.tencent.qqmusic","搜索日用品","搜索日用品")));
  String global="总预算5元，先打开企业微信看工作台，然后播放示例歌曲";List<StageSourceRules.Clause> gc=StageSourceRules.clauses(global,AppTaskScope.spans(activity,global));rejected("source_context",()->workflow(global,step("com.tencent.wework","查看工作台",null),step("com.tencent.qqmusic","搜索并播放示例歌曲",gc.get(gc.size()-1).source)));
  String revisit="先打开企业微信看工作台然后打开高德地图查公园最后回到企业微信查看通讯录";TaskWorkflow revisited=workflow(revisit,work,map,step("com.tencent.wework","查看通讯录后停止",null));yes(revisited.steps.size()==3,"legal app revisit rejected or merged");
  if(access.allowed("com.tencent.mm")&&access.launchable("com.tencent.mm")){
   String voice="在微信播放朋友发来的语音";yes(workflow(voice,step("com.tencent.mm","播放朋友语音后停止",voice)).steps.size()==1,"explicit WeChat voice misrouted");
   String mini="在微信打开音乐小程序播放示例歌曲";yes(workflow(mini,step("com.tencent.mm","播放示例歌曲",mini)).steps.size()==1,"explicit in-app music capability rejected");
  }
  if(access.allowed("cn.wps.moffice_eng")&&access.launchable("cn.wps.moffice_eng")){
   String text="在WPS写入《播放示例歌曲》这段文字，不播放音乐";yes(workflow(text,step("cn.wps.moffice_eng","写入《播放示例歌曲》文字，不播放音乐",text)).steps.size()==1,"document text became music task");
  }
  if(access.allowed("tv.danmaku.bili")&&access.launchable("tv.danmaku.bili")){
   String video="在哔哩哔哩搜索并播放音乐视频MV";yes(workflow(video,step("tv.danmaku.bili","播放音乐视频MV后停止",video)).steps.size()==1,"video became standalone music task");
  }
  String routing=access.routing(user);JSONArray candidates=(JSONArray)new JSONTokener(routing.substring(routing.indexOf('['))).nextValue();HashSet<String> included=new HashSet<>();HashMap<String,Integer> counts=new HashMap<>();
  for(int i=0;i<candidates.length();i++){JSONObject candidate=candidates.getJSONObject(i);String pkg=candidate.getString("package");yes(access.allowed(pkg)&&access.launchable(pkg)&&included.add(pkg),"candidate expanded permission or duplicated app");String type=candidate.getString("category");counts.put(type,counts.getOrDefault(type,0)+1);}
  yes(included.size()==access.approved().size()&&included.contains("com.tencent.qqmusic"),"matched early category hid authorized music candidate");
  for(int i=0;i<candidates.length();i++){JSONObject candidate=candidates.getJSONObject(i);yes(candidate.getBoolean("sole_in_category")== (counts.get(candidate.getString("category"))==1),"sole category candidate claim incorrect");}
  yes(selections.equals(access.saved().toString()),"routing changed selected applications or defaults");success=true;
 }catch(Exception failure){try{report.put("error","workflow routing contract failed").put("failure_code",failure instanceof WorkflowPlanException?((WorkflowPlanException)failure).code():"fixture_failure");}catch(Exception ignored){}
 }finally{try{unchanged=before.matches()&&(selections.isEmpty()||selections.equals(new AppAccess(activity).saved().toString()));report.put("sampledAt",System.currentTimeMillis()).put("success",success&&unchanged).put("checks",checks).put("cleanup_verified",unchanged).put("runtime_unchanged",unchanged).put("model_calls",0).put("business_actions",0);try(FileOutputStream out=activity.openFileOutput("workflow-routing-contracts.json",0)){out.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}}
 }
 static final class RuntimeBefore {
  final ControlService owner=ControlService.instance;final long generation;final String run;final boolean idle,running,paused,pending;final int actions,calls,shots;
  RuntimeBefore(){if(owner==null){generation=0;run="";idle=true;running=paused=pending=false;actions=calls=shots=0;}else synchronized(owner){generation=owner.generation;run=owner.runId;running=owner.running;paused=owner.paused;pending=owner.pendingDispatch;idle=!running;actions=owner.actions;calls=owner.modelCalls;shots=owner.screenshots;}}
  boolean matches(){if(ControlService.instance!=owner)return false;if(owner==null)return true;synchronized(owner){return owner.generation==generation&&owner.runId.equals(run)&&owner.running==running&&owner.paused==paused&&owner.pendingDispatch==pending&&owner.actions==actions&&owner.modelCalls==calls&&owner.screenshots==shots;}}
 }
}
