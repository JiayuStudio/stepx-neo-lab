package studio.jiayu.neoagent;

import android.accessibilityservice.InputMethod;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.SurroundingText;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;

/** Phone-local search input with native editor and fresh accessible-node receipt paths. */
final class SearchInputBridge {
 private final ControlService owner;
 private final SearchInputRules.Pending pending=new SearchInputRules.Pending();
 private final SearchNodeRules.Session nodeSession=new SearchNodeRules.Session();
 void begin(String run,long generation){nodeSession.begin(run,generation);}
 void end(String run,long generation){nodeSession.end(run,generation);}
 SearchInputBridge(ControlService owner){this.owner=owner;}
 private InputMethod method(){return Build.VERSION.SDK_INT>=33?owner.getInputMethod():null;}
 private boolean ready(InputMethod method){
  if(method==null)return false;EditorInfo info=method.getCurrentInputEditorInfo();
  return info!=null&&SearchInputRules.ready(Build.VERSION.SDK_INT,method.getCurrentInputStarted(),method.getCurrentInputConnection()!=null,owner.allowed.equals(info.packageName),info.inputType,info.imeOptions);
 }
 JSONObject capture(ControlService.Observation observation)throws Exception{
  InputMethod method=method();boolean ready=ready(method);EditorInfo info=method==null?null:method.getCurrentInputEditorInfo();
  observation.searchEditor=ready?info:null;
  EditorInfo nodeEditor=info!=null&&owner.allowed.equals(info.packageName)?info:null;
  int focusedId=0;AccessibilityNodeInfo focusedNode=null;
  for(java.util.Map.Entry<Integer,AccessibilityNodeInfo> entry:observation.targets.entrySet())if(publicNodeEligible(entry.getValue())){
   if(focusedId!=0){focusedId=0;focusedNode=null;break;}focusedId=entry.getKey();focusedNode=entry.getValue();
  }
  observation.searchNodeKey=nodeSession.focus(owner.runId,observation.id.epoch,focusedNode,nodeEditor,focusedNode!=null);
  observation.searchNodeId=observation.searchNodeKey==null?0:focusedId;observation.searchNodeEditor=nodeEditor;observation.searchNodeShape=focusedNode==null?"":nodeShape(focusedNode);
  SearchInputRules.Sample node=focusedNode==null?null:nodeSample(focusedNode),sample=null;
  if(ready)try{sample=read(method.getCurrentInputConnection());}catch(Exception unavailable){/* No readable receipt. */}
  boolean readable=sample!=null&&sample.readable(),nodeReadable=node!=null&&node.readable();
  observation.searchNodeReadable=nodeReadable;
  boolean conflict=readable&&nodeReadable&&!sample.text.equals(node.text);
  SearchInputRules.Sample effective=conflict?null:nodeReadable?node:sample;
  boolean active=ready||observation.searchNodeKey!=null;
  JSONObject metadata=new JSONObject().put("focused",active).put("source",ready?"android_input_method_editor_info":"android_accessibility_search_node");
  metadata.put("input_editor_flag",(owner.getServiceInfo().flags&android.accessibilityservice.AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR)!=0);
  if(method!=null)metadata.put("session_started",method.getCurrentInputStarted()).put("connection_available",method.getCurrentInputConnection()!=null).put("editor_available",info!=null);
  if(info!=null)metadata.put("target_package_match",owner.allowed.equals(info.packageName)).put("input_type",info.inputType).put("ime_action",info.imeOptions&255).put("ime_submit_allowed",SearchInputRules.canSubmit(info.imeOptions));
  if(!active)return metadata;
  if(sample!=null)metadata.put("readback_length",sample.text==null?-1:sample.text.length()).put("readback_offset",sample.offset).put("selection_start",sample.start).put("selection_end",sample.end);
  Object key=operationKey(observation,observation.id.epoch);
  boolean unresolved=pending.unresolved(key,owner.runId,effective),reserved=effective!=null&&effective.readable()&&owner.searchFinish.submissionState(owner.runId,observation.id.epoch,key).dispatched(key,owner.runId,effective.text);
  SearchFinishRules.Decision finish=owner.searchFinish.beforeFinish(owner.runId,observation.id.epoch,key,effective==null?null:effective.text,true,effective!=null&&effective.readable(),unresolved,false);
  boolean priorReserved=reserved&&finish!=SearchFinishRules.Decision.NO_LOCAL_BLOCK,submitted=reserved&&!priorReserved;
  boolean nativeAllowed=ready&&SearchInputRules.canSubmit(info.imeOptions);
  boolean nodeAllowed=focusedNode!=null&&(nodeEditor==null||SearchInputRules.canSubmit(nodeEditor.imeOptions))&&(nativeAllowed||focusedNode.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER));
  observation.searchNodeSubmitAllowed=nodeAllowed;
  boolean nodeBound=focusedId!=0&&observation.searchNodeKey!=null&&observation.searchNodeId==focusedId;
  boolean nodeSubmit=nodeBound&&nodeReadable&&!conflict&&!unresolved&&!reserved&&nodeAllowed&&!node.text.trim().isEmpty()&&node.text.length()<=500;
  boolean nodeWrite=nodeBound&&nodeReadable&&!conflict&&!unresolved,systemWrite=readable&&!conflict&&!unresolved;
  boolean boundSystemWrite=systemWrite&&nodeBound&&nodeEditor==observation.searchEditor;
  metadata.put("node_readback_available",nodeReadable).put("system_readback_available",readable)
   .put("can_write_node",nodeWrite).put("can_write_system",systemWrite).put("can_write_with_id",nodeWrite||boundSystemWrite);
  if(focusedId!=0){metadata.put("submit_node_id",focusedId).put("write_node_id",focusedId);org.json.JSONArray nodes=observation.json.getJSONArray("nodes");for(int i=0;i<nodes.length();i++)if(nodes.getJSONObject(i).optInt("id")==focusedId)nodes.getJSONObject(i).put("can_write",nodeWrite||boundSystemWrite).put("write_path",nodeWrite?"accessibility_set_text_confirm_readback":boundSystemWrite?"system_input_connection_bound_to_focused_node":"unavailable").put("submit_search",nodeBound&&nodeReadable&&nodeAllowed).put("submit_path",nodeBound&&nodeReadable&&nodeAllowed?nativeAllowed?"system_ime_search_with_focused_node_readback":"accessibility_search_node":"unavailable");}
  metadata.put("finish_input_blocked",finish!=SearchFinishRules.Decision.NO_LOCAL_BLOCK).put("finish_input_instruction",SearchFinishRules.instruction(finish)).put("prior_query_submit_reserved",priorReserved)
   .put("can_submit_node",nodeSubmit).put("search_readback_conflict",conflict).put("purpose","搜索输入框").put("native_action",ready?"IME_ACTION_SEARCH":"ACTION_IME_ENTER")
   .put("readback_available",readable||nodeReadable).put("pending_write_unverified",unresolved).put("can_write",nodeWrite||systemWrite).put("submit_dispatched_unverified",submitted)
   .put("can_submit",nativeAllowed&&readable&&!conflict&&!unresolved&&!reserved&&!sample.text.trim().isEmpty()&&sample.text.length()<=500)
   .put("write_mode",nodeWrite?"accessibility_set_text_confirm_readback":boundSystemWrite?"system_input_connection_bound_to_focused_node":systemWrite?sample.offset==-1?"select_all_confirm_replace":"native_readback_replace":"unavailable")
   .put("instruction",conflict?"系统编辑器与聚焦节点的搜索词读回不一致，未输入或提交；先重新观察，不盲目重复。":unresolved?"上一条搜索输入已派发但未核验，禁止再次type或盲目提交；核对新画面，必要时返回搜索入口。":priorReserved?"该编辑器同词已有提交尝试，未重复派发；请观察实际结果或重新进入搜索页。":submitted?"当前搜索动作已派发但结果未核验，不要重复提交；观察结果页。":nodeEditor!=null&&!SearchInputRules.canSubmit(nodeEditor.imeOptions)?"当前编辑器未允许系统搜索动作；填写后使用应用内可见搜索按钮，不调用submit_search或按键盘坐标。":nodeSubmit?"当前聚焦的搜索节点可完整读回；填写后重新观察，按submit_node_id调用device.submit_search，再核对实际结果。":boundSystemWrite&&!nodeReadable?"节点自身不能完整读回，但同一聚焦节点绑定的系统编辑器可读回；可用type(id=write_node_id,text)或省略id填写，系统会重新核对节点和编辑器会话。填写后重新观察；仅can_submit=true时可省略id调用submit_search，或使用应用内可见搜索按钮。":readable||nodeReadable?"当前搜索词可完整读回，可type(text)填写；填写不自动提交。重新观察可用的搜索入口或提交动作，再核对实际结果。":"当前搜索输入不能完整读回，can_write=false；先重新聚焦与观察，不重复输入或盲目提交。");
  return metadata;
 }
 private Object operationKey(ControlService.Observation source,long generation)throws SearchSubmitRules.Rejected{return owner.searchFinish.operationKey(owner.runId,generation,source.searchNodeKey,source.searchEditor);}
 /** Local fresh readback only; NO_LOCAL_BLOCK never proves a result appeared. */
 SearchFinishRules.Decision finishDecision(ControlService.Observation source,long generation)throws Exception{
  if(source.searchEditor==null&&source.searchNodeKey==null)return SearchFinishRules.Decision.NO_LOCAL_BLOCK;
  return owner.ui(()->{
   SearchInputRules.Sample sample=null;
   if(source.searchNodeKey!=null)sample=readSearchNode(source,source.searchNodeId,generation);
   if(source.searchEditor!=null&&(sample==null||!sample.readable()))try{sample=read(connection(source,generation));}catch(RuntimeException unavailable){/* Missing readback remains pending. */}
   Object key=operationKey(source,generation);boolean pendingWrite=pending.unresolved(key,owner.runId,sample);
   return owner.searchFinish.beforeFinish(owner.runId,generation,key,sample==null?null:sample.text,true,sample!=null&&sample.readable(),pendingWrite,false);
  });
 }
 private boolean publicNodeEligible(AccessibilityNodeInfo node){return node!=null&&node.isVisibleToUser()&&node.isFocused()&&node.isEnabled()&&node.isEditable()&&!node.isPassword()&&owner.allowed.equals(String.valueOf(node.getPackageName()))&&SearchQueryPolicy.searchField(node.getViewIdResourceName(),String.valueOf(node.getHintText()),String.valueOf(node.getContentDescription()));}
 private static String nodeShape(AccessibilityNodeInfo node){android.graphics.Rect box=new android.graphics.Rect();node.getBoundsInScreen(box);return node.getWindowId()+"|"+node.getPackageName()+"|"+node.getViewIdResourceName()+"|"+node.getClassName()+"|"+box.toShortString();}
 private AccessibilityNodeInfo currentNode(ControlService.Observation source,int id,long generation)throws Exception{return currentNode(source,id,generation,false);}
 private AccessibilityNodeInfo currentNode(ControlService.Observation source,int id,long generation,boolean readingDispatchedWrite)throws Exception{
  owner.guard(generation);owner.validateObservation(source,generation,false);AccessibilityNodeInfo node=source.targets.get(id);
  if(id!=source.searchNodeId||node==null||!node.refresh()||(!readingDispatchedWrite&&!GuiRevision.identity(node).equals(source.identities.get(id)))||!nodeShape(node).equals(source.searchNodeShape)||!publicNodeEligible(node)||!nodeSession.accepts(owner.runId,generation,source.searchNodeKey,node,source.searchNodeEditor))throw new ControlService.UiChangedException();
  InputMethod method=method();EditorInfo info=method==null?null:method.getCurrentInputEditorInfo();EditorInfo samePackage=info!=null&&owner.allowed.equals(info.packageName)?info:null;
  if(samePackage!=source.searchNodeEditor)throw new ControlService.UiChangedException();
  return node;
 }
 private SearchInputRules.Sample readSearchNode(ControlService.Observation source,int id,long generation)throws Exception{return readSearchNode(source,id,generation,false);}
 private SearchInputRules.Sample readSearchNode(ControlService.Observation source,int id,long generation,boolean readingDispatchedWrite)throws Exception{
  AccessibilityNodeInfo node=currentNode(source,id,generation,readingDispatchedWrite);SearchInputRules.Sample value=nodeSample(node);
  if(source.searchEditor!=null){SearchInputRules.Sample nativeValue=null;try{nativeValue=read(connection(source,generation));}catch(RuntimeException unavailable){}if(nativeValue!=null&&nativeValue.readable()&&value.readable()&&!value.text.equals(nativeValue.text))throw new ControlService.LocalBoundaryException("系统编辑器与聚焦节点读回不一致，未派发；请重新观察");}
  return value;
 }
 String writeNode(ControlService.Observation source,int id,String text,long generation)throws Exception{
  SearchWriteRouteRules.Path path=owner.ui(()->{
   AccessibilityNodeInfo node=currentNode(source,id,generation);SearchInputRules.Sample nativeSample=null;
   boolean bound=source.searchEditor!=null&&source.searchNodeEditor==source.searchEditor;
   if(bound)try{nativeSample=read(connection(source,generation));}catch(RuntimeException unavailable){/* No reliable system receipt. */}
   return SearchWriteRouteRules.choose(id,source.searchNodeId,true,bound,nodeSample(node),nativeSample);
  });
  if(path==SearchWriteRouteRules.Path.CONFLICT)throw new ControlService.LocalBoundaryException("系统编辑器与聚焦节点读回不一致，未派发；请重新观察");
  if(path==SearchWriteRouteRules.Path.STALE_TARGET)throw new ControlService.UiChangedException();
  if(path==SearchWriteRouteRules.Path.SYSTEM)return write(source,id,text,generation);
  if(path!=SearchWriteRouteRules.Path.NODE)throw new ControlService.LocalBoundaryException("搜索节点与对应系统编辑器均不能完整读回，未改写；请先聚焦并重新观察");
  validateQuery(text);final Object key=operationKey(source,generation);final boolean[] dispatched={false};
  SearchNodeRules.Port port=new SearchNodeRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->readSearchNode(source,id,generation,dispatched[0]));}
   public void set(SearchInputRules.Sample expected,String value,Runnable dispatching)throws Exception{owner.ui(()->{synchronized(owner){AccessibilityNodeInfo node=currentNode(source,id,generation);SearchInputRules.Sample now=readSearchNode(source,id,generation);if(!expected.same(now))throw new ControlService.UiChangedException();if(!owner.setPublicText(node,value,()->{dispatching.run();dispatched[0]=true;}))throw new ControlService.LocalBoundaryException("搜索节点未确认输入已接收；先观察，不重复填写");return true;}});}
   public void pause(int ms)throws Exception{Thread.sleep(ms);owner.check(generation);}
  };
  try{SearchInputRules.Result result=SearchNodeRules.replace(port,pending,owner.searchFinish,key,owner.runId,generation,text);return result==SearchInputRules.Result.ALREADY_PRESENT?"搜索节点已是本次关键词，未重复输入；搜索结果仍须核对":result==SearchInputRules.Result.VERIFIED?"搜索节点已填写并完整读回；尚未提交，请重新观察搜索入口后提交，再核对结果页":"搜索节点填写已派发但未完整读回；先观察，禁止重复输入或盲目提交";}catch(SearchInputRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
 }
 String submitNode(ControlService.Observation source,int id,long generation)throws Exception{
  if(!source.searchNodeSubmitAllowed)throw new ControlService.LocalBoundaryException("当前编辑器未允许系统搜索动作，请使用应用内可见搜索按钮");
  final Object key=operationKey(source,generation);
  SearchSubmitRules.Port port=new SearchSubmitRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->readSearchNode(source,id,generation));}
   public void selectAll()throws Exception{throw new SearchSubmitRules.Rejected("节点读回范围不完整，未提交");}
   public void pause(int ms)throws Exception{Thread.sleep(ms);owner.check(generation);}
   public void search(SearchInputRules.Sample expected,Runnable dispatching)throws Exception{owner.ui(()->{synchronized(owner){AccessibilityNodeInfo node=currentNode(source,id,generation);SearchInputRules.Sample now=readSearchNode(source,id,generation);if(!expected.same(now))throw new ControlService.UiChangedException();if(source.searchNodeEditor!=null&&!SearchInputRules.canSubmit(source.searchNodeEditor.imeOptions))throw new ControlService.LocalBoundaryException("当前编辑器未允许搜索动作，原动作未派发");if(!node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER))throw new ControlService.UiChangedException();validateQuery(now.text);dispatching.run();owner.searchFinish.submitted(owner.runId,generation,key,now.text);if(!node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId()))throw new ControlService.LocalBoundaryException("搜索动作接收状态未核验；先观察实际结果，不重复提交");return true;}});}
  };
  try{return SearchSubmitRules.submit(port,pending,owner.searchFinish.submissionState(owner.runId,generation,key),key,owner.runId,this::validateQuery);}catch(SearchSubmitRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
 }
 private InputMethod.AccessibilityInputConnection connection(ControlService.Observation source,long generation)throws Exception{
  owner.guard(generation);owner.validateObservation(source,generation,false);InputMethod method=method();
  // Android returns a new connection wrapper on each getter call. EditorInfo identity
  // binds this operation to the editor session sampled with the screenshot instead.
  if(source.searchEditor==null||!ready(method)||method.getCurrentInputEditorInfo()!=source.searchEditor)
   throw new ControlService.UiChangedException();
  return method.getCurrentInputConnection();
 }
 private static SearchInputRules.Sample read(InputMethod.AccessibilityInputConnection connection){
  SurroundingText text=connection.getSurroundingText(513,513,0);
  return text==null?null:new SearchInputRules.Sample(text.getText()==null?null:text.getText().toString(),text.getOffset(),text.getSelectionStart(),text.getSelectionEnd());
 }
 private static SearchInputRules.Sample nodeSample(AccessibilityNodeInfo node){return new SearchInputRules.Sample(node.getText()==null?null:node.getText().toString(),0,node.getTextSelectionStart(),node.getTextSelectionEnd());}
 boolean nodeEligible(ControlService.Observation source,int id){
  AccessibilityNodeInfo node=source.targets.get(id);
  return source.searchEditor!=null&&source.searchNodeKey!=null&&id==source.searchNodeId&&node!=null&&node.isFocused()&&node.isEnabled()&&node.isEditable()&&!node.isPassword()
   &&SearchQueryPolicy.searchField(node.getViewIdResourceName(),String.valueOf(node.getHintText()),String.valueOf(node.getContentDescription()))&&nodeSample(node).readable();
 }
 private SearchInputRules.Sample readNode(ControlService.Observation source,int id,long generation)throws Exception{
  InputMethod.AccessibilityInputConnection live=connection(source,generation);AccessibilityNodeInfo node=source.targets.get(id);
  if(node==null||!node.refresh()||!GuiRevision.identity(node).equals(source.identities.get(id)))throw new ControlService.UiChangedException();
  if(!nodeEligible(source,id))throw new ControlService.LocalBoundaryException("当前搜索节点没有可靠焦点或完整读回，未提交；请先聚焦搜索框");
  SearchInputRules.Sample sample=nodeSample(node),nativeSample=null;try{nativeSample=read(live);}catch(RuntimeException unavailable){/* The focused public node is the readback source. */}
  if(nativeSample!=null&&nativeSample.readable()&&!sample.text.equals(nativeSample.text))throw new ControlService.LocalBoundaryException("系统编辑器与聚焦节点的搜索词读回不一致，未提交；请重新观察");
  return sample;
 }
 String submit(ControlService.Observation source,long generation)throws Exception{
  return submit(source,-1,generation);
 }
 String submit(ControlService.Observation source,int nodeId,long generation)throws Exception{
  final Object key=operationKey(source,generation);
  SearchSubmitRules.Port port=new SearchSubmitRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->nodeId>0?readNode(source,nodeId,generation):SearchInputBridge.read(connection(source,generation)));}
   public void selectAll()throws Exception{owner.ui(()->{connection(source,generation).performContextMenuAction(android.R.id.selectAll);return true;});}
   public void pause(int milliseconds)throws Exception{Thread.sleep(milliseconds);owner.check(generation);}
   public void search(SearchInputRules.Sample expected,Runnable dispatching)throws Exception{owner.ui(()->{
    InputMethod.AccessibilityInputConnection live=connection(source,generation);if(!SearchInputRules.canSubmit(source.searchEditor.imeOptions))throw new ControlService.LocalBoundaryException("当前编辑器未允许系统搜索动作，请使用应用内可见搜索按钮");SearchInputRules.Sample now=nodeId>0?readNode(source,nodeId,generation):SearchInputBridge.read(live);
    if(!expected.same(now))throw new ControlService.UiChangedException();
    validateQuery(now.text);synchronized(owner){owner.guard(generation);InputMethod.AccessibilityInputConnection current=connection(source,generation);dispatching.run();owner.searchFinish.submitted(owner.runId,generation,key,now.text);current.performEditorAction(EditorInfo.IME_ACTION_SEARCH);}return true;
   });}
  };
  try{return SearchSubmitRules.submit(port,pending,owner.searchFinish.submissionState(owner.runId,generation,key),key,owner.runId,this::validateQuery);}
  catch(SearchSubmitRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
 }
 private void validateQuery(String text)throws Exception{
  if(!SearchQueryPolicy.allowed(owner.task,owner.activeRequest==null?"":owner.activeRequest.original,owner.activeRequest==null?"":owner.activeRequest.target,owner.allowed,text,new AppAccess(owner).independentSearchFields(owner.allowed)))
   throw new ControlService.LocalBoundaryException("搜索词尚未核对，未提交；请保留原目标对象");
 }
 private SearchInputRules.Sample readBoundSystem(ControlService.Observation source,int nodeId,long generation,boolean readingDispatchedWrite)throws Exception{
  AccessibilityNodeInfo node=null;
  if(nodeId>0){
   node=currentNode(source,nodeId,generation,readingDispatchedWrite);
   if(source.searchEditor==null||source.searchNodeEditor!=source.searchEditor)throw new ControlService.UiChangedException();
  }
  SearchInputRules.Sample sample=read(connection(source,generation));
  SearchInputRules.Sample nodeValue=node==null?null:nodeSample(node);
  if(nodeValue!=null&&nodeValue.readable()&&sample!=null&&sample.readable()&&!nodeValue.text.equals(sample.text))
   throw new ControlService.LocalBoundaryException("系统编辑器与聚焦节点读回不一致，未派发；请重新观察");
  return sample;
 }
 String write(ControlService.Observation source,String text,long generation)throws Exception{
  return write(source,source.searchNodeKey==null?0:source.searchNodeId,text,generation);
 }
 private String write(ControlService.Observation source,int nodeId,String text,long generation)throws Exception{
  if(!SearchQueryPolicy.allowed(owner.task,owner.activeRequest==null?"":owner.activeRequest.original,owner.activeRequest==null?"":owner.activeRequest.target,owner.allowed,text,new AppAccess(owner).independentSearchFields(owner.allowed)))
   throw new ControlService.LocalBoundaryException("搜索词尚未核对，未写入；请保留原目标对象");
  final Object key=operationKey(source,generation);final boolean[] dispatched={false};
  SearchInputRules.Port nativePort=new SearchInputRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->readBoundSystem(source,nodeId,generation,dispatched[0]));}
   public void selectAll()throws Exception{owner.ui(()->{SearchInputRules.Sample now=readBoundSystem(source,nodeId,generation,false);if(now==null||!now.readable())throw new ControlService.UiChangedException();connection(source,generation).performContextMenuAction(android.R.id.selectAll);return true;});}
   public void commit(SearchInputRules.Sample expected,String value,Runnable dispatching)throws Exception{
    owner.ui(()->{
     synchronized(owner){SearchInputRules.Sample now=readBoundSystem(source,nodeId,generation,false);
     if(!expected.same(now))throw new ControlService.UiChangedException();
     InputMethod.AccessibilityInputConnection connection=connection(source,generation);
     // A dispatch that cannot be read back remains pending; never automatically replay it.
     dispatching.run();dispatched[0]=true;connection.commitText(value,1,null);return true;}
    });
   }
   public void pause(int milliseconds)throws Exception{Thread.sleep(milliseconds);owner.check(generation);}
  };
  SearchInputRules.Result result;
  try{result=SearchWriteRouteRules.replaceSystem(nativePort,pending,owner.searchFinish,key,owner.runId,generation,text);}
  catch(SearchInputRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
  if(result==SearchInputRules.Result.ALREADY_PRESENT)return "系统搜索框已是本次关键词，未重复输入；搜索结果仍须核对";
  if(result==SearchInputRules.Result.VERIFIED)return "关键词已由手机系统输入连接写入并精确读回；输入未提交，请重新观察后调用device.submit_search（系统搜索框可省略id），再核对结果页";
  return "关键词已派发但尚未读回核验，禁止再次type或盲目提交；先核对新画面，不把输入回执当搜索完成";
 }
}
