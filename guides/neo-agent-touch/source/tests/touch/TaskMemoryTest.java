package studio.jiayu.neoagent;
public final class TaskMemoryTest {
 static int count;static void yes(boolean result){if(!result)throw new AssertionError("Wrong tentative memory state");count++;}
 public static void main(String[] args){
  TaskMemory memory=new TaskMemory();yes(memory.summary().isEmpty());
  yes(memory.propose("已观察：暂存槽草2、铃铛1；下一步寻找可见草牌", "1:2","tap"));yes(!memory.sent());
  memory.dispatched();yes(memory.sent());
  yes(!memory.propose("","1:3","wait"));yes(memory.sent());
  yes(memory.propose("新画面暂存槽为空，需要重新核对棋盘", "1:4","wait"));yes(!memory.sent());
  TaskMemory restored=new TaskMemory();restored.restore(memory.summary(),"1:4","wait",false);yes(restored.summary().equals(memory.summary()));yes(!restored.sent());
  boolean denied=false;try{memory.propose(new String(new char[4097]),"1:5","tap");}catch(IllegalArgumentException expected){denied=true;}yes(denied);yes(memory.summary().startsWith("新画面"));
  memory.propose("临时调试key="+"sk-"+new String(new char[32]).replace((char)0,'a'), "1:6","wait");yes(!memory.summary().contains("sk-"));
  yes(String.valueOf(memory.snapshot().get("trust")).contains("不是当前画面事实或权限"));
  memory.clear();yes(memory.summary().isEmpty());yes(!memory.sent());
  System.out.println("PASS: TaskMemory "+count+" tentative state, receipts, redaction and reset checks");
 }
}
