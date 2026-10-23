package studio.jiayu.neoagent;
import android.os.*;import org.json.*;import java.net.*;import java.io.*;
/** Debug-only end-to-end test: native observation -> mocked ask -> encrypted outcome -> chat. */
public final class FlowQaActivity extends MainActivity {
 String originalGrant="";String originalChats="",originalOutcome="",originalDelivered="",testSession="";long began;boolean beganRun=false;int checks=0;
 @Override public void onCreate(Bundle b){try{Vault v=new Vault(this);originalGrant=v.get(RoutineAccess.KEY);originalChats=v.get("local-chats");originalOutcome=v.get("last-outcome");originalDelivered=getSharedPreferences("ui",0).getString("deliveredRun","");v.remove("last-outcome");}catch(Exception e){}super.onCreate(b);getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);handler.postDelayed(this::startQa,1600);}
 void yes(boolean ok,String name)throws Exception{if(!ok)throw new Exception(name);checks++;}
 void startQa(){try{
  yes(ControlService.instance!=null,"service ready");newChat();testSession=sessionId;
  URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new ModelContracts.Fake(u){public InputStream getInputStream(){return new ByteArrayInputStream("{\"choices\":[{\"message\":{\"content\":\"{\\\"action\\\":\\\"ask\\\",\\\"message\\\":\\\"请在手机处理登录验证码后交还 Agent。\\\"}\"}}]}".getBytes());}};}}:null);
  Config c=new Config();c.endpoint="https://example.invalid";c.key="TEST_TOKEN";c.vision=true;
  new RoutineAccess(this).approve(c,java.util.Collections.singletonList(getPackageName()));began=System.currentTimeMillis();beganRun=ControlService.instance.start(c,getPackageName(),"本地测试，遇到规格问题请询问用户。",testSession);yes(beganRun,"run started");handler.postDelayed(this::poll,700);
 }catch(Throwable e){complete(e);}}
 void poll(){try{ControlService service=ControlService.instance;if(service!=null&&service.running){if(System.currentTimeMillis()-began>20000)throw new Exception("timeout");handler.postDelayed(this::poll,700);return;}
  JSONObject outcome=new JSONObject(new Vault(this).get("last-outcome"));yes(testSession.equals(outcome.getString("session")),"originating session");yes(outcome.getString("detail").contains("登录验证码"),"concrete model question");yes(!getSharedPreferences("vault",0).getString("last-outcome","").contains("登录验证码"),"outcome encrypted at rest");
  int found=0;JSONArray m=chat().getJSONArray("messages");for(int i=0;i<m.length();i++)if(m.getJSONObject(i).getString("text").contains("登录验证码"))found++;
  if(found==0&&System.currentTimeMillis()-began<18000){handler.postDelayed(this::poll,700);return;}
  yes(found==1,"question delivered exactly once");loadChats();m=chat().getJSONArray("messages");found=0;for(int i=0;i<m.length();i++)if(m.getJSONObject(i).getString("text").contains("登录验证码"))found++;yes(found==1,"question survives encrypted reload");yes(service.modelCalls==1&&service.actions==0,"ask executes no target actions");complete(null);
 }catch(Throwable e){complete(e);}}
 void complete(Throwable error){try{if(ControlService.instance!=null&&ControlService.instance.busy())ControlService.instance.stop("QA 停止测试");JSONObject report=new JSONObject().put("passed",checks).put("real_network_calls",0).put("native_observation",beganRun).put("success",error==null);if(error!=null)report.put("failure",error.toString());try(FileOutputStream out=openFileOutput("flow-contracts.json",0)){out.write(report.put("sampledAt",System.currentTimeMillis()).toString(2).getBytes("UTF-8"));}
  Vault v=new Vault(this);if(originalGrant.isEmpty())v.remove(RoutineAccess.KEY);else v.put(RoutineAccess.KEY,originalGrant);if(originalChats.isEmpty())v.remove("local-chats");else v.put("local-chats",originalChats);if(originalOutcome.isEmpty())v.remove("last-outcome");else v.put("last-outcome",originalOutcome);getSharedPreferences("ui",0).edit().putString("deliveredRun",originalDelivered).commit();
 }catch(Exception ignored){}handler.removeCallbacksAndMessages(null);finish();}
}
