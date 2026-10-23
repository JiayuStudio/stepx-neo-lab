package studio.jiayu.neoagent;
import java.util.*;import java.util.regex.*;import java.security.*;import java.nio.charset.StandardCharsets;
/** Scalar facts extracted from visible native labels. No model-provided prices are accepted. */
final class PurchaseFacts {
 String item="",orderKey="",deliveryKey="";int total=-1,quantity,itemCount;long orderAt;boolean shippingIncluded,deliverySelected,unsafe,confirmPage,cashier,submitted,paid,awaitingPayment;
 static String digest(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 static PurchaseFacts read(Collection<String> raw,boolean unsafeSelection){PurchaseFacts f=new PurchaseFacts();f.unsafe=unsafeSelection;Set<String> items=new LinkedHashSet<>(),orders=new HashSet<>(),deliveries=new HashSet<>();Set<Integer> totals=new HashSet<>(),quantities=new HashSet<>();
  for(String original:raw){if(original==null||original.length()>500)continue;String s=original.replaceAll("\\s+","").replace('￥','¥');
   f.confirmPage|=s.equals("确认订单")||s.equals("填写订单");f.cashier|=s.equals("收银台")||s.equals("订单支付");f.submitted|=s.equals("订单提交成功")||s.equals("下单成功");f.paid|=s.equals("支付成功")||s.equals("付款成功");f.awaitingPayment|=s.equals("待付款")||s.equals("等待付款")||s.equals("待支付");
   Matcher amount=Pattern.compile("^(?:实付款|实付|应付|待付款金额|支付金额)(?:总额|金额)?[:：]?¥([0-9]{1,6}(?:\\.[0-9]{1,2})?)(?:[（(]含运费[）)])?$").matcher(s);if(amount.matches()){int n=PurchasePolicy.cents(amount.group(1));if(n>=0)totals.add(n);if(s.contains("含运费"))f.shippingIncluded=true;}
   if(s.matches("^(?:运费|配送费)[:：]?¥[0-9]{1,6}(?:\\.[0-9]{1,2})?$"))f.shippingIncluded=true;
   Matcher qty=Pattern.compile("^(?:购买数量|数量)[:：]?([1-9][0-9]{0,2})(?:件)?$").matcher(s);if(qty.matches())quantities.add(Integer.parseInt(qty.group(1)));else{qty=Pattern.compile("^共([1-9][0-9]{0,2})件$").matcher(s);if(qty.matches())quantities.add(Integer.parseInt(qty.group(1)));}
   Matcher product=Pattern.compile("^(?:商品名称|商品)[:：](.{2,240})$").matcher(s);if(product.matches())items.add(product.group(1));
   if(s.startsWith("下单时间：")||s.startsWith("下单时间:")){try{java.time.LocalDateTime when=java.time.LocalDateTime.parse(s.substring(5),java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss"));f.orderAt=when.atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();}catch(Exception ignored){}}
   Matcher order=Pattern.compile("^订单(?:编号|号)[:：]([A-Za-z0-9-]{8,40})$").matcher(s);if(order.matches())orders.add(digest(order.group(1)));
   if(s.matches("^(?:配送至|收货地址)[:：].{4,200}$")&&!s.matches("(?s).*(请选择|未选择|暂无|未设置|待确认|添加收货地址|示例地址).*"))deliveries.add(digest(s));
  }
  f.itemCount=items.size();if(items.size()==1)f.item=items.iterator().next();if(totals.size()==1)f.total=totals.iterator().next();if(quantities.size()==1)f.quantity=quantities.iterator().next();if(orders.size()==1)f.orderKey=orders.iterator().next();if(deliveries.size()==1){f.deliveryKey=deliveries.iterator().next();f.deliverySelected=true;}if(f.awaitingPayment)f.paid=false;return f;
 }
 String fingerprint(){return digest(item+"|"+total+"|"+quantity+"|"+itemCount+"|"+shippingIncluded+"|"+deliveryKey+"|"+orderKey+"|"+unsafe+"|"+confirmPage+"|"+cashier);}
}
