package studio.jiayu.neoagent;

/** A native IME search action is required for the node-less input fallback. */
final class SearchInputRules {
 /** Shared by metadata and the two native submit ports. High flags are not actions. */
 static boolean canSubmit(int imeOptions){return (imeOptions&255)==3&&(imeOptions&0x40000000)==0;}
 static boolean ready(int sdk,boolean started,boolean connection,boolean samePackage,int inputType,int imeOptions){
  int type=inputType&15,variation=inputType&4080;
  return sdk>=33&&started&&connection&&samePackage&&type==1
   &&(variation==0||variation==160||variation==176)&&(imeOptions&255)==3;
 }
 /** -1 is the official default InputConnection's unknown absolute offset, not a failed read. */
 static boolean readable(String text,int offset,int start,int end){
  return text!=null&&text.length()<=512&&(offset==0||offset==-1)&&start>=0&&end>=0&&start<=text.length()&&end<=text.length();
 }
 static boolean selected(String text,int offset,int start,int end){
  return readable(text,offset,start,end)&&Math.min(start,end)==0&&Math.max(start,end)==text.length();
 }
 static final class Sample {
  final String text;final int offset,start,end;
  Sample(String value,int position,int selectionStart,int selectionEnd){text=value;offset=position;start=selectionStart;end=selectionEnd;}
  boolean readable(){return SearchInputRules.readable(text,offset,start,end);}
  boolean selected(){return SearchInputRules.selected(text,offset,start,end);}
  boolean same(Sample other){return other!=null&&offset==other.offset&&start==other.start&&end==other.end&&text!=null&&text.equals(other.text);}
 }
 interface Port {
  Sample read()throws Exception;
  void selectAll()throws Exception;
  /** Revalidate the expected sample and owner/session atomically before calling dispatching and the native commit. */
  void commit(Sample expected,String text,Runnable dispatching)throws Exception;
  void pause(int milliseconds)throws Exception;
 }
 static final class Rejected extends Exception {Rejected(String message){super(message);}}
 enum Result { ALREADY_PRESENT, VERIFIED, DISPATCHED_UNVERIFIED }
 /** One unresolved write per live editor/run. A timeout is not a failed write that may be replayed. */
 static final class Pending {
  private Object editor;private String run="",text="";
  synchronized boolean unresolved(Object current,String task,Sample readback){
   if(editor!=current||!run.equals(task))return false;
   if(exact(readback,text)){editor=null;text="";run="";return false;}return true;
  }
  synchronized void dispatched(Object current,String task,String value){editor=current;run=task;text=value;}
 }
 static boolean exact(Sample sample,String query){return sample!=null&&sample.readable()&&query.equals(sample.text);}
 static Result replace(Port port,Pending pending,Object editor,String run,String query)throws Exception{
  Sample before=port.read();
  if(pending.unresolved(editor,run,before))throw new Rejected("上一条搜索输入已派发但尚未读回核验，未再次输入；请核对新画面或返回搜索入口");
  if(before==null||!before.readable())throw new Rejected("搜索输入框暂不能读回有效范围，未改写；请重新观察搜索焦点");
  // With no absolute offset, select-all + observed selection provides the replacement range.
  // Merely issuing a void context-menu call is not an acknowledgement of selection.
  if(before.offset==-1||!before.text.isEmpty()&&!before.text.equals(query)){
   port.selectAll();Sample selected=null;
   for(int attempt=0;attempt<6;attempt++){
    port.pause(100);Sample now=port.read();if(now!=null&&now.selected()&&before.text.equals(now.text)){selected=now;break;}
   }
   if(selected==null)throw new Rejected("原搜索词尚未完整选中，未改写；请重新观察");
   before=selected;
  }
  if(before.text.equals(query))return Result.ALREADY_PRESENT;
  port.commit(before,query,()->pending.dispatched(editor,run,query));
  for(int attempt=0;attempt<8;attempt++){
   port.pause(100);Sample now;
   try{now=port.read();}catch(Exception unavailable){continue;}
   if(exact(now,query)){pending.unresolved(editor,run,now);return Result.VERIFIED;}
  }
  return Result.DISPATCHED_UNVERIFIED;
 }
}
