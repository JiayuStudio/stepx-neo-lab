package studio.jiayu.neoagent;

/** Phone-local node sessions and actual ACTION_SET_TEXT attempts, never text-based identities. */
final class SearchNodeRules {
 static final class Session {
  private String run="";private long epoch=-1;private boolean active;private Object nativeNode,editor,key;
  synchronized boolean begin(String owner,long generation){if(owner==null||owner.isEmpty()||generation<0)return false;if(generation<=epoch)return active&&generation==epoch&&run.equals(owner);run=owner;epoch=generation;active=true;clear();return true;}
  private boolean current(String owner,long generation){return active&&epoch==generation&&run.equals(owner);}
  private void clear(){nativeNode=null;editor=null;key=null;}
  /** Android node equality identifies the native source node across refreshed wrappers. */
  synchronized Object focus(String owner,long generation,Object node,Object inputEditor,boolean eligible){
   if(!current(owner,generation))return null;
   if(!eligible||node==null){clear();return null;}
   if(key==null||nativeNode==null||!nativeNode.equals(node)||editor!=inputEditor){nativeNode=node;editor=inputEditor;key=new Object();}
   return key;
  }
  synchronized boolean accepts(String owner,long generation,Object expected,Object node,Object inputEditor){return current(owner,generation)&&key!=null&&key==expected&&nativeNode!=null&&nativeNode.equals(node)&&editor==inputEditor;}
  synchronized boolean end(String owner,long generation){if(!current(owner,generation))return false;active=false;clear();return true;}
 }
 interface Port {SearchInputRules.Sample read()throws Exception;void set(SearchInputRules.Sample expected,String value,Runnable dispatching)throws Exception;void pause(int ms)throws Exception;}
 static SearchInputRules.Result replace(Port port,SearchInputRules.Pending pending,SearchFinishRules finish,Object key,String run,long generation,String query)throws Exception{
  SearchInputRules.Sample before=port.read();
  if(pending.unresolved(key,run,before))throw new SearchInputRules.Rejected("上一条搜索输入尚未读回核验，未再次输入；请重新观察");
  if(before==null||!before.readable())throw new SearchInputRules.Rejected("搜索节点不能完整读回，未改写；请先聚焦并重新观察");
  if(before.text.equals(query))return SearchInputRules.Result.ALREADY_PRESENT;
  port.set(before,query,()->{if(!finish.written(run,generation,key,query,false))throw new IllegalStateException("搜索输入租约已变化，原动作未派发");pending.dispatched(key,run,query);});
  for(int n=0;n<8;n++){port.pause(100);SearchInputRules.Sample value;try{value=port.read();}catch(Exception unavailable){continue;}if(SearchInputRules.exact(value,query)){pending.unresolved(key,run,value);return SearchInputRules.Result.VERIFIED;}}
  return SearchInputRules.Result.DISPATCHED_UNVERIFIED;
 }
}
