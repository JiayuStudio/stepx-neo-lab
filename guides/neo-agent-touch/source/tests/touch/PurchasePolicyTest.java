package studio.jiayu.neoagent;
import java.util.*;
public class PurchasePolicyTest {
 static int n;static void yes(boolean ok,String why){if(!ok)throw new AssertionError(why);n++;}
 static final String USER="帮我正式购买一件5块钱内的物品看看流程是否跑通京东淘宝";
 static PurchaseFacts facts(String... more){ArrayList<String> labels=new ArrayList<>(Arrays.asList("确认订单","商品：百洁布一包","数量：1","实付款：￥4.99（含运费）","收货地址：本地测试收件地址"));labels.addAll(Arrays.asList(more));return PurchaseFacts.read(labels,false);}
 public static void main(String[] args){PurchasePolicy.Grant g=PurchasePolicy.grant(USER,"com.taobao.taobao");yes(g.order&&g.pay&&g.cap==500,"actual user grant lost");yes(PurchasePolicy.grant(USER,"com.jingdong.app.mall").order,"JD not supported");yes(!PurchasePolicy.grant(USER,"com.sankuai.meituan").order,"wrong merchant");
  for(String text:new String[]{"看看5元内商品，不下单","帮我正式购买一件5元以内商品，只到订单核对页","帮我正式购买一件5元以内商品，不提交订单","帮我正式购买两件5元内商品","帮我正式购买京东淘宝各一件5元内商品","帮我正式购买一件5元以内商品，运费另算","帮我正式购买一件10元以内商品","帮我正式购买一件5美元以内商品","帮我正式购买一件商品","帮我正式购买一件预算3元、不超过5元的商品"})yes(!PurchasePolicy.grant(text,"com.taobao.taobao").order,"overbroad grant: "+text);
  yes(PurchasePolicy.grant(USER+"。不买虚拟品、会员、保险或分期，使用已有默认地址。","com.jingdong.app.mall").order,"Excluded unsafe goods incorrectly revoke purchase");
  yes(PurchasePolicy.grant("帮我正式购买一件5块钱内的物品看看流程是否跑通京东淘宝。两平台合计只买一件普通实物日用品，含运费实付不超过5元；优先百洁布或抽纸，使用已保存的默认收货地址，不添加地址、不买虚拟品、会员、保险或分期。若一个平台已提交，不在另一个平台重复下单。密码、验证码或银行确认交给我处理。","com.jingdong.app.mall").order,"Exact phone chat request lost");
  yes(!PurchasePolicy.grant(USER+"。不买了。","com.jingdong.app.mall").order,"Cancellation ignored");
  yes(!PurchasePolicy.grant(USER+"。不要购买百洁布。","com.jingdong.app.mall").order,"Product-specific exclusion relaxed");
  yes(!PurchasePolicy.grant(USER+"。不买虚拟品，不下单。","com.jingdong.app.mall").order,"No-order lost after restricted-goods exclusion");
  yes(!PurchasePolicy.grant("请正式购买一件5元以内商品，不付款","com.taobao.taobao").pay,"no-pay ignored");yes(PurchasePolicy.cents("4.99")==499&&PurchasePolicy.cents("5.00")==500,"cent math");for(String bad:new String[]{"NaN","4.999","-1","1e2","五","5,00","0x5"})yes(PurchasePolicy.cents(bad)==-1,"bad money: "+bad);
  PurchaseFacts good=facts();yes(PurchasePolicy.validOrder(g,good),"native total not accepted");yes(good.confirmPage&&!good.cashier&&!good.paid,"wrong stage");yes(!good.deliveryKey.contains("地址"),"address leaked");
  yes(!PurchasePolicy.validOrder(g,facts("实付款：￥5.01（含运费）")),"conflicting totals accepted");yes(!PurchasePolicy.validOrder(g,facts("数量：2")),"quantity conflict");yes(!PurchasePolicy.validOrder(g,facts("商品：抽纸")),"multiple products");yes(!PurchasePolicy.validOrder(g,facts("收货地址：另一个本地测试地址")),"delivery changed ambiguity");
  PurchaseFacts over=PurchaseFacts.read(Arrays.asList("确认订单","商品：抽纸","数量1","实付¥5.01(含运费)","收货地址：本地测试收件地址"),false);yes(!PurchasePolicy.validOrder(g,over),"over cap");
  PurchaseFacts noShipping=PurchaseFacts.read(Arrays.asList("商品：抽纸","数量1","实付¥4.99","收货地址：本地测试收件地址"),false);yes(!PurchasePolicy.validOrder(g,noShipping),"shipping unknown");
  for(String title:new String[]{"百洁布定金","垃圾袋运费专拍","抽纸分期","会员抽纸","话费充值","虚拟商品","补差价百洁布"})yes(!PurchasePolicy.ordinaryItem(title),"unsafe item");
  yes(!PurchasePolicy.validOrder(g,PurchaseFacts.read(Arrays.asList("确认订单","商品：百洁布","数量1","实付¥4.99(含运费)","收货地址：请选择收货地址"),false)),"missing delivery");
  yes(!PurchasePolicy.validOrder(g,PurchaseFacts.read(Arrays.asList("确认订单","商品：百洁布","数量1","实付¥4.99(含运费)","收货地址：本地测试收件地址"),true)),"credit/add-on selection");
  yes(!facts("订单提交成功").paid,"order creation counted as paid");yes(!facts("支付成功","待付款").paid,"contradictory payment accepted");yes(facts("支付成功").paid,"paid receipt flag missing");yes(!facts("订单号：123456789012").orderKey.isEmpty(),"order identity missing");yes(!facts("订单号：123456789012").orderKey.equals("123456789012"),"order identity not digested");System.out.println("PASS: "+n+" small-purchase grant, total, quantity, shipping and receipt checks");}
}
