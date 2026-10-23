package studio.jiayu.neoagent;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.content.Context;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;

/** Service-owned chat/planning. The panel uses the selected system keyboard; no ASR lives here. */
final class FloatingConversation {
 private static final String CHATS="local-chats";
 private final ControlService owner;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final FloatingPlanRules rules=new FloatingPlanRules();
 private final LinkedHashMap<String,String> submittedRuns=new LinkedHashMap<>();
 private volatile ModelClient client=new ModelClient();
 private Future<?> job;
 private Runnable listener;
 private volatile boolean opened,closed;
 private String session="",notice="",lastMarker="";
 private ControlService.VoiceHold hold;
 private Pending pending;
 private boolean ticking;

 private static final class Pending {
  final long token,connection;
  final String session;
  final Config config;
  final TaskWorkflow workflow;
  final ControlService.VoiceHold hold;
  boolean scheduled;
  Pending(long t,long c,String s,Config cfg,TaskWorkflow plan,ControlService.VoiceHold h){token=t;connection=c;session=s;config=cfg;workflow=plan;hold=h;}
 }
 FloatingConversation(ControlService service){owner=service;}
 private void ui(Runnable work){if(Looper.myLooper()==Looper.getMainLooper())work.run();else main.post(work);}
 private boolean connected(){return !closed&&owner==ControlService.instance&&owner.ownsConnection();}
 private void requireOwner()throws Exception{if(!connected())throw new Exception("控制服务尚未连接，请先核对无障碍状态");}
 private boolean locked(){try{KeyguardManager keyguard=(KeyguardManager)owner.getSystemService(Context.KEYGUARD_SERVICE);PowerManager power=(PowerManager)owner.getSystemService(Context.POWER_SERVICE);return keyguard==null||power==null||!power.isInteractive()||keyguard.isKeyguardLocked()||keyguard.isDeviceLocked();}catch(RuntimeException unavailable){return true;}}
 private void requireUnlocked()throws Exception{if(locked())throw new Exception("请先解锁手机，再打开侧边对话");}
 void setListener(Runnable value){ui(()->{listener=value;if(!closed)startTick();changed();});}
 boolean active(){return opened&&!closed&&!locked();}
 boolean ownsPlanning(){return !closed&&rules.ownsPlanning();}

 /** Reopening a panel preserves its own pending request. It never launches an Activity. */
 void open(){ui(this::openNow);}
 private void openNow(){
  try{
   requireOwner();requireUnlocked();MainActivity.prepareForFloating();
   if(rules.ownsPlanning()){opened=true;startTick();changed();return;}
   // Pause before choosing a chat so the running target cannot advance underneath it.
   if(owner.running)owner.pauseForUser();
   synchronized(ChatStore.LOCK){
    JSONObject store=readStore();JSONArray sessions=store.getJSONArray("sessions");
    String chosen=owner.busy()&&!owner.ownerSession.isEmpty()?owner.ownerSession:store.optString("current");
    if(chosen.isEmpty())chosen=UUID.randomUUID().toString();
    if(find(sessions,chosen)==null)sessions.put(new JSONObject().put("id",chosen).put("title","新对话").put("created",System.currentTimeMillis()).put("messages",new JSONArray()));
    session=chosen;store.put("current",session);writeStore(store);
   }
   hold=owner.busy()?owner.holdForVoice(session,UUID.randomUUID().toString()):null;
   opened=true;notice="使用当前输入法输入，核对文字后发送";startTick();MainActivity.refreshFromFloating();changed();
  }catch(Exception failed){notice=ControlService.safeError(failed);changed();}
 }

 void submit(String text){ui(()->submitNow(text));}
 private void submitNow(String text){
  if(locked()){if(cancelForLock())changed();return;}
  if(!active())openNow();
  if(!active())return;
  if(rules.ownsPlanning()){notice="正在整理上一条目标；可先取消本次规划";changed();return;}
  try{
   requireOwner();if(text==null||text.trim().isEmpty())return;
   if(PrivateText.credential(text))throw new Exception("密钥请在模型与连接中保存，不要发送到聊天");
   if(text.length()>6000)throw new Exception("输入过长，请分开发送");
   Config config=Config.load(owner);config.validate();
   if(!new AppAccess(owner).configured())throw new Exception("请先在「交给 Agent 的应用」选择允许操作的应用");
   requireSession(readStore(),session);
   if(owner.busy()){
    if(!session.equals(owner.ownerSession))throw new Exception("另一对话的目标仍在执行，请先在原对话接管");
    if(hold==null||!owner.voiceHoldCurrent(hold))hold=owner.holdForVoice(session,UUID.randomUUID().toString());
   }else hold=null;
   String message=text.trim();append(session,"user",message,null,true);
   JSONObject store=readStore();JSONObject chat=requireSession(store,session);
   try{new UserMemory(owner,"user-preferences").ingest(store.getJSONArray("sessions"));}catch(Exception unavailable){/* The current request is still usable. */}
   StringBuilder history=new StringBuilder();JSONArray messages=chat.getJSONArray("messages");
   for(int i=Math.max(0,messages.length()-12);i<messages.length();i++){JSONObject item=messages.getJSONObject(i);history.append(item.getString("role")).append(": ").append(PrivateText.redact(item.getString("text"))).append('\n');}
   String memory="";try{memory=new UserMemory(owner,"user-preferences").contextFor(message);}catch(Exception ignored){}
   String context=history+"\n"+memory+"\n"+new AppAccess(owner).routing(PrivateText.redact(message));
   long token=rules.begin(session),connection=owner.connectionToken();String boundSession=session;ControlService.VoiceHold boundHold=hold;
   client.cancel();client=new ModelClient();pending=null;notice="正在理解你的目标";changed();
   request(token,connection,boundSession,boundHold,config,client,message,context,0);
  }catch(Exception failed){notice=ControlService.safeError(failed);changed();}
 }
 private boolean current(long token,long connection,String boundSession){return connected()&&!locked()&&owner.connectionToken()==connection&&session.equals(boundSession)&&rules.planning(token,boundSession);}
 private void request(long token,long connection,String boundSession,ControlService.VoiceHold boundHold,Config config,ModelClient planner,String message,String context,int repair){
  if(!current(token,connection,boundSession))return;
  job=worker.submit(()->{
   try{
    if(!rules.planning(token,boundSession))return;
    JSONObject plan=SharedComparisonPlan.create(owner,PrivateText.redact(message));
    if(plan==null){if(!rules.planning(token,boundSession))return;plan=Conversation.plan(planner,config,context);}
    final JSONObject response=plan;
    main.post(()->{
     if(!current(token,connection,boundSession))return;
     try{
      if(!"task".equals(response.optString("kind"))){append(boundSession,"assistant",response.optString("reply","请告诉我你想做什么"),null,true);rules.cancel();notice="";changed();return;}
      TaskWorkflow workflow=new TaskWorkflow(owner,response,PrivateText.redact(message));workflow.baseSteps=config.maxSteps;
      if(!rules.ready(token,boundSession))return;
      pending=new Pending(token,connection,boundSession,config,workflow,boundHold);
      append(boundSession,"assistant",workflow.steps.size()>1?"按顺序执行"+workflow.steps.size()+"个目标：\n"+workflow.brief():"我会打开"+workflow.current().app+"，"+workflow.current().goal+"。",null,true);
      if(new RoutineAccess(owner).allows(config,workflow.packages()))startPending(false);
      else{notice="请确认本次应用画面接收范围后执行";changed();}
     }catch(WorkflowPlanException invalid){
      if(repair==0&&current(token,connection,boundSession)){notice="正在校正应用与原话的对应关系…";changed();request(token,connection,boundSession,boundHold,config,planner,message,context+"\n本机计划校验未通过："+invalid.safeFeedback()+"。请仅根据相同原话重新规划一次，使用本机给出的完整原文子句作为source，不增加目标或权限。",1);}
      else fail(token,boundSession,invalid.userMessage());
     }catch(Exception failed){fail(token,boundSession,ControlService.safeError(failed));}
    });
   }catch(Exception failed){main.post(()->{if(current(token,connection,boundSession))fail(token,boundSession,ControlService.safeError(failed));});}
  });
 }
 private void fail(long token,String boundSession,String message){
  if(!rules.planning(token,boundSession)&&!rules.pending(token,boundSession))return;
  rules.cancel();pending=null;notice=message;
  try{append(boundSession,"assistant",message,null,true);}catch(Exception unavailable){notice="本机对话保存失败；目标未开始，原目标仍保留";}
  changed();
 }

 /** false is an explicit cancellation; automatic reuse of existing consent is internal. */
 void confirm(boolean accepted){ui(()->{if(locked()){if(cancelForLock())changed();return;}if(accepted)startPending(true);else cancelNow();});}
 private void validatePending(Pending value)throws Exception{
  requireOwner();requireUnlocked();if(owner.connectionToken()!=value.connection||!session.equals(value.session)||!rules.pending(value.token,value.session))throw new Exception("本次目标已失效，请重新发送");
  Config fresh=Config.load(owner);fresh.validate();
  if(!sameConfig(value.config,fresh))throw new Exception("模型配置已变化，未开始旧计划");
  requireSession(readStore(),value.session);
  if(value.hold!=null){if(!owner.voiceHoldCurrent(value.hold))throw new Exception("原暂停目标已变化，未替换；请重新发送目标");}
  else if(owner.busy())throw new Exception("当前已有其他目标，未替换；请重新打开侧边对话");
 }
 private void startPending(boolean explicit){
  Pending value=pending;if(value==null||value.scheduled)return;
  try{
   validatePending(value);RoutineAccess access=new RoutineAccess(owner);
   if(!access.allows(value.config,value.workflow.packages())){
    if(!explicit){notice="请确认本次应用画面接收范围后执行";changed();return;}
    access.approve(value.config,value.workflow.packages());
   }
   value.scheduled=true;owner.floatingChat.beforeExecution();
   main.postDelayed(()->commitPending(value),160);
  }catch(Exception failed){fail(value.token,value.session,ControlService.safeError(failed));}
 }
 private void commitPending(Pending value){
  if(locked()){if(cancelForLock())changed();return;}
  if(pending!=value||!value.scheduled||closed||!rules.pending(value.token,value.session))return;
  value.scheduled=false;
  try{
   validatePending(value);
   if(owner.floatingChat.panelVisible()){notice="侧边对话已重新打开；请收起后再点执行";changed();return;}
   if(!new RoutineAccess(owner).allows(value.config,value.workflow.packages()))throw new Exception("应用或数据接收范围已变化，目标未开始");
   boolean started=value.hold==null?owner.startWorkflow(value.config,value.workflow,value.session):owner.replaceHeldVoice(value.hold,value.config,value.workflow,value.session);
   if(!started)throw new Exception("目标尚未开始，请核对控制服务和授权状态");
   submittedRuns.put(owner.runId,value.session);while(submittedRuns.size()>32)submittedRuns.remove(submittedRuns.keySet().iterator().next());
   rules.committed(value.token,value.session);pending=null;hold=null;notice="已开始执行，可随时接管";
   try{append(value.session,"assistant","已开始操作"+value.workflow.apps()+"，可随时接管。",null,true);}catch(Exception unavailable){notice="目标已开始；本机对话保存待核对";}
   startTick();changed();
  }catch(Exception failed){fail(value.token,value.session,ControlService.safeError(failed));}
 }
 private static boolean sameConfig(Config a,Config b){return a.endpoint.equals(b.endpoint)&&a.model.equals(b.model)&&a.protocol.equals(b.protocol)&&a.vision==b.vision&&a.key.equals(b.key);}
 void cancelPlanning(){ui(this::cancelNow);}
 private void cancelNow(){if(closed)return;if(locked()){if(cancelForLock())changed();return;}if(!rules.ownsPlanning())return;rules.cancel();client.cancel();if(job!=null)job.cancel(true);pending=null;notice=owner.paused?"本次规划已取消；原目标仍暂停":"本次规划已取消";changed();}
 /** Retire only this engine's unsent request/hold. Never stop an already submitted run. */
 private boolean cancelForLock(){
  if(closed)return false;
  boolean update=opened||rules.ownsPlanning()||pending!=null||hold!=null||!"请先解锁手机，再打开侧边对话".equals(notice);
  if(!update)return false;
  ControlService.VoiceHold original=hold,waiting=pending==null?null:pending.hold;
  if(connected()&&(original!=null&&owner.voiceHoldCurrent(original)||waiting!=null&&owner.voiceHoldCurrent(waiting)))owner.voiceGoals.invalidate();
  if(rules.ownsPlanning())rules.cancel();client.cancel();if(job!=null)job.cancel(true);pending=null;hold=null;opened=false;notice="请先解锁手机，再打开侧边对话";return update;
 }

 private JSONObject readStore()throws Exception{
  requireUnlocked();synchronized(ChatStore.LOCK){JSONObject store=ChatStore.read(owner,CHATS);store.getJSONArray("sessions");requireUnlocked();return store;}
 }
 private void writeStore(JSONObject store)throws Exception{requireUnlocked();ChatStore.write(owner,CHATS,store);}
 private static JSONObject find(JSONArray sessions,String id){for(int i=0;i<sessions.length();i++){JSONObject item=sessions.optJSONObject(i);if(item!=null&&id.equals(item.optString("id")))return item;}return null;}
 private static JSONObject requireSession(JSONObject store,String id)throws Exception{JSONObject chat=find(store.getJSONArray("sessions"),id);if(chat==null)throw new Exception("本机对话已变化，请重新打开侧边对话");chat.getJSONArray("messages");return chat;}
 private void append(String id,String role,String text,String event,boolean makeCurrent)throws Exception{
  synchronized(ChatStore.LOCK){
   JSONObject store=readStore(),chat=requireSession(store,id);JSONArray messages=chat.getJSONArray("messages");
   if(event!=null){JSONObject found=outcome(messages,event);if(found!=null){if(!role.equals(found.optString("role"))||!text.equals(found.optString("text")))throw new Exception("任务结果保存状态待核对");return;}}
   JSONObject item=new JSONObject().put("message_id",UUID.randomUUID().toString()).put("role",role).put("text",text).put("time",System.currentTimeMillis());if(event!=null)item.put("outcome_event",event);messages.put(item);while(messages.length()>100)messages.remove(0);
   if("user".equals(role)&&"新对话".equals(chat.optString("title")))chat.put("title",text.substring(0,Math.min(24,text.length())));
   if(makeCurrent)store.put("current",id);writeStore(store);
  }
  requireUnlocked();MainActivity.refreshFromFloating();
 }
 private static JSONObject outcome(JSONArray messages,String event){for(int i=0;i<messages.length();i++){JSONObject item=messages.optJSONObject(i);if(item!=null&&event.equals(item.optString("outcome_event")))return item;}return null;}
 private void deliverOutcome()throws Exception{
  if(submittedRuns.isEmpty())return;
  requireUnlocked();
  String raw=new Vault(owner).get("last-outcome");if(raw.isEmpty())return;
  JSONObject result=new JSONObject(raw);String run=result.optString("run"),boundSession=submittedRuns.get(run);
  if(boundSession==null||!boundSession.equals(result.optString("session")))return;
  String event=result.optString("event",run);if(event.isEmpty()||event.equals(owner.getSharedPreferences("ui",0).getString("deliveredRun","")))return;
  String detail=result.optString("detail"),text=result.getString("status")+(detail.isEmpty()?"":"\n\n"+detail);
  append(boundSession,"assistant",text,event,false);
  JSONObject saved=outcome(requireSession(readStore(),boundSession).getJSONArray("messages"),event);
  if(saved==null||!"assistant".equals(saved.optString("role"))||!text.equals(saved.optString("text")))throw new Exception("任务结果保存尚未核验");
  requireUnlocked();if(!owner.getSharedPreferences("ui",0).edit().putString("deliveredRun",event).commit())throw new Exception("任务结果已保存，送达状态待核对");
  MainActivity.refreshFromFloating();changed();
 }
 private final Runnable tick=new Runnable(){public void run(){
  if(closed){ticking=false;return;}
  if(locked()){cancelForLock();if(!"locked".equals(lastMarker)){lastMarker="locked";changed();}main.postDelayed(this,750);return;}
  if(!connected()&&rules.ownsPlanning()){rules.cancel();client.cancel();if(job!=null)job.cancel(true);pending=null;notice="控制服务已断开；新目标未开始，原目标保留";}
  if(connected())try{deliverOutcome();}catch(Exception unavailable){notice="任务结果保存状态待核对";}
  String marker="unlocked|"+owner.status+"|"+owner.running+"|"+owner.paused+"|"+rules.phase();if(!marker.equals(lastMarker)){lastMarker=marker;changed();}
  main.postDelayed(this,750);
 }};
 private void startTick(){if(!ticking){ticking=true;main.post(tick);}}
 private void changed(){if(listener!=null)try{listener.run();}catch(RuntimeException ignored){}}
 JSONObject snapshot(){
  if(locked())return lockedSnapshot();
  if(Looper.myLooper()!=Looper.getMainLooper())try{return owner.ui(this::snapshot);}catch(Exception ignored){return new JSONObject();}
  JSONObject result=new JSONObject();
  try{
   JSONArray messages=new JSONArray();if(!session.isEmpty()){JSONObject chat=find(readStore().getJSONArray("sessions"),session);if(chat!=null)messages=new JSONArray(chat.getJSONArray("messages").toString());}
   result.put("active",active()).put("session",session).put("messages",messages).put("planning",rules.phase()==FloatingPlanRules.Phase.PLANNING).put("pending",JSONObject.NULL).put("notice",notice).put("status",owner.status).put("running",connected()&&owner.running).put("paused",owner.paused);
   if(pending!=null){String host=new URI(pending.config.endpoint).getHost();result.put("pending",new JSONObject().put("host",host==null?"当前模型接口":host).put("apps",pending.workflow.apps()).put("goal",pending.workflow.description()).put("tasks",pending.workflow.list()).put("vision",pending.config.vision).put("confirmation_required",!new RoutineAccess(owner).allows(pending.config,pending.workflow.packages())));}
  }catch(Exception unavailable){try{result.put("notice","本机对话暂不能读取");}catch(Exception ignored){}}
  return locked()?lockedSnapshot():result;
 }
 private JSONObject lockedSnapshot(){JSONObject value=new JSONObject();try{value.put("active",false).put("locked",true).put("session","").put("messages",new JSONArray()).put("planning",false).put("pending",JSONObject.NULL).put("notice","请先解锁手机，再打开侧边对话").put("status","请先解锁手机").put("running",false).put("paused",false);}catch(Exception ignored){}return value;}
 /** Service disposal only. Hiding a panel must not call this or stop a submitted goal. */
 void close(){client.cancel();ui(()->{if(closed)return;closed=true;opened=false;rules.close();pending=null;hold=null;if(job!=null)job.cancel(true);worker.shutdownNow();main.removeCallbacks(tick);ticking=false;listener=null;});}
}
