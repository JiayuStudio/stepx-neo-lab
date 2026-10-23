package studio.jiayu.neoagent;
import java.util.concurrent.*;
public class ScreenshotWaitTest {
 public static void main(String[] args)throws Exception{
  CompletableFuture<String> complete=CompletableFuture.completedFuture("current frame");
  if(!"current frame".equals(ScreenshotWait.read(complete,50)))throw new AssertionError("Completed frame lost");
  CompletableFuture<String> missing=new CompletableFuture<>();boolean timedOut=false;
  try{ScreenshotWait.read(missing,1);}catch(ScreenshotWait.Deadline expected){timedOut=true;}
  if(!timedOut||!missing.isCancelled()||missing.complete("late frame"))throw new AssertionError("Late frame remained eligible");
  Object resource=new Object();java.util.concurrent.atomic.AtomicInteger recycled=new java.util.concurrent.atomic.AtomicInteger();
  CompletableFuture<Object> racing=new CompletableFuture<Object>(){public boolean cancel(boolean interrupt){complete(resource);return super.cancel(interrupt);}};
  try{ScreenshotWait.read(racing,1,value->{if(value!=resource)throw new AssertionError("Wrong resource discarded");recycled.incrementAndGet();});throw new AssertionError("Racing frame accepted after deadline");}catch(ScreenshotWait.Deadline expected){}
  if(recycled.get()!=1)throw new AssertionError("Frame arriving between deadline and cancellation leaked");
  CompletableFuture<String> fresh=CompletableFuture.completedFuture("fresh frame");
  if(!"fresh frame".equals(ScreenshotWait.read(fresh,50)))throw new AssertionError("Old deadline affected new capture");
  Exception protectedScreen=new Exception("protected screen");CompletableFuture<String> denied=new CompletableFuture<>();denied.completeExceptionally(protectedScreen);
  try{ScreenshotWait.read(denied,50);throw new AssertionError("Protected capture accepted");}catch(Exception expected){if(expected!=protectedScreen)throw new AssertionError("Protected capture became transient timeout");}
  CompletableFuture<String> interrupted=new CompletableFuture<>();Thread.currentThread().interrupt();
  try{ScreenshotWait.read(interrupted,50);throw new AssertionError("Interrupted capture returned");}catch(InterruptedException expected){if(!Thread.currentThread().isInterrupted()||!interrupted.isCancelled())throw new AssertionError("Interrupt/ownership cancellation lost");}finally{Thread.interrupted();}
  if(!CaptureRules.retryTimeout(1,8000)||CaptureRules.retryTimeout(2,10000)||CaptureRules.retryTimeout(1,13000)||CaptureRules.retryable(6))throw new AssertionError("Unbounded retry or protected-screen retry");
  System.out.println("PASS: screenshot callback deadline, late frame rejection, fresh capture, protected error, interruption and bounded retry");
 }
}
