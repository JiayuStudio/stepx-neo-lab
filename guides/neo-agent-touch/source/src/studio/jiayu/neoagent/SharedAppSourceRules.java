package studio.jiayu.neoagent;
import java.util.*;
import java.util.regex.*;

/** A narrow native scope: one consecutive app list sharing one public price query. */
final class SharedAppSourceRules {
 private static final Pattern PREFIX=Pattern.compile("(?i)^(?:(?:请你|我想要|我想|我要|请|麻烦|帮我|给我|现在|先|前往|分别|依次|同时|使用|通过|打开|进入|比较|对比|比价|看看|查查|查询|一下|购物平台|电商平台|购物软件|电商软件|平台|软件|应用|APP|这几|这|那|几|两|二|三|四|五|六|[2-6]|个|家|大|去|到|在|用|上|里|中)|[\\s、，,:：])*$");
 private static final Pattern GAP=Pattern.compile("\\s*(?:(?:以及|还有|和|与|及|跟|、|，|,|/|\\+|＋)\\s*)*");
 private static final Pattern PRICE=Pattern.compile("比价|比较|对比|价格|价钱|售价|到手价|多少钱|最便宜|最低价|哪家便宜|哪个便宜|哪里便宜|优惠");
 private static final Pattern LEADING=Pattern.compile("^(?:(?:分别|各自|一起|帮我|给我|怎样购买|怎么购买|怎样买|怎么买|哪家购买|哪家买|哪里购买|哪里买|比较一下|对比一下|搜索|搜一下|查找|查一下|查询|查看|看看|看一下|查查|比较|对比|比价|看看|一下|的|都|各|先|在|上|里|中|搜|看|查)\\s*)+");
 private static final Pattern PRICE_TAIL=Pattern.compile("(?:的)?(?:最便宜|最低价|价格|价钱|售价|到手价|多少钱|比价|比较|对比|哪家便宜|哪个便宜|哪里便宜|优惠).*$");
 private static final Pattern FLOW=Pattern.compile("然后|接着|最后|随后|接下来|之后|再去|再打开|再用|再在");
 private static final Pattern EXTRA=Pattern.compile("发送|发消息|删除|上传|下载|播放|写入|新建|创建|领取|领券|收藏|点赞|关注|正式购买|正式下单|直接买|立即买|就买|后买|买一|购买一|替我买|帮我买|给我买");
 static final class Group {
  final String source,target;final List<StageSourceRules.AppSpan> members;
  Group(String u,String t,List<StageSourceRules.AppSpan> a){source=u.trim();target=t;members=Collections.unmodifiableList(new ArrayList<>(a));}
  StageSourceRules.AppSpan member(String pkg){for(StageSourceRules.AppSpan a:members)if(pkg.equals(a.pkg))return a;return null;}
  String goal(String app){return "在"+app+"搜索同一商品「"+target+"」并查价；核对同款型号、规格、数量、售价、运费与优惠适用条件，记录页面实际显示的价格和条件后结束。本阶段只查看，不加购、不提交订单、不付款，不把未核实的券后价当到手价。保留本次全部原话与限制："+source;}
 }
 static Group detect(String user,List<StageSourceRules.AppSpan> apps){
  if(user==null||user.length()>2400||apps.size()<2||apps.size()>6)return null;
  int last=-1;Set<String> identities=new HashSet<>();
  for(StageSourceRules.AppSpan a:apps){
   if(a.start<0||a.end<=a.start||a.end>user.length()||a.start<last||!identities.add(a.pkg)||PlanCoverage.inQuote(user,a.start))return null;
   if(last>=0&&!GAP.matcher(user.substring(last,a.start)).matches())return null;
   last=a.end;
  }
  if(!PREFIX.matcher(user.substring(0,apps.get(0).start)).matches())return null;
  String tail=user.substring(last).trim();String plain=unquoted(user);
  if(!PRICE.matcher(plain).find()||FLOW.matcher(plain).find()||EXTRA.matcher(plain).find())return null;
  // These exact runtime policies must also deny checkout and financial grants.
  // A price question containing '怎样购买' is different from an actual buy request.
  if(ShoppingIntent.preview(user)||PurchasePolicy.grant(user,"com.taobao.taobao").order||PurchasePolicy.grant(user,"com.jingdong.app.mall").order)return null;
  String transactions=plain.replaceAll("(?:不要|禁止|无需|不用|不必|别|不)\\s*(?:提交订单|加入购物车|下单|付款|支付|加购)","");
  if(transactions.matches("(?s).*(?:提交订单|加入购物车|下单|付款|支付|购物车|加购|结算).*"))return null;
  Matcher leading=LEADING.matcher(tail);if(!leading.find())return null;
  String target=tail.substring(leading.end()).trim();int end=firstPunctuation(target);target=target.substring(0,end).trim();
  target=PRICE_TAIL.matcher(target).replaceFirst("").trim().replaceAll("[《》「」“”\"『』]"," ").replaceAll("\\s+"," ").trim();
  if(target.length()<2||target.length()>60||target.matches("(?s).*(?:怎样|怎么|哪家|哪里|便宜|预算|最多|不超过|以内|不要|不买|不购|购买|买|下单|付款|支付|然后|打开|搜索|查询|查看|比较|对比|以及|和|与).*")||PrivateText.credential(target))return null;
  return new Group(user,target,apps);
 }
 private static int firstPunctuation(String text){for(int i=0;i<text.length();i++)if("，。；,;\n!?！？".indexOf(text.charAt(i))>=0&&!PlanCoverage.inQuote(text,i))return i;return text.length();}
 private static String unquoted(String text){StringBuilder out=new StringBuilder(text);for(int i=0;i<text.length();i++)if(PlanCoverage.inQuote(text,i))out.setCharAt(i,' ');return out.toString();}
}
