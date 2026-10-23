package studio.jiayu.neoagent;

/** A numbered search field may use only its own freshly bound editor connection. */
final class SearchWriteRouteRules {
 enum Path { NODE, SYSTEM, UNAVAILABLE, CONFLICT, STALE_TARGET }
 static Path choose(int requestedId,int focusedId,boolean targetBound,boolean systemBound,SearchInputRules.Sample node,SearchInputRules.Sample system){
  if(requestedId<0||requestedId>0&&(requestedId!=focusedId||!targetBound))return Path.STALE_TARGET;
  boolean nodeReadable=requestedId>0&&node!=null&&node.readable(),systemReadable=systemBound&&system!=null&&system.readable();
  if(nodeReadable&&systemReadable&&!node.text.equals(system.text))return Path.CONFLICT;
  if(nodeReadable)return Path.NODE;
  return systemReadable?Path.SYSTEM:Path.UNAVAILABLE;
 }
 /** Both routes share the same pending receipt and actual-write cycle. The native
  * port invokes its callback only after the final owner, node and sample checks. */
 static SearchInputRules.Result replaceSystem(SearchInputRules.Port port,SearchInputRules.Pending pending,SearchFinishRules finish,Object key,String run,long generation,String query)throws Exception{
  SearchInputRules.Port recorded=new SearchInputRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return port.read();}
   public void selectAll()throws Exception{port.selectAll();}
   public void pause(int milliseconds)throws Exception{port.pause(milliseconds);}
   public void commit(SearchInputRules.Sample expected,String value,Runnable dispatching)throws Exception{
    port.commit(expected,value,()->{
     if(!finish.written(run,generation,key,value,false))throw new IllegalStateException("搜索输入租约已变化，原动作未派发");
     dispatching.run();
    });
   }
  };
  return SearchInputRules.replace(recorded,pending,key,run,query);
 }
}
