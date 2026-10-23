package studio.jiayu.neoagent;
import android.app.Activity;import android.os.*;import org.json.*;import java.io.*;
/** Fixed already-approved WPS -> Meituan launch, zero model calls or in-app business actions. */
final class LauncherContracts {
 static void start(Activity a){new Thread(()->run(a)).start();}
 static void run(Activity a){ControlService s=ControlService.instance;JSONObject r=new JSONObject();try{
  if(s==null||!s.connected||s.busy())throw new Exception("Runtime not idle");Config cfg=Config.load(a);cfg.vision=false;for(String pkg:new String[]{"cn.wps.moffice_eng","com.sankuai.meituan"})s.requireDataGrant(cfg,pkg);
  long g=s.ui(()->{s.workflow=null;s.task="跨应用原生启动测试，无模型、无业务动作";s.runId="";s.ownerSession="";s.allowed="cn.wps.moffice_eng";s.runConfig=cfg;s.running=true;s.paused=false;s.generation=s.ownership.start();s.runtimeHealth.phase("starting");s.launchRecoveries=0;s.modelCalls=s.actions=s.screenshots=0;s.startedAt=System.currentTimeMillis();s.activeMillis=0;s.activeSince=SystemClock.elapsedRealtime();s.showOverlay();return s.generation;});
  boolean wps=AppLauncher.open(s,"cn.wps.moffice_eng",g);s.allowed="com.sankuai.meituan";s.runtimeHealth.phase("starting");boolean meituan=AppLauncher.open(s,"com.sankuai.meituan",g);s.runtimeHealth.phase("waiting");r.put("wps_foreground",wps).put("meituan_foreground",meituan).put("launch_recoveries",s.launchRecoveries).put("success",wps&&meituan);Thread.sleep(4000);
 }catch(Throwable failure){try{r.put("success",false).put("failure",failure.getMessage());}catch(Exception ignored){}}
 finally{if(s!=null)s.stop("跨应用启动回归结束");try{r.put("real_network_calls",0).put("business_actions",0).put("host_business_clicks",0).put("sampledAt",System.currentTimeMillis());try(FileOutputStream out=a.openFileOutput("launcher-contracts.json",0)){out.write(r.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}new Handler(Looper.getMainLooper()).post(a::finish);}
 }
}
