package studio.jiayu.neoagent;
final class ActionGuard {
 static String withoutSales(String label){return label==null?"":label.replaceAll("[0-9]+(?:\\.[0-9]+)?[万亿]?\\+?\\s*人(?:已)?付款","销量信息");}
 static boolean unlabelledCheckoutBottom(boolean checkout,boolean merchant,boolean hasId,int normalizedY){return checkout&&merchant&&!hasId&&normalizedY>=820;}
 static boolean finalAction(String label){return withoutSales(label).matches("(?s).*(提交订单|确认下单|确认购票|确认选座|支付|付款).*");}
 static boolean requiresHandoff(String label,boolean checkout){
  // Public sales-count captions are evidence about a product, never a payment control.
  String action=withoutSales(label);
  return action.matches("(?s).*((?<!不)同意|接受条款|确认协议|付款|支付|提交订单|确认订单|确认下单|发送|评论|点赞|关注|想看|删除|清空|退出|卸载|授权|个人主页).*")||(!checkout&&action.matches("(?s).*(购买|下单|购票|选座|抢票).*"));
 }
}
