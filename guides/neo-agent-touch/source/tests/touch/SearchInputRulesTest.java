package studio.jiayu.neoagent;
public final class SearchInputRulesTest {
 static int checks;
 static void check(boolean actual,boolean expected){if(actual!=expected)throw new AssertionError("Wrong search editor boundary "+checks);checks++;}
 static SearchInputRules.Sample sample(String text,int offset,int start,int end){return new SearchInputRules.Sample(text,offset,start,end);}
 static final class Editor implements SearchInputRules.Port {
  SearchInputRules.Sample value;int selectCalls,commitCalls,selectDelay,readsSinceSelect,pauses;boolean ignoreSelection,nullRead,hideAfterCommit,changeBeforeCommit,failAfterDispatch;
  Editor(String text,int offset){value=sample(text,offset,text.length(),text.length());}
  public SearchInputRules.Sample read(){if(nullRead||hideAfterCommit&&commitCalls>0)return null;if(selectCalls>0&&!ignoreSelection&&readsSinceSelect++>=selectDelay)value=sample(value.text,value.offset,0,value.text.length());return value;}
  public void selectAll(){selectCalls++;readsSinceSelect=0;}
  public void commit(SearchInputRules.Sample expected,String text,Runnable dispatching)throws Exception{
   if(changeBeforeCommit)value=sample("外部输入已改变",value.offset,0,0);if(!expected.same(value))throw new SearchInputRules.Rejected("editor changed before commit");
   dispatching.run();commitCalls++;if(failAfterDispatch)throw new Exception("asynchronous commit receipt lost");value=sample(text,value.offset,text.length(),text.length());
  }
  public void pause(int milliseconds){pauses++;}
 }
 static void rejected(Editor editor,SearchInputRules.Pending pending,Object session,String query)throws Exception{boolean refused=false;try{SearchInputRules.replace(editor,pending,session,"run",query);}catch(SearchInputRules.Rejected expected){refused=true;}check(refused,true);}
 public static void main(String[] args)throws Exception{
  check(SearchInputRules.ready(36,true,true,true,1,3),true);
  check(SearchInputRules.ready(33,true,true,true,161,0x10000003),true);
  check(SearchInputRules.ready(33,true,true,true,177,3),true);
  check(SearchInputRules.ready(36,true,true,true,0x2c0a1,0x12000003),true);
  check(SearchInputRules.ready(36,true,true,true,0x20001,3),true);
  check(SearchInputRules.ready(36,true,true,true,0x40001,3),true);
  check(SearchInputRules.ready(32,true,true,true,1,3),false);
  check(SearchInputRules.ready(36,false,true,true,1,3),false);
  check(SearchInputRules.ready(36,true,false,true,1,3),false);
  check(SearchInputRules.ready(36,true,true,false,1,3),false);
  for(int action:new int[]{0,1,2,4,5,6})check(SearchInputRules.ready(36,true,true,true,1,action),false);
  for(int type:new int[]{2,3,129,145,225,33,209,0x20001|64})check(SearchInputRules.ready(36,true,true,true,type,3),false);
  check(SearchInputRules.ready(36,true,true,true,0x2c0a1,4),false);
  check(SearchInputRules.readable("StepX Neo",0,9,9),true);
  check(SearchInputRules.selected("StepX Neo",0,0,9),true);
  check(SearchInputRules.selected("StepX Neo",0,9,0),true);
  check(SearchInputRules.selected("",0,0,0),true);
  check(SearchInputRules.selected("StepX Neo",0,0,4),false);
  check(SearchInputRules.readable("StepX Neo",5,0,9),false);
  check(SearchInputRules.readable(null,0,0,0),false);
  check(SearchInputRules.readable("StepX Neo",0,-1,9),false);
  check(SearchInputRules.readable("StepX Neo",0,0,10),false);
  check(SearchInputRules.readable(new String(new char[513]),0,0,0),false);
  // AOSP's default getSurroundingText returns offset -1 even for a valid short/empty editor.
  check(SearchInputRules.readable("公共关键词",-1,5,5),true);
  check(SearchInputRules.selected("公共关键词",-1,0,5),true);
  check(SearchInputRules.selected("公共关键词",-1,5,0),true);
  check(SearchInputRules.selected("公共关键词",-1,0,3),false);
  check(SearchInputRules.selected("",-1,0,0),true);
  check(SearchInputRules.readable("公共关键词",-2,0,5),false);
  check(SearchInputRules.readable("公共关键词",-1,-1,5),false);
  check(SearchInputRules.readable("公共关键词",-1,0,6),false);
  Object session=new Object();SearchInputRules.Pending pending=new SearchInputRules.Pending();
  Editor unknown=new Editor("旧公共搜索词",-1);unknown.selectDelay=2;
  check(SearchInputRules.replace(unknown,pending,session,"run","目标关键词")==SearchInputRules.Result.VERIFIED,true);
  check(unknown.selectCalls==1&&unknown.commitCalls==1&&unknown.value.text.equals("目标关键词"),true);
  check(unknown.pauses>=3,true);
  check(SearchInputRules.replace(unknown,pending,session,"run","目标关键词")==SearchInputRules.Result.ALREADY_PRESENT,true);
  check(unknown.commitCalls==1,true);
  Editor empty=new Editor("",-1);
  check(SearchInputRules.replace(empty,new SearchInputRules.Pending(),new Object(),"run","目标关键词")==SearchInputRules.Result.VERIFIED,true);
  check(empty.selectCalls==1&&empty.commitCalls==1&&empty.value.text.equals("目标关键词"),true);
  Editor knownEmpty=new Editor("",0);
  check(SearchInputRules.replace(knownEmpty,new SearchInputRules.Pending(),new Object(),"run","目标关键词")==SearchInputRules.Result.VERIFIED,true);
  check(knownEmpty.selectCalls==0&&knownEmpty.commitCalls==1,true);
  Editor already=new Editor("目标关键词",0);
  check(SearchInputRules.replace(already,new SearchInputRules.Pending(),new Object(),"run","目标关键词")==SearchInputRules.Result.ALREADY_PRESENT,true);
  check(already.selectCalls==0&&already.commitCalls==0,true);
  Editor missing=new Editor("旧词",-1);missing.nullRead=true;
  rejected(missing,new SearchInputRules.Pending(),new Object(),"目标关键词");check(missing.commitCalls==0&&missing.selectCalls==0,true);
  Editor ignored=new Editor("旧公共搜索词",-1);ignored.ignoreSelection=true;
  rejected(ignored,new SearchInputRules.Pending(),new Object(),"目标关键词");check(ignored.commitCalls==0&&ignored.value.text.equals("旧公共搜索词"),true);
  Editor late=new Editor("旧词",-1);late.hideAfterCommit=true;Object lateSession=new Object();SearchInputRules.Pending latePending=new SearchInputRules.Pending();
  check(SearchInputRules.replace(late,latePending,lateSession,"run","目标关键词")==SearchInputRules.Result.DISPATCHED_UNVERIFIED,true);
  rejected(late,latePending,lateSession,"目标关键词");check(late.commitCalls==1,true);
  // An unrelated refinement cannot bypass the unresolved-write guard, either.
  rejected(late,latePending,lateSession,"另一个关键词");check(late.commitCalls==1,true);
  late.hideAfterCommit=false;
  check(SearchInputRules.replace(late,latePending,lateSession,"run","目标关键词")==SearchInputRules.Result.ALREADY_PRESENT,true);
  check(late.commitCalls==1,true);
  Editor changed=new Editor("旧词",-1);changed.changeBeforeCommit=true;
  rejected(changed,new SearchInputRules.Pending(),new Object(),"目标关键词");check(changed.commitCalls==0,true);
  Editor failed=new Editor("旧词",-1);failed.failAfterDispatch=true;SearchInputRules.Pending failedPending=new SearchInputRules.Pending();Object failedSession=new Object();boolean failedOnce=false;
  try{SearchInputRules.replace(failed,failedPending,failedSession,"run","目标关键词");}catch(Exception expected){failedOnce=true;}check(failedOnce,true);
  failed.failAfterDispatch=false;rejected(failed,failedPending,failedSession,"目标关键词");check(failed.commitCalls==1,true);
  // A genuinely new editor/run does not inherit the old editor's pending receipt.
  Editor fresh=new Editor("",-1);
  check(SearchInputRules.replace(fresh,failedPending,new Object(),"run","目标关键词")==SearchInputRules.Result.VERIFIED,true);
  Editor anotherRun=new Editor("",-1);
  check(SearchInputRules.replace(anotherRun,failedPending,failedSession,"new-run","目标关键词")==SearchInputRules.Result.VERIFIED,true);
  System.out.println("PASS: SearchInputRules "+checks+" search purpose, unknown offset, select/confirm/replace, exact readback and no replay checks");
 }
}
