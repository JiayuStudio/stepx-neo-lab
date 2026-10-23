package studio.jiayu.neoagent;

import android.app.Activity;import android.content.pm.ApplicationInfo;import java.io.*;import org.json.*;

/** Synthetic state events through the production guard; never constructs or reads AudioRecord. */
final class VoiceCaptureContracts {
 final JSONObject report=new JSONObject();final JSONArray passed=new JSONArray();int checks;
 static void start(Activity activity){new Thread(()->new VoiceCaptureContracts().run(activity),"voice-capture-contracts").start();}
 void yes(boolean value,String label)throws Exception{if(!value)throw new IllegalStateException(label);passed.put(label);checks++;}
 void interrupted(VoiceCaptureRules.Signal signal)throws Exception{
  VoiceSession session=new VoiceSession();long lease=session.begin(true);session.listening(lease);Object record=new Object();VoiceCaptureRules capture=new VoiceCaptureRules(lease,record);capture.start(lease,record,0);
  session.segment(lease,"先打开企业微信然后");yes(VoiceCaptureRules.forceDraft(session,capture,lease,record,signal),signal.name()+" event accepted by production helper");session.stop(lease);VoiceSession.Result result=session.finish(lease,true);
  yes(result!=null&&!result.direct&&result.text.equals("先打开企业微信然后"),signal.name()+" final is draft only");
 }
 void run(Activity activity){try{
  if((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)throw new IllegalStateException("Debug fixture only");
  interrupted(VoiceCaptureRules.Signal.SYSTEM_SILENCED);interrupted(VoiceCaptureRules.Signal.ROUTE_CHANGED);interrupted(VoiceCaptureRules.Signal.CONFIGURATION_LOST);interrupted(VoiceCaptureRules.Signal.MONITOR_UNAVAILABLE);interrupted(VoiceCaptureRules.Signal.READ_ERROR);interrupted(VoiceCaptureRules.Signal.INPUT_STALLED);
  VoiceSession session=new VoiceSession();long lease=session.begin(true);session.listening(lease);Object record=new Object();VoiceCaptureRules capture=new VoiceCaptureRules(lease,record);capture.start(lease,record,0);session.segment(lease,"打开高德地图");
  yes(!VoiceCaptureRules.forceDraft(session,capture,lease,new Object(),VoiceCaptureRules.Signal.SYSTEM_SILENCED),"foreign recorder ignored");
  session.cancel();long next=session.begin(true);session.listening(next);session.segment(next,"打开企业微信");yes(!VoiceCaptureRules.forceDraft(session,capture,lease,record,VoiceCaptureRules.Signal.SYSTEM_SILENCED),"stale callback cannot reach new lease");yes(session.finish(next,true).direct,"new healthy utterance still permits direct final");
  lease=session.begin(true);session.listening(lease);record=new Object();capture=new VoiceCaptureRules(lease,record);capture.start(lease,record,0);session.preview(lease,"部分识别不能执行");VoiceCaptureRules.forceDraft(session,capture,lease,record,VoiceCaptureRules.Signal.READ_ERROR);yes(session.finish(lease,true)==null,"partial ASR cannot become interrupted final");
  lease=session.begin(true);session.listening(lease);record=new Object();capture=new VoiceCaptureRules(lease,record);capture.start(lease,record,0);session.segment(lease,"打开微信");capture.configuration(lease,record,true,false,1,10);yes(capture.completion(lease,record)==VoiceCaptureRules.Signal.NONE,"normal stop has observed active capture");capture.end(lease,record);yes(!VoiceCaptureRules.forceDraft(session,capture,lease,record,VoiceCaptureRules.Signal.CONFIGURATION_LOST),"intentional stop disables late capture callback");yes(session.finish(lease,true).direct,"normal stop retains direct final");
  lease=session.begin(true);session.listening(lease);record=new Object();capture=new VoiceCaptureRules(lease,record);capture.start(lease,record,0);session.segment(lease,"打开微信");yes(VoiceCaptureRules.forceDraft(session,capture,lease,record,capture.completion(lease,record)),"early stop without active configuration is draft only");yes(!session.finish(lease,true).direct,"startup grace cannot bypass final capture check");
  report.put("success",true).put("stage","synthetic_capture_guard_contracts_passed");
 }catch(Throwable failure){try{report.put("success",false).put("stage","synthetic_capture_guard_failed").put("failure_type",failure.getClass().getSimpleName()).put("failure_check",failure.getMessage()==null?"":failure.getMessage());}catch(Exception ignored){}}
 finally{try{report.put("checks",checks).put("passed",passed).put("source","synthetic capture-state events").put("production_helper","VoiceCaptureRules.forceDraft").put("microphone_used",false).put("microphone_permission_requested",false).put("actual_audio_interruption_tested",false).put("audio_file_read",false).put("audio_file_written",false).put("network_calls",0).put("task_started",false).put("sampledAt",System.currentTimeMillis());try(OutputStream target=activity.openFileOutput("voice-capture-contracts.json",0)){target.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}activity.runOnUiThread(activity::finish);}}
}
