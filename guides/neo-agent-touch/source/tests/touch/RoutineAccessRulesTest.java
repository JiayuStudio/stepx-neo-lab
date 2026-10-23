package studio.jiayu.neoagent;
import java.util.*;
public final class RoutineAccessRulesTest {
 static int checks;
 static void yes(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static List<String> apps(String... packages){return Arrays.asList(packages);}
 static void invalid(Runnable operation,String why){boolean rejected=false;try{operation.run();}catch(IllegalArgumentException expected){rejected=true;}yes(rejected,why);}
 public static void main(String[] args){
  Map<String,Boolean> empty=Collections.emptyMap();yes(!RoutineAccessRules.allows(empty,apps("app.wps"),false),"missing consent accepted");
  LinkedHashMap<String,Boolean> first=RoutineAccessRules.merge(empty,apps("app.wps"),true);
  LinkedHashMap<String,Boolean> mixed=RoutineAccessRules.merge(first,apps("app.wechat"),false);
  yes(!RoutineAccessRules.allows(mixed,apps("app.wechat"),true),"new chat app inherited another app's visual scope");
  yes(RoutineAccessRules.allows(mixed,apps("app.wechat"),false),"explicit chat scope lost");
  yes(RoutineAccessRules.allows(mixed,apps("app.wps"),true),"existing app visual scope overwritten");
  yes(!RoutineAccessRules.allows(mixed,apps("app.wps","app.wechat"),true),"mixed queue elevated chat scope");
  yes(RoutineAccessRules.allows(mixed,apps("app.wps","app.wechat"),false),"mixed queue chat scope lost");
  yes(!first.containsKey("app.wechat")&&empty.isEmpty(),"caller scopes mutated");
  Map<String,Boolean> upgraded=RoutineAccessRules.merge(mixed,apps("app.wechat"),true);
  yes(RoutineAccessRules.allows(upgraded,apps("app.wps","app.wechat"),true),"explicit same-app upgrade lost");
  yes(!RoutineAccessRules.allows(mixed,apps("app.wechat"),true),"upgrade mutated previous scope");
  Map<String,Boolean> repeated=RoutineAccessRules.merge(upgraded,apps("app.wechat","app.wechat"),false);
  yes(repeated.size()==2&&RoutineAccessRules.allows(repeated,apps("app.wechat"),true),"repeat chat approval downgraded same-app visual scope");
  Map<String,Boolean> opposite=RoutineAccessRules.merge(RoutineAccessRules.merge(empty,apps("app.wechat"),false),apps("app.wps"),true);
  yes(!RoutineAccessRules.allows(opposite,apps("app.wechat"),true),"new visual app upgraded an earlier chat-only app");
  yes(!RoutineAccessRules.allows(mixed,apps("app.unknown"),false),"unlisted app accepted");
  yes(!RoutineAccessRules.allows(mixed,apps(),false),"empty queue accepted");yes(!RoutineAccessRules.allows(mixed,null,false),"null queue accepted");yes(!RoutineAccessRules.allows(null,apps("app.wps"),false),"null store accepted");
  yes(!RoutineAccessRules.allows(mixed,apps("app.wps",null),false),"null application accepted");yes(!RoutineAccessRules.allows(mixed,apps(""),false),"empty application accepted");
  for(boolean visual:new boolean[]{false,true}){
   Map<String,Boolean> legacy=RoutineAccessRules.legacy(apps("app.wps","app.wechat"),visual);
   yes(RoutineAccessRules.allows(legacy,apps("app.wps","app.wechat"),false),"old explicit applications lost");
   yes(RoutineAccessRules.allows(legacy,apps("app.wps","app.wechat"),true)==visual,"legacy scope guessed rather than retained");
   Map<String,Boolean> imported=RoutineAccessRules.merge(legacy,apps("app.disk"),false);
   yes(!RoutineAccessRules.allows(imported,apps("app.disk"),true),"legacy scope leaked into new app");
   yes(RoutineAccessRules.allows(imported,apps("app.wps"),true)==visual,"legacy same-app scope changed during migration");
  }
  Map<String,Boolean> otherEndpoint=RoutineAccessRules.merge(empty,apps("app.wechat"),false);
  yes(!RoutineAccessRules.allows(otherEndpoint,apps("app.wps"),true),"separate recipient map inherited applications");
  invalid(()->RoutineAccessRules.merge(empty,null,false),"null approval accepted");invalid(()->RoutineAccessRules.merge(empty,apps(""),true),"empty package approved");invalid(()->RoutineAccessRules.merge(empty,apps(" app.wps"),true),"whitespace package approved");
  HashMap<String,Boolean> malformed=new HashMap<>();malformed.put("app.wps",null);yes(!RoutineAccessRules.allows(malformed,apps("app.wps"),false),"unknown visual value accepted");invalid(()->RoutineAccessRules.merge(malformed,apps("app.wechat"),true),"malformed prior scope adopted");
  System.out.println("RoutineAccessRulesTest: "+checks+" checks passed");
 }
}
