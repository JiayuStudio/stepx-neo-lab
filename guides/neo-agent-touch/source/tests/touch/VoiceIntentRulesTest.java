package studio.jiayu.neoagent;

final class VoiceIntentRulesTest {
 static int checks;
 static void yes(boolean value){if(!value)throw new AssertionError("voice intent check "+(checks+1));checks++;}
 public static void main(String[] args){
  VoiceIntentRules rules=new VoiceIntentRules();VoiceIntentRules.Ticket first=rules.begin("a","chat",3,true);
  yes(rules.owns(first,"a","chat"));yes(first.holdsGoal);yes(!rules.owns(first,"wrong","chat"));yes(!rules.owns(first,"a","other"));
  yes(rules.result(first,"a","chat",2,true,true,"公开目标")==VoiceIntentRules.Final.REJECT);
  yes(rules.result(first,"a","chat",3,true,true,"公开目标")==VoiceIntentRules.Final.SEND);
  yes(rules.result(first,"a","chat",3,true,true,"重复目标")==VoiceIntentRules.Final.REJECT);
  rules.cancelCapture();yes(rules.canPlan(first,"chat",3));yes(!rules.planning(first,"other",3,4));yes(!rules.planning(first,"chat",3,3));yes(rules.planning(first,"chat",3,4));
  yes(!rules.planningCurrent(first,"chat",3));yes(rules.planningCurrent(first,"chat",4));yes(!rules.mayCommit(first,"chat",4));yes(rules.ready(first,"chat",4));yes(!rules.ready(first,"chat",4));yes(rules.mayCommit(first,"chat",4));yes(rules.committed(first,"chat",4));yes(!rules.committed(first,"chat",4));
  VoiceIntentRules.Ticket second=rules.begin("b","chat",5,true);yes(!rules.cancel("a"));yes(rules.owns(second,"b","chat"));yes(!rules.planningCurrent(first,"chat",4));
  yes(rules.result(second,"b","chat",5,true,false,"中断只保留草稿")==VoiceIntentRules.Final.DRAFT);yes(!rules.canPlan(second,"chat",5));yes(!rules.manualDraft(second,"other",5));yes(!rules.manualDraft(second,"chat",6));yes(rules.manualDraft(second,"chat",5));yes(rules.planning(second,"chat",5,6));yes(rules.cancel("b"));yes(!rules.ready(second,"chat",6));yes(!rules.cancel("b"));
  VoiceIntentRules.Ticket third=rules.begin("c","chat",7,false);yes(!third.holdsGoal);rules.cancelCapture();yes(rules.result(third,"c","chat",7,true,true,"取消后目标")==VoiceIntentRules.Final.REJECT);
  VoiceIntentRules.Ticket background=rules.begin("d","chat",8,true);yes(rules.result(background,"d","chat",8,false,true,"后台结果")==VoiceIntentRules.Final.REJECT);yes(!rules.canPlan(background,"chat",8));
  VoiceIntentRules.Ticket empty=rules.begin("e","chat",9,true);yes(rules.result(empty,"e","chat",9,true,true,"  ")==VoiceIntentRules.Final.REJECT);
  VoiceIntentRules.Ticket older=rules.begin("f","chat",10,true),newer=rules.begin("g","chat",11,true);yes(older.phase()==VoiceIntentRules.Phase.CANCELLED);rules.abandon(older);yes(rules.owns(newer,"g","chat"));yes(rules.result(older,"f","chat",10,true,true,"旧结果")==VoiceIntentRules.Final.REJECT);
  yes(rules.result(newer,"g","chat",11,true,true,"新目标")==VoiceIntentRules.Final.SEND);yes(rules.planning(newer,"chat",11,12));rules.abandon(newer);yes(!rules.ready(newer,"chat",12));yes(!rules.mayCommit(newer,"chat",12));
  VoiceIntentRules.Ticket retry=rules.begin("h","chat",13,true);yes(rules.result(retry,"h","chat",13,true,true,"新目标")==VoiceIntentRules.Final.SEND);yes(rules.draft(retry));yes(rules.manualDraft(retry,"chat",13));yes(rules.planning(retry,"chat",13,14));yes(!rules.draft(retry));
  VoiceIntentRules.Ticket keyboard=rules.begin("keyboard","chat",15,true);yes(!rules.keyboardDraft(keyboard,"other",15));yes(!rules.keyboardDraft(keyboard,"chat",14));yes(rules.keyboardDraft(keyboard,"chat",15));rules.cancelCapture();yes(keyboard.phase()==VoiceIntentRules.Phase.DRAFT);yes(!rules.canPlan(keyboard,"chat",15));yes(rules.manualDraft(keyboard,"chat",15));yes(!rules.manualDraft(keyboard,"chat",15));yes(rules.planning(keyboard,"chat",15,16));yes(!rules.keyboardDraft(keyboard,"chat",15));rules.abandon(keyboard);yes(!rules.ready(keyboard,"chat",16));
  System.out.println("VoiceIntentRulesTest "+checks+" checks passed");
 }
}
