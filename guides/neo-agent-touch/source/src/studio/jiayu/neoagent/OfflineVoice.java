package studio.jiayu.neoagent;
import android.Manifest;import android.app.Activity;import android.content.pm.PackageManager;import android.media.*;import android.os.*;import org.json.*;import org.vosk.Model;import org.vosk.Recognizer;import java.io.*;import java.nio.file.Files;import java.util.*;import java.util.concurrent.*;import java.util.zip.*;

/** Offline Chinese ASR. PCM is streamed only into bundled Vosk; no audio files or network. */
final class OfflineVoice {
 interface Listener {void state(JSONObject value);void result(String text,boolean direct);void error(String message);boolean foreground();}
 static final String MODEL="vosk-model-small-cn-0.22",ASSET="voice/"+MODEL+".zip";
 static final int RATE=16000,MAX_SECONDS=90;
 final Activity owner;final Listener listener;final Handler main=new Handler(Looper.getMainLooper());final ExecutorService worker=Executors.newSingleThreadExecutor();final VoiceSession session=new VoiceSession();final Object captureLock=new Object();
 volatile AudioRecord microphone;volatile VoiceCaptureRules captureRules;volatile boolean stopRequested,closed;volatile long token;Model model;
 OfflineVoice(Activity context,Listener callback){owner=context;listener=callback;}
 boolean active(){return session.state()!=VoiceSession.State.IDLE;}
 void start(boolean direct){if(closed||!listener.foreground())return;if(owner.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){listener.error("请先允许麦克风；音频只在手机内识别");return;}long lease=session.begin(direct);if(lease<0)return;token=lease;stopRequested=false;emit(lease,"preparing","首次准备离线中文模型…",0);worker.submit(()->capture(lease));}
 void stop(){synchronized(captureLock){long lease=token;AudioRecord recorder=microphone;VoiceCaptureRules rules=captureRules;if(recorder!=null&&rules!=null)completeCapture(lease,recorder,rules);if(session.stop(lease)){stopRequested=true;if(rules!=null)rules.end(lease,recorder);emit(lease,"finishing","正在整理最终识别结果…",0);stopMicrophone();}}}
 void cancel(){synchronized(captureLock){session.cancel();stopRequested=true;if(captureRules!=null)captureRules.end(token,microphone);stopMicrophone();}if(!closed)listener.state(state("idle","",0));}
 void stopMicrophone(){AudioRecord r=microphone;if(r!=null)try{r.stop();}catch(IllegalStateException ignored){}}
 void emit(long lease,String phase,String text,int level){main.post(()->{if(!session.current(lease)||closed)return;if("listening".equals(phase)&&session.state()!=VoiceSession.State.LISTENING)return;if("preparing".equals(phase)&&session.state()!=VoiceSession.State.PREPARING)return;listener.state(state(phase,text,level));});}
 static JSONObject state(String phase,String text,int level){JSONObject o=new JSONObject();try{o.put("phase",phase).put("text",text).put("level",level).put("engine","Vosk 离线中文").put("audio_upload",false);}catch(Exception ignored){}return o;}
 // Platform contract: https://developer.android.com/media/platform/sharing-audio-input
 // AOSP AudioRecord (Apache-2.0) and PiotrPrus/AudioCapture (Apache-2.0) were
 // inspected as references. This lease-bound Java implementation copies no library code.
 boolean currentCapture(long lease,AudioRecord recorder,VoiceCaptureRules rules){return !closed&&session.current(lease)&&microphone==recorder&&captureRules==rules&&rules.active(lease,recorder);}
 boolean interruptCapture(long lease,AudioRecord recorder,VoiceCaptureRules rules,VoiceCaptureRules.Signal signal){synchronized(captureLock){
  if(!currentCapture(lease,recorder,rules)||!VoiceCaptureRules.forceDraft(session,rules,lease,recorder,signal))return false;
  session.stop(lease);stopRequested=true;emit(lease,"finishing","采音已中断，仅保留已识别草稿…",0);
  try{recorder.stop();}catch(IllegalStateException ignored){}return true;
 }}
 void observeConfiguration(long lease,AudioRecord recorder,VoiceCaptureRules rules,AudioRecordingConfiguration configuration){
  if(!currentCapture(lease,recorder,rules))return;
  if(configuration!=null&&configuration.getClientAudioSessionId()!=recorder.getAudioSessionId()){interruptCapture(lease,recorder,rules,VoiceCaptureRules.Signal.MONITOR_UNAVAILABLE);return;}
  AudioDeviceInfo device=configuration==null?null:configuration.getAudioDevice();
  VoiceCaptureRules.Signal signal=rules.configuration(lease,recorder,configuration!=null,configuration!=null&&configuration.isClientSilenced(),device==null?0:device.getId(),SystemClock.elapsedRealtime());
  if(signal!=VoiceCaptureRules.Signal.NONE)interruptCapture(lease,recorder,rules,signal);
 }
 void inspectCapture(long lease,AudioRecord recorder,VoiceCaptureRules rules){synchronized(captureLock){
  if(!currentCapture(lease,recorder,rules))return;
  try{if(Build.VERSION.SDK_INT<29){interruptCapture(lease,recorder,rules,VoiceCaptureRules.Signal.MONITOR_UNAVAILABLE);return;}observeConfiguration(lease,recorder,rules,recorder.getActiveRecordingConfiguration());}
  catch(RuntimeException failure){interruptCapture(lease,recorder,rules,VoiceCaptureRules.Signal.MONITOR_UNAVAILABLE);}
 }}
 void completeCapture(long lease,AudioRecord recorder,VoiceCaptureRules rules){synchronized(captureLock){inspectCapture(lease,recorder,rules);VoiceCaptureRules.Signal signal=rules.completion(lease,recorder);if(signal!=VoiceCaptureRules.Signal.NONE)interruptCapture(lease,recorder,rules,signal);}}
 AudioManager.AudioRecordingCallback monitor(long lease,AudioRecord recorder,VoiceCaptureRules rules){return new AudioManager.AudioRecordingCallback(){@Override public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configurations){synchronized(captureLock){
  if(!currentCapture(lease,recorder,rules))return;
  try{if(configurations!=null)for(AudioRecordingConfiguration value:configurations)if(value.getClientAudioSessionId()==recorder.getAudioSessionId()){observeConfiguration(lease,recorder,rules,value);break;}inspectCapture(lease,recorder,rules);}
  catch(RuntimeException failure){interruptCapture(lease,recorder,rules,VoiceCaptureRules.Signal.MONITOR_UNAVAILABLE);}
 }}};}
 void capture(long lease){AudioRecord recorder=null;Recognizer recognizer=null;VoiceCaptureRules rules=null;AudioManager.AudioRecordingCallback callback=null;boolean registered=false,completed=false,hardLimit=false;String error="",endReason="manual_stop_or_cancel";long captureBegan=0;int peakRms=0;VoiceEndpointRules endpoint=null;try{
  if(model==null){File path=unpack();if(!session.current(lease)||closed)return;model=new Model(path.getAbsolutePath());}
  if(!session.current(lease)||closed||!listener.foreground())return;
  int minimum=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(minimum<=0)throw new IOException("当前手机不支持16kHz麦克风采样");
  recognizer=new Recognizer(model,RATE);recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(minimum,6400));if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("麦克风暂时不可用，请关闭其他录音后重试");
  rules=new VoiceCaptureRules(lease,recorder);callback=monitor(lease,recorder,rules);
  synchronized(captureLock){if(!session.listening(lease)||closed||!listener.foreground())return;microphone=recorder;captureRules=rules;
   if(Build.VERSION.SDK_INT<29)throw new IOException("麦克风采音状态检测需要Android 10或更新系统，请使用文字输入");
   recorder.registerAudioRecordingCallback(command->main.post(command),callback);registered=true;
   recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IOException("麦克风未开始录音");rules.start(lease,recorder,SystemClock.elapsedRealtime());inspectCapture(lease,recorder,rules);}
  if(!stopRequested)emit(lease,"listening","正在听，请说出目标…",0);short[] pcm=new short[1600];long began=SystemClock.elapsedRealtime(),lastPreview=0;captureBegan=began;endpoint=new VoiceEndpointRules(began);
  while(session.current(lease)&&!closed&&!stopRequested){inspectCapture(lease,recorder,rules);if(stopRequested)break;int n;
   try{n=recorder.read(pcm,0,pcm.length,AudioRecord.READ_NON_BLOCKING);}catch(RuntimeException failure){if(!stopRequested&&session.current(lease))interruptCapture(lease,recorder,rules,VoiceCaptureRules.Signal.READ_ERROR);break;}
   if(stopRequested||!session.current(lease)||closed)break;long now=SystemClock.elapsedRealtime();VoiceCaptureRules.Signal readSignal=rules.read(lease,recorder,n,now);if(readSignal!=VoiceCaptureRules.Signal.NONE){interruptCapture(lease,recorder,rules,readSignal);break;}
   inspectCapture(lease,recorder,rules);if(stopRequested)break;if(n==0){SystemClock.sleep(20);continue;}long energy=0;for(int i=0;i<n;i++)energy+=(long)pcm[i]*pcm[i];int rms=(int)Math.sqrt(energy/(double)n);peakRms=Math.max(peakRms,rms);endpoint.audio(rms,now);
   if(recognizer.acceptWaveForm(pcm,n)){String segment=new JSONObject(recognizer.getResult()).optString("text");session.segment(lease,segment);endpoint.segment(segment,now);}
   if(now-lastPreview>250){String partial=new JSONObject(recognizer.getPartialResult()).optString("partial");endpoint.partial(partial,now);emit(lease,"listening",session.preview(lease,partial),Math.min(100,rms/70));lastPreview=now;}
   if(now-began>=MAX_SECONDS*1000L){hardLimit=session.forceDraft(lease);endReason="hard_limit";session.stop(lease);stopRequested=true;break;}
   if(endpoint.shouldStop(now)){endReason=endpoint.heard()?"trailing_silence":"initial_silence";session.stop(lease);stopRequested=true;break;}
  }
  if(session.current(lease)&&!closed&&rules.interruption()==VoiceCaptureRules.Signal.NONE){completeCapture(lease,recorder,rules);if(rules.interruption()==VoiceCaptureRules.Signal.NONE){session.segment(lease,new JSONObject(recognizer.getFinalResult()).optString("text"));if(SystemClock.elapsedRealtime()-began>=MAX_SECONDS*1000L)hardLimit=session.forceDraft(lease);completed=true;}}
 }catch(Throwable failure){if(session.current(lease)&&!closed)error=failure instanceof IllegalArgumentException?failure.getMessage():failure instanceof SecurityException?"麦克风权限已关闭，请在系统中允许后重试":failure instanceof IOException&&failure.getMessage()!=null&&failure.getMessage().startsWith("麦克风")?failure.getMessage():"离线语音识别不可用，请重新尝试或使用文字输入";}
  finally{if(recorder!=null){synchronized(captureLock){if(rules!=null)rules.end(lease,recorder);if(microphone==recorder)microphone=null;if(captureRules==rules)captureRules=null;if(registered)try{recorder.unregisterAudioRecordingCallback(callback);}catch(RuntimeException ignored){}try{recorder.stop();}catch(IllegalStateException ignored){}try{recorder.release();}catch(RuntimeException ignored){}}}if(recognizer!=null)recognizer.close();final boolean done=completed,limited=hardLimit;final VoiceCaptureRules.Signal interrupted=rules==null?VoiceCaptureRules.Signal.NONE:rules.interruption();final String message=error;final long captureMs=captureBegan==0?0:SystemClock.elapsedRealtime()-captureBegan;final int measuredPeak=peakRms,decoderUpdates=endpoint==null?0:endpoint.decoderUpdates();final String reason=limited?"hard_limit":interrupted!=VoiceCaptureRules.Signal.NONE?"capture_interrupted":!message.isEmpty()?"error":endReason;main.post(()->{if(!session.current(lease)||closed)return;VoiceSession.Result result=session.finish(lease,listener.foreground());saveCaptureDiagnostics(captureMs,measuredPeak,decoderUpdates,reason,result);listener.state(state("idle","",0));if(interrupted!=VoiceCaptureRules.Signal.NONE){if(result!=null)listener.result(result.text,false);if(listener.foreground())listener.error(VoiceCaptureRules.explanation(interrupted)+(result==null?"，未发送任务；请重新说出目标":"，内容可能不完整；仅填入草稿，请补全后发送或重新说出目标"));}else if(!message.isEmpty())listener.error(message);else if(done&&result!=null){listener.result(result.text,result.direct);if(limited)listener.error("语音已达90秒上限，内容可能未说完；仅填入草稿，请补全后再发送");}else if(done&&listener.foreground())listener.error(limited?"语音已达90秒上限，未发送任务；请分次重新说出目标":"没有听清，请再说一次");});}}
 void saveCaptureDiagnostics(long duration,int peak,int updates,String reason,VoiceSession.Result result){
  if((owner.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  try{JSONObject data=new JSONObject().put("sampledAt",System.currentTimeMillis()).put("capture_duration_ms",duration).put("peak_frame_rms",peak).put("decoder_progress_events",updates).put("end_reason",reason).put("final_characters",result==null?0:result.text.length()).put("direct_result",result!=null&&result.direct).put("raw_audio_saved",false).put("transcript_saved_in_diagnostics",false);try(FileOutputStream output=owner.openFileOutput("voice-capture-diagnostics.json",0)){output.write(data.toString().getBytes("UTF-8"));}}catch(Exception ignored){}
 }
 File unpack()throws IOException{return unpack(owner);}
 static synchronized File unpack(Activity owner)throws IOException{File base=new File(owner.getFilesDir(),"offline-voice"),ready=new File(base,MODEL),marker=new File(ready,".ready-v1");if(marker.isFile()&&new File(ready,"am/final.mdl").isFile()&&new File(ready,"graph/HCLr.fst").isFile())return ready;if(!base.isDirectory()&&!base.mkdirs())throw new IOException("无法准备离线模型");File pending=new File(base,MODEL+".pending");erase(pending);if(!pending.mkdirs())throw new IOException("无法准备离线模型");long total=0;int entries=0;try(InputStream source=owner.getAssets().open(ASSET);ZipInputStream zip=new ZipInputStream(source)){ZipEntry item;byte[] buffer=new byte[32768];while((item=zip.getNextEntry())!=null){if(++entries>1000)throw new IOException("模型文件过多");String name=item.getName();if(!name.startsWith(MODEL+"/"))throw new IOException("无效模型内容");String local=name.substring(MODEL.length()+1);if(local.isEmpty())continue;File destination=new File(pending,local);if(!destination.getCanonicalPath().startsWith(pending.getCanonicalPath()+File.separator))throw new IOException("无效模型路径");if(item.isDirectory()){if(!destination.isDirectory()&&!destination.mkdirs())throw new IOException("无法准备模型目录");continue;}File parent=destination.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("无法准备模型目录");try(OutputStream target=new FileOutputStream(destination)){int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>300L*1024*1024)throw new IOException("模型超出大小限制");target.write(buffer,0,n);}}zip.closeEntry();}}
  if(!new File(pending,"am/final.mdl").isFile()||!new File(pending,"graph/HCLr.fst").isFile())throw new IOException("离线模型不完整");Files.write(new File(pending,".ready-v1").toPath(),MODEL.getBytes("UTF-8"));erase(ready);if(!pending.renameTo(ready))throw new IOException("无法保存离线模型");return ready;}
 static void erase(File file)throws IOException{if(!file.exists())return;File[] children=file.listFiles();if(children!=null)for(File child:children)erase(child);if(!file.delete())throw new IOException("无法准备离线模型文件");}
 void close(){synchronized(captureLock){closed=true;session.cancel();stopRequested=true;if(captureRules!=null)captureRules.end(token,microphone);stopMicrophone();}worker.submit(()->{if(model!=null){model.close();model=null;}});worker.shutdown();}
}
