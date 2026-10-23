package studio.jiayu.neoagent;
import android.app.Activity;import android.os.*;import android.widget.*;import android.view.accessibility.AccessibilityNodeInfo;import java.io.*;import org.json.*;
/** Real native windows; no merchant actions or network. Tests the production reviewPage collector. */
final class CheckoutReviewContracts {
 final Activity a;final ControlService s;final String oldTask;final TaskRequest oldRequest;int index,passed;JSONObject result=new JSONObject();
 final String[][] pages={
  {"选择规格","数量：1","立即支付¥1.97"},
  {"确认订单","提交订单"},
  {"确认订单","商品：百洁布","数量：1","实付¥4.99(含运费)","收货地址：本地合成收件地址","提交订单"},
  {"选择座位","影片：本地合成电影","影院：本地合成影院","场次：2026-10-23 20:30","座位：5排6座","数量：1","合计¥39.9","提交订单"},
  {"确认订单","影片：本地合成电影","影院：本地合成影院","场次：2026-10-23 20:30","座位：5排6座","数量：1","合计¥39.9","提交订单"},
  {"确认订单","影片：本地合成电影","影院：本地合成影院","场次：2026-10-23 20:30","座位：请选择5排6座","数量：1","合计¥39.9","提交订单"}
 };final boolean[] expected={false,false,true,false,true,false};
 CheckoutReviewContracts(Activity a,ControlService s){this.a=a;this.s=s;oldTask=s.task;oldRequest=s.activeRequest;}
 static void start(Activity a){ControlService s=ControlService.instance;if(s==null||!s.connected||s.busy()){a.finish();return;}new CheckoutReviewContracts(a,s).page();}
 void page(){LinearLayout root=new LinearLayout(a);root.setOrientation(1);root.setPadding(40,160,40,40);for(String text:pages[index]){TextView row=text.startsWith("提交订单")||text.startsWith("立即支付")?new Button(a):new TextView(a);row.setText(text);root.addView(row);}a.setContentView(root);s.activeRequest=null;s.task=index>=3?"查看电影订单核对页":"查看商品订单核对页";new Handler().postDelayed(this::check,700);}
 void check(){try{AccessibilityNodeInfo root=s.getRootInActiveWindow();if(root==null||!a.getPackageName().contentEquals(root.getPackageName()))throw new Exception("Owned fixture lost foreground");boolean actual=s.reviewPage(root);if(actual!=expected[index])throw new Exception("Review proof mismatch in owned case "+index);passed++;if(index==0){if(!s.checkoutPage(root,0))throw new Exception("Financial marker missing");passed++;}if(++index<pages.length){page();return;}result.put("success",true);finish();}catch(Exception e){try{result.put("success",false).put("failure",e.getMessage());}catch(Exception ignored){}finish();}}
 void finish(){s.task=oldTask;s.activeRequest=oldRequest;try(FileOutputStream out=a.openFileOutput("checkout-review-contracts.json",0)){out.write(result.put("passed",passed).put("real_network_calls",0).put("third_party_actions",0).toString(2).getBytes("UTF-8"));}catch(Exception ignored){}a.finish();}
}
