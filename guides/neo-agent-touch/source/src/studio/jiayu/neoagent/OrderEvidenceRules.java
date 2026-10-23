package studio.jiayu.neoagent;
import java.util.*;import java.util.regex.*;import java.math.BigDecimal;
/** Completion facts only. This class never authorizes a payment or submission. */
final class OrderEvidenceRules {
 static String budget(String user){if(user==null)return "";Set<String> values=new HashSet<>();for(String pattern:new String[]{"(?:不超过|最多|预算|合计(?:不超过)?|总额)\\s*([0-9]{1,6}(?:\\.[0-9]{1,2})?)\\s*元","([0-9]{1,6}(?:\\.[0-9]{1,2})?)\\s*元(?:以内|以下)"}){Matcher m=Pattern.compile(pattern).matcher(user);while(m.find())values.add(new BigDecimal(m.group(1)).stripTrailingZeros().toPlainString());}return values.size()>1?"?":values.isEmpty()?"":values.iterator().next();}
 static int wantedQuantity(String user){if(user==null)return 1;Matcher m=Pattern.compile("(?<![0-9])([1-9][0-9]{0,2})\\s*(?:件|个|份|杯)").matcher(user);Set<Integer> values=new HashSet<>();while(m.find())values.add(Integer.parseInt(m.group(1)));return values.size()>1?0:values.isEmpty()?1:values.iterator().next();}
 static boolean sameAmount(String a,String b){try{return a!=null&&b!=null&&new BigDecimal(a).compareTo(new BigDecimal(b))==0;}catch(Exception e){return false;}}
 static boolean verified(boolean requested,boolean target,boolean delivery,boolean finalAction,boolean privateCart,String wantedColor,String selectedColor,int wantedQuantity,int quantity,String headerTotal,String paymentTotal,String budget){
  if(!requested||!target||!delivery||!finalAction||privateCart||wantedQuantity<1||quantity!=wantedQuantity||!sameAmount(headerTotal,paymentTotal)||"?".equals(budget))return false;if(!VariantRules.compatible(wantedColor,selectedColor))return false;try{BigDecimal total=new BigDecimal(headerTotal);return total.signum()>0&&(budget==null||budget.isEmpty()||total.compareTo(new BigDecimal(budget))<=0);}catch(Exception e){return false;}
 }
}
