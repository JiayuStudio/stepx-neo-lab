package studio.jiayu.neoagent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Debug-only display regression in an isolated real Android WebView using the production assets.
 * All state/text is public synthetic fixture data. The bridge only records UI actions: it cannot
 * start a planner, microphone, third-party app, or provider request. Not an ASR-quality test.
 */
final class VoiceDisplayContracts {
 private final Activity activity;
 private final Handler main = new Handler(Looper.getMainLooper());
 private final JSONArray bridgeActions = new JSONArray();
 private WebView web;
 private boolean completed, started;
 private final Runnable deadline = () -> fail("webview_fixture_timeout");
 private VoiceDisplayContracts(Activity activity) { this.activity = activity; }
 static void start(Activity activity) {
  activity.runOnUiThread(() -> new VoiceDisplayContracts(activity).load());
 }
 static void run(Activity activity) { start(activity); }
 private void load() {
  try {
   if ((activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
    throw new IllegalStateException("debug_fixture_only");
   if (activity.isFinishing() || activity.isDestroyed())
    throw new IllegalStateException("activity_not_active");
   activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
     | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
   web = new WebView(activity);
   WebSettings settings = web.getSettings();
   settings.setJavaScriptEnabled(true);
   settings.setBlockNetworkLoads(true);
   settings.setAllowFileAccess(false);
   settings.setAllowContentAccess(false);
   settings.setAllowFileAccessFromFileURLs(false);
   settings.setAllowUniversalAccessFromFileURLs(false);
   settings.setDomStorageEnabled(false);
   settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
   web.addJavascriptInterface(new FixtureBridge(), "NativeAgent");
   web.setWebViewClient(new WebViewClient() {
    @Override public void onPageFinished(WebView view, String url) {
     if (completed || started || !"file:///android_asset/index.html".equals(url)) return;
     started = true;
     main.postDelayed(() -> {
      if (!completed && !activity.isFinishing() && !activity.isDestroyed())
       web.evaluateJavascript(SCRIPT, null);
      else if (!completed) fail("activity_left_before_script");
     }, 200);
    }
    @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
     if (request.isForMainFrame()) fail("webview_main_frame_error_" + error.getErrorCode());
    }
   });
   activity.setContentView(web, new ViewGroup.LayoutParams(
     ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
   main.postDelayed(deadline, 45000);
   web.loadUrl("file:///android_asset/index.html");
  } catch (Throwable failure) { fail(failure.getClass().getSimpleName()); }
 }
 private final class FixtureBridge {
  @JavascriptInterface public void action(String kind, String raw) {
   synchronized (bridgeActions) {
    try {
     bridgeActions.put(new JSONObject().put("kind", kind)
       .put("data", new JSONObject(raw == null || raw.isEmpty() ? "{}" : raw)));
    } catch (Exception ignored) {
     try { bridgeActions.put(new JSONObject().put("kind", kind).put("invalid_json", true)); }
     catch (Exception ignoredAgain) { }
    }
   }
  }
  @JavascriptInterface public String actions() {
   synchronized (bridgeActions) { return bridgeActions.toString(); }
  }
  @JavascriptInterface public void complete(String raw) {
   main.post(() -> {
    try { finish(new JSONObject(raw)); }
    catch (Exception invalid) { fail("fixture_report_not_json"); }
   });
  }
 }
 private void fail(String reason) {
  if (Looper.myLooper() != Looper.getMainLooper()) { main.post(() -> fail(reason)); return; }
  try { finish(new JSONObject().put("success", false).put("stage", "synthetic_display_contracts_failed")
    .put("failure_check", reason)); } catch (Exception ignored) { }
 }
 private String assetSha256(String name) throws Exception {
  MessageDigest digest = MessageDigest.getInstance("SHA-256");
  try (InputStream input = activity.getAssets().open(name)) {
   byte[] buffer = new byte[8192]; int read;
   while ((read = input.read(buffer)) > 0) digest.update(buffer, 0, read);
  }
  StringBuilder value = new StringBuilder();
  for (byte item : digest.digest()) value.append(String.format(java.util.Locale.ROOT, "%02x", item & 255));
  return value.toString();
 }
 private void finish(JSONObject result) {
  if (completed) return;
  completed = true; main.removeCallbacks(deadline);
  try {
   result.put("source", "synthetic voice/state callbacks rendered by production app.js in an isolated Android WebView")
    .put("app_js_sha256", assetSha256("app.js")).put("style_css_sha256", assetSha256("style.css"))
    .put("fixture_bridge_only", true).put("production_bridge_used", false)
    .put("microphone_used", false).put("microphone_permission_requested", false)
    .put("asr_decoder_used", false).put("asr_accuracy_or_real_microphone_quality_proven", false)
    .put("network_calls", 0).put("external_network_loads_blocked", true)
    .put("third_party_actions", 0).put("planner_started", false)
    .put("ime_visibility_or_real_user_touch_tested", false)
    .put("rectangle_tolerance_css_px", 1).put("chat_scroll_tolerance_css_px", 1)
    .put("sampledAt", System.currentTimeMillis());
   synchronized (bridgeActions) { result.put("recorded_fixture_actions", new JSONArray(bridgeActions.toString())); }
   try (OutputStream output = activity.openFileOutput("voice-display-contracts.json", 0)) {
    output.write(result.toString(2).getBytes("UTF-8"));
   }
  } catch (Exception ignored) { }
  finally {
   if (web != null) {
    web.stopLoading(); web.removeJavascriptInterface("NativeAgent");
    if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
    web.destroy(); web = null;
   }
   activity.finish();
  }
 }
 private static final String SCRIPT = String.join("\n",
  "",
  "(async function(){",
  " const out={checks:0,passed:[],samples:[],success:false};",
  " const pass=(ok,label)=>{if(!ok)throw Error(label);out.checks++;out.passed.push(label);};",
  " const settle=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(()=>setTimeout(resolve,25))));",
  " const rect=n=>{const r=n.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height};};",
  " const delta=(a,b)=>Math.max(...['x','y','width','height'].map(k=>Math.abs(a[k]-b[k])));",
  " const emit=v=>window.agentEvent({type:'voice',data:v});",
  " const actions=()=>JSON.parse(window.NativeAgent.actions());",
  " let chatObserver=null,transcriptObserver=null;",
  " try{",
  "  pass(typeof window.agentEvent==='function','production agentEvent loaded');",
  "  pass(typeof render==='function','production app.js loaded');",
  "  const messages=Array.from({length:32},(_,i)=>({role:i%2?'assistant':'user',text:'公开合成显示回归 '+i+'：检查同一语音请求的文字、焦点和布局保持。'}));",
  "  const state={config:{endpoint:'https://fixture.invalid/v1',model:'display-fixture',protocol:'OpenAI',vision:true,hasKey:false},sessions:[{id:'display-contract-session',title:'合成显示回归',messages}],current:'display-contract-session',ready:true,running:false,paused:false,planning:false,status:'',appearance:'studio',voiceDirect:false,appAccess:{configured:true,apps:[]}};",
  "  window.agentEvent({type:'state',data:state});",
  "  await settle();",
  "  const header=document.querySelector('.topbar'),chat=document.getElementById('chatBody'),input=document.getElementById('draft');",
  "  pass(!!header&&!!chat&&!!input,'owned chat nodes present');",
  "  const micBox=rect(document.querySelector('.voiceMic'));pass(micBox.width>=48&&micBox.height>=48,'microphone touch target at least 48 CSS px');",
  "  chat.style.scrollBehavior='auto';",
  "  pass(chat.scrollHeight-chat.clientHeight>400,'fixture has scrollable public chat');",
  "  chat.scrollTop=Math.min(180,(chat.scrollHeight-chat.clientHeight)/3);",
  "  const expectedDraft='公开合成草稿，不触发真实任务';",
  "  input.value=expectedDraft;input.dispatchEvent(new Event('input',{bubbles:true}));",
  "  input.focus({preventScroll:true});input.setSelectionRange(2,7,'backward');",
  "  await settle();",
  "  const scroll=chat.scrollTop;",
  "  pass(document.activeElement===input,'draft DOM focus established');",
  "  pass(input.selectionStart===2&&input.selectionEnd===7,'draft selection established');",
  "  pass(scroll>0&&chat.scrollHeight-chat.clientHeight-scroll>100,'fixture is deliberately away from chat bottom');",
  "  const firstRequest='display-request-A';",
  "  let panel=null,transcript=null,panelBox=null,textBox=null,chatMutations=0,textMutations=0;",
  "  chatObserver=new MutationObserver(entries=>{chatMutations+=entries.length;});",
  "  chatObserver.observe(chat,{subtree:true,childList:true,characterData:true});",
  "  function stable(label,checkRect=true){",
  "   const b=document.querySelector('.voicePanel'),t=document.querySelector('.voiceTranscript');",
  "   pass(document.querySelector('.topbar')===header,label+': header identity retained');",
  "   pass(document.getElementById('chatBody')===chat,label+': chat identity retained');",
  "   pass(document.getElementById('draft')===input,label+': draft identity retained');",
  "   pass(document.activeElement===input,label+': draft focus retained');",
  "   pass(input.selectionStart===2&&input.selectionEnd===7&&input.selectionDirection==='backward',label+': draft selection retained');",
  "   pass(input.value===expectedDraft,label+': draft text retained');",
  "   pass(Math.abs(chat.scrollTop-scroll)<=1,label+': non-bottom chat scroll retained');",
  "   if(checkRect){",
  "    pass(b===panel&&t===transcript,label+': same-request panel and transcript retained');",
  "    const p=rect(b),q=rect(t),pd=delta(p,panelBox),td=delta(q,textBox);",
  "    pass(pd<=1,label+': panel rectangle stable');",
  "    pass(td<=1,label+': transcript rectangle stable');",
  "    out.samples.push({label,panel:p,transcript:q,panel_delta_css_px:pd,transcript_delta_css_px:td,chat_scroll_delta_css_px:Math.abs(chat.scrollTop-scroll)});",
  "   }",
  "  }",
  "  emit({phase:'preparing',request:firstRequest,text:'',level:0});await settle();",
  "  stable('preparing creation',false);",
  "  panel=document.querySelector('.voicePanel');transcript=document.querySelector('.voiceTranscript');",
  "  pass(!!panel&&!!transcript,'voice panel and transcript created');",
  "  panelBox=rect(panel);textBox=rect(transcript);",
  "  pass(panelBox.width>0&&panelBox.height>0&&textBox.height>0,'voice geometry has visible non-zero extent');",
  "  transcriptObserver=new MutationObserver(entries=>{textMutations+=entries.length;});",
  "  transcriptObserver.observe(transcript,{subtree:true,childList:true,characterData:true});",
  "  const updates=[",
  "   {label:'listening empty',phase:'listening',text:'',level:0.1},",
  "   {label:'partial short',phase:'listening',text:'先打开企业微信',level:0.3},",
  "   {label:'partial grows',phase:'listening',text:'先打开企业微信工作台，然后打开高德地图',level:0.8},",
  "   {label:'partial revised shorter',phase:'listening',text:'打开高德地图',level:0.2},",
  "   {label:'partial raw characters',phase:'listening',text:'临时修订 <b>原文</b> & 内容',level:0.5},",
  "   {label:'partial long',phase:'listening',text:Array.from({length:35},(_,i)=>'公开合成第'+i+'行：先打开企业微信，再查看上海人民广场。').join('\\n'),level:0.6},",
  "   {label:'partial contracts again',phase:'listening',text:'先打开企业微信，再查看上海人民广场。',level:0.15}",
  "  ];",
  "  for(const u of updates){",
  "   emit({phase:u.phase,request:firstRequest,text:u.text,level:u.level});await settle();",
  "   stable(u.label);",
  "   if(u.text)pass(transcript.textContent===u.text,u.label+': ASR text shown without lexical rewrite');",
  "   pass(!transcript.querySelector('b'),u.label+': transcript text is not parsed as markup');",
  "   const next=JSON.parse(JSON.stringify(state));",
  "   window.agentEvent({type:'state',data:next});await settle();",
  "   stable(u.label+' same-content state');",
  "  }",
  "  const unchanged=updates[updates.length-1].text;",
  "  const beforeTextMutations=textMutations;",
  "  const textChild=transcript.firstChild;",
  "  for(const level of [0,25,75,100]){",
  "   emit({phase:'listening',request:firstRequest,text:unchanged,level});await settle();stable('level '+level);",
  "   pass(transcript.firstChild===textChild,'level '+level+': transcript text node retained');",
  "  }",
  "  pass(textMutations===beforeTextMutations,'level-only updates do not mutate transcript text');",
  "  const longText=Array.from({length:40},(_,i)=>'公开合成长段'+i+'，文字修订仍只在面板内部滚动。').join('\\n');",
  "  emit({phase:'listening',request:firstRequest,text:longText,level:0.3});await settle();stable('overflow transcript');",
  "  pass(transcript.scrollHeight>transcript.clientHeight+20,'long transcript scrolls inside fixed rectangle');",
  "  transcript.scrollTop=0;await settle();",
  "  emit({phase:'listening',request:firstRequest,text:longText+'\\n用户正在查看前文，保持当前位置。',level:0.45});await settle();",
  "  stable('overflow while reading earlier text');",
  "  pass(transcript.scrollTop<=1,'transcript reading position is not forced to bottom');",
  "  emit({phase:'finishing',request:firstRequest,text:longText,level:0});await settle();stable('finishing');",
  "  pass(document.querySelector('.voiceActions [data-a=voiceStop]').disabled,'finishing disables current stop');",
  "  pass(chatMutations===0,'same-session voice/state events leave chat subtree untouched');",
  "  chatObserver.disconnect();chatObserver=null;transcriptObserver.disconnect();transcriptObserver=null;",
  "  const oldPanel=panel,oldStop=panel.querySelector('[data-a=voiceStop]'),oldCancel=panel.querySelector('[data-a=voiceCancel]'),oldMic=document.querySelector('.voiceMic');",
  "  pass(oldStop.dataset.voiceRequest===firstRequest&&oldCancel.dataset.voiceRequest===firstRequest,'old buttons initially own request A');",
  "  const secondRequest='display-request-B';",
  "  emit({phase:'listening',request:secondRequest,text:'第二次合成请求',level:0.2});await settle();",
  "  stable('new request preserves chat chrome',false);",
  "  const nextPanel=document.querySelector('.voicePanel'),nextStop=nextPanel.querySelector('[data-a=voiceStop]');",
  "  pass(nextPanel!==oldPanel&&!oldPanel.isConnected,'new request replaces and detaches previous panel');",
  "  pass(oldStop.dataset.voiceRequest===firstRequest&&oldCancel.dataset.voiceRequest===firstRequest,'detached old buttons never acquire request B');",
  "  pass(oldMic.dataset.voiceRequest===firstRequest&&!oldMic.isConnected,'old mic node never acquires request B');",
  "  pass(document.querySelector('.voiceMic').dataset.voiceRequest===secondRequest,'new mic node owns request B');",
  "  pass(nextStop.dataset.voiceRequest===secondRequest&&!nextStop.disabled,'new button owns active request B');",
  "  emit({phase:'idle',request:firstRequest,text:'',level:0});await settle();",
  "  pass(document.querySelector('.voicePanel')===nextPanel,'stale request A idle cannot remove request B panel');",
  "  const oldHost=document.createElement('div');oldHost.style.cssText='position:fixed;left:-9999px;top:0';document.body.appendChild(oldHost);",
  "  oldHost.appendChild(oldCancel);",
  "  const beforeOld=actions().length;oldCancel.click();await settle();",
  "  let sent=actions().slice(beforeOld).filter(x=>x.kind==='voiceCancel');",
  "  pass(sent.length===1&&sent[0].data.request===firstRequest,'delegated stale cancel sends only its old request A');",
  "  oldHost.remove();",
  "  const beforeNew=actions().length;nextStop.click();await settle();",
  "  sent=actions().slice(beforeNew).filter(x=>x.kind==='voiceStop');",
  "  pass(sent.length===1&&sent[0].data.request===secondRequest,'current stop sends active request B');",
  "  emit({phase:'idle',request:'',text:'',level:0});await settle();",
  "  stable('idle removes panel only',false);",
  "  pass(!document.querySelector('.voicePanel'),'idle removes voice panel');",
  "  const finalText='最终公开合成草稿，未连接识别器';",
  "  window.agentEvent({type:'voiceResult',data:{text:finalText,direct:false}});await settle();",
  "  pass(document.querySelector('.topbar')===header&&document.getElementById('chatBody')===chat&&document.getElementById('draft')===input,'final result retains chat chrome and draft nodes');",
  "  pass(input.value===finalText,'final result fills draft without lexical rewrite');",
  "  out.success=true;",
  "  out.stage='synthetic_display_contracts_passed';",
  "  out.same_request_dom_identity_preserved=true;out.draft_focus_selection_preserved=true;",
  "  out.chat_scroll_preserved=true;out.panel_transcript_rectangles_stable=true;",
  "  out.stale_button_token_preserved=true;",
  "  out.window_css={width:innerWidth,height:innerHeight};",
  " }catch(error){out.stage='synthetic_display_contracts_failed';out.failure_check=String(error&&error.message||error);}",
  " finally{",
  "  if(chatObserver)chatObserver.disconnect();if(transcriptObserver)transcriptObserver.disconnect();",
  "  window.NativeAgent.complete(JSON.stringify(out));",
  " }",
  "})()",
  ""
 );
}

