package studio.jiayu.neoagent;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.database.ContentObserver;
import android.os.*;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import org.json.JSONObject;
import java.util.List;

/** Read-only checks for this one service; no global service list is retained or exposed. */
final class AccessibilityAccess {
 private final Activity activity;private final Runnable changed;private final ComponentName component;private final Handler main=new Handler(Looper.getMainLooper());private final AccessibilityManager manager;
 private boolean registeredKnown,registered,enabledKnown,enabled,watching,observerRegistered,managerRegistered,closed;
 private final ContentObserver observer=new ContentObserver(main){@Override public void onChange(boolean self){refresh();}};
 private final AccessibilityManager.AccessibilityStateChangeListener managerListener=value->refresh();
 private final Runnable recheck=()->{if(watching&&!closed)refresh();};
 AccessibilityAccess(Activity activity,Runnable changed){this.activity=activity;this.changed=changed;component=new ComponentName(activity,ControlService.class);manager=(AccessibilityManager)activity.getSystemService(Context.ACCESSIBILITY_SERVICE);refresh();}
 static boolean isCurrentConnected(ControlService service){return AccessibilityStateRules.connected(service!=null,service!=null&&ControlService.instance==service,service!=null&&service.connected);}
 private boolean containsOwnService(List<AccessibilityServiceInfo> services){if(services==null)return false;for(AccessibilityServiceInfo item:services){if(item==null)continue;ResolveInfo resolve=item.getResolveInfo();ServiceInfo info=resolve==null?null:resolve.serviceInfo;if(info!=null&&component.equals(ComponentName.unflattenFromString(info.packageName+"/"+info.name)))return true;}return false;}
 private boolean selected(String value){if(value==null||value.isEmpty())return false;for(String part:value.split(":")){ComponentName parsed=ComponentName.unflattenFromString(part);if(component.equals(parsed))return true;}return false;}
 private void refresh(){if(closed)return;String before=systemMarker();registeredKnown=false;registered=false;enabledKnown=false;enabled=false;
  try{ServiceInfo info=activity.getPackageManager().getServiceInfo(component,0);registeredKnown=true;registered=info!=null&&info.enabled&&"android.permission.BIND_ACCESSIBILITY_SERVICE".equals(info.permission);}catch(PackageManager.NameNotFoundException missing){registeredKnown=true;}catch(RuntimeException unavailable){}
  // The secure setting preserves a selected component while Android is still binding it.
  try{String setting=Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);enabledKnown=true;enabled=selected(setting);}catch(RuntimeException unavailable){}
  // Some OEMs restrict the settings query; an exact match in the public manager list is useful too.
  try{if(manager!=null){List<AccessibilityServiceInfo> list=manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);if(list!=null){enabled=enabled||containsOwnService(list);enabledKnown=true;}}}catch(RuntimeException unavailable){}
  if(watching&&!before.equals(systemMarker()))changed.run();
 }
 private String systemMarker(){return registeredKnown+"|"+registered+"|"+enabledKnown+"|"+enabled;}
 JSONObject snapshot(){ControlService service=ControlService.instance;boolean connected=isCurrentConnected(service);JSONObject value=new JSONObject();try{value.put("state",AccessibilityStateRules.state(registeredKnown,registered,enabledKnown,enabled,connected)).put("registeredKnown",registeredKnown).put("registered",registered).put("enabledKnown",enabledKnown).put("enabled",enabled).put("connected",connected);}catch(Exception ignored){}return value;}
 String marker(){ControlService service=ControlService.instance;return AccessibilityStateRules.marker(registeredKnown,registered,enabledKnown,enabled,isCurrentConnected(service));}
 String status(){return AccessibilityStateRules.status(AccessibilityStateRules.state(registeredKnown,registered,enabledKnown,enabled,isCurrentConnected(ControlService.instance)));}
 void start(){if(closed||watching)return;watching=true;
  try{activity.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),false,observer);observerRegistered=true;activity.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),false,observer);}catch(RuntimeException unavailable){}
  try{if(manager!=null)managerRegistered=manager.addAccessibilityStateChangeListener(managerListener);}catch(RuntimeException unavailable){}
  refresh();for(long delay:new long[]{200,700,2000,5000})main.postDelayed(recheck,delay);
 }
 void stop(){watching=false;main.removeCallbacksAndMessages(null);if(observerRegistered){try{activity.getContentResolver().unregisterContentObserver(observer);}catch(RuntimeException ignored){}observerRegistered=false;}if(managerRegistered){try{manager.removeAccessibilityStateChangeListener(managerListener);}catch(RuntimeException ignored){}managerRegistered=false;}}
 /** Requests a public settings page. A resolved Intent is not proof that an OEM selected the row. */
 String openSettings(){if(closed||activity.isFinishing())return "failed";String own=component.flattenToString();Bundle args=new Bundle();args.putString(":settings:fragment_args_key",own);Intent focused=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).putExtra(":settings:fragment_args_key",own).putExtra(":settings:show_fragment_args",args);
  if(launch(focused))return "accessibility";
  if(launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))return "accessibility";
  if(launch(new Intent(Settings.ACTION_SETTINGS)))return "settings";
  return "failed";
 }
 private boolean launch(Intent intent){try{activity.startActivity(intent,AppLauncher.fullScreen());return true;}catch(ActivityNotFoundException|SecurityException unavailable){return false;}}
 void close(){if(closed)return;stop();closed=true;}
}
