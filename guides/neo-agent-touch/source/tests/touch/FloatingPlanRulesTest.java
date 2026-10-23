package studio.jiayu.neoagent;

final class FloatingPlanRulesTest {
 static int checks;
 static void yes(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 public static void main(String[] args){
  FloatingPlanRules rules=new FloatingPlanRules();
  yes(!rules.ownsPlanning()&&rules.phase()==FloatingPlanRules.Phase.IDLE,"Initial ownership");
  yes(rules.begin("")==-1,"Empty session began");yes(rules.begin(null)==-1,"Null session began");
  long first=rules.begin("chat-one");yes(first>0&&rules.ownsPlanning(),"Planning not owned");
  yes(rules.planning(first,"chat-one"),"Current callback lost");yes(!rules.planning(first,"chat-two"),"Cross-chat callback accepted");
  yes(!rules.ready(first,"chat-two"),"Cross-chat plan accepted");yes(!rules.pending(first,"chat-one"),"Unvalidated plan became pending");
  yes(!rules.committed(first,"chat-one"),"Unvalidated plan committed");
  yes(rules.ready(first,"chat-one"),"Validated plan lost");yes(rules.ownsPlanning()&&rules.pending(first,"chat-one"),"Pending confirmation ownership lost");
  yes(!rules.planning(first,"chat-one"),"Late planner callback re-entered pending");yes(!rules.ready(first,"chat-one"),"Duplicate callback replaced pending");
  rules.cancel();yes(!rules.ownsPlanning()&&rules.phase()==FloatingPlanRules.Phase.IDLE,"Cancel retained planning");
  yes(!rules.pending(first,"chat-one")&&!rules.committed(first,"chat-one"),"Canceled plan committed");
  long second=rules.begin("chat-one");yes(second>first,"Request epoch did not advance");
  yes(!rules.ready(first,"chat-one"),"Canceled callback changed newer request");yes(rules.planning(second,"chat-one"),"Old callback damaged current request");
  yes(rules.ready(second,"chat-one"),"Second validated plan lost");yes(rules.committed(second,"chat-one"),"Pending commit rejected");
  yes(!rules.ownsPlanning()&&rules.phase()==FloatingPlanRules.Phase.COMMITTED,"Committed task still owns a planner");
  yes(!rules.committed(second,"chat-one"),"Double start allowed");yes(!rules.ready(second,"chat-one"),"Late result reopened committed task");
  long third=rules.begin("chat-two");yes(third>second&&rules.planning(third,"chat-two"),"Next chat did not get new lease");
  yes(!rules.planning(second,"chat-one")&&!rules.pending(second,"chat-one"),"Previous chat leaked into next request");
  rules.cancel();long fourth=rules.begin("chat-two");yes(fourth>third,"Cancellation did not retire request");
  yes(!rules.ready(third,"chat-two"),"Old same-chat result replaced latest request");
  rules.close();yes(rules.phase()==FloatingPlanRules.Phase.CLOSED&&!rules.ownsPlanning(),"Close left work owned");
  yes(!rules.planning(fourth,"chat-two")&&!rules.ready(fourth,"chat-two"),"Service-close callback accepted");
  yes(rules.begin("chat-two")==-1,"Disposed service engine reopened");rules.cancel();yes(rules.phase()==FloatingPlanRules.Phase.CLOSED,"Cancel reopened disposed engine");
  for(int i=0;i<20;i++){yes(!rules.committed(fourth-i,"chat-two"),"Late commit after close");}
  System.out.println("PASS: FloatingPlanRules "+checks+" session, cancel, pending and one-start checks");
 }
}
