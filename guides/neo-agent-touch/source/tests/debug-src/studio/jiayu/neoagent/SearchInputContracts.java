package studio.jiayu.neoagent;
import android.app.Activity;import android.os.*;import android.text.InputType;import android.view.inputmethod.*;import android.widget.*;import org.json.*;import java.io.*;import java.util.concurrent.*;
/** Owned Android EditText/InputConnection only. No model, network, third-party app or permission changes. */
final class SearchInputContracts {
 static int checks;
 interface Job<T>{T run()throws Exception;}
 static <T>T ui(Activity a,Job<T> job)throws Exception{FutureTask<T> task=new FutureTask<>(()->{if(a.isFinishing()||a.isDestroyed())throw new Exception("Owned fixture closed");return job.run();});a.runOnUiThread(task);return task.get(5,TimeUnit.SECONDS);}
 static void yes(boolean value,String reason)throws Exception{if(!value)throw new Exception(reason);checks++;}
 static final class OffsetEditor extends EditText {
  OffsetEditor(Activity a){super(a);setSingleLine(true);setInputType(InputType.TYPE_CLASS_TEXT);setImeOptions(EditorInfo.IME_ACTION_SEARCH);}
  @Override public InputConnection onCreateInputConnection(EditorInfo info){InputConnection base=super.onCreateInputConnection(info);return base==null?null:new InputConnectionWrapper(base,false){
   // Exercise the documented default InputConnection contract: valid text with unknown absolute offset.
   @Override public SurroundingText getSurroundingText(int before,int after,int flags){CharSequence left=getTextBeforeCursor(before,flags),right=getTextAfterCursor(after,flags),selected=getSelectedText(flags);if(left==null||right==null)return null;String picked=selected==null?"":selected.toString();String value=left.toString()+picked+right.toString();return new SurroundingText(value,left.length(),left.length()+picked.length(),-1);}
  };}
 }
 static final class Port implements SearchInputRules.Port {
  final Activity activity;final InputConnection connection;int commits,selections;boolean ignoreSelection,hideAfterCommit;
  Port(Activity a,InputConnection c){activity=a;connection=c;}
  SearchInputRules.Sample raw(){SurroundingText s=connection.getSurroundingText(513,513,0);return s==null?null:new SearchInputRules.Sample(s.getText().toString(),s.getOffset(),s.getSelectionStart(),s.getSelectionEnd());}
  public SearchInputRules.Sample read()throws Exception{return ui(activity,()->hideAfterCommit&&commits>0?null:raw());}
  public void selectAll()throws Exception{ui(activity,()->{selections++;if(!ignoreSelection)connection.performContextMenuAction(android.R.id.selectAll);return true;});}
  public void commit(SearchInputRules.Sample expected,String text,Runnable dispatching)throws Exception{ui(activity,()->{if(!expected.same(raw()))throw new SearchInputRules.Rejected("Owned editor changed");dispatching.run();commits++;connection.commitText(text,1);return true;});}
  public void pause(int milliseconds)throws Exception{Thread.sleep(milliseconds);if(activity.isFinishing()||activity.isDestroyed())throw new Exception("Owned fixture closed");}
 }
 static void start(Activity a){
  ControlService service=ControlService.instance;if(service!=null&&service.busy()){a.finish();return;}
  LinearLayout layout=new LinearLayout(a);layout.setOrientation(1);TextView label=new TextView(a);label.setText("原生搜索输入连接：未知 offset、选择确认及禁止重放");layout.addView(label);OffsetEditor editor=new OffsetEditor(a);layout.addView(editor);a.setContentView(layout);
  new Handler(Looper.getMainLooper()).postDelayed(()->new Thread(()->run(a,editor),"search-input-native-fixture").start(),500);
 }
 static void run(Activity a,OffsetEditor field){JSONObject out=new JSONObject();checks=0;try{
  InputConnection connection=ui(a,()->{field.setText("旧公共搜索词");field.setSelection(field.length());return field.onCreateInputConnection(new EditorInfo());});yes(connection!=null,"Owned input connection absent");
  Port port=new Port(a,connection);Object session=new Object();SearchInputRules.Pending pending=new SearchInputRules.Pending();SearchInputRules.Sample initial=port.read();yes(initial!=null&&initial.offset==-1&&initial.readable(),"Unknown-offset native editor rejected");
  yes(SearchInputRules.replace(port,pending,session,"fixture","公共搜索甲")==SearchInputRules.Result.VERIFIED,"Native replacement not verified");
  yes(ui(a,()->field.getText().toString()).equals("公共搜索甲")&&port.commits==1&&port.selections==1,"Native replacement appended old text");
  yes(SearchInputRules.replace(port,pending,session,"fixture","公共搜索甲")==SearchInputRules.Result.ALREADY_PRESENT&&port.commits==1,"Same query committed twice");
  ui(a,()->{field.setText("旧公共搜索词");field.setSelection(field.length());return true;});Port ignored=new Port(a,connection);ignored.ignoreSelection=true;boolean rejected=false;
  try{SearchInputRules.replace(ignored,new SearchInputRules.Pending(),new Object(),"fixture","公共搜索乙");}catch(SearchInputRules.Rejected expected){rejected=true;}
  yes(rejected&&ignored.commits==0&&ui(a,()->field.getText().toString()).equals("旧公共搜索词"),"Unacknowledged selection modified text");
  Port lost=new Port(a,connection);lost.hideAfterCommit=true;SearchInputRules.Pending unresolved=new SearchInputRules.Pending();Object lostSession=new Object();
  yes(SearchInputRules.replace(lost,unresolved,lostSession,"fixture","公共搜索乙")==SearchInputRules.Result.DISPATCHED_UNVERIFIED,"Lost readback fabricated success");
  rejected=false;try{SearchInputRules.replace(lost,unresolved,lostSession,"fixture","公共搜索乙");}catch(SearchInputRules.Rejected expected){rejected=true;}
  yes(rejected&&lost.commits==1&&ui(a,()->field.getText().toString()).equals("公共搜索乙"),"Unresolved replacement replayed");
  lost.hideAfterCommit=false;yes(SearchInputRules.replace(lost,unresolved,lostSession,"fixture","公共搜索乙")==SearchInputRules.Result.ALREADY_PRESENT&&lost.commits==1,"Later native readback did not resolve receipt");
  out.put("success",true).put("unknown_offset_native_replace",true).put("readback_loss_not_success",true).put("unresolved_write_not_replayed",true);
 }catch(Throwable error){try{out.put("success",false).put("failure",error.getMessage());}catch(Exception ignored){}}
 finally{try{out.put("checks",checks).put("fixture_kind","owned Android editor and production search replacement protocol; not third-party acceptance").put("native_accessibility_editor_session_tested",false).put("network_calls",0).put("third_party_actions",0).put("permission_changes",0).put("sampledAt",System.currentTimeMillis());try(OutputStream stream=a.openFileOutput("search-input-contracts.json",0)){stream.write(out.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.runOnUiThread(a::finish);}}
}
