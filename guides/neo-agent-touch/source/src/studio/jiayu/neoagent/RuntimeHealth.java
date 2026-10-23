package studio.jiayu.neoagent;
import android.os.*;import org.json.*;import java.util.concurrent.*;
/** Main-thread observations and an independent deadline sentinel. No gestures or network. */
final class RuntimeHealth {
 private final ControlService host;private final Handler main=new Handler(Looper.getMainLooper());private final ScheduledExecutorService sentinel=Executors.newSingleThreadScheduledExecutor();
 private volatile long heartbeat,phaseSince,outsideSince;private volatile String phase="idle";private volatile boolean expected,covered;private volatile boolean closed;private boolean started;
 private volatile String sampleStage="not_started";private volatile long sampleStartedAt,sampleElapsed;private volatile boolean sampleActive;private volatile DeadlineDiagnostic deadlineDiagnostic;
 private static final class DeadlineDiagnostic {
  final String run,phase,stage,threadState;final long generation,capturedAt,heartbeatAge,sampleElapsed;final boolean sampleActive;final String[] frames;volatile long pausedGeneration=-1;
  DeadlineDiagnostic(String run,long generation,String phase,String stage,long capturedAt,long heartbeatAge,long sampleElapsed,boolean sampleActive,String threadState,String[] frames){this.run=run;this.generation=generation;this.phase=phase;this.stage=stage;this.capturedAt=capturedAt;this.heartbeatAge=heartbeatAge;this.sampleElapsed=sampleElapsed;this.sampleActive=sampleActive;this.threadState=threadState;this.frames=frames;}
 }
 RuntimeHealth(ControlService host){this.host=host;}
 void start(){if(started||closed)return;started=true;heartbeat=SystemClock.elapsedRealtime();main.post(sample);sentinel.scheduleAtFixedRate(this::checkDeadline,1,1,TimeUnit.SECONDS);}
 private void checkDeadline(){
  if(closed)return;long now=SystemClock.elapsedRealtime(),generation=host.generation;String run=host.runId,currentPhase=phase;boolean running=host.running;long heartbeatAge=heartbeat==0?0:now-heartbeat;
  String reason=HealthRules.failure(running,heartbeatAge,currentPhase,now-phaseSince,outsideSince==0?0:now-outsideSince);if(reason.isEmpty())return;
  DeadlineDiagnostic diagnostic=null;
  if(running&&heartbeatAge>3500){
   Thread mainThread=main.getLooper().getThread();String state="unavailable";String[] frames=new String[0];try{state=mainThread.getState().name();frames=HealthRules.diagnosticFrames(mainThread.getStackTrace());}catch(RuntimeException unavailable){}
   boolean active=sampleActive;long elapsed=active?Math.max(0,now-sampleStartedAt):sampleElapsed;
   diagnostic=new DeadlineDiagnostic(run,generation,HealthRules.phaseCode(currentPhase),sampleStage,now,heartbeatAge,elapsed,active,state,frames);
  }
  // Capture the stack before waiting for the service monitor. A new run cannot inherit it.
  synchronized(host){if(closed||!host.running||host.generation!=generation||!run.equals(host.runId))return;if(diagnostic!=null)deadlineDiagnostic=diagnostic;host.pauseForHealth(reason);}
 }
 synchronized void phase(String next){phase=next;phaseSince=SystemClock.elapsedRealtime();if("starting".equals(next)||"idle".equals(next)||"disconnected".equals(next))deadlineDiagnostic=null;DeadlineDiagnostic diagnostic=deadlineDiagnostic;if("paused".equals(next)&&diagnostic!=null&&diagnostic.run.equals(host.runId))diagnostic.pausedGeneration=host.generation;if(!host.running||"starting".equals(next))outsideSince=0;}
 private final Runnable sample=new Runnable(){public void run(){if(closed)return;long now=SystemClock.elapsedRealtime();sampleStartedAt=now;sampleActive=true;sampleStage="begin";heartbeat=now;try{if(host.running){sampleStage="window_inspect";NativeWindowState state=NativeWindowState.inspect(host);sampleStage="window_result";expected=state.expected;covered=state.covered;boolean usable=expected&&!covered;if(!usable){sampleStage="keyboard_release";host.keyboardLease.releaseUi();if(!"starting".equals(phase)){sampleStage="ime_release";host.agentImeLease.releaseUi();}}if(usable)outsideSince=0;else if(!"starting".equals(phase)){if(outsideSince==0)outsideSince=now;sampleStage="decision_invalidate";host.invalidatePendingDecision();}}else outsideSince=0;}catch(Exception failure){sampleStage="sample_exception";expected=false;if(host.running&&outsideSince==0)outsideSince=now;}finally{sampleElapsed=Math.max(0,SystemClock.elapsedRealtime()-now);sampleActive=false;if(!"sample_exception".equals(sampleStage))sampleStage="completed";if(!closed)main.postDelayed(this,500);}}};
 JSONObject snapshot()throws Exception{long now=SystemClock.elapsedRealtime();JSONObject result=new JSONObject().put("sampled_at_ms",System.currentTimeMillis()).put("phase",phase).put("phase_age_ms",Math.max(0,now-phaseSince)).put("heartbeat_age_ms",heartbeat==0?-1:Math.max(0,now-heartbeat)).put("expected_foreground",expected).put("covered",covered).put("outside_age_ms",outsideSince==0?0:Math.max(0,now-outsideSince)).put("sample_stage",sampleStage).put("sample_in_progress",sampleActive).put("sample_elapsed_ms",sampleActive?Math.max(0,now-sampleStartedAt):sampleElapsed);
  DeadlineDiagnostic diagnostic=deadlineDiagnostic;if(diagnostic!=null&&HealthRules.diagnosticScope(diagnostic.run,diagnostic.generation,diagnostic.pausedGeneration,host.runId,host.generation,host.paused)){JSONArray frames=new JSONArray();for(String frame:diagnostic.frames)frames.put(frame);result.put("heartbeat_diagnostic",new JSONObject().put("kind","heartbeat_deadline").put("captured_elapsed_ms",diagnostic.capturedAt).put("heartbeat_age_ms",diagnostic.heartbeatAge).put("phase",diagnostic.phase).put("sample_stage",diagnostic.stage).put("sample_elapsed_ms",diagnostic.sampleElapsed).put("sample_in_progress",diagnostic.sampleActive).put("main_thread_state",diagnostic.threadState).put("main_stack",frames));}return result;}
 void close(){closed=true;main.removeCallbacks(sample);sentinel.shutdownNow();}
}
