package studio.jiayu.neoagent;
import java.math.*;import java.util.*;import java.util.regex.*;
/** This small real-purchase experiment is capped at CNY 5 including delivery, one item per goal. */
final class PurchasePolicy {
 static final int HARD_CAP_CENTS=500;
 static final class Grant {final boolean order,pay;final int cap;Grant(boolean o,boolean p,int c){order=o;pay=p;cap=c;}}
 static boolean merchant(String pkg){return "com.taobao.taobao".equals(pkg)||"com.jingdong.app.mall".equals(pkg);}
 static int cents(String text){try{if(text==null||!text.matches("[0-9]{1,6}(?:\\.[0-9]{1,2})?"))return -1;return new BigDecimal(text).movePointRight(2).intValueExact();}catch(Exception e){return -1;}}
 static Grant grant(String raw,String pkg){if(raw==null||!merchant(pkg)||raw.length()>6000)return new Grant(false,false,0);
  if(!raw.matches("(?s).*(帮我|替我|请|直接).*(正式购买|实际购买|真的购买|正式下单|实际下单).*"))return new Grant(false,false,0);
  String intent=raw.replaceAll("(?:仅|只)(?:购买|买)一件", "购买一件").replaceAll("(?:不|不要|禁止|别)(?:购买|买)(?:虚拟品|虚拟商品|会员|保险|分期)(?:[、和或](?:虚拟品|虚拟商品|会员|保险|分期))*", "");
  if(intent.matches("(?s).*(不|不要|禁止|别|不用|无需|仅|只)(?:再|先|实际|正式)?(?:购买|下单|提交|买|到订单核对|到订单确认).*"))return new Grant(false,false,0);
  if(raw.matches("(?s).*(各一件|每个平台|两件|二件|多件|[2-9][0-9]*件|不含运费|运费另算|美元|美金|港币).*"))return new Grant(false,false,0);
  Set<Integer> caps=new HashSet<>();for(String regex:new String[]{"(?:不超过|最多|预算|限额|含运费不超过|实付不超过)\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:元|块钱|块)","([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:元|块钱|块)(?:以内|以下|内)"}){Matcher m=Pattern.compile(regex).matcher(raw);while(m.find())caps.add(cents(m.group(1)));}
  if(caps.size()!=1)return new Grant(false,false,0);int cap=caps.iterator().next();if(cap<1||cap>HARD_CAP_CENTS)return new Grant(false,false,0);
  boolean pay=!raw.matches("(?s).*(不|不要|禁止|别|无需)(?:自动|实际|正式)?(?:付款|支付).*" );return new Grant(true,pay,cap);
 }
 static boolean ordinaryItem(String title){return title!=null&&title.length()>=2&&title.length()<=240&&title.matches("(?s).*(抽纸|垃圾袋|百洁布|海绵|中性笔|笔芯|文件袋|衣架|抹布|棉签|牙线).*")&&!title.matches("(?s).*(定金|预售|虚拟|充值|彩票|药品|处方|白条|花呗|分期|先用后付|保险|会员|连续|赠品专拍|补差价|运费专拍).*" );}
 static boolean validOrder(Grant g,PurchaseFacts f){return g.order&&f.total>0&&f.total<=g.cap&&f.total<=HARD_CAP_CENTS&&f.quantity==1&&f.itemCount==1&&ordinaryItem(f.item)&&f.shippingIncluded&&f.deliverySelected&&!f.deliveryKey.isEmpty()&&!f.unsafe;}
}
