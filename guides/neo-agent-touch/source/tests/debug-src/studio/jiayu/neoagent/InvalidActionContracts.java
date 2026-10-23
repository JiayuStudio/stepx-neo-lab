package studio.jiayu.neoagent;
import android.app.Activity;
import android.os.*;
import android.widget.*;
import android.view.accessibility.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Owned native fixture; process-local fake network is cleared by host cleanup. */
final class InvalidActionContracts {
 static void start(Activity a){
  TextView label=new TextView(a);label.setText("输入预检与无效动作恢复检查");a.setContentView(label);
  new Handler().postDelayed(()->new Thread(()->run(a)).start(),1000);
 }
 static void run(Activity a){
  ControlService s=ControlService.instance;JSONObject out=new JSONObject();String before="";
  AtomicInteger requests=new AtomicInteger();AtomicBoolean noEarlyActions=new AtomicBoolean(true);
  AtomicBoolean preflightFeedback=new AtomicBoolean();int passed=0;
  try{
   if(Action.parse("{\"action\":\"tap\",\"id\":\"6\"}").data.getInt("id")!=6)throw new Exception("numeric string not normalized");passed++;
   for(String raw:new String[]{"{\"action\":\"tap\",\"id\":\"6\\\"\"}","{\"action\":\"tap\",\"id\":6.5}","{\"action\":\"tap\",\"id\":-1}","{\"action\":\"submit_search\",\"id\":0}","{\"action\":\"scroll\",\"direction\":\"sideways\"}"}){
    boolean denied=false;try{Action.parse(raw);}catch(Exception expected){denied=true;}
    if(!denied)throw new Exception("bad schema accepted");passed++;
   }
   if(!Action.parse("{\"action\":\"submit_search\"}").name.equals("submit_search"))throw new Exception("native editor search schema absent");passed++;
   if(s==null||!s.connected||s.busy())throw new Exception("Service unavailable");
   before=new Vault(a).get(RoutineAccess.KEY);Config c=new Config();
   c.endpoint="https://fixture.invalid/v1";c.key="LOCAL_FIXTURE";c.vision=true;c.maxSteps=9;
   new RoutineAccess(a).approve(c,Collections.singletonList(a.getPackageName()));
   URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){
    protected URLConnection openConnection(URL url){return new ModelContracts.Fake(url){
     public InputStream getInputStream(){
      try{
       int turn=requests.incrementAndGet();JSONObject request=new JSONObject(body.toString("UTF-8"));
       JSONArray content=request.getJSONArray("messages").getJSONObject(1).getJSONArray("content");
       String text=content.getJSONObject(0).getString("text");
       JSONObject state=new JSONObject(text.substring(text.indexOf("当前界面：")+5));
       JSONArray nodes=state.getJSONArray("nodes");String response;
       if(turn==1){
        int notInput=-1;
        for(int i=0;i<nodes.length();i++)if(!nodes.getJSONObject(i).optBoolean("editable")&&nodes.getJSONObject(i).optBoolean("clickable")){notInput=nodes.getJSONObject(i).getInt("id");break;}
        if(notInput<0)throw new Exception("No non-editor fixture target");
        response=new JSONObject().put("action","type").put("id",notInput).put("text","must not dispatch").toString();
       }else if(turn==2){
        JSONArray feedback=state.optJSONArray("rejected_actions");
        preflightFeedback.set(!s.paused&&!s.pendingDispatch&&s.actions==0&&feedback!=null&&feedback.length()==1&&feedback.getJSONObject(0).getString("action").equals("type")&&!feedback.getJSONObject(0).getBoolean("executed"));
        response="{\"action\":\"tap\",\"id\":\"6\\\"\"}";
       }else if(turn==3)response="not-json";
       else if(turn==4){
        noEarlyActions.set(s.actions==0);int input=-1;
        for(int i=0;i<nodes.length();i++)if(nodes.getJSONObject(i).optBoolean("editable"))input=nodes.getJSONObject(i).getInt("id");
        response=new JSONObject().put("action","type").put("id",input).put("text","schema recovery").toString();
       }else response="{\"action\":\"finish\",\"message\":\"本机格式与输入预检恢复验证\"}";
       return new ByteArrayInputStream(new JSONObject().put("choices",new JSONArray().put(new JSONObject().put("message",new JSONObject().put("content",response)))).toString().getBytes("UTF-8"));
      }catch(Exception error){return new ByteArrayInputStream("{}".getBytes());}
     }
    };}
   }:null);
   if(!s.ui(()->s.start(c,a.getPackageName(),"在本地演示中输入schema recovery，检查后结束")))throw new Exception("Cannot start");
   long until=SystemClock.elapsedRealtime()+26000;
   while(s.busy()&&SystemClock.elapsedRealtime()<until)Thread.sleep(100);
   if(s.busy()||s.paused||requests.get()!=5||s.actions!=1)throw new Exception("invalid input/action interrupted goal or wrong action count");passed++;
   if(!preflightFeedback.get())throw new Exception("preflight rejection left an uncertain dispatch or requested takeover");passed++;
   if(!noEarlyActions.get())throw new Exception("invalid candidate dispatched");passed++;
   boolean visible=s.ui(()->{
    AccessibilityNodeInfo root=s.getRootInActiveWindow();
    for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText("schema recovery"))if(n.isVisibleToUser()&&"schema recovery".contentEquals(n.getText()))return true;
    return false;
   });
   if(!visible)throw new Exception("actual input missing");passed++;
   out.put("success",true).put("passed",passed).put("mock_requests",requests.get()).put("preflight_rejection_recovered",true);
  }catch(Throwable e){try{out.put("success",false).put("passed",passed).put("failure",e.getMessage());}catch(Exception ignored){}}
  finally{
   if(s!=null)s.stop("输入预检与无效动作回归结束");
   try{
    if(before.isEmpty())new Vault(a).remove(RoutineAccess.KEY);else new Vault(a).put(RoutineAccess.KEY,before);
    out.put("routine_access_restored",before.equals(new Vault(a).get(RoutineAccess.KEY)));
    try(FileOutputStream f=a.openFileOutput("invalid-action-contracts.json",0)){
     f.write(out.put("real_network_calls",0).put("third_party_apps",0).put("sampledAt",System.currentTimeMillis()).toString(2).getBytes("UTF-8"));
    }
   }catch(Exception ignored){}
   a.runOnUiThread(a::finish);
  }
 }
}
