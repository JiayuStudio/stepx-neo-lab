package studio.jiayu.neoagent;

/** System selection and an actual service callback are separate pieces of evidence. */
public final class AccessibilityStateRules {
 private AccessibilityStateRules(){}
 public static boolean connected(boolean instancePresent,boolean instanceIsCurrent,boolean callbackConnected){return instancePresent&&instanceIsCurrent&&callbackConnected;}
 public static String state(boolean registeredKnown,boolean registered,boolean enabledKnown,boolean enabled,boolean connected){
  // A current callback is stronger evidence than a stale or temporarily unavailable settings query.
  if(connected)return "connected";
  if(registeredKnown&&!registered)return "missing";
  if(!registeredKnown||!enabledKnown)return "unknown";
  return enabled?"enabled_waiting":"disabled";
 }
 public static String status(String state){switch(state){case "connected":return "控制服务已连接";case "enabled_waiting":return "系统已启用，等待控制服务连接";case "disabled":return "系统无障碍未启用";case "missing":return "系统未找到控制服务";default:return "暂时无法核对系统无障碍状态";}}
 public static String marker(boolean registeredKnown,boolean registered,boolean enabledKnown,boolean enabled,boolean connected){return state(registeredKnown,registered,enabledKnown,enabled,connected)+"|"+registeredKnown+"|"+registered+"|"+enabledKnown+"|"+enabled+"|"+connected;}
}
