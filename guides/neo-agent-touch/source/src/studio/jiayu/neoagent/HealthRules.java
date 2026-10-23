package studio.jiayu.neoagent;
/** Deadlines for stages that can otherwise look alive while doing no useful work. */
final class HealthRules {
 static long budget(String phase){if("starting".equals(phase))return 15000;if("observing".equals(phase))return 15000;if("loading".equals(phase))return 45000;if("deciding".equals(phase))return 75000;if("dispatching".equals(phase))return 10000;return 0;}
 static String failure(boolean running,long heartbeatAge,String phase,long phaseAge,long outsideAge){if(!running)return "";if(heartbeatAge>3500)return "界面心跳中断 · 原目标保留";if(outsideAge>5000)return "目标应用持续不在可操作前台 · 原目标保留";long budget=budget(phase);if(budget>0&&phaseAge>budget)return "observing".equals(phase)?"界面观察超时 · 原目标保留":"deciding".equals(phase)?"模型响应超时 · 原目标保留":"动作阶段超时 · 原目标保留";return "";}
 /** Diagnostic codes only; never export an arbitrary phase string or stack file name. */
 static String phaseCode(String phase){return "idle".equals(phase)||"starting".equals(phase)||"observing".equals(phase)||"loading".equals(phase)||"deciding".equals(phase)||"dispatching".equals(phase)||"waiting".equals(phase)||"paused".equals(phase)||"disconnected".equals(phase)?phase:"unknown";}
 static String[] diagnosticFrames(StackTraceElement[] stack){
  java.util.ArrayList<String> safe=new java.util.ArrayList<>();
  if(stack!=null)for(int i=0;i<stack.length&&i<64&&safe.size()<8;i++){
   StackTraceElement frame=stack[i];if(frame==null)continue;String type=frame.getClassName(),method=frame.getMethodName();
   boolean approved=type.startsWith("studio.jiayu.neoagent.")||type.startsWith("android.")||type.startsWith("com.android.")||type.startsWith("java.")||type.startsWith("javax.")||type.startsWith("dalvik.")||type.startsWith("libcore.");
   if(!approved||type.length()>180||method.length()>120||!type.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+")||!method.matches("[A-Za-z_$][A-Za-z0-9_$]*|<init>|<clinit>"))continue;
   safe.add(type+"."+method+":"+frame.getLineNumber());
  }
  return safe.toArray(new String[0]);
 }
 static boolean diagnosticScope(String run,long sourceGeneration,long pausedGeneration,String currentRun,long currentGeneration,boolean paused){return run!=null&&!run.isEmpty()&&run.equals(currentRun)&&(sourceGeneration==currentGeneration||paused&&pausedGeneration==currentGeneration);}
}
