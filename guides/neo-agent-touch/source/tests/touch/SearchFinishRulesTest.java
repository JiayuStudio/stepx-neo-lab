package studio.jiayu.neoagent;

final class SearchFinishRulesTest {
 static int checks;
 static void yes(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 static SearchFinishRules.Decision check(SearchFinishRules rules,String run,long epoch,Object key,String text,boolean focused,boolean readable,boolean pending,boolean submitted){return rules.beforeFinish(run,epoch,key,text,focused,readable,pending,submitted);}
 interface Job{void run()throws Exception;}
 static void denied(Job job,String why)throws Exception{boolean rejected=false;try{job.run();}catch(SearchSubmitRules.Rejected expected){rejected=true;}yes(rejected,why);}
 static final class Port implements SearchSubmitRules.Port {
  final SearchFinishRules rules;final Object editor;String text;int dispatches;
  Port(SearchFinishRules r,Object e){rules=r;editor=e;}
  public SearchInputRules.Sample read(){return new SearchInputRules.Sample(text,0,0,0);}
  public void selectAll(){throw new AssertionError("Complete field should not require selection");}
  public void pause(int milliseconds){}
  public void search(SearchInputRules.Sample expected,Runnable dispatching){if(!expected.text.equals(text))throw new AssertionError("Wrong sample");dispatching.run();rules.submitted("cycles",20,editor,text);dispatches++;}
 }
 static String submit(SearchFinishRules rules,Port port,Object editor)throws Exception{return SearchSubmitRules.submit(port,new SearchInputRules.Pending(),rules.submissionState("cycles",20,editor),editor,"cycles",text->{});}
 public static void main(String[] args)throws Exception{
  SearchFinishRules rules=new SearchFinishRules();Object editor=new Object(),other=new Object();
  yes(!rules.begin("",1),"Empty run accepted");yes(!rules.begin("run",-1),"Invalid epoch accepted");
  yes(rules.begin("run",1),"New lease rejected");
  yes(check(rules,"run",1,editor,"旧页面常驻搜索词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Existing field became a write");
  yes(check(rules,"run",1,editor,"公共搜索词",true,true,true,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Unrelated pending state became a recorded write");
  yes(!rules.written("old",1,editor,"公共搜索词",true),"Wrong run recorded a write");
  yes(!rules.written("run",0,editor,"公共搜索词",true),"Old epoch recorded a write");
  yes(!rules.written("run",1,null,"公共搜索词",true),"Unknown editor recorded a write");
  yes(!rules.written("run",1,editor," ",true),"Empty query recorded a write");
  yes(!rules.written("run",1,editor,null,true),"Null query recorded a write");
  yes(!rules.written("run",1,editor,"x".repeat(501),true),"Oversize query recorded a write");
  yes(rules.written("run",1,editor,"公共搜索词",true),"Verified write rejected");
  yes(check(rules,"run",1,editor,"公共搜索词",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"type then finish accepted without submit");
  yes(check(rules,"run",1,editor,"公共搜索词",true,true,true,false)==SearchFinishRules.Decision.WAIT_FOR_READBACK,"Pending write accepted finish");
  yes(check(rules,"run",1,editor,null,true,false,false,false)==SearchFinishRules.Decision.WAIT_FOR_READBACK,"Unreadable same editor accepted finish");
  yes(check(rules,"run",1,other,"公共搜索词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Unrelated editor permanently blocked");
  yes(check(rules,"run",1,editor,"公共搜索词",false,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Departed editor permanently blocked");
  yes(check(rules,"run",1,editor,"手动改变的搜索词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Changed field inherits old query's blocker");
  yes(!rules.submitted("old",1,editor,"公共搜索词"),"Wrong run submitted");
  yes(!rules.submitted("run",0,editor,"公共搜索词"),"Old epoch submitted");
  yes(!rules.submitted("run",1,other,"公共搜索词"),"Other editor submitted");
  yes(!rules.submitted("run",1,editor,"第二搜索词"),"Different query submitted");
  yes(rules.submitted("run",1,editor,"公共搜索词"),"Actual matching submit not recorded");
  yes(check(rules,"run",1,editor,"公共搜索词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Dispatched submit still treated as missing");
  yes(rules.written("run",1,editor,"第二搜索词",false),"New actual write rejected");
  yes(check(rules,"run",1,editor,"旧值",true,true,false,false)==SearchFinishRules.Decision.WAIT_FOR_READBACK,"Unconfirmed new write accepted old field");
  yes(check(rules,"run",1,editor,"第二搜索词",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"Matching readback did not resolve write");
  yes(check(rules,"run",1,editor,"第二搜索词",true,true,false,true)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Matching native submit observation ignored");
  yes(rules.begin("run",1),"Repeated current begin rejected");
  yes(check(rules,"run",1,editor,"第二搜索词",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"Repeated begin cleared pending goal");
  yes(!rules.begin("different",1),"Same epoch changed owner");
  yes(!rules.end("old",1),"Wrong owner ended current lease");
  yes(rules.end("run",1),"Current lease did not end");
  yes(!rules.begin("run",1),"Retired lease reopened");
  yes(!rules.written("run",1,editor,"公共搜索词",true),"Late write after end accepted");
  yes(check(rules,"run",1,editor,"第二搜索词",true,true,true,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Retired lease kept blocker");
  yes(rules.begin("run",3),"Resume/new stage epoch rejected");
  yes(check(rules,"run",3,editor,"第二搜索词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Old words blocked resumed/new stage");
  yes(rules.written("run",3,editor,"公共搜索词",true),"Current new write rejected");
  yes(!rules.begin("old",2),"Late begin replaced new lease");
  yes(!rules.submitted("run",1,editor,"公共搜索词"),"Late submit changed new lease");
  yes(check(rules,"run",3,editor,"公共搜索词",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"Old callback released new query");
  yes(rules.written("run",3,other,"另一公开词",true),"Next actual editor write rejected");
  yes(check(rules,"run",3,editor,"公共搜索词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Older editor write remained permanent");
  yes(check(rules,"run",3,other,"另一公开词",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"Latest editor missed submit guard");
  yes(rules.begin("new-run",4),"New run rejected");
  yes(check(rules,"new-run",4,other,"另一公开词",true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Previous run's search persisted");
  yes(SearchFinishRules.instruction(SearchFinishRules.Decision.NO_LOCAL_BLOCK).isEmpty(),"Passing guard claims a result");
  yes(SearchFinishRules.instruction(SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED).contains("不代表目标完成"),"Submit confused with successful result");
  yes(SearchFinishRules.instruction(SearchFinishRules.Decision.WAIT_FOR_READBACK).contains("不重复输入"),"Pending-write guidance encourages replay");
  SearchFinishRules cycles=new SearchFinishRules();Object field=new Object();Port port=new Port(cycles,field);
  yes(cycles.begin("cycles",20),"Cycle lease rejected");
  yes(cycles.written("cycles",20,field,"公开甲",true),"Actual first write rejected");port.text="公开甲";
  SearchSubmitRules.State first=cycles.submissionState("cycles",20,field);
  yes(submit(cycles,port,field).contains("尚未核验结果")&&port.dispatches==1,"First submit does not remain unverified");
  denied(()->submit(cycles,port,field),"Repeated submit without a new write accepted");
  yes(port.dispatches==1&&first==cycles.submissionState("cycles",20,field),"Reading/already-present path resets reservation");
  yes(cycles.written("cycles",20,field,"公开乙",true),"Actual second write rejected");port.text="公开乙";
  yes(first!=cycles.submissionState("cycles",20,field),"Actual second write reused old reservation scope");
  submit(cycles,port,field);denied(()->submit(cycles,port,field),"Second write allowed duplicate submit");
  yes(port.dispatches==2,"Second write did not allow exactly one submit");
  yes(cycles.written("cycles",20,field,"公开甲",true),"Actual A-B-A write rejected");port.text="公开甲";
  SearchSubmitRules.State third=cycles.submissionState("cycles",20,field);
  yes(third!=first&&!third.dispatched(field,"cycles","公开甲"),"A-B-A write inherited first A submission");
  yes(check(cycles,"cycles",20,field,"公开甲",true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"Old A submit released current A finish");
  submit(cycles,port,field);denied(()->submit(cycles,port,field),"New A allowed duplicate submit");
  yes(port.dispatches==3,"Three actual writes did not dispatch exactly once each");
  first.reserve(field,"cycles","旧周期引用");
  yes(!third.dispatched(field,"cycles","旧周期引用")&&third==cycles.submissionState("cycles",20,field),"Late old State reference changed current write cycle");
  denied(()->cycles.submissionState("cycles",19,field),"Stale scope obtained current reservation state");
  yes(cycles.begin("next-stage",21),"Next cycle scope rejected");
  yes(!cycles.submissionState("next-stage",21,field).dispatched(field,"cycles","公开甲"),"New stage inherited prior query reservation");
  System.out.println("PASS: SearchFinishRules "+checks+" scoped write, missing-submit, late callback and non-success checks");
 }
}
