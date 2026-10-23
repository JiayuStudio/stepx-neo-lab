package local.pokewilds.standalone;
import android.app.*;import android.content.*;import android.os.*;import java.util.*;

/** Explicitly started by the user. No boot receiver or automatic restart. */
public class NeoPetService extends Service {
    static volatile boolean running;static volatile String status="未启动背屏 · 可预览互动";
    static volatile int[] preview=new int[391];
    private HandlerThread thread;private Handler handler;private NeoRearDisplay display;private PowerManager.WakeLock lock;
    private android.content.SharedPreferences prefs;private NeoPetCatalog catalog;private Properties game=new Properties();
    private long frames,lastPoll,lastWake,lastReport,lastReportFrames,deadline;
    private final Runnable tick=new Runnable(){public void run(){
        if(!running)return;long elapsed=SystemClock.elapsedRealtime(),now=System.currentTimeMillis();
        try{
            if(elapsed-lastPoll>=1000){lastPoll=elapsed;game=NeoPetCatalog.readGame(NeoPetService.this);}
            NeoPetCatalog.Pet pet=catalog.find(NeoPetCatalog.resolve(prefs,game));
            if(!pet.id.equals(prefs.getString("lastPet","")))prefs.edit().putString("lastPet",pet.id).apply();
            String action=now<prefs.getLong("until",0)&&pet.id.equals(prefs.getString("actionPet",pet.id))?prefs.getString("action","idle"):"idle";
            if(action.equals("idle")&&"battle".equals(game.getProperty("activity"))&&now-Long.parseLong(game.getProperty("updated","0"))<10000)action="play";
            int[] pixels=NeoPetAnimator.render(pet.poses,action,elapsed/1000.0,pet.chibi);
            display.send(pixels);preview=pixels;frames++;
            if(elapsed-lastWake>=30000){lastWake=elapsed;lock.acquire(90000);}
            if(elapsed-lastReport>=1000){
                double hz=(frames-lastReportFrames)*1000.0/(elapsed-lastReport);
                status=pet.name+" · 发送 "+String.format(java.util.Locale.ROOT,"%.1f",hz)+" 帧/秒";
                prefs.edit().putLong("frames",frames).putLong("heartbeat",now).putString("status",status).apply();
                lastReport=elapsed;lastReportFrames=frames;
            }
        }catch(Exception e){status="背屏已停止："+NeoRearDisplay.reason(e);prefs.edit().putString("status",status).apply();stopSelf();return;}
        int fps=Math.max(15,Math.min(30,prefs.getInt("fps",24)));long period=1000/fps;
        deadline+=period;long uptime=SystemClock.uptimeMillis();if(deadline<uptime-period)deadline=uptime;
        if(running)handler.postAtTime(this,Math.max(uptime+1,deadline));
    }};
    public IBinder onBind(Intent i){return null;}
    public void onCreate(){super.onCreate();prefs=getSharedPreferences("neo-pet",0);}
    public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(running)return START_NOT_STICKY;
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("neo-pet","宝可梦背屏伙伴",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,72,new Intent(this,NeoPetActivity.class),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,71,new Intent(this,NeoPetService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
        startForeground(812,new Notification.Builder(this,"neo-pet").setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("宝可梦背屏陪伴").setContentText("点击互动 · 点击停止恢复背屏").setContentIntent(open)
            .addAction(new Notification.Action.Builder(null,"停止陪伴",stop).build()).setOngoing(true).build());
        try{
            catalog=NeoPetCatalog.get(this);display=new NeoRearDisplay(this);display.acquire();
            lock=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PokeWildsNeo:companion");
            lock.setReferenceCounted(false);lock.acquire(90000);
            thread=new HandlerThread("NeoCompanion");thread.start();handler=new Handler(thread.getLooper());
            long t=SystemClock.elapsedRealtime();lastReport=lastWake=t;deadline=SystemClock.uptimeMillis();running=true;handler.post(tick);
        }catch(Exception e){status="无法启动背屏："+NeoRearDisplay.reason(e);stopSelf();}
        return START_NOT_STICKY;
    }
    public void onDestroy(){
        running=false;if(handler!=null)handler.removeCallbacksAndMessages(null);
        if(thread!=null){thread.quitSafely();try{thread.join(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        try{if(display!=null)display.restore();if(!status.contains("无法")&&!status.contains("停止："))status="陪伴已停止 · 系统背屏已恢复";}
        catch(Exception e){status="背屏恢复待重试："+NeoRearDisplay.reason(e);}
        if(lock!=null&&lock.isHeld())lock.release();stopForeground(true);super.onDestroy();
    }
}
