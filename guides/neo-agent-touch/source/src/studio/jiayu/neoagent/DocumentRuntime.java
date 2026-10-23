package studio.jiayu.neoagent;
import android.os.*;import android.graphics.Rect;import android.view.accessibility.*;import android.view.inputmethod.*;import org.json.*;
/** Bounded native editor adapter: only the file created by this run can be replaced. */
final class DocumentRuntime {
 final DocumentReopen reopen=new DocumentReopen();final ControlService owner;boolean created,permitted,attempted,bodyVerified,saved,reopened,editVerified,persistedEdit;boolean createArmed,pendingReopen;int opens,writes;String lastWrite="",name="",candidateName="",evidence="";long saveAt,tapAt;int returnBacks;int tapY,tapWindow;boolean recoveryNeedsFile;String readbackState="unwritten";boolean nextArm,nextBlank,nextReopen,nextSave;String nextName="";String nameToken=java.util.UUID.randomUUID().toString().replace("-","");boolean nameConflict;
 DocumentRuntime(ControlService s){owner=s;}
 void reset(){nameToken=java.util.UUID.randomUUID().toString().replace("-","");nameConflict=false;readbackState="unwritten";recoveryNeedsFile=false;reopen.reset();created=permitted=attempted=bodyVerified=saved=reopened=editVerified=persistedEdit=false;opens=writes=0;createArmed=pendingReopen=false;lastWrite=name=candidateName=evidence="";saveAt=tapAt=0;returnBacks=0;}

 JSONObject checkpoint()throws Exception{return new JSONObject().put("created",created).put("attempted",attempted).put("body",lastWrite).put("name",name).put("candidate",candidateName).put("writes",writes).put("opens",opens).put("name_token",nameToken).put("name_conflict",nameConflict);}
 void restore(JSONObject o)throws Exception{reset();created=o.getBoolean("created");attempted=o.getBoolean("attempted");lastWrite=o.getString("body");name=o.getString("name");candidateName=o.optString("candidate");writes=o.getInt("writes");opens=o.getInt("opens");String restoredToken=o.optString("name_token",nameToken);if(!restoredToken.matches("[a-zA-Z0-9]{8,40}"))throw new Exception("Invalid document name token");nameToken=restoredToken;nameConflict=o.optBoolean("name_conflict");if(lastWrite.length()>8000||writes<0||opens<0)throw new Exception("Invalid document checkpoint");permitted=false;reopen.wrote(writes);recoveryNeedsFile=created&&!name.isEmpty();saveAt=recoveryNeedsFile?System.currentTimeMillis():0;}
 String local(AccessibilityNodeInfo n,int depth){if(n==null||depth>30||!n.isVisibleToUser())return "";StringBuilder b=new StringBuilder();if(!n.isPassword())b.append(n.getText()).append(' ').append(n.getContentDescription()).append('\n');for(int i=0;i<n.getChildCount();i++)b.append(local(n.getChild(i),depth+1));return b.toString();}
 String expectedName(){return DocumentNameRules.expected(originalRequest(),nameToken,nameConflict);}
 void noteNameConflict(String context){if(!DocumentNameRules.conflict(context))return;if(!nameConflict||!name.isEmpty())nameToken=java.util.UUID.randomUUID().toString().replace("-","");nameConflict=true;name=candidateName="";saved=reopened=editVerified=persistedEdit=pendingReopen=recoveryNeedsFile=false;saveAt=0;returnBacks=0;reopen.wrote(writes);reopen.invalidateRead();}
 void beforeTap(ControlService.Observation source,int x,int y)throws Exception{
  nextArm=nextBlank=nextReopen=nextSave=false;nextName="";if(!DocumentScope.applies(owner.allowed))return;
  String context=local(owner.getRootInActiveWindow(),0);noteNameConflict(context);
  AccessibilityNodeInfo smallest=null;long area=Long.MAX_VALUE;for(AccessibilityNodeInfo n:source.targets.values()){Rect box=new Rect();n.getBoundsInScreen(box);long size=(long)box.width()*box.height();if(box.contains(x,y)&&size>0&&size<area){area=size;smallest=n;}}
  nextReopen=reopen.listed&&!name.isEmpty()&&ownFileAt(owner.getRootInActiveWindow(),x,y,0);if(smallest==null){if(DocumentNameRules.conflict(context))throw new ControlService.LocalBoundaryException("同名文件不能覆盖；请取消后用本轮独立文件名保存");return;}
  String t=smallest.getText()==null?"":smallest.getText().toString(),d=smallest.getContentDescription()==null?"":smallest.getContentDescription().toString(),grounded=NativeLabels.label(smallest);
  if(DocumentNameRules.overwriteAction(t,context)||DocumentNameRules.overwriteAction(d,context)||DocumentNameRules.overwriteAction(grounded,context))throw new ControlService.LocalBoundaryException("同名文件不能覆盖；请取消后按expected_file_name保存，不打开同名旧文件");
  if(!created&&!DocumentScope.entry(grounded,"").isEmpty()&&grounded.matches("(?s).*(新建|创建|文字|Word|空白).*"))nextArm=true;
  nextBlank=DocumentScope.blank(t,d)||DocumentScope.blank(grounded,"");if(nextBlank&&created)throw new ControlService.LocalBoundaryException("本任务文档已经创建，请核对原文件，不能重复新建");
  nextReopen=nextReopen||reopen.listed&&!name.isEmpty()&&(DocumentScope.sameFile(t,name)||DocumentScope.sameFile(grounded,name)||ownFileAt(owner.getRootInActiveWindow(),x,y,0));
  if((t.equals("保存")||d.equals("保存")||grounded.equals("保存")||bodyVerified&&!name.isEmpty()&&grounded.equals("完成"))&&(context.contains("保存路径")||!candidateName.isEmpty())){
   if(!created)throw new ControlService.LocalBoundaryException("只能保存本任务新建的文档");
   boolean nameFieldSeen=false;for(AccessibilityNodeInfo n:source.targets.values())if(n.isEditable()&&n.isVisibleToUser()&&n.getText()!=null){nameFieldSeen=true;String v=n.getText().toString().trim();if(DocumentNameRules.matches(v,expectedName())){nextSave=true;nextName=DocumentNameRules.base(v);break;}}
   if(!nextSave&&!nameFieldSeen&&opens>0&&DocumentNameRules.matches(name,expectedName())){nextSave=true;nextName=DocumentNameRules.base(name);}
   if(!nextSave)throw new ControlService.LocalBoundaryException("请按document_tools.expected_file_name命名本任务文档，不覆盖旧文件");
  }
 }
 void afterTap(int x,int y){if(!DocumentScope.applies(owner.allowed))return;if(nextArm)createArmed=true;if(nextBlank){created=permitted=true;}if(nextReopen&&reopen.openRequested()){permitted=true;pendingReopen=true;}if(nextSave){name=nextName;saved=false;saveAt=System.currentTimeMillis();returnBacks=0;}tapAt=System.currentTimeMillis();tapY=y;AccessibilityNodeInfo r=owner.getRootInActiveWindow();tapWindow=r==null?-1:r.getWindowId();nextBlank=nextReopen=nextSave=false;}

 void confirmNewEditor(){if(created||!createArmed||Build.VERSION.SDK_INT<33)return;android.accessibilityservice.InputMethod method=owner.getInputMethod();if(method==null||!method.getCurrentInputStarted()||method.getCurrentInputConnection()==null||method.getCurrentInputEditorInfo()==null||!owner.allowed.equals(method.getCurrentInputEditorInfo().packageName))return;AccessibilityNodeInfo root=owner.getRootInActiveWindow();if(root==null||!owner.allowed.contentEquals(root.getPackageName()))return;String context=local(root,0);if(!context.contains("writer frame"))return;SurroundingText around=method.getCurrentInputConnection().getSurroundingText(DocumentReadback.PROBE,DocumentReadback.PROBE,0);if(around!=null&&around.getOffset()==0&&around.getText()!=null&&around.getText().toString().trim().isEmpty()){created=permitted=true;createArmed=false;}}
 boolean leaveUnownedEditor(long g)throws Exception{if(created||createArmed)return false;AccessibilityNodeInfo root=owner.getRootInActiveWindow();if(root==null||!owner.allowed.contentEquals(root.getPackageName())||!local(root,0).contains("writer frame"))return false;owner.check(g);return owner.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);}

 static boolean nativeControl(String label){return label!=null&&label.trim().matches("^(编辑|编辑文档|保存|完成|返回|最近|首页|空白文档|新建|文字|确认保存|另存为|键盘|显示键盘|软键盘)$");}
 void filenameTyped(String text,AccessibilityNodeInfo node){if(!DocumentScope.applies(owner.allowed)||!created)return;String context=local(owner.getRootInActiveWindow(),0);noteNameConflict(context);if(!DocumentNameRules.matches(text,expectedName()))return;Rect b=new Rect();node.getBoundsInScreen(b);if(b.height()<owner.screenMetrics().heightPixels*.15&&context.contains("保存"))candidateName=DocumentNameRules.base(text);}
 boolean containsFile(AccessibilityNodeInfo n,int depth){if(n==null||depth>5)return false;if(DocumentScope.sameFile(NativeLabels.label(n),name))return true;for(int i=0;i<n.getChildCount();i++)if(containsFile(n.getChild(i),depth+1))return true;return false;}
 boolean ownFileAt(AccessibilityNodeInfo n,int x,int y,int depth){if(n==null||depth>30||!n.isVisibleToUser())return false;Rect b=new Rect();n.getBoundsInScreen(b);if(!b.contains(x,y))return false;if(b.height()<owner.screenMetrics().heightPixels*.18&&containsFile(n,0))return true;for(int i=0;i<n.getChildCount();i++)if(ownFileAt(n.getChild(i),x,y,depth+1))return true;return false;}
 boolean ready(){if(Build.VERSION.SDK_INT<33||!DocumentScope.applies(owner.allowed)||!created||!permitted||recoveryNeedsFile)return false;AccessibilityNodeInfo root=owner.getRootInActiveWindow();if(root==null||!owner.allowed.contentEquals(root.getPackageName()))return false;android.accessibilityservice.InputMethod method=owner.getInputMethod();if(method==null||!method.getCurrentInputStarted()||method.getCurrentInputConnection()==null)return false;EditorInfo info=method.getCurrentInputEditorInfo();if(info==null||!owner.allowed.equals(info.packageName))return false;int variation=info.inputType&android.text.InputType.TYPE_MASK_VARIATION,type=info.inputType&android.text.InputType.TYPE_MASK_CLASS;if(type==1&&(variation==128||variation==144||variation==224)||type==2&&variation==16)return false;String editorContext=local(root,0);if(DocumentInputRules.bodyProxy(info.inputType,info.imeOptions,editorContext.contains("writer frame"),editorContext.contains("保存路径")||editorContext.contains("文件名称")))return true;Rect app=new Rect();root.getBoundsInScreen(app);AccessibilityNodeInfo focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(focused!=null&&focused.isEditable()){Rect box=new Rect();focused.getBoundsInScreen(box);if(box.height()>app.height()*.18&&box.top>app.top+app.height()*.10)return true;if(focused.isVisibleToUser()&&box.height()>owner.dp(8)&&box.width()>app.width()*.2&&box.height()<app.height()*.12)return false;}return tapWindow==root.getWindowId()&&System.currentTimeMillis()-tapAt<30000&&tapY>app.top+app.height()*.18&&tapY<app.bottom-app.height()*.12;}
 JSONObject proof(){try{return new JSONObject().put("create_intent_armed",createArmed).put("created_this_task",created).put("expected_file_name",expectedName()).put("name_conflict",nameConflict).put("body_verified",bodyVerified).put("readback_state",readbackState).put("save_verified",saved).put("reopened",reopened).put("edit_verified",editVerified).put("writes",writes).put("persisted_edit_verified",persistedEdit).put("opens",opens).put("written_revision",reopen.written).put("read_revision",reopen.read).put("file_list_observed",reopen.listed).put("requested_clause_verified",requestedClauseVerified());}catch(Exception ignored){return new JSONObject();}}
 void acceptReadback(SurroundingText around){
  String text=around==null||around.getText()==null?null:around.getText().toString();
  readbackState=DocumentReadback.classify(lastWrite,text,around==null?-1:around.getOffset(),originalRequest());
  if(!readbackState.equals("verified")){
   bodyVerified=false;evidence="";
   if(readbackState.equals("mismatch")||readbackState.equals("content_incomplete")){saved=reopened=editVerified=persistedEdit=false;reopen.invalidateRead();}
   return;
  }
  evidence=text;bodyVerified=true;
  if(pendingReopen&&reopen.pending){opens++;reopened=true;reopen.bodyRead();pendingReopen=false;}
  if(writes>=2&&reopened){editVerified=true;if(reopen.currentRead())persistedEdit=true;}
 }
 JSONObject status()throws Exception{return owner.ui(()->{
  confirmNewEditor();AccessibilityNodeInfo root=owner.getRootInActiveWindow();String context=local(root,0);noteNameConflict(context);
  if(name.isEmpty()&&!candidateName.isEmpty()&&DocumentScope.saved(context))name=candidateName;
  boolean home=fileListVisible(root);if(home){recoveryNeedsFile=false;reopen.listObserved();}
  if(home&&!pendingReopen)permitted=false;if(pendingReopen&&context.contains("writer frame"))permitted=true;
  boolean r=ready();if(r)acceptReadback(owner.getInputMethod().getCurrentInputConnection().getSurroundingText(DocumentReadback.PROBE,DocumentReadback.PROBE,0));
  if(bodyVerified&&!name.isEmpty()&&(DocumentScope.saved(context)||home&&context.contains(name)&&!context.contains("保存路径")))saved=true;
  return proof().put("input_connection_ready",r).put("write_attempted",attempted).put("file_name",name).put("next_capability",next(r,context));
 });}

 String next(boolean r,String context){if(!created)return context.contains("writer frame")?"当前不是本任务编辑器，back返回首页再新建":"从新建入口选择空白文字文档，不打开已有文件";if(pendingReopen&&!r)return "已打开本任务文件。先点击编辑，再使用document_focus聚焦正文，然后document_check读回。不要再返回或新建。";if(DocumentNameRules.conflict(context))return "同名文件不能覆盖。取消该弹窗，按expected_file_name改为本轮独立文件名；不打开同名旧文件，不重复写正文。";if(readbackState.equals("content_incomplete"))return "正文已完整一致读回，但不符合原请求的合同结构，不能宣告完成；不重复插入或覆盖正文。";if(readbackState.equals("mismatch"))return "本次正文与预期不一致，旧核验已失效。不要覆盖或重复插入；先从最近列表重开本任务文件，再完整读回核对。";if(!r&&reopen.listed&&!name.isEmpty())return "从最近列表打开本任务文件，再聚焦正文读回；不要新建或覆盖其他文件。";if((readbackState.equals("unavailable")||readbackState.equals("partial"))&&attempted)return "正文暂时不能完整读回，先聚焦正文再document_check；不重复写入，不宣告完成。";if(!bodyVerified)return r?"按原用户目标自行生成完整正文并调用document_write(text)，仅原目标要求合同时生成合同；若已经尝试写入，只document_check，不重复插入":"点编辑，再document_focus聚焦正文，用document_check核对输入连接";if(!saved)return saveAt==0?"正文已写入或修改，请点击保存/完成，核对文件名并等待存储结果":"已执行保存。请back返回最近文档，核对本任务文件名，不要重复新建或写入";if(!reopen.currentRead())return "在最近列表打开本任务文件，点编辑，然后document_focus、document_check读回持久化正文";if(owner.task.contains("修改")&&!editVerified)return "依据last_verified_document_body用document_replace(find,replacement)仅替换用户目标指定的条款，其他段落不改，再保存、重开核对";return completed()?"正文、保存和重开修改已核对，可以finish":"请保存修改，返回最近列表重开并document_check核对";}
 boolean navigateSavedFile(long g)throws Exception{if(!recoveryNeedsFile&&(!created||!bodyVerified||name.isEmpty()||saveAt==0||System.currentTimeMillis()-saveAt<1500||pendingReopen||reopen.currentRead()||returnBacks>=4))return false;if(recoveryNeedsFile&&returnBacks>=6)throw new ControlService.LocalBoundaryException("恢复时仍未找到最近文件列表，未改写正文");AccessibilityNodeInfo root=owner.getRootInActiveWindow();if(root==null||!owner.allowed.contentEquals(root.getPackageName()))return false;String context=local(root,0);if(fileListVisible(root)||context.matches("(?s).*(正在保存|保存中|同步中|正在上传).*"))return false;owner.check(g);returnBacks++;return owner.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);}
 boolean exposedControl(String label){return nativeControl(label)||DocumentScope.sameFile(label,name)||"writer frame".equalsIgnoreCase(label.trim());}
 String focus(long g,ControlService.Observation source)throws Exception{if(!created||!permitted)throw new ControlService.LocalBoundaryException("仅聚焦本任务文档，请先打开本任务文件");boolean inputReady=owner.ui(()->{owner.guard(g);return ready();});if(inputReady){status();return "正文输入连接已就绪，已重新核对正文";}int keyboard=-1,edit=-1,frame=-1;for(java.util.Map.Entry<Integer,AccessibilityNodeInfo> e:source.targets.entrySet()){String label=NativeLabels.label(e.getValue()).trim();if(label.matches("^(键盘|显示键盘|软键盘)$"))keyboard=e.getKey();else if(label.matches("^(编辑|编辑文档)$"))edit=e.getKey();else if("writer frame".equalsIgnoreCase(label))frame=e.getKey();}int id=keyboard>0?keyboard:edit>0?edit:frame;if(id<0)throw new ControlService.LocalBoundaryException("请先进入编辑或打开WPS键盘，等待本任务正文输入区出现");return owner.execute(new Action(new JSONObject().put("action","tap").put("id",id)),g,source);}

 boolean fileListVisible(AccessibilityNodeInfo root){if(root==null)return false;for(String marker:new String[]{"最近","最近文档"})for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(marker)){Rect b=new Rect();n.getBoundsInScreen(b);if(n.isVisibleToUser()&&!b.isEmpty()&&marker.equals(n.getText()==null?"":n.getText().toString().trim()))return true;}return false;}
 String originalRequest(){return owner.activeRequest==null?owner.task:owner.activeRequest.original;}
 boolean requestedClauseVerified(){return DocumentEditContract.targetSatisfied(originalRequest(),lastWrite);}
 void validateReplacement(String next)throws ControlService.LocalBoundaryException{if(!DocumentEditContract.permits(originalRequest(),lastWrite,next))throw new ControlService.LocalBoundaryException("这次修改超出原目标。只更改指定的支付日期并保持其他内容逐字不变；请基于上次完整正文重新选择唯一片段。");}

 boolean verifyCompletion()throws Exception{return owner.ui(()->{JSONObject current=status();if(!current.optBoolean("input_connection_ready")){acceptReadback(null);return false;}return completed();});}
 boolean completed(){return created&&bodyVerified&&readbackState.equals("verified")&&saved&&reopened&&reopen.currentRead()&&requestedClauseVerified()&&(!owner.task.contains("修改")||editVerified&&persistedEdit);}
 String replace(String find,String replacement,long g)throws Exception{if(!bodyVerified||!reopened||!reopen.currentRead())throw new ControlService.LocalBoundaryException("先重新打开本任务文件并完整读回，再修改");String next;try{next=DocumentText.replaceOnce(lastWrite,find,replacement);}catch(Exception invalid){throw new ControlService.LocalBoundaryException("原文片段不是唯一匹配，未改写，请核对上次完整正文");}return write(next,g);}
 String write(String text,long g)throws Exception {
  owner.check(g);
  if(!DocumentScope.applies(owner.allowed)||!created||!permitted)throw new ControlService.LocalBoundaryException("正文工具仅用于本任务新建的WPS文档");
  if(text.isEmpty()||text.length()>8000||PrivateText.credential(text))throw new ControlService.LocalBoundaryException("正文长度或内容不符合要求");
  if(attempted&&!bodyVerified)throw new ControlService.LocalBoundaryException("已尝试写入，先完整读回核对，不重复插入");
  if(text.equals(lastWrite))return "正文已尝试写入，请读回与保存，不重复插入";
  if(writes>0&&(!reopened||!reopen.currentRead()))throw new ControlService.LocalBoundaryException("修改前先保存并重新打开本任务文件");
  boolean replacing=writes>0;if(replacing)validateReplacement(text);
  owner.ui(()->{owner.guard(g);if(!ready())throw new ControlService.LocalBoundaryException("正文尚未聚焦，请使用document_focus");
   android.accessibilityservice.InputMethod.AccessibilityInputConnection connection=owner.getInputMethod().getCurrentInputConnection();
   SurroundingText before=connection.getSurroundingText(DocumentReadback.PROBE,DocumentReadback.PROBE,0);
   if(before==null||before.getText()==null||before.getOffset()!=0)throw new ControlService.LocalBoundaryException("正文无法完整读回，未改写");
   String existing=before.getText().toString();
   if(replacing){if(!DocumentText.same(existing,lastWrite))throw new ControlService.LocalBoundaryException("当前正文与上次完整读回不一致，未改写");connection.performContextMenuAction(android.R.id.selectAll);}
   else if(!DocumentText.normalized(existing).isEmpty())throw new ControlService.LocalBoundaryException("初始正文并非空白，未改写");
   return true;
  });
  if(replacing){boolean selected=false;
   for(int attempt=0;attempt<8;attempt++){
    Thread.sleep(100);owner.check(g);
    selected=owner.ui(()->{owner.guard(g);if(!ready())return false;SurroundingText around=owner.getInputMethod().getCurrentInputConnection().getSurroundingText(DocumentReadback.PROBE,DocumentReadback.PROBE,0);return around!=null&&around.getText()!=null&&DocumentText.wholeSelection(around.getText().toString(),lastWrite,around.getOffset(),around.getSelectionStart(),around.getSelectionEnd());});
    if(selected)break;
    if(attempt==3)owner.ui(()->{owner.guard(g);SurroundingText around=owner.getInputMethod().getCurrentInputConnection().getSurroundingText(DocumentReadback.PROBE,DocumentReadback.PROBE,0);if(around!=null&&around.getText()!=null&&DocumentText.same(around.getText().toString(),lastWrite))owner.getInputMethod().getCurrentInputConnection().setSelection(0,around.getText().length());return true;});
   }
   if(!selected)throw new ControlService.LocalBoundaryException("尚未确认全文选中，未执行替换，请重新聚焦正文后核对");
  }
  owner.ui(()->{owner.guard(g);if(!ready())throw new ControlService.LocalBoundaryException("正文焦点已变化，未改写");
   android.accessibilityservice.InputMethod.AccessibilityInputConnection connection=owner.getInputMethod().getCurrentInputConnection();SurroundingText before=connection.getSurroundingText(DocumentReadback.PROBE,DocumentReadback.PROBE,0);
   if(before==null||before.getText()==null||before.getOffset()!=0)throw new ControlService.LocalBoundaryException("正文读回已变化，未改写");
   if(replacing&&!DocumentText.wholeSelection(before.getText().toString(),lastWrite,before.getOffset(),before.getSelectionStart(),before.getSelectionEnd()))throw new ControlService.LocalBoundaryException("全文选区已变化，未改写");
   if(!replacing&&!DocumentText.normalized(before.getText().toString()).isEmpty())throw new ControlService.LocalBoundaryException("初始正文已变化，未改写");
   attempted=true;readbackState="pending";bodyVerified=false;saved=false;saveAt=0;returnBacks=0;persistedEdit=false;lastWrite=text;writes++;reopen.wrote(writes);owner.checkpointBeforeMutation();connection.commitText(text,1,null);return true;
  });
  Thread.sleep(500);owner.check(g);JSONObject proof=status();
  return proof.optBoolean("body_verified")?"正文已通过手机原生全文一致性核验，请保存":"正文已发送，尚未完整一致读回，请document_check，不重复插入";
 }
}
