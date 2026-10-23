package studio.jiayu.neoagent;

import android.view.inputmethod.EditorInfo;
import org.json.JSONObject;

/** Search-only runtime adapter. The active IME remains the owner of its current connection. */
final class AgentImeInputBridge {
 final ControlService owner;
 final SearchInputRules.Pending writes=new SearchInputRules.Pending();
 final SearchSubmitRules.State submissions=new SearchSubmitRules.State();
 private AgentInputMethodService remembered;
 private String rememberedRun="";
 private final java.util.HashMap<Long,Object> editors=new java.util.HashMap<>();
 AgentImeInputBridge(ControlService service){owner=service;}
 JSONObject capture(ControlService.Observation source,long g)throws Exception{
  AgentInputMethodService ime=AgentInputMethodService.instance;
  if(ime==null||!owner.agentImeLease.held(g))return null;
  AgentInputMethodService.EditorSnapshot editor;
  try{editor=ime.snapshot(owner,g);}catch(Exception unavailable){owner.check(g);return null;}
  if(!editor.connectionReady||editor.imeAction!=EditorInfo.IME_ACTION_SEARCH)return null;
  String text=null;try{text=ime.readText(owner,g,editor.epoch,501);}catch(Exception unavailable){owner.check(g);}
  if(text==null||text.length()>500)return null;
  if(remembered!=ime||!rememberedRun.equals(owner.runId)){remembered=ime;rememberedRun=owner.runId;editors.clear();}
  Object key=editors.get(editor.epoch);
  if(key==null){if(editors.size()>=32)throw new ControlService.LocalBoundaryException("本轮搜索编辑器记录已达上限，原目标保留");key=new Object();editors.put(editor.epoch,key);}
  source.agentIme=ime;source.agentSearchEditor=editor;source.agentSearchKey=key;
  source.agentSearchText=text;
  SearchInputRules.Sample sample=text==null?null:new SearchInputRules.Sample(text,0,0,0);
  boolean readable=text!=null&&text.length()<=500,unresolved=writes.unresolved(key,owner.runId,sample),submitted=readable&&submissions.dispatched(key,owner.runId,text);
  return new JSONObject().put("focused",true).put("source","neo_agent_input_method").put("engine","agent_ime").put("purpose","搜索输入框").put("native_action","IME_ACTION_SEARCH")
   .put("readback_available",readable).put("readback_length",text==null?-1:text.length()).put("pending_write_unverified",unresolved).put("submit_dispatched_unverified",submitted)
   .put("can_write",readable&&!unresolved).put("can_submit",readable&&!unresolved&&!submitted&&!text.trim().isEmpty())
   .put("instruction",unresolved?"上一条输入已派发但尚未完整核验；先观察，不重复输入或提交。":submitted?"当前搜索已派发；观察实际结果，不重复提交。":readable?"Agent输入法已绑定当前目标应用的搜索编辑器。直接type(text)或submit_search，必须省略id；输入和提交后都要观察实际结果，不以派发回执代替结果页。":"Agent输入法已连接，但完整搜索词暂不能读回；先重新观察或重新聚焦，不盲目输入或提交。");
 }
 private AgentInputMethodService live(ControlService.Observation source,long g)throws Exception{
  owner.guard(g);owner.validateObservation(source,g,false);
  AgentInputMethodService ime=AgentInputMethodService.instance;
  if(ime==null||ime!=source.agentIme||source.agentSearchEditor==null)throw new ControlService.UiChangedException();
  AgentInputMethodService.EditorSnapshot current=ime.snapshot(owner,g);
  if(current.epoch!=source.agentSearchEditor.epoch||current.imeAction!=EditorInfo.IME_ACTION_SEARCH)throw new ControlService.UiChangedException();
  return ime;
 }
 private String read(ControlService.Observation source,long g)throws Exception{return live(source,g).readText(owner,g,source.agentSearchEditor.epoch,501);}
 private void query(String text)throws Exception{
  if(text==null||text.length()>500||!SearchQueryPolicy.allowed(owner.task,owner.activeRequest==null?"":owner.activeRequest.original,owner.activeRequest==null?"":owner.activeRequest.target,owner.allowed,text,new AppAccess(owner).independentSearchFields(owner.allowed)))
   throw new ControlService.LocalBoundaryException("搜索词尚未核对，原输入未派发；请保留目标对象和明确规格");
 }
 private void selected(ControlService.Observation source,Action action,String current)throws Exception{
  if(!action.data.has("id"))return;
  throw new ControlService.LocalBoundaryException("Agent输入法只绑定当前编辑器，不接受节点id；请重新观察后省略id输入或提交");
 }
 String write(ControlService.Observation source,Action action,long g)throws Exception{
  String text=action.data.getString("text");query(text);
  final boolean[] already={false};
  owner.ui(()->{synchronized(owner){AgentInputMethodService ime=live(source,g);String before=read(source,g);selected(source,action,before);
   if(source.agentSearchText==null||!source.agentSearchText.equals(before))throw new ControlService.UiChangedException();
   SearchInputRules.Sample sample=new SearchInputRules.Sample(before,0,0,0);
   if(writes.unresolved(source.agentSearchKey,owner.runId,sample))throw new ControlService.LocalBoundaryException("上一条输入尚未核验，不重复输入");
   if(before.equals(text)){already[0]=true;return true;}
   writes.dispatched(source.agentSearchKey,owner.runId,text);ime.replaceText(owner,g,source.agentSearchEditor.epoch,text);return true;
  }});
  if(already[0])return "Agent搜索编辑器已是本次关键词，未重复输入；请提交搜索后观察结果";
  for(int n=0;n<8;n++){Thread.sleep(100);owner.check(g);try{String value=owner.ui(()->read(source,g));if(text.equals(value)){writes.unresolved(source.agentSearchKey,owner.runId,new SearchInputRules.Sample(value,0,0,0));return "Agent输入法已填写并完整读回关键词；尚未提交，请使用submit_search后观察结果";}}catch(ControlService.UiChangedException changed){throw changed;}catch(Exception unavailable){owner.check(g);}}
  return "Agent输入法填写已派发但未完整读回；先观察，禁止重复输入或盲目提交";
 }
 String submit(ControlService.Observation source,Action action,long g)throws Exception{
  SearchSubmitRules.Port port=new SearchSubmitRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->{String value=AgentImeInputBridge.this.read(source,g);selected(source,action,value);if(source.agentSearchText==null||!source.agentSearchText.equals(value))throw new ControlService.UiChangedException();return new SearchInputRules.Sample(value,0,0,0);});}
   public void selectAll()throws Exception{throw new ControlService.LocalBoundaryException("完整字段读回不应需要未知范围选择");}
   public void pause(int ms)throws Exception{Thread.sleep(ms);owner.check(g);}
   public void search(SearchInputRules.Sample expected,Runnable dispatching)throws Exception{owner.ui(()->{synchronized(owner){AgentInputMethodService ime=live(source,g);String value=AgentImeInputBridge.this.read(source,g);selected(source,action,value);if(!expected.text.equals(value))throw new ControlService.UiChangedException();query(value);dispatching.run();if(!ime.submitSearch(owner,g,source.agentSearchEditor.epoch))throw new ControlService.LocalBoundaryException("系统未确认搜索动作已接收；先观察实际结果，不重复提交");return true;}});}
  };
  try{return SearchSubmitRules.submit(port,writes,submissions,source.agentSearchKey,owner.runId,this::query);}catch(SearchSubmitRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
 }
}
