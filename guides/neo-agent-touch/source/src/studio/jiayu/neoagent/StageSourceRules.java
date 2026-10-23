package studio.jiayu.neoagent;
import java.util.*;import java.util.regex.*;
/** Exact source ranges from local boundaries. No semantic paraphrase establishes authorization. */
final class StageSourceRules {
 static final class AppSpan {final String pkg;final int start,end;AppSpan(String p,int s,int e){pkg=p;start=s;end=e;}}
 static final class Clause {final String source;final int start,end;Clause(String u,int s,int e){while(s<e&&Character.isWhitespace(u.charAt(s)))s++;while(e>s&&Character.isWhitespace(u.charAt(e-1)))e--;source=u.substring(s,e);start=s;end=e;}}
 private static final Pattern FLOW=Pattern.compile("然后|接着|最后|随后|接下来|之后|再(?=去|打开|使用|用|在|播放|搜索|听歌|查看|回到)");
 private static final Pattern NEXT=Pattern.compile("^(?:(?:又|可|就|说|请|帮我|给我|再|去|在|用|先|现在)\\s*){0,8}(?:打开|启动|进入|回到|切换|使用|播放|听歌|听音乐|搜索|搜一搜|查看|查询|新建|创建|写|修改|导航|购买|买|比价|关注|加入)");
 private static final Pattern LEAD=Pattern.compile("(?:然后|接着|最后|随后|接下来|之后|再|先)(?:(?:又|可|就|说|请|帮我|给我|现在)\\s*)*$");
 private static final Pattern RESTRICTION=Pattern.compile("不要|不用|不得|不能|禁止|不允许|不必|无需|不需要|不买|不购|不付|不下单|不提交|不发送|只查|仅查|只看|仅看|预算|不超过|最多|至多|限额|[0-9一二三四五六七八九十百]+(?:元|块)以内");
 private static final Pattern GLOBAL=Pattern.compile("全程|所有(?:任务|步骤|操作)|这些(?:任务|操作)|以上(?:任务|操作)|一律|都(?:不要|不用|不得|不能|只|不)|总预算|总共|合计");
 static List<Clause> clauses(String user,List<AppSpan> apps){
  String u=user==null?"":user;TreeSet<Integer> cuts=new TreeSet<>();cuts.add(0);cuts.add(u.length());
  int firstApp=Integer.MAX_VALUE;for(AppSpan a:apps)firstApp=Math.min(firstApp,a.start);
  for(AppSpan a:apps)if(a.start>0&&a.start<u.length()){
   // A budget/polite prefix belongs to the first app clause; it is not a standalone task.
   String prefix=u.substring(0,a.start);if(a.start==firstApp&&!prefix.matches("(?s).*(?:打开|搜索|播放|听歌|新建|创建|修改|查询|导航|购买).*"))continue;
   cuts.add(expand(u,a.start));
  }
  Matcher flow=FLOW.matcher(u);while(flow.find())if(!PlanCoverage.inQuote(u,flow.start())&&NEXT.matcher(u.substring(flow.end())).find())cuts.add(flow.start());
  ArrayList<Clause> out=new ArrayList<>();Integer prior=null;for(Integer cut:cuts){if(prior!=null&&cut>prior){Clause c=new Clause(u,prior,cut);if(!c.source.isEmpty())out.add(c);}prior=cut;}return out;
 }
 private static int expand(String user,int start){Matcher m=LEAD.matcher(user.substring(0,start));return m.find()&&!PlanCoverage.inQuote(user,m.start())?m.start():start;}
 static Clause bind(String user,String source,String pkg,List<AppSpan> apps)throws WorkflowPlanException{
  if(source==null||source.isEmpty())throw new WorkflowPlanException("source_required");
  List<Clause> list=clauses(user,apps);Clause found=null;
  // A stage may include adjacent same-app operations, but must keep every character between boundaries.
  for(int i=0;i<list.size();i++)for(int j=i;j<list.size();j++){Clause candidate=new Clause(user,list.get(i).start,list.get(j).end);if(!candidate.source.equals(source))continue;if(found!=null)throw new WorkflowPlanException("source_invalid");found=candidate;}
  if(found==null)throw new WorkflowPlanException("source_invalid");
  for(AppSpan a:apps)if(a.start<found.end&&a.end>found.start&&!pkg.equals(a.pkg))throw new WorkflowPlanException("source_app_mismatch");
  // Do not silently lose an unscoped/global financial or negative qualification in another range.
  for(Clause c:list){if(c.start>=found.start&&c.end<=found.end)continue;Matcher r=RESTRICTION.matcher(c.source);if(!r.find())continue;
   int firstApp=Integer.MAX_VALUE;for(AppSpan a:apps)if(a.start>=c.start&&a.start<c.end)firstApp=Math.min(firstApp,a.start-c.start);
   if(firstApp==Integer.MAX_VALUE||r.start()<firstApp||GLOBAL.matcher(c.source).find())throw new WorkflowPlanException("source_context");
  }
  return found;
 }
 static boolean explicit(List<AppSpan> apps,String pkg,Clause source){for(AppSpan a:apps)if(pkg.equals(a.pkg)&&(source==null||a.start>=source.start&&a.end<=source.end))return true;return false;}
 static void ordered(List<AppSpan> apps,List<String> packages,List<Clause> sources)throws WorkflowPlanException{
  int cursor=-1;String previous="";
  for(int i=0;i<packages.size();i++){String pkg=packages.get(i);Clause source=sources.get(i);int position=Integer.MAX_VALUE;
   if(source!=null)position=source.start;
   else for(AppSpan a:apps)if(pkg.equals(a.pkg)&&a.start>=cursor&&(a.start>cursor||pkg.equals(previous)))position=Math.min(position,a.start);
   if(position==Integer.MAX_VALUE||position<cursor||position==cursor&&!pkg.equals(previous))throw new WorkflowPlanException("source_order");cursor=position;previous=pkg;
  }
 }
}
