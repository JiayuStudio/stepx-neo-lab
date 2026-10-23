package studio.jiayu.neoagent;

/** Local checkout detection; evaluated before any screen or node text is sent out. */
final class TaskBoundary {
 static boolean checkoutReview(boolean checkout,String text){
  if(!checkout||text==null)return false;
  for(String marker:new String[]{"提交订单","确认订单","确认下单","提交并支付","确认并支付","去支付","立即支付","实付款","支付订单"})if(text.contains(marker))return true;
  return false;
 }
 static boolean termsPrompt(String context,String consentLabel){return context!=null&&consentLabel!=null&&consentLabel.replaceAll("\\s+","").matches("^(确认并同意|同意并继续|接受条款)([（(][0-9]+.*[）)])?$")&&context.matches("(?s).*(票务须知|观演规则|退票规则|用户协议|服务协议|隐私政策|协议条款).*" );}
 static String question(String text){
  if(text==null||text.trim().isEmpty())return "请查看当前应用画面，处理后回到聊天告诉我下一步目标。";
  String clean=text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]","").trim();return clean.substring(0,Math.min(600,clean.length()));
 }
}
