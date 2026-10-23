package studio.jiayu.neoagent;

import java.util.*;

/** Drives the production lease protocol through a persistent fake system port, without Android. */
public final class AgentImeRulesTest {
 static int checks;
 static void yes(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static final String OWN="agent/IME",HUMAN="human/IME",OTHER="other/IME",RUN="run-A";
 static final class SystemPort implements AgentImeRules.Port {
  String current=HUMAN,run=RUN,blocked="";long epoch=1;int ids,switches,saves;
  Set<String> enabled=new HashSet<>(Arrays.asList(OWN,HUMAN,OTHER));
  AgentImeRules.Ticket disk;
  boolean owner=true,saveFails,clearFails,blockFails,rejectSwitch,deferSwitch,throwAfterSwitch,changeOwnerOnSave,changeImeOnSave;
  final List<String> events=new ArrayList<>();
  public String current(){return current;}
  public boolean enabled(String ime){return enabled.contains(ime);}
  public AgentImeRules.Ticket load(){return disk;}
  public boolean save(AgentImeRules.Ticket ticket){saves++;events.add("save:"+ticket.phase);if(saveFails)return false;disk=ticket;if(changeOwnerOnSave)owner=false;if(changeImeOnSave)current=OTHER;return true;}
  public boolean clear(AgentImeRules.Ticket expected){if(clearFails)return false;if(disk!=null&&!expected.same(disk))return false;disk=null;events.add("clear");return true;}
  public String blockedRun(){return blocked;}
  public boolean blockRun(String run){if(blockFails)return false;blocked=run;events.add("block");return true;}
  public boolean validOwner(long g,String request){return owner&&epoch==g&&run.equals(request);}
  public boolean switchTo(String ime)throws Exception{
   yes(disk!=null&&disk.valid(),"platform switch always has durable recovery record");
   yes(enabled.contains(ime),"platform switch never enables an IME");
   if(!OWN.equals(ime))yes("restoring".equals(disk.phase),"restore intent persisted before platform switch");
   switches++;events.add("switch:"+ime);if(rejectSwitch)return false;
   if(!deferSwitch)current=ime;if(throwAfterSwitch)throw new Exception("synthetic binder response loss");return true;
  }
  public String newId(){return "ticket-"+(++ids);}
  AgentImeRules lease(){return new AgentImeRules(OWN,this);}
 }
 public static void main(String[] args)throws Exception{
  SystemPort p=new SystemPort();p.enabled.remove(OWN);AgentImeRules lease=p.lease();
  yes(!lease.acquire(1,RUN)&&p.disk==null&&p.switches==0,"disabled Agent IME requires user settings without mutation");
  p.enabled.add(OWN);yes(lease.acquire(1,RUN)&&lease.held(1,RUN),"read-back selected IME establishes held lease");
  yes(p.events.indexOf("save:switching")<p.events.indexOf("switch:"+OWN),"original record is saved before selection changes");
  yes(HUMAN.equals(p.disk.original)&&"held".equals(p.disk.phase),"confirmed lease preserves original keyboard");
  AgentImeRules.Ticket first=p.disk;int calls=p.switches;
  yes(lease.acquire(1,RUN)&&p.switches==calls,"same lease does not switch again");
  p.epoch=2;yes(lease.acquire(2,RUN)&&HUMAN.equals(p.disk.original)&&!p.disk.id.equals(first.id),"workflow epoch transfer retains original and changes ticket");
  yes(!lease.release(first)&&OWN.equals(p.current)&&lease.held(2,RUN)&&p.switches==calls,"queued old release cannot restore newer epoch");
  yes(lease.release(p.disk)&&HUMAN.equals(p.current)&&p.disk==null,"release verifies original selection then clears record");

  SystemPort delayed=new SystemPort();delayed.deferSwitch=true;AgentImeRules wait=delayed.lease();
  yes(!wait.acquire(1,RUN)&&!wait.held(1,RUN)&&delayed.disk!=null,"platform true without selection readback is not held");
  delayed.current=OWN;yes(wait.poll(1,RUN)&&wait.held(1,RUN)&&delayed.switches==1,"late selection readback establishes lease without replay");
  AgentImeRules.Ticket delayedTicket=delayed.disk;
  yes(!wait.release(delayedTicket)&&delayed.disk!=null&&"restore_pending".equals(wait.state),"accepted restore without readback remains pending");
  calls=delayed.switches;yes(!wait.release(delayedTicket)&&delayed.switches==calls,"pending restore observation does not repeat native switch");
  delayed.current=HUMAN;yes(wait.release(delayedTicket)&&delayed.disk==null,"later restore readback clears durable recovery");

  SystemPort failedSave=new SystemPort();failedSave.saveFails=true;
  yes(!failedSave.lease().acquire(1,RUN)&&failedSave.switches==0,"save failure prevents selection mutation");
  SystemPort rejected=new SystemPort();rejected.rejectSwitch=true;AgentImeRules rejectedLease=rejected.lease();
  yes(!rejectedLease.acquire(1,RUN)&&rejected.disk!=null,"native rejection is not success and recovery survives");
  yes(rejectedLease.release(rejected.disk)&&HUMAN.equals(rejected.current),"unchanged original after rejected acquire needs no switch");
  SystemPort unknown=new SystemPort();unknown.current="";yes(!unknown.lease().acquire(1,RUN)&&unknown.switches==0,"unknown current IME cannot be replaced");
  unknown.current=OWN;yes(!unknown.lease().acquire(1,RUN)&&unknown.disk==null,"own IME without original record is not fabricated into a lease");
  yes(!unknown.lease().recover(),"own selected without original record cannot claim restoration; user picker required");

  SystemPort manual=new SystemPort();AgentImeRules choice=manual.lease();yes(choice.acquire(1,RUN),"manual-choice setup acquires");
  manual.current=OTHER;calls=manual.switches;
  yes(!choice.poll(1,RUN)&&manual.disk==null&&RUN.equals(manual.blocked)&&manual.switches==calls,"manual other keyboard persists opt-out without forced restore");
  manual.epoch=2;yes(!choice.acquire(2,RUN)&&manual.switches==calls&&OTHER.equals(manual.current),"same run next App epoch cannot steal keyboard back");
  manual.run="run-B";yes(choice.acquire(2,"run-B")&&OTHER.equals(manual.disk.original),"new user run may acquire preserving new chosen keyboard");
  yes(choice.release(manual.disk)&&OTHER.equals(manual.current),"new run restores newly chosen original");

  SystemPort restart=new SystemPort();AgentImeRules beforeRestart=restart.lease();yes(beforeRestart.acquire(1,RUN),"restart setup acquires");
  AgentImeRules afterRestart=restart.lease();
  yes(!afterRestart.acquire(1,RUN)&&!afterRestart.held(1,RUN),"new service cannot adopt a persisted orphan as an active lease");
  yes(afterRestart.recover()&&HUMAN.equals(restart.current)&&restart.disk==null,"orphan recovers original from durable record");
  yes(afterRestart.recover(),"recovery without record is already complete");

  SystemPort disabledOriginal=new SystemPort();AgentImeRules missing=disabledOriginal.lease();yes(missing.acquire(1,RUN),"disabled-original setup acquires");
  disabledOriginal.enabled.remove(HUMAN);calls=disabledOriginal.switches;
  yes(!missing.release(disabledOriginal.disk)&&disabledOriginal.disk!=null&&disabledOriginal.switches==calls,"disabled original is never enabled or falsely restored");
  disabledOriginal.enabled.add(HUMAN);yes(disabledOriginal.lease().recover()&&HUMAN.equals(disabledOriginal.current),"reconnected recovery can use user-reenabled original");

  SystemPort changed=new SystemPort();changed.changeOwnerOnSave=true;
  yes(!changed.lease().acquire(1,RUN)&&changed.switches==0&&HUMAN.equals(changed.current),"owner lost after durable save cannot dispatch selection");
  SystemPort selectionRace=new SystemPort();selectionRace.changeImeOnSave=true;
  yes(!selectionRace.lease().acquire(1,RUN)&&selectionRace.switches==0&&OTHER.equals(selectionRace.current)&&RUN.equals(selectionRace.blocked),"manual selection between save and dispatch is respected");

  SystemPort lostResponse=new SystemPort();lostResponse.throwAfterSwitch=true;AgentImeRules loss=lostResponse.lease();boolean threw=false;
  try{loss.acquire(1,RUN);}catch(Exception expected){threw=true;}
  yes(threw&&OWN.equals(lostResponse.current)&&lostResponse.disk!=null,"exception after native switch retains recovery record");
  lostResponse.throwAfterSwitch=false;yes(lostResponse.lease().recover()&&HUMAN.equals(lostResponse.current),"restart can restore after lost native response");

  SystemPort failedClear=new SystemPort();AgentImeRules cleanup=failedClear.lease();yes(cleanup.acquire(1,RUN),"failed-clear setup acquires");
  failedClear.clearFails=true;yes(!cleanup.release(failedClear.disk)&&HUMAN.equals(failedClear.current)&&failedClear.disk!=null,"clear failure is reported despite actual original selection");
  failedClear.clearFails=false;yes(cleanup.release(failedClear.disk)&&failedClear.disk==null,"retry can confirm restored state and clear record");
  SystemPort unknownRestore=new SystemPort();AgentImeRules noCurrent=unknownRestore.lease();yes(noCurrent.acquire(1,RUN),"unknown-restore setup acquires");
  unknownRestore.current="";calls=unknownRestore.switches;
  yes(!noCurrent.release(unknownRestore.disk)&&unknownRestore.switches==calls&&unknownRestore.disk!=null,"unknown restore state is not guessed or mutated");
  System.out.println("PASS: AgentImeRules "+checks+" durable switch, exact ticket, workflow epoch, user choice and orphan recovery checks");
 }
}
