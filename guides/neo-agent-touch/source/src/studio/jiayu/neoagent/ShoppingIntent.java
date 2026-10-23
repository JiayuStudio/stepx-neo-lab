package studio.jiayu.neoagent;
/** Raw user intent, independent of a model's proposed checkout flag. Submission is never granted here. */
final class ShoppingIntent {
 static boolean planCheckout(boolean shopping,boolean proposed,boolean cartOnly,boolean follow,boolean comparison,String user){return !comparison&&(cartOnly||(!follow&&proposed&&(!shopping||preview(user))));}
 static boolean cartPermitted(boolean cartOnly,boolean checkout,boolean shopping,boolean targetMentioned,String user){return cartOnly||checkout&&shopping&&targetMentioned&&preview(user)&&!cartForbidden(user);}
 static boolean cartForbidden(String user){return user!=null&&(user.matches("(?s).*(不要|禁止|别|不必|不用|不)\\s*(?:先|再)?(?:把[^，。；\\n]{1,20})?(加入|加到|加进|添加|放入|放进|放到|加).{0,3}购物车.*")||user.matches("(?s).*(不要|禁止|别|不用|不走)\\s*(?:先|再)?(?:进|进入|走)?购物车.*"));}
 static boolean cartButton(String label){return label!=null&&label.replaceAll("\\s+","").matches("^(加入购物车|加购)(，?按钮)?$");}
 static boolean cartNavigation(String text,String description){String t=text==null?"":text.trim(),d=description==null?"":description.trim();String label=d.isEmpty()?t:d;boolean consistent=t.isEmpty()||d.isEmpty()||t.equals(d)||t.matches("[\\uE000-\\uF8FF\\uA000-\\uA4CF]");return consistent&&label.replaceAll("\\s+","").matches("^(购物车(?:[（(]?[0-9]+[+]?[）)]?)?|查看购物车|去购物车)(，?按钮)?$");}
 static boolean preview(String user){if(user==null)return false;
  if(user.matches("(?s).*(不要|不必|禁止|不用|别|不进|不去|不走).{0,8}(订单核对|订单确认|确认订单|结算|下单流程).*"))return false;
  if(user.matches("(?s).*(订单核对|订单确认页|确认订单页|结算页|下单流程).*"))return true;
  if(user.matches("(?s).*(只查价|只看价格|先看价格|先看看价格|先查价格|只比价|先比价|只是搜索|仅搜索).*"))return false;
  return user.matches("(?s).*(帮我|替我|给我)(买|购买|下单|点外卖|点餐|点一|点杯|订一).*" );
 }
}
