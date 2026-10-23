package studio.jiayu.neoagent;
import android.app.*;import android.content.*;import android.os.*;import android.view.accessibility.*;
/** Native requests are verified by the target foreground; a visible relay handles background restrictions. */
final class AppLauncher {
 static Bundle fullScreen(){ActivityOptions options=ActivityOptions.makeBasic().setLaunchBounds(null);Bundle b=options.toBundle();b.putInt("android.activity.windowingMode",1);b.putInt("start_windowmode",-2);return b;}
 static Intent target(ControlService s,String pkg){Intent i=pkg.equals(s.getPackageName())?new Intent(s,DemoActivity.class):s.getPackageManager().getLaunchIntentForPackage(pkg);return i==null?null:i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);}
 static void requireRelay(ControlService s,String pkg,long g,String run)throws Exception{if(s==null)throw new Exception("控制服务未连接");s.check(g);if(pkg==null||run==null||!pkg.equals(s.allowed)||!run.equals(s.runId)||s.runConfig==null)throw new Exception("启动目标已变化 · 原请求未执行");s.requireDataGrant(s.runConfig,pkg);}
 static boolean foreground(ControlService s,String pkg)throws Exception{return s.ui(()->{AccessibilityNodeInfo root=s.getRootInActiveWindow();return root!=null&&pkg.contentEquals(root.getPackageName());});}
 static boolean request(ControlService s,String pkg,long g)throws Exception{return s.ui(()->{requireRelay(s,pkg,g,s.runId);Intent launch=target(s,pkg);if(launch==null)return false;s.startActivity(launch,fullScreen());return true;});}
 static boolean requestRelay(ControlService s,String pkg,long g)throws Exception{return s.ui(()->{requireRelay(s,pkg,g,s.runId);Intent i=new Intent(s,LaunchRelayActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS).putExtra("package",pkg).putExtra("generation",g).putExtra("run",s.runId);s.startActivity(i,fullScreen());return true;});}
 static void fromForeground(Activity a,ControlService s,String pkg,long g,String run)throws Exception{requireRelay(s,pkg,g,run);if(!a.hasWindowFocus())throw new Exception("启动过渡页未在前台 · 原请求未执行");Intent launch=target(s,pkg);if(launch==null)throw new Exception("目标应用没有启动入口");a.startActivity(launch,fullScreen());s.logFor(g,"前台启动请求已派发 · 等待目标应用实际出现");}
 static boolean waitFor(ControlService s,String pkg,long g,int count)throws Exception{for(int i=0;i<count;i++){s.check(g);if(foreground(s,pkg))return true;Thread.sleep(200);}return false;}
 static boolean open(ControlService s,String pkg,long g)throws Exception{if(!request(s,pkg,g))return false;if(waitFor(s,pkg,g,16))return true;
  s.logFor(g,"目标应用未出现 · 使用可见启动过渡页继续");if(!requestRelay(s,pkg,g))return false;if(!waitFor(s,pkg,g,60))throw new Exception("应用未能进入前台 · 前台启动亦未完成，原目标保留");s.launchRecoveries++;s.logFor(g,"目标应用已进入前台 · 前台启动恢复成功");return true;
 }
}
