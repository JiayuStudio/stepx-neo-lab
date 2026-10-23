package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.util.*;
/** User-editable taste preferences. Encrypted locally; only relevant facts enter the planner. */
final class UserMemory {
 private final Vault vault;private final String storageKey;private JSONObject data;private boolean batching;
 UserMemory(Context c)throws Exception{this(c,"user-preferences");}
 UserMemory(Context c,String key)throws Exception{storageKey=key;vault=new Vault(c);String saved=vault.get(storageKey);data=saved.isEmpty()?new JSONObject():new JSONObject(saved);if(!data.has("items"))data.put("items",new JSONObject());}
 boolean enabled(){return data.optBoolean("enabled",true);}
 JSONObject json()throws Exception{JSONArray items=new JSONArray();JSONObject stored=data.getJSONObject("items");for(String key:MemoryRules.LABELS.keySet())items.put(new JSONObject().put("id",key).put("label",MemoryRules.LABELS.get(key)).put("value",stored.optJSONObject(key)==null?"":stored.getJSONObject(key).optString("value")).put("source",stored.optJSONObject(key)==null?"":stored.getJSONObject(key).optString("source")));return new JSONObject().put("enabled",enabled()).put("items",items);}
 int learn(String text)throws Exception{return learnAt(text,System.currentTimeMillis());}
 int learnAt(String text,long time)throws Exception{if(!enabled())return 0;Map<String,String> found=MemoryRules.extract(text);JSONObject stored=data.getJSONObject("items");int changed=0;for(Map.Entry<String,String> e:found.entrySet()){JSONObject previous=stored.optJSONObject(e.getKey());if(previous!=null&&previous.optLong("updated",0)>time)continue;if(stored.optJSONObject(e.getKey())!=null&&stored.getJSONObject(e.getKey()).optString("value").equals(e.getValue())&&!stored.getJSONObject(e.getKey()).optString("source").equals("inferred-repeat"))continue;stored.put(e.getKey(),new JSONObject().put("value",e.getValue()).put("source","explicit-chat").put("updated",time));changed++;}if(changed>0)save();return changed+observeAt(text,java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay(),java.time.LocalDate.now().toEpochDay());}
 int observe(String text,long day)throws Exception{return observeAt(text,day,day);}
 int observeAt(String text,long day,long nowDay)throws Exception{if(!enabled())return 0;JSONObject observations=data.optJSONObject("observations");if(observations==null){observations=new JSONObject();data.put("observations",observations);}JSONObject items=data.getJSONObject("items");int changed=0;
  for(Map.Entry<String,String> choice:MemoryRules.choices(text).entrySet()){
   JSONObject existing=items.optJSONObject(choice.getKey());if(existing!=null&&!existing.optString("source").equals("inferred-repeat"))continue;
   JSONObject observation=observations.optJSONObject(choice.getKey());if(observation==null||!observation.optString("value").equals(choice.getValue()))observation=new JSONObject().put("value",choice.getValue()).put("days",new JSONArray());
   JSONArray days=observation.getJSONArray("days");boolean today=false;for(int i=0;i<days.length();i++)if(days.getLong(i)==day)today=true;if(!today)days.put(day);while(days.length()>12)days.remove(0);observations.put(choice.getKey(),observation);
   int recent=0;for(int i=0;i<days.length();i++)if(nowDay-days.getLong(i)>=0&&nowDay-days.getLong(i)<=30)recent++;
   if(recent>=3&&(existing==null||!existing.optString("value").equals(choice.getValue()))){items.put(choice.getKey(),new JSONObject().put("value",choice.getValue()).put("source","inferred-repeat").put("updated",System.currentTimeMillis()));changed++;}
  }save();return changed;
 }
 int ingest(JSONArray sessions)throws Exception{
  if(!enabled())return 0;JSONArray seen=data.optJSONArray("seen-history");if(seen==null)seen=new JSONArray();HashSet<String> known=new HashSet<>();for(int i=0;i<seen.length();i++)known.add(seen.getString(i));
  ArrayList<JSONObject> messages=new ArrayList<>();for(int i=0;i<sessions.length();i++){JSONObject session=sessions.optJSONObject(i);if(session==null)continue;JSONArray list=session.optJSONArray("messages");if(list==null)continue;for(int j=0;j<list.length();j++){JSONObject message=list.optJSONObject(j);if(message!=null&&message.optString("role").equals("user")&&message.optLong("time",0)>data.optLong("forget-before",0))messages.add(message);}}
  java.util.Collections.sort(messages,(a,b)->Long.compare(a.optLong("time"),b.optLong("time")));int changed=0;batching=true;
  try{for(int i=Math.max(0,messages.size()-1000);i<messages.size();i++){JSONObject message=messages.get(i);String text=message.optString("text");if(!text.matches("(?is).*(咖啡|拿铁|美式|馥芮白|卡布奇诺|澳白|瑞幸|星巴克|库迪|外卖|餐|吃饭).*"))continue;
    byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((message.optLong("time")+"|"+text).getBytes("UTF-8"));StringBuilder id=new StringBuilder();for(byte b:digest)id.append(String.format(java.util.Locale.ROOT,"%02x",b&255));if(!known.add(id.toString()))continue;
    changed+=learnAt(text,message.getLong("time"));seen.put(id.toString());while(seen.length()>1000)seen.remove(0);
   }data.put("seen-history",seen);
  }finally{batching=false;}save();return changed;
 }
 void edit(JSONObject input)throws Exception{data.put("enabled",input.optBoolean("enabled",true));JSONObject stored=new JSONObject(),values=input.getJSONObject("values");for(String key:MemoryRules.LABELS.keySet()){String value=values.optString(key,"").trim();if(value.isEmpty())continue;if(!MemoryRules.safeValue(value))throw new Exception("偏好内容不能包含密钥、账号、地址或联系方式");JSONObject previous=data.getJSONObject("items").optJSONObject(key);stored.put(key,previous!=null&&value.equals(previous.optString("value"))?previous:new JSONObject().put("value",value).put("source","manual").put("updated",System.currentTimeMillis()));}data.put("items",stored);data.remove("observations");data.put("forget-before",System.currentTimeMillis());save();}
 void clear()throws Exception{data.put("items",new JSONObject());data.remove("observations");data.remove("seen-history");data.put("forget-before",System.currentTimeMillis());save();}
 void save()throws Exception{if(batching)return;vault.put(storageKey,data.toString());}
 String contextFor(String text)throws Exception{if(!enabled())return "";StringBuilder b=new StringBuilder();JSONObject stored=data.getJSONObject("items");for(String key:MemoryRules.LABELS.keySet()){JSONObject item=stored.optJSONObject(key);if(item!=null&&MemoryRules.relevant(key,text))b.append(MemoryRules.LABELS.get(key)).append('：').append(item.getString("value")).append(item.optString("source").equals("inferred-repeat")?"（根据多次选择推测）":"").append('\n');}return b.length()==0?"":"已保存的口味偏好（推测项已标注；本次要求优先，不是当前订单事实或付款授权）：\n"+b;}
}
