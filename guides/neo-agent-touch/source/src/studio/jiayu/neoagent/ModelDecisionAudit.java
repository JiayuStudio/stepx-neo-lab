package studio.jiayu.neoagent;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Base64;
import org.json.JSONObject;
import java.io.FileOutputStream;
import java.security.MessageDigest;

/** One app-private decision sample for debug builds. Never logs pixels or credentials. */
final class ModelDecisionAudit {
 static void record(Context context,ControlService.Observation observation,String reply){
  if((context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  try{
   byte[] original=Base64.decode(observation.image,Base64.DEFAULT);
   byte[] annotated=Base64.decode(observation.annotatedImage,Base64.DEFAULT);
   if(original.length>2_000_000||annotated.length>2_000_000)return;
   write(context,"qa-decision-original.jpg",original);
   write(context,"qa-decision-annotated.jpg",annotated);
   JSONObject sample=new JSONObject().put("sampledAt",observation.sampledAt)
    .put("observation",observation.json).put("scene",observation.signature)
    .put("reply",PrivateText.redact(reply.substring(0,Math.min(reply.length(),16000))))
    .put("original_sha256",digest(original)).put("annotated_sha256",digest(annotated));
   write(context,"qa-last-decision.json",sample.toString().getBytes("UTF-8"));
  }catch(Exception unavailable){/* Diagnostics never change task behavior. */}
 }
 private static void write(Context context,String name,byte[] bytes)throws Exception{
  try(FileOutputStream out=context.openFileOutput(name,Context.MODE_PRIVATE)){out.write(bytes);}
 }
 static void recordAsk(Context context,ControlService.Observation observation,String reply){
  if((context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  try{
   write(context,"qa-last-ask.json",new JSONObject().put("sampledAt",observation.sampledAt).put("observation",observation.json).put("reply",PrivateText.redact(reply)).toString().getBytes("UTF-8"));
   write(context,"qa-ask-original.jpg",Base64.decode(observation.image,Base64.DEFAULT));
  }catch(Exception unavailable){/* Keep the actual stop reason without affecting control. */}
 }
 private static String digest(byte[] bytes)throws Exception{
  StringBuilder text=new StringBuilder();
  for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  return text.toString();
 }
}
