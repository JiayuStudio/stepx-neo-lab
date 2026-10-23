package studio.jiayu.neoagent;
import java.util.*;

/** Public, fixed regression inputs. No planner, Android service or network is invoked. */
public final class SharedAppSourceRulesTest {
 static int checks;
 static final String[] SHOPS={"京东","淘宝","拼多多"};
 static void yes(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 interface Attempt{void run()throws Exception;}
 static void reject(String code,Attempt attempt)throws Exception{try{attempt.run();throw new AssertionError("unexpectedly accepted "+code);}catch(WorkflowPlanException expected){yes(code.equals(expected.code()),"wrong rejection category");}}
 // Deliberately independent of PlanCoverage: these are literal spans supplied to detect's public contract.
 static List<StageSourceRules.AppSpan> literals(String user,String... names){
  ArrayList<StageSourceRules.AppSpan> out=new ArrayList<>();int from=0;
  for(String name:names){int at=user.indexOf(name,from);if(at<0)throw new AssertionError("bad fixed fixture");out.add(new StageSourceRules.AppSpan(name,at,at+name.length()));from=at+name.length();}return out;
 }
 static List<StageSourceRules.AppSpan> productionSpans(String user,String... selected){
  ArrayList<StageSourceRules.AppSpan> out=new ArrayList<>();for(PlanCoverage.Mention m:PlanCoverage.mentions(user,Arrays.asList(selected)))out.add(new StageSourceRules.AppSpan(m.app,m.start,m.end));return out;
 }
 static SharedAppSourceRules.Group positive(String user,String target,String... names){
  List<StageSourceRules.AppSpan> input=literals(user,names);SharedAppSourceRules.Group group=SharedAppSourceRules.detect(user,input);
  yes(group!=null,"shared price query was rejected");yes(group.source.equals(user.trim()),"source was shortened or rewritten");yes(group.target.equals(target),"common exact model changed");yes(group.members.size()==names.length,"list member lost");
  for(int i=0;i<names.length;i++){StageSourceRules.AppSpan member=group.member(names[i]);yes(member!=null&&member.start==input.get(i).start&&member.end==input.get(i).end,"member identity or source position changed");}
  yes(group.member("未列出的商城")==null,"unlisted app acquired shared scope");return group;
 }
 static void negative(String user,String... names){yes(SharedAppSourceRules.detect(user,literals(user,names))==null,"non-shared intent became a shared price query");}
 static List<StageSourceRules.Clause> bindOrder(String user,List<StageSourceRules.AppSpan> spans,String... order)throws Exception{
  ArrayList<StageSourceRules.Clause> bound=new ArrayList<>();for(String app:order)bound.add(StageSourceRules.bind(user,user.trim(),app,spans));return bound;
 }
 static List<PlanCoverage.Stage> stages(SharedAppSourceRules.Group group,String... order){ArrayList<PlanCoverage.Stage> out=new ArrayList<>();for(String app:order)out.add(new PlanCoverage.Stage(app,group.goal(app)));return out;}
 public static void main(String[] args)throws Exception{
  String screenshot="去三个购物平台京东淘宝拼多多看看怎样购买海信g7pro最便宜";
  SharedAppSourceRules.Group shared=positive(screenshot,"海信g7pro",SHOPS);
  positive("  请去三个购物平台 京东 淘宝 拼多多 看看怎样购买海信 G7 Pro 最便宜  ","海信 G7 Pro",SHOPS);
  for(String separator:new String[]{"、","，",",","和","以及","/","＋"})positive("去京东"+separator+"淘宝"+separator+"拼多多查看海信g7pro价格","海信g7pro",SHOPS);
  positive("去京东、淘宝及拼多多搜索「海信 G7 Pro」的价格","海信 G7 Pro",SHOPS);
  positive("去甲选商城乙选商城丙选商城查看示例显示器Q8价格","示例显示器Q8","甲选商城","乙选商城","丙选商城");
  String limited="去京东、淘宝和拼多多查看海信G7 Pro的到手价，含运费，预算3000元，只查价，不下单，不付款，不加购";
  SharedAppSourceRules.Group constraints=positive(limited,"海信G7 Pro",SHOPS);
  for(String app:SHOPS){String goal=constraints.goal(app);yes(goal.contains("「海信G7 Pro」"),"stage lost the same exact model");yes(goal.contains(limited),"stage lost raw budget or prohibitions");}
  yes(!ShoppingIntent.preview(screenshot),"price question acquired checkout preview");
  yes(!PurchasePolicy.grant(screenshot,"com.taobao.taobao").order&&!PurchasePolicy.grant(screenshot,"com.jingdong.app.mall").order,"price question acquired actual purchase authority");

  // Exercise the real mention -> source -> order -> coverage chain, rather than a second parser.
  List<PlanCoverage.Mention> mentions=PlanCoverage.mentions(screenshot,Arrays.asList(SHOPS));List<StageSourceRules.AppSpan> spans=productionSpans(screenshot,SHOPS);
  yes(mentions.size()==3&&spans.size()==3,"consecutive unpunctuated platform list was not recognized");
  for(int i=0;i<SHOPS.length;i++)yes(mentions.get(i).app.equals(SHOPS[i]),"platform list order changed");
  List<StageSourceRules.Clause> clauses=StageSourceRules.clauses(screenshot,spans);yes(clauses.size()==1&&clauses.get(0).source.equals(screenshot),"shared source split into noun fragments");
  List<StageSourceRules.Clause> bound=bindOrder(screenshot,spans,SHOPS);
  for(int i=0;i<bound.size();i++){yes(bound.get(i).source.equals(screenshot),"member source is not the complete original");yes(bound.get(i).order==spans.get(i).start,"member ordering is not its original list position");}
  StageSourceRules.ordered(spans,Arrays.asList(SHOPS),bound);checks++;
  List<String> identities=Arrays.asList(SHOPS);yes(PlanCoverage.issue(screenshot,mentions,identities,stages(shared,SHOPS)).isEmpty(),"complete shared plan failed coverage");
  for(int omitted=0;omitted<SHOPS.length;omitted++){
   ArrayList<String> remaining=new ArrayList<>(identities);remaining.remove(omitted);
   yes(!PlanCoverage.issue(screenshot,mentions,identities,stages(shared,remaining.toArray(new String[0]))).isEmpty(),"missing requested platform passed coverage");
   ArrayList<StageSourceRules.AppSpan> incomplete=new ArrayList<>(literals(screenshot,SHOPS));incomplete.remove(omitted);
   yes(SharedAppSourceRules.detect(screenshot,incomplete)==null,"partial app list acquired full shared source");
  }
  String[] reversed={"淘宝","京东","拼多多"};
  reject("source_order",()->StageSourceRules.ordered(spans,Arrays.asList(reversed),bindOrder(screenshot,spans,reversed)));
  yes(!PlanCoverage.issue(screenshot,mentions,identities,stages(shared,reversed)).isEmpty(),"reversed shared platform plan passed coverage");
  reject("source_app_mismatch",()->StageSourceRules.bind(screenshot,screenshot,"未列出的商城",spans));
  reject("source_invalid",()->StageSourceRules.bind(screenshot,"看看怎样购买海信g7pro最便宜","京东",spans));
  List<StageSourceRules.AppSpan> limitedSpans=productionSpans(limited,SHOPS);
  reject("source_invalid",()->StageSourceRules.bind(limited,limited.replace("预算3000元，",""),"淘宝",limitedSpans));
  List<StageSourceRules.AppSpan> reverseSpans=new ArrayList<>(literals(screenshot,SHOPS));Collections.reverse(reverseSpans);yes(SharedAppSourceRules.detect(screenshot,reverseSpans)==null,"unordered caller spans were trusted");

  // Independent goals retain separate scopes and legitimate revisits retain all stages.
  String independent="先在京东搜索显示器，然后在淘宝搜索咖啡豆，最后回到京东搜索键盘";
  List<StageSourceRules.AppSpan> independentApps=productionSpans(independent,"京东","淘宝");
  yes(SharedAppSourceRules.detect(independent,independentApps)==null,"independent goals were combined");
  List<StageSourceRules.Clause> separate=StageSourceRules.clauses(independent,independentApps);yes(separate.size()==3,"legal app revisit was collapsed");
  reject("source_app_mismatch",()->StageSourceRules.bind(independent,independent,"京东",independentApps));
  reject("source_app_mismatch",()->StageSourceRules.bind(independent,separate.get(0).source,"淘宝",independentApps));
  List<String> revisit=Arrays.asList("京东","淘宝","京东");ArrayList<StageSourceRules.Clause> revisitSources=new ArrayList<>();for(int i=0;i<revisit.size();i++)revisitSources.add(StageSourceRules.bind(independent,separate.get(i).source,revisit.get(i),independentApps));
  StageSourceRules.ordered(independentApps,revisit,revisitSources);checks++;
  List<PlanCoverage.Mention> revisitMentions=PlanCoverage.mentions(independent,Arrays.asList("京东","淘宝"));
  yes(PlanCoverage.issue(independent,revisitMentions,revisit,Arrays.asList(new PlanCoverage.Stage("京东","搜索显示器"),new PlanCoverage.Stage("淘宝","搜索咖啡豆"),new PlanCoverage.Stage("京东","搜索键盘"))).isEmpty(),"complete legal revisit failed coverage");
  yes(!PlanCoverage.issue(independent,revisitMentions,revisit,Arrays.asList(new PlanCoverage.Stage("京东","搜索显示器和键盘"),new PlanCoverage.Stage("淘宝","搜索咖啡豆"))).isEmpty(),"earlier app stage replaced a later revisit");
  negative("先在京东查看显示器价格，然后在淘宝查看咖啡豆价格","京东","淘宝");
  negative(screenshot+"，然后在WPS新建合同",SHOPS);

  // Alternative, negated, quoted and transactional instructions are not shared read-only grants.
  negative("不要去京东淘宝拼多多查看海信g7pro价格",SHOPS);
  negative("去京东或淘宝或拼多多看看海信g7pro哪里便宜",SHOPS);
  negative("在WPS写下「去京东淘宝拼多多看看海信g7pro价格」",SHOPS);
  negative("去京东淘宝拼多多搜索「海信g7pro最便宜购买教程」",SHOPS);
  negative("去京东淘宝拼多多看看海信g7pro怎么样",SHOPS);
  negative("去京东淘宝拼多多正式购买海信g7pro，选最便宜的",SHOPS);
  negative("去京东淘宝拼多多查看海信g7pro价格并加入购物车",SHOPS);
  negative("去京东淘宝拼多多比价海信g7pro，找到最低价后加购",SHOPS);
  negative("请去京东淘宝拼多多实际购买一件纸巾，预算5元，选最便宜的",SHOPS);
  negative("去京东淘宝拼多多查看海信g7pro价格，选最低价后付款",SHOPS);
  negative("去京东淘宝拼多多查询海信g7pro和示例显示器Q8价格",SHOPS);
  String repeated="去京东淘宝京东查看海信g7pro价格";negative(repeated,"京东","淘宝","京东");
  yes(SharedAppSourceRules.detect(screenshot,Collections.emptyList())==null,"empty app list acquired scope");
  yes(SharedAppSourceRules.detect(null,Collections.emptyList())==null,"null user text acquired scope");
  System.out.println("PASS: "+checks+" shared comparison source, exact target, coverage, order and negative-scope checks");
 }
}
