package studio.jiayu.neoagent;
import java.util.*;import java.time.*;import java.util.regex.*;
/** An explicitly permitted stop is partial progress, never an order-review success. */
final class MovieBoundaryStop {
 static boolean allowed(String pkg,String original){
  if(!"com.sankuai.meituan".equals(pkg)||original==null)return false;String s=original.replaceAll("\\s+","");
  if(s.matches("(?s).*(不能|不要|不得|不可|不允许)(?:仅)?停在(?:选座页|选座图|座位图).*"))return false;
  return s.matches("(?s).*(电影|影院|场次).*" )&&s.matches("(?s).*(不锁座|不确认选座|不要确认锁座|不生成订单).*" )&&s.matches("(?s).*(若|如果|如需|需要|必须|无法).*" )&&s.matches("(?s).*(停在|停到|止步于)(?:当前)?(?:选座页|选座图|座位图).*" );
 }
 static boolean verified(String pkg,String original,boolean checkout,Collection<String> labels){
  return verified(pkg,original,checkout,labels,LocalDate.now());
 }
 static boolean verified(String pkg,String original,boolean checkout,Collection<String> labels,LocalDate taskDate){
  return verified(pkg,original,checkout,labels,taskDate,taskDate);
 }
 static boolean verified(String pkg,String original,boolean checkout,Collection<String> labels,LocalDate taskDate,LocalDate observedDate){
  if(!checkout||!allowed(pkg,original))return false;boolean seat=false,date=false,time=false;
  ArrayList<String> schedule=new ArrayList<>();
  for(String text:labels){if(text==null||text.length()>500)continue;String s=text.replaceAll("\\s+","");
   if(s.matches("^(确认订单|填写订单|订单核对|订单支付|收银台)$"))return false;
   seat|=s.matches("^(请先选座|请选座|确认选座|确认并锁座|锁定座位)(?:[：:]?[¥￥]?[0-9]+(?:\\.[0-9]{1,2})?元?)?$");
   date|=s.matches("(?s).*(今天|明天|后天|[0-9]{1,2}月[0-9]{1,2}日).*" );time|=s.matches("(?s).*[0-2]?[0-9]:[0-5][0-9].*");
   if(s.matches("(?s).*(今天|明天|后天|[0-9]{1,2}月[0-9]{1,2}日).*"))schedule.add(s);
  }
  return seat&&date&&time&&scheduleMatches(original,schedule,labels,taskDate,observedDate);
 }
 static LocalDate calendarDate(String year,String month,String day,LocalDate base){
  LocalDate d=LocalDate.of(year==null?base.getYear():Integer.parseInt(year),Integer.parseInt(month),Integer.parseInt(day));
  if(year==null&&d.isBefore(base.minusMonths(6)))d=d.plusYears(1);else if(year==null&&d.isAfter(base.plusMonths(6)))d=d.minusYears(1);return d;
 }
 static boolean scheduleMatches(String original,Collection<String> dates,Collection<String> labels,LocalDate taskDate,LocalDate observedDate){
  String request=original.replaceAll("\\s+","");Set<LocalDate> wanted=new HashSet<>();
  if(request.contains("今天"))wanted.add(taskDate);if(request.contains("明天"))wanted.add(taskDate.plusDays(1));if(request.contains("后天"))wanted.add(taskDate.plusDays(2));
  Matcher explicit=Pattern.compile("(?:(20[0-9]{2})年)?([0-9]{1,2})月([0-9]{1,2})日").matcher(request);
  while(explicit.find()){try{LocalDate d=calendarDate(explicit.group(1),explicit.group(2),explicit.group(3),taskDate);if(!wanted.isEmpty()&&!request.contains("或")&&!wanted.contains(d))return false;wanted.add(d);}catch(DateTimeException bad){return false;}}
  boolean matched=wanted.isEmpty();
  for(String label:dates){Set<LocalDate> seen=new HashSet<>();Matcher m=Pattern.compile("(?:(20[0-9]{2})年)?([0-9]{1,2})月([0-9]{1,2})日").matcher(label);boolean hasExplicit=false;
   while(m.find()){hasExplicit=true;try{seen.add(calendarDate(m.group(1),m.group(2),m.group(3),observedDate));}catch(DateTimeException bad){return false;}}
   // The request keeps its start date; the current screen's relative labels use the observation date.
   if(!hasExplicit){if(label.contains("今天"))seen.add(observedDate);if(label.contains("明天"))seen.add(observedDate.plusDays(1));if(label.contains("后天"))seen.add(observedDate.plusDays(2));}
   for(LocalDate d:seen)if(wanted.contains(d))matched=true;
  }
  if(!matched)return false;
  String joined=String.join("\n",labels).replaceAll("\\s+","");
  Matcher title=Pattern.compile("[《「]([^》」]{1,80})[》」]").matcher(request);boolean hasTitle=false,titleMatched=false;
  while(title.find()){hasTitle=true;if(joined.contains(title.group(1)))titleMatched=true;}if(hasTitle&&!titleMatched)return false;
  Matcher clock=Pattern.compile("(?<![0-9])([0-2]?[0-9]:[0-5][0-9])(?![0-9])").matcher(request);boolean hasClock=false,clockMatched=false;
  while(clock.find()){hasClock=true;if(joined.contains(clock.group(1)))clockMatched=true;}return !hasClock||clockMatched;
 }
}
