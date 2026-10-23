package studio.jiayu.neoagent;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Debug metadata only. No candidate text, exception messages, unknown fields or editor data. */
final class ActionFailureAudit {
 static final String FILE="action-failure-audit.json";
 static synchronized JSONObject read(Context context){
  try(FileInputStream input=context.openFileInput(FILE);ByteArrayOutputStream output=new ByteArrayOutputStream()){
   byte[] block=new byte[2048];int n;while((n=input.read(block))!=-1){output.write(block,0,n);if(output.size()>16384)throw new Exception("audit_size");}
   JSONObject raw=new JSONObject(output.toString("UTF-8"));JSONArray stored=raw.getJSONArray("items"),items=new JSONArray();
   if(raw.getInt("version")!=1||stored.length()>16)throw new Exception("audit_shape");
   for(int i=0;i<stored.length();i++){JSONObject entry=stored.getJSONObject(i);ActionEnvelopeRules.Invalid known=new ActionEnvelopeRules.Invalid(entry.getString("code"),entry.getString("field"));
    String bucket=entry.getString("length_bucket");if(!bucket.equals(ActionEnvelopeRules.lengthBucket(bucketLength(bucket))))throw new Exception("audit_bucket");
    items.put(new JSONObject().put("sequence",entry.getLong("sequence")).put("at",entry.getLong("at")).put("code",known.code).put("field",known.field).put("length_bucket",bucket));}
   return new JSONObject().put("version",1).put("sequence",Math.max(0,raw.getLong("sequence"))).put("items",items);
  }catch(Exception unavailable){return empty();}
 }
 private static int bucketLength(String bucket){for(int length:new int[]{0,1,128,512,2048,8192,16384,16385})if(ActionEnvelopeRules.lengthBucket(length).equals(bucket))return length;return -1;}
 private static JSONObject empty(){try{return new JSONObject().put("version",1).put("sequence",0).put("items",new JSONArray());}catch(Exception impossible){return new JSONObject();}}
 static synchronized void record(Context context,Throwable failure,int rawLength){
  if((context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  try{JSONObject audit=read(context);long sequence=audit.getLong("sequence");if(sequence==Long.MAX_VALUE){audit=empty();sequence=0;}sequence++;
   ActionEnvelopeRules.Invalid kind=ActionEnvelopeRules.classify(failure);JSONArray items=audit.getJSONArray("items");
   items.put(new JSONObject().put("sequence",sequence).put("at",System.currentTimeMillis()).put("code",kind.code).put("field",kind.field).put("length_bucket",ActionEnvelopeRules.lengthBucket(rawLength)));
   while(items.length()>16)items.remove(0);audit.put("sequence",sequence);
   try(FileOutputStream output=context.openFileOutput(FILE,Context.MODE_PRIVATE)){output.write(audit.toString().getBytes("UTF-8"));}
  }catch(Exception unavailable){/* Diagnostics cannot grant or prevent actions. */}
 }
}
