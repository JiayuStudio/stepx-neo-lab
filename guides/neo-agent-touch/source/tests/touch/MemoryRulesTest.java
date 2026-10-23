package studio.jiayu.neoagent;
import java.util.*;
public final class MemoryRulesTest {
 static int count;
 public static void main(String[] args){
  Map<String,String> p=MemoryRules.extract("记住，我平时咖啡都要冰的，无糖，大杯，喜欢拿铁");yes("冰".equals(p.get("coffee.temperature")),"temperature");yes("无糖".equals(p.get("coffee.sugar")),"sugar");yes("大杯".equals(p.get("coffee.size")),"size");yes("拿铁".equals(p.get("coffee.drink")),"usual drink");
  for(String s:new String[]{"今天来杯热拿铁", "这次咖啡半糖", "例如我平时喝冰咖啡无糖", "以后咖啡冰的还是热的？", "朋友平时咖啡冰的无糖", "不要记住我的无糖咖啡偏好"})yes(MemoryRules.extract(s).isEmpty(),"do not learn "+s);
  yes("超大杯".equals(MemoryRules.extract("我喜欢超大杯冰拿铁，无糖").get("coffee.size")),"extra large");
  yes(!MemoryRules.extract("我平时咖啡热的和冰的都喝").containsKey("coffee.temperature"),"ambiguous temperature");
  yes("不放香菜".equals(MemoryRules.extract("我平时外卖不吃香菜").get("food.avoid")),"food avoid");
  yes("30元以内".equals(MemoryRules.extract("以后外卖预算30元以内").get("food.budget")),"budget");
  yes(!MemoryRules.relevant("coffee.sugar","在淘宝搜索手机支架"),"unrelated task gets no coffee memory");yes(MemoryRules.relevant("coffee.sugar","点一杯咖啡"),"coffee scope");
  yes(!MemoryRules.safeValue("收货地址：示例街道"),"no private address");yes(!MemoryRules.safeValue("API Key TEST"),"no credential field");
  for(String s:new String[]{"以后咖啡不要无糖", "我平时不喝冰拿铁", "我平时喝热咖啡吗", "我喜欢拿铁而非美式"})yes(MemoryRules.extract(s).isEmpty(),"negative or question: "+s);
  yes("无糖".equals(MemoryRules.extract("我平时拿铁不要糖").get("coffee.sugar")),"positive sugar omission");
  yes("热".equals(MemoryRules.extract("以后咖啡都改成热的").get("coffee.temperature")),"explicit correction");
  System.out.println("PASS: "+count+" preference assertions; explicit habit, one-off, hypothetical, ambiguity, relevance and private data");
 }
 static void yes(boolean ok,String name){if(!ok)throw new AssertionError(name);count++;}
}
