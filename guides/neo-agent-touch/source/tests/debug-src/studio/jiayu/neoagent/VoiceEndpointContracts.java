package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.json.*;
import org.vosk.Model;
import org.vosk.Recognizer;

/** Fixed public, attenuated PCM -> real Vosk -> production endpoint rules. No microphone or tasks. */
final class VoiceEndpointContracts {
 static final String REPORT="voice-endpoint-contracts.json",INPUT="qa-voice-soft-long.pcm";
 static final String PCM_SHA256="a63db8e13f11f02437853946176a348c82fb2a1b10b307fa09f25bd1b55d1279";
 static final String MODEL_SHA256="3af8b0e7e0f835ae9d414ce5df580237a3cfb08d586c9fbbb0f7ff29ad5b14ba";
 static final int PCM_BYTES=672392,FRAME_SAMPLES=1600;
 static final long DEADLINE_MS=45000;
 final Activity activity;final Handler main=new Handler(Looper.getMainLooper());
 final JSONObject report=new JSONObject();final JSONArray rawResults=new JSONArray(),partialResults=new JSONArray(),progress=new JSONArray();
 final ArrayList<String> finals=new ArrayList<>();final long wallBegan=SystemClock.elapsedRealtime();
 volatile boolean expired;Thread worker;String stage="precondition";boolean modelCreated,recognizerCreated,modelReleased,recognizerReleased;
 final Runnable timeout=()->{expired=true;if(worker!=null)worker.interrupt();};
 VoiceEndpointContracts(Activity owner){activity=owner;}
 static void start(Activity activity){new VoiceEndpointContracts(activity).begin();}
 void begin(){worker=new Thread(this::run,"voice-endpoint-contracts");main.postDelayed(timeout,DEADLINE_MS);worker.start();}
 void check()throws Exception{if(expired||Thread.currentThread().isInterrupted()||SystemClock.elapsedRealtime()-wallBegan>=DEADLINE_MS)throw new Failure("deadline_exceeded");if(activity.isFinishing()||activity.isDestroyed())throw new Failure("activity_left");}
 void require(boolean ok,String code)throws Exception{if(!ok)throw new Failure(code);}
 void run(){Model model=null;Recognizer recognizer=null;boolean decoded=false;try{
  require((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"not_debug");
  report.put("source","fixed public synthetic attenuated PCM; actual on-device bundled Vosk decode")
   .put("input_file",INPUT).put("expected_pcm_sha256",PCM_SHA256).put("expected_pcm_bytes",PCM_BYTES)
   .put("sample_rate",OfflineVoice.RATE).put("format","mono PCM16LE").put("frame_samples",FRAME_SAMPLES)
   .put("sample_time_clock",true).put("real_time_delay_used",false).put("partial_poll_condition","sample_now_ms - last_poll_ms > 250")
   .put("microphone_used",false).put("microphone_permission_requested",false).put("audio_uploaded",false)
   .put("network_calls",0).put("planner_started",false).put("task_started",false).put("business_lease_used",false)
   .put("correct_reference_given_to_decoder",false).put("lexical_correction",false)
   .put("real_microphone_acceptance",false).put("recognition_accuracy_proven",false).put("hard_limit_exercised",false)
   .put("success",false).put("native_decode_resources_released",false);
  stage="validating_audio";persist();check();File input=new File(activity.getFilesDir(),INPUT);
  require(input.isFile()&&input.length()==PCM_BYTES,"pcm_missing_or_size_mismatch");byte[] pcm=Files.readAllBytes(input.toPath());
  String pcmHash=digest(new ByteArrayInputStream(pcm));report.put("pcm_sha256",pcmHash).put("pcm_bytes",pcm.length);
  require(PCM_SHA256.equals(pcmHash),"pcm_sha256_mismatch");report.put("input_verified",true).put("duration_seconds",pcm.length/(2.0*OfflineVoice.RATE));
  stage="validating_model";persist();String modelHash=digest(activity.getAssets().open(OfflineVoice.ASSET));report.put("asset_model_sha256",modelHash);
  require(MODEL_SHA256.equals(modelHash),"model_asset_sha256_mismatch");check();File directory=OfflineVoice.unpack(activity);check();
  report.put("model_cache_files_verified",verifyModel(directory)).put("model_verified",true);stage="loading_native_model";persist();
  model=new Model(directory.getAbsolutePath());modelCreated=true;check();recognizer=new Recognizer(model,OfflineVoice.RATE);recognizerCreated=true;
  report.put("model_ready",true);stage="decoding_sample_time";persist();
  VoiceEndpointRules endpoint=new VoiceEndpointRules(0);long lastPreview=0,oldLastVoice=0,oldFirstStop=-1,newFirstStop=-1;boolean oldHeard=false,afterTen=false;int frames=0;double maxRms=0;String partial="";
  long totalSamples=pcm.length/2;double knownTailBeginsMs=(totalSamples*1000.0/OfflineVoice.RATE)-4000.0;
  for(int offset=0;offset<pcm.length;offset+=FRAME_SAMPLES*2){check();int count=Math.min(FRAME_SAMPLES,(pcm.length-offset)/2);short[] frame=new short[count];long energy=0;
   for(int i=0;i<count;i++){int low=pcm[offset+i*2]&255,high=pcm[offset+i*2+1];frame[i]=(short)(low|(high<<8));energy+=(long)frame[i]*frame[i];}
   double rmsValue=Math.sqrt(energy/(double)count);maxRms=Math.max(maxRms,rmsValue);int rms=(int)rmsValue;
   long samplesEnd=offset/2L+count,now=samplesEnd*1000L/OfflineVoice.RATE;endpoint.audio(rms,now);if(rms>500){oldHeard=true;oldLastVoice=now;}
   if(recognizer.acceptWaveForm(frame,count)){JSONObject value=new JSONObject(recognizer.getResult());String text=value.optString("text");rawResults.put(new JSONObject().put("sample_ms",now).put("kind","segment").put("raw",value));if(!text.trim().isEmpty())finals.add(text);long before=endpoint.lastActivity();endpoint.segment(text,now);if(endpoint.lastActivity()>before){recordProgress("segment",text,now);if(now>10000)afterTen=true;}}
   if(now-lastPreview>250){JSONObject value=new JSONObject(recognizer.getPartialResult());partial=value.optString("partial");partialResults.put(new JSONObject().put("sample_ms",now).put("raw",value));long before=endpoint.lastActivity();endpoint.partial(partial,now);if(endpoint.lastActivity()>before){recordProgress("partial",partial,now);if(now>10000)afterTen=true;}lastPreview=now;}
   if(oldFirstStop<0&&((oldHeard&&now-oldLastVoice>3200)||(!oldHeard&&now>10000))){oldFirstStop=now;report.put("old_stop_committed_raw",join()).put("old_stop_last_polled_partial_raw",partial);}
   if(newFirstStop<0&&endpoint.shouldStop(now)){newFirstStop=now;report.put("new_stop_committed_raw",join()).put("new_stop_last_polled_partial_raw",partial);}
   frames++;
  }
  check();JSONObject finalValue=new JSONObject(recognizer.getFinalResult());rawResults.put(new JSONObject().put("sample_ms",totalSamples*1000.0/OfflineVoice.RATE).put("kind","full_file_flush_not_endpoint_activity").put("raw",finalValue));String finalText=finalValue.optString("text");if(!finalText.trim().isEmpty())finals.add(finalText);
  boolean laterProgress=false;if(newFirstStop>=0)for(int i=0;i<progress.length();i++)if(progress.getJSONObject(i).optLong("sample_ms")>newFirstStop)laterProgress=true;
  boolean early=newFirstStop>=0&&newFirstStop<knownTailBeginsMs;
  boolean baseline=oldFirstStop>=0&&oldFirstStop<knownTailBeginsMs&&!oldHeard;
  boolean preserved=!early&&(newFirstStop<0||newFirstStop>10000);
  boolean passed=baseline&&afterTen&&!early&&!laterProgress&&newFirstStop>=0&&!join().trim().isEmpty()&&maxRms<500;
  report.put("full_file_decode_completed",true).put("frames_decoded",frames).put("sample_duration_ms",totalSamples*1000.0/OfflineVoice.RATE)
   .put("known_four_second_tail_begins_ms",knownTailBeginsMs).put("known_tail_source","fixture construction metadata; not a reference transcript")
   .put("max_frame_rms",maxRms).put("old_rms_heard",oldHeard).put("all_frame_rms_below_500",maxRms<500)
   .put("old_rule_first_stop_ms",oldFirstStop<0?JSONObject.NULL:oldFirstStop).put("old_rule_comparison","original strict >10000 / >3200 checks")
   .put("new_rule_first_stop_ms",newFirstStop<0?JSONObject.NULL:newFirstStop).put("new_rule_endpoint_observed",newFirstStop>=0)
   .put("old_initial_silence_cut_reproduced",baseline).put("new_recognition_activity_after_10_seconds",afterTen)
   .put("new_rule_stopped_before_known_tail",early).put("decoder_activity_after_new_first_stop",laterProgress)
   .put("new_rule_preserved_non_tail_input",preserved).put("raw_transcript",join()).put("raw_results",rawResults).put("raw_partial_results",partialResults)
   .put("recognition_progress_events",progress).put("decode_continued_after_first_stop_for_counterfactual_comparison",true)
   .put("success",passed).put("success_meaning","fixed low-energy fixture reproduces old cutoff and the new rule reaches an endpoint after preserving non-tail input; not live speech or word accuracy")
   .put("comparison_limit","full-file Vosk remains running for analysis after recorded stop decisions; final flush is not counted as endpoint activity");
  stage=passed?"fixed_pcm_endpoint_comparison_passed":early?"new_rule_stopped_before_expected_tail":laterProgress?"decoder_progress_after_new_stop":newFirstStop<0?"new_endpoint_not_observed_in_fixture":"comparison_not_demonstrated";decoded=true;
 }catch(Throwable failure){try{report.put("success",false).put("failure_stage",stage).put("failure_type",failure.getClass().getSimpleName()).put("failure_code",failure instanceof Failure?failure.getMessage():"native_or_io_failure");}catch(Exception ignored){}stage="failed";}
 finally{main.removeCallbacks(timeout);if(recognizer!=null)try{recognizer.close();recognizerReleased=true;}catch(Throwable ignored){}if(model!=null)try{model.close();modelReleased=true;}catch(Throwable ignored){}
  try{boolean released=(!recognizerCreated||recognizerReleased)&&(!modelCreated||modelReleased),deadline=expired||SystemClock.elapsedRealtime()-wallBegan>=DEADLINE_MS;report.put("model_created",modelCreated).put("recognizer_created",recognizerCreated).put("model_released",modelReleased).put("recognizer_released",recognizerReleased).put("native_decode_resources_released",released).put("deadline_exceeded",deadline).put("decode_verified",decoded).put("raw_transcript",join()).put("raw_results",rawResults).put("raw_partial_results",partialResults).put("recognition_progress_events",progress);if(!released){report.put("success",false).put("failure_code","native_release_failed");stage="failed";}else if(deadline){report.put("success",false).put("failure_code","deadline_exceeded");stage="failed";}persist();}catch(Exception ignored){}activity.runOnUiThread(activity::finish);
 }}
 void recordProgress(String kind,String raw,long now)throws Exception{progress.put(new JSONObject().put("kind",kind).put("sample_ms",now).put("raw_text",raw).put("production_normalized_activity_text",VoiceSession.normalize(raw)));}
 String join(){StringBuilder text=new StringBuilder();for(String value:finals){if(text.length()>0)text.append(' ');text.append(value);}return text.toString();}
 int verifyModel(File model)throws Exception{int checked=0;try(ZipInputStream zip=new ZipInputStream(activity.getAssets().open(OfflineVoice.ASSET))){ZipEntry entry;byte[] buffer=new byte[32768];while((entry=zip.getNextEntry())!=null){check();if(entry.isDirectory())continue;require(entry.getName().startsWith(OfflineVoice.MODEL+"/"),"invalid_model_entry");File cached=new File(model,entry.getName().substring(OfflineVoice.MODEL.length()+1));require(cached.getCanonicalPath().startsWith(model.getCanonicalPath()+File.separator)&&cached.isFile(),"model_cache_missing");MessageDigest sha=MessageDigest.getInstance("SHA-256");int n;long size=0;while((n=zip.read(buffer))!=-1){check();sha.update(buffer,0,n);size+=n;}require(cached.length()==size&&hex(sha.digest()).equals(digest(new FileInputStream(cached))),"model_cache_integrity_mismatch");checked++;zip.closeEntry();}}require(checked>5,"model_cache_inventory_incomplete");return checked;}
 String digest(InputStream stream)throws Exception{try(InputStream input=stream){MessageDigest sha=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1){check();sha.update(buffer,0,n);}return hex(sha.digest());}}
 static String hex(byte[] bytes){StringBuilder value=new StringBuilder();for(byte b:bytes)value.append(String.format(Locale.ROOT,"%02x",b&255));return value.toString();}
 void persist()throws Exception{report.put("stage",stage).put("wall_elapsed_ms",SystemClock.elapsedRealtime()-wallBegan).put("sampledAt",System.currentTimeMillis());try(FileOutputStream out=activity.openFileOutput(REPORT,0)){out.write(report.toString(2).getBytes("UTF-8"));out.getFD().sync();}}
 static final class Failure extends Exception{Failure(String code){super(code);}}
}
