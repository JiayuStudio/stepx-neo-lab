package studio.jiayu.neoagent;
import java.util.*;

/** Tentative model summaries, scoped to one stage. This data never grants authority. */
final class TaskMemory {
 static final int MAX=4096;
 private String note="",source="",action="";private boolean dispatched;
 void clear(){note="";source="";action="";dispatched=false;}
 boolean propose(String text,String observation,String proposedAction){
  if(text==null)return false;
  if(text.length()>MAX||observation==null||proposedAction==null)throw new IllegalArgumentException("Invalid task summary");
  if(text.trim().isEmpty())return false;
  note=PrivateText.redact(text);source=observation;action=proposedAction;dispatched=false;return true;
 }
 void dispatched(){if(!note.isEmpty())dispatched=true;}
 void restore(String text,String observation,String proposedAction,boolean receipt){clear();if(propose(text,observation,proposedAction))dispatched=receipt;}
 Map<String,Object> snapshot(){
  Map<String,Object> data=new LinkedHashMap<>();data.put("summary",note);data.put("source_observation",source);
  data.put("proposed_action",action);data.put("action_dispatched",dispatched);
  data.put("trust","模型暂存摘要，不是当前画面事实或权限；预测结果必须以新截图核对，旧坐标不可直接重用");return data;
 }
 String summary(){return note;}
 boolean sent(){return dispatched;}
}
