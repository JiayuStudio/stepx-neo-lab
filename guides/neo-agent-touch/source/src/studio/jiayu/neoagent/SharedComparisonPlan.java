package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.util.*;

/** Compile a locally recognized shared query; no model flag grants the shared scope. */
final class SharedComparisonPlan {
 static JSONObject create(Context c,String user)throws Exception{
  List<StageSourceRules.AppSpan> spans=AppTaskScope.spans(c,user);SharedAppSourceRules.Group group=SharedAppSourceRules.detect(user,spans);if(group==null)return null;
  AppAccess access=new AppAccess(c);JSONArray tasks=new JSONArray();
  for(StageSourceRules.AppSpan member:group.members){
   access.require(member.pkg);if(!"购物".equals(access.planCategory(member.pkg)))throw new WorkflowPlanException("category_conflict");
   String label=user.substring(member.start,member.end).trim();if(!member.pkg.equals(Conversation.resolve(c,label)))throw new WorkflowPlanException("source_invalid");
   tasks.put(new JSONObject().put("app",label).put("source",group.source).put("target",group.target).put("goal",group.goal(label)).put("checkout",false));
  }
  return new JSONObject().put("kind","task").put("tasks",tasks).put("reply","按平台顺序查询同一商品的价格与优惠条件");
 }
}
