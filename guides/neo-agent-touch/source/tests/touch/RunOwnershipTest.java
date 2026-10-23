package studio.jiayu.neoagent;
import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
public final class RunOwnershipTest {
 static int passed;static void check(boolean ok){if(!ok)throw new AssertionError("Ownership invariant "+passed);passed++;}
 public static void main(String[] args)throws Exception{
  RunOwnership r=new RunOwnership();long first=r.start();RunOwnership.ObservationId old=r.observe(first);check(r.current(old));
  RunOwnership.ObservationId newer=r.observe(first);check(!r.current(old)&&r.current(newer));
  long handoff=r.pause();check(!r.accepts(first)&&!r.current(newer)&&r.busy());
  boolean rejected=false;try{r.start();}catch(IllegalStateException expected){rejected=true;}check(rejected);
  check(r.pause()==handoff);long resumed=r.resume();check(resumed>first&&!r.accepts(first)&&r.accepts(resumed));
  RunOwnership.ObservationId fresh=r.observe(resumed);check(r.current(fresh)&&!r.current(newer));
  check(!r.finish(first)&&r.accepts(resumed));check(r.finish(resumed)&&!r.busy());check(!r.finish(resumed));
  long next=r.start();check(!r.current(fresh)&&r.accepts(next));r.end();check(!r.accepts(next)&&!r.busy());
  RunOwnership swap=new RunOwnership();long former=swap.start();RunOwnership.ObservationId formerView=swap.observe(former);long held=swap.pause();
  boolean staleSwap=false;try{swap.replacePaused(former);}catch(IllegalStateException expected){staleSwap=true;}check(staleSwap&&swap.state()==RunOwnership.State.PAUSED);
  long replacement=swap.replacePaused(held);check(replacement>held&&swap.accepts(replacement)&&!swap.accepts(former)&&!swap.current(formerView));
  boolean runningSwap=false;try{swap.replacePaused(replacement);}catch(IllegalStateException expected){runningSwap=true;}check(runningSwap&&swap.accepts(replacement));
  long rollback=swap.pause();check(rollback>replacement&&!swap.accepts(replacement)&&swap.busy());long recovered=swap.resume();check(recovered>rollback&&swap.accepts(recovered)&&!swap.finish(former));
  // Model response is deliberately released only after handoff and a new lease.
  for(int i=0;i<100;i++){RunOwnership shared=new RunOwnership();long token=shared.start();RunOwnership.ObservationId view=shared.observe(token);CountDownLatch release=new CountDownLatch(1);AtomicBoolean staleAccepted=new AtomicBoolean();Thread response=new Thread(()->{try{release.await();staleAccepted.set(shared.accepts(token)||shared.current(view)||shared.finish(token));}catch(InterruptedException e){throw new RuntimeException(e);}});response.start();shared.pause();long latest=shared.resume();release.countDown();response.join(2000);check(!response.isAlive()&&!staleAccepted.get()&&shared.accepts(latest));}
  System.out.println("PASS: "+passed+" ownership checks; late responses after takeover cannot act or finish a resumed goal");
 }
}
