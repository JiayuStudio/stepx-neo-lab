package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real Android JSON + production decoders. No RPC invocation, executor, permissions or network. */
final class ActionEnvelopeContracts {
 interface Job {void run()throws Exception;}
 private static final String MARKER="FIXTURE_MARKER_7";
 final Activity activity;
 final JSONObject report=new JSONObject();
 final JSONArray passed=new JSONArray(),rejections=new JSONArray();
 RuntimeBefore before;
 int checks;
 boolean parsingTested,mcpDecodeTested;
 boolean auditTested,auditRestored=true;
 ActionEnvelopeContracts(Activity activity){this.activity=activity;}
 static void start(Activity activity){new Thread(()->new ActionEnvelopeContracts(activity).run(),"action-envelope-native-contracts").start();}
 void yes(boolean value,String label)throws Exception{if(!value)throw new IllegalStateException(label);checks++;passed.put(label);}
 String call(String tool,JSONObject args)throws Exception{return new JSONObject().put("jsonrpc","2.0").put("id",1).put("method","tools/call").put("params",new JSONObject().put("name",tool).put("arguments",args)).toString();}
 void rejected(String label,String expectedCode,Job operation)throws Exception{
  Exception failure=null;try{operation.run();}catch(Exception expected){failure=expected;}
  yes(failure!=null,label+": rejected before execution");
  ActionEnvelopeRules.Invalid detail=ActionEnvelopeRules.classify(failure);
  rejections.put(new JSONObject().put("case",label).put("code",detail.code).put("field",detail.field)
   .put("typed_at_source",failure instanceof ActionEnvelopeRules.Invalid));
  if(expectedCode!=null)yes(expectedCode.equals(detail.code),label+": specific structural category");
  yes(!detail.code.contains(MARKER)&&!detail.field.contains(MARKER)&&!detail.safeFeedback().contains(MARKER)
   &&!detail.getMessage().contains(MARKER),label+": diagnostic never reflects payload");
 }
 void run(){Throwable failure=null;boolean unchanged=false;
  try{
   yes((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0,"debug fixture only");
   before=new RuntimeBefore();yes(before.idle,"fixture does not interrupt an active runtime");
   parsingTested=true;
   rejected("incomplete Android JSON","json_syntax",()->Action.parse("{\"action\":"));
   rejected("valid array is not an action object","json_structure",()->Action.parse("[]"));
   rejected("valid scalar is not an action object","json_structure",()->Action.parse(JSONObject.quote(MARKER)));
   rejected("second JSON action cannot trail first action","json_syntax",()->Action.parse("{\"action\":\"wait\"}{\"action\":\"back\"}"));
   rejected("prose cannot trail action object","json_syntax",()->Action.parse("{\"action\":\"wait\"} "+MARKER));
   rejected("second JSON after closing fence is not discarded","json_syntax",()->Action.parse("```json\n{\"action\":\"wait\"}\n```\n{\"action\":\"back\"}"));
   rejected("prose after closing fence is not discarded","json_syntax",()->DeviceMcp.decode("```json\n{\"action\":\"wait\"}\n```\n"+MARKER));
   rejected("missing action","action_missing",()->new Action(new JSONObject()));
   rejected("numeric action type","action_type",()->new Action(new JSONObject().put("action",7)));
   rejected("unknown action","action_unknown",()->new Action(new JSONObject().put("action",MARKER)));
   rejected("compound action is not one action","action_unknown",()->Action.parse("{\"action\":\"tap long_press\",\"id\":1}"));
   mcpDecodeTested=true;
   rejected("decode incomplete JSON","json_syntax",()->DeviceMcp.decode("{\"action\":"));
   rejected("decode array root","json_structure",()->DeviceMcp.decode("[]"));

   for(Object id:new Object[]{true,JSONObject.NULL,new JSONObject().put("value",MARKER),new JSONArray().put(6),"#6",MARKER,"6.0",6.5})
    rejected("wrong node id type "+ActionEnvelopeRules.kind(id),"id_type",()->new Action(new JSONObject().put("action","tap").put("id",id)));
   for(int id:new int[]{0,-1,10001})rejected("out of range node "+id,"id_range",()->new Action(new JSONObject().put("action","tap").put("id",id)));
   yes(Action.parse("{\"action\":\"tap\",\"id\":\"6\"}").data.getInt("id")==6,"legacy numeric string id normalized without relaxed range");
   yes(DeviceMcp.decode("{\"action\":\"tap\",\"id\":\"6\"}").data.getInt("id")==6,"legacy decoder keeps numeric string compatibility");
   yes(DeviceMcp.decode(call("device.tap",new JSONObject().put("id",6))).data.getInt("id")==6,"MCP numeric integer id accepted structurally");
   for(Object id:new Object[]{"6",6.5,true,JSONObject.NULL,new JSONObject()})
    rejected("MCP strict id "+ActionEnvelopeRules.kind(id),"argument_type",()->DeviceMcp.decode(call("device.tap",new JSONObject().put("id",id))));
   rejected("MCP fractional coordinate","argument_type",()->DeviceMcp.decode(call("device.tap",new JSONObject().put("x",6.5).put("y",7))));
   rejected("MCP string coordinate","argument_type",()->DeviceMcp.decode(call("device.tap",new JSONObject().put("x","6").put("y",7))));
   rejected("MCP unknown parameter","argument_unknown",()->DeviceMcp.decode(call("device.type",new JSONObject().put("text",MARKER).put(MARKER,MARKER))));
   rejected("MCP unsupported tool","tool_unknown",()->DeviceMcp.decode(call(MARKER,new JSONObject())));

   rejected("missing type text","field_missing",()->new Action(new JSONObject().put("action","type")));
   rejected("object type text","field_type",()->new Action(new JSONObject().put("action","type").put("text",new JSONObject().put("value",MARKER))));
   rejected("ordinary type maximum","field_length",()->new Action(new JSONObject().put("action","type").put("text",repeat(501))));
   rejected("missing document text","field_missing",()->new Action(new JSONObject().put("action","document_write")));
   rejected("boolean document text","field_type",()->new Action(new JSONObject().put("action","document_write").put("text",true)));
   rejected("empty document text","field_length",()->new Action(new JSONObject().put("action","document_write").put("text","")));
   rejected("document maximum","field_length",()->new Action(new JSONObject().put("action","document_write").put("text",repeat(8001))));
   yes(new Action(new JSONObject().put("action","document_write").put("text",repeat(8000))).name.equals("document_write"),"document maximum 8000 remains accepted structurally");
   rejected("missing replacement","field_missing",()->new Action(new JSONObject().put("action","document_replace").put("find",MARKER)));
   rejected("null replacement","field_type",()->new Action(new JSONObject().put("action","document_replace").put("find",MARKER).put("replacement",JSONObject.NULL)));
   yes(new Action(new JSONObject().put("action","document_replace").put("find",MARKER).put("replacement","")).name.equals("document_replace"),"empty replacement remains structurally valid");
   rejected("MCP boolean document text","argument_type",()->DeviceMcp.decode(call("document.write",new JSONObject().put("text",true))));
   rejected("wrong memory type","memory_type",()->new Action(new JSONObject().put("action","wait").put("memory",new JSONArray().put(MARKER))));
   rejected("memory maximum","memory_length",()->new Action(new JSONObject().put("action","wait").put("memory",repeat(4097))));
   rejected("coordinate outside range","coordinate_range",()->Action.parse("{\"action\":\"tap\",\"x\":1001,\"y\":1}"));
   rejected("coordinate object type","coordinate_type",()->new Action(new JSONObject().put("action","tap").put("x",new JSONObject().put("value",MARKER)).put("y",1)));
   rejected("missing financial node","id_required",()->Action.parse("{\"action\":\"purchase_submit\",\"x\":500,\"y\":900}"));
   rejected("wrong scroll direction","direction_invalid",()->Action.parse("{\"action\":\"scroll\",\"direction\":\"sideways\"}"));
   yes(Action.parse("{\"action\":\"submit_search\"}").name.equals("submit_search"),"native editor submit may omit node id");
   yes(DeviceMcp.decode(call("device.submit_search",new JSONObject())).name.equals("submit_search"),"MCP submit omission remains valid before native checks");
   yes("documents".equals(DeviceMcp.decode(call("skills.read",new JSONObject().put("id","documents"))).data.getString("id")),"skill string id stays distinct from node id");
   ActionEnvelopeRules.Invalid scrubbed=new ActionEnvelopeRules.Invalid(MARKER,MARKER);
   yes("envelope_invalid".equals(scrubbed.code)&&"other".equals(scrubbed.field)&&!scrubbed.getMessage().contains(MARKER),"unknown diagnostic identifiers are never reflected");
   ActionEnvelopeRules.Invalid wrapped=ActionEnvelopeRules.classify(new Exception(MARKER,new ActionEnvelopeRules.Invalid("id_type","id")));
   yes("id_type".equals(wrapped.code)&&!wrapped.getMessage().contains(MARKER),"wrapped typed feedback has no exception raw message");
   yes("null".equals(ActionEnvelopeRules.kind(JSONObject.NULL)),"Android JSON null is a fixed diagnostic label");
   audit();
   report.put("success",true);
  }catch(Throwable error){failure=error;}
  finally{
   try{unchanged=before!=null&&before.matches();}catch(Throwable error){if(failure==null)failure=error;}
   complete(failure,unchanged);
  }
 }
 static String repeat(int count){char[] chars=new char[count];java.util.Arrays.fill(chars,'x');return new String(chars);}
 void audit()throws Exception{
  // Keep the pre-existing diagnostic file byte-for-byte. This never reads credentials or chats.
  synchronized(ActionFailureAudit.class){
   File file=new File(activity.getFilesDir(),"action-failure-audit.json");
   boolean existed=file.exists();byte[] original=existed?bytes(file):null;
   auditRestored=false;
   try{
    long baseline=ActionFailureAudit.read(activity).optLong("sequence",0);
    for(int i=0;i<20;i++){
     Throwable error=i%3==0?new Exception(MARKER+"-synthetic-secretlike"):
      new Exception(MARKER,new ActionEnvelopeRules.Invalid(i%2==0?"id_type":"field_type",i%2==0?"id":"text"));
     ActionFailureAudit.record(activity,error,i==19?17000:12+i);
    }
    JSONObject value=ActionFailureAudit.read(activity);
    yes(value.length()==3&&value.has("version")&&value.has("sequence")&&value.has("items"),"audit root has only version sequence items");
    yes(value.getInt("version")==1&&value.getLong("sequence")==baseline+20,"audit sequence increases for every synthetic failure");
    JSONArray items=value.getJSONArray("items");yes(items.length()==16,"audit keeps exactly last sixteen entries");
    Set<String> keys=new HashSet<>(Arrays.asList("sequence","at","code","field","length_bucket"));
    Set<String> codes=new HashSet<>(Arrays.asList("id_type","field_type","envelope_invalid"));
    long previous=value.getLong("sequence")-16;
    for(int i=0;i<items.length();i++){
     JSONObject item=items.getJSONObject(i);boolean allowed=item.length()==5;
     Iterator<String> names=item.keys();while(names.hasNext())if(!keys.contains(names.next()))allowed=false;
     yes(allowed,"audit item contains only permitted structural metadata");
     yes(item.get("sequence") instanceof Number&&item.get("at") instanceof Number
      &&item.get("code") instanceof String&&item.get("field") instanceof String&&item.get("length_bucket") instanceof String,
      "audit metadata uses expected native JSON types");
     yes(item.getLong("sequence")==previous+1&&item.getLong("at")>0,"retained audit sequence is consecutive with a timestamp");previous=item.getLong("sequence");
     yes(codes.contains(item.getString("code"))&&new HashSet<>(Arrays.asList("","id","text")).contains(item.getString("field")),"audit stores only safe code and field enums");
     yes(item.getString("length_bucket").equals(i==items.length()-1?">16384":"1-64"),"audit stores bucket instead of raw response length or content");
    }
    String serialized=new String(bytes(file),"UTF-8");
    yes(serialized.length()<65536&&!serialized.contains(MARKER),"bounded audit file never contains throwable message or synthetic payload");
    auditTested=true;
   }finally{
    if(existed){try(FileOutputStream output=new FileOutputStream(file)){output.write(original);output.getFD().sync();}}
    else if(file.exists()&&!file.delete())throw new IllegalStateException("audit fixture file cleanup failed");
    auditRestored=existed==file.exists()&&(!existed||Arrays.equals(original,bytes(file)));
    yes(auditRestored,"original audit file presence and bytes restored");
   }
  }
 }
 static byte[] bytes(File file)throws Exception{
  if(file.length()>131072)throw new IllegalStateException("audit fixture input exceeds bound");
  try(FileInputStream input=new FileInputStream(file);ByteArrayOutputStream output=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){output.write(buffer,0,count);if(output.size()>131072)throw new IllegalStateException("audit fixture input exceeds bound");}return output.toByteArray();
  }
 }
 void complete(Throwable failure,boolean unchanged){
  try{
   if(failure!=null)report.put("failure_type",failure.getClass().getSimpleName()).put("failure_code",ActionEnvelopeRules.classify(failure).code).put("last_passed_check",passed.optString(passed.length()-1,"preconditions"));
   report.put("success",failure==null&&unchanged&&auditRestored&&report.optBoolean("success")).put("checks",checks).put("passed",passed).put("rejections",rejections)
    .put("sampledAt",System.currentTimeMillis()).put("cleanup_verified",unchanged&&auditRestored).put("runtime_unchanged",unchanged)
    .put("android_json_parser_tested",parsingTested).put("production_mcp_decode_tested",mcpDecodeTested).put("real_rpc_invoked",false)
    .put("business_worker_started",false).put("gestures",0).put("network_calls",0).put("permission_changes",0)
    .put("production_preferences_modified",false).put("raw_response_recorded",false).put("failure_audit_tested",auditTested)
    .put("failure_audit_original_restored",auditRestored).put("failure_audit_capacity",16)
    .put("fixture_source","fixed public structural payloads; no replay of past private model responses");
   try(OutputStream out=activity.openFileOutput("action-envelope-contracts.json",Activity.MODE_PRIVATE)){out.write(report.toString(2).getBytes("UTF-8"));}
  }catch(Exception ignored){}
  finally{activity.runOnUiThread(activity::finish);}
 }
 static final class RuntimeBefore {
  final ControlService service=ControlService.instance;
  final boolean idle,running,paused,pending;
  final long epoch;
  final String run;
  final int actions,calls,shots;
  RuntimeBefore(){if(service==null){idle=true;running=paused=pending=false;epoch=0;run="";actions=calls=shots=0;}else synchronized(service){idle=!service.busy()&&!service.running&&!service.paused;running=service.running;paused=service.paused;pending=service.pendingDispatch;epoch=service.generation;run=service.runId;actions=service.actions;calls=service.modelCalls;shots=service.screenshots;}}
  boolean matches(){if(ControlService.instance!=service)return false;if(service==null)return true;synchronized(service){return service.running==running&&service.paused==paused&&service.pendingDispatch==pending&&service.generation==epoch&&run.equals(service.runId)&&service.actions==actions&&service.modelCalls==calls&&service.screenshots==shots;}}
 }
}
