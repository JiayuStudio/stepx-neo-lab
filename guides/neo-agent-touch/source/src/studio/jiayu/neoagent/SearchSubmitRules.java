package studio.jiayu.neoagent;

/** Dispatching IME_SEARCH is not proof that a result page has loaded. */
final class SearchSubmitRules {
 interface Port {
  SearchInputRules.Sample read()throws Exception;
  void selectAll()throws Exception;
  void pause(int milliseconds)throws Exception;
  /** Recheck the live editor, owner and exact sample before reserving and dispatching. */
  void search(SearchInputRules.Sample expected,Runnable dispatching)throws Exception;
 }
 interface Validator {void check(String text)throws Exception;}
 static final class Rejected extends Exception {Rejected(String message){super(message);}}
 static final class State {
  private String run="";
  private final java.util.IdentityHashMap<Object,java.util.HashSet<String>> queries=new java.util.IdentityHashMap<>();
  synchronized boolean dispatched(Object current,String task,String text){java.util.Set<String> sent=run.equals(task)?queries.get(current):null;return sent!=null&&sent.contains(text);}
  synchronized void reserve(Object current,String task,String text)throws Rejected{
   if(current==null||task==null||task.isEmpty()||text==null||text.trim().isEmpty())throw new Rejected("搜索编辑器或关键词无效，未提交");
   if(!run.equals(task)){run=task;queries.clear();}
   if(dispatched(current,task,text))throw new Rejected("该搜索已派发，未重复提交；请观察结果或重新进入搜索页");
   java.util.HashSet<String> sent=queries.get(current);
   if(sent==null){if(queries.size()>=32)throw new Rejected("本轮搜索编辑器记录已达上限，未派发；请结束本轮后再开始新目标");sent=new java.util.HashSet<>();queries.put(current,sent);}
   if(sent.size()>=64)throw new Rejected("本编辑器的搜索记录已达上限，未派发；请结束本轮后再开始新目标");
   sent.add(text);
  }
 }
 static String submit(Port port,SearchInputRules.Pending writes,State state,Object editor,String run,Validator validator)throws Exception{
  SearchInputRules.Sample before=port.read();
  if(writes.unresolved(editor,run,before))throw new Rejected("上一条输入尚未读回核验，未提交搜索");
  if(before==null||!before.readable()||before.text.trim().isEmpty()||before.text.length()>500)
   throw new Rejected("搜索词不能完整读回，未提交；请重新观察搜索框");
  if(state.dispatched(editor,run,before.text))throw new Rejected("该搜索已派发，未重复提交；请观察结果或重新进入搜索页");
  if(before.offset==-1){
   port.selectAll();SearchInputRules.Sample selected=null;
   for(int i=0;i<6;i++){port.pause(100);SearchInputRules.Sample sample=port.read();if(sample!=null&&sample.selected()&&before.text.equals(sample.text)){selected=sample;break;}}
   if(selected==null)throw new Rejected("搜索词的完整范围未确认，未提交");
   before=selected;
  }
  validator.check(before.text);
  final String query=before.text;
  // The port calls this only after its final owner/sample recheck. Reserve before a
  // void platform call: even an exception after dispatch must never trigger replay.
  final boolean[] reserved={false};
  port.search(before,()->{try{state.reserve(editor,run,query);reserved[0]=true;}catch(Rejected rejected){throw new IllegalStateException(rejected);}});
  if(!reserved[0])throw new Rejected("搜索未派发，请重新观察");
  return "已向当前搜索编辑器派发系统搜索动作；尚未核验结果，请观察搜索结果页，不重复提交";
 }
}
