package studio.jiayu.neoagent;
/** Deadlines for stages that can otherwise look alive while doing no useful work. */
final class HealthRules {
 static long budget(String phase){if("starting".equals(phase))return 15000;if("observing".equals(phase))return 15000;if("loading".equals(phase))return 45000;if("deciding".equals(phase))return 75000;if("dispatching".equals(phase))return 10000;return 0;}
 static String failure(boolean running,long heartbeatAge,String phase,long phaseAge,long outsideAge){if(!running)return "";if(heartbeatAge>3500)return "界面心跳中断 · 原目标保留";if(outsideAge>5000)return "目标应用持续不在可操作前台 · 原目标保留";long budget=budget(phase);if(budget>0&&phaseAge>budget)return "observing".equals(phase)?"界面观察超时 · 原目标保留":"deciding".equals(phase)?"模型响应超时 · 原目标保留":"动作阶段超时 · 原目标保留";return "";}
}
