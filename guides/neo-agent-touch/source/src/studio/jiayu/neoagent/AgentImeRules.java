package studio.jiayu.neoagent;

/** The same persisted switch/restore protocol is used by Android and by the pure port tests. */
final class AgentImeRules {
 static final class Ticket {
  final String id, original, own, run, phase;
  final long epoch;
  Ticket(String id,String original,String own,String run,long epoch,String phase){
   this.id=id;this.original=original;this.own=own;this.run=run;this.epoch=epoch;this.phase=phase;
  }
  Ticket phase(String value){return new Ticket(id,original,own,run,epoch,value);}
  boolean same(Ticket other){return other!=null&&id.equals(other.id)&&epoch==other.epoch&&run.equals(other.run);}
  boolean valid(){return !id.isEmpty()&&!original.isEmpty()&&!own.isEmpty()&&!original.equals(own)&&!run.isEmpty()&&epoch>0&&("switching".equals(phase)||"held".equals(phase)||"restoring".equals(phase));}
 }
 interface Port {
  String current()throws Exception;
  boolean enabled(String ime)throws Exception;
  Ticket load()throws Exception;
  boolean save(Ticket ticket)throws Exception;
  boolean clear(Ticket expected)throws Exception;
  String blockedRun()throws Exception;
  boolean blockRun(String run)throws Exception;
  boolean validOwner(long epoch,String run)throws Exception;
  boolean switchTo(String ime)throws Exception;
  String newId();
 }
 final Port port;
 final String own;
 volatile Ticket active;
 volatile String state="idle";
 private String restoreRequested="";
 AgentImeRules(String own,Port port){this.own=own;this.port=port;}
 private boolean save(Ticket value)throws Exception{
  if(!value.valid()||!port.save(value)){state="record_save_failed";return false;}
  Ticket read=port.load();if(!value.same(read)||!value.phase.equals(read.phase)){state="record_readback_failed";return false;}
  active=value;return true;
 }
 private boolean clear(Ticket expected,String result)throws Exception{
  if(!port.clear(expected)){state="record_clear_failed";return false;}
  if(expected.same(port.load())){state="record_clear_readback_failed";return false;}
  if(expected.same(active))active=null;
  restoreRequested="";state=result;return true;
 }
 private boolean manual(Ticket ticket)throws Exception{
  // Persist the opt-out before removing the recovery record; a stage epoch is not a new run.
  if(!port.blockRun(ticket.run)||!ticket.run.equals(port.blockedRun())){state="manual_choice_not_saved";return false;}
  return clear(ticket,"manual_choice_respected");
 }
 boolean acquire(long epoch,String run)throws Exception{
  if(epoch<=0||run==null||run.isEmpty()){state="stale_owner";return false;}
  if(!port.validOwner(epoch,run)){state="stale_owner";return false;}
  if(run.equals(port.blockedRun())){state="manual_choice_respected";return false;}
  if(!port.enabled(own)){state="enable_in_settings";return false;}
  Ticket saved=port.load();String current=port.current();
  if(saved!=null){
   if(!saved.valid()||!own.equals(saved.own)){state="invalid_recovery_record";return false;}
   if(active!=null&&active.same(saved)&&!"restoring".equals(saved.phase)&&run.equals(saved.run)){
    if(!own.equals(current)&&!"switching".equals(saved.phase)){manual(saved);return false;}
    if(saved.epoch==epoch)return poll(epoch,run);
    // A live workflow may move to a fresh epoch without changing the original keyboard.
    if(!own.equals(current)){state="switch_not_confirmed";return false;}
    Ticket moved=new Ticket(port.newId(),saved.original,own,run,epoch,"held");
    if(!port.validOwner(epoch,run)||!save(moved))return false;
    state="held";return poll(epoch,run);
   }
   state="recovery_required";return false; // Never adopt a persisted orphan as a live lease.
  }
  if(current.isEmpty()){state="current_ime_unknown";return false;}
  if(own.equals(current)){state="own_ime_without_recovery_record";return false;}
  if(!port.enabled(current)){state="original_ime_not_enabled";return false;}
  Ticket ticket=new Ticket(port.newId(),current,own,run,epoch,"switching");
  if(!save(ticket))return false; // Durable recovery comes before the platform mutation.
  if(!port.validOwner(epoch,run)){state="stale_owner";return false;}
  if(!current.equals(port.current())){manual(ticket);return false;}
  if(!port.switchTo(own)){state="switch_rejected";return false;}
  state="switch_pending";return poll(epoch,run);
 }
 boolean poll(long epoch,String run)throws Exception{
  Ticket saved=port.load();
  if(active==null||!active.same(saved)||saved.epoch!=epoch||!run.equals(saved.run)||"restoring".equals(saved.phase)||!port.validOwner(epoch,run))return false;
  String current=port.current();
  if(own.equals(current)){
   if(!"held".equals(saved.phase)&&!save(saved.phase("held")))return false;
   state="held";return true;
  }
  if(!current.isEmpty()&&(!current.equals(saved.original)||"held".equals(saved.phase)))manual(saved);
  return false;
 }
 boolean held(long epoch,String run)throws Exception{
  Ticket saved=port.load();return active!=null&&active.same(saved)&&saved.epoch==epoch&&run.equals(saved.run)
   &&"held".equals(saved.phase)&&port.validOwner(epoch,run)&&own.equals(port.current());
 }
 /** Captured ticket makes a queued old release harmless after a newer workflow lease. */
 boolean release(Ticket expected)throws Exception{
  Ticket saved=port.load();
  if(saved==null){state=own.equals(port.current())?"own_ime_without_recovery_record":"idle";return "idle".equals(state);}
  if(expected==null||!expected.same(saved)){state="stale_release_ignored";return false;}
  if(!saved.valid()||!own.equals(saved.own)){state="invalid_recovery_record";return false;}
  String current=port.current();
  if(current.isEmpty()){state="restore_current_unknown";return false;}
  if(!own.equals(current)){
   if(saved.original.equals(current)&&"restoring".equals(saved.phase))return clear(saved,"restored");
   // A failed, unconfirmed request left the original selected; there is nothing to replace.
   if(saved.original.equals(current)&&"switching".equals(saved.phase))return clear(saved,"original_unchanged");
   return manual(saved);
  }
  if(!port.enabled(saved.original)){state="restore_original_not_enabled";return false;}
  Ticket restoring=saved.phase("restoring");
  if(!"restoring".equals(saved.phase)&&!save(restoring))return false;
  if(!restoring.same(port.load())||!own.equals(port.current())){state="restore_boundary_changed";return false;}
  if(!restoreRequested.equals(restoring.id)){
   if(!port.switchTo(restoring.original)){state="restore_rejected";return false;}
   restoreRequested=restoring.id;
  }
  if(restoring.original.equals(port.current()))return clear(restoring,"restored");
  if(!own.equals(port.current())&&!port.current().isEmpty())return manual(restoring);
  state="restore_pending";return false;
 }
 boolean recover()throws Exception{Ticket saved=port.load();return release(saved);}
}
