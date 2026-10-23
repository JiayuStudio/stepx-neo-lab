package studio.jiayu.neoagent;
import android.app.Activity;import org.json.*;import java.io.*;import java.util.*;
/** Synthetic Android JSON adapter checks only. The old live-vault exercise remains archived in v19. */
final class RoutineContracts {
 static final class Checks {
  int count;
  void yes(boolean value,String why)throws Exception{if(!value)throw new Exception(why);count++;}
  void rejects(JSONObject record,String why)throws Exception{boolean rejected=false;try{RoutineAccess.scopes(record);}catch(Exception expected){rejected=true;}yes(rejected,why);}
 }
 static List<String> apps(String... values){return Arrays.asList(values);}
 static JSONObject legacy(String endpoint,Collection<String> apps,boolean vision)throws Exception{return new JSONObject().put("endpoint",endpoint).put("visual_scope",RoutineAccessRules.LEGACY_SCOPE).put("apps",new JSONArray(apps)).put("vision",vision);}
 static boolean allows(JSONArray records,String endpoint,Collection<String> apps,boolean vision)throws Exception{return RoutineAccessRules.allows(RoutineAccess.recipientScopes(records,endpoint),apps,vision);}
 /** No Context, Vault, AppAccess, network or user state is accessed by this entry point. */
 static JSONObject jsonContracts(){JSONObject result=new JSONObject();Checks check=new Checks();try{
  String recipient="https://example.invalid/v1",other="https://other.invalid/v1",path="https://example.invalid/other";
  List<String> wps=apps("fixture.wps"),chat=apps("fixture.wechat"),disk=apps("fixture.disk");
  JSONArray records=new JSONArray();check.yes(!allows(records,recipient,wps,false),"missing consent accepted");
  records=RoutineAccess.approvalRecords(records,recipient,wps,true,pkg->true);String before=records.toString();
  records=RoutineAccess.approvalRecords(records,recipient,chat,false,pkg->true);
  check.yes(!allows(records,recipient,chat,true),"new chat app inherited another app visual scope");check.yes(allows(records,recipient,chat,false),"new chat scope lost");check.yes(allows(records,recipient,wps,true),"existing same-app visual scope lost");
  check.yes(!allows(records,recipient,apps("fixture.wps","fixture.wechat"),true),"mixed queue elevated chat app");check.yes(allows(records,recipient,apps("fixture.wps","fixture.wechat"),false),"mixed queue chat scope lost");
  check.yes(!allows(records,other,wps,true)&&!allows(records,path,wps,true),"recipient or endpoint path ignored");check.yes(!allows(records,recipient,apps(),false)&&!allows(records,recipient,apps("fixture.unknown"),false),"empty or unlisted app accepted");
  JSONObject current=records.getJSONObject(0);check.yes(records.length()==1&&RoutineAccessRules.SCOPE.equals(current.getString("visual_scope"))&&!current.has("vision")&&!current.has("apps"),"new record retains global vision schema");
  JSONArray prior=RoutineAccess.decodeRecords(new JSONObject().put("grants",new JSONArray(before)).toString());check.yes(!allows(prior,recipient,chat,false),"approval mutated previous records");
  records=RoutineAccess.approvalRecords(records,recipient,chat,true,pkg->true);records=RoutineAccess.approvalRecords(records,recipient,chat,false,pkg->true);check.yes(allows(records,recipient,chat,true),"later chat approval downgraded explicit same-app vision");
  records=RoutineAccess.approvalRecords(records,other,chat,false,pkg->true);check.yes(!allows(records,other,chat,true)&&allows(records,recipient,chat,true),"different recipient scopes merged");
  records=RoutineAccess.decodeRecords(new JSONObject().put("schema",2).put("grants",records).toString());check.yes(allows(records,recipient,wps,true)&&!allows(records,other,chat,true),"v2 JSON roundtrip expanded scope");
  for(boolean vision:new boolean[]{false,true}){
   JSONObject old=legacy(recipient,apps("fixture.wps","fixture.wechat"),vision);String oldText=old.toString();JSONArray single=RoutineAccess.decodeRecords(oldText);
   check.yes(allows(single,recipient,wps,true)==vision&&allows(single,recipient,chat,false),"old single-record explicit scope lost");check.yes(oldText.equals(single.getJSONObject(0).toString()),"read-only legacy import changed record");
   JSONArray migrated=RoutineAccess.approvalRecords(single,recipient,disk,false,pkg->true);
   check.yes(allows(migrated,recipient,wps,true)==vision&&!allows(migrated,recipient,disk,true)&&allows(migrated,recipient,disk,false),"migration changed old scope or elevated new app");check.yes(RoutineAccessRules.SCOPE.equals(migrated.getJSONObject(0).getString("visual_scope")),"legacy approval not migrated");
  }
  records=RoutineAccess.decodeRecords(new JSONObject().put("grants",new JSONArray().put(legacy(recipient,wps,true)).put(legacy(other,chat,false))).toString());records=RoutineAccess.approvalRecords(records,recipient,disk,false,pkg->true);
  check.yes(records.length()==2&&allows(records,recipient,wps,true)&&!allows(records,other,chat,true)&&allows(records,other,chat,false),"legacy wrapper recipient isolation lost");
  JSONObject malformed=RoutineAccess.record(recipient,RoutineAccessRules.legacy(wps,false));malformed.getJSONArray("app_scopes").put(new JSONObject().put("package","fixture.wps").put("vision",true));check.rejects(malformed,"duplicate app scope accepted");
  malformed=RoutineAccess.record(recipient,RoutineAccessRules.legacy(wps,false));malformed.getJSONArray("app_scopes").getJSONObject(0).put("vision","true");check.rejects(malformed,"string visual scope accepted");
  malformed=RoutineAccess.record(recipient,RoutineAccessRules.legacy(wps,false));malformed.getJSONArray("app_scopes").getJSONObject(0).remove("vision");check.rejects(malformed,"missing visual scope accepted");
  records=new JSONArray();for(int i=0;i<=RoutineAccessRules.MAX_RECORDS;i++)records=RoutineAccess.approvalRecords(records,"https://recipient"+i+".invalid/v1",wps,true,pkg->true);
  check.yes(records.length()==RoutineAccessRules.MAX_RECORDS,"recipient history limit changed");check.yes(!allows(records,"https://recipient0.invalid/v1",wps,true)&&allows(records,"https://recipient1.invalid/v1",wps,true),"oldest recipient eviction wrong");
  records=RoutineAccess.approvalRecords(records,"https://recipient1.invalid/v1",wps,false,pkg->true);check.yes(records.length()==RoutineAccessRules.MAX_RECORDS&&allows(records,"https://recipient1.invalid/v1",wps,true),"recipient refresh duplicated or downgraded record");
  records=RoutineAccess.approvalRecords(records,"https://recipient17.invalid/v1",wps,true,pkg->true);check.yes(allows(records,"https://recipient1.invalid/v1",wps,true)&&!allows(records,"https://recipient2.invalid/v1",wps,true),"recipient recency not retained");
  records=new JSONArray().put(legacy(recipient,apps("fixture.wps","fixture.wechat"),true));records=RoutineAccess.approvalRecords(records,recipient,disk,false,pkg->!pkg.equals("fixture.wechat"));check.yes(!allows(records,recipient,chat,true)&&allows(records,recipient,wps,true),"unselected app scope retained during merge");
  check.yes(!records.toString().contains("allowOrderApproval")&&!records.toString().contains("apiKey"),"non-routine authority serialized");
  result.put("success",true).put("passed",check.count);
 }catch(Exception failed){try{result.put("success",false).put("passed",check.count).put("failure",failed.getMessage());}catch(Exception ignored){}}try{result.put("uses_real_vault",false).put("storage_writes",0).put("network_calls",0).put("gestures",0);}catch(Exception ignored){}return result;}
 /** Historical Activity entry now runs only synthetic JSON checks and writes its private result. */
 static void run(Activity a){JSONObject out=jsonContracts();try(FileOutputStream f=a.openFileOutput("routine-contracts.json",0)){f.write(out.toString(2).getBytes("UTF-8"));}catch(Exception ignored){}a.finish();}
}
