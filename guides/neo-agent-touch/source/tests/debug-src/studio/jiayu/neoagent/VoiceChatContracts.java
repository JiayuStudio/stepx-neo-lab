package studio.jiayu.neoagent;
import android.content.pm.ApplicationInfo;import org.json.*;import java.io.*;
/** Explicit synthetic final callback into the real chat planner; never reads a microphone. */
final class VoiceChatContracts {
 static final String DAILY="先打开企业微信查看工作台，然后打开高德地图查看从上海虹桥站到上海人民广场的公共交通路线；不打卡审批发送消息，不用当前位置、不打车不支付不接受新协议。";
 static void daily(UiQaActivity a){JSONObject report=source();try{
  if((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)throw new IllegalStateException("Debug fixture only");
  ControlService service=ControlService.instance;if(service==null||!service.connected||service.busy()||a.planning)throw new IllegalStateException("Runtime not idle");
  if(!a.resumed||!a.loaded||!"daily".equals(a.voiceMode())||a.voiceUiTest())throw new IllegalStateException("Fresh daily fixture UI not ready");
  a.newChat();a.chat().put("title","调试 · 合成语音多目标");a.saveChats();String session=a.sessionId;
  a.beginVoiceRequest();a.voiceOwnerSession=session;long before=a.turn;
  a.voiceResult(DAILY,true);
  report.put("success",a.turn!=before).put("pipeline","MainActivity.voiceResult -> MainActivity.sendMessage -> real Conversation.plan -> Android native executor").put("planner_submission_accepted",a.turn!=before).put("session_bound",session.equals(a.sessionId)).put("input",DAILY).put("memory_namespace",a.memoryKey()).put("chat_storage","test-voice-chats").put("production_memory_write_path_used",false).put("real_microphone_acceptance",false).put("task_completion_proven",false);
 }catch(Exception failed){try{report.put("success",false).put("failure_type",failed.getClass().getSimpleName()).put("failure",failed.getMessage());}catch(Exception ignored){}}
 write(a,"voice-chat-entry.json",report);}
 static JSONObject source(){JSONObject o=new JSONObject();try{o.put("source","synthetic final callback").put("microphone_used",false).put("microphone_permission_requested",false).put("audio_decoded_here",false).put("sampledAt",System.currentTimeMillis());}catch(Exception ignored){}return o;}
 static void write(UiQaActivity a,String file,JSONObject report){try(OutputStream out=a.openFileOutput(file,0)){out.write(report.toString(2).getBytes("UTF-8"));}catch(Exception ignored){}}
}
