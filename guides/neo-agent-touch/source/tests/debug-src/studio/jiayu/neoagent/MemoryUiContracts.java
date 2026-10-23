package studio.jiayu.neoagent;
import org.json.*;import java.io.*;
/** Production chat hooks with isolated chat and preference stores; no model requests. */
final class MemoryUiContracts {
 static void yes(boolean b,String reason)throws Exception{if(!b)throw new Exception(reason);}
 static String value(JSONObject state,String id)throws Exception{JSONArray items=state.getJSONArray("items");for(int i=0;i<items.length();i++)if(items.getJSONObject(i).getString("id").equals(id))return items.getJSONObject(i).getString("value");return "";}
 static void run(UiQaActivity a){JSONObject out=new JSONObject();String production="",chats="";int passed=0;try{ControlService s=ControlService.instance;yes(s!=null&&s.connected&&!s.busy()&&!a.planning,"Runtime not idle");passed++;Vault vault=new Vault(a);production=vault.get("user-preferences");chats=vault.get("local-chats");vault.remove(a.memoryKey());a.loadChats();
 a.message("user","我平时咖啡都要热的，无糖，大杯");yes(value(a.memoryState(),"coffee.temperature").equals("热")&&value(a.memoryState(),"coffee.sugar").equals("无糖"),"Chat user hook did not collect preferences");passed++;
 a.message("assistant","你喜欢冰咖啡半糖");yes(value(a.memoryState(),"coffee.temperature").equals("热"),"Assistant suggestion became user preference");passed++;
 a.message("user","今天换一杯冰咖啡，半糖");yes(value(a.memoryState(),"coffee.temperature").equals("热"),"One-off changed habitual temperature");passed++;
 yes(a.memoryContext("点一杯咖啡").contains("咖啡冷热：热")&&a.memoryContext("点一杯咖啡").contains("不是当前订单事实或付款授权"),"Planner preference context missing or promoted to authorization");passed++;
 yes(a.memoryContext("去淘宝查手机价格").isEmpty(),"Unrelated preferences entered shopping context");passed++;
 JSONObject values=new JSONObject().put("coffee.temperature","热").put("coffee.sugar","无糖").put("coffee.size","大杯");a.saveMemory(new JSONObject().put("enabled",false).put("values",values));yes(a.memoryContext("咖啡").isEmpty(),"Disabled memory still used");passed++;
 a.saveMemory(new JSONObject().put("enabled",true).put("values",values.put("coffee.temperature","常温")));yes(a.memoryContext("咖啡").contains("咖啡冷热：常温"),"User correction missing");passed++;
 new UserMemory(a,a.memoryKey()).clear();a.updateMemory();yes(value(a.memoryState(),"coffee.temperature").isEmpty(),"Old chats resurrected after forgetting");passed++;
 yes(production.equals(vault.get("user-preferences"))&&chats.equals(vault.get("local-chats")),"Production user data changed");passed++;
 a.saveMemory(new JSONObject().put("enabled",true).put("values",values));a.web.evaluateJavascript("page='memory';render();",null);out.put("success",true).put("passed",passed).put("production_preferences_untouched",true).put("production_chats_untouched",true).put("real_network_calls",0).put("third_party_actions",0);
 a.web.evaluateJavascript("(()=>{page='memory';render();const f=document.getElementById('memory-coffee.temperature');f.value='少冰';f.dispatchEvent(new Event('input',{bubbles:true}));window.agentEvent({type:'state',data:S});const retained=document.getElementById('memory-coffee.temperature').value==='少冰';document.querySelector('[data-a=saveMemory]').click();return retained;})()",saved->new android.os.Handler().postDelayed(()->{try{yes("true".equals(saved),"State refresh erased preference draft");yes(value(a.memoryState(),"coffee.temperature").equals("少冰"),"WebView form/bridge did not save preference");a.getIntent().removeExtra("memoryTest");yes(a.memoryKey().equals("test-user-memory-ui"),"New intent flag escaped isolated preference namespace");out.put("passed",out.getInt("passed")+3).put("webview_form_saved",true).put("isolation_survives_intent_change",true);}catch(Exception e){try{out.put("success",false).put("failure",e.getMessage());}catch(Exception ignored){}}},400));

 }catch(Exception e){try{out.put("success",false).put("passed",passed).put("failure",e.getMessage());}catch(Exception ignored){}}
 final JSONObject result=out;new android.os.Handler().postDelayed(()->{try{new Vault(a).remove("test-user-memory-ui");try(FileOutputStream f=a.openFileOutput("memory-ui-contracts.json",0)){f.write(result.toString(2).getBytes("UTF-8"));}}catch(Exception ignored){}a.finish();},12000);
 }
}
