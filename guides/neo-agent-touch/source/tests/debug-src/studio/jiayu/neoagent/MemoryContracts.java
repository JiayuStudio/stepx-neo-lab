package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.io.*;
final class MemoryContracts {
 static int passed;static final String KEY="test-user-preferences-contracts";
 static void yes(boolean ok,String name)throws Exception{if(!ok)throw new Exception(name);passed++;}
 static void run(Context c){String original="",production="";JSONObject report=new JSONObject();try{Vault vault=new Vault(c);original=vault.get(KEY);production=vault.get("user-preferences");UserMemory memory=new UserMemory(c,KEY);memory.edit(new JSONObject().put("enabled",true).put("values",new JSONObject()));
  yes(memory.learn("记住，我平时咖啡都要冰的，无糖，大杯")>=3,"explicit habit learned");
  yes(new UserMemory(c,KEY).contextFor("点一杯咖啡").contains("无糖"),"encrypted reload");
  yes(!c.getSharedPreferences("vault",0).getString(KEY,"").contains("无糖"),"encrypted at rest");
  yes(memory.contextFor("在淘宝找手机支架").isEmpty(),"purpose limited context");
  yes(memory.learn("今天来杯热咖啡，半糖")==0,"one-off does not overwrite");
  yes(new UserMemory(c,KEY).contextFor("咖啡").contains("冰"),"habit retained after one-off");
  memory.edit(new JSONObject().put("enabled",false).put("values",new JSONObject().put("coffee.sugar","半糖")));yes(memory.contextFor("咖啡").isEmpty(),"disabled means no context");yes(memory.learn("以后咖啡都无糖")==0,"disabled means no learning");
  memory.clear();yes(!new UserMemory(c,KEY).json().toString().contains("半糖"),"clear removes habit");memory.edit(new JSONObject().put("enabled",true).put("values",new JSONObject()));
  memory.observe("今天来杯冰咖啡无糖",100);memory.observe("今天来杯冰咖啡无糖",100);memory.observe("今天来杯冰咖啡无糖",101);yes(memory.contextFor("咖啡").isEmpty(),"not inferred from one day or two days");
  memory.observe("今天来杯冰咖啡无糖",102);yes(memory.contextFor("咖啡").contains("推测"),"three distinct days infer with label");
  memory.learn("以后我的咖啡都要热的");memory.observe("今天来杯冰咖啡无糖",103);memory.observe("今天来杯冰咖啡无糖",104);memory.observe("今天来杯冰咖啡无糖",105);yes(memory.contextFor("咖啡").contains("咖啡冷热：热"),"explicit preference beats inference");
  memory.clear();yes(memory.contextFor("咖啡").isEmpty(),"clear removes inferred preferences");
  memory.edit(new JSONObject().put("enabled",true).put("values",new JSONObject()));
  new Vault(c).remove(KEY);memory=new UserMemory(c,KEY);long dayMillis=86400000L,now=System.currentTimeMillis();JSONArray history=new JSONArray(),messages=new JSONArray();for(int i=3;i>=1;i--)messages.put(new JSONObject().put("role","user").put("text","今天来杯冰咖啡，无糖").put("time",now-i*dayMillis));messages.put(new JSONObject().put("role","assistant").put("text","你喜欢热咖啡，加糖").put("time",now));history.put(new JSONObject().put("messages",messages));
  memory.ingest(history);yes(memory.contextFor("点咖啡").contains("推测"),"learns from historical user choices");yes(!memory.contextFor("点咖啡").contains("咖啡冷热：热"),"ignores assistant suggestions as evidence");
  memory.clear();memory.ingest(history);yes(memory.contextFor("点咖啡").isEmpty(),"cleared history does not resurrect");
  messages.put(new JSONObject().put("role","user").put("text","以后我的咖啡都要热的").put("time",System.currentTimeMillis()+10));memory.ingest(history);yes(memory.contextFor("点咖啡").contains("咖啡冷热：热"),"new explicit historical preference applies");
  report.put("success",true).put("passed",passed).put("real_network_calls",0);
 }catch(Exception e){try{report.put("success",false).put("passed",passed).put("failure",e.getMessage());}catch(Exception ignored){}}
 finally{try{if(original.isEmpty())new Vault(c).remove(KEY);else new Vault(c).put(KEY,original);report.put("production_preferences_untouched",production.equals(new Vault(c).get("user-preferences")));try(FileOutputStream out=c.openFileOutput("memory-contracts.json",0)){out.write(report.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}}}
}
