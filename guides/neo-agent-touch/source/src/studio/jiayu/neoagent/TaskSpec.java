package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.util.*;
/** App identity is resolved locally; original user text is never replaced by a planner claim. */
final class TaskSpec {
 final String app,pkg,goal,target,original,source;final StageSourceRules.Clause sourceClause;final boolean checkout;String state="pending",result="";boolean verified;JSONObject proof=new JSONObject();
 TaskSpec(Context c,JSONObject o,String user)throws Exception{this(c,o,user,AppTaskScope.spans(c,user));}
 TaskSpec(Context c,JSONObject o,String user,List<StageSourceRules.AppSpan> spans)throws Exception{
  app=o.getString("app").trim();goal=PrivateText.redact(o.getString("goal").trim());target=o.optString("target","").trim();
  if(app.isEmpty()||app.length()>80||goal.isEmpty()||goal.length()>2400||target.length()>120||PrivateText.credential(goal))throw new Exception("任务计划超出范围，请重新描述");
  pkg=Conversation.resolve(c,app);AppAccess access=new AppAccess(c);access.require(pkg);AppTaskScope.Binding binding=AppTaskScope.bind(c,pkg,user,readSource(o),spans);source=binding.source;sourceClause=binding.clause;original=binding.original;
  if(WorkflowIntentRules.conflict(access.planCategory(pkg),original,goal,binding.explicit))throw new WorkflowPlanException("category_conflict");
  if(WorkflowIntentRules.independentMusic(goal)&&!WorkflowIntentRules.positivePlayback(original))throw new WorkflowPlanException("source_denied");
  checkout=new TaskRequest(pkg,original,target,o.optBoolean("checkout")).checkout;
 }
 static String readSource(JSONObject o)throws Exception{if(!o.has("source"))return "";Object value=o.get("source");if(!(value instanceof String))throw new WorkflowPlanException("source_invalid");return (String)value;}
 JSONObject json()throws Exception{JSONObject out=new JSONObject().put("app",app).put("package",pkg).put("goal",goal).put("target",target).put("checkout",checkout).put("state",state).put("result",result).put("verified",verified).put("proof",proof);if(!source.isEmpty())out.put("source",source);return out;}
 Config config(Config base,int steps){Config c=new Config();c.endpoint=base.endpoint;c.model=base.model;c.protocol=base.protocol;c.key=base.key;c.vision=base.vision;c.maxSteps=(DocumentScope.applies(pkg)||checkout)?Math.max(steps,40):steps;c.maxSteps=GameplayIntent.steps(original,c.maxSteps);c.checkout=checkout;return c;}
}
