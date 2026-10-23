package studio.jiayu.neoagent;
import java.util.*;
/** Permission comes from this app's original clause, never another stage or the planner's wording. */
final class TaskScope {
 static String forPackage(String pkg,String user){String name=pkg.equals("cn.wps.moffice_eng")?"WPS":pkg.equals("com.taobao.taobao")?"淘宝":pkg.equals("com.jingdong.app.mall")?"京东":pkg.equals("tv.danmaku.bili")?"哔哩哔哩":pkg.equals("com.sankuai.meituan")?"美团":pkg.equals("com.sankuai.meituan.takeoutnew")?"美团外卖":pkg.equals("me.ele")?"饿了么":pkg.equals("cn.damai")?"大麦":"";
  List<PlanCoverage.Mention> mentions=PlanCoverage.mentions(user);if(name.isEmpty())return user;Set<String> names=new HashSet<>();names.add(name);if(name.equals("美团"))names.add("美团外卖");return forNames(names,user,mentions);
 }
 static String forNames(Set<String> names,String user,List<PlanCoverage.Mention> mentions){if(mentions.size()<2)return user;StringBuilder source=new StringBuilder();
  for(int i=0;i<mentions.size();i++){PlanCoverage.Mention m=mentions.get(i);if(names.contains(m.app)){int start=i==0?0:m.start,end=i+1<mentions.size()?mentions.get(i+1).start:user.length();source.append(user.substring(start,end)).append('\n');}}
  return source.toString().trim();
 }
}
