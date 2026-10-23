package studio.jiayu.neoagent;
import android.app.Activity;import org.json.JSONObject;
/** Release compatibility shell. Voice capture belongs to the user's selected system keyboard. */
final class OfflineVoice {
 interface Listener {void state(JSONObject value);void result(String text,boolean direct);void error(String message);boolean foreground();}
 final Listener listener;
 OfflineVoice(Activity owner,Listener listener){this.listener=listener;}
 boolean active(){return false;}
 void start(boolean direct){listener.error("请使用当前输入法的语音输入，核对文字后发送");}
 void stop(){}
 void cancel(){}
 void close(){}
}
