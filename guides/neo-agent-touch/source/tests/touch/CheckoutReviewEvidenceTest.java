package studio.jiayu.neoagent;
import java.util.*;
public class CheckoutReviewEvidenceTest {
 static int n;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);n++;}
 static List<String> goods(){return new ArrayList<>(Arrays.asList("确认订单","商品：StepX Neo 16+256G 手机","数量：1","实付¥4.99(含运费)","收货地址：本地合成收件地址","提交订单"));}
 static List<String> movie(){return new ArrayList<>(Arrays.asList("确认订单","影片：本地合成电影","影院：本地合成影院","场次：2026-10-23 20:30","座位：5排6座","数量：1","合计¥39.9","提交订单"));}
 public static void main(String[] args){
  check(CheckoutReviewEvidence.verified(goods(),"StepX Neo 16+256G",1,false),"native goods review missing");
  check(!CheckoutReviewEvidence.verified(goods(),"StepX Neo 16+512G",1,false),"wrong variant accepted");
  check(!CheckoutReviewEvidence.verified(goods(),"",2,false),"wrong quantity accepted");
  List<String> g=goods();g.remove("收货地址：本地合成收件地址");check(!CheckoutReviewEvidence.verified(g,"",1,false),"missing delivery accepted");
  check(!CheckoutReviewEvidence.verified(Arrays.asList("选择规格","数量：1","立即支付¥1.97"),"",1,false),"direct SKU payment is not review");
  check(!CheckoutReviewEvidence.verified(Arrays.asList("确认订单","提交订单"),"",1,false),"title alone accepted");
  check(!CheckoutReviewEvidence.verified(Arrays.asList("实付款¥4.99","立即支付"),"",1,false),"money alone accepted");
  check(CheckoutReviewEvidence.verified(movie(),"本地合成电影",1,true),"native movie review missing");
  List<String> m=movie();m.set(0,"选择座位");check(!CheckoutReviewEvidence.verified(m,"",1,true),"seat picker counted as review");
  for(String field:new String[]{"影院：本地合成影院","场次：2026-10-23 20:30","座位：5排6座","数量：1","合计¥39.9"}){m=movie();m.remove(field);check(!CheckoutReviewEvidence.verified(m,"",1,true),"missing movie field: "+field);}
  check(!CheckoutReviewEvidence.verified(movie(),"另一部影片",1,true),"wrong movie accepted");
  m=movie();m.add("合计¥40");check(!CheckoutReviewEvidence.verified(m,"",1,true),"conflicting amount accepted");
  m=movie();m.add("数量：2");check(!CheckoutReviewEvidence.verified(m,"",1,true),"conflicting quantity accepted");
  m=movie();m.set(4,"座位：请选择5排6座");check(!CheckoutReviewEvidence.verified(m,"",1,true),"unselected seat accepted");
  g=goods();g.add("数量：2");check(!CheckoutReviewEvidence.verified(g,"",1,false),"conflicting goods quantity accepted");
  check(!CheckoutReviewEvidence.verified(Arrays.asList("确认订单","影片：本地合成电影","影院：本地合成影院","场次：明天","座位：5排6座","数量：1","合计¥39.9","提交订单"),"",1,true),"show time missing");
  System.out.println("PASS: "+n+" checkout review evidence and false completion cases");
 }
}
