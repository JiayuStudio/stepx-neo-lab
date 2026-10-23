package studio.jiayu.neoagent;
import android.app.Activity;import android.os.*;import org.json.*;import java.io.*;
/** Debug-only, shell permission protected, fixed harmless tasks. Not in release. */
public class QaActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);String mode=getIntent().getStringExtra("mode");ControlService s=ControlService.instance;
  if("approve_purchase_scope".equals(mode)){try{Config current=Config.load(this);if(!"api.deepseek.com".equals(new java.net.URI(current.endpoint).getHost()))throw new Exception("recipient changed");AppAccess access=new AppAccess(this);org.json.JSONArray installed=access.installed();java.util.List<String> wanted=java.util.Arrays.asList("com.taobao.taobao","com.jingdong.app.mall");for(int i=0;i<installed.length();i++)installed.getJSONObject(i).put("enabled",access.allowed(installed.getJSONObject(i).getString("package"))||wanted.contains(installed.getJSONObject(i).getString("package")));access.save(new org.json.JSONObject().put("apps",installed).put("preferredShopping",access.saved().optString("preferredShopping","")).put("compareShopping",access.saved().optBoolean("compareShopping",false)));java.util.ArrayList<String> approved=new java.util.ArrayList<>();for(String app:wanted)if(access.allowed(app))approved.add(app);if(approved.isEmpty())throw new Exception("purchase apps unavailable");new RoutineAccess(this).approve(current,approved);try(java.io.FileOutputStream out=openFileOutput("purchase-scope-approved.json",0)){out.write(new org.json.JSONObject().put("approved",true).put("apps",approved).put("recipient","api.deepseek.com").toString().getBytes("UTF-8"));}}catch(Exception e){try(java.io.FileOutputStream out=openFileOutput("purchase-scope-approved.json",0)){out.write("{\"approved\":false}".getBytes("UTF-8"));}catch(Exception ignored){}}finish();return;}
  if("approve_full_test".equals(mode)){try{Config current=Config.load(this);if(!"api.deepseek.com".equals(new java.net.URI(current.endpoint).getHost()))throw new Exception("recipient changed");new RoutineAccess(this).approve(current,java.util.Arrays.asList("cn.wps.moffice_eng","com.sankuai.meituan"));try(FileOutputStream out=openFileOutput("full-scope-test-approved.json",0)){out.write("{\"approved\":true,\"apps\":2,\"recipient\":\"api.deepseek.com\"}".getBytes("UTF-8"));}}catch(Exception ignored){}finish();return;}
  if("voice_endpoint".equals(mode)){VoiceEndpointContracts.start(this);return;}
  if("action_envelope".equals(mode)){ActionEnvelopeContracts.start(this);return;}
  if("agent_ime".equals(mode)){AgentImeContracts.start(this);return;}
  if("ime_enter".equals(mode)){ImeEnterContracts.start(this);return;}
  if("voice_display".equals(mode)){VoiceDisplayContracts.start(this);return;}
  if("voice_capture".equals(mode)){VoiceCaptureContracts.start(this);return;}
  if("workflow_routing".equals(mode)){WorkflowRoutingContracts.start(this);return;}
  if("voice_asr".equals(mode)){VoiceAsrContracts.start(this);return;}
  if("adaptive_routing".equals(mode)){AdaptiveRoutingContracts.run(this);finish();return;}
  if("approve_adaptive_apps".equals(mode)){AdaptiveAppApproval.run(this);finish();return;}
  if("boundary_workflow".equals(mode)){BoundaryWorkflowContracts.run(this);finish();return;}
  if("app_inventory".equals(mode)){AppInventoryContracts.run(this);finish();return;}
  if("document_evidence".equals(mode)){DocumentEvidenceContracts.start(this);return;}
  if("checkpoint_contracts".equals(mode)){try{CheckpointContracts.contracts(this,s);}catch(Exception ignored){}finish();return;}
  if("checkpoint_seed".equals(mode)||"checkpoint_report".equals(mode)){try{if("checkpoint_seed".equals(mode))CheckpointContracts.seed(this,s);else CheckpointContracts.report(this,s);}catch(Exception e){try{org.json.JSONObject failure=new org.json.JSONObject().put("failure",e.getMessage());try(java.io.FileOutputStream out=openFileOutput("checkpoint-process.json",0)){out.write(failure.toString().getBytes("UTF-8"));}}catch(Exception ignored){}}finish();return;}
  if("effects".equals(mode)||"effects_preview".equals(mode)){EffectsContracts.start(this);return;}
  if("keyboard".equals(mode)||"keyboard_timeout".equals(mode)){KeyboardContracts.start(this);return;}
  if("checkout_review".equals(mode)){CheckoutReviewContracts.start(this);return;}
  if("memory".equals(mode)){new Thread(()->MemoryContracts.run(this)).start();finish();return;}
  if("mcp".equals(mode)){McpContracts.start(this);return;}
  if("search_node".equals(mode)){SearchNodeContracts.start(this);return;}
  if("search_write".equals(mode)){SearchWriteContracts.start(this);return;}
  if("rejections".equals(mode)){RejectionContracts.start(this);return;}
  if("native_labels".equals(mode)){NativeLabelsContracts.start(this);return;}
  if("launcher".equals(mode)){LauncherContracts.start(this);return;}
  if("purchase_contracts".equals(mode)||"purchase_mcp".equals(mode)){PurchaseContracts.start(this);return;}
  if("workflow".equals(mode)){WorkflowContracts.start(this);return;}
  if("invalid_actions".equals(mode)){InvalidActionContracts.start(this);return;}
  if("search_submit".equals(mode)){SearchSubmitContracts.start(this);return;}
  if("search_input".equals(mode)){SearchInputContracts.start(this);return;}
  if("capture".equals(mode)){CaptureContracts.start(this);return;}
  if("visual_action".equals(mode)){VisualActionContracts.start(this);return;}
  if("visual_scope".equals(mode)){new Thread(()->VisualScopeContracts.run(this)).start();finish();return;}
  if("routine".equals(mode)){new Thread(()->RoutineContracts.run(this)).start();finish();return;}
  if("vision_api".equals(mode)){new Thread(()->VisionCapabilityContracts.run(this)).start();finish();return;}
  if("vision".equals(mode)){new Thread(()->VisionContracts.run(this)).start();finish();return;}
  if("runtime".equals(mode)){RuntimeContracts.start(this);return;}
  if("chat".equals(mode)){new Thread(()->{try{Config conf=Config.load(this);org.json.JSONArray results=new org.json.JSONArray();for(String text:new String[]{"去哔哩哔哩搜索航拍中国，打开一个视频", "帮我去淘宝搜索对应产品并走一下下单流程", "用户：我想找一个桌面手机支架，预算50元以内。\n助手：好的。\n用户：去淘宝搜索对应产品，先看看价格", "帮我点外卖", "请使用美团外卖，找番茄鸡蛋饭一份，预算30元，按评分4.5以上筛选商家，核对订单但不提交，不付款。"}){org.json.JSONObject plan=Conversation.plan(new ModelClient(),conf,text);if(plan.optString("kind").equals("task")){try{plan.put("resolved",Conversation.resolve(this,plan.getString("app")));}catch(Exception e){plan.put("local_app_available",false);}}results.put(plan);}try(java.io.FileOutputStream out=openFileOutput("chat-plans.json",0)){out.write(results.toString(2).getBytes("UTF-8"));}}catch(Exception e){}}).start();finish();return;}
  if("contracts".equals(mode)){new Thread(()->ModelContracts.run(this)).start();finish();return;}
  if("resume".equals(mode)){if(s!=null)s.resumeFromUser();finish();return;}
  if("stop".equals(mode)){if(s!=null)s.stop("QA 停止测试");finish();return;}
  if("status".equals(mode)){if(s!=null)s.saveReport();finish();return;}
  Config c=Config.load(this);if("wps".equals(mode))c.maxSteps=40;if("text".equals(mode))c.vision=false;
  new Handler().postDelayed(()->{ControlService service=ControlService.instance;if(service!=null){if("wps".equals(mode))service.start(c,"cn.wps.moffice_eng","在WPS新建空白文字文档，自行生成一份完整中文租赁合同模板，初稿租金支付期限采用每月10日前支付，未知资料用占位符，保存为测试租赁合同_完整验证8_恢复；返回后重新打开该文件，只把租金支付条款修改为每月5日前支付，其他段落保持不变，保存并再次打开核对。只编辑本次新文件，不改其他文档，不签署、不发送。");else if("movie".equals(mode)){c.checkout=true;service.start(c,"com.sankuai.meituan","使用美团电影入口，以当前城市选择一个近期可售电影、影院和场次，查看单人票价格；如果可以在不生成订单和不锁座的情况下到订单核对页则继续，到提交或付款前停止。不要提交订单、不要付款、不要确认锁座、不要接受新协议或开会员。");}else if("taobao".equals(mode))service.start(c,"com.taobao.taobao","在淘宝搜索 StepX Neo 手机，看到搜索结果后停止，只看价格，不打开购物车，不加购，不购买，不提交订单。");else if("bili".equals(mode))service.start(c,"tv.danmaku.bili","在哔哩哔哩搜索「航拍中国」，打开一个与航拍中国相关的视频，进入视频详情页后停止。不要登录、点赞、关注或评论。");else if("takeover".equals(mode))service.start(c,getPackageName(),"在演示中依次搜索北京、上海、广州、深圳、南京、杭州，每次搜索后返回输入框继续，全部完成后停止。");else service.start(c,getPackageName(),"在本地演示中搜索「航拍中国」，点击搜索结果的演示视频，看到「演示视频详情 · 已打开：航拍中国」后完成。");}new Handler().postDelayed(this::finish,4000);},400);
 }
}
