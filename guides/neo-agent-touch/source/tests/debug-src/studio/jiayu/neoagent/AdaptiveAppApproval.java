package studio.jiayu.neoagent;
import android.app.Activity;import org.json.*;import java.util.*;import java.io.*;
/** Debug bridge for this session's explicit five-app consent. Never present in release. */
final class AdaptiveAppApproval {
 static final List<String> APPS=Arrays.asList("com.tencent.mm","com.autonavi.minimap","com.tencent.qqmusic","com.tencent.wework","com.baidu.netdisk");
 static void run(Activity a){JSONObject out=new JSONObject();try{
  Config c=Config.load(a);if(!"api.deepseek.com".equals(new java.net.URI(c.endpoint).getHost()))throw new Exception("recipient changed");
  AppAccess access=new AppAccess(a);JSONObject choices=access.state();JSONArray apps=choices.getJSONArray("apps");Set<String> found=new HashSet<>();int previous=access.approved().size();
  for(int i=0;i<apps.length();i++){JSONObject item=apps.getJSONObject(i);String pkg=item.getString("package");if(APPS.contains(pkg)){item.put("enabled",true);found.add(pkg);}}
  if(found.size()!=APPS.size())throw new Exception("requested app unavailable");access.save(choices);new RoutineAccess(a).approve(c,APPS);
  for(String pkg:APPS)if(!access.allowed(pkg)||!new RoutineAccess(a).allows(c,Collections.singletonList(pkg)))throw new Exception("consent readback failed");
  out.put("approved",true).put("apps",new JSONArray(APPS)).put("recipient","api.deepseek.com").put("visual_scope","full_target_app_v1").put("previous_selected_count",previous).put("selected_count",access.approved().size()).put("persisted_readback",true).put("authorization","explicit user reply 2026-10-23; query/navigation/playback only");
 }catch(Exception e){try{out.put("approved",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){}}
 try(FileOutputStream f=a.openFileOutput("adaptive-app-scope.json",0)){f.write(out.toString(2).getBytes("UTF-8"));}catch(Exception ignored){}
 }
}
