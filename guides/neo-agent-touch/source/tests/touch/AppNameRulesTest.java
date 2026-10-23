package studio.jiayu.neoagent;
import java.util.*;
public final class AppNameRulesTest {
 static int checks;
 static void yes(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static AppNameRules.App app(String pkg,String label,boolean selected){return new AppNameRules.App(pkg,label,selected);}
 static AppNameRules.Snapshot snapshot(AppNameRules.App... apps){List<AppNameRules.App> list=Arrays.asList(apps);return new AppNameRules.Snapshot(list,AppNameRules.aliases("fixture.agent",list));}
 static void resolves(AppNameRules.Snapshot local,String name,String expected)throws Exception{yes(expected.equals(local.resolve(name)),"wrong identity for "+name);}
 static void rejects(AppNameRules.Snapshot local,String name,String code)throws Exception{try{local.resolve(name);throw new AssertionError("unexpected resolution of "+name);}catch(AppNameRules.Invalid rejected){yes(code.equals(rejected.code),"wrong rejection for "+name);}}
 public static void main(String[] args)throws Exception{
  AppNameRules.App work=app("com.tencent.wework","企业微信",true),personal=app("com.tencent.mm","微信",false);
  AppNameRules.Snapshot workOnly=snapshot(work,personal);
  for(String name:new String[]{"微信"," WeChat ","com.tencent.mm"})rejects(workOnly,name,"unselected");
  for(String name:new String[]{"企业微信","WeCom","com.tencent.wework"})resolves(workOnly,name,"com.tencent.wework");
  rejects(snapshot(work),"微信","unavailable");
  AppNameRules.Snapshot both=snapshot(work,app("com.tencent.mm","微信",true));resolves(both,"微信","com.tencent.mm");resolves(both,"企业微信","com.tencent.wework");resolves(both,"COM.TENCENT.MM","com.tencent.mm");
  rejects(snapshot(personal,app("com.tencent.wework","企业微信",false)),"企业微信","unselected");
  AppNameRules.Snapshot custom=snapshot(app("fixture.office","极简办公",true),app("fixture.other","其他办公",false));resolves(custom,"办公","fixture.office");
  rejects(snapshot(app("fixture.office","极简办公",true),app("fixture.other","其他办公",true)),"办公","ambiguous");
  rejects(snapshot(app("fixture.exact","办公",false),app("fixture.office","极简办公",true)),"办公","unselected");
  rejects(snapshot(app("fixture.a","相同名称",true),app("fixture.b","相同名称",false)),"相同名称","ambiguous");
  resolves(snapshot(app("fixture.a","同一名称",true),app("fixture.a","同一名称",true)),"同一名称","fixture.a");
  rejects(custom,"不存在的应用","unknown");rejects(custom,"", "unknown");rejects(custom,null,"unknown");rejects(custom,"办","unknown");
  String[][] aliases={{"cn.wps.moffice_eng","WPS Office"},{"tv.danmaku.bili","B站"},{"com.taobao.taobao","淘宝"},{"com.jingdong.app.mall","京东"},{"com.xunmeng.pinduoduo","拼多多"},{"me.ele","淘宝闪购"},{"com.autonavi.minimap","高德"},{"com.tencent.qqmusic","QQMusic"},{"com.baidu.netdisk","百度网盘"},{"cn.damai","大麦"}};
  for(String[] entry:aliases){resolves(snapshot(app(entry[0],"fixture application",true)),entry[1],entry[0]);rejects(snapshot(app(entry[0],"fixture application",false)),entry[1],"unselected");}
  resolves(snapshot(app("com.sankuai.meituan","美团",true)),"美团外卖","com.sankuai.meituan");
  rejects(snapshot(app("com.sankuai.meituan","美团",true),app("com.sankuai.meituan.takeoutnew","美团外卖",false)),"美团外卖","unselected");
  resolves(snapshot(app("fixture.agent","Neo Agent Touch 本地演示",true)),"本地演示","fixture.agent");
  List<PlanCoverage.Mention> mentions=PlanCoverage.mentions("打开微信搜一搜测试，不发送消息",workOnly.names());yes(mentions.size()==1&&mentions.get(0).app.equals("微信"),"unselected explicit personal app disappeared from source coverage");rejects(workOnly,mentions.get(0).app,"unselected");
  mentions=PlanCoverage.mentions("打开企业微信查看工作台，不发送消息",workOnly.names());yes(mentions.size()==1&&mentions.get(0).app.equals("企业微信"),"personal name inside enterprise name caused false rejection");resolves(workOnly,mentions.get(0).app,"com.tencent.wework");
  mentions=PlanCoverage.mentions("在企业微信搜索微信教程，不发送消息",workOnly.names());yes(mentions.size()==1&&mentions.get(0).app.equals("企业微信"),"search object treated as explicit app launch");
  mentions=PlanCoverage.mentions("先打开企业微信查看工作台，然后打开微信搜一搜测试",workOnly.names());yes(mentions.size()==2,"later unselected explicit app omitted");rejects(workOnly,mentions.get(1).app,"unselected");
  mentions=PlanCoverage.mentions("不要打开微信，只打开企业微信查看工作台",workOnly.names());yes(mentions.size()==1&&mentions.get(0).app.equals("企业微信"),"negated personal-app launch triggered authorization");
  String original="打开微信搜一搜测试，不发送消息",plannerApp="企业微信",fullSource=original;resolves(workOnly,plannerApp,"com.tencent.wework");
  boolean rejectedSource=false;try{for(PlanCoverage.Mention mention:PlanCoverage.mentions(fullSource,workOnly.names()))workOnly.resolve(mention.app);}catch(AppNameRules.Invalid denied){rejectedSource=denied.code.equals("unselected");}yes(rejectedSource,"valid selected planner app hid the unselected exact app in complete original source");
  AppNameRules.Snapshot video=snapshot(work,personal,app("tv.danmaku.bili","哔哩哔哩",true));
  for(String query:new String[]{"用B站搜索如何打开微信","在企业微信搜索怎么使用微信"}){mentions=PlanCoverage.mentions(query,video.names());yes(mentions.size()==1,"question search created a second app launch");for(PlanCoverage.Mention mention:mentions)video.resolve(mention.app);}
  for(String query:new String[]{"用B站搜索如何打开微信，然后打开微信搜一搜测试","用B站搜索如何打开微信然后打开微信搜一搜测试","在企业微信搜索怎么使用微信并打开微信搜一搜测试"}){mentions=PlanCoverage.mentions(query,video.names());yes(mentions.size()==2,"real next app launch was hidden by search context");rejects(video,mentions.get(1).app,"unselected");}
  System.out.println("AppNameRulesTest: "+checks+" checks passed");
 }
}
