package studio.jiayu.neoagent;
/** Exact controls, plus a strictly monetary suffix on final payment controls (read-only). */
final class FooterLabelRules {
 static String normalizeCurrency(String text){if(text!=null&&text.matches("^(实付|应付|合计|总计|共计|立即支付|提交订单|确认并支付|提交并支付|去支付)(?:金额)?[:：]?Y[0-9]{1,6}\\.[0-9]{1,2}(?:$|[|｜].*|优惠前.*|共减.*|已减.*|含.*|包.*|[（(].*)"))return text.replaceFirst("Y","¥");return text;}
 static boolean accepted(String text){if(text==null)return false;if(text.matches("^(确定|加入购物车|领券购买|立即购买|去结算|提交订单|立即支付|确认并支付|提交并支付|去支付)$"))return true;return text.matches("^(立即支付|提交订单|确认并支付|提交并支付|去支付)[:：]?[¥￥][0-9]{1,6}(?:\\.[0-9]{1,2})?(?:元)?$");}
}
