package studio.jiayu.neoagent;
import java.util.*;
/** Read-only completion proof. Financial controls alone never prove an order review. */
final class CheckoutReviewEvidence {
 static String normalize(String s){return java.text.Normalizer.normalize(s==null?"":s,java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
 static boolean field(String s,String prefixes){return s.matches("^(?:"+prefixes+")[：:].{2,240}$")&&!s.matches("(?s).*(请选择|未选择|未设置|待确认|暂无|添加收货).*" );}
 static boolean verified(Collection<String> labels,String target,int wantedQuantity,boolean movie){
  PurchaseFacts f=PurchaseFacts.read(labels,false);boolean title=false,submit=false,cinema=false,show=false,seat=false;Set<String> names=new HashSet<>();Set<Integer> amounts=new HashSet<>(),quantities=new HashSet<>();
  for(String label:labels){if(label==null||label.length()>500)continue;String s=label.replaceAll("\\s+","").replace('￥','¥');
   title|=s.matches("^(确认订单|填写订单|订单核对|核对订单)$");submit|=s.matches("^(提交订单|确认下单|确认并支付|提交并支付|立即支付|去支付)(?:[：:]?¥[0-9]{1,6}(?:\\.[0-9]{1,2})?)?$");
   String[] amount=CommerceReadbackRules.amount(s);if(amount!=null&&!amount[0].matches("立即支付|提交订单"))amounts.add(PurchasePolicy.cents(amount[1]));int q=CommerceReadbackRules.quantity(s);if(q>0)quantities.add(q);
   if(movie){if(field(s,"影片|电影|片名"))names.add(s.substring(s.indexOf('：')>=0?s.indexOf('：')+1:s.indexOf(':')+1));cinema|=field(s,"影院");show|=field(s,"场次|观影时间")&&s.matches(".*[0-2]?[0-9]:[0-5][0-9].*");seat|=field(s,"座位|座位信息")&&s.matches(".*[0-9]+排[0-9]+座.*");}
  }
  if(!title||!submit||wantedQuantity<1||amounts.size()!=1||amounts.iterator().next()<1||quantities.size()!=1||quantities.iterator().next()!=wantedQuantity)return false;
  if(movie)return names.size()==1&&cinema&&show&&seat&&(normalize(target).isEmpty()||normalize(names.iterator().next()).contains(normalize(target)));
  return f.itemCount==1&&f.quantity==wantedQuantity&&f.total==amounts.iterator().next()&&f.shippingIncluded&&f.deliverySelected&&!f.deliveryKey.isEmpty()&&(normalize(target).isEmpty()||normalize(f.item).contains(normalize(target)));
 }
}
