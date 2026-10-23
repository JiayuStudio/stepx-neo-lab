package studio.jiayu.neoagent;
import java.time.LocalDate;import java.util.regex.*;
/** Narrow, user-derived mutation grants. Never grants payment, messaging or removal. */
final class MutationRules {
 static String normalized(String s){return s==null?"":s.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+","");}
 static boolean targetMentioned(String user,String target){String t=normalized(target);return t.length()>=2&&t.length()<=60&&normalized(user).contains(t);}
 static boolean followGrant(String user,String target,String pkg){return pkg.equals("cn.damai")&&targetMentioned(user,target)&&user.contains("关注")&&!user.matches("(?s).*(不要|别|不必|不用|禁止|取消).{0,5}关注.*");}
 static boolean cartGrant(String user,String target,String pkg){return pkg.equals("com.taobao.taobao")&&targetMentioned(user,target)&&user.matches("(?s).*(加入|加到|加进|添加|放入|放进|放到|加).{0,6}购物车.*")&&!ShoppingIntent.cartForbidden(user);}
 static boolean targetOnPage(String context,String target){String c=normalized(context),t=normalized(target);if(!t.isEmpty()&&c.contains(t))return true;return t.contains("stepxneo")&&c.contains("neo")&&(c.contains("stepx")||c.contains("阶跃"))&&c.contains("手机");}
 static boolean futureShow(String context,LocalDate today){boolean dated=false;Matcher dates=Pattern.compile("(20\\d{2})[年./-]\\s*(\\d{1,2})[月./-]\\s*(\\d{1,2})").matcher(context);while(dates.find()){try{LocalDate date=LocalDate.of(Integer.parseInt(dates.group(1)),Integer.parseInt(dates.group(2)),Integer.parseInt(dates.group(3)));dated=true;if(!date.isBefore(today))return true;}catch(Exception ignored){}}return !dated&&context.matches("(?s).*(未开售|待开票|预售|即将开售|待官宣|开票提醒).*");}
 static boolean followButton(String label,String context,String target,boolean granted){String s=normalized(label).replace("null","").replace("+","").replace("＋","");return granted&&s.matches("^(关注|关注演出|想看)$")&&targetOnPage(context,target)&&context.contains("演唱会")&&futureShow(context,LocalDate.now());}
 static boolean forbiddenForCart(String label,boolean cart){String action=label.replaceAll("[0-9]+(?:\\.[0-9]+)?[万亿]?\\+?\\s*人(?:已)?付款","销量信息");return cart&&action.matches("(?s).*(立即购买|立即抢购|去结算|结算|提交订单|立即支付|去支付|付款).*");}
}
