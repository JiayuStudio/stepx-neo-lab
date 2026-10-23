package studio.jiayu.neoagent;

/**
 * Per-recorder input continuity, independent of Android and of ASR text.
 * A known interruption invalidates automatic execution for the whole utterance.
 */
final class VoiceCaptureRules {
 enum Signal { NONE, SYSTEM_SILENCED, ROUTE_CHANGED, CONFIGURATION_LOST, MONITOR_UNAVAILABLE, READ_ERROR, INPUT_STALLED }
 static final long STARTUP_GRACE_MILLIS=1500,INPUT_STALL_MILLIS=1500;
 private final long lease;private final Object recorder;
 private boolean started,ended,configurationSeen;private long began,lastData;private int route;
 private Signal interruption=Signal.NONE;
 VoiceCaptureRules(long captureLease,Object record){if(record==null)throw new IllegalArgumentException("Recorder identity required");lease=captureLease;recorder=record;}
 synchronized boolean start(long captureLease,Object record,long now){if(!matches(captureLease,record)||started||ended)return false;started=true;began=lastData=now;return true;}
 private boolean matches(long captureLease,Object record){return lease==captureLease&&recorder==record;}
 synchronized boolean active(long captureLease,Object record){return matches(captureLease,record)&&started&&!ended;}
 synchronized Signal configuration(long captureLease,Object record,boolean present,boolean silenced,int inputRoute,long now){
  if(!active(captureLease,record))return Signal.NONE;
  if(!present)return configurationSeen||now-began>=STARTUP_GRACE_MILLIS?Signal.CONFIGURATION_LOST:Signal.NONE;
  if(silenced)return Signal.SYSTEM_SILENCED;
  configurationSeen=true;
  if(inputRoute!=0){if(route!=0&&route!=inputRoute)return Signal.ROUTE_CHANGED;route=inputRoute;}
  return Signal.NONE;
 }
 synchronized Signal read(long captureLease,Object record,int samples,long now){
  if(!active(captureLease,record))return Signal.NONE;
  if(samples<0)return Signal.READ_ERROR;
  if(samples>0){lastData=now;return Signal.NONE;}
  return now-lastData>=INPUT_STALL_MILLIS?Signal.INPUT_STALLED:Signal.NONE;
 }
 synchronized Signal completion(long captureLease,Object record){return active(captureLease,record)&&!configurationSeen?Signal.MONITOR_UNAVAILABLE:Signal.NONE;}
 synchronized boolean interrupt(long captureLease,Object record,Signal signal){if(signal==null||signal==Signal.NONE||!active(captureLease,record))return false;interruption=signal;ended=true;return true;}
 synchronized void end(long captureLease,Object record){if(matches(captureLease,record))ended=true;}
 synchronized Signal interruption(){return interruption;}
 /** Shared by the real capture monitor and debug event injection; never changes words. */
 static boolean forceDraft(VoiceSession session,VoiceCaptureRules capture,long lease,Object recorder,Signal signal){
  if(!session.current(lease)||!capture.interrupt(lease,recorder,signal))return false;
  return session.forceDraft(lease);
 }
 static String explanation(Signal signal){switch(signal){
  case SYSTEM_SILENCED:return "麦克风被系统或其他录音静音";
  case ROUTE_CHANGED:return "麦克风音源发生切换";
  case CONFIGURATION_LOST:return "麦克风采音连接中断";
  case MONITOR_UNAVAILABLE:return "无法确认本次麦克风采音状态";
  case READ_ERROR:return "麦克风读取中断";
  case INPUT_STALLED:return "麦克风没有继续提供音频";
  default:return "麦克风采音中断";
 }}
}
