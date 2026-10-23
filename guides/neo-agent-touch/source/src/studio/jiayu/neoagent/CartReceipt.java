package studio.jiayu.neoagent;
import android.graphics.Rect;import java.util.Map;import org.json.*;
/** A current visible app receipt for this generation's pending cart action, never a model claim. */
final class CartReceipt {
 static boolean observed(JSONArray nodes,Map<Integer,Rect> boxes,Rect app,boolean granted,boolean armed,boolean current,boolean pending)throws Exception{
  if(!granted||!armed||!current||!pending||app==null||app.isEmpty())return false;
  for(int i=0;i<nodes.length();i++){JSONObject n=nodes.getJSONObject(i);String t=n.optString("text").trim(),d=n.optString("description").trim();if(!t.isEmpty()&&!d.isEmpty()&&!t.equals(d))continue;String label=t.isEmpty()?d:t;Rect b=boxes.get(n.optInt("id"));if(!n.optBoolean("clickable")&&!n.optBoolean("editable")&&label.matches("^(已成功加入购物车|成功加入购物车|已加入购物车|加入购物车成功|加购成功)$")&&b!=null&&!b.isEmpty()&&app.contains(b)&&b.width()<app.width()*.85&&b.height()<app.height()*.15)return true;}return false;
 }
}
