package studio.jiayu.neoagent;
import java.util.*;
public final class StageSourceRulesTest {
 static int n;static void yes(boolean b,String label){if(!b)throw new AssertionError(label);n++;}
 interface Attempt{void run()throws Exception;}
 static void reject(String code,Attempt a)throws Exception{try{a.run();throw new AssertionError("accepted "+code);}catch(WorkflowPlanException e){yes(e.code().equals(code),"wrong safe code");yes(!e.safeFeedback().contains("演唱者甲")&&!e.getMessage().contains("标记私文"),"unsafe feedback");}}
 static List<StageSourceRules.AppSpan> apps(String user,String... names){List<StageSourceRules.AppSpan> out=new ArrayList<>();for(PlanCoverage.Mention m:PlanCoverage.mentions(user,Arrays.asList(names)))out.add(new StageSourceRules.AppSpan(m.app,m.start,m.end));return out;}
 public static void main(String[] args)throws Exception{
  String u="先打开企业微信查看工作台用高德地图搜索附近的公园最后又可就说播放演唱者甲的示例歌曲";List<StageSourceRules.AppSpan> a=apps(u,"企业微信","高德地图","QQ音乐");List<StageSourceRules.Clause> clauses=StageSourceRules.clauses(u,a);
  yes(clauses.size()==3,"implicit final stage was swallowed");yes(clauses.get(0).source.equals("先打开企业微信查看工作台"),"work source rewritten");yes(clauses.get(1).source.equals("用高德地图搜索附近的公园"),"map scope leaked");yes(clauses.get(2).source.equals("最后又可就说播放演唱者甲的示例歌曲"),"noisy source corrected");
  StageSourceRules.Clause music=StageSourceRules.bind(u,clauses.get(2).source,"QQ音乐",a);yes(music.source.equals(u.substring(music.start,music.end)),"source is not exact original range");
  reject("source_invalid",()->StageSourceRules.bind(u,"播放演唱者甲的示例歌曲","QQ音乐",a));
  reject("source_invalid",()->StageSourceRules.bind(u,"最后打开QQ音乐播放演唱者甲的示例歌曲","QQ音乐",a));
  reject("source_app_mismatch",()->StageSourceRules.bind(u,clauses.get(1).source,"QQ音乐",a));
  StageSourceRules.ordered(a,Arrays.asList("企业微信","高德地图","QQ音乐"),Arrays.asList(null,null,music));n++;
  reject("source_order",()->StageSourceRules.ordered(a,Arrays.asList("QQ音乐","企业微信","高德地图"),Arrays.asList(music,null,null)));
  String limited="先搜索日用品，只查价格，不买，预算5元，停在结果页";List<StageSourceRules.AppSpan> noApps=Collections.emptyList();
  StageSourceRules.Clause full=StageSourceRules.bind(limited,limited,"购物应用",noApps);yes(full.source.contains("不买，预算5元"),"restrictions dropped");
  reject("source_invalid",()->StageSourceRules.bind(limited,"搜索日用品","购物应用",noApps));
  reject("source_invalid",()->StageSourceRules.bind(limited,"买，预算5元","购物应用",noApps));
  String context="总预算5元，先在淘宝查纸巾，然后播放示例歌曲";List<StageSourceRules.AppSpan> contextApps=apps(context,"淘宝");String tail=StageSourceRules.clauses(context,contextApps).get(1).source;
  reject("source_context",()->StageSourceRules.bind(context,tail,"QQ音乐",contextApps));
  String global="先播放示例歌曲，然后打开企业微信看工作台，所有任务不要购买";List<StageSourceRules.AppSpan> ga=apps(global,"企业微信");String first=StageSourceRules.clauses(global,ga).get(0).source;
  reject("source_context",()->StageSourceRules.bind(global,first,"QQ音乐",ga));
  String local="先打开企业微信看工作台，不发送消息；然后播放示例歌曲";List<StageSourceRules.AppSpan> la=apps(local,"企业微信");List<StageSourceRules.Clause> lc=StageSourceRules.clauses(local,la);yes(StageSourceRules.bind(local,lc.get(lc.size()-1).source,"QQ音乐",la).source.contains("播放"),"another app's local restriction blocked music");
  String quote="在WPS写入《然后播放示例歌曲》，不要执行，然后打开企业微信";List<StageSourceRules.AppSpan> qa=apps(quote,"WPS","企业微信");yes(StageSourceRules.clauses(quote,qa).size()==2,"quoted flow split");
  String revisit="先打开微信搜索甲然后打开高德地图查公园最后回到微信搜索乙";List<StageSourceRules.AppSpan> ra=apps(revisit,"微信","高德地图");List<StageSourceRules.Clause> rc=StageSourceRules.clauses(revisit,ra);yes(rc.size()==3,"revisit lost");StageSourceRules.ordered(ra,Arrays.asList("微信","高德地图","微信"),rc);n++;
  String same="先在WPS新建合同然后在WPS修改并保存";List<StageSourceRules.AppSpan> sa=apps(same,"WPS");yes(StageSourceRules.bind(same,same,"WPS",sa).source.equals(same),"same-app continuous operations rejected");
  String duplicate="然后播放示例歌曲然后播放示例歌曲";reject("source_invalid",()->StageSourceRules.bind(duplicate,"然后播放示例歌曲","QQ音乐",noApps));
  yes(WorkflowIntentRules.conflict("通讯","最后播放演唱者甲的示例歌曲","搜索并播放示例歌曲",false),"music routed into communications");
  yes(!WorkflowIntentRules.conflict("音乐",music.source,"搜索并播放示例歌曲",false),"compatible music rejected");
  for(String g:new String[]{"在微信播放朋友发来的语音","播放柯南第一集视频","在B站搜索并播放音乐视频MV","在WPS写入‘播放示例歌曲’这段文字，不播放音乐","搜索歌曲链接，不播放音乐","暂停播放音乐","打开音乐播放器设置"})yes(!WorkflowIntentRules.independentMusic(g),"nonmusic playback false conflict");
  yes(!WorkflowIntentRules.conflict("通讯","在微信打开音乐小程序播放歌曲","播放示例歌曲",true),"explicit in-app media blocked");
  yes(!WorkflowIntentRules.positivePlayback("最后不要播放示例歌曲"),"negative became playback grant");yes(!WorkflowIntentRules.positivePlayback("请写下播放示例歌曲这几个字"),"text became playback grant");yes(WorkflowIntentRules.positivePlayback(music.source),"noisy positive playback lost");
  yes(!WorkflowIntentRules.positivePlayback("不要在QQ音乐播放示例歌曲"),"app name separated denial from verb");yes(!WorkflowIntentRules.positivePlayback("请勿使用音乐应用播放歌曲"),"polite prohibition became playback grant");
  yes(CandidateRoutingRules.priority(true,true)<CandidateRoutingRules.priority(true,false),"related default not preferred");yes(CandidateRoutingRules.priority(true,false)<CandidateRoutingRules.priority(false,true),"unrelated default outranked explicit category");yes(CandidateRoutingRules.priority(false,true)<CandidateRoutingRules.priority(false,false),"default not prioritized");
  WorkflowPlanException unknown=new WorkflowPlanException("标记私文");yes(unknown.code().equals("source_invalid")&&!unknown.safeFeedback().contains("标记私文"),"unknown code leaked");
  System.out.println("PASS: "+n+" exact source, restriction, ordering, capability and candidate priority checks");
 }
}
