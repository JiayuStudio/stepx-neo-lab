package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.util.*;
/** Locally bind each selected app's exact label/package to its own original clause. */
final class AppTaskScope {
 static final class Binding {final String source,original;final StageSourceRules.Clause clause;final boolean explicit;Binding(String s,String o,StageSourceRules.Clause c,boolean e){source=s;original=o;clause=c;explicit=e;}}
 static List<PlanCoverage.Mention> mentions(Context c,String user)throws Exception{
  AppNameRules.Snapshot local=Conversation.localApps(c);List<PlanCoverage.Mention> mentions=PlanCoverage.mentions(user,local.names());
  // Explicit app identities stay local. An unselected exact app cannot disappear from source coverage.
  for(PlanCoverage.Mention mention:mentions)local.resolve(mention.app);return mentions;
 }
 static String original(Context c,String pkg,String user)throws Exception{
  List<PlanCoverage.Mention> all=mentions(c,user);Set<String> matching=new HashSet<>();for(PlanCoverage.Mention m:all)if(pkg.equals(Conversation.resolve(c,m.app)))matching.add(m.app);
  return TaskScope.forNames(matching,user,all);
 }
 static List<StageSourceRules.AppSpan> spans(Context c,String user)throws Exception{ArrayList<StageSourceRules.AppSpan> result=new ArrayList<>();for(PlanCoverage.Mention m:mentions(c,user)){String pkg;try{pkg=Conversation.resolve(c,m.app);}catch(Exception unavailable){pkg="unresolved:"+m.app;}result.add(new StageSourceRules.AppSpan(pkg,m.start,m.end));}return result;}
 static Binding bind(Context c,String pkg,String user,String source,List<StageSourceRules.AppSpan> spans)throws Exception{
  SharedAppSourceRules.Group shared=SharedAppSourceRules.detect(user,spans);if(source.isEmpty()&&shared!=null&&shared.member(pkg)!=null)source=shared.source;
  if(source.isEmpty()){
   if(!StageSourceRules.explicit(spans,pkg,null)&&!c.getPackageName().equals(pkg))throw new WorkflowPlanException("source_required");
   return new Binding("",original(c,pkg,user),null,StageSourceRules.explicit(spans,pkg,null));
  }
  StageSourceRules.Clause clause=StageSourceRules.bind(user,source,pkg,spans);return new Binding(clause.source,clause.source,clause,StageSourceRules.explicit(spans,pkg,clause));
 }
 static String mergeSources(Context c,String pkg,String user,String first,String next)throws Exception{
  if(first.isEmpty()&&next.isEmpty())return "";if(first.isEmpty()||next.isEmpty())throw new WorkflowPlanException("source_invalid");List<StageSourceRules.AppSpan> spans=spans(c,user);StageSourceRules.Clause a=StageSourceRules.bind(user,first,pkg,spans),b=StageSourceRules.bind(user,next,pkg,spans);if(b.start<a.start)throw new WorkflowPlanException("source_order");return StageSourceRules.bind(user,user.substring(a.start,Math.max(a.end,b.end)),pkg,spans).source;
 }
}
