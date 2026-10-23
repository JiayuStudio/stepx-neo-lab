package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.util.*;
/** Shared encrypted journal. Cached UI snapshots are merged against their read baseline. */
final class ChatStore {
 static final Object LOCK=new Object();
 private ChatStore(){}
 static JSONObject empty(){try{return new JSONObject().put("sessions",new JSONArray()).put("current","");}catch(Exception impossible){throw new IllegalStateException(impossible);}}
 static JSONObject copy(JSONObject o)throws JSONException{return new JSONObject(o.toString());}
 static JSONObject read(Context c,String key)throws Exception{synchronized(LOCK){String raw=new Vault(c).get(key);return raw.isEmpty()?empty():new JSONObject(raw);}}
 static JSONObject write(Context c,String key,JSONObject value)throws Exception{synchronized(LOCK){String text=value.toString();new Vault(c).put(key,text);String stored=new Vault(c).get(key);if(!text.equals(stored))throw new Exception("本机对话保存未通过读回核验");return new JSONObject(stored);}}
 static JSONObject save(Context c,String key,JSONObject baseline,JSONObject local)throws Exception{synchronized(LOCK){return write(c,key,merge(read(c,key),baseline,local));}}
 private static LinkedHashMap<String,JSONObject> sessions(JSONObject data){LinkedHashMap<String,JSONObject> map=new LinkedHashMap<>();JSONArray a=data.optJSONArray("sessions");if(a!=null)for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s!=null&&!s.optString("id").isEmpty())map.put(s.optString("id"),s);}return map;}
 private static LinkedHashMap<String,JSONObject> messages(JSONObject session){LinkedHashMap<String,JSONObject> map=new LinkedHashMap<>();HashMap<String,Integer> repeats=new HashMap<>();JSONArray a=session==null?null:session.optJSONArray("messages");if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;String key;if(!m.optString("outcome_event").isEmpty())key="event:"+m.optString("outcome_event");else if(!m.optString("message_id").isEmpty())key="id:"+m.optString("message_id");else{String raw=m.optString("role")+"\u0000"+m.optLong("time")+"\u0000"+m.optString("text");int count=repeats.containsKey(raw)?repeats.get(raw):0;repeats.put(raw,count+1);key="legacy:"+raw+"\u0000"+count;}if(!map.containsKey(key))map.put(key,m);}return map;}
 private static JSONObject normalized(JSONObject value)throws Exception{JSONObject out=copy(value);ArrayList<JSONObject> all=new ArrayList<>(messages(value).values());Collections.sort(all,(a,b)->Long.compare(a.optLong("time"),b.optLong("time")));JSONArray list=new JSONArray();for(int i=Math.max(0,all.size()-100);i<all.size();i++)list.put(all.get(i));out.put("messages",list);return out;}
 static JSONObject merge(JSONObject latest,JSONObject baseline,JSONObject local)throws Exception{
  LinkedHashMap<String,JSONObject> result=sessions(latest),base=sessions(baseline),changes=sessions(local);
  for(String id:base.keySet())if(!changes.containsKey(id))result.remove(id);
  for(Map.Entry<String,JSONObject> entry:changes.entrySet()){
   String id=entry.getKey();JSONObject proposed=entry.getValue(),before=base.get(id),now=result.get(id);
   if(before!=null&&before.toString().equals(proposed.toString()))continue;
   if(now==null){if(before==null)result.put(id,normalized(proposed));continue;}
   JSONObject merged=copy(now);for(java.util.Iterator<String> it=proposed.keys();it.hasNext();){String k=it.next();if(k.equals("messages")||k.equals("id"))continue;Object value=proposed.opt(k),old=before==null?null:before.opt(k);if(before==null||!String.valueOf(value).equals(String.valueOf(old)))merged.put(k,value);}
   LinkedHashMap<String,JSONObject> oldMessages=messages(before),newMessages=messages(proposed),all=messages(now);
   for(String key:oldMessages.keySet())if(!newMessages.containsKey(key))all.remove(key);
   for(Map.Entry<String,JSONObject> item:newMessages.entrySet()){String key=item.getKey();if(!oldMessages.containsKey(key)&&!all.containsKey(key))all.put(key,item.getValue());}
   ArrayList<JSONObject> ordered=new ArrayList<>(all.values());Collections.sort(ordered,(a,b)->Long.compare(a.optLong("time"),b.optLong("time")));JSONArray ma=new JSONArray();for(int i=Math.max(0,ordered.size()-100);i<ordered.size();i++)ma.put(ordered.get(i));merged.put("messages",ma);result.put(id,merged);
  }
  JSONObject out=copy(latest);JSONArray array=new JSONArray();for(JSONObject s:result.values())array.put(s);String current=latest.optString("current");if(!local.optString("current").equals(baseline.optString("current")))current=local.optString("current");if(!result.containsKey(current))current=result.isEmpty()?"":new ArrayList<>(result.keySet()).get(result.size()-1);out.put("sessions",array).put("current",current);return out;
 }
}
