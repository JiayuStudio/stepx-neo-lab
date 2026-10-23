package studio.jiayu.neoagent;
import java.util.*;

/** Resolve application identity before considering fuzzy matches among selected applications. */
final class AppNameRules {
 static final class App {final String pkg,label;final boolean selected;App(String pkg,String label,boolean selected){this.pkg=pkg;this.label=label;this.selected=selected;}}
 static final class Invalid extends Exception {
  final String code;
  Invalid(String code){super("unselected".equals(code)?"该应用尚未选择，请在「交给 Agent 的应用」勾选后保存":"unavailable".equals(code)?"手机没有可启动的该应用，请刷新应用列表":"ambiguous".equals(code)?"匹配到多个应用，请使用完整名称":"手机没有找到对应应用，请告诉我完整应用名称");this.code=code;}
 }
 static final class Snapshot {
  private final ArrayList<App> apps=new ArrayList<>();private final LinkedHashMap<String,String> aliases=new LinkedHashMap<>();
  Snapshot(Collection<App> apps,Map<String,String> aliases){if(apps!=null)for(App app:apps)if(app!=null&&app.pkg!=null&&!app.pkg.isEmpty()&&app.label!=null)this.apps.add(app);if(aliases!=null)for(Map.Entry<String,String> alias:aliases.entrySet())this.aliases.put(normalize(alias.getKey()),alias.getValue());}
  List<String> names(){LinkedHashSet<String> result=new LinkedHashSet<>(aliases.keySet());for(App app:apps){result.add(app.label);result.add(app.pkg);}return new ArrayList<>(result);}
  String resolve(String name)throws Invalid{
   String n=normalize(name);if(n.isEmpty())throw new Invalid("unknown");
   LinkedHashSet<String> exact=new LinkedHashSet<>();for(App app:apps)if(normalize(app.pkg).equals(n))exact.add(app.pkg);if(!exact.isEmpty())return selected(exact);
   String alias=aliases.get(n);if(alias!=null)return selected(Collections.singleton(alias));
   for(App app:apps)if(normalize(app.label).equals(n))exact.add(app.pkg);if(!exact.isEmpty())return selected(exact);
   LinkedHashSet<String> fuzzy=new LinkedHashSet<>();if(n.length()>1)for(App app:apps)if(app.selected&&normalize(app.label).contains(n))fuzzy.add(app.pkg);if(!fuzzy.isEmpty())return selected(fuzzy);
   if(n.length()>1)for(App app:apps)if(normalize(app.label).contains(n))fuzzy.add(app.pkg);if(!fuzzy.isEmpty())return selected(fuzzy);
   throw new Invalid("unknown");
  }
  private String selected(Collection<String> matches)throws Invalid{
   if(matches.size()!=1)throw new Invalid("ambiguous");String pkg=matches.iterator().next();boolean installed=false,selected=false;for(App app:apps)if(pkg.equals(app.pkg)){installed=true;selected|=app.selected;}
   if(!installed)throw new Invalid("unavailable");if(!selected)throw new Invalid("unselected");return pkg;
  }
 }
 static String normalize(String name){return name==null?"":name.trim().toLowerCase(Locale.ROOT).replace(" ","");}
 static LinkedHashMap<String,String> aliases(String own,Collection<App> installed){
  LinkedHashMap<String,String> result=new LinkedHashMap<>();
  String[][] names={{"cn.wps.moffice_eng","wps","wpsoffice","wps办公"},{"tv.danmaku.bili","b站","bilibili","哔哩哔哩"},{"com.taobao.taobao","淘宝"},{"com.jingdong.app.mall","京东"},{"com.xunmeng.pinduoduo","拼多多"},{"com.sankuai.meituan","美团"},{"com.sankuai.meituan.takeoutnew","美团外卖"},{"me.ele","饿了么","淘宝闪购"},{"com.tencent.mm","微信","wechat"},{"com.tencent.wework","企业微信","wecom"},{"com.autonavi.minimap","高德地图","高德","amap"},{"com.tencent.qqmusic","qq音乐","qqmusic"},{"com.baidu.netdisk","百度网盘"},{"cn.damai","大麦"}};
  for(String[] app:names)for(int i=1;i<app.length;i++)result.put(normalize(app[i]),app[0]);
  boolean takeout=false,meituan=false;if(installed!=null)for(App app:installed){takeout|=app.pkg.equals("com.sankuai.meituan.takeoutnew");meituan|=app.pkg.equals("com.sankuai.meituan");}if(!takeout&&meituan)result.put("美团外卖","com.sankuai.meituan");
  if(own!=null&&!own.isEmpty())for(String name:new String[]{"neoagent本地演示","本地演示","neoagenttouch本地演示"})result.put(name,own);return result;
 }
 private AppNameRules(){}
}
