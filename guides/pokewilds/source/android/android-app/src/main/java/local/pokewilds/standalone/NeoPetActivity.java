package local.pokewilds.standalone;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;
import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;
import android.text.*;import java.io.*;import java.util.*;

public class NeoPetActivity extends Activity {
    private android.content.SharedPreferences prefs;private NeoPetCatalog catalog;private NeoPetCatalog.Pet selected;
    private final Handler handler=new Handler(Looper.getMainLooper());private TextView info;private View matrix;
    private Properties game=new Properties();private long lastRead;private static final int INK=0xffe7e9f0;
    private final Runnable refresh=new Runnable(){public void run(){
        long elapsed=SystemClock.elapsedRealtime(),now=System.currentTimeMillis();
        if(elapsed-lastRead>1000){lastRead=elapsed;game=NeoPetCatalog.readGame(NeoPetActivity.this);updateText();}
        if(catalog!=null){selected=catalog.find(NeoPetCatalog.resolve(prefs,game));
            if(!NeoPetService.running){String action=now<prefs.getLong("until",0)&&selected.id.equals(prefs.getString("actionPet",selected.id))?prefs.getString("action","idle"):"idle";
                NeoPetService.preview=NeoPetAnimator.render(selected.poses,action,elapsed/1000.0,selected.chibi);}
            matrix.invalidate();}
        handler.postDelayed(this,1000/Math.max(15,Math.min(30,prefs.getInt("fps",24))));
    }};
    public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("neo-pet",0);NeoRearDisplay.recover(this);
        try{catalog=NeoPetCatalog.get(this);}catch(Exception e){Toast.makeText(this,"伙伴资源读取失败",1).show();finish();return;}
        selected=catalog.find(NeoPetCatalog.resolve(prefs,game));
        LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(20),dp(20),dp(20),dp(32));body.setBackgroundColor(0xff101923);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);setContentView(scroll);applyInsets(scroll);
        TextView title=new TextView(this);title.setText("荒野 · 背屏伙伴");title.setTextSize(26);title.setTextColor(INK);body.addView(title);
        info=new TextView(this);info.setTextSize(13);info.setTextColor(0xffb7c4d3);info.setPadding(0,dp(12),0,dp(4));body.addView(info);
        matrix=new View(this){final Paint paint=new Paint(3);protected void onDraw(Canvas c){
            float cell=Math.min(getWidth()/19f,getHeight()/25f),ox=(getWidth()-17*cell)/2,oy=(getHeight()-23*cell)/2;
            int[] px=NeoPetService.preview;for(int y=0;y<23;y++)for(int x=0;x<17;x++){int v=px[y*17+x];paint.setColor(v==0?0xff26333f:Color.rgb(v,v,v));c.drawCircle(ox+(x+.5f)*cell,oy+(y+.5f)*cell,cell*.36f,paint);}
        }};body.addView(matrix,new LinearLayout.LayoutParams(-1,dp(250)));matrix.setContentDescription("当前宝可梦的17乘23点阵动画预览");
        button(body,"开始背屏陪伴",()->{
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},813);
            else startCompanion();
        });
        button(body,"停止并恢复系统背屏",()->{stopService(new Intent(this,NeoPetService.class));handler.postDelayed(()->{NeoRearDisplay.recover(this);updateText();},150);});
        LinearLayout actions=new LinearLayout(this);body.addView(actions);
        String[][] labels={{"喂食","feed"},{"抚摸","pet"},{"玩耍","play"},{"睡觉","sleep"}};
        for(String[] a:labels){Button b=button(actions,a[0],()->interact(a[1]));b.setTextSize(12);b.setMinWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));}
        button(body,"队伍跟随 · 选择第几只",()->new AlertDialog.Builder(this).setTitle("跟随当前游戏队伍")
            .setItems(new String[]{"第1只","第2只","第3只","第4只","第5只","第6只"},(d,i)->{prefs.edit().putInt("slot",i).putBoolean("follow",true).apply();lastRead=0;}).show());
        button(body,"图鉴搜索 · "+catalog.pets.size()+"种形象",this::choose);
        button(body,"动画帧率 · 15 / 24 / 30",()->new AlertDialog.Builder(this).setTitle("目标帧率 · 实际发送速度见状态")
            .setSingleChoiceItems(new String[]{"15 FPS · 节能","24 FPS · 默认","30 FPS · 流畅"},prefs.getInt("fps",24)==15?0:prefs.getInt("fps",24)==30?2:1,(d,i)->{prefs.edit().putInt("fps",new int[]{15,24,30}[i]).apply();d.dismiss();lastRead=0;}).show());
        button(body,"请求保存当前游戏",()->{try{File f=new File(getFilesDir(),"game/neo-save.request");f.getParentFile().mkdirs();new FileOutputStream(f).close();Toast.makeText(this,"已请求，进入安全状态后保存",0).show();}catch(IOException e){Toast.makeText(this,"无法创建保存请求",0).show();}});
        TextView help=new TextView(this);help.setText("预览和背屏共用动画。离开游戏后保留最近伙伴；喂食、抚摸等为陪伴互动，不改变游戏内战斗数值。熄屏续播受系统后台策略影响。");help.setTextSize(12);help.setTextColor(0xff93a7bb);help.setPadding(0,dp(12),0,0);body.addView(help);
    }
    private void applyInsets(android.view.View view){
        if(android.os.Build.VERSION.SDK_INT>=35){
            getWindow().setDecorFitsSystemWindows(false);
            view.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return android.view.WindowInsets.CONSUMED;});
            view.requestApplyInsets();
        }
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void startCompanion(){try{startForegroundService(new Intent(this,NeoPetService.class));}catch(Exception e){Toast.makeText(this,NeoRearDisplay.reason(e),1).show();}}
    public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==813){if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)startCompanion();else Toast.makeText(this,"允许通知后即可使用带停止入口的背屏陪伴",1).show();}}
    private void updateText(){
        if(catalog==null)return;selected=catalog.find(NeoPetCatalog.resolve(prefs,game));
        String mode=prefs.getBoolean("follow",true)?"跟随队伍第"+(prefs.getInt("slot",0)+1)+"只":"固定伙伴";
        String live=game.getProperty("pet."+prefs.getInt("slot",0));
        String details=live!=null&&prefs.getBoolean("follow",true)?" · Lv."+game.getProperty("level."+prefs.getInt("slot",0),"?")+" · HP "+game.getProperty("hp."+prefs.getInt("slot",0),"?"):" · 陪伴形象";
        info.setText(selected.name+" · "+mode+details+"\n"+NeoPetService.status+"\n亲密度 "+prefs.getInt("bond."+selected.id,0)+" · 目标 "+prefs.getInt("fps",24)+" FPS\n"+game.getProperty("save","等待进入游戏世界")+saveTime());
    }
    private String saveTime(){try{long ms=Long.parseLong(game.getProperty("savedAt","0"));return ms>0?" · "+android.text.format.DateFormat.format("HH:mm:ss",ms):"";}catch(NumberFormatException e){return "";}}
    private void interact(String action){
        if(selected==null)return;long now=System.currentTimeMillis();String key="bond."+selected.id;int bond=prefs.getInt(key,0);
        SharedPreferences.Editor edit=prefs.edit().putString("action",action).putString("actionPet",selected.id).putLong("until",now+(action.equals("sleep")?3600000:12000));
        if(now-prefs.getLong("lastBond."+selected.id,0)>10000)edit.putInt(key,Math.min(999,bond+1)).putLong("lastBond."+selected.id,now);
        edit.apply();updateText();
    }
    private Button button(LinearLayout body,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());body.addView(b);return b;}
    private void choose(){
        ArrayList<NeoPetCatalog.Pet> all=new ArrayList<>(catalog.pets.values()),filtered=new ArrayList<>(all);
        LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(16),dp(8),dp(16),0);
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("搜索中文名或英文名");body.addView(search);
        ListView list=new ListView(this);ArrayAdapter<NeoPetCatalog.Pet> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,filtered);list.setAdapter(adapter);body.addView(list,new LinearLayout.LayoutParams(-1,dp(330)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("选择陪伴形象").setView(body).setNegativeButton("取消",null).create();
        list.setOnItemClickListener((parent,v,pos,id)->{prefs.edit().putBoolean("follow",false).putString("pet",filtered.get(pos).id).apply();lastRead=0;dialog.dismiss();updateText();});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){String query=s.toString().trim().toLowerCase(Locale.ROOT);filtered.clear();for(NeoPetCatalog.Pet pet:all)if(pet.toString().toLowerCase(Locale.ROOT).contains(query))filtered.add(pet);adapter.notifyDataSetChanged();}public void afterTextChanged(Editable e){}});
        dialog.getWindow();dialog.show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    protected void onResume(){super.onResume();NeoRearDisplay.recover(this);lastRead=0;handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
