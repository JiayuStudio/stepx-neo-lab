package studio.jiayu.neoagent;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.util.Locale;

/**
 * Same-process Agent text port. Never learns, persists, displays, or sends editor contents.
 * Platform references: Android InputMethodService/InputConnection docs and AOSP SoftKeyboard
 * (Apache-2.0, inspected only; no keyboard code or dependency copied).
 */
public final class AgentInputMethodService extends InputMethodService {
 static volatile AgentInputMethodService instance;
 static final int MAX_TEXT=6000;
 private final Handler main=new Handler(Looper.getMainLooper());
 private EditorInfo editor;private InputConnection connection;private long editorEpoch;
 private TextView heading,detail;private boolean destroyed;private Runnable focusLoss;
 private ControlService boundOwner;private long boundGeneration;private String boundRun="";

 static final class EditorSnapshot {
  final long epoch;final String packageName;final int inputType,imeAction,imeOptions;final boolean connectionReady;
  EditorSnapshot(long epoch,String pkg,int inputType,int options,boolean ready){this.epoch=epoch;packageName=pkg;this.inputType=inputType;imeOptions=options;imeAction=options&EditorInfo.IME_MASK_ACTION;connectionReady=ready;}
 }
 @Override public void onCreate(){super.onCreate();instance=this;main.post(()->AgentImeLease.recoverAsync(this));}
 @Override public boolean onEvaluateFullscreenMode(){return false;}
 @Override public View onCreateInputView(){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(16),dp(18),dp(14));panel.setBackgroundColor(Color.rgb(35,34,37));
  LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
  TextView mark=new TextView(this);mark.setText("✦");mark.setTextColor(Color.rgb(255,159,111));mark.setTextSize(25);titleRow.addView(mark,new LinearLayout.LayoutParams(dp(34),dp(38)));
  heading=text("Agent 输入通道",17,Color.rgb(244,239,235));heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);titleRow.addView(heading,new LinearLayout.LayoutParams(0,dp(38),1));panel.addView(titleRow);
  detail=text("由当前任务提供文字，随时可以接管。",12,Color.rgb(184,175,177));detail.setMinHeight(dp(42));detail.setGravity(Gravity.CENTER_VERTICAL);panel.addView(detail);
  LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);Button takeover=action("接管并切回",true);takeover.setOnClickListener(v->manualRestore(true));Button restore=action("切回原输入法",false);restore.setOnClickListener(v->manualRestore(true));
  LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,dp(46),1);first.setMarginEnd(dp(9));actions.addView(takeover,first);actions.addView(restore,new LinearLayout.LayoutParams(0,dp(46),1));panel.addView(actions);
  Button picker=action("选择其他输入法",false);picker.setBackgroundColor(Color.TRANSPARENT);picker.setTextColor(Color.rgb(191,184,183));picker.setOnClickListener(v->{pauseOwnedRun();showPicker();});LinearLayout.LayoutParams pick=new LinearLayout.LayoutParams(-1,dp(42));pick.topMargin=dp(5);panel.addView(picker,pick);
  TextView local=text("本机输入 · 不记录内容",10,Color.rgb(133,126,129));local.setGravity(Gravity.CENTER);panel.addView(local,new LinearLayout.LayoutParams(-1,dp(20)));updatePanel();return panel;
 }
 private TextView text(String value,int size,int color){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
 private Button action(String value,boolean primary){Button button=new Button(this);button.setText(value);button.setTextSize(12);button.setAllCaps(false);button.setPadding(dp(8),0,dp(8),0);button.setTextColor(primary?Color.rgb(43,24,16):Color.rgb(235,225,218));GradientDrawable fill=new GradientDrawable();fill.setColor(primary?Color.rgb(255,163,116):Color.rgb(62,51,48));fill.setCornerRadius(dp(13));button.setBackground(fill);return button;}
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override public void onStartInput(EditorInfo attribute,boolean restarting){super.onStartInput(attribute,restarting);cancelFocusLoss();editorEpoch++;editor=attribute;connection=getCurrentInputConnection();bindCurrentOwner();updatePanel();if(privateEditor(attribute)||!allowedCurrentEditor())restoreAfterFocusLoss();}
 @Override public void onStartInputView(EditorInfo attribute,boolean restarting){super.onStartInputView(attribute,restarting);cancelFocusLoss();EditorInfo current=getCurrentInputEditorInfo();InputConnection input=getCurrentInputConnection();if(editor!=current||connection!=input){editorEpoch++;editor=current;connection=input;}bindCurrentOwner();updatePanel();}
 @Override public void onFinishInput(){editorEpoch++;editor=null;connection=null;super.onFinishInput();scheduleFocusLoss();updatePanel();}
 @Override public void onFinishInputView(boolean finishingInput){super.onFinishInputView(finishingInput);if(finishingInput)scheduleFocusLoss();}
 @Override public void onUnbindInput(){editorEpoch++;editor=null;connection=null;super.onUnbindInput();scheduleFocusLoss();}
 @Override public void onDestroy(){destroyed=true;cancelFocusLoss();ControlService owner=boundOwner;long g=boundGeneration;String run=boundRun;if(owner!=null&&owner==ControlService.instance&&run.equals(owner.runId)&&owner.generation==g&&owner.agentImeLease.held(g))owner.agentImeLease.releaseAsync();editorEpoch++;editor=null;connection=null;if(instance==this)instance=null;AgentImeLease.recoverAsync(this);super.onDestroy();}
 private void cancelFocusLoss(){if(focusLoss!=null){main.removeCallbacks(focusLoss);focusLoss=null;}}
 private void scheduleFocusLoss(){cancelFocusLoss();final long ended=editorEpoch;final ControlService owner=boundOwner;final long g=boundGeneration;final String run=boundRun;focusLoss=()->{focusLoss=null;if(editorEpoch!=ended||editor!=null)return;if(owner!=null&&owner==ControlService.instance&&run.equals(owner.runId)&&owner.generation==g&&owner.agentImeLease.held(g))owner.agentImeLease.releaseAsync();else AgentImeLease.recoverAsync(this);};main.postDelayed(focusLoss,150);}
 private void bindCurrentOwner(){ControlService owner=ControlService.instance;if(owner!=null&&owner.agentImeLease.held(owner.generation)){boundOwner=owner;boundGeneration=owner.generation;boundRun=owner.runId;}}
 private boolean allowedCurrentEditor(){ControlService owner=ControlService.instance;return owner!=null&&owner.running&&!owner.paused&&editor!=null&&owner.allowed.equals(editor.packageName);}
 private void restoreAfterFocusLoss(){ControlService owner=ControlService.instance;if(owner!=null&&owner.agentImeLease.held(owner.generation))owner.agentImeLease.releaseAsync();else AgentImeLease.recoverAsync(this);}
 private void updatePanel(){if(heading==null)return;ControlService owner=ControlService.instance;boolean active=owner!=null&&owner.running&&!owner.paused&&owner.agentImeLease.held(owner.generation)&&allowedCurrentEditor()&&!privateEditor(editor);heading.setText(active?"Agent 正在输入":"Agent 输入通道");detail.setText(privateEditor(editor)?"此输入框需要你手动输入，请切回常用输入法。":active?"当前任务的输入框已连接。接管后切回原键盘。":"等待当前任务连接；你也可以切回常用输入法。");}
 private void pauseOwnedRun(){ControlService owner=ControlService.instance;if(owner!=null&&owner.agentImeLease.held(owner.generation)){owner.pauseForUser();owner.agentImeLease.releaseAsync();}}
 private void manualRestore(boolean takeover){if(takeover)pauseOwnedRun();boolean restored=false;ControlService owner=ControlService.instance;try{restored=owner!=null&&owner.agentImeLease.releaseUi();if(!restored)restored=AgentImeLease.recoverUi(this);}catch(RuntimeException ignored){}if(!restored)showPicker();updatePanel();}
 private void showPicker(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();}catch(RuntimeException ignored){if(detail!=null)detail.setText("请通过系统输入法切换入口选择常用键盘。");}}

 private static boolean privateEditor(EditorInfo value){if(value==null)return false;int type=value.inputType,kind=type&InputType.TYPE_MASK_CLASS,variation=type&InputType.TYPE_MASK_VARIATION;if(kind==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))return true;if(kind==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD)return true;String options=value.privateImeOptions==null?"":value.privateImeOptions.toLowerCase(Locale.ROOT);return options.contains("password")||options.contains("incognito")||options.contains("sensitive")||options.contains("private=true");}
 private void mainThread()throws IOException{if(Looper.myLooper()!=Looper.getMainLooper())throw new IOException("agent_ime_ui_thread_required");}
 private void authorize(ControlService owner,long g)throws Exception{mainThread();if(destroyed||instance!=this||owner==null||owner!=ControlService.instance||!owner.connected||!owner.running||owner.paused||owner.runConfig==null||!owner.agentImeLease.held(g))throw new IOException("agent_ime_lease_unavailable");owner.check(g);owner.requireDataGrant(owner.runConfig,owner.allowed);}
 private InputConnection guard(ControlService owner,long g,long expectedEpoch)throws Exception{authorize(owner,g);EditorInfo current=getCurrentInputEditorInfo();InputConnection input=getCurrentInputConnection();if(expectedEpoch!=editorEpoch||editor==null||current!=editor||connection==null||input!=connection)throw new IOException("agent_ime_editor_changed");if(!owner.allowed.equals(editor.packageName)||privateEditor(editor)||(editor.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_NULL)throw new IOException("agent_ime_editor_not_allowed");boundOwner=owner;boundGeneration=g;boundRun=owner.runId;return input;}
 EditorSnapshot snapshot(ControlService owner,long g)throws Exception{authorize(owner,g);EditorInfo current=getCurrentInputEditorInfo();if(current==null||current!=editor)return new EditorSnapshot(editorEpoch,"",0,0,false);if(!owner.allowed.equals(current.packageName)||privateEditor(current)||(current.inputType&InputType.TYPE_MASK_CLASS)==InputType.TYPE_NULL)throw new IOException("agent_ime_editor_not_allowed");InputConnection input=getCurrentInputConnection();boolean ready=input!=null&&input==connection;if(ready){boundOwner=owner;boundGeneration=g;boundRun=owner.runId;}return new EditorSnapshot(editorEpoch,current.packageName,current.inputType,current.imeOptions,ready);}
 /** Transient read only. Callers must not log/persist the returned editor contents. */
 String readText(ControlService owner,long g,long expectedEpoch,int maxChars)throws Exception{if(maxChars<1||maxChars>MAX_TEXT)throw new IOException("agent_ime_read_limit");InputConnection input=guard(owner,g,expectedEpoch);try{ExtractedTextRequest request=new ExtractedTextRequest();request.hintMaxChars=maxChars+1;request.hintMaxLines=200;ExtractedText value=input.getExtractedText(request,0);guard(owner,g,expectedEpoch);if(value==null||value.text==null||value.startOffset!=0||value.partialStartOffset>=0||value.text.length()>maxChars){releaseRejectedPort(owner,g);throw new IOException("agent_ime_complete_read_unavailable");}return value.text.toString();}catch(RuntimeException failure){connectionFailure(owner,g);throw new IOException("agent_ime_connection_failure");}}
 boolean replaceText(ControlService owner,long g,long expectedEpoch,String text)throws Exception{return replaceText(owner,g,expectedEpoch,text,null);}
 boolean replaceText(ControlService owner,long g,long expectedEpoch,String text,Runnable dispatching)throws Exception{validateText(text);String before=readText(owner,g,expectedEpoch,MAX_TEXT);InputConnection input=guard(owner,g,expectedEpoch);boolean batch=false;try{guard(owner,g,expectedEpoch);batch=input.beginBatchEdit();if(!input.finishComposingText())throw new IOException("agent_ime_composition_unconfirmed");guard(owner,g,expectedEpoch);if(!input.setSelection(0,before.length()))throw new IOException("agent_ime_selection_unconfirmed");guard(owner,g,expectedEpoch);if(dispatching!=null)dispatching.run();boolean accepted=input.commitText(text,1);guard(owner,g,expectedEpoch);if(!accepted)throw new IOException("agent_ime_commit_unconfirmed");return true;}catch(Exception failure){connectionFailure(owner,g);if(failure instanceof IOException)throw failure;throw new IOException("agent_ime_connection_failure");}finally{if(batch)try{input.endBatchEdit();}catch(RuntimeException ignored){}}}
 boolean commitText(ControlService owner,long g,long expectedEpoch,String text)throws Exception{validateText(text);InputConnection input=guard(owner,g,expectedEpoch);try{boolean accepted=input.commitText(text,1);guard(owner,g,expectedEpoch);if(!accepted)throw new IOException("agent_ime_commit_unconfirmed");return true;}catch(Exception failure){connectionFailure(owner,g);if(failure instanceof IOException)throw failure;throw new IOException("agent_ime_connection_failure");}}
 boolean submitSearch(ControlService owner,long g,long expectedEpoch)throws Exception{return submitSearch(owner,g,expectedEpoch,null);}
 boolean submitSearch(ControlService owner,long g,long expectedEpoch,Runnable dispatching)throws Exception{InputConnection input=guard(owner,g,expectedEpoch);if(!SearchInputRules.canSubmit(editor.imeOptions)){releaseRejectedPort(owner,g);throw new IOException("agent_ime_search_action_unavailable");}try{if(dispatching!=null)dispatching.run();boolean accepted=input.performEditorAction(EditorInfo.IME_ACTION_SEARCH);if(!accepted)throw new IOException("agent_ime_search_unconfirmed");return true;}catch(Exception failure){connectionFailure(owner,g);if(failure instanceof IOException)throw failure;throw new IOException("agent_ime_connection_failure");}}
 private void validateText(String text)throws IOException{if(text==null||text.length()>MAX_TEXT)throw new IOException("agent_ime_text_limit");}
 private void releaseRejectedPort(ControlService owner,long g){if(owner!=null&&owner==ControlService.instance&&owner.generation==g&&owner.agentImeLease.held(g))owner.agentImeLease.releaseAsync();else AgentImeLease.recoverAsync(this);updatePanel();}
 private void connectionFailure(ControlService owner,long g){if(owner!=null&&owner==ControlService.instance&&owner.generation==g&&owner.agentImeLease.held(g)){owner.pauseForHealth("Agent输入连接已变化 · 原目标保留");owner.agentImeLease.releaseAsync();}else AgentImeLease.recoverAsync(this);updatePanel();}
}
