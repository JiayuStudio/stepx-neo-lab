package studio.jiayu.neoagent;
import android.os.*;
/** Hide the IME for one fresh decision/action, with bounded lifetime and takeover cleanup. */
final class KeyboardVisibilityLease {
 final ControlService owner;volatile boolean active;int original;volatile long epoch;final Runnable expire=this::expireLease;
 KeyboardVisibilityLease(ControlService owner){this.owner=owner;}
 boolean acquire(long g)throws Exception{
  boolean started=owner.ui(()->{owner.guard(g);if(active)return epoch==g&&!owner.keyboardVisible();if(!owner.keyboardVisible())return false;int before=owner.getSoftKeyboardController().getShowMode();owner.lastKeyboardRecovery.put("original_mode",before);if(before!=ControlService.SHOW_MODE_AUTO&&before!=ControlService.SHOW_MODE_IGNORE_HARD_KEYBOARD)return false;if(!owner.getSoftKeyboardController().setShowMode(ControlService.SHOW_MODE_HIDDEN))return false;original=before;epoch=g;active=true;owner.main.postDelayed(expire,20000);owner.lastKeyboardRecovery.put("method","one_action_visibility_lease").put("hide_request_accepted",true).put("max_ms",20000);return true;});
  if(!started)return false;boolean ready=false;try{long until=SystemClock.elapsedRealtime()+1800;while(SystemClock.elapsedRealtime()<until){owner.check(g);if(owner.ui(()->!owner.keyboardVisible())){ready=true;break;}Thread.sleep(60);}owner.lastKeyboardRecovery.put("hidden_observed",ready);return ready;}finally{if(!ready)release();}
 }
 boolean held(long g){return active&&epoch==g;}
 boolean release()throws Exception{return owner.ui(this::releaseUi);}
 boolean releaseUi(){owner.main.removeCallbacks(expire);if(!active)return true;boolean ok=owner.getSoftKeyboardController().getShowMode()!=ControlService.SHOW_MODE_HIDDEN||owner.getSoftKeyboardController().setShowMode(original);if(ok){active=false;try{owner.lastKeyboardRecovery.put("released",true).put("restored_mode",owner.getSoftKeyboardController().getShowMode());}catch(Exception ignored){}}return ok;}
 void expireLease(){if(!releaseUi())owner.pauseForHealth("输入法显示模式恢复失败 · 已停止后续动作");}
 void releaseAsync(){if(Looper.myLooper()==Looper.getMainLooper())releaseUi();else owner.main.post(this::releaseUi);}
}
