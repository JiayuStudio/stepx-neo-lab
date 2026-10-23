package studio.jiayu.neoagent;

/** Phone-local ownership. A response from before takeover can never reacquire control. */
final class RunOwnership {
 enum State { IDLE, RUNNING, PAUSED, ENDED }
 static final class ObservationId {
  final long epoch,revision;
  ObservationId(long epoch,long revision){this.epoch=epoch;this.revision=revision;}
  @Override public String toString(){return epoch+":"+revision;}
 }
 private State state=State.IDLE;private long epoch,revision;
 synchronized long start(){if(busy())throw new IllegalStateException("A goal already owns the runtime");state=State.RUNNING;revision=0;return ++epoch;}
 synchronized long pause(){if(state!=State.RUNNING)return epoch;state=State.PAUSED;return ++epoch;}
 synchronized long resume(){if(state!=State.PAUSED)throw new IllegalStateException("No paused goal");state=State.RUNNING;return ++epoch;}
 synchronized long replacePaused(long expected){if(state!=State.PAUSED||epoch!=expected)throw new IllegalStateException("Paused goal changed");state=State.RUNNING;revision=0;return ++epoch;}
 synchronized long end(){state=State.ENDED;return ++epoch;}
 synchronized boolean finish(long token){if(!accepts(token))return false;state=State.ENDED;++epoch;return true;}
 synchronized boolean accepts(long token){return state==State.RUNNING&&epoch==token;}
 synchronized ObservationId observe(long token){if(!accepts(token))throw new IllegalStateException("Control lease expired");return new ObservationId(epoch,++revision);}
 synchronized boolean current(ObservationId id){return id!=null&&accepts(id.epoch)&&id.revision==revision;}
 synchronized boolean busy(){return state==State.RUNNING||state==State.PAUSED;}
 synchronized State state(){return state;}
}
