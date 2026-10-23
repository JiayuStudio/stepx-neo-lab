package studio.jiayu.neoagent;
/** Keep model-only repair guidance separate from the fixed user-facing fallback. */
final class WorkflowPlanException extends Exception {
 private static final String USER_MESSAGE="这次未能完成任务规划，尚未开始操作。请重试。";
 private final String code;
 WorkflowPlanException(String value){super(USER_MESSAGE);code=known(value)?value:"source_invalid";}
 String code(){return code;}
 String safeFeedback(){return feedback(code);}
 String userMessage(){return USER_MESSAGE;}
 private static boolean known(String c){return "source_required".equals(c)||"source_invalid".equals(c)||"source_order".equals(c)||"source_app_mismatch".equals(c)||"source_context".equals(c)||"source_denied".equals(c)||"category_conflict".equals(c);}
 private static String feedback(String c){
  if("category_conflict".equals(c))return "任务目标与所选应用类别明显冲突。请从完整已授权候选中选择适合的应用，保留用户明确指定的应用与原顺序，不新增授权。";
  if("source_required".equals(c))return "未明确点名应用的任务缺少原文绑定。请为该步提供本机给出的完整source子句，不用改写后的goal替代。";
  if("source_order".equals(c))return "任务source顺序与用户原文不符。请按原文顺序规划，保留合法的应用重访。";
  if("source_app_mismatch".equals(c))return "任务source包含其他应用的明确指令。请绑定本步自己的完整原文子句，不跨应用借用目标。";
  if("source_context".equals(c))return "原文存在不能安全分离的否定或预算限制。请保留全部限制；无法绑定时简短追问，不自行扩大任务范围。";
  if("source_denied".equals(c))return "该播放目标与原文的否定或文字内容范围冲突。请保留否定及文字用途，不执行被排除的播放。";
  return "任务source不是本机提供的完整原文子句，或匹配不唯一。请逐字使用完整source，保留否定、预算和停止条件，不截取内部短语。";
 }
}
