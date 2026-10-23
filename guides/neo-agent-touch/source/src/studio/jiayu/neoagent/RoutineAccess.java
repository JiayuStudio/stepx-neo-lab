package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.util.*;
/** Remember routine control consent by recipient and explicitly selected applications. */
final class RoutineAccess {
 static final String KEY="routine-control-consent-v1";final Context context;
 RoutineAccess(Context c){context=c;}
 static JSONArray decodeRecords(String raw)throws Exception{if(raw.isEmpty())return new JSONArray();JSONObject saved=new JSONObject(raw);return saved.has("grants")?saved.getJSONArray("grants"):new JSONArray().put(saved);}
 JSONArray records()throws Exception{return decodeRecords(new Vault(context).get(KEY));}
 private static boolean known(JSONObject record){String scope=record.optString("visual_scope");return RoutineAccessRules.SCOPE.equals(scope)||RoutineAccessRules.LEGACY_SCOPE.equals(scope);}
 static LinkedHashMap<String,Boolean> scopes(JSONObject record)throws Exception{
  LinkedHashMap<String,Boolean> scopes=new LinkedHashMap<>();String scope=record.optString("visual_scope");
  if(RoutineAccessRules.LEGACY_SCOPE.equals(scope)){
   JSONArray apps=record.getJSONArray("apps");ArrayList<String> packages=new ArrayList<>();for(int i=0;i<apps.length();i++)packages.add(apps.getString(i));
   return RoutineAccessRules.legacy(packages,record.optBoolean("vision"));
  }
  if(!RoutineAccessRules.SCOPE.equals(scope))return scopes;
  JSONArray apps=record.getJSONArray("app_scopes");for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");Object vision=app.get("vision");
   if(!(vision instanceof Boolean)||scopes.containsKey(pkg))throw new JSONException("Invalid application scope");
   scopes=RoutineAccessRules.merge(scopes,Collections.singletonList(pkg),(Boolean)vision);
  }
  return scopes;
 }
 static JSONObject record(String endpoint,Map<String,Boolean> scopes)throws Exception{
  JSONArray apps=new JSONArray();for(Map.Entry<String,Boolean> app:scopes.entrySet())apps.put(new JSONObject().put("package",app.getKey()).put("vision",app.getValue()));
  return new JSONObject().put("endpoint",endpoint).put("visual_scope",RoutineAccessRules.SCOPE).put("app_scopes",apps);
 }
 static JSONArray approvalRecords(JSONArray records,String endpoint,Collection<String> apps,boolean vision,java.util.function.Predicate<String> retained)throws Exception{
  if(apps==null||apps.isEmpty())throw new IllegalArgumentException("Missing applications");
  JSONArray next=new JSONArray();LinkedHashMap<String,Boolean> existing=new LinkedHashMap<>();
  for(int i=0;i<records.length();i++){
   JSONObject previous=records.getJSONObject(i);
   if(endpoint.equals(previous.optString("endpoint"))&&known(previous)){
    for(Map.Entry<String,Boolean> app:scopes(previous).entrySet())if(retained.test(app.getKey()))existing=RoutineAccessRules.merge(existing,Collections.singletonList(app.getKey()),app.getValue());
   }else next.put(previous);
  }
  next.put(record(endpoint,RoutineAccessRules.merge(existing,apps,vision)));while(next.length()>RoutineAccessRules.MAX_RECORDS)next.remove(0);return next;
 }
 static LinkedHashMap<String,Boolean> recipientScopes(JSONArray records,String endpoint)throws Exception{
  LinkedHashMap<String,Boolean> combined=new LinkedHashMap<>();
  for(int i=0;i<records.length();i++){JSONObject record=records.getJSONObject(i);if(!known(record)||!endpoint.equals(record.optString("endpoint")))continue;for(Map.Entry<String,Boolean> app:scopes(record).entrySet())combined=RoutineAccessRules.merge(combined,Collections.singletonList(app.getKey()),app.getValue());}
  return combined;
 }
 void approve(Config config,Collection<String> apps)throws Exception{
  config.validate();if(apps==null||apps.isEmpty())throw new Exception("没有可保存的应用范围");AppAccess access=new AppAccess(context);for(String pkg:apps)access.require(pkg);
  JSONArray next=approvalRecords(records(),config.endpoint,apps,config.vision,access::allowed);
  new Vault(context).put(KEY,new JSONObject().put("schema",2).put("grants",next).toString());
 }
 boolean allows(Config config,Collection<String> apps){try{
  if(apps==null||apps.isEmpty())return false;AppAccess access=new AppAccess(context);for(String pkg:apps)access.require(pkg);
  return RoutineAccessRules.allows(recipientScopes(records(),config.endpoint),apps,config.vision);
 }catch(Exception ignored){return false;}}
}
