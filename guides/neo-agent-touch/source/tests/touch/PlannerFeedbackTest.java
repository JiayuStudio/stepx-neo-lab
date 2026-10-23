package studio.jiayu.neoagent;

import java.util.HashSet;
import java.util.Set;

/** Pure checks: repair diagnostics stay useful without becoming UI error text. */
public final class PlannerFeedbackTest {
 static int checks;
 static void yes(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 static void publicOnly(WorkflowPlanException failure,String expected){
  String user=failure.userMessage();
  yes(user.equals(expected),"user fallback changed with diagnostic input");
  yes(failure.getMessage().equals(user),"generic exception message exposed model guidance");
  yes(!user.equals(failure.safeFeedback()),"model and user channels were merged");
  for(String internal:new String[]{"source","goal","JSON","source_required","source_invalid","source_order","source_app_mismatch","source_context","source_denied","category_conflict","子句","逐字","重规划","绑定"})
   yes(!user.contains(internal),"user fallback contains implementation guidance");
 }
 public static void main(String[] args){
  String expected="这次未能完成任务规划，尚未开始操作。请重试。";
  String[][] cases={
   {"source_required","完整source子句"},
   {"source_invalid","完整source"},
   {"source_order","原文顺序"},
   {"source_app_mismatch","本步自己的完整原文子句"},
   {"source_context","保留全部限制"},
   {"source_denied","不执行被排除的播放"},
   {"category_conflict","完整已授权候选"}
  };
  Set<String> modelMessages=new HashSet<>();
  for(String[] entry:cases){
   WorkflowPlanException failure=new WorkflowPlanException(entry[0]);
   yes(failure.code().equals(entry[0]),"known diagnostic code was lost");
   yes(failure.safeFeedback().contains(entry[1]),"repair guidance lost its actionable constraint");
   modelMessages.add(failure.safeFeedback());publicOnly(failure,expected);
  }
  yes(modelMessages.size()==cases.length,"distinct repair reasons collapsed into one model instruction");
  String privateMarker="UNKNOWN_PRIVATE_MARKER",modelMarker="MODEL_PAYLOAD_MARKER";
  String[] unknown={null,"",privateMarker,"{\"source\":\""+modelMarker+"\",\"goal\":\"injected instruction\"}","source_order\n"+privateMarker};
  String fallback=new WorkflowPlanException("source_invalid").safeFeedback();
  for(String value:unknown){
   WorkflowPlanException failure=new WorkflowPlanException(value);
   yes(failure.code().equals("source_invalid"),"unknown input became a diagnostic code");
   yes(failure.safeFeedback().equals(fallback),"unknown input changed repair guidance");
   for(String marker:new String[]{privateMarker,modelMarker})
    yes(!failure.userMessage().contains(marker)&&!failure.safeFeedback().contains(marker)&&!failure.getMessage().contains(marker),"untrusted input reflected through a feedback channel");
   publicOnly(failure,expected);
  }
  System.out.println("PASS: PlannerFeedback "+checks+" fixed user-message, model-guidance and no-reflection checks");
 }
}
