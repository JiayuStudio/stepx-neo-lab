package studio.jiayu.neoagent;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** A timed-out capture cannot later supply a frame to a newer observation. */
final class ScreenshotWait {
 static final class Deadline extends Exception {Deadline(){super("截图回调超时，原动作未执行");}}
 static <T> T read(CompletableFuture<T> pending,long millis)throws Exception{return read(pending,millis,value->{});}
 static <T> void discard(CompletableFuture<T> pending,Consumer<T> abandoned){
  if(!pending.cancel(false)&&!pending.isCompletedExceptionally()){
   T orphan=pending.getNow(null);if(orphan!=null)abandoned.accept(orphan);
  }
 }
 static <T> T read(CompletableFuture<T> pending,long millis,Consumer<T> abandoned)throws Exception{
  try{return pending.get(millis,TimeUnit.MILLISECONDS);}
  catch(TimeoutException timeout){discard(pending,abandoned);throw new Deadline();}
  catch(InterruptedException interrupted){discard(pending,abandoned);Thread.currentThread().interrupt();throw interrupted;}
  catch(ExecutionException failed){discard(pending,abandoned);if(failed.getCause() instanceof Exception)throw (Exception)failed.getCause();throw failed;}
 }
}
