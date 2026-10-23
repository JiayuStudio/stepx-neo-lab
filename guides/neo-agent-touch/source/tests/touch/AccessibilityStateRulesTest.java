package studio.jiayu.neoagent;

public final class AccessibilityStateRulesTest {
 static int checks;static void yes(boolean condition){checks++;if(!condition)throw new AssertionError("accessibility-state "+checks);}
 public static void main(String[] args){
  for(int mask=0;mask<32;mask++){boolean registrationKnown=(mask&1)!=0,registered=(mask&2)!=0,enabledKnown=(mask&4)!=0,enabled=(mask&8)!=0,connected=(mask&16)!=0;String actual=AccessibilityStateRules.state(registrationKnown,registered,enabledKnown,enabled,connected);String expected=connected?"connected":registrationKnown&&!registered?"missing":!registrationKnown||!enabledKnown?"unknown":enabled?"enabled_waiting":"disabled";yes(expected.equals(actual));yes(!AccessibilityStateRules.status(actual).contains("首次"));yes(AccessibilityStateRules.marker(registrationKnown,registered,enabledKnown,enabled,connected).startsWith(expected+"|"));}
  for(int mask=0;mask<8;mask++){boolean present=(mask&1)!=0,current=(mask&2)!=0,callback=(mask&4)!=0;yes(AccessibilityStateRules.connected(present,current,callback)==(mask==7));}
  yes("enabled_waiting".equals(AccessibilityStateRules.state(true,true,true,true,false)));yes("disabled".equals(AccessibilityStateRules.state(true,true,true,false,false)));yes("unknown".equals(AccessibilityStateRules.state(true,true,false,false,false)));yes("missing".equals(AccessibilityStateRules.state(true,false,true,true,false)));yes("connected".equals(AccessibilityStateRules.state(false,false,false,false,true)));
  yes(!AccessibilityStateRules.marker(true,true,true,false,false).equals(AccessibilityStateRules.marker(true,true,true,true,false)));yes(!AccessibilityStateRules.marker(true,true,true,true,false).equals(AccessibilityStateRules.marker(true,true,true,true,true)));yes(!AccessibilityStateRules.marker(true,true,false,false,false).equals(AccessibilityStateRules.marker(true,true,true,false,false)));
  yes(!AccessibilityStateRules.status("enabled_waiting").contains("未启用"));yes(!AccessibilityStateRules.status("unknown").contains("未启用"));yes(!AccessibilityStateRules.status("connected").contains("已就绪"));yes(AccessibilityStateRules.status("unknown").equals(AccessibilityStateRules.status("invalid")));
  System.out.println("AccessibilityStateRulesTest: "+checks+" checks passed");
 }
}
