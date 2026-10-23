package studio.jiayu.neoagent;
public final class TaskBoundaryTest {
 public static void main(String[] args){
  for(String s:new String[]{"确认订单","提交订单 ¥28.8","确认下单","确认并支付","去支付","立即支付","实付款 ¥50","支付订单"})check(TaskBoundary.checkoutReview(true,s),s);
  for(String s:new String[]{"餐品详情","加入购物车","选择规格","去结算","米饭一份","搜索结果"})check(!TaskBoundary.checkoutReview(true,s),s);
  check(!TaskBoundary.checkoutReview(false,"确认订单"),"checkout flag");
  check(TaskBoundary.question(" 请选择规格 ").equals("请选择规格"),"keep concrete question");check(TaskBoundary.question("").length()>0,"fallback");check(TaskBoundary.question(new String(new char[1000]).replace('\0','甲')).length()==600,"bounded");
  System.out.println("PASS: checkout markers across apps, non-final pages, question preservation and limit");
 }
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
