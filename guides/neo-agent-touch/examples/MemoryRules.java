package studio.jiayu.neoagent;
import java.util.*;

/** Learns explicit habits, never guesses or treats a one-off order as a permanent preference. */
final class MemoryRules {
 static final LinkedHashMap<String,String> LABELS=new LinkedHashMap<>();
 static {LABELS.put("coffee.drink","常喝的咖啡");LABELS.put("coffee.brand","咖啡品牌");LABELS.put("coffee.temperature","咖啡冷热");LABELS.put("coffee.sugar","咖啡糖量");LABELS.put("coffee.size","咖啡杯型");LABELS.put("food.usual","常点的外卖");LABELS.put("food.avoid","口味与忌口");LABELS.put("food.budget","外卖预算");}
 static boolean safeValue(String value){return value!=null&&value.length()<=100&&!value.matches("(?is).*(sk-[a-z0-9]{12,}|api.?key|验证码|密码|身份证|银行卡|收货地址|详细地址|https?://|[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}|1[3-9][0-9]{9}).*");}
 static Map<String,String> extract(String text){
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  if(text==null||text.length()>3000||!text.matches("(?s).*(习惯|平时|一般|通常|以后|记住|记一下|每次|默认|常点|常喝|我喜欢).*")||text.matches("(?s).*(例如|举例|假如|如果|今天|这次|本次|临时|偶尔|试试|还是|是不是|不喜欢|不要热|不要冰|不是|不要记|别记|忘记|朋友|同事|客户|别人|妈妈|爸爸|孩子|[？?]).*")||!safeValue(text))return out;
  String affirmed=text.replace("不另外加糖","无糖").replace("不加糖","无糖").replace("不要糖","无糖").replace("不放香菜","忌香菜").replace("不要香菜","忌香菜").replace("不吃香菜","忌香菜");
  if(affirmed.matches("(?s).*(不要|不喝|不吃|不选|别给|而非|取消).*")||text.matches("(?s).*(吗|呢|么)[。.!！]*$"))return out;
  boolean coffee=text.matches("(?is).*(咖啡|拿铁|美式|馥芮白|卡布奇诺|澳白|瑞幸|星巴克|库迪).*");
  if(coffee){
   choose(out,"coffee.drink",text,new String[][]{{"拿铁","拿铁"},{"美式","美式"},{"馥芮白","馥芮白"},{"卡布奇诺","卡布奇诺"},{"澳白","澳白"}});
   choose(out,"coffee.brand",text,new String[][]{{"瑞幸","瑞幸"},{"星巴克","星巴克"},{"库迪","库迪"}});
   String temp=text.replace("去冰","").replace("少冰","");
   if(text.contains("去冰")&&!temp.contains("热"))out.put("coffee.temperature","去冰");
   else if(text.contains("少冰")&&!temp.contains("热"))out.put("coffee.temperature","少冰");
   else choose(out,"coffee.temperature",text,new String[][]{{"热的|热饮|热咖啡|热拿铁|热美式","热"},{"冰的|冰饮|冰咖啡|冰拿铁|冰美式","冰"},{"常温","常温"}});
   choose(out,"coffee.sugar",text,new String[][]{{"无糖|不加糖|不另外加糖|不要糖|零糖","无糖"},{"三分糖","三分糖"},{"微糖","微糖"},{"半糖|五分糖","半糖"},{"七分糖","七分糖"},{"全糖|正常糖","正常糖"}});
   choose(out,"coffee.size",text,new String[][]{{"中杯","中杯"},{"(?<!超)大杯","大杯"},{"超大杯","超大杯"},{"小杯","小杯"}});
  }
  if(text.contains("外卖")||text.contains("吃饭")||text.contains("餐")){
   if(text.contains("不吃香菜")||text.contains("不要香菜")||text.contains("不放香菜"))out.put("food.avoid","不放香菜");
   java.util.regex.Matcher usual=java.util.regex.Pattern.compile("外卖(?:常点|一般点|通常点|喜欢点|都点)([\\p{L}\\p{N}]{1,24})").matcher(text);if(usual.find()&&safeValue(usual.group(1)))out.put("food.usual",usual.group(1));
   java.util.regex.Matcher budget=java.util.regex.Pattern.compile("(?:外卖|餐)(?:的)?预算(?:是|为)?\\s*(\\d{1,3})\\s*元?(?:以内|以下|左右)?").matcher(text);if(budget.find())out.put("food.budget",budget.group(1)+"元以内");
  }
  return out;
 }
 static void choose(Map<String,String> out,String key,String text,String[][] rules){LinkedHashSet<String> found=new LinkedHashSet<>();for(String[] rule:rules)if(text.matches("(?is).*?(?:"+rule[0]+").*"))found.add(rule[1]);if(found.size()==1)out.put(key,found.iterator().next());}
 static Map<String,String> choices(String text){if(text==null)return java.util.Collections.emptyMap();return extract("记住，我平时"+text.replaceAll("今天|这次|本次|临时|偶尔|试试",""));}
 static boolean relevant(String key,String text){if(text.matches("(?s).*(照旧|老样子|和平时一样|按习惯).*"))return true;if(key.startsWith("coffee."))return text.matches("(?is).*(咖啡|拿铁|美式|馥芮白|瑞幸|星巴克|库迪|澳白|卡布奇诺).*");return text.matches("(?s).*(外卖|吃饭|午餐|晚餐|早餐|午饭|晚饭|餐品|点餐).*");}
}
