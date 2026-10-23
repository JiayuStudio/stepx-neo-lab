package studio.jiayu.neoagent;

/** Negative evidence only. Passing this guard never proves a search or a goal completed. */
final class SearchFinishRules {
 enum Decision { NO_LOCAL_BLOCK, WAIT_FOR_READBACK, SEARCH_NOT_SUBMITTED }
 private String run="",query="";
 private long epoch=-1;
 private Object editor;
 private boolean active,readbackVerified,submitted;
 private final java.util.IdentityHashMap<Object,SearchSubmitRules.State> submissionStates=new java.util.IdentityHashMap<>();

 /** Call from the owner's start/resume/stage transition, never from a model response. */
 synchronized boolean begin(String owner,long generation){
  if(owner==null||owner.isEmpty()||generation<0)return false;
  if(generation<=epoch)return active&&generation==epoch&&owner.equals(run);
  run=owner;epoch=generation;active=true;clearWrite();submissionStates.clear();return true;
 }
 private boolean current(String owner,long generation){return active&&epoch==generation&&run.equals(owner);}
 private void clearWrite(){editor=null;query="";readbackVerified=false;submitted=false;}

 /** Record only an actual Agent write to a locally validated search editor.
  * An already-present value, an ordinary field or a model proposal is not a write.
  * The editor key must be stable across readback and must not contain the field text.
  */
 synchronized boolean written(String owner,long generation,Object key,String value,boolean verified){
  if(!current(owner,generation)||key==null||value==null||value.trim().isEmpty()||value.length()>500)return false;
  if(!submissionStates.containsKey(key)&&submissionStates.size()>=32)return false;
  editor=key;query=value;readbackVerified=verified;submitted=false;
  // An actual new input is a new search attempt, including A -> B -> A. Merely
  // finding the same text already present never reaches this method.
  submissionStates.put(key,new SearchSubmitRules.State());return true;
 }

 /** Keep a node write and its matching native editor on the same local receipt key. */
 synchronized Object operationKey(String owner,long generation,Object preferred,Object fallback)throws SearchSubmitRules.Rejected{
  if(!current(owner,generation))throw new SearchSubmitRules.Rejected("搜索输入租约已变化，原动作未派发");
  if(editor!=null&&(editor==preferred||editor==fallback))return editor;
  return preferred!=null?preferred:fallback;
 }

 /** Reservations belong to the current actual-write cycle, not all prior words in a run. */
 synchronized SearchSubmitRules.State submissionState(String owner,long generation,Object key)throws SearchSubmitRules.Rejected{
  if(!current(owner,generation)||key==null)throw new SearchSubmitRules.Rejected("搜索输入租约已变化，原动作未派发");
  SearchSubmitRules.State state=submissionStates.get(key);
  if(state==null){if(submissionStates.size()>=32)throw new SearchSubmitRules.Rejected("本轮搜索编辑器记录已达上限，原动作未派发");state=new SearchSubmitRules.State();submissionStates.put(key,state);}
  return state;
 }

 /** This records a locally dispatched submit, not a result-page or playback success. */
 synchronized boolean submitted(String owner,long generation,Object key,String value){
  if(!current(owner,generation)||editor==null||editor!=key||!query.equals(value))return false;
  submitted=true;return true;
 }

 /** Inspect the latest fresh local snapshot before accepting an ordinary finish.
  * No recorded write, a departed editor or a new lease creates no local blocker.
  * submissionObserved must come from the matching local native dispatch ledger.
  */
 synchronized Decision beforeFinish(String owner,long generation,Object key,String value,boolean focused,boolean readable,boolean pendingWrite,boolean submissionObserved){
  if(!current(owner,generation)||editor==null||key!=editor||!focused)return Decision.NO_LOCAL_BLOCK;
  if(pendingWrite||!readable||value==null)return Decision.WAIT_FOR_READBACK;
  if(!query.equals(value))return readbackVerified?Decision.NO_LOCAL_BLOCK:Decision.WAIT_FOR_READBACK;
  readbackVerified=true;
  return submitted||submissionObserved?Decision.NO_LOCAL_BLOCK:Decision.SEARCH_NOT_SUBMITTED;
 }

 /** Retire this lease without allowing its callbacks to reopen it. */
 synchronized boolean end(String owner,long generation){
  if(!current(owner,generation))return false;
  active=false;clearWrite();submissionStates.clear();return true;
 }
 static String instruction(Decision decision){
  if(decision==Decision.WAIT_FOR_READBACK)return "本轮搜索输入尚未完整读回核验，不能结束目标；请重新观察，不重复输入或盲目提交。";
  if(decision==Decision.SEARCH_NOT_SUBMITTED)return "本轮关键词已填入当前搜索编辑器，尚无本机提交回执；请完成搜索或进入实际结果后重新观察。输入、提交回执均不代表目标完成。";
  return "";
 }
}
