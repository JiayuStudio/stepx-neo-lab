package studio.jiayu.neoagent;
import android.content.Context;import android.graphics.*;import org.json.*;import java.io.*;
final class VisionContracts {
 static void run(Context c){JSONObject report=new JSONObject();Bitmap b=null;int passed=0;try{
 JSONArray nodes=new JSONArray().put(new JSONObject().put("id",1).put("text","背景正文")).put(new JSONObject().put("id",2).put("clickable",true).put("description","搜索").put("bounds",new JSONArray().put(100).put(100).put(300).put(180))).put(new JSONObject().put("id",3).put("clickable",true).put("bounds",new JSONArray().put(400).put(100).put(600).put(180)));
 JSONArray controls=VisionGrounding.controls(nodes);if(controls.length()!=2)throw new Exception("Plain tree text leaked to control map");passed++;
 if(!controls.getJSONObject(0).getString("purpose").equals("搜索")||!controls.getJSONObject(0).getString("purpose_source").equals("android_accessibility"))throw new Exception("Native purpose lost");passed++;
 if(!controls.getJSONObject(1).getString("purpose_source").equals("unknown"))throw new Exception("Invented purpose");passed++;
 if(!VisionGrounding.role("android.widget.Switch",false,false).equals("开关")||!VisionGrounding.role("custom",true,false).equals("输入框"))throw new Exception("Native role lost");passed++;
 b=Bitmap.createBitmap(1000,1000,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);VisionGrounding.annotate(b,controls);if(!controls.getJSONObject(0).optBoolean("image_annotated")||b.getPixel(100,100)==Color.WHITE)throw new Exception("Missing visual labels");passed++;
 if(nodes.getJSONObject(2).has("purpose"))throw new Exception("Source mutated");passed++;
 ControlService original=ControlService.instance;ControlService stale=new ControlService();try{stale.onAccessibilityEvent(null);if(ControlService.instance!=original)throw new Exception("Disconnected service resurrected by late event");passed++;}finally{stale.runtimeHealth.close();stale.worker.shutdownNow();stale.workerScreenshot.shutdownNow();}
 report.put("success",true);
 }catch(Exception e){try{report.put("success",false).put("failure",e.getMessage());}catch(Exception ignored){}}
 finally{if(b!=null)b.recycle();try(FileOutputStream out=c.openFileOutput("vision-contracts.json",0)){out.write(report.put("passed",passed).put("sampledAt",System.currentTimeMillis()).put("network_calls",0).toString(2).getBytes("UTF-8"));}catch(Exception ignored){}}}
}
