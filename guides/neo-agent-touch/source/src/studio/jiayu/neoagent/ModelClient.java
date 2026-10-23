package studio.jiayu.neoagent;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class ModelClient {
 volatile HttpURLConnection active;volatile boolean cancelled;
 void cancel(){cancelled=true;HttpURLConnection c=active;if(c!=null)c.disconnect();}
 String request(Config c,String system,String text,String image)throws Exception{return request(c,system,text,image,null);}
 String request(Config c,String system,String text,String image,String annotated)throws Exception{return request(c,system,text,image,annotated,1536);}
 String request(Config c,String system,String text,String image,String annotated,int maxOutput)throws Exception{
  int outputTokens=Math.max(512,Math.min(8192,maxOutput));
  if(cancelled)throw new Exception("请求已停止");c.validate();String base=c.endpoint.replaceAll("/+$","");JSONObject body=new JSONObject();String url;
  if(c.protocol.equals("Anthropic")){
   url=base.endsWith("/messages")?base:base+"/messages";
   JSONArray blocks=new JSONArray().put(new JSONObject().put("type","text").put("text",text));
   if(image!=null)blocks.put(new JSONObject().put("type","image").put("source",new JSONObject().put("type","base64").put("media_type","image/jpeg").put("data",image)));
   if(annotated!=null)blocks.put(new JSONObject().put("type","image").put("source",new JSONObject().put("type","base64").put("media_type","image/jpeg").put("data",annotated)));
   body.put("model",c.model).put("max_tokens",outputTokens).put("system",system).put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",blocks)));
  }else if(c.protocol.equals("Gemini")){
   url=base.contains(":generateContent")?base:base+"/models/"+URLEncoder.encode(c.model,"UTF-8")+":generateContent";
   JSONArray parts=new JSONArray().put(new JSONObject().put("text",text));if(image!=null)parts.put(new JSONObject().put("inlineData",new JSONObject().put("mimeType","image/jpeg").put("data",image)));
   if(annotated!=null)parts.put(new JSONObject().put("inlineData",new JSONObject().put("mimeType","image/jpeg").put("data",annotated)));
   body.put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",system)))).put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",parts))).put("generationConfig",new JSONObject().put("maxOutputTokens",outputTokens));
  }else{
   url=base.endsWith("/chat/completions")?base:base+"/chat/completions";
   Object content=text;if(image!=null)content=new JSONArray().put(new JSONObject().put("type","text").put("text",text)).put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url","data:image/jpeg;base64,"+image)));
   if(annotated!=null){if(!(content instanceof JSONArray))throw new Exception("标注画面缺少原始画面");((JSONArray)content).put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url","data:image/jpeg;base64,"+annotated)));}
   body.put("model",c.model).put("max_tokens",outputTokens).put("stream",false).put("messages",new JSONArray().put(new JSONObject().put("role","system").put("content",system)).put(new JSONObject().put("role","user").put("content",content)));
   if(c.endpoint.contains("api.deepseek.com"))body.put("thinking",new JSONObject().put("type","disabled"));
  }
  if(cancelled)throw new Exception("请求已停止");HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();active=h;
  try{if(cancelled)throw new Exception("请求已停止");h.setInstanceFollowRedirects(false);h.setConnectTimeout(20000);h.setReadTimeout(60000);h.setRequestMethod("POST");h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json");
   if(c.protocol.equals("Anthropic")){h.setRequestProperty("x-api-key",c.key);h.setRequestProperty("anthropic-version","2023-06-01");}else if(c.protocol.equals("Gemini"))h.setRequestProperty("x-goog-api-key",c.key);else h.setRequestProperty("Authorization","Bearer "+c.key);
   byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);h.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=h.getOutputStream()){out.write(bytes);}int code=h.getResponseCode();
   if(code!=200)throw new Exception("模型请求 HTTP "+code+"；请核对接口、模型、余额和图像支持。服务器原文未记录。");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=h.getInputStream()){byte[]b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2_000_000)throw new Exception("模型响应过大");}}
   JSONObject o=new JSONObject(out.toString("UTF-8"));
   if(c.protocol.equals("Anthropic")){StringBuilder s=new StringBuilder();JSONArray a=o.getJSONArray("content");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("type").equals("text"))s.append(a.getJSONObject(i).getString("text"));return s.toString();}
   if(c.protocol.equals("Gemini")){JSONArray a=o.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).optBoolean("thought"))s.append(a.getJSONObject(i).optString("text"));return s.toString();}
   return o.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
  }finally{active=null;h.disconnect();}
 }
}
