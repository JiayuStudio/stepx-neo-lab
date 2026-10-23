package studio.jiayu.neoagent;
final class SearchSubmitRulesTest {
 static int checks;
 static void yes(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 interface Job{void run()throws Exception;}
 static void denied(Job job,String why)throws Exception{boolean rejected=false;try{job.run();}catch(Exception expected){rejected=true;}yes(rejected,why);}
 static final SearchSubmitRules.Validator PUBLIC=text->{if(!text.equals("公共搜索词")&&!text.equals("第二搜索词"))throw new SearchSubmitRules.Rejected("Wrong original target");};
 static final class Port implements SearchSubmitRules.Port {
  SearchInputRules.Sample sample=new SearchInputRules.Sample("公共搜索词",0,5,5);int actions,selections;boolean owner=true,ignoreSelect,changeBeforeSearch,failAfterDispatch;
  public SearchInputRules.Sample read(){return sample;}
  public void selectAll(){selections++;if(!ignoreSelect)sample=new SearchInputRules.Sample(sample.text,sample.offset,0,sample.text.length());}
  public void pause(int milliseconds){}
  public void search(SearchInputRules.Sample expected,Runnable dispatching)throws Exception{
   if(!owner)throw new SearchSubmitRules.Rejected("Old owner");
   if(changeBeforeSearch)sample=new SearchInputRules.Sample("第二搜索词",0,5,5);
   if(!expected.same(sample))throw new SearchSubmitRules.Rejected("Editor changed before dispatch");
   dispatching.run();actions++;if(failAfterDispatch)throw new Exception("Result unknown after void native call");
  }
 }
 static String send(Port p,SearchInputRules.Pending writes,SearchSubmitRules.State state,Object editor,String run)throws Exception{return SearchSubmitRules.submit(p,writes,state,editor,run,PUBLIC);}
 public static void main(String[] args)throws Exception{
  Object editor=new Object();SearchSubmitRules.State state=new SearchSubmitRules.State();SearchInputRules.Pending writes=new SearchInputRules.Pending();Port p=new Port();
  String receipt=send(p,writes,state,editor,"one");yes(p.actions==1&&receipt.contains("尚未核验结果"),"Dispatch must not claim result success");
  denied(()->send(p,writes,state,editor,"one"),"Repeated query dispatched twice");yes(p.actions==1,"Duplicate made native call");
  p.sample=new SearchInputRules.Sample("第二搜索词",0,5,5);send(p,writes,state,editor,"one");yes(p.actions==2,"New query in same editor not accepted");
  send(p,writes,state,new Object(),"one");yes(p.actions==3,"New editor session blocked by old reservation");
  send(p,writes,state,editor,"two");yes(p.actions==4,"New run blocked by old reservation");
  Port unknown=new Port();unknown.sample=new SearchInputRules.Sample("公共搜索词",-1,5,5);send(unknown,new SearchInputRules.Pending(),new SearchSubmitRules.State(),editor,"one");yes(unknown.selections==1&&unknown.actions==1,"Unknown absolute offset needs confirmed full selection");
  Port noSelect=new Port();noSelect.sample=new SearchInputRules.Sample("公共搜索词",-1,5,5);noSelect.ignoreSelect=true;
  denied(()->send(noSelect,new SearchInputRules.Pending(),new SearchSubmitRules.State(),editor,"one"),"Unconfirmed selection submitted");yes(noSelect.actions==0,"Selection failure dispatched");
  Port unresolved=new Port();SearchInputRules.Pending lostWrite=new SearchInputRules.Pending();lostWrite.dispatched(editor,"one","第二搜索词");
  denied(()->send(unresolved,lostWrite,new SearchSubmitRules.State(),editor,"one"),"Unverified write submitted");yes(unresolved.actions==0,"Unverified input dispatched");
  unresolved.sample=new SearchInputRules.Sample("第二搜索词",0,5,5);send(unresolved,lostWrite,new SearchSubmitRules.State(),editor,"one");yes(unresolved.actions==1,"Verified later readback still blocked");
  for(SearchInputRules.Sample invalid:new SearchInputRules.Sample[]{null,new SearchInputRules.Sample("",0,0,0),new SearchInputRules.Sample("公共搜索词",10,5,5),new SearchInputRules.Sample("公共搜索词",0,-1,0),new SearchInputRules.Sample("x".repeat(501),0,0,0)}){
   Port bad=new Port();bad.sample=invalid;denied(()->send(bad,new SearchInputRules.Pending(),new SearchSubmitRules.State(),editor,"one"),"Invalid/full-text-unknown input submitted");yes(bad.actions==0,"Invalid sample made native call");
  }
  Port wrong=new Port();wrong.sample=new SearchInputRules.Sample("错误目标",0,4,4);denied(()->send(wrong,new SearchInputRules.Pending(),new SearchSubmitRules.State(),editor,"one"),"Wrong original target submitted");yes(wrong.actions==0,"Policy rejection made native call");
  Port race=new Port();race.changeBeforeSearch=true;SearchSubmitRules.State raceState=new SearchSubmitRules.State();denied(()->send(race,new SearchInputRules.Pending(),raceState,editor,"one"),"Raced editor contents submitted");yes(race.actions==0&&!raceState.dispatched(editor,"one","公共搜索词"),"Pre-dispatch race reserved or called native action");
  Port stale=new Port();stale.owner=false;denied(()->send(stale,new SearchInputRules.Pending(),new SearchSubmitRules.State(),editor,"old-run"),"Old owner submitted");yes(stale.actions==0,"Stale owner dispatched");
  Port uncertain=new Port();uncertain.failAfterDispatch=true;SearchSubmitRules.State hold=new SearchSubmitRules.State();denied(()->send(uncertain,new SearchInputRules.Pending(),hold,editor,"one"),"Native exception hidden");yes(uncertain.actions==1&&hold.dispatched(editor,"one","公共搜索词"),"Unknown result lost dispatch reservation");
  uncertain.failAfterDispatch=false;denied(()->send(uncertain,new SearchInputRules.Pending(),hold,editor,"one"),"Unknown native result replayed");yes(uncertain.actions==1,"Unknown native action called again");
  Port aba=new Port();SearchSubmitRules.State abaState=new SearchSubmitRules.State();send(aba,new SearchInputRules.Pending(),abaState,editor,"aba");aba.sample=new SearchInputRules.Sample("第二搜索词",0,5,5);send(aba,new SearchInputRules.Pending(),abaState,editor,"aba");aba.sample=new SearchInputRules.Sample("公共搜索词",0,5,5);denied(()->send(aba,new SearchInputRules.Pending(),abaState,editor,"aba"),"A-B-A unresolved query replayed");yes(aba.actions==2,"A-B-A made third native call");
  Object otherEditor=new Object();send(aba,new SearchInputRules.Pending(),abaState,otherEditor,"aba");denied(()->send(aba,new SearchInputRules.Pending(),abaState,editor,"aba"),"Returning to same old editor lost reservation");yes(aba.actions==3,"Editor switch evicted old query reservation");
  Port emptyOwner=new Port();denied(()->send(emptyOwner,new SearchInputRules.Pending(),new SearchSubmitRules.State(),editor,""),"Missing run accepted");yes(emptyOwner.actions==0,"Missing run dispatched");
  System.out.println("PASS: SearchSubmitRules "+checks+" native-search dispatch, exact-readback, owner and no-replay checks");
 }
}
