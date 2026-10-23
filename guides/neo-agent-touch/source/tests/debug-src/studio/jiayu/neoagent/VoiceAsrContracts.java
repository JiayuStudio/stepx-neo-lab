package studio.jiayu.neoagent;
import android.app.Activity;import android.content.pm.ApplicationInfo;import android.os.SystemClock;import java.io.*;import org.json.*;import org.vosk.*;
/** Debug only: bundled native model + optional public fixture PCM. Never reads microphone or starts tasks. */
final class VoiceAsrContracts {
 static void start(Activity activity){new Thread(()->run(activity),"voice-native-fixture").start();}
 static void run(Activity a){JSONObject report=new JSONObject();long began=SystemClock.elapsedRealtime();boolean ready=false,decoded=false;StringBuilder words=new StringBuilder();try{
  if((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)throw new IllegalStateException("Debug fixture only");
  File directory=OfflineVoice.unpack(a);try(Model model=new Model(directory.getAbsolutePath());Recognizer recognizer=new Recognizer(model,OfflineVoice.RATE)){
   ready=true;File pcm=new File(a.getFilesDir(),"qa-voice-input.pcm");report.put("pcm_present",pcm.isFile());if(pcm.isFile()){
    if(pcm.length()<2||pcm.length()>OfflineVoice.RATE*2L*OfflineVoice.MAX_SECONDS||(pcm.length()&1)!=0)throw new IllegalArgumentException("Fixture must be bounded mono PCM16LE 16kHz");
    report.put("pcm_bytes",pcm.length());try(InputStream stream=new FileInputStream(pcm)){byte[] frame=new byte[6400];int n;while((n=stream.read(frame))!=-1){if((n&1)!=0)throw new IllegalArgumentException("Odd PCM frame");if(recognizer.acceptWaveForm(frame,n))append(words,new JSONObject(recognizer.getResult()).optString("text"));}}append(words,new JSONObject(recognizer.getFinalResult()).optString("text"));decoded=words.length()>0;
   }
  }
  report.put("success",ready&&(!report.optBoolean("pcm_present")||decoded));
 }catch(Throwable failed){try{report.put("success",false).put("failure_type",failed.getClass().getSimpleName());}catch(Exception ignored){}}
 finally{try{report.put("model_ready",ready).put("decode_verified",decoded).put("decoded",VoiceSession.normalize(words.toString())).put("sampleRate",OfflineVoice.RATE).put("format","mono PCM16LE").put("microphone_used",false).put("microphone_permission_requested",false).put("network_calls",0).put("task_started",false).put("fixture_kind","public synthetic input; not live speech acceptance").put("elapsedMs",SystemClock.elapsedRealtime()-began).put("sampledAt",System.currentTimeMillis());try(OutputStream out=a.openFileOutput("voice-asr-contracts.json",0)){out.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.runOnUiThread(a::finish);}}
 static void append(StringBuilder target,String word){String value=VoiceSession.normalize(word);if(value.isEmpty())return;if(target.length()>0)target.append(' ');target.append(value);}
}
