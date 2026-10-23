package studio.jiayu.neoagent;

import android.accessibilityservice.InputMethod;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.SurroundingText;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;

/** Phone-local search input when an app supplies an IME editor but no accessible node. */
final class SearchInputBridge {
 private final ControlService owner;
 private final SearchInputRules.Pending pending=new SearchInputRules.Pending();
 private final SearchSubmitRules.State submissions=new SearchSubmitRules.State();
 SearchInputBridge(ControlService owner){this.owner=owner;}
 private InputMethod method(){return Build.VERSION.SDK_INT>=33?owner.getInputMethod():null;}
 private boolean ready(InputMethod method){
  if(method==null)return false;EditorInfo info=method.getCurrentInputEditorInfo();
  return info!=null&&SearchInputRules.ready(Build.VERSION.SDK_INT,method.getCurrentInputStarted(),method.getCurrentInputConnection()!=null,owner.allowed.equals(info.packageName),info.inputType,info.imeOptions);
 }
 JSONObject capture(ControlService.Observation observation)throws Exception{
  InputMethod method=method();boolean ready=ready(method);
  observation.searchEditor=ready?method.getCurrentInputEditorInfo():null;
  JSONObject metadata=new JSONObject().put("focused",ready).put("source","android_input_method_editor_info");
  metadata.put("input_editor_flag",(owner.getServiceInfo().flags&android.accessibilityservice.AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR)!=0);
  if(method!=null){
   EditorInfo info=method.getCurrentInputEditorInfo();
   metadata.put("session_started",method.getCurrentInputStarted()).put("connection_available",method.getCurrentInputConnection()!=null).put("editor_available",info!=null);
   if(info!=null)metadata.put("target_package_match",owner.allowed.equals(info.packageName)).put("input_type",info.inputType).put("ime_action",info.imeOptions&255);
  }
  if(ready){
   SearchInputRules.Sample sample=null;
   try{sample=read(method.getCurrentInputConnection());}catch(Exception unavailable){/* Focus alone never establishes readable input. */}
   // Selection and length diagnostics disclose no additional search text.
   if(sample!=null)metadata.put("readback_length",sample.text==null?-1:sample.text.length()).put("readback_offset",sample.offset).put("selection_start",sample.start).put("selection_end",sample.end);
   int focusedId=0;SearchInputRules.Sample focusedSample=null;
   for(java.util.Map.Entry<Integer,AccessibilityNodeInfo> entry:observation.targets.entrySet())if(nodeEligible(observation,entry.getKey())){if(focusedId!=0){focusedId=0;focusedSample=null;break;}focusedId=entry.getKey();focusedSample=nodeSample(entry.getValue());}
   boolean readable=sample!=null&&sample.readable(),conflict=readable&&focusedSample!=null&&!sample.text.equals(focusedSample.text);
   SearchInputRules.Sample effective=conflict?null:focusedSample!=null?focusedSample:sample;
   boolean unresolved=pending.unresolved(observation.searchEditor,owner.runId,effective);
   boolean submitted=effective!=null&&submissions.dispatched(observation.searchEditor,owner.runId,effective.text);
   boolean nodeSubmit=focusedSample!=null&&!conflict&&!unresolved&&!submitted&&!focusedSample.text.trim().isEmpty()&&focusedSample.text.length()<=500;
   metadata.put("can_submit_node",nodeSubmit).put("search_readback_conflict",conflict);
   if(focusedId!=0){metadata.put("submit_node_id",focusedId).put("node_readback_available",true);org.json.JSONArray nodes=observation.json.getJSONArray("nodes");for(int i=0;i<nodes.length();i++)if(nodes.getJSONObject(i).optInt("id")==focusedId)nodes.getJSONObject(i).put("submit_search",true).put("submit_path","system_ime_search_with_focused_node_readback");}
   metadata.put("purpose","搜索输入框").put("native_action","IME_ACTION_SEARCH").put("readback_available",readable).put("pending_write_unverified",unresolved).put("can_write",readable&&!unresolved).put("submit_dispatched_unverified",submitted).put("can_submit",readable&&!conflict&&!unresolved&&!submitted&&!sample.text.trim().isEmpty()&&sample.text.length()<=500)
    .put("write_mode",readable&&sample.offset==-1?"select_all_confirm_replace":"native_readback_replace")
    .put("instruction",conflict?"系统编辑器与聚焦节点的搜索词读回不一致，未提交；先重新观察，不盲目重复输入或搜索。":unresolved?"上一条输入已派发但未核验，禁止再次type或盲目提交；核对新画面，必要时返回搜索入口。":submitted?"本编辑器的当前搜索已派发但结果未核验，不要重复提交；观察结果页或返回搜索入口。":nodeSubmit?"当前聚焦的搜索节点可完整读回，可用device.submit_search并指定submit_node_id；手机会重新核对节点、系统编辑器与搜索词后派发明确的IME_SEARCH。派发不代表结果页已出现。":readable?"系统已确认搜索焦点和有效读回。可以type(text)省略id；填写后重新观察，can_submit=true时用device.submit_search省略id，派发系统搜索动作。输入或派发回执不代表结果页已出现。": "系统有搜索焦点，但当前输入不能有效读回，can_write=false。不要重复type；重新聚焦应用搜索框、退出语音输入，或返回后选择其他可见搜索入口。");
  }
  return metadata;
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
  return source.searchEditor!=null&&node!=null&&node.isFocused()&&node.isEnabled()&&node.isEditable()&&!node.isPassword()
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
  SearchSubmitRules.Port port=new SearchSubmitRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->nodeId>0?readNode(source,nodeId,generation):SearchInputBridge.read(connection(source,generation)));}
   public void selectAll()throws Exception{owner.ui(()->{connection(source,generation).performContextMenuAction(android.R.id.selectAll);return true;});}
   public void pause(int milliseconds)throws Exception{Thread.sleep(milliseconds);owner.check(generation);}
   public void search(SearchInputRules.Sample expected,Runnable dispatching)throws Exception{owner.ui(()->{
    InputMethod.AccessibilityInputConnection live=connection(source,generation);SearchInputRules.Sample now=nodeId>0?readNode(source,nodeId,generation):SearchInputBridge.read(live);
    if(!expected.same(now))throw new ControlService.UiChangedException();
    validateQuery(now.text);synchronized(owner){owner.guard(generation);InputMethod.AccessibilityInputConnection current=connection(source,generation);dispatching.run();current.performEditorAction(EditorInfo.IME_ACTION_SEARCH);}return true;
   });}
  };
  try{return SearchSubmitRules.submit(port,pending,submissions,source.searchEditor,owner.runId,this::validateQuery);}
  catch(SearchSubmitRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
 }
 private void validateQuery(String text)throws Exception{
  if(!SearchQueryPolicy.allowed(owner.task,owner.activeRequest==null?"":owner.activeRequest.original,owner.activeRequest==null?"":owner.activeRequest.target,owner.allowed,text,new AppAccess(owner).independentSearchFields(owner.allowed)))
   throw new ControlService.LocalBoundaryException("搜索词尚未核对，未提交；请保留原目标对象");
 }
 String write(ControlService.Observation source,String text,long generation)throws Exception{
  if(!SearchQueryPolicy.allowed(owner.task,owner.activeRequest==null?"":owner.activeRequest.original,owner.activeRequest==null?"":owner.activeRequest.target,owner.allowed,text,new AppAccess(owner).independentSearchFields(owner.allowed)))
   throw new ControlService.LocalBoundaryException("搜索词尚未核对，未写入；请保留原目标对象");
  SearchInputRules.Port nativePort=new SearchInputRules.Port(){
   public SearchInputRules.Sample read()throws Exception{return owner.ui(()->SearchInputBridge.read(connection(source,generation)));}
   public void selectAll()throws Exception{owner.ui(()->{connection(source,generation).performContextMenuAction(android.R.id.selectAll);return true;});}
   public void commit(SearchInputRules.Sample expected,String value,Runnable dispatching)throws Exception{
    owner.ui(()->{
     InputMethod.AccessibilityInputConnection connection=connection(source,generation);SearchInputRules.Sample now=SearchInputBridge.read(connection);
     if(!expected.same(now))throw new ControlService.UiChangedException();
     // A dispatch that cannot be read back remains pending; never automatically replay it.
     dispatching.run();connection.commitText(value,1,null);return true;
    });
   }
   public void pause(int milliseconds)throws Exception{Thread.sleep(milliseconds);owner.check(generation);}
  };
  SearchInputRules.Result result;
  try{result=SearchInputRules.replace(nativePort,pending,source.searchEditor,owner.runId,text);}
  catch(SearchInputRules.Rejected rejected){throw new ControlService.LocalBoundaryException(rejected.getMessage());}
  if(result==SearchInputRules.Result.ALREADY_PRESENT)return "系统搜索框已是本次关键词，未重复输入；搜索结果仍须核对";
  if(result==SearchInputRules.Result.VERIFIED)return "关键词已由手机系统输入连接写入并精确读回；输入未提交，请重新观察后调用device.submit_search（系统搜索框可省略id），再核对结果页";
  return "关键词已派发但尚未读回核验，禁止再次type或盲目提交；先核对新画面，不把输入回执当搜索完成";
 }
}
