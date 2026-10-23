package studio.jiayu.neoagent;
final class SearchNodeRulesTest {
 static int n;static void yes(boolean value,String message){if(!value)throw new AssertionError(message);n++;}
 interface Work{void run()throws Exception;}
 static void denied(Work work,String message)throws Exception{boolean failed=false;try{work.run();}catch(Exception expected){failed=true;}yes(failed,message);}
 static final class NativeNode {final int id;NativeNode(int value){id=value;}public boolean equals(Object o){return o instanceof NativeNode&&((NativeNode)o).id==id;}public int hashCode(){return id;}}
 static SearchInputRules.Sample sample(String text){return new SearchInputRules.Sample(text,0,text.length(),text.length());}
 static final class Port implements SearchNodeRules.Port {
  SearchInputRules.Sample value=sample("旧公开搜索词");int calls;boolean hide,failAfter,changeBefore;
  public SearchInputRules.Sample read(){return hide&&calls>0?null:value;}
  public void set(SearchInputRules.Sample expected,String text,Runnable dispatching)throws Exception{if(changeBefore)value=sample("其他公开文本");if(!expected.same(value))throw new SearchInputRules.Rejected("changed");dispatching.run();calls++;if(failAfter)throw new Exception("receipt unknown");value=sample(text);}
  public void pause(int ms){}
 }
 public static void main(String[] args)throws Exception{
  SearchNodeRules.Session session=new SearchNodeRules.Session();Object editor=new Object(),otherEditor=new Object();NativeNode first=new NativeNode(1),second=new NativeNode(2);
  yes(session.focus("one",1,first,editor,true)==null,"Observation started an unauthorized session");
  yes(session.begin("one",1),"Owner begin rejected");Object key=session.focus("one",1,first,editor,true);yes(key!=null,"Focused public node has no key");
  yes(key==session.focus("one",1,new NativeNode(1),editor,true),"Native equal wrapper changed session");
  yes(session.accepts("one",1,key,new NativeNode(1),editor),"Same native node rejected");
  yes(!session.accepts("one",1,key,second,editor),"Different empty field borrowed session");
  yes(!session.accepts("one",1,key,first,otherEditor),"Changed native editor borrowed session");
  yes(!session.accepts("two",1,key,first,editor)&&!session.accepts("one",0,key,first,editor),"Wrong run/epoch accepted");
  yes(session.focus("one",0,second,otherEditor,false)==null,"Stale focus event accepted");yes(session.accepts("one",1,key,first,editor),"Stale callback retired new focus");
  yes(session.begin("one",1)&&session.focus("one",1,first,editor,true)==key,"Same begin cleared session");
  Object changed=session.focus("one",1,first,otherEditor,true);yes(changed!=key,"New EditorInfo reused old node key");
  yes(session.focus("one",1,null,null,false)==null&&!session.accepts("one",1,changed,first,otherEditor),"Departed focus retained key");
  Object reentered=session.focus("one",1,first,otherEditor,true);yes(reentered!=changed,"Reentered field reused departed session");
  yes(session.begin("one",2),"Next epoch rejected");Object newer=session.focus("one",2,first,editor,true);yes(newer!=reentered,"New lease reused old key");
  yes(!session.end("one",1)&&session.accepts("one",2,newer,first,editor),"Old end killed new lease");
  yes(session.end("one",2)&&!session.begin("one",2),"Retired lease reopened");

  SearchFinishRules finish=new SearchFinishRules();yes(finish.begin("run",1),"Finish begin rejected");Object nodeKey=new Object(),nativeKey=new Object();SearchInputRules.Pending pending=new SearchInputRules.Pending();Port port=new Port();
  yes(finish.operationKey("run",1,nodeKey,nativeKey)==nodeKey,"No write did not prefer node key");
  yes(SearchNodeRules.replace(port,pending,finish,nodeKey,"run",1,"公开搜索甲")==SearchInputRules.Result.VERIFIED,"Node write not verified");yes(port.calls==1&&"公开搜索甲".equals(port.value.text),"Native set-text count or value wrong");
  yes(finish.beforeFinish("run",1,nodeKey,port.value.text,true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"type -> finish bypassed missing submit");
  yes(SearchNodeRules.replace(port,pending,finish,nodeKey,"run",1,"公开搜索甲")==SearchInputRules.Result.ALREADY_PRESENT&&port.calls==1,"Already-present query wrote again");
  SearchSubmitRules.State state=finish.submissionState("run",1,nodeKey);state.reserve(nodeKey,"run","公开搜索甲");yes(finish.submitted("run",1,nodeKey,"公开搜索甲"),"Matching submit attempt not recorded");
  yes(finish.beforeFinish("run",1,nodeKey,port.value.text,true,true,false,false)==SearchFinishRules.Decision.NO_LOCAL_BLOCK,"Recorded attempt still missing");
  yes(SearchNodeRules.replace(port,pending,finish,nodeKey,"run",1,"公开搜索甲")==SearchInputRules.Result.ALREADY_PRESENT&&finish.submissionState("run",1,nodeKey)==state,"Already-present text erased prior reservation");
  denied(()->state.reserve(nodeKey,"run","公开搜索甲"),"No-write replay accepted");
  yes(SearchNodeRules.replace(port,pending,finish,nodeKey,"run",1,"公开搜索乙")==SearchInputRules.Result.VERIFIED,"A to B write rejected");
  yes(SearchNodeRules.replace(port,pending,finish,nodeKey,"run",1,"公开搜索甲")==SearchInputRules.Result.VERIFIED,"B to A actual write rejected");
  SearchSubmitRules.State returned=finish.submissionState("run",1,nodeKey);yes(returned!=state,"A-B-A kept old write cycle");returned.reserve(nodeKey,"run","公开搜索甲");denied(()->returned.reserve(nodeKey,"run","公开搜索甲"),"New A cycle dispatched twice");
  yes(finish.operationKey("run",1,nativeKey,nodeKey)==nodeKey,"Native submit lost matching node receipt");

  Port lost=new Port();lost.hide=true;SearchInputRules.Pending lostPending=new SearchInputRules.Pending();Object lostKey=new Object();
  yes(SearchNodeRules.replace(lost,lostPending,finish,lostKey,"run",1,"公开搜索丙")==SearchInputRules.Result.DISPATCHED_UNVERIFIED,"Lost readback fabricated verification");
  yes(finish.beforeFinish("run",1,lostKey,null,true,false,true,false)==SearchFinishRules.Decision.WAIT_FOR_READBACK,"Lost readback allowed finish");
  denied(()->SearchNodeRules.replace(lost,lostPending,finish,lostKey,"run",1,"公开搜索丙"),"Unknown dispatch replayed");yes(lost.calls==1,"Unknown write called twice");
  lost.hide=false;yes(SearchNodeRules.replace(lost,lostPending,finish,lostKey,"run",1,"公开搜索丙")==SearchInputRules.Result.ALREADY_PRESENT&&lost.calls==1,"Later readback replayed write");
  yes(finish.beforeFinish("run",1,lostKey,lost.value.text,true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"Later readback fabricated submit");
  Port failed=new Port();failed.failAfter=true;SearchInputRules.Pending failedPending=new SearchInputRules.Pending();Object failedKey=new Object();
  denied(()->SearchNodeRules.replace(failed,failedPending,finish,failedKey,"run",1,"公开搜索甲"),"Native failure hidden");failed.failAfter=false;
  denied(()->SearchNodeRules.replace(failed,failedPending,finish,failedKey,"run",1,"公开搜索甲"),"Post-dispatch exception allowed replay");yes(failed.calls==1,"Unknown exception duplicated native call");
  Port race=new Port();race.changeBefore=true;denied(()->SearchNodeRules.replace(race,new SearchInputRules.Pending(),finish,new Object(),"run",1,"公开搜索甲"),"Final readback race wrote");yes(race.calls==0,"Raced field mutated");
  Port stale=new Port();denied(()->SearchNodeRules.replace(stale,new SearchInputRules.Pending(),finish,new Object(),"run",0,"公开搜索甲"),"Stale generation dispatched node write");yes(stale.calls==0,"Stale write reached native");
  yes(finish.end("run",1),"End failed");denied(()->finish.operationKey("run",1,nodeKey,nativeKey),"Old capture reopened finish lease");
  System.out.println("PASS: SearchNodeRules "+n+" native-node session, actual-write, finish, no replay and stale lease checks");
 }
}
