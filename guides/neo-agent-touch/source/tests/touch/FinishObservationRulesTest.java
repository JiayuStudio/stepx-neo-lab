package studio.jiayu.neoagent;

public final class FinishObservationRulesTest {
 static int passed;static void check(boolean condition){if(!condition)throw new AssertionError("Finish observation check "+passed);passed++;}
 static final String GOAL="在大麦搜索“测试艺人”，打开一条相关演出查看详情。";
 static FinishObservationRules.Scope scope(String run,String pkg,long epoch,long revision,int window,boolean running,boolean current,boolean expected,boolean covered,int right){return new FinishObservationRules.Scope(run,pkg,epoch,revision,window,0,0,right,2000,running,current,expected,covered);}
 static FinishObservationRules.Scope normal(){return scope("test-run","cn.damai",4,7,8,true,true,true,false,1000);}
 static FinishObservationRules.Anchor anchor(String resource,String identity,String label,boolean refreshed,boolean visible,boolean unique,int window,int right){return new FinishObservationRules.Anchor("cn.damai",resource,"android.widget.LinearLayout",identity,"",label,window,10,100,right,200,visible,true,unique,refreshed);}
 static FinishObservationRules.Anchor title(){return anchor(FinishObservationRules.TITLE,"frozen-title-identity","城市·测试艺人巡回演唱会",true,true,true,8,990);}
 static FinishObservationRules.Anchor venue(){return anchor(FinishObservationRules.VENUE,"frozen-venue-identity","测试场馆",true,true,true,8,700);}
 static FinishObservationRules.Verdict evaluate(String pkg,String goal,boolean checkout,boolean purchase,boolean document,FinishObservationRules.Scope source,FinishObservationRules.Scope fresh,FinishObservationRules.Anchor st,FinishObservationRules.Anchor ft,FinishObservationRules.Anchor sv,FinishObservationRules.Anchor fv){FinishObservationRules.Verdict result=FinishObservationRules.evaluate(pkg,goal,checkout,purchase,document,source,fresh,st,ft,sv,fv);check(!result.verified);return result;}
 static FinishObservationRules.Verdict evaluate(FinishObservationRules.Scope fresh,FinishObservationRules.Anchor ft,FinishObservationRules.Anchor fv){return evaluate("cn.damai",GOAL,false,false,false,normal(),fresh,title(),ft,venue(),fv);}
 public static void main(String[] args){
  check(evaluate(normal(),title(),venue()).canReport);
  for(String goal:new String[]{"搜索测试艺人，打开演出查看详情。","搜索“测试艺人”","搜索“测试艺人”，打开详情。","搜索“测试艺人”，打开演出详情并关注。","搜索“测试艺人”，打开演出详情并购票。","搜索“测试艺人”，打开演出详情并付款。","搜索“测试艺人”，打开演出详情并修改文档。","搜索“测试艺人”，打开演出详情，再搜索“其他艺人”。","查看演出详情，然后搜索“测试艺人”。","搜索“a”，打开演出详情。"})check(!evaluate("cn.damai",goal,false,false,false,normal(),normal(),title(),title(),venue(),venue()).canReport);
  for(String pkg:new String[]{"other.app","cn.wps.moffice_eng","com.taobao.taobao"})check(!evaluate(pkg,GOAL,false,false,false,normal(),normal(),title(),title(),venue(),venue()).canReport);
  for(int flag=0;flag<3;flag++)check(!evaluate("cn.damai",GOAL,flag==0,flag==1,flag==2,normal(),normal(),title(),title(),venue(),venue()).canReport);
  FinishObservationRules.Scope[] stale={null,scope("other-run","cn.damai",4,7,8,true,true,true,false,1000),scope("test-run","cn.damai",5,7,8,true,true,true,false,1000),scope("test-run","cn.damai",4,8,8,true,true,true,false,1000),scope("test-run","cn.damai",4,7,9,true,true,true,false,1000),scope("test-run","other.app",4,7,8,true,true,true,false,1000),scope("test-run","cn.damai",4,7,8,false,true,true,false,1000),scope("test-run","cn.damai",4,7,8,true,false,true,false,1000),scope("test-run","cn.damai",4,7,8,true,true,false,false,1000),scope("test-run","cn.damai",4,7,8,true,true,true,true,1000),scope("test-run","cn.damai",4,7,8,true,true,true,false,999)};
  for(FinishObservationRules.Scope item:stale)check(!evaluate(item,title(),venue()).canReport);
  FinishObservationRules.Anchor[] invalid={null,anchor(FinishObservationRules.TITLE,"frozen-title-identity","城市·测试艺人巡回演唱会",false,true,true,8,990),anchor(FinishObservationRules.TITLE,"frozen-title-identity","城市·测试艺人巡回演唱会",true,false,true,8,990),anchor(FinishObservationRules.TITLE,"frozen-title-identity","城市·测试艺人巡回演唱会",true,true,false,8,990),anchor(FinishObservationRules.TITLE,"changed","城市·测试艺人巡回演唱会",true,true,true,8,990),anchor(FinishObservationRules.TITLE,"frozen-title-identity","其他艺人演唱会",true,true,true,8,990),anchor(FinishObservationRules.TITLE,"frozen-title-identity","城市·测试艺人巡回演唱会",true,true,true,9,990),anchor(FinishObservationRules.TITLE,"frozen-title-identity","城市·测试艺人巡回演唱会",true,true,true,8,989),anchor("cn.damai:id/other","frozen-title-identity","城市·测试艺人巡回演唱会",true,true,true,8,990)};
  for(FinishObservationRules.Anchor item:invalid)check(!evaluate(normal(),item,venue()).canReport);
  check(!evaluate(normal(),title(),null).canReport);check(!evaluate(normal(),title(),anchor(FinishObservationRules.VENUE,"frozen-venue-identity","其他场馆",true,true,true,8,700)).canReport);
  FinishObservationRules.Anchor absentTarget=anchor(FinishObservationRules.TITLE,"same","其他艺人巡回演唱会",true,true,true,8,990);check(!evaluate("cn.damai",GOAL,false,false,false,normal(),normal(),absentTarget,absentTarget,venue(),venue()).canReport);
  check("测试艺人".equals(FinishObservationRules.requestedObject("cn.damai","在大麦搜索「测试艺人」，查看演出详情。",false,false,false)));
  System.out.println("PASS: "+passed+" narrow read-only finish checks; never verified, action paths unchanged");
 }
}
