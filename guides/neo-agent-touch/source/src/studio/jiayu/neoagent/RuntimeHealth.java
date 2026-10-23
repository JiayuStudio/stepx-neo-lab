package studio.jiayu.neoagent;
import android.os.*;import org.json.*;import java.util.concurrent.*;
/** Main-thread observations and an independent deadline sentinel. No gestures or network. */
final class RuntimeHealth {
 private final ControlService host;private final Handler main=new Handler(Looper.getMainLooper());private final ScheduledExecutorService sentinel=Executors.newSingleThreadScheduledExecutor();
 private volatile long heartbeat,phaseSince,outsideSince;private volatile String phase="idle";private volatile boolean expected,covered;private volatile boolean closed;private boolean started;
 RuntimeHealth(ControlService host){this.host=host;}
 void start(){if(started||closed)return;started=true;heartbeat=SystemClock.elapsedRealtime();main.post(sample);sentinel.scheduleAtFixedRate(()->{if(closed)return;long now=SystemClock.elapsedRealtime();String reason=HealthRules.failure(host.running,heartbeat==0?0:now-heartbeat,phase,now-phaseSince,outsideSince==0?0:now-outsideSince);if(!reason.isEmpty())host.pauseForHealth(reason);},1,1,TimeUnit.SECONDS);}
 synchronized void phase(String next){phase=next;phaseSince=SystemClock.elapsedRealtime();if(!host.running||"starting".equals(next))outsideSince=0;}
 private final Runnable sample=new Runnable(){public void run(){if(closed)return;long now=SystemClock.elapsedRealtime();heartbeat=now;try{if(host.running){NativeWindowState state=NativeWindowState.inspect(host);expected=state.expected;covered=state.covered;boolean usable=expected&&!covered;if(!usable){host.keyboardLease.releaseUi();if(!"starting".equals(phase))host.agentImeLease.releaseUi();}if(usable)outsideSince=0;else if(!"starting".equals(phase)){if(outsideSince==0)outsideSince=now;host.invalidatePendingDecision();}}else outsideSince=0;}catch(Exception failure){expected=false;if(host.running&&outsideSince==0)outsideSince=now;}finally{if(!closed)main.postDelayed(this,500);}}};
 JSONObject snapshot()throws Exception{long now=SystemClock.elapsedRealtime();return new JSONObject().put("sampled_at_ms",System.currentTimeMillis()).put("phase",phase).put("phase_age_ms",Math.max(0,now-phaseSince)).put("heartbeat_age_ms",heartbeat==0?-1:Math.max(0,now-heartbeat)).put("expected_foreground",expected).put("covered",covered).put("outside_age_ms",outsideSince==0?0:Math.max(0,now-outsideSince));}
 void close(){closed=true;main.removeCallbacks(sample);sentinel.shutdownNow();}
}
