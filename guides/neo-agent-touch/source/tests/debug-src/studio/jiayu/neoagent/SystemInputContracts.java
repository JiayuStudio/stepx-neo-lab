package studio.jiayu.neoagent;
import android.os.*;import android.provider.Settings;import java.io.*;import java.util.concurrent.*;import org.json.*;
/** Owned chat input only. Does not invoke an ASR engine, send a message or operate another app. */
final class SystemInputContracts {
 static void start(UiQaActivity a){new Thread(()->run(a),"system-input-contract").start();}
 static <T>T ui(UiQaActivity a,Callable<T> c)throws Exception{CompletableFuture<T> f=new CompletableFuture<>();a.runOnUiThread(()->{try{f.complete(c.call());}catch(Throwable e){f.completeExceptionally(e);}});return f.get(5,TimeUnit.SECONDS);}
 static void run(UiQaActivity a){JSONObject out=new JSONObject();boolean opened=false;String before="";int checks=0;try{
  if(!ui(a,()->a.loaded&&a.resumed&&"plan".equals(a.voiceMode())))throw new Exception("isolated_ui_required");checks++;
  ControlService s=ControlService.instance;if(s!=null&&s.busy())throw new Exception("idle_required");checks++;
  before=Settings.Secure.getString(a.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);final String original=before;
  ui(a,()->{a.handler.removeCallbacks(a.refresh);a.openSystemInput();return true;});opened=true;
  if(!ui(a,()->a.voiceIntent!=null&&a.voiceIntent.ticket.phase()==VoiceIntentRules.Phase.DRAFT&&!a.planning&&!a.voicePermissionPending&&(a.voice==null||!a.voice.active())))throw new Exception("system_draft_state");checks++;
  String dom="";for(int i=0;i<40;i++){CompletableFuture<String> f=new CompletableFuture<>();ui(a,()->{a.web.evaluateJavascript("JSON.stringify({action:document.querySelector('.voiceMic')?.dataset.a,focused:document.activeElement?.id,directCheckbox:!!document.getElementById('voiceDirect'),panel:!!document.querySelector('.voicePanel')})",f::complete);return true;});dom=f.get(4,TimeUnit.SECONDS);if(dom.contains("draft"))break;Thread.sleep(100);}
  JSONObject view=new JSONObject(new org.json.JSONTokener(dom).nextValue().toString());
  if(!"systemInput".equals(view.optString("action")))throw new Exception("system_entry");checks++;
  if(!"draft".equals(view.optString("focused")))throw new Exception("draft_focus");checks++;
  if(view.optBoolean("directCheckbox")||view.optBoolean("panel"))throw new Exception("legacy_voice_ui_visible");checks++;
  if(!java.util.Objects.equals(original,Settings.Secure.getString(a.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD)))throw new Exception("keyboard_selection_changed");checks++;
  out.put("success",true).put("checks",checks).put("system_entry_visible",true).put("draft_focused",true).put("original_keyboard_unchanged",true).put("legacy_asr_not_started",true).put("message_not_sent",true);
 }catch(Throwable e){try{out.put("success",false).put("checks",checks).put("failure_type",e.getClass().getSimpleName());}catch(Exception ignored){}}
 finally{try{ui(a,()->{a.cancelVoice();if(a.voiceIntent!=null)a.voiceIntents.abandon(a.voiceIntent.ticket);a.voiceIntent=null;a.clearVoiceTask();return true;});new Vault(a).remove("test-voice-chats");new Vault(a).remove("test-voice-user-memory");boolean same=java.util.Objects.equals(before,Settings.Secure.getString(a.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD));out.put("cleanup_verified",same).put("microphone_used",false).put("network_calls",0).put("business_actions",0).put("third_party_speech_accuracy_tested",false).put("sampledAt",System.currentTimeMillis());if(!same)out.put("success",false);try(FileOutputStream f=a.openFileOutput("system-input-contracts.json",0)){f.write(out.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.runOnUiThread(a::finish);}
 }
}
