package studio.jiayu.neoagent;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.Gravity;import android.widget.*;
/** Visible, private launch surface for the currently owned task; never accepts an external target. */
public final class LaunchRelayActivity extends Activity {
 boolean dispatched;final Handler main=new Handler(Looper.getMainLooper());
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);view.setGravity(Gravity.CENTER);view.setPadding(32,32,32,32);view.setBackgroundColor(Color.rgb(28,28,30));TextView title=new TextView(this);title.setTextColor(Color.rgb(237,237,240));title.setTextSize(20);title.setGravity(Gravity.CENTER);String name="应用";try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getIntent().getStringExtra("package"),0)).toString();}catch(Exception ignored){}title.setText("正在打开"+name+"…");view.addView(title);ProgressBar progress=new ProgressBar(this);view.addView(progress,new LinearLayout.LayoutParams(56,56));setContentView(view);main.postDelayed(this::finish,5000);}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&!dispatched)main.postDelayed(this::dispatch,180);}
 void dispatch(){if(dispatched||isFinishing()||!hasWindowFocus())return;dispatched=true;ControlService s=ControlService.instance;long g=getIntent().getLongExtra("generation",-1);try{AppLauncher.fromForeground(this,s,getIntent().getStringExtra("package"),g,getIntent().getStringExtra("run"));}catch(Exception rejected){if(s!=null)try{s.logFor(g,"前台启动请求未执行 · 保留原目标");}catch(Exception ignored){}}finally{finish();}}
 @Override public void onDestroy(){main.removeCallbacksAndMessages(null);super.onDestroy();}
}
