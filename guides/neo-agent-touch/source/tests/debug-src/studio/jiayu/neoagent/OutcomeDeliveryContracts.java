package studio.jiayu.neoagent;
import android.content.*;import android.content.pm.ApplicationInfo;import org.json.*;import java.io.*;import java.util.*;import java.util.concurrent.*;

/** Isolated encrypted-store outcome delivery; never starts, pauses, stops or fabricates a device lease. */
final class OutcomeDeliveryContracts {
 final UiQaActivity a;int checks;String oldOutcome,oldChats,oldMarker,oldSession,oldSessions;boolean hadOutcome,hadChats,hadMarker,oldResumed,oldReject,captured;
 ControlService service;long generation;String nativeRun;boolean busy;final JSONObject report=new JSONObject();
 OutcomeDeliveryContracts(UiQaActivity activity){a=activity;}
 static void run(UiQaActivity a){new Thread(()->new OutcomeDeliveryContracts(a).run(),"outcome-delivery-contracts").start();}
 <T>T ui(Callable<T> task)throws Exception{CompletableFuture<T> value=new CompletableFuture<>();a.runOnUiThread(()->{try{value.complete(task.call());}catch(Throwable failure){value.completeExceptionally(failure);}});return value.get(5,TimeUnit.SECONDS);}
 void yes(boolean value,String why)throws Exception{if(!value)throw new Exception(why);checks++;}
 JSONObject outcome(String event,String owner,String text)throws Exception{return new JSONObject().put("event",event).put("run","local-outcome-contract").put("session",owner).put("status",text).put("detail","");}
 int countEvent(String event)throws Exception{int count=0;JSONArray messages=a.chat().getJSONArray("messages");for(int i=0;i<messages.length();i++)if(event.equals(messages.getJSONObject(i).optString("outcome_event")))count++;return count;}
 void restore(Vault vault,String key,boolean existed,String value)throws Exception{if(existed)vault.put(key,value);else vault.remove(key);}
 void run(){Vault vault=new Vault(a);SharedPreferences prefs=a.getSharedPreferences("ui",0);try{
  yes((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"Debug fixture only");
  yes(ui(()->a.loaded&&a.resumed&&"plan".equals(a.voiceMode())&&a.voiceTest()&&"test-voice-chats".equals(a.chatStorageKey())&&!a.planning),"Fresh isolated outcome UI required");
  yes(ui(()->!a.handler.hasCallbacks(a.refresh)),"Isolated activity unexpectedly polls outcomes");
  hadOutcome=vault.has("last-outcome");oldOutcome=vault.get("last-outcome");hadChats=vault.has(a.chatStorageKey());oldChats=vault.get(a.chatStorageKey());hadMarker=prefs.contains("deliveredRun");oldMarker=prefs.getString("deliveredRun","");
  ui(()->{oldSessions=a.sessions.toString();oldSession=a.sessionId;oldResumed=a.resumed;oldReject=a.rejectOutcomeChatSave;service=ControlService.instance;if(service!=null)synchronized(service){generation=service.generation;nativeRun=service.runId;busy=service.busy();}return true;});captured=true;
  // Test the production foreground guard before exposing any synthetic global outcome.
  yes(ui(()->{a.resumed=false;try{return !a.deliverOutcome();}finally{a.resumed=oldResumed;}}),"Background outcome guard failed before fixture data");
  yes(prefs.contains("deliveredRun")==hadMarker&&oldMarker.equals(prefs.getString("deliveredRun",""))&&oldOutcome.equals(vault.get("last-outcome")),"Background guard changed original delivery state");
  final String owner="outcome-fixture-"+UUID.randomUUID(),prior="prior-"+UUID.randomUUID(),foreign="foreign-"+UUID.randomUUID(),next="next-"+UUID.randomUUID();
  ui(()->{JSONArray messages=new JSONArray();for(int i=0;i<99;i++)messages.put(new JSONObject().put("role","assistant").put("text","隔离记录 "+i).put("time",i));messages.put(new JSONObject().put("role","assistant").put("text","隔离旧结果").put("time",99).put("outcome_event",prior));a.sessions=new JSONArray().put(new JSONObject().put("id",owner).put("title","隔离结果送达").put("messages",messages));a.sessionId=owner;a.rejectOutcomeChatSave=false;a.saveChats();return true;});
  String baseline=vault.get(a.chatStorageKey());yes(!baseline.isEmpty(),"Isolated baseline not persisted");yes(prefs.edit().putString("deliveredRun",prior).commit(),"Cannot seed isolated marker");
  vault.put("last-outcome",outcome(foreign,"not-a-loaded-owner","隔离未知会话结果").toString());
  yes(ui(()->{a.resumed=false;try{return !a.deliverOutcome();}finally{a.resumed=oldResumed;}}),"Background foreign outcome was delivered");
  yes(prior.equals(prefs.getString("deliveredRun",""))&&baseline.equals(vault.get(a.chatStorageKey())),"Background foreign outcome consumed marker or chat");
  yes(ui(()->!a.deliverOutcome()),"Foreground unknown owner was acknowledged");yes(prior.equals(prefs.getString("deliveredRun",""))&&baseline.equals(vault.get(a.chatStorageKey())),"Unknown owner changed durable delivery state");
  vault.put("last-outcome",outcome(prior,owner,"隔离旧结果").toString());yes(ui(()->!a.deliverOutcome()&&countEvent(prior)==1),"Restoring previously delivered outcome duplicated it");
  vault.put("last-outcome",outcome(next,owner,"隔离新结果").toString());
  ui(()->{a.rejectOutcomeChatSave=true;return true;});
  for(int attempt=0;attempt<2;attempt++){yes(ui(()->!a.deliverOutcome()),"Rejected chat save acknowledged delivery");yes(ui(()->countEvent(next)==0&&a.chat().getJSONArray("messages").length()==100&&a.chat().getJSONArray("messages").getJSONObject(0).getString("text").equals("隔离记录 0")),"Failed delivery changed or repeatedly appended in-memory history");yes(prior.equals(prefs.getString("deliveredRun",""))&&baseline.equals(vault.get(a.chatStorageKey())),"Rejected chat save changed marker or durable chat");}
  ui(()->{a.rejectOutcomeChatSave=false;return true;});yes(ui(a::deliverOutcome),"Valid owner outcome did not persist and acknowledge");
  yes(ui(()->countEvent(next)==1&&a.chat().getJSONArray("messages").length()==100&&a.chat().getJSONArray("messages").getJSONObject(0).getString("text").equals("隔离记录 1")),"Recovery duplicated outcome or trimmed history twice");
  yes(next.equals(prefs.getString("deliveredRun",""))&&ui(()->a.persistedOutcome(owner,next)!=null),"Successful outcome missing durable event or marker");yes(ui(()->!a.deliverOutcome()&&countEvent(next)==1),"Same event was appended again");
  // Model the crash window after chat commit but before marker commit, including a new chat snapshot read.
  yes(prefs.edit().remove("deliveredRun").commit(),"Cannot clear fixture marker");String committed=vault.get(a.chatStorageKey());
  ui(()->{JSONObject saved=new JSONObject(committed);a.sessions=saved.getJSONArray("sessions");a.sessionId=saved.getString("current");return true;});
  yes(ui(a::deliverOutcome),"Persisted event did not repair missing marker");yes(ui(()->countEvent(next)==1)&&committed.equals(vault.get(a.chatStorageKey()))&&next.equals(prefs.getString("deliveredRun","")),"Marker recovery appended an already durable event");
  yes(ui(()->service==null?ControlService.instance==null:ControlService.instance==service&&service.generation==generation&&service.runId.equals(nativeRun)&&service.busy()==busy),"Fixture changed device control ownership");
  report.put("success",true).put("checks",checks).put("background_guard_checked_before_fixture_data",true).put("foreign_owner_rejected",true).put("restored_old_event_not_duplicated",true).put("two_rejected_saves_rolled_back",true).put("recovery_stores_one_message",true).put("persisted_event_repairs_marker_without_append",true);
 }catch(Throwable failure){try{report.put("success",false).put("checks",checks).put("failure_type",failure.getClass().getSimpleName()).put("failure",failure.getMessage());}catch(Exception ignored){}}
 finally{boolean clean=!captured;try{if(captured){ui(()->{a.rejectOutcomeChatSave=oldReject;a.sessions=new JSONArray(oldSessions);a.sessionId=oldSession;a.resumed=oldResumed;return true;});restore(vault,"last-outcome",hadOutcome,oldOutcome);restore(vault,a.chatStorageKey(),hadChats,oldChats);SharedPreferences.Editor edit=prefs.edit();if(hadMarker)edit.putString("deliveredRun",oldMarker);else edit.remove("deliveredRun");if(!edit.commit())throw new IOException("Original marker restoration failed");clean=vault.has("last-outcome")==hadOutcome&&oldOutcome.equals(vault.get("last-outcome"))&&vault.has(a.chatStorageKey())==hadChats&&oldChats.equals(vault.get(a.chatStorageKey()))&&prefs.contains("deliveredRun")==hadMarker&&oldMarker.equals(prefs.getString("deliveredRun",""))&&ui(()->a.resumed==oldResumed&&a.rejectOutcomeChatSave==oldReject&&a.sessions.toString().equals(oldSessions)&&a.sessionId.equals(oldSession)&&(service==null?ControlService.instance==null:ControlService.instance==service&&service.generation==generation&&service.runId.equals(nativeRun)&&service.busy()==busy));}}catch(Exception failure){clean=false;try{report.put("cleanup_failure_type",failure.getClass().getSimpleName());}catch(Exception ignored){}}
  try{report.put("cleanup_verified",clean).put("production_chat_write_path_used",false).put("network_calls",0).put("microphone_used",false).put("fake_lease_used",false).put("business_lease_touched",false).put("sampledAt",System.currentTimeMillis());if(!clean)report.put("success",false);try(FileOutputStream file=a.openFileOutput("outcome-delivery-contracts.json",0)){file.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.runOnUiThread(a::finish);
 }
 }
}
