package studio.jiayu.neoagent;

final class SearchWriteRouteRulesTest {
 static int checks;
 static void yes(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
 interface Work {void run()throws Exception;}
 static void denied(Work work,String reason)throws Exception{boolean refused=false;try{work.run();}catch(Exception expected){refused=true;}yes(refused,reason);}
 static SearchInputRules.Sample text(String value){return new SearchInputRules.Sample(value,0,value.length(),value.length());}
 static void route(SearchWriteRouteRules.Path expected,int requested,int focused,boolean bound,boolean systemBound,SearchInputRules.Sample node,SearchInputRules.Sample system){yes(SearchWriteRouteRules.choose(requested,focused,bound,systemBound,node,system)==expected,"Wrong search input path "+checks);}
 static final class Port implements SearchInputRules.Port,SearchNodeRules.Port {
  SearchInputRules.Sample value=text("");int commits,sets,selects;boolean hiddenAfterDispatch,failAfterDispatch,focusChangedBeforeCommit,valueChangedBeforeCommit;
  public SearchInputRules.Sample read(){return hiddenAfterDispatch&&(commits+sets)>0?null:value;}
  public void selectAll(){selects++;value=new SearchInputRules.Sample(value.text,value.offset,0,value.text.length());}
  public void pause(int ms){}
  void dispatch(SearchInputRules.Sample expected,String query,Runnable dispatching,boolean nativePort)throws Exception{
   if(focusChangedBeforeCommit)throw new SearchInputRules.Rejected("Node/editor/window binding changed before dispatch");
   if(valueChangedBeforeCommit)value=text("其他公开搜索词");
   if(!expected.same(value))throw new SearchInputRules.Rejected("Readback changed before dispatch");
   dispatching.run();if(nativePort)commits++;else sets++;
   if(failAfterDispatch)throw new Exception("Native dispatch receipt unknown");
   value=text(query);
  }
  public void commit(SearchInputRules.Sample expected,String query,Runnable dispatching)throws Exception{dispatch(expected,query,dispatching,true);}
  public void set(SearchInputRules.Sample expected,String query,Runnable dispatching)throws Exception{dispatch(expected,query,dispatching,false);}
 }
 public static void main(String[] args)throws Exception{
  SearchInputRules.Sample empty=text(""),query=text("公开关键词"),unreadable=new SearchInputRules.Sample("旧原生文字",0,-1,-1),partial=new SearchInputRules.Sample("局部",5,0,2);
  route(SearchWriteRouteRules.Path.SYSTEM,2,2,true,true,unreadable,empty);
  route(SearchWriteRouteRules.Path.SYSTEM,2,2,true,true,null,query);
  route(SearchWriteRouteRules.Path.SYSTEM,2,2,true,true,partial,query);
  route(SearchWriteRouteRules.Path.SYSTEM,2,2,true,true,unreadable,new SearchInputRules.Sample("公开关键词",-1,0,5));
  route(SearchWriteRouteRules.Path.NODE,2,2,true,true,query,query);
  route(SearchWriteRouteRules.Path.NODE,2,2,true,false,query,null);
  route(SearchWriteRouteRules.Path.NODE,2,2,true,true,query,unreadable);
  route(SearchWriteRouteRules.Path.CONFLICT,2,2,true,true,empty,query);
  route(SearchWriteRouteRules.Path.CONFLICT,2,2,true,true,query,empty);
  route(SearchWriteRouteRules.Path.STALE_TARGET,3,2,true,true,unreadable,query);
  route(SearchWriteRouteRules.Path.STALE_TARGET,2,0,true,true,unreadable,query);
  route(SearchWriteRouteRules.Path.STALE_TARGET,2,2,false,true,unreadable,query);
  route(SearchWriteRouteRules.Path.STALE_TARGET,-1,2,true,true,query,query);
  route(SearchWriteRouteRules.Path.UNAVAILABLE,2,2,true,false,unreadable,query);
  route(SearchWriteRouteRules.Path.UNAVAILABLE,2,2,true,true,unreadable,null);
  route(SearchWriteRouteRules.Path.UNAVAILABLE,2,2,true,true,unreadable,partial);
  route(SearchWriteRouteRules.Path.UNAVAILABLE,2,2,true,true,unreadable,new SearchInputRules.Sample(new String(new char[513]),0,0,0));
  route(SearchWriteRouteRules.Path.SYSTEM,0,0,false,true,null,query);
  route(SearchWriteRouteRules.Path.UNAVAILABLE,0,0,false,false,query,null);

  String run="run";long generation=3;Object key=new Object();SearchFinishRules finish=new SearchFinishRules();yes(finish.begin(run,generation),"Could not start search lease");SearchInputRules.Pending pending=new SearchInputRules.Pending();Port port=new Port();
  yes(SearchWriteRouteRules.replaceSystem(port,pending,finish,key,run,generation,"公开查询甲")==SearchInputRules.Result.VERIFIED,"Bound system write not verified");
  yes(port.commits==1&&port.sets==0,"System route attempted inaccessible node mutation");
  yes(finish.beforeFinish(run,generation,key,port.value.text,true,true,false,false)==SearchFinishRules.Decision.SEARCH_NOT_SUBMITTED,"System write bypassed actual-write ledger");
  SearchSubmitRules.State first=finish.submissionState(run,generation,key);first.reserve(key,run,port.value.text);finish.submitted(run,generation,key,port.value.text);
  yes(SearchWriteRouteRules.replaceSystem(port,pending,finish,key,run,generation,"公开查询甲")==SearchInputRules.Result.ALREADY_PRESENT,"Present system query not preserved");
  yes(port.commits==1&&finish.submissionState(run,generation,key)==first,"Already-present route reset ledger or wrote again");
  denied(()->first.reserve(key,run,"公开查询甲"),"Switching route allowed duplicate submit");
  yes(SearchNodeRules.replace(port,pending,finish,key,run,generation,"公开查询乙")==SearchInputRules.Result.VERIFIED,"Same-node route could not use a new actual-write cycle");
  yes(SearchWriteRouteRules.replaceSystem(port,pending,finish,key,run,generation,"公开查询甲")==SearchInputRules.Result.VERIFIED,"System route could not return A after B");
  yes(port.commits==2&&port.sets==1&&finish.submissionState(run,generation,key)!=first,"A-B-A actual writes not distinguished");

  Port lost=new Port();lost.hiddenAfterDispatch=true;Object lostKey=new Object();SearchInputRules.Pending lostPending=new SearchInputRules.Pending();
  yes(SearchWriteRouteRules.replaceSystem(lost,lostPending,finish,lostKey,run,generation,"公开查询甲")==SearchInputRules.Result.DISPATCHED_UNVERIFIED,"Unknown readback was reported verified");
  yes(finish.beforeFinish(run,generation,lostKey,null,true,false,true,false)==SearchFinishRules.Decision.WAIT_FOR_READBACK,"Unknown system receipt allowed finish");
  denied(()->SearchWriteRouteRules.replaceSystem(lost,lostPending,finish,lostKey,run,generation,"公开查询乙"),"Pending system write was replayed");
  denied(()->SearchNodeRules.replace(lost,lostPending,finish,lostKey,run,generation,"公开查询甲"),"Node route bypassed pending system write");
  yes(lost.commits==1&&lost.sets==0,"Cross-route pending write reached another native port");
  lost.hiddenAfterDispatch=false;
  yes(SearchNodeRules.replace(lost,lostPending,finish,lostKey,run,generation,"公开查询甲")==SearchInputRules.Result.ALREADY_PRESENT,"Later node readback duplicated system write");
  yes(lost.commits==1&&lost.sets==0,"Matching later receipt wrote again");
  Port nodeLost=new Port();nodeLost.hiddenAfterDispatch=true;Object nodeLostKey=new Object();SearchInputRules.Pending nodePending=new SearchInputRules.Pending();
  yes(SearchNodeRules.replace(nodeLost,nodePending,finish,nodeLostKey,run,generation,"公开查询甲")==SearchInputRules.Result.DISPATCHED_UNVERIFIED,"Unknown node receipt not retained");
  denied(()->SearchWriteRouteRules.replaceSystem(nodeLost,nodePending,finish,nodeLostKey,run,generation,"公开查询乙"),"System route bypassed pending node write");
  yes(nodeLost.commits==0&&nodeLost.sets==1,"Cross-route node pending write duplicated");

  Port unknown=new Port();unknown.failAfterDispatch=true;Object unknownKey=new Object();SearchInputRules.Pending unknownPending=new SearchInputRules.Pending();
  denied(()->SearchWriteRouteRules.replaceSystem(unknown,unknownPending,finish,unknownKey,run,generation,"公开查询甲"),"Native dispatch exception was hidden");
  unknown.failAfterDispatch=false;
  denied(()->SearchWriteRouteRules.replaceSystem(unknown,unknownPending,finish,unknownKey,run,generation,"公开查询甲"),"Native dispatch exception permitted replay");
  yes(unknown.commits==1,"Native exception committed twice");
  Port focusRace=new Port();focusRace.focusChangedBeforeCommit=true;Object focusKey=new Object();SearchInputRules.Pending focusPending=new SearchInputRules.Pending();
  denied(()->SearchWriteRouteRules.replaceSystem(focusRace,focusPending,finish,focusKey,run,generation,"公开查询甲"),"Changed target/editor binding wrote");
  yes(focusRace.commits==0&&!focusPending.unresolved(focusKey,run,focusRace.value),"Pre-dispatch binding failure created pending receipt");
  Port textRace=new Port();textRace.valueChangedBeforeCommit=true;
  denied(()->SearchWriteRouteRules.replaceSystem(textRace,new SearchInputRules.Pending(),finish,new Object(),run,generation,"公开查询甲"),"Changed system text wrote");
  yes(textRace.commits==0,"Readback race dispatched commit");
  Port stale=new Port();Object staleKey=new Object();SearchInputRules.Pending stalePending=new SearchInputRules.Pending();
  denied(()->SearchWriteRouteRules.replaceSystem(stale,stalePending,finish,staleKey,run,generation-1,"公开查询甲"),"Stale run generation committed");
  yes(stale.commits==0&&!stalePending.unresolved(staleKey,run,stale.value),"Rejected lease recorded a phantom dispatch");
  yes(finish.end(run,generation),"Lease did not end");Port ended=new Port();
  denied(()->SearchWriteRouteRules.replaceSystem(ended,new SearchInputRules.Pending(),finish,new Object(),run,generation,"公开查询甲"),"Retired lease committed");
  yes(ended.commits==0,"Retired lease reached system native call");
  System.out.println("PASS: SearchWriteRouteRules "+checks+" exact-id route, separate readback, conflict, cross-port pending and actual-write ledger checks");
 }
}
