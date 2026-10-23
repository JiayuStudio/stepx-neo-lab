package studio.jiayu.neoagent;
import org.json.*;import java.net.*;import java.io.*;import java.util.*;import android.content.Context;
/** Runs in debug APK on Android against an in-process fake transport; never sends keys/network. */
final class ModelContracts {
 static Fake last;static int passed=0;static ArrayList<String> cases=new ArrayList<>();
 static class Fake extends HttpURLConnection {
  ByteArrayOutputStream body=new ByteArrayOutputStream();HashMap<String,String> headers=new HashMap<>();
  Fake(URL u){super(u);}public void disconnect(){}public boolean usingProxy(){return false;}public void connect(){}public OutputStream getOutputStream(){return body;}public void setRequestProperty(String k,String v){headers.put(k,v);}public int getResponseCode(){return 200;}
  public InputStream getInputStream(){String json=url.toString().contains("messages")?"{\"content\":[{\"type\":\"text\",\"text\":\"OK\"}]}":url.toString().contains("generateContent")?"{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}":"{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}";return new ByteArrayInputStream(json.getBytes());}
 }
 static void yes(boolean b,String name)throws Exception{if(!b)throw new Exception("FAIL "+name);passed++;cases.add(name);}
 static void bad(String json,String name)throws Exception{boolean rejected=false;try{Action.parse(json);}catch(Exception e){rejected=true;}yes(rejected,name);}
 static void run(Context ctx){try{
  URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){last=new Fake(u);return last;}}:null);
  for(String protocol:new String[]{"OpenAI","Anthropic","Gemini"}){
   Config c=new Config();c.endpoint="https://example.invalid/v1";c.model="test-model";c.key="TEST_TOKEN";c.protocol=protocol;ModelClient client=new ModelClient();
   yes(client.request(c,"test system","test user","aGVsbG8=","YW5ub3RhdGVk").equals("OK"),protocol+" response parse");JSONObject b=new JSONObject(last.body.toString("UTF-8"));String payload=b.toString();
   yes(payload.contains("aGVsbG8=")&&payload.contains("YW5ub3RhdGVk"),protocol+" original and annotation images");yes(!payload.contains("TEST_TOKEN"),protocol+" key only in headers");yes(!last.getInstanceFollowRedirects(),protocol+" redirects disabled");
   if(protocol.equals("OpenAI")){yes(last.headers.get("Authorization").equals("Bearer TEST_TOKEN"),"OpenAI auth");yes(b.getJSONArray("messages").getJSONObject(1).getJSONArray("content").getJSONObject(1).getString("type").equals("image_url"),"OpenAI image block");yes(last.getURL().toString().equals("https://example.invalid/v1/chat/completions"),"OpenAI base URL");}
   if(protocol.equals("Anthropic")){yes(last.headers.get("x-api-key").equals("TEST_TOKEN"),"Anthropic auth");yes(b.getJSONArray("messages").getJSONObject(0).getJSONArray("content").getJSONObject(1).getJSONObject("source").getString("media_type").equals("image/jpeg"),"Anthropic image block");yes(b.getString("system").equals("test system"),"Anthropic system");}
   if(protocol.equals("Gemini")){yes(last.headers.get("x-goog-api-key").equals("TEST_TOKEN"),"Gemini header auth");yes(!last.getURL().toString().contains("TEST_TOKEN"),"Gemini key absent in URL");yes(b.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(1).has("inlineData"),"Gemini image block");}
   client.request(c,"test","test",null);yes(!last.body.toString("UTF-8").contains("image"),protocol+" text mode omits images");
  }
  Config c=new Config();c.endpoint="https://example.invalid/v1/chat/completions";c.key="TEST_TOKEN";new ModelClient().request(c,"test","test",null);yes(last.getURL().toString().equals(c.endpoint),"custom full endpoint preserved");
  for(String ep:new String[]{"http://example.invalid","https://user:secret@example.invalid","https://example.invalid?key=secret"}){c.endpoint=ep;boolean rejected=false;try{c.validate();}catch(Exception e){rejected=true;}yes(rejected,"reject unsafe endpoint "+passed);}
  bad("{\"action\":\"shell\",\"command\":\"id\"}","reject shell");bad("{\"action\":\"tap\",\"x\":1001,\"y\":20}","reject out of bounds");bad("{\"action\":\"tap\",\"x\":-1,\"y\":20}","reject negative coordinates");bad("not json","reject invalid JSON");bad("{\"action\":\"tap\"}","reject missing coordinates");
  yes(Action.parse("```json\n{\"action\":\"tap\",\"id\":1}\n```").name.equals("tap"),"accept fenced JSON");
  write(ctx,new JSONObject().put("passed",passed).put("cases",new JSONArray(cases)).put("real_network_calls",0));
 }catch(Throwable e){try{write(ctx,new JSONObject().put("passed",passed).put("failure",e.toString()));}catch(Exception ignored){}}}
 static void write(Context c,JSONObject o)throws Exception{try(FileOutputStream out=c.openFileOutput("contracts.json",0)){out.write(o.put("sampledAt",System.currentTimeMillis()).toString(2).getBytes("UTF-8"));}}
}
