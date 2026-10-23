package studio.jiayu.neoagent;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.lang.ref.WeakReference;
import java.util.UUID;
import org.json.JSONObject;

/** Phone-local IME selection lease, separate from the keyboard visibility lease. */
final class AgentImeLease {
 private static final String PREFS="agent-ime-lease-v1",RECORD="recovery",BLOCKED="manual_run";
 private static final Object STORE=new Object();
 private static WeakReference<AgentImeLease> latest=new WeakReference<>(null);
 private final ControlService owner;
 private final Handler main=new Handler(Looper.getMainLooper());
 private volatile AgentImeRules rules;
 private boolean watching;
 private final ContentObserver observer=new ContentObserver(main){@Override public void onChange(boolean self){
  if(rules==null)return;AgentImeRules.Ticket ticket=rules.active;if(ticket==null)return;
  try{if("restoring".equals(ticket.phase))rules.release(ticket);else rules.poll(ticket.epoch,ticket.run);}catch(Exception failed){rules.state="ime_observation_failed";}
 }};
 AgentImeLease(ControlService owner){this.owner=owner;latest=new WeakReference<>(this);}
 private synchronized AgentImeRules protocol(){if(rules==null)rules=new AgentImeRules(imeId(owner),new AndroidPort(owner,owner));return rules;}
 static String imeId(Context context){return new ComponentName(context,AgentInputMethodService.class).flattenToShortString();}
 private static String canonical(String value){ComponentName component=ComponentName.unflattenFromString(value==null?"":value);return component==null?"":component.flattenToShortString();}
 private static String current(Context context){try{return canonical(Settings.Secure.getString(context.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD));}catch(RuntimeException denied){return "";}}
 static boolean enabled(Context context){return enabled(context,imeId(context));}
 private static boolean enabled(Context context,String id){
  if(id==null||id.isEmpty())return false;
  try{InputMethodManager manager=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);if(manager==null)return false;for(InputMethodInfo info:manager.getEnabledInputMethodList())if(id.equals(canonical(info.getId())))return true;}catch(RuntimeException denied){}
  return false;
 }
 private static SharedPreferences preferences(Context context){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 private static AgentImeRules.Ticket read(Context context)throws Exception{
  synchronized(STORE){String raw=preferences(context).getString(RECORD,"");if(raw.isEmpty())return null;JSONObject j=new JSONObject(raw);AgentImeRules.Ticket ticket=new AgentImeRules.Ticket(j.getString("id"),j.getString("original"),j.getString("own"),j.getString("run"),j.getLong("epoch"),j.getString("phase"));if(!ticket.valid())throw new IllegalStateException("Invalid IME recovery record");return ticket;}
 }
 private static final class AndroidPort implements AgentImeRules.Port {
  final Context context;final ControlService owner;
  AndroidPort(Context context,ControlService owner){this.context=context;this.owner=owner;}
  public String current(){return AgentImeLease.current(context);}
  public boolean enabled(String id){return AgentImeLease.enabled(context,id);}
  public AgentImeRules.Ticket load()throws Exception{return read(context);}
  public boolean save(AgentImeRules.Ticket t)throws Exception{synchronized(STORE){JSONObject j=new JSONObject().put("id",t.id).put("original",t.original).put("own",t.own).put("run",t.run).put("epoch",t.epoch).put("phase",t.phase);return preferences(context).edit().putString(RECORD,j.toString()).commit();}}
  public boolean clear(AgentImeRules.Ticket expected)throws Exception{synchronized(STORE){AgentImeRules.Ticket now=read(context);return now==null||expected.same(now)&&preferences(context).edit().remove(RECORD).commit();}}
  public String blockedRun(){synchronized(STORE){return preferences(context).getString(BLOCKED,"");}}
  public boolean blockRun(String run){synchronized(STORE){return preferences(context).edit().putString(BLOCKED,run).commit();}}
  public boolean validOwner(long epoch,String run){if(owner==null)return false;synchronized(owner){return owner==ControlService.instance&&owner.connected&&owner.running&&owner.generation==epoch&&owner.ownership.accepts(epoch)&&run.equals(owner.runId);}}
  public boolean switchTo(String id){
   if(Build.VERSION.SDK_INT<30||!enabled(id))return false;
   if(owner!=null&&owner.connected&&owner==ControlService.instance)return owner.getSoftKeyboardController().switchToInputMethod(id);
   // A selected IME may restore with its own live framework token after accessibility disconnects.
   InputMethodService selected=context instanceof InputMethodService?(InputMethodService)context:AgentInputMethodService.instance;
   if(selected!=null&&imeId(context).equals(current())){selected.switchInputMethod(id);return true;}
   return false;
  }
  public String newId(){return UUID.randomUUID().toString();}
 }
 private void watchUi(){if(watching)return;owner.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),false,observer);watching=true;}
 boolean acquire(long generation)throws Exception{
  final String run;synchronized(owner){owner.check(generation);run=owner.runId;}
  final AgentImeRules rules=protocol();boolean held=owner.ui(()->{watchUi();return rules.acquire(generation,run);});
  if(held)return true;
  AgentImeRules.Ticket ticket=rules.active;
  if(ticket==null||ticket.epoch!=generation||!run.equals(ticket.run))return false;
  if(Looper.myLooper()!=Looper.getMainLooper()){
   long until=SystemClock.elapsedRealtime()+1800;
   while(SystemClock.elapsedRealtime()<until){Thread.sleep(60);synchronized(owner){if(!owner.running||owner.generation!=generation)break;}if(owner.ui(()->rules.poll(generation,run)))return true;if(rules.active==null)break;}
  }
  releaseAsync(ticket);return false;
 }
 boolean held(long generation){try{final String run;synchronized(owner){run=owner.runId;}return protocol().held(generation,run);}catch(Exception failed){return false;}}
 boolean releaseUi(){final AgentImeRules rules=protocol();try{AgentImeRules.Ticket ticket=rules.active;if(ticket==null)ticket=read(owner);return rules.release(ticket);}catch(Exception failed){rules.state="restore_failed";return false;}}
 void releaseAsync(){final AgentImeRules rules=protocol();AgentImeRules.Ticket ticket=rules.active;try{if(ticket==null)ticket=read(owner);}catch(Exception failed){rules.state="record_read_failed";return;}releaseAsync(ticket);}
 private void releaseAsync(AgentImeRules.Ticket ticket){if(ticket==null)return;final AgentImeRules rules=protocol();final long deadline=SystemClock.elapsedRealtime()+1800;main.post(new Runnable(){public void run(){try{if(rules.release(ticket))return;if("restore_pending".equals(rules.state)&&SystemClock.elapsedRealtime()<deadline)main.postDelayed(this,60);}catch(Exception failed){rules.state="restore_failed";}}});}
 boolean recoverUi(){final AgentImeRules rules=protocol();try{watchUi();return rules.recover();}catch(Exception failed){rules.state="recovery_failed";return false;}}
 private static boolean liveOwner(AgentImeLease live){
  if(live==null||live.rules==null||live.owner!=ControlService.instance||!live.owner.connected)return false;
  synchronized(live.owner){AgentImeRules.Ticket active=live.rules.active;return active!=null&&!"restoring".equals(active.phase)&&live.owner.running&&live.owner.generation==active.epoch&&active.run.equals(live.owner.runId)&&live.owner.ownership.accepts(active.epoch);}
 }
 /** Safe for IME callbacks: an active current lease is not an orphan. Never acquire here. */
 static boolean recoverUi(Context context){
  if(Looper.myLooper()!=Looper.getMainLooper())return false;
  AgentImeLease live=latest.get();
  if(liveOwner(live))return true;
  if(live!=null&&live.owner==ControlService.instance&&live.owner.connected){
   return live.recoverUi();
  }
  try{return new AgentImeRules(imeId(context),new AndroidPort(context,null)).recover();}catch(Exception failed){return false;}
 }
 static void recoverAsync(Context context){
  final AgentImeRules.Ticket expected;try{expected=read(context);}catch(Exception failed){return;}if(expected==null)return;
  final AgentImeRules fallback=new AgentImeRules(imeId(context),new AndroidPort(context,null));
  final Handler handler=new Handler(Looper.getMainLooper());final long until=SystemClock.elapsedRealtime()+1800;
  handler.post(new Runnable(){public void run(){try{if(!expected.same(read(context)))return;AgentImeLease live=latest.get();if(liveOwner(live))return;boolean done=live!=null&&live.owner==ControlService.instance&&live.owner.connected?live.protocol().release(expected):fallback.release(expected);if(!done&&SystemClock.elapsedRealtime()<until)handler.postDelayed(this,80);}catch(Exception failed){/* Durable record remains for reconnect/manual recovery. */}}});
 }
 void close(){releaseAsync();main.post(()->{if(watching){try{owner.getContentResolver().unregisterContentObserver(observer);}catch(RuntimeException ignored){}watching=false;}});}
 JSONObject status(){JSONObject result=new JSONObject();try{AgentImeRules.Ticket saved=read(owner);boolean active=held(owner.generation),opted=owner.runId.equals(preferences(owner).getString(BLOCKED,""))&&!owner.runId.isEmpty();result.put("enabled",enabled(owner)).put("active",active).put("selected",imeId(owner).equals(current(owner))).put("held",active).put("restore_pending",saved!=null&&!active).put("recovery_record_present",saved!=null).put("opted_out_this_run",opted).put("restore_original_enabled",saved!=null&&enabled(owner,saved.original)).put("state",protocol().state).put("requires_manual_enable",!enabled(owner)).put("manual_choice_respected",opted);}catch(Exception failed){try{result.put("enabled",false).put("active",false).put("restore_pending",true).put("opted_out_this_run",false).put("state","status_unavailable").put("restore_verified",false);}catch(Exception ignored){}}return result;}
}
